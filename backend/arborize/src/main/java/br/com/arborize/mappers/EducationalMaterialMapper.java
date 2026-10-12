package br.com.arborize.mappers;

import br.com.arborize.dtos.request.EducationalMaterialRequestDTO;
import br.com.arborize.dtos.response.EducationalMaterialResponseDTO;
import br.com.arborize.entities.EducationalMaterial;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class EducationalMaterialMapper {
    public EducationalMaterial toEntity(EducationalMaterialRequestDTO dto, String filePath, Double fileSize) {
        return EducationalMaterial.builder()
                .title(dto.title())
                .type(dto.type())
                .category(dto.category())
                .description(dto.description())
                .status(dto.status())
                .build();
    }

    public EducationalMaterialResponseDTO toResponseDTO(EducationalMaterial entity) {
        List<DocumentResponseDTO> documentDTOs = entity.getDocuments() != null) ?
            entity.getDocuments().strem()
                    .map(doc -> new DocumentResponseDTO(
                            doc.getId(),
                            doc.getOriginalFilename(),
                            doc.getSizeBytes(),
                            doc.getExtension()
                    ))
                    .collect(Colectors.toList())
            : Collections.emptyList();

        return new EducationalMaterialResponseDTO(
                entity.getId(),
                entity.getTitle(),
                entity.getType(),
                entity.getCategory(),
                entity.getDescription(),
                entity.getUploadDate(),
                entity.getStatus(),
                documentDTOs
        );
    }
}
