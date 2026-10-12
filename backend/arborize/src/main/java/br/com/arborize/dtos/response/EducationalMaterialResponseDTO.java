package br.com.arborize.dtos.response;

import br.com.arborize.enums.MaterialCategory;
import br.com.arborize.enums.MaterialType;

import java.time.LocalDate;

public record EducationalMaterialResponseDTO(
        Long id,
        String title,
        MaterialType type,
        MaterialCategory category,
        String description,
        String filePath,
        Double fileSize,
        LocalDate uploadDate,
        String status
) {
}
