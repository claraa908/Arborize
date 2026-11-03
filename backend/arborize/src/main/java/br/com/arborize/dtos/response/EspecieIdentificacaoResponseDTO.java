package br.com.arborize.dtos.response;

public record EspecieIdentificacaoResponseDTO(
        Long id,
        String nomePopular,
        String nomeCientifico,
        String familiaBotanica
) {
}
