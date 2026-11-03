package br.com.arborize.dtos.response;

import java.util.List;

public record DashboardGoalsResponse(
        int ano,
        int trimestre,
        String zona,
        List<String> zonasDisponiveis,
        List<Integer> anosDisponiveis,
        List<DashboardGoalResponse> metas
) {
}
