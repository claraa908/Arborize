package br.com.arborize.dtos.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import br.com.arborize.enums.SubstituicaoBotanicaStatus;

public record DecisaoSubstituicaoRequest(
        @NotNull SubstituicaoBotanicaStatus status,
        @Size(max = 500) String justificativa
) {}
