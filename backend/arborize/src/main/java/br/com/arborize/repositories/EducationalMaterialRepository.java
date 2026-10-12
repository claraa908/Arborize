package br.com.arborize.repositories;

import br.com.arborize.entities.EducationalMaterial;
import br.com.arborize.enums.MaterialCategory;
import br.com.arborize.enums.MaterialType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EducationalMaterialRepository extends JpaRepository<EducationalMaterial,Long> {
    List<EducationalMaterial> findByStatus(String status);
    List<EducationalMaterial> findByStatusAndCategory(String status, MaterialCategory category);
    List<EducationalMaterial> findByStatusAndTitleContainingIgnoreCase(String status, String title);
}
