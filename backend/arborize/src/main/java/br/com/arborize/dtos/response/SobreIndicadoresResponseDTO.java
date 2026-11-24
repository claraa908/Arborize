package br.com.arborize.dtos.response;

public record SobreIndicadoresResponseDTO(
        long mudasDoadas,
        long estoqueViveiro,
        long garrafasPetArrecadadas,
        long ninsSubstituidos
) {
}
