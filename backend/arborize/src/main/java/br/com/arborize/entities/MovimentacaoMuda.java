package br.com.arborize.entities;

import br.com.arborize.enums.TipoMovimentacaoMuda;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "movimentacoes_mudas", indexes = {
        @Index(name = "idx_movimentacao_data", columnList = "data"),
        @Index(name = "idx_movimentacao_especie", columnList = "especie_id"),
        @Index(name = "idx_movimentacao_tipo", columnList = "tipo")
})
@Getter
@Setter
@NoArgsConstructor
public class MovimentacaoMuda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TipoMovimentacaoMuda tipo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "especie_id", nullable = false)
    private Especie especie;

    @Column(nullable = false)
    @Positive
    private Integer quantidade;

    @Column(name = "data", nullable = false)
    private LocalDate data;

    @Column(length = 180)
    private String destino;

    @Column(length = 160)
    private String responsavel;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comprovante_id", unique = true)
    private DocumentEntities comprovante;
}
