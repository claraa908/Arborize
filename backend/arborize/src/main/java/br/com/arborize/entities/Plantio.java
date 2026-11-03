package br.com.arborize.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "plantios", indexes = {
        @Index(name = "idx_plantio_zone_date_confirmed", columnList = "zona, bairro, occurred_at, confirmed")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Plantio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotBlank
    private String zona;

    @Column(nullable = false)
    @NotBlank
    private String bairro;

    @Column(name = "species_name", nullable = false)
    @NotBlank
    private String speciesName;

    @Column(nullable = false)
    @NotNull
    @Positive
    private Integer quantity;

    @Column(nullable = false)
    private boolean confirmed;

    @Column(name = "occurred_at", nullable = false)
    @NotNull
    private LocalDate occurredAt;
}
