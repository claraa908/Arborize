package br.com.arborize.entities;

import br.com.arborize.enums.CondicaoFitossanitaria;
import br.com.arborize.enums.ClassificacaoTerritorial;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "arvores", indexes = @Index(name = "idx_arvores_bairro", columnList = "bairro_id"))
@Getter
@Setter
@NoArgsConstructor
public class Arvore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "especie_id", nullable = false)
    private Especie especie;

    // Sem cascata: excluir um bairro referenciado deve ser impedido pela chave estrangeira.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bairro_id")
    private Bairro bairro;

    @Column(name = "bairro_nome", columnDefinition = "TEXT")
    private String bairroNome;

    @Column(name = "cep", columnDefinition = "TEXT")
    private String cep;

    @Column(name = "localidade_rural", columnDefinition = "TEXT")
    private String localidadeRural;

    @Enumerated(EnumType.STRING)
    @Column(name = "classificacao_territorial", nullable = false)
    private ClassificacaoTerritorial classificacaoTerritorial = ClassificacaoTerritorial.BAIRRO_NAO_IDENTIFICADO;

    @Column(name = "referencia_localizacao", columnDefinition = "TEXT")
    private String referenciaLocalizacao;

    @Column(nullable = false, columnDefinition = "NUMERIC")
    private BigDecimal latitude;

    @Column(nullable = false, columnDefinition = "NUMERIC")
    private BigDecimal longitude;

    @Column(name = "data_plantio")
    private LocalDate dataPlantio;

    @Column(name = "dap_cm", columnDefinition = "NUMERIC")
    private BigDecimal dapCm;

    @Column(name = "altura_estimada_m", columnDefinition = "NUMERIC")
    private BigDecimal alturaEstimadaM;

    @Enumerated(EnumType.STRING)
    @Column(name = "condicao_fitossanitaria", nullable = false)
    private CondicaoFitossanitaria condicaoFitossanitaria = CondicaoFitossanitaria.NAO_AVALIADA;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @Column(name = "criado_em", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    @Setter(AccessLevel.NONE)
    private LocalDateTime atualizadoEm;

    @PrePersist
    void aoCriar() {
        validarRepresentacaoTerritorial();
        LocalDateTime agora = LocalDateTime.now();
        criadoEm = agora;
        atualizadoEm = agora;
        if (condicaoFitossanitaria == null) {
            condicaoFitossanitaria = CondicaoFitossanitaria.NAO_AVALIADA;
        }
    }

    @PreUpdate
    void aoAtualizar() {
        validarRepresentacaoTerritorial();
        atualizadoEm = LocalDateTime.now();
    }

    private void validarRepresentacaoTerritorial() {
        if (classificacaoTerritorial == null) {
            throw new IllegalStateException("A classificação territorial deve ser informada.");
        }
        boolean bairroTextualInformado = bairroNome != null && !bairroNome.isBlank();
        if (classificacaoTerritorial == ClassificacaoTerritorial.BAIRRO_IDENTIFICADO) {
            if (bairro == null && !bairroTextualInformado) {
                throw new IllegalStateException("Bairro identificado exige um nome confirmado.");
            }
        } else if (bairro != null || bairroTextualInformado) {
            throw new IllegalStateException("Informe um bairro somente na situação de bairro identificado.");
        }
    }
}
