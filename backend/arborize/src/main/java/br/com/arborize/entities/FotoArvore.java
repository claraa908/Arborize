package br.com.arborize.entities;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/** Associação exclusiva do inventário, sem cascatas e sem alterar documentos dos outros módulos. */
@Entity
@Table(name = "fotos_arvores", indexes = @Index(name = "idx_fotos_arvore", columnList = "arvore_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FotoArvore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "arvore_id", nullable = false, updatable = false)
    private Arvore arvore;

    @Column(nullable = false, unique = true, updatable = false)
    private String objeto;
    @Column(nullable = false, updatable = false)
    private String contentType;
    @Column(nullable = false, updatable = false)
    private long sizeBytes;
    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    public FotoArvore(Arvore arvore, String objeto, String contentType, long sizeBytes) {
        this.arvore = java.util.Objects.requireNonNull(arvore);
        this.objeto = java.util.Objects.requireNonNull(objeto);
        this.contentType = java.util.Objects.requireNonNull(contentType);
        this.sizeBytes = sizeBytes;
    }

    @PrePersist
    void aoCriar() {
        criadoEm = LocalDateTime.now();
    }
}
