package br.com.arborize.dtos.response;

public record IfceBolsistaResponseDTO(
        Long id,
        String nome,
        String email,
        String whatsapp
) {
}
