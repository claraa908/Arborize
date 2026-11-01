package br.com.arborize.dtos.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AssistentePlantioRequest(
        @NotNull
        @NotEmpty
        List<@NotNull @Valid AssistentePlantioAnswerRequest> respostas
) {
}
