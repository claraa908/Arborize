package br.com.arborize.services;

import br.com.arborize.dtos.request.EventosRequestDTO;
import br.com.arborize.dtos.response.EventosResponseDTO;
import br.com.arborize.entities.DocumentEntities; // Verifique se a sua classe filha se chama Documento ou DocumentEntities
import br.com.arborize.entities.Eventos;
import br.com.arborize.mappers.EventosMapper;
import br.com.arborize.repositories.EventosRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventosService {

    @Autowired
    private EventosRepository repository;

    @Autowired
    private EventosMapper mapper;

    @Autowired
    private DocumentService documentService;

    @Transactional
    public EventosResponseDTO criarEvento(EventosRequestDTO dto) {
        // 1. Converte o DTO para Entidade
        Eventos evento = mapper.toEntity(dto);

        // 2. Lida com o upload do banner (Imagem)
        if (dto.banner() != null && !dto.banner().isEmpty()) {
            // Usa o mesmo serviço de documentos que usou nos Materiais Educativos
            DocumentEntities bannerDoc = documentService.uploadAndCreateDocument(dto.banner());
            bannerDoc.setEventos(evento);
            evento.getDocuments().add(bannerDoc);
        }

        // 3. Salva no banco (O CascadeType.ALL vai salvar o Documento junto automaticamente)
        Eventos eventoSalvo = repository.save(evento);

        // 4. Converte o resultado de volta para DTO e retorna
        return mapper.toResponseDTO(eventoSalvo);
    }

    @Transactional(readOnly = true)
    public List<EventosResponseDTO> listarTodos() {
        return repository.findAll().stream()
                .map(mapper::toResponseDTO)
                .collect(Collectors.toList());
    }
}