package br.com.arborize.dtos.request;

import br.com.arborize.enums.MaterialCategory;
import br.com.arborize.enums.MaterialType;
import org.springframework.web.multipart.MultipartFile;

public record EducationalMaterialRequestDTO(
        String title,
        MaterialType type,
        MaterialCategory category,
        String description,
        String status,
        MultipartFile coverPhoto,
        MultipartFile file
) {
}
