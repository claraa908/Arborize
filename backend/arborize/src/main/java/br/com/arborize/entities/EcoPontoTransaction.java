package br.com.arborize.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "eco_ponto_transactions", indexes = {
        @Index(name = "idx_eco_ponto_transaction_date_confirmed", columnList = "transaction_date, confirmed")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EcoPontoTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bottles_collected", nullable = false)
    @NotNull
    @Positive
    private Integer bottlesCollected;

    @Column(nullable = false)
    private boolean confirmed;

    @Column(name = "transaction_date", nullable = false)
    @NotNull
    private LocalDate transactionDate;
}
