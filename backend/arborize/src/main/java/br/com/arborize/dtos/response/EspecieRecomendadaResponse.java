package br.com.arborize.dtos.response;

public record EspecieRecomendadaResponse(
        Long id,
        String nomePopular,
        String nomeCientifico,
        String porte,
        String fotoUrl
) {
}
