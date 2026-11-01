package br.com.arborize.dtos.request;

import jakarta.validation.constraints.Min;
import org.springframework.web.multipart.MultipartFile;

public record EspecieRequestDTO(
        String nomePopular,
        String nomeCientifico,
        String familiaBotanica,
        String categoria,
        String porteArvore,
        String descricaoCultivo,
        String tempoGerminacaoDias, // Mantemos String no request para evitar erros de formatação
        String recomendadaCalcadas, // Mantemos String (ex: "true" ou "false")
        @Min(0) Integer mudasDisponiveis,
        MultipartFile foto // O ficheiro da imagem
) {
}