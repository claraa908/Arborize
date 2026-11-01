package br.com.arborize.controllers;


import br.com.arborize.dtos.request.EspecieRequestDTO;
import br.com.arborize.dtos.response.EspecieResponseDTO;
import br.com.arborize.services.EspecieService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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

@WebMvcTest(EspecieController.class)
@AutoConfigureMockMvc(addFilters = false)
class EspecieControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EspecieService service;

    @Test
    void deveRetornarStatus200EListaDeEspecies() throws Exception {
        EspecieResponseDTO dto = new EspecieResponseDTO(1L, "Cajueiro", null, null, null, null, null, null, null, 0, new ArrayList<>());
        when(service.listarTodos()).thenReturn(List.of(dto));

        mockMvc.perform(get("/especies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nomePopular").value("Cajueiro"));
    }

    @Test
    void deveCriarEspecieRetornarStatus201() throws Exception {
        MockMultipartFile foto = new MockMultipartFile("foto", "caju.png", "image/png", "img".getBytes());
        EspecieResponseDTO dto = new EspecieResponseDTO(1L, "Cajueiro Anão", null, null, null, null, null, null, null, 0, new ArrayList<>());

        when(service.criarEspecie(any(EspecieRequestDTO.class))).thenReturn(dto);

        mockMvc.perform(multipart("/especies")
                        .file(foto)
                        .param("nomePopular", "Cajueiro Anão")
                        .param("recomendadaCalcadas", "true"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nomePopular").value("Cajueiro Anão"));
    }
}