package br.com.arborize.mappers;

import br.com.arborize.dtos.request.EspecieRequestDTO;
import br.com.arborize.dtos.response.DocumentResponseDTO;
import br.com.arborize.dtos.response.EspecieResponseDTO;
import br.com.arborize.entities.Especie;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class EspecieMapper {

    public Especie toEntity(EspecieRequestDTO dto) {
        Especie especie = new Especie();
        especie.setNomePopular(dto.nomePopular());
        especie.setNomeCientifico(dto.nomeCientifico());
        especie.setFamiliaBotanica(dto.familiaBotanica());
        especie.setCategoria(dto.categoria());
        especie.setPorteArvore(dto.porteArvore());
        especie.setDescricaoCultivo(dto.descricaoCultivo());

        try {
            if (dto.tempoGerminacaoDias() != null && !dto.tempoGerminacaoDias().isBlank()) {
                especie.setTempoGerminacaoDias(Integer.parseInt(dto.tempoGerminacaoDias()));
            }
            if (dto.recomendadaCalcadas() != null && !dto.recomendadaCalcadas().isBlank()) {
                especie.setRecomendadaCalcadas(Boolean.parseBoolean(dto.recomendadaCalcadas()));
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Erro ao processar números ou opções da espécie.");
        }

        return especie;
    }

    public EspecieResponseDTO toResponseDTO(Especie entity) {
        List<DocumentResponseDTO> documentDTOs = entity.getDocuments() != null ?
                entity.getDocuments().stream()
                        .map(doc -> new DocumentResponseDTO(
                                doc.getId(),
                                doc.getOriginalFilename(),
                                doc.getContentType(),
                                doc.getSizeBytes(),
                                doc.getExtension()
                        ))
                        .collect(Collectors.toList())
                : Collections.emptyList();

        return new EspecieResponseDTO(
                entity.getId(),
                entity.getNomePopular(),
                entity.getNomeCientifico(),
                entity.getFamiliaBotanica(),
                entity.getCategoria(),
                entity.getPorteArvore(),
                entity.getDescricaoCultivo(),
                entity.getTempoGerminacaoDias(),
                entity.getRecomendadaCalcadas(),
                documentDTOs
        );
    }
}