package br.com.arborize.services;

import br.com.arborize.dtos.response.CidadaoAcompanhamentosResponseDTO;
import br.com.arborize.dtos.response.CidadaoAcompanhamentosResponseDTO.SolicitacaoMudaResumo;
import br.com.arborize.dtos.response.CidadaoAcompanhamentosResponseDTO.SubstituicaoEventoResumo;
import br.com.arborize.dtos.response.CidadaoAcompanhamentosResponseDTO.SubstituicaoResumo;
import br.com.arborize.entities.Usuario;
import br.com.arborize.mappers.SolicitacaoMudaMapper;
import br.com.arborize.repositories.SolicitacaoMudaRepository;
import br.com.arborize.repositories.SolicitacaoSubstituicaoEventoRepository;
import br.com.arborize.repositories.SolicitacaoSubstituicaoRepository;
import br.com.arborize.repositories.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CidadaoAcompanhamentoService {

    private final UsuarioRepository usuarioRepository;
    private final SolicitacaoMudaRepository solicitacaoMudaRepository;
    private final SolicitacaoSubstituicaoRepository solicitacaoSubstituicaoRepository;
    private final SolicitacaoSubstituicaoEventoRepository solicitacaoSubstituicaoEventoRepository;
    private final SolicitacaoMudaMapper solicitacaoMudaMapper;

    public CidadaoAcompanhamentoService(
            UsuarioRepository usuarioRepository,
            SolicitacaoMudaRepository solicitacaoMudaRepository,
            SolicitacaoSubstituicaoRepository solicitacaoSubstituicaoRepository,
            SolicitacaoSubstituicaoEventoRepository solicitacaoSubstituicaoEventoRepository,
            SolicitacaoMudaMapper solicitacaoMudaMapper
    ) {
        this.usuarioRepository = usuarioRepository;
        this.solicitacaoMudaRepository = solicitacaoMudaRepository;
        this.solicitacaoSubstituicaoRepository = solicitacaoSubstituicaoRepository;
        this.solicitacaoSubstituicaoEventoRepository = solicitacaoSubstituicaoEventoRepository;
        this.solicitacaoMudaMapper = solicitacaoMudaMapper;
    }

    @Transactional(readOnly = true)
    public CidadaoAcompanhamentosResponseDTO listarDoUsuario(Long usuarioId) {
        if (usuarioId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado.");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));

        String cpfNormalizado = usuario.getCpf() == null ? "" : usuario.getCpf().replaceAll("\\D", "");
        if (cpfNormalizado.isEmpty()) {
            return new CidadaoAcompanhamentosResponseDTO(List.of(), List.of());
        }

        List<SolicitacaoMudaResumo> mudas = solicitacaoMudaRepository
                .findByCpfOrderByCriadaEmDesc(cpfNormalizado)
                .stream()
                .map(item -> new SolicitacaoMudaResumo(
                        item.getId(),
                        solicitacaoMudaMapper.protocolo(item),
                        item.getEspecie() != null ? item.getEspecie().getNomePopular() : "Espécie não informada",
                        item.getQuantidadeMudas(),
                        item.getTotalRecicaveis(),
                        item.getQuantidadeRecebida(),
                        item.getStatus().name(),
                        item.getMotivoRejeicao(),
                        item.getLogradouro(),
                        item.getNumeroEndereco(),
                        item.getBairro(),
                        item.getComplemento(),
                        item.getPontoReferencia(),
                        item.getLocalPlantio() != null ? item.getLocalPlantio().name() : null,
                        item.getCriadaEm()
                ))
                .toList();

        List<SubstituicaoResumo> substituicoes = solicitacaoSubstituicaoRepository
                .findByCpfOrderByCriadaEmDesc(cpfNormalizado)
                .stream()
                .map(item -> {
                    List<SubstituicaoEventoResumo> historico = solicitacaoSubstituicaoEventoRepository
                            .findBySolicitacaoIdAndPublicoTrueOrderByCriadoEmAsc(item.getId())
                            .stream()
                            .map(ev -> new SubstituicaoEventoResumo(
                                    ev.getStatus().name(),
                                    ev.getDescricao(),
                                    ev.getCriadoEm()
                            ))
                            .toList();

                    return new SubstituicaoResumo(
                            item.getId(),
                            item.getProtocolo(),
                            item.getStatus().name(),
                            item.getBairro(),
                            item.getLogradouro(),
                            item.getNumeroEndereco(),
                            item.getComplemento(),
                            item.getPontoReferencia(),
                            item.getDescricaoArvore(),
                            item.getCriadaEm(),
                            historico
                    );
                })
                .toList();

        return new CidadaoAcompanhamentosResponseDTO(mudas, substituicoes);
    }
}
