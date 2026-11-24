package br.com.arborize.entities;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "entregas_compensacao", indexes = @Index(name = "idx_entregas_compensacao_termo_criado", columnList = "termo_id, criado_em, id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EntregaCompensacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "termo_id", nullable = false, updatable = false)
    private TermoCompensacao termo;

    @Column(nullable = false, updatable = false)
    private int quantidade;

    @Column(name = "recibo_objeto", nullable = false, unique = true, length = 255, updatable = false)
    private String reciboObjeto;

    @Column(name = "recibo_nome", nullable = false, length = 255, updatable = false)
    private String reciboNome;

    @Column(name = "recibo_content_type", nullable = false, length = 64, updatable = false)
    private String reciboContentType;

    @Column(name = "recibo_size_bytes", nullable = false, updatable = false)
    private long reciboSizeBytes;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    public EntregaCompensacao(TermoCompensacao termo, int quantidade, String reciboObjeto, String reciboNome,
                              String reciboContentType, long reciboSizeBytes) {
        this.termo = Objects.requireNonNull(termo);
        this.quantidade = quantidade;
        this.reciboObjeto = Objects.requireNonNull(reciboObjeto);
        this.reciboNome = Objects.requireNonNull(reciboNome);
        this.reciboContentType = Objects.requireNonNull(reciboContentType);
        this.reciboSizeBytes = reciboSizeBytes;
    }

    @PrePersist
    void aoCriar() {
        criadoEm = LocalDateTime.now();
    }
}
