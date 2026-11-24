package br.com.arborize.dtos.response;

import br.com.arborize.entities.SolicitacaoMuda.LocalPlantio;
import br.com.arborize.enums.SolicitacaoMudaStatus;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

public record SolicitacaoMudaGestaoResponse(
        String protocolo, SolicitacaoMudaStatus status, String especie, Integer quantidadeMudas,
        Integer totalRecicaveis, String nomeSolicitante, LocalDateTime criadaEm, String destino,
        LocalPlantio localPlantio, Integer quantidadeRecebida, String motivoRejeicao
) {
    public record Cartao(
            String protocolo, SolicitacaoMudaStatus status, String especie, Integer quantidadeMudas,
            Integer totalRecicaveis, String nomeSolicitante, LocalDateTime criadaEm
    ) {}

    public record Pagina(List<Cartao> content, int page, int totalPages) {
        public static Pagina de(Page<Cartao> resultado) {
            return new Pagina(resultado.getContent(), resultado.getNumber(), resultado.getTotalPages());
        }
    }
}
