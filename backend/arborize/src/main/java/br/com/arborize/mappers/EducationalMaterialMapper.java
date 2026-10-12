package br.com.arborize.mappers;

import br.com.arborize.dtos.request.EducationalMaterialRequestDTO;
import br.com.arborize.dtos.response.DocumentResponseDTO;
import br.com.arborize.dtos.response.EducationalMaterialResponseDTO;
import br.com.arborize.entities.EducationalMaterialEntities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class EducationalMaterialMapper {

    @Autowired
    private DocumentMapper documentMapper;

    // Converte o RequestDTO de entrada para a Entidade EducationalMaterialEntities
    public EducationalMaterialEntities toEntity(EducationalMaterialRequestDTO dto) {
        return EducationalMaterialEntities.builder()
                .title(dto.title())
                .type(dto.type())
                .category(dto.category())
                .description(dto.description())
                .status(dto.status())
                .build();
    }

    // Converte a Entidade do Banco (contendo a lista de documentos) para o ResponseDTO
    public EducationalMaterialResponseDTO toResponseDTO(EducationalMaterialEntities entity) {
        List<DocumentResponseDTO> documentDTOs = (entity.getDocuments() != null) ?
                entity.getDocuments().stream()
                        .map(documentMapper::toResponseDTO)
                        .collect(Collectors.toList())
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