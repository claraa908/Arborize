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
        LocalDate uploadDate,
        String status,
        List<DocumentResponseDTO> documents
) {
}
