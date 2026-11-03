package br.com.arborize.dtos.response;

public record DashboardMonthlyMetricResponse(
        String mes,
        long mudasDoadas,
        long mudasPlantadas
) {
}
