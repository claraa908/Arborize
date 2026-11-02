package br.com.arborize.dtos.request;

import br.com.arborize.enums.ClassificacaoTerritorial;
import br.com.arborize.enums.CondicaoFitossanitaria;
import br.com.arborize.validation.TerritorioConsistente;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Representação completa para PUT: nulos em campos opcionais significam limpeza. */
@TerritorioConsistente
public record ArvoreAtualizacaoRequestDTO(
        @NotNull(message = "A espécie deve ser informada.")
        @Positive(message = "O identificador da espécie deve ser positivo.") Long especieId,
        @NotNull(message = "A latitude deve ser informada.")
        @DecimalMin(value = "-90", message = "A latitude deve ser maior ou igual a -90.")
        @DecimalMax(value = "90", message = "A latitude deve ser menor ou igual a 90.") BigDecimal latitude,
        @NotNull(message = "A longitude deve ser informada.")
        @DecimalMin(value = "-180", message = "A longitude deve ser maior ou igual a -180.")
        @DecimalMax(value = "180", message = "A longitude deve ser menor ou igual a 180.") BigDecimal longitude,
        ClassificacaoTerritorial classificacaoTerritorial,
        @Positive(message = "O identificador do bairro deve ser positivo.") Long bairroId,
        String bairroNome,
        String cep,
        String referenciaLocalizacao,
        LocalDate dataPlantio,
        @Positive(message = "O DAP deve ser positivo.") BigDecimal dapCm,
        @Positive(message = "A altura estimada deve ser positiva.") BigDecimal alturaEstimadaM,
        @NotNull(message = "A condição fitossanitária deve ser informada.") CondicaoFitossanitaria condicaoFitossanitaria,
        String observacoes,
        Boolean zonaRural,
        String localidadeRural
) implements DadosArvoreRequest {
    public ArvoreAtualizacaoRequestDTO {
        bairroNome = normalizar(bairroNome);
        cep = normalizar(cep);
        localidadeRural = normalizar(localidadeRural);
    }

    public ArvoreAtualizacaoRequestDTO(Long especieId, BigDecimal latitude, BigDecimal longitude,
            ClassificacaoTerritorial classificacaoTerritorial, Long bairroId, String referenciaLocalizacao,
            LocalDate dataPlantio, BigDecimal dapCm, BigDecimal alturaEstimadaM,
            CondicaoFitossanitaria condicaoFitossanitaria, String observacoes) {
        this(especieId, latitude, longitude, classificacaoTerritorial, bairroId, null, null,
                referenciaLocalizacao, dataPlantio, dapCm, alturaEstimadaM,
                condicaoFitossanitaria, observacoes, null, null);
    }

    public ArvoreAtualizacaoRequestDTO(Long especieId, BigDecimal latitude, BigDecimal longitude,
            ClassificacaoTerritorial classificacaoTerritorial, Long bairroId, String bairroNome,
            String cep, String referenciaLocalizacao, LocalDate dataPlantio, BigDecimal dapCm,
            BigDecimal alturaEstimadaM, CondicaoFitossanitaria condicaoFitossanitaria,
            String observacoes) {
        this(especieId, latitude, longitude, classificacaoTerritorial, bairroId, bairroNome, cep,
                referenciaLocalizacao, dataPlantio, dapCm, alturaEstimadaM,
                condicaoFitossanitaria, observacoes, null, null);
    }

    private static String normalizar(String valor) {
        if (valor == null) return null;
        String normalizado = valor.strip();
        return normalizado.isEmpty() ? null : normalizado;
    }
}
