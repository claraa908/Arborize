package br.com.arborize.dtos.response;

import br.com.arborize.enums.ClassificacaoTerritorial;
import br.com.arborize.enums.CondicaoFitossanitaria;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ArvoreResponseDTO(
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
        LocalDate dataPlantio,
        BigDecimal dapCm,
        BigDecimal alturaEstimadaM,
        CondicaoFitossanitaria condicaoFitossanitaria,
        String observacoes,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm,
        Boolean zonaRural,
        String localidadeRural
) {
    public ArvoreResponseDTO(Long id, String codigo, EspecieIdentificacaoResponseDTO especie,
            BigDecimal latitude, BigDecimal longitude, ClassificacaoTerritorial classificacaoTerritorial,
            BairroResumoResponseDTO bairro, String referenciaLocalizacao, LocalDate dataPlantio,
            BigDecimal dapCm, BigDecimal alturaEstimadaM, CondicaoFitossanitaria condicaoFitossanitaria,
            String observacoes, LocalDateTime criadoEm, LocalDateTime atualizadoEm) {
        this(id, codigo, especie, latitude, longitude, classificacaoTerritorial, bairro,
                bairro == null ? null : bairro.nome(), null, referenciaLocalizacao, dataPlantio,
                dapCm, alturaEstimadaM, condicaoFitossanitaria, observacoes, criadoEm, atualizadoEm,
                null, null);
    }

    public ArvoreResponseDTO(Long id, String codigo, EspecieIdentificacaoResponseDTO especie,
            BigDecimal latitude, BigDecimal longitude, ClassificacaoTerritorial classificacaoTerritorial,
            BairroResumoResponseDTO bairro, String bairroNome, String cep, String referenciaLocalizacao,
            LocalDate dataPlantio, BigDecimal dapCm, BigDecimal alturaEstimadaM,
            CondicaoFitossanitaria condicaoFitossanitaria, String observacoes,
            LocalDateTime criadoEm, LocalDateTime atualizadoEm) {
        this(id, codigo, especie, latitude, longitude, classificacaoTerritorial, bairro, bairroNome, cep,
                referenciaLocalizacao, dataPlantio, dapCm, alturaEstimadaM, condicaoFitossanitaria,
                observacoes, criadoEm, atualizadoEm, null, null);
    }
}
