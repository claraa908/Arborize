package br.com.arborize.entities;

import br.com.arborize.enums.MaterialCategory;
import br.com.arborize.enums.MaterialType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "educational_materials")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EducationalMaterialEntities {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "material_type", nullable = false)
    private MaterialType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaterialCategory category;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "upload_date")
    private LocalDate uploadDate;

    @Column(nullable = false)
    private String status;

    @OneToMany(mappedBy = "educationalMaterial", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DocumentEntities> documents;

    @PrePersist
    public void prePersist() {
        this.uploadDate = LocalDate.now();
    }

    public List<DocumentEntities> getDocument() {
        return documents;
    }
}
