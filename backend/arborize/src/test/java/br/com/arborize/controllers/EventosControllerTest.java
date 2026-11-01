package br.com.arborize.controllers;

import br.com.arborize.dtos.request.EventosRequestDTO;
import br.com.arborize.dtos.response.EventosResponseDTO;
import br.com.arborize.services.EventosService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventosController.class)
@AutoConfigureMockMvc(addFilters = false)
class EventosControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventosService service;

    @Test
    void deveRetornarStatus200EListaDeEventos() throws Exception {
        // Ajuste para 13 parâmetros (adicionei mais valores nulos conforme a assinatura real do record)
        EventosResponseDTO dto = new EventosResponseDTO(1L, "Trilha", null, null, null, null, null, null, null, null, null, null, new ArrayList<>());
        when(service.listarTodos()).thenReturn(List.of(dto));

        mockMvc.perform(get("/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Trilha"));
    }

    @Test
    void deveCriarEventoRetornarStatus201() throws Exception {
        MockMultipartFile banner = new MockMultipartFile("banner", "teste.png", "image/png", "img".getBytes());
        EventosResponseDTO dto = new EventosResponseDTO(1L, "Trilha Nova", null, null, null, null, null, null, null, null, null, null, new ArrayList<>());

        when(service.criarEvento(any(EventosRequestDTO.class))).thenReturn(dto);

        mockMvc.perform(multipart("/eventos")
                        .file(banner)
                        .param("nome", "Trilha Nova")
                        .param("dataInicio", "2026-10-20"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Trilha Nova"));
    }
}