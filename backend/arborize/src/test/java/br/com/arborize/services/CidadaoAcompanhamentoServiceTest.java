package br.com.arborize.services;

import br.com.arborize.dtos.response.CidadaoAcompanhamentosResponseDTO;
import br.com.arborize.entities.Especie;
import br.com.arborize.entities.SolicitacaoMuda;
import br.com.arborize.entities.SolicitacaoSubstituicao;
import br.com.arborize.entities.SolicitacaoSubstituicaoEvento;
import br.com.arborize.entities.Usuario;
import br.com.arborize.enums.SolicitacaoMudaStatus;
import br.com.arborize.enums.SubstituicaoBotanicaStatus;
import br.com.arborize.mappers.SolicitacaoMudaMapper;
import br.com.arborize.repositories.SolicitacaoMudaRepository;
import br.com.arborize.repositories.SolicitacaoSubstituicaoEventoRepository;
import br.com.arborize.repositories.SolicitacaoSubstituicaoRepository;
import br.com.arborize.repositories.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CidadaoAcompanhamentoServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private SolicitacaoMudaRepository solicitacaoMudaRepository;

    @Mock
    private SolicitacaoSubstituicaoRepository solicitacaoSubstituicaoRepository;

    @Mock
    private SolicitacaoSubstituicaoEventoRepository solicitacaoSubstituicaoEventoRepository;

    @Spy
    private SolicitacaoMudaMapper solicitacaoMudaMapper = new SolicitacaoMudaMapper();

    @InjectMocks
    private CidadaoAcompanhamentoService service;

    @Test
    void deveRetornarSolicitacoesDeMudasESubstituicoesComHistoricoDoUsuarioAutenticado() {
        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setCpf("123.456.789-01");

        Especie especie = new Especie();
        especie.setId(2L);
        especie.setNomePopular("Ipê Amarelo");

        SolicitacaoMuda muda = new SolicitacaoMuda();
        muda.setId(15L);
        muda.setEspecie(especie);
        muda.setQuantidadeMudas(2);
        muda.setTotalRecicaveis(10);
        muda.setQuantidadeRecebida(10);
        muda.setStatus(SolicitacaoMudaStatus.APROVADA);
        muda.setLogradouro("Rua Central");
        muda.setNumeroEndereco("100");
        muda.setBairro("Centro");
        muda.setCriadaEm(LocalDateTime.of(2026, 11, 20, 10, 0));

        SolicitacaoSubstituicao sub = new SolicitacaoSubstituicao();
        sub.setId(30L);
        sub.setProtocolo("SUB-2026-0001");
        sub.setStatus(SubstituicaoBotanicaStatus.EM_ANALISE);
        sub.setBairro("Campo Novo");
        sub.setLogradouro("Rua das Flores");
        sub.setNumeroEndereco("120");
        sub.setDescricaoArvore("Nim Indiano na calçada");
        sub.setCriadaEm(LocalDateTime.of(2026, 11, 22, 14, 30));

        SolicitacaoSubstituicaoEvento evento = new SolicitacaoSubstituicaoEvento();
        evento.setStatus(SubstituicaoBotanicaStatus.RECEBIDA);
        evento.setDescricao("Solicitação recebida pela AMMA.");
        evento.setPublico(true);
        evento.setCriadoEm(LocalDateTime.of(2026, 11, 22, 14, 30));

        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(solicitacaoMudaRepository.findByCpfOrderByCriadaEmDesc("12345678901")).thenReturn(List.of(muda));
        when(solicitacaoSubstituicaoRepository.findByCpfOrderByCriadaEmDesc("12345678901")).thenReturn(List.of(sub));
        when(solicitacaoSubstituicaoEventoRepository.findBySolicitacaoIdAndPublicoTrueOrderByCriadoEmAsc(30L))
                .thenReturn(List.of(evento));

        CidadaoAcompanhamentosResponseDTO resultado = service.listarDoUsuario(10L);

        assertThat(resultado.solicitacoesMuda()).hasSize(1);
        assertThat(resultado.solicitacoesMuda().getFirst().protocolo()).isEqualTo("SOL-2026-0015");
        assertThat(resultado.solicitacoesMuda().getFirst().especieNome()).isEqualTo("Ipê Amarelo");
        assertThat(resultado.solicitacoesMuda().getFirst().status()).isEqualTo("APROVADA");

        assertThat(resultado.substituicoesBotanicas()).hasSize(1);
        assertThat(resultado.substituicoesBotanicas().getFirst().protocolo()).isEqualTo("SUB-2026-0001");
        assertThat(resultado.substituicoesBotanicas().getFirst().historico()).hasSize(1);
        assertThat(resultado.substituicoesBotanicas().getFirst().historico().getFirst().descricao())
                .isEqualTo("Solicitação recebida pela AMMA.");

        verify(solicitacaoMudaRepository).findByCpfOrderByCriadaEmDesc("12345678901");
        verify(solicitacaoSubstituicaoRepository).findByCpfOrderByCriadaEmDesc("12345678901");
    }

    @Test
    void deveRetornarListasVaziasQuandoUsuarioNaoTemSolicitacoes() {
        Usuario usuario = new Usuario();
        usuario.setId(11L);
        usuario.setCpf("99988877766");

        when(usuarioRepository.findById(11L)).thenReturn(Optional.of(usuario));
        when(solicitacaoMudaRepository.findByCpfOrderByCriadaEmDesc("99988877766")).thenReturn(List.of());
        when(solicitacaoSubstituicaoRepository.findByCpfOrderByCriadaEmDesc("99988877766")).thenReturn(List.of());

        CidadaoAcompanhamentosResponseDTO resultado = service.listarDoUsuario(11L);

        assertThat(resultado.solicitacoesMuda()).isEmpty();
        assertThat(resultado.substituicoesBotanicas()).isEmpty();
    }
}
