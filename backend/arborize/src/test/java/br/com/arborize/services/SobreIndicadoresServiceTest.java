package br.com.arborize.services;

import br.com.arborize.dtos.response.SobreIndicadoresResponseDTO;
import br.com.arborize.enums.SubstituicaoBotanicaStatus;
import br.com.arborize.enums.TipoMovimentacaoMuda;
import br.com.arborize.repositories.EspecieRepository;
import br.com.arborize.repositories.MovimentacaoMudaRepository;
import br.com.arborize.repositories.SolicitacaoMudaRepository;
import br.com.arborize.repositories.SolicitacaoSubstituicaoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SobreIndicadoresServiceTest {

    @Mock
    private EspecieRepository especieRepository;

    @Mock
    private MovimentacaoMudaRepository movimentacaoMudaRepository;

    @Mock
    private SolicitacaoMudaRepository solicitacaoMudaRepository;

    @Mock
    private SolicitacaoSubstituicaoRepository solicitacaoSubstituicaoRepository;

    @InjectMocks
    private SobreIndicadoresService service;

    @Test
    void deveAgregarOsQuatroIndicadoresPublicosDoBanco() {
        when(movimentacaoMudaRepository.somarQuantidadePorTipo(TipoMovimentacaoMuda.SAIDA_PLANTIO)).thenReturn(1240L);
        when(especieRepository.sumMudasDisponiveis()).thenReturn(3850L);
        when(solicitacaoMudaRepository.somarTotalRecicaveis()).thenReturn(14500L);
        when(solicitacaoSubstituicaoRepository.countByStatus(SubstituicaoBotanicaStatus.CONCLUIDA)).thenReturn(310L);

        SobreIndicadoresResponseDTO resultado = service.obterIndicadores();

        assertThat(resultado.mudasDoadas()).isEqualTo(1240L);
        assertThat(resultado.estoqueViveiro()).isEqualTo(3850L);
        assertThat(resultado.garrafasPetArrecadadas()).isEqualTo(14500L);
        assertThat(resultado.ninsSubstituidos()).isEqualTo(310L);

        verify(movimentacaoMudaRepository).somarQuantidadePorTipo(TipoMovimentacaoMuda.SAIDA_PLANTIO);
        verify(especieRepository).sumMudasDisponiveis();
        verify(solicitacaoMudaRepository).somarTotalRecicaveis();
        verify(solicitacaoSubstituicaoRepository).countByStatus(SubstituicaoBotanicaStatus.CONCLUIDA);
    }

    @Test
    void deveRetornarZeroQuandoNaoHaRegistros() {
        when(movimentacaoMudaRepository.somarQuantidadePorTipo(TipoMovimentacaoMuda.SAIDA_PLANTIO)).thenReturn(0L);
        when(especieRepository.sumMudasDisponiveis()).thenReturn(0L);
        when(solicitacaoMudaRepository.somarTotalRecicaveis()).thenReturn(0L);
        when(solicitacaoSubstituicaoRepository.countByStatus(SubstituicaoBotanicaStatus.CONCLUIDA)).thenReturn(0L);

        SobreIndicadoresResponseDTO resultado = service.obterIndicadores();

        assertThat(resultado).isEqualTo(new SobreIndicadoresResponseDTO(0L, 0L, 0L, 0L));
    }
}
