package br.com.arborize.mappers;

import br.com.arborize.dtos.response.DocumentResponseDTO;
import br.com.arborize.entities.DocumentEntities;
import org.springframework.stereotype.Component;

@Component
public class DocumentMapper {

    public DocumentResponseDTO toResponseDTO(DocumentEntities entity) {
        if (entity == null) {
            return null;
        }
        return new DocumentResponseDTO(
                entity.getId(),
                entity.getOriginalFilename(),
                entity.getContentType(),
                entity.getSizeBytes(),
                entity.getExtension()
        );
    }
}