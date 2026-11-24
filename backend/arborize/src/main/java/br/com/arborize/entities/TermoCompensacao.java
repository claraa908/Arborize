package br.com.arborize.entities;

import br.com.arborize.enums.CompensacaoStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "termos_compensacao", indexes = @Index(name = "idx_termos_compensacao_razao_social", columnList = "razao_social"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TermoCompensacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "razao_social", nullable = false, length = 180)
    private String razaoSocial;

    @Column(nullable = false, length = 14)
    private String cnpj;

    @Column(name = "numero_termo", length = 60)
    private String numeroTermo;

    @Column(name = "data_assinatura")
    private LocalDate dataAssinatura;

    @Column(name = "prazo_entrega")
    private LocalDate prazoEntrega;

    @Column(name = "quantidade_acordada", nullable = false)
    private int quantidadeAcordada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private CompensacaoStatus status;

    @Column(name = "termo_objeto", nullable = false, unique = true, length = 255, updatable = false)
    private String termoObjeto;

    @Column(name = "termo_content_type", nullable = false, length = 64, updatable = false)
    private String termoContentType;

    @Column(name = "termo_nome", nullable = false, length = 255, updatable = false)
    private String termoNome;

    @Column(name = "termo_size_bytes", nullable = false, updatable = false)
    private long termoSizeBytes;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    public TermoCompensacao(String razaoSocial, String cnpj, String numeroTermo, LocalDate dataAssinatura,
                           LocalDate prazoEntrega, int quantidadeAcordada, String termoObjeto,
                           String termoContentType, String termoNome, long termoSizeBytes) {
        this.razaoSocial = Objects.requireNonNull(razaoSocial);
        this.cnpj = Objects.requireNonNull(cnpj);
        this.numeroTermo = numeroTermo;
        this.dataAssinatura = dataAssinatura;
        this.prazoEntrega = prazoEntrega;
        this.quantidadeAcordada = quantidadeAcordada;
        this.status = CompensacaoStatus.EM_ABERTO;
        this.termoObjeto = Objects.requireNonNull(termoObjeto);
        this.termoContentType = Objects.requireNonNull(termoContentType);
        this.termoNome = Objects.requireNonNull(termoNome);
        this.termoSizeBytes = termoSizeBytes;
    }

    public void atualizarDados(String razaoSocial, String cnpj, String numeroTermo, LocalDate dataAssinatura,
                               LocalDate prazoEntrega, int quantidadeAcordada) {
        this.razaoSocial = Objects.requireNonNull(razaoSocial);
        this.cnpj = Objects.requireNonNull(cnpj);
        this.numeroTermo = numeroTermo;
        this.dataAssinatura = dataAssinatura;
        this.prazoEntrega = prazoEntrega;
        this.quantidadeAcordada = quantidadeAcordada;
        atualizadoEm = LocalDateTime.now();
    }

    public void atualizarStatus(CompensacaoStatus status) {
        this.status = Objects.requireNonNull(status);
        atualizadoEm = LocalDateTime.now();
    }

    @PrePersist
    void aoCriar() {
        LocalDateTime agora = LocalDateTime.now();
        criadoEm = agora;
        atualizadoEm = agora;
    }
}
