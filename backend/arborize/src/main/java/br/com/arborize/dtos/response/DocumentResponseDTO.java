package br.com.arborize.dtos.response;

public record DocumentResponseDTO(
        Long id,
        String originalFilename,
        String contentType,
        Long sizeBytes,
        String extension
) {
}
