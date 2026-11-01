package br.com.arborize.dtos.response;

import java.util.List;

public record EspecieResponseDTO(
        Long id,
        String nomePopular,
        String nomeCientifico,
        String familiaBotanica,
        String categoria,
        String porteArvore,
        String descricaoCultivo,
        Integer tempoGerminacaoDias,
        Boolean recomendadaCalcadas,
        Integer mudasDisponiveis,
        List<DocumentResponseDTO> documentos
) {
}