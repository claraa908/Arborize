package br.com.arborize.repositories;

import br.com.arborize.entities.EducationalMaterialEntities;
import br.com.arborize.enums.MaterialCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EducationalMaterialRepository extends JpaRepository<EducationalMaterialEntities,Long> {
    List<EducationalMaterialEntities> findAllByOrderByIdDesc();
    List<EducationalMaterialEntities> findByStatus(String status);
    List<EducationalMaterialEntities> findByStatusAndTitleContainingIgnoreCase(String status, String title);
    List<EducationalMaterialEntities> findByStatusAndCategory(String status, MaterialCategory category);
}
