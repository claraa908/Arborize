package br.com.arborize.dtos.response;

import java.util.List;

public record DashboardOverviewResponse(
        long mudasDoadas,
        long mudasPlantadas,
        long estoqueAtual,
        long garrafasPetArrecadadas,
        long substituicoesNim,
        List<DashboardMonthlyMetricResponse> evolucao,
        List<DashboardSpeciesStockResponse> estoquePorEspecie
) {
}
