package br.com.arborize.dtos.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
        @NotBlank(message = "Informe seu e-mail ou CPF.")
        String login,

        @NotBlank(message = "Informe sua senha.")
        String senha
) {
}
