package br.com.arborize.mappers;

import br.com.arborize.dtos.response.SolicitacaoMudaResponse;
import br.com.arborize.dtos.response.SolicitacaoMudaGestaoResponse;
import br.com.arborize.entities.SolicitacaoMuda;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.Locale;

@Component
public class SolicitacaoMudaMapper {
    public SolicitacaoMudaResponse toResponse(SolicitacaoMuda solicitacao) {
        return new SolicitacaoMudaResponse(
                solicitacao.getId(),
                protocolo(solicitacao),
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

    public String protocolo(SolicitacaoMuda solicitacao) {
        return String.format(Locale.ROOT, "SOL-%d-%04d", Year.from(solicitacao.getCriadaEm()).getValue(), solicitacao.getId());
    }

    public SolicitacaoMudaGestaoResponse.Cartao toCartao(SolicitacaoMuda item) {
        return new SolicitacaoMudaGestaoResponse.Cartao(protocolo(item), item.getStatus(),
                item.getEspecie().getNomePopular(), item.getQuantidadeMudas(), item.getTotalRecicaveis(),
                item.getNomeSolicitante(), item.getCriadaEm());
    }

    public SolicitacaoMudaGestaoResponse toGestao(SolicitacaoMuda item) {
        String destino = item.getLogradouro() + ", " + item.getNumeroEndereco() + " · " + item.getBairro();
        if (item.getComplemento() != null) destino += " · " + item.getComplemento();
        return new SolicitacaoMudaGestaoResponse(protocolo(item), item.getStatus(),
                item.getEspecie().getNomePopular(), item.getQuantidadeMudas(), item.getTotalRecicaveis(),
                item.getNomeSolicitante(), item.getCriadaEm(), destino, item.getLocalPlantio(),
                item.getQuantidadeRecebida(), item.getMotivoRejeicao());
    }
}
