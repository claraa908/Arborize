package br.com.arborize.controllers;

import br.com.arborize.dtos.response.BairroResumoResponseDTO;
import br.com.arborize.services.BairroService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class BairroControllerTest {
    private BairroService service;
    private MockMvc mvc;

    @BeforeEach
    void prepare() {
        service = mock(BairroService.class);
        mvc = standaloneSetup(new BairroController(service)).build();
    }

    @Test
    void listaOsBairrosDisponiveisSemModificarOServico() throws Exception {
        when(service.listarDisponiveis()).thenReturn(List.of(
                new BairroResumoResponseDTO(2L, "Centro", true),
                new BairroResumoResponseDTO(4L, "Planalto", true)));

        mvc.perform(get("/api/bairros"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Centro"))
                .andExpect(jsonPath("$[1].nome").value("Planalto"));

        verify(service).listarDisponiveis();
        verifyNoMoreInteractions(service);
    }
}
