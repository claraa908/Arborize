package br.com.arborize.dtos.request;

import jakarta.validation.constraints.NotBlank;

public record UsuarioPerfilRequestDTO(
        @NotBlank(message = "O nome é obrigatório.")
        String nome,

        @NotBlank(message = "O WhatsApp é obrigatório.")
        String whatsapp,

        @NotBlank(message = "O endereço é obrigatório.")
        String endereco
) {
}
