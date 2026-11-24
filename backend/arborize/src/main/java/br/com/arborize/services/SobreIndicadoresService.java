package br.com.arborize.services;

import br.com.arborize.dtos.response.SobreIndicadoresResponseDTO;
import br.com.arborize.enums.SubstituicaoBotanicaStatus;
import br.com.arborize.enums.TipoMovimentacaoMuda;
import br.com.arborize.repositories.EspecieRepository;
import br.com.arborize.repositories.MovimentacaoMudaRepository;
import br.com.arborize.repositories.SolicitacaoMudaRepository;
import br.com.arborize.repositories.SolicitacaoSubstituicaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SobreIndicadoresService {

    private final EspecieRepository especieRepository;
    private final MovimentacaoMudaRepository movimentacaoMudaRepository;
    private final SolicitacaoMudaRepository solicitacaoMudaRepository;
    private final SolicitacaoSubstituicaoRepository solicitacaoSubstituicaoRepository;

    public SobreIndicadoresService(
            EspecieRepository especieRepository,
            MovimentacaoMudaRepository movimentacaoMudaRepository,
            SolicitacaoMudaRepository solicitacaoMudaRepository,
            SolicitacaoSubstituicaoRepository solicitacaoSubstituicaoRepository
    ) {
        this.especieRepository = especieRepository;
        this.movimentacaoMudaRepository = movimentacaoMudaRepository;
        this.solicitacaoMudaRepository = solicitacaoMudaRepository;
        this.solicitacaoSubstituicaoRepository = solicitacaoSubstituicaoRepository;
    }

    @Transactional(readOnly = true)
    public SobreIndicadoresResponseDTO obterIndicadores() {
        long mudasDoadas = Math.max(0L, movimentacaoMudaRepository.somarQuantidadePorTipo(TipoMovimentacaoMuda.SAIDA_PLANTIO));
        long estoqueViveiro = Math.max(0L, especieRepository.sumMudasDisponiveis());
        long garrafasPetArrecadadas = Math.max(0L, solicitacaoMudaRepository.somarTotalRecicaveis());
        long ninsSubstituidos = Math.max(0L, solicitacaoSubstituicaoRepository.countByStatus(SubstituicaoBotanicaStatus.CONCLUIDA));

        return new SobreIndicadoresResponseDTO(
                mudasDoadas,
                estoqueViveiro,
                garrafasPetArrecadadas,
                ninsSubstituidos
        );
    }
}
