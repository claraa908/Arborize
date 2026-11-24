package br.com.arborize.dtos.request;

import br.com.arborize.enums.CompensacaoStatus;
import jakarta.validation.constraints.NotNull;

public record CompensacaoStatusRequestDTO(@NotNull CompensacaoStatus status) { }
