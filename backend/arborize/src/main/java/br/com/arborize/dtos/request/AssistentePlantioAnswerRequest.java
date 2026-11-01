package br.com.arborize.dtos.request;

import jakarta.validation.constraints.NotBlank;

public record AssistentePlantioAnswerRequest(
        @NotBlank String perguntaId,
        @NotBlank String valor
) {
}
