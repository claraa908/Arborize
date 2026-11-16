package br.com.arborize.services;

import br.com.arborize.dtos.request.EventosRequestDTO;
import br.com.arborize.dtos.response.EventosResponseDTO;
import br.com.arborize.entities.DocumentEntities;
import br.com.arborize.entities.Eventos;
import br.com.arborize.mappers.EventosMapper;
import br.com.arborize.repositories.EventosRepository;
import io.minio.MinioClient;
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

    @Mock
    private MinioClient minioClient;

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

    @Test
    void deveCriarEventoSemBannerSemInvocarArmazenamentoDeArquivos() {
        EventosRequestDTO request = new EventosRequestDTO(
                "Plantio comunitário", "Descrição", null, null, "UFC", null,
                null, null, null, null, null, null
        );
        Eventos evento = new Eventos();
        EventosResponseDTO response = new EventosResponseDTO(
                2L, "Plantio comunitário", null, null, null, null, null, null, null, null, null, null, new ArrayList<>()
        );
        when(mapper.toEntity(request)).thenReturn(evento);
        when(repository.save(evento)).thenReturn(evento);
        when(mapper.toResponseDTO(evento)).thenReturn(response);

        assertEquals(response, service.criarEvento(request));

        verify(documentService, never()).uploadAndCreateDocument(any());
        verify(repository).save(evento);
    }

    @Test
    void deveMapearEventoEncontradoPorId() {
        Eventos evento = new Eventos();
        EventosResponseDTO response = new EventosResponseDTO(
                3L, "Mutirão", null, null, null, null, null, null, null, null, null, null, new ArrayList<>()
        );
        when(repository.findById(3L)).thenReturn(java.util.Optional.of(evento));
        when(mapper.toResponseDTO(evento)).thenReturn(response);

        assertEquals(response, service.buscarPorId(3L));

        verify(mapper).toResponseDTO(evento);
    }

    @Test
    void deveRejeitarBuscaDeEventoInexistente() {
        when(repository.findById(404L)).thenReturn(java.util.Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.buscarPorId(404L));

        assertTrue(exception.getMessage().contains("404"));
        verify(mapper, never()).toResponseDTO(any(Eventos.class));
    }

    @Test
    void deveRecuperarBannerPelaChaveAssociadaAoEvento() {
        DocumentEntities banner = new DocumentEntities();
        banner.setFilePath("eventos/banner-teste");
        Eventos evento = new Eventos();
        evento.setDocuments(new ArrayList<>(List.of(banner)));
        byte[] bytes = {1, 2, 3};
        when(repository.findById(7L)).thenReturn(java.util.Optional.of(evento));
        when(documentService.getFileBytes("eventos/banner-teste")).thenReturn(bytes);

        assertArrayEquals(bytes, service.obterBytesDoBanner(7L));

        verify(documentService).getFileBytes("eventos/banner-teste");
    }

    @Test
    void deveAtualizarEventoESubstituirBannerAntigoQuandoNovoArquivoForEnviado() {
        MockMultipartFile novoBanner = new MockMultipartFile("banner", "novo.png", "image/png", new byte[]{7});
        EventosRequestDTO request = new EventosRequestDTO(
                "Mutirão atualizado", "Descrição", "2026-11-20", "2026-11-21", "UFC", "Praça",
                "08:00", "12:00", "Saída", "Fim", "PLANEJADO", novoBanner
        );
        DocumentEntities bannerAntigo = new DocumentEntities();
        DocumentEntities bannerNovo = new DocumentEntities();
        Eventos existente = new Eventos();
        existente.setDocuments(new ArrayList<>(List.of(bannerAntigo)));
        EventosResponseDTO response = new EventosResponseDTO(
                5L, "Mutirão atualizado", null, null, null, null, null, null, null, null, null, null, new ArrayList<>()
        );
        when(repository.findById(5L)).thenReturn(java.util.Optional.of(existente));
        when(documentService.uploadAndCreateDocument(novoBanner)).thenReturn(bannerNovo);
        when(repository.save(existente)).thenReturn(existente);
        when(mapper.toResponseDTO(existente)).thenReturn(response);

        assertEquals(response, service.atualizarEvento(5L, request));

        verify(mapper).updateEntity(request, existente);
        assertEquals(List.of(bannerNovo), existente.getDocuments());
        assertSame(existente, bannerNovo.getEventos());
        verify(repository).save(existente);
    }

    @Test
    void deveManterBannerExistenteAoAtualizarSomenteOsDadosDoEvento() {
        DocumentEntities banner = new DocumentEntities();
        Eventos existente = new Eventos();
        existente.setDocuments(new ArrayList<>(List.of(banner)));
        EventosRequestDTO request = new EventosRequestDTO(
                "Novo nome", "Nova descrição", null, null, "UFC", null, null, null, null, null, null, null
        );
        EventosResponseDTO response = new EventosResponseDTO(
                6L, "Novo nome", null, null, null, null, null, null, null, null, null, null, new ArrayList<>()
        );
        when(repository.findById(6L)).thenReturn(java.util.Optional.of(existente));
        when(repository.save(existente)).thenReturn(existente);
        when(mapper.toResponseDTO(existente)).thenReturn(response);

        assertEquals(response, service.atualizarEvento(6L, request));

        assertEquals(List.of(banner), existente.getDocuments());
        verify(documentService, never()).uploadAndCreateDocument(any());
        verify(repository).save(existente);
    }

    @Test
    void falhaAoApagarBannerNoMinioNaoImpedeExclusaoDoEvento() throws Exception {
        DocumentEntities banner = new DocumentEntities();
        banner.setFilePath("eventos/banner-com-falha");
        Eventos existente = new Eventos();
        existente.setDocuments(new ArrayList<>(List.of(banner)));
        when(repository.findById(9L)).thenReturn(java.util.Optional.of(existente));
        doThrow(new RuntimeException("MinIO indisponível"))
                .when(minioClient).removeObject(any(io.minio.RemoveObjectArgs.class));

        service.deletarEvento(9L);

        verify(minioClient).removeObject(any(io.minio.RemoveObjectArgs.class));
        verify(repository).delete(existente);
    }

    @Test
    void deveRejeitarLeituraDoBannerQuandoEventoNaoTemDocumento() {
        Eventos eventoSemBanner = new Eventos();
        eventoSemBanner.setDocuments(new ArrayList<>());
        when(repository.findById(10L)).thenReturn(java.util.Optional.of(eventoSemBanner));

        assertThrows(RuntimeException.class, () -> service.obterBytesDoBanner(10L));

        verifyNoInteractions(documentService);
    }
}
