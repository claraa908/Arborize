package br.com.arborize.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlterarSenhaRequestDTO(
        @NotBlank(message = "Informe sua senha atual.")
        String senhaAtual,

        @NotBlank(message = "Informe a nova senha.")
        @Size(min = 8, message = "A nova senha deve ter pelo menos 8 caracteres.")
        String novaSenha
) {
}
