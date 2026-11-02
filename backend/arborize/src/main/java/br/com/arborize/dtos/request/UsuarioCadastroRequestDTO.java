package br.com.arborize.dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UsuarioCadastroRequestDTO(
        @NotBlank(message = "O nome é obrigatório.")
        String nome,

        @NotBlank(message = "O CPF é obrigatório.")
        @Pattern(regexp = "\\d{11}", message = "O CPF deve conter 11 dígitos.")
        String cpf,

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "Informe um e-mail válido.")
        String email,

        @NotBlank(message = "O WhatsApp é obrigatório.")
        String whatsapp,

        @NotBlank(message = "O endereço é obrigatório.")
        String endereco,

        @NotBlank(message = "A senha é obrigatória.")
        String senha
) {
}
