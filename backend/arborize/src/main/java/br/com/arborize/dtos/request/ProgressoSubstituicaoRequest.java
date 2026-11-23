package br.com.arborize.dtos.request;

import br.com.arborize.enums.SubstituicaoBotanicaStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProgressoSubstituicaoRequest(
        @NotNull SubstituicaoBotanicaStatus status,
        @NotBlank @Size(max = 500) String descricao
) {}
