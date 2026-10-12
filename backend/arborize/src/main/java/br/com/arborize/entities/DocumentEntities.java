package br.com.arborize.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "document")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentEntities {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "educational_material_id")
    private EducationalMaterialEntities educationalMaterialEntities;

    // (Futuramente você poderá adicionar outros relacionamentos aqui, ex: technicalReport, treeSpecies, etc.)

    @Column(name = "original_filename", nullable = false)
    private String originalFilename; // Nome original do arquivo enviado pelo usuário

    @Column(name = "file_path", nullable = false)
    private String filePath; // Nome único gerado no MinIO (UUID + nome)

    @Column(name = "content_type")
    private String contentType; // Ex: application/pdf, image/jpeg

    private String extension; // Ex: .pdf, .png

    @Column(name = "size_bytes")
    private Long sizeBytes; // Tamanho em bytes

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
