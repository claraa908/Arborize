package br.com.arborize.dtos.response;

import java.util.List;

public record MovimentacaoMudaPaginaResponseDTO(
        List<MovimentacaoMudaResponseDTO> content,
        int number,
        int size,
        long totalElements,
        int totalPages
) { }
