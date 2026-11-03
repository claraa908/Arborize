package br.com.arborize.dtos.response;

import java.time.LocalDateTime;

public record SolicitacaoMudaResponse(
        Long id,
        String protocolo,
        String status,
        EspecieResumoResponse especie,
        Integer quantidadeMudas,
        Integer totalRecicaveis,
        String nomeSolicitante,
        LocalDateTime criadaEm
) {
    public record EspecieResumoResponse(Long id, String nomePopular) {
    }
}
