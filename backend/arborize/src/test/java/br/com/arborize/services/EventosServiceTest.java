package br.com.arborize.services;

import br.com.arborize.dtos.request.EventosRequestDTO;
import br.com.arborize.dtos.response.EventosResponseDTO;
import br.com.arborize.entities.DocumentEntities;
import br.com.arborize.entities.Eventos;
import br.com.arborize.mappers.EventosMapper;
import br.com.arborize.repositories.EventosRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventosServiceTest {

    @InjectMocks
    private EventosService service;

    @Mock
    private EventosRepository repository;

    @Mock
    private EventosMapper mapper;

    @Mock
    private DocumentService documentService;

    @Test
    void deveListarTodosOsEventos() {
        Eventos evento = new Eventos();
        EventosResponseDTO dto = new EventosResponseDTO(1L, "Caminhada", null, null, null, null, null, null, null, null, null, null, new ArrayList<>());

        when(repository.findAll()).thenReturn(List.of(evento));
        when(mapper.toResponseDTO(evento)).thenReturn(dto);

        List<EventosResponseDTO> resultado = service.listarTodos();

        assertFalse(resultado.isEmpty());
        assertEquals("Caminhada", resultado.get(0).nome());
        verify(repository, times(1)).findAll();
    }

    @Test
    void deveCriarEventoComBanner() {
        MockMultipartFile bannerFalso = new MockMultipartFile("banner", "banner.png", "image/png", "conteudo".getBytes());
        EventosRequestDTO requestDTO = new EventosRequestDTO("Plantio", "Desc", "2026-10-15", "2026-10-16", "UFC", "Cedro", "08:00", "12:00", "Saída", "Fim", null, bannerFalso);

        Eventos eventoMapeado = new Eventos();
        eventoMapeado.setDocuments(new ArrayList<>());

        DocumentEntities documentoSalvo = new DocumentEntities();
        EventosResponseDTO responseDTO = new EventosResponseDTO(1L, "Plantio", null, null, null, null, null, null, null, null, null, null, new ArrayList<>());

        when(mapper.toEntity(requestDTO)).thenReturn(eventoMapeado);
        when(documentService.uploadAndCreateDocument(bannerFalso)).thenReturn(documentoSalvo);
        when(repository.save(any(Eventos.class))).thenReturn(eventoMapeado);
        when(mapper.toResponseDTO(eventoMapeado)).thenReturn(responseDTO);

        EventosResponseDTO resultado = service.criarEvento(requestDTO);

        assertNotNull(resultado);
        assertEquals("Plantio", resultado.nome());
        verify(documentService, times(1)).uploadAndCreateDocument(bannerFalso);
        verify(repository, times(1)).save(eventoMapeado);
    }
}