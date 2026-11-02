package br.com.arborize.dtos.response;

import br.com.arborize.enums.ClassificacaoTerritorial;
import br.com.arborize.enums.CondicaoFitossanitaria;

import java.math.BigDecimal;

/** Contrato comum de listagem e marcadores, sem os dados extensos da ficha técnica. */
public record ArvoreResumoResponseDTO(
        Long id,
        String codigo,
        EspecieIdentificacaoResponseDTO especie,
        BigDecimal latitude,
        BigDecimal longitude,
        ClassificacaoTerritorial classificacaoTerritorial,
        BairroResumoResponseDTO bairro,
        String bairroNome,
        String cep,
        String referenciaLocalizacao,
        CondicaoFitossanitaria condicaoFitossanitaria,
        FotoCapaResponseDTO fotoCapa,
        Boolean zonaRural,
        String localidadeRural
) {
    public ArvoreResumoResponseDTO(Long id, String codigo, EspecieIdentificacaoResponseDTO especie,
            BigDecimal latitude, BigDecimal longitude, ClassificacaoTerritorial classificacaoTerritorial,
            BairroResumoResponseDTO bairro, String referenciaLocalizacao,
            CondicaoFitossanitaria condicaoFitossanitaria, FotoCapaResponseDTO fotoCapa) {
        this(id, codigo, especie, latitude, longitude, classificacaoTerritorial, bairro,
                bairro == null ? null : bairro.nome(), null, referenciaLocalizacao,
                condicaoFitossanitaria, fotoCapa, null, null);
    }

    public ArvoreResumoResponseDTO(Long id, String codigo, EspecieIdentificacaoResponseDTO especie,
            BigDecimal latitude, BigDecimal longitude, ClassificacaoTerritorial classificacaoTerritorial,
            BairroResumoResponseDTO bairro, String bairroNome, String cep, String referenciaLocalizacao,
            CondicaoFitossanitaria condicaoFitossanitaria, FotoCapaResponseDTO fotoCapa) {
        this(id, codigo, especie, latitude, longitude, classificacaoTerritorial, bairro, bairroNome, cep,
                referenciaLocalizacao, condicaoFitossanitaria, fotoCapa, null, null);
    }
}
