package br.com.arborize.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "metas_zoneamento", uniqueConstraints = {
        @UniqueConstraint(name = "uk_meta_zoneamento_periodo_bairro", columnNames = {"ano", "trimestre", "zona", "bairro"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetaZoneamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotBlank
    private String zona;

    @Column(nullable = false)
    @NotBlank
    private String bairro;

    @Column(nullable = false)
    @NotNull
    @Positive
    private Integer ano;

    @Column(nullable = false)
    @NotNull
    @Min(1)
    @Max(4)
    private Integer trimestre;

    @Column(name = "planting_target", nullable = false)
    @NotNull
    @Min(0)
    private Integer plantingTarget;
}
