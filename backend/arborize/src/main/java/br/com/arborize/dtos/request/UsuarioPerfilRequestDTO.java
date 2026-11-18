package br.com.arborize.dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UsuarioPerfilRequestDTO(
        @NotBlank(message = "O nome é obrigatório.")
        String nome,

        String whatsapp,

        String endereco,

        @Email(message = "Informe um e-mail válido.")
        String email
) {
    public UsuarioPerfilRequestDTO(String nome, String whatsapp, String endereco) {
        this(nome, whatsapp, endereco, null);
    }
}
