package br.com.arborize.mappers;

import br.com.arborize.dtos.response.SolicitacaoMudaResponse;
import br.com.arborize.entities.SolicitacaoMuda;
import org.springframework.stereotype.Component;

import java.time.Year;

@Component
public class SolicitacaoMudaMapper {
    public SolicitacaoMudaResponse toResponse(SolicitacaoMuda solicitacao) {
        return new SolicitacaoMudaResponse(
                solicitacao.getId(),
                String.format("SOL-%d-%04d", Year.from(solicitacao.getCriadaEm()).getValue(), solicitacao.getId()),
                solicitacao.getStatus().name(),
                new SolicitacaoMudaResponse.EspecieResumoResponse(
                        solicitacao.getEspecie().getId(), solicitacao.getEspecie().getNomePopular()
                ),
                solicitacao.getQuantidadeMudas(),
                solicitacao.getTotalRecicaveis(),
                solicitacao.getNomeSolicitante(),
                solicitacao.getCriadaEm()
        );
    }
}
