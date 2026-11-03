package br.com.arborize.dtos.response;

public record DashboardGoalResponse(
        String bairro,
        String zona,
        long metaPlantio,
        long realizado,
        long saldoLiquido,
        double deficitPercentual,
        String prioridade
) {
}
