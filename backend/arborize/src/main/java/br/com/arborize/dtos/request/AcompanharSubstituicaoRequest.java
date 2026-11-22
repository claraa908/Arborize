package br.com.arborize.dtos.request;

import jakarta.validation.constraints.NotBlank;

public record AcompanharSubstituicaoRequest(
        @NotBlank String protocolo,
        @NotBlank String cpf
) {}
