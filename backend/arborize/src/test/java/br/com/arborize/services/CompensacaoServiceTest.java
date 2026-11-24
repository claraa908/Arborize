package br.com.arborize.services;

import br.com.arborize.entities.EntregaCompensacao;
import br.com.arborize.entities.TermoCompensacao;
import br.com.arborize.enums.CompensacaoStatus;
import br.com.arborize.exceptions.CompensacaoException;
import br.com.arborize.repositories.EntregaCompensacaoRepository;
import br.com.arborize.repositories.TermoCompensacaoRepository;
import br.com.arborize.storage.CompensacaoStorage;
import br.com.arborize.validation.CompensacaoArquivoValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompensacaoServiceTest {
    @Mock TermoCompensacaoRepository termos;
    @Mock EntregaCompensacaoRepository entregas;
    @Mock CompensacaoStorage storage;
    @Mock CompensacaoArquivoValidator validator;

    private CompensacaoService service() { return new CompensacaoService(termos, entregas, storage, validator); }

    @AfterEach void limparTransacao() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.clearSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test void parcelaAcumulaRecibosERecalculaEstado() {
        var termo = termoComId();
        var arquivo = new CompensacaoArquivoValidator.Arquivo(new byte[]{1}, "application/pdf", "recibo.pdf");
        when(validator.validarRecibo(any())).thenReturn(arquivo);
        when(termos.findByIdForUpdate(7L)).thenReturn(Optional.of(termo));
        when(entregas.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        when(entregas.somarQuantidadesPorTermo(7L)).thenReturn(25L);
        when(entregas.findAllByTermoIdOrderByCriadoEmAscIdAsc(7L)).thenAnswer(call -> List.of(
                new EntregaCompensacao(termo, 10, "key-a", "a.pdf", "application/pdf", 1),
                new EntregaCompensacao(termo, 15, "key-b", "b.pdf", "application/pdf", 1)));
        when(termos.saveAndFlush(termo)).thenReturn(termo);
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);

        var resposta = service().adicionarEntrega(7L, 15, new MockMultipartFile("recibo", "recibo.pdf", "application/pdf", new byte[]{1}));

        assertEquals(25, resposta.quantidadeEntregue());
        assertEquals(CompensacaoStatus.PARCIALMENTE_DOADO, resposta.status());
        assertEquals(2, resposta.entregas().size());
        verify(entregas).saveAndFlush(any(EntregaCompensacao.class));
    }

    @Test void atualizacaoManualPersisteEstadoAutorizado() {
        var termo = termoComId();
        when(termos.findByIdForUpdate(7L)).thenReturn(Optional.of(termo));
        when(entregas.findAllByTermoIdOrderByCriadoEmAscIdAsc(7L)).thenReturn(List.of());
        when(termos.saveAndFlush(termo)).thenReturn(termo);

        var resposta = service().atualizarStatus(7L, CompensacaoStatus.TOTALMENTE_DOADO);

        assertEquals(CompensacaoStatus.TOTALMENTE_DOADO, resposta.status());
    }

    @Test void acordoIniciaAbertoEFechamentoAconteceAoAlcancarQuantidade() {
        var termo = termoComId();
        assertEquals(CompensacaoStatus.EM_ABERTO, termo.getStatus());
        when(validator.validarRecibo(any())).thenReturn(new CompensacaoArquivoValidator.Arquivo(
                new byte[]{1}, "application/pdf", "recibo.pdf"));
        when(termos.findByIdForUpdate(7L)).thenReturn(Optional.of(termo));
        when(entregas.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        when(entregas.somarQuantidadesPorTermo(7L)).thenReturn(100L);
        when(entregas.findAllByTermoIdOrderByCriadoEmAscIdAsc(7L)).thenReturn(List.of(
                new EntregaCompensacao(termo, 40, "key-a", "a.pdf", "application/pdf", 1),
                new EntregaCompensacao(termo, 60, "key-b", "b.pdf", "application/pdf", 1)));
        when(termos.saveAndFlush(termo)).thenReturn(termo);
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);

        var resposta = service().adicionarEntrega(7L, 60,
                new MockMultipartFile("recibo", "recibo.pdf", "application/pdf", new byte[]{1}));

        assertEquals(100, resposta.quantidadeEntregue());
        assertEquals(CompensacaoStatus.TOTALMENTE_DOADO, resposta.status());
    }

    @Test void estadoNuloEQuantidadeInvalidaSaoRejeitados() {
        assertThrows(CompensacaoException.class, () -> service().atualizarStatus(7L, null));
        assertThrows(CompensacaoException.class, () -> service().adicionarEntrega(7L, 0, null));
        verifyNoInteractions(termos, entregas, storage);
    }

    @Test void falhaMinioImpedePersistenciaDoTermo() {
        var arquivo = new CompensacaoArquivoValidator.Arquivo(new byte[]{1}, "application/pdf", "termo.pdf");
        when(validator.validarTermo(any())).thenReturn(arquivo);
        doThrow(new CompensacaoException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                "armazenamento-indisponivel", "Não foi possível armazenar o documento.")).when(storage)
                .enviar(anyString(), any(), anyString());
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        assertThrows(CompensacaoException.class, () -> service().criar(
                new br.com.arborize.dtos.request.TermoCompensacaoRequestDTO("Empresa", "12345678000190", null, null, null, 1),
                new MockMultipartFile("arquivo", "termo.pdf", "application/pdf", new byte[]{1})));
        verifyNoInteractions(termos);
        TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCompletion(org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK));
        verify(storage).remover(anyString());
    }

    private TermoCompensacao termo() {
        return new TermoCompensacao("Empresa Teste", "12345678000190", "TC-1", LocalDate.now(), null,
                100, "term-key", "application/pdf", "termo.pdf", 20);
    }

    private TermoCompensacao termoComId() {
        var termo = spy(termo());
        doReturn(7L).when(termo).getId();
        return termo;
    }
}
