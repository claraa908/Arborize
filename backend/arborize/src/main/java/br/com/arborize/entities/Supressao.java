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
@Table(name = "supressoes", indexes = {
        @Index(name = "idx_supressao_zone_date_authorized", columnList = "zona, bairro, occurred_at, authorized")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Supressao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotBlank
    private String zona;

    @Column(nullable = false)
    @NotBlank
    private String bairro;

    @Column(name = "removed_species", nullable = false)
    @NotBlank
    private String removedSpecies;

    @Column(name = "replacement_species")
    private String replacementSpecies;

    @Column(nullable = false)
    @NotNull
    @Positive
    private Integer quantity;

    @Column(nullable = false)
    private boolean authorized;

    @Column(name = "occurred_at", nullable = false)
    @NotNull
    private LocalDate occurredAt;
}
