package br.com.arborize.mappers;

import br.com.arborize.dtos.request.EducationalMaterialRequestDTO;
import br.com.arborize.dtos.response.EducationalMaterialResponseDTO;
import br.com.arborize.entities.EducationalMaterial;
import org.springframework.stereotype.Component;

@Component
public class EducationalMaterialMapper {
    public EducationalMaterial toEntity(EducationalMaterialRequestDTO dto, String filePath, Double fileSize) {
        return EducationalMaterial.builder()
                .title(dto.title())
                .type(dto.type())
                .category(dto.category())
                .description(dto.description())
                .status(dto.status())
                .filePath(filePath)
                .fileSize(fileSize)
                .build();
    }

    public EducationalMaterialResponseDTO toResponseDTO(EducationalMaterial entity) {
        return new EducationalMaterialResponseDTO(
                entity.getId(),
                entity.getTitle(),
                entity.getType(),
                entity.getCategory(),
                entity.getDescription(),
                entity.getFilePath(),
                entity.getFileSize(),
                entity.getUploadDate(),
                entity.getStatus()
        );
    }
}
