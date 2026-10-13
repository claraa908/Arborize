package br.com.arborize.mappers;

import br.com.arborize.dtos.request.EventosRequestDTO;
import br.com.arborize.dtos.response.DocumentResponseDTO;
import br.com.arborize.dtos.response.EventosResponseDTO;
import br.com.arborize.entities.Eventos;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class EventosMapper {

    // Converte o que vem do Front-end (String) para a Entidade do Banco (LocalDate/LocalTime)
    public Eventos toEntity(EventosRequestDTO dto) {
        Eventos evento = new Eventos();
        evento.setNome(dto.nome());
        evento.setDescricao(dto.descricao());
        evento.setLocalConcentracao(dto.localConcentracao());
        evento.setDestinoFinal(dto.destinoFinal());
        evento.setDescricaoPontoPartida(dto.descricaoPontoPartida());
        evento.setDescricaoEncerramento(dto.descricaoEncerramento());

        try {
            if (dto.dataInicio() != null && !dto.dataInicio().isBlank() && !dto.dataInicio().equals("string")) {
                evento.setDataInicio(LocalDate.parse(dto.dataInicio()));
            }
            if (dto.dataTermino() != null && !dto.dataTermino().isBlank() && !dto.dataTermino().equals("string")) {
                evento.setDataTermino(LocalDate.parse(dto.dataTermino()));
            }

            if (dto.horarioInicio() != null && !dto.horarioInicio().isBlank() && !dto.horarioInicio().equals("string")) {
                // O Vue envia "HH:mm", mas o parse exige segundos em algumas versões. O safeParse lida com isso.
                evento.setHorarioInicio(LocalTime.parse(dto.horarioInicio().length() == 5 ? dto.horarioInicio() + ":00" : dto.horarioInicio()));
            }
            if (dto.horarioTermino() != null && !dto.horarioTermino().isBlank() && !dto.horarioTermino().equals("string")) {
                evento.setHorarioTermino(LocalTime.parse(dto.horarioTermino().length() == 5 ? dto.horarioTermino() + ":00" : dto.horarioTermino()));
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Erro ao converter data ou hora. Verifique os formatos enviados: " + e.getMessage());
        }

        return evento;
    }

    // Converte a Entidade salva (LocalDate/LocalTime) para enviar de volta ao Front-end (String)
    public EventosResponseDTO toResponseDTO(Eventos entity) {
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

        return new EventosResponseDTO(
                entity.getId(),
                entity.getNome(),
                entity.getDescricao(),
                // Converte de volta para String ao enviar para o Vue.js
                entity.getDataInicio() != null ? entity.getDataInicio().toString() : null,
                entity.getDataTermino() != null ? entity.getDataTermino().toString() : null,
                entity.getLocalConcentracao(),
                entity.getDestinoFinal(),
                entity.getHorarioInicio() != null ? entity.getHorarioInicio().toString() : null,
                entity.getHorarioTermino() != null ? entity.getHorarioTermino().toString() : null,
                entity.getDescricaoPontoPartida(),
                entity.getDescricaoEncerramento(),
                documentDTOs
        );
    }
}