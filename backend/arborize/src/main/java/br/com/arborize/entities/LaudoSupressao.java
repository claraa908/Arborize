package br.com.arborize.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "laudos_supressao", uniqueConstraints = @UniqueConstraint(
        name = "uk_laudo_supressao_solicitacao", columnNames = "solicitacao_id"))
@Getter
@Setter
@NoArgsConstructor
public class LaudoSupressao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitacao_id", nullable = false)
    private SolicitacaoSubstituicao solicitacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "especie_reposicao_id", nullable = false)
    private Especie especieReposicao;

    @Column(nullable = false, length = 80)
    private String parecer;

    @Column(name = "tipo_justificativa", nullable = false, length = 80)
    private String tipoJustificativa;

    @Column(nullable = false, columnDefinition = "text")
    private String justificativa;

    @Column(name = "data_vistoria", nullable = false)
    private LocalDate dataVistoria;

    @Column(name = "pdf_objeto", nullable = false, length = 512)
    private String pdfObjeto;

    @Column(name = "pdf_nome_original", nullable = false, length = 255)
    private String pdfNomeOriginal;

    @Column(name = "pdf_content_type", nullable = false, length = 100)
    private String pdfContentType;

    @Column(name = "pdf_tamanho_bytes", nullable = false)
    private long pdfTamanhoBytes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "emitido_por_id", nullable = false)
    private Usuario emitidoPor;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;
}
