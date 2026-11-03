package br.com.arborize.dtos.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Set;

public record DashboardReportRequest(
        @NotNull LocalDate dataInicial,
        @NotNull LocalDate dataFinal,
        @NotEmpty Set<@NotNull DashboardReportDataset> dados,
        @NotNull DashboardReportFormat formato
) {

    @AssertTrue(message = "A data final não pode ser anterior à data inicial.")
    public boolean isPeriodoValido() {
        return dataInicial == null || dataFinal == null || !dataFinal.isBefore(dataInicial);
    }
}
