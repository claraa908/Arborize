package br.com.arborize.entities;

import br.com.arborize.enums.SubstituicaoBotanicaStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "eventos_solicitacao_substituicao")
@Getter
@Setter
@NoArgsConstructor
public class SolicitacaoSubstituicaoEvento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitacao_id", nullable = false)
    private SolicitacaoSubstituicao solicitacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private SubstituicaoBotanicaStatus status;

    @Column(nullable = false, length = 500)
    private String descricao;

    @Column(nullable = false)
    private boolean publico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsavel_id")
    private Usuario responsavel;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;
}
