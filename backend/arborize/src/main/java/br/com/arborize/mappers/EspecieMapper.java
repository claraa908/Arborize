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

    // Converte de DTO para uma nova Entidade (Usado na Criação)
    public Especie toEntity(EspecieRequestDTO dto) {
        Especie especie = new Especie();
        return mapearCampos(dto, especie);
    }

    // Atualiza uma Entidade existente com base no DTO (Usado na Edição)
    public Especie updateEntity(EspecieRequestDTO dto, Especie especieExistente) {
        return mapearCampos(dto, especieExistente);
    }

    // Método centralizado para não repetir código
    private Especie mapearCampos(EspecieRequestDTO dto, Especie especie) {
        especie.setNomePopular(dto.nomePopular());
        especie.setNomeCientifico(dto.nomeCientifico());
        especie.setFamiliaBotanica(dto.familiaBotanica());
        especie.setCategoria(dto.categoria());
        especie.setPorteArvore(dto.porteArvore());
        especie.setDescricaoCultivo(dto.descricaoCultivo());

        try {
            // Lógica para Germinação
            if (dto.tempoGerminacaoDias() != null && !dto.tempoGerminacaoDias().isBlank() && !dto.tempoGerminacaoDias().equals("null")) {
                especie.setTempoGerminacaoDias(Integer.parseInt(dto.tempoGerminacaoDias()));
            } else {
                especie.setTempoGerminacaoDias(null);
            }

            // Lógica para Calçadas
            if (dto.recomendadaCalcadas() != null && !dto.recomendadaCalcadas().isBlank() && !dto.recomendadaCalcadas().equals("null")) {
                especie.setRecomendadaCalcadas(Boolean.parseBoolean(dto.recomendadaCalcadas()));
            } else {
                especie.setRecomendadaCalcadas(null);
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