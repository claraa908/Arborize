package br.com.arborize.dtos.response;

public record UsuarioResponseDTO(
        Long id,
        String nome,
        String cpf,
        String email,
        String whatsapp,
        String endereco
) {
}
