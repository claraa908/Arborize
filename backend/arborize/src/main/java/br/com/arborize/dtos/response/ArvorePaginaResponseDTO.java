package br.com.arborize.dtos.response;

import java.util.List;

/** Contrato HTTP explícito, independente da serialização interna de PageImpl. */
public record ArvorePaginaResponseDTO(
        List<ArvoreResumoResponseDTO> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public ArvorePaginaResponseDTO {
        content = List.copyOf(content);
    }
}
