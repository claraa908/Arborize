package br.com.arborize.controllers;

import br.com.arborize.dtos.request.EspecieRequestDTO;
import br.com.arborize.dtos.response.DocumentResponseDTO;
import br.com.arborize.dtos.response.EspecieResponseDTO;
import br.com.arborize.entities.Especie;
import br.com.arborize.mappers.EspecieMapper;
import br.com.arborize.services.EspecieService;
import br.com.arborize.services.TokenService; // Importe o TokenService
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EspecieController.class)
@AutoConfigureMockMvc(addFilters = false)
class EspecieControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EspecieService service;

    // Adicione o Mock do TokenService para o contexto do Spring Security carregar corretamente
    @MockitoBean
    private TokenService tokenService;

    @Test
    void deveRetornarStatus200EListaDeEspecies() throws Exception {
        EspecieResponseDTO dto = new EspecieResponseDTO(1L, "Cajueiro", null, null, null, null, null, null, null, 0, new ArrayList<>());
        when(service.listarTodos()).thenReturn(List.of(dto));

        mockMvc.perform(get("/especies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nomePopular").value("Cajueiro"));
    }

    @Test
    void deveBuscarEspeciePorId() throws Exception {
        EspecieResponseDTO dto = new EspecieResponseDTO(
                9L, "Juazeiro", "Ziziphus joazeiro", null, "Nativa", "Médio", null,
                null, true, 0, new ArrayList<>()
        );
        when(service.buscarPorId(9L)).thenReturn(dto);

        mockMvc.perform(get("/especies/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomePopular").value("Juazeiro"))
                .andExpect(jsonPath("$.recomendadaCalcadas").value(true));
    }

    @Test
    void deveRetornarBytesDaImagemComTipoDeclaradoNoDocumento() throws Exception {
        byte[] image = {2, 4, 6};
        DocumentResponseDTO document = new DocumentResponseDTO(2L, "juazeiro.png", "image/png", 3L, "png");
        EspecieResponseDTO dto = new EspecieResponseDTO(
                9L, "Juazeiro", "Ziziphus joazeiro", null, "Nativa", "Médio", null,
                null, true, 0, List.of(document)
        );
        when(service.buscarPorId(9L)).thenReturn(dto);
        when(service.obterBytesDaImagem(9L)).thenReturn(image);

        mockMvc.perform(get("/especies/9/imagem"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(image));
    }

    @Test
    void deveExcluirEspecieERetornarSemConteudo() throws Exception {
        mockMvc.perform(delete("/especies/9"))
                .andExpect(status().isNoContent());

        org.mockito.Mockito.verify(service).deletarEspecie(9L);
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

    @Test
    void deveIgnorarQuantidadeLegadaNaAtualizacaoEManterEstoqueExistente() throws Exception {
        Especie existente = new Especie();
        existente.setMudasDisponiveis(42);
        EspecieMapper mapper = new EspecieMapper();
        EspecieResponseDTO response = new EspecieResponseDTO(
                11L, "Ipê atualizado", null, null, "Nativa", "Médio", null,
                null, true, 42, new ArrayList<>()
        );

        when(service.atualizarEspecie(eq(11L), any(EspecieRequestDTO.class))).thenAnswer(invocation -> {
            EspecieRequestDTO request = invocation.getArgument(1);
            mapper.updateEntity(request, existente);
            return response;
        });

        mockMvc.perform(multipart("/especies/editar/11")
                        .param("nomePopular", "Ipê atualizado")
                        .param("nomeCientifico", "Handroanthus albus")
                        .param("categoria", "Nativa")
                        .param("porteArvore", "Médio")
                        .param("recomendadaCalcadas", "true")
                        .param("mudasDisponiveis", "999")
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mudasDisponiveis").value(42));

        org.junit.jupiter.api.Assertions.assertEquals(42, existente.getMudasDisponiveis());
    }
}
