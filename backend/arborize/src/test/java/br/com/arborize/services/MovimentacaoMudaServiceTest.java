package br.com.arborize.services;

import br.com.arborize.dtos.request.MovimentacaoMudaRequestDTO;
import br.com.arborize.entities.Especie;
import br.com.arborize.entities.DocumentEntities;
import br.com.arborize.entities.MovimentacaoMuda;
import br.com.arborize.enums.TipoMovimentacaoMuda;
import br.com.arborize.exceptions.ComprovanteMovimentacaoInvalidoException;
import br.com.arborize.exceptions.EstoqueInsuficienteException;
import br.com.arborize.exceptions.MovimentacaoMudaInvalidaException;
import br.com.arborize.mappers.MovimentacaoMudaMapper;
import br.com.arborize.repositories.DocumentRepository;
import br.com.arborize.repositories.EspecieRepository;
import br.com.arborize.repositories.MovimentacaoMudaRepository;
import br.com.arborize.validation.ComprovanteMovimentacaoValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

class MovimentacaoMudaServiceTest {
    private MovimentacaoMudaRepository movimentacoes;
    private EspecieRepository especies;
    private DocumentRepository documentos;
    private DocumentService documentService;
    private MovimentacaoMudaService service;

    @BeforeEach
    void configurar() {
        movimentacoes = mock(MovimentacaoMudaRepository.class);
        especies = mock(EspecieRepository.class);
        documentos = mock(DocumentRepository.class);
        documentService = mock(DocumentService.class);
        service = new MovimentacaoMudaService(
                movimentacoes,
                especies,
                documentos,
                documentService,
                new ComprovanteMovimentacaoValidator(),
                new MovimentacaoMudaMapper());
        when(movimentacoes.saveAndFlush(any(MovimentacaoMuda.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void entradaAumentaSaldoERegistraMovimentoPorEspecie() {
        Especie especie = especie(4L, 20);
        when(especies.findByIdForUpdate(4L)).thenReturn(Optional.of(especie));

        var response = service.criar(request(TipoMovimentacaoMuda.ENTRADA_GERMINACAO, 4L, 30));

        assertEquals(50, especie.getMudasDisponiveis());
        assertEquals(4L, response.especieId());
        assertEquals(30, response.quantidade());
        assertEquals(TipoMovimentacaoMuda.ENTRADA_GERMINACAO, response.tipo());
    }

    @Test
    void saidaEPerdaSubtraemSaldo() {
        Especie especie = especie(8L, 50);
        when(especies.findByIdForUpdate(8L)).thenReturn(Optional.of(especie));

        service.criar(request(TipoMovimentacaoMuda.SAIDA_PLANTIO, 8L, 15));
        assertEquals(35, especie.getMudasDisponiveis());

        service.criar(request(TipoMovimentacaoMuda.BAIXA_PERDA, 8L, 5));
        assertEquals(30, especie.getMudasDisponiveis());
    }

    @Test
    void saidaMaiorQueSaldoNaoAlteraEstoqueNemGravaMovimento() {
        Especie especie = especie(5L, 7);
        when(especies.findByIdForUpdate(5L)).thenReturn(Optional.of(especie));

        assertThrows(EstoqueInsuficienteException.class,
                () -> service.criar(request(TipoMovimentacaoMuda.SAIDA_PLANTIO, 5L, 8)));

        assertEquals(7, especie.getMudasDisponiveis());
        verify(movimentacoes, never()).saveAndFlush(any());
    }

    @Test
    void edicaoCompensaEAplicaSomenteADiferencaDeSaldo() {
        Especie especie = especie(2L, 15);
        MovimentacaoMuda existente = movimento(TipoMovimentacaoMuda.SAIDA_PLANTIO, especie, 5);
        when(movimentacoes.findByIdForUpdate(22L)).thenReturn(Optional.of(existente));
        when(especies.findByIdForUpdate(2L)).thenReturn(Optional.of(especie));

        var response = service.atualizar(22L, request(TipoMovimentacaoMuda.ENTRADA_GERMINACAO, 2L, 3));

        assertEquals(23, especie.getMudasDisponiveis());
        assertEquals(TipoMovimentacaoMuda.ENTRADA_GERMINACAO, response.tipo());
        assertEquals(3, response.quantidade());
    }

    @Test
    void edicaoEntreEspeciesAplicaDeltaEmCadaSaldo() {
        Especie antiga = especie(2L, 14);
        Especie nova = especie(9L, 10);
        MovimentacaoMuda existente = movimento(TipoMovimentacaoMuda.ENTRADA_GERMINACAO, antiga, 4);
        when(movimentacoes.findByIdForUpdate(31L)).thenReturn(Optional.of(existente));
        when(especies.findByIdForUpdate(2L)).thenReturn(Optional.of(antiga));
        when(especies.findByIdForUpdate(9L)).thenReturn(Optional.of(nova));

        service.atualizar(31L, request(TipoMovimentacaoMuda.SAIDA_PLANTIO, 9L, 3));

        assertEquals(10, antiga.getMudasDisponiveis());
        assertEquals(7, nova.getMudasDisponiveis());
        org.mockito.InOrder ordem = org.mockito.Mockito.inOrder(especies);
        ordem.verify(especies).findByIdForUpdate(2L);
        ordem.verify(especies).findByIdForUpdate(9L);
    }

    @Test
    void saidaParaPlantioExigeDestinoMesmoSeChamadoForaDoController() {
        Especie especie = especie(1L, 10);
        when(especies.findByIdForUpdate(1L)).thenReturn(Optional.of(especie));
        var request = request(TipoMovimentacaoMuda.SAIDA_PLANTIO, 1L, 1);
        request.setDestino(null);

        assertThrows(MovimentacaoMudaInvalidaException.class, () -> service.criar(request));

        assertEquals(10, especie.getMudasDisponiveis());
        verify(movimentacoes, never()).saveAndFlush(any());
    }

    @Test
    void comprovanteValidadoAntesDeUploadOuGravacao() throws java.io.IOException {
        Especie especie = especie(7L, 10);
        when(especies.findByIdForUpdate(7L)).thenReturn(Optional.of(especie));
        var request = request(TipoMovimentacaoMuda.ENTRADA_GERMINACAO, 7L, 1);
        MultipartFile arquivoInvalido = mock(MultipartFile.class);
        when(arquivoInvalido.isEmpty()).thenReturn(false);
        when(arquivoInvalido.getSize()).thenReturn(12L);
        when(arquivoInvalido.getOriginalFilename()).thenReturn("arquivo.txt");
        when(arquivoInvalido.getContentType()).thenReturn("text/plain");
        when(arquivoInvalido.getBytes()).thenReturn("texto".getBytes());
        request.setComprovante(arquivoInvalido);

        assertThrows(ComprovanteMovimentacaoInvalidoException.class, () -> service.criar(request));

        verify(documentService, never()).uploadAndCreateDocument(any());
        verify(documentos, never()).save(any());
        verify(movimentacoes, never()).saveAndFlush(any());
    }

    @Test
    void arquivoNovoEApagadoSeATransacaoDeCriacaoFalhar() {
        Especie especie = especie(7L, 10);
        when(especies.findByIdForUpdate(7L)).thenReturn(Optional.of(especie));
        DocumentEntities documento = documento(40L, "novo-pdf");
        when(documentService.uploadAndCreateDocument(any())).thenReturn(documento);
        when(documentos.save(documento)).thenReturn(documento);
        var request = request(TipoMovimentacaoMuda.ENTRADA_GERMINACAO, 7L, 1);
        request.setComprovante(pdf());

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.criar(request);
            TransactionSynchronization synchronization = TransactionSynchronizationManager.getSynchronizations().getFirst();
            synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        verify(documentService).deleteStoredObject("novo-pdf");
    }

    @Test
    void falhaNoUploadNaoPersisteDocumentoOuMovimentacao() {
        Especie especie = especie(7L, 10);
        when(especies.findByIdForUpdate(7L)).thenReturn(Optional.of(especie));
        when(documentService.uploadAndCreateDocument(any()))
                .thenThrow(new IllegalStateException("MinIO indisponível"));
        var request = request(TipoMovimentacaoMuda.ENTRADA_GERMINACAO, 7L, 1);
        request.setComprovante(pdf());

        assertThrows(IllegalStateException.class, () -> service.criar(request));

        verify(documentos, never()).save(any());
        verify(movimentacoes, never()).saveAndFlush(any());
        verify(documentService, never()).deleteStoredObject(any());
    }

    @Test
    void falhaAoPersistirMetadadosRemoveObjetoRecemEnviado() {
        Especie especie = especie(7L, 10);
        when(especies.findByIdForUpdate(7L)).thenReturn(Optional.of(especie));
        DocumentEntities documento = documento(41L, "upload-sem-metadados");
        when(documentService.uploadAndCreateDocument(any())).thenReturn(documento);
        when(documentos.save(documento)).thenThrow(new IllegalStateException("Banco indisponível"));
        var request = request(TipoMovimentacaoMuda.ENTRADA_GERMINACAO, 7L, 1);
        request.setComprovante(pdf());

        assertThrows(IllegalStateException.class, () -> service.criar(request));

        verify(documentService).deleteStoredObject("upload-sem-metadados");
        verify(movimentacoes, never()).saveAndFlush(any());
    }

    @Test
    void comprovanteAnteriorSoEApagadoDepoisDoCommitDaSubstituicao() {
        Especie especie = especie(7L, 10);
        DocumentEntities anterior = documento(30L, "comprovante-antigo");
        MovimentacaoMuda existente = movimento(TipoMovimentacaoMuda.ENTRADA_GERMINACAO, especie, 2);
        existente.setComprovante(anterior);
        when(movimentacoes.findByIdForUpdate(22L)).thenReturn(Optional.of(existente));
        when(especies.findByIdForUpdate(7L)).thenReturn(Optional.of(especie));
        DocumentEntities novo = documento(31L, "comprovante-novo");
        when(documentService.uploadAndCreateDocument(any())).thenReturn(novo);
        when(documentos.save(novo)).thenReturn(novo);
        var request = request(TipoMovimentacaoMuda.ENTRADA_GERMINACAO, 7L, 4);
        request.setComprovante(pdf());

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.atualizar(22L, request);
            TransactionSynchronization synchronization = TransactionSynchronizationManager.getSynchronizations().getFirst();
            synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        verify(documentos).deleteById(30L);
        verify(documentService).deleteStoredObject("comprovante-antigo");
        verify(documentService, never()).deleteStoredObject("comprovante-novo");
    }

    private MovimentacaoMudaRequestDTO request(TipoMovimentacaoMuda tipo, Long especieId, int quantidade) {
        MovimentacaoMudaRequestDTO request = new MovimentacaoMudaRequestDTO();
        request.setTipo(tipo);
        request.setEspecieId(especieId);
        request.setQuantidade(quantidade);
        request.setData(LocalDate.of(2026, 6, 10));
        if (tipo == TipoMovimentacaoMuda.SAIDA_PLANTIO) request.setDestino("Praça municipal");
        return request;
    }

    private Especie especie(Long id, int estoque) {
        Especie especie = new Especie();
        especie.setId(id);
        especie.setNomePopular("Ipê-amarelo");
        especie.setNomeCientifico("Handroanthus albus");
        especie.setMudasDisponiveis(estoque);
        return especie;
    }

    private MovimentacaoMuda movimento(TipoMovimentacaoMuda tipo, Especie especie, int quantidade) {
        MovimentacaoMuda movimento = new MovimentacaoMuda();
        movimento.setId(22L);
        movimento.setTipo(tipo);
        movimento.setEspecie(especie);
        movimento.setQuantidade(quantidade);
        movimento.setData(LocalDate.of(2026, 6, 1));
        return movimento;
    }

    private DocumentEntities documento(Long id, String caminho) {
        DocumentEntities documento = DocumentEntities.builder()
                .originalFilename("guia.pdf")
                .filePath(caminho)
                .contentType("application/pdf")
                .extension(".pdf")
                .sizeBytes(8L)
                .build();
        documento.setId(id);
        return documento;
    }

    private MockMultipartFile pdf() {
        return new MockMultipartFile("comprovante", "guia.pdf", "application/pdf", "%PDF-1.7".getBytes());
    }
}
