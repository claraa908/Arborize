package br.com.arborize.controllers;

import br.com.arborize.dtos.response.MovimentacaoMudaResponseDTO;
import br.com.arborize.dtos.response.MovimentacaoMudaResumoResponseDTO;
import br.com.arborize.enums.TipoMovimentacaoMuda;
import br.com.arborize.exceptions.EstoqueInsuficienteException;
import br.com.arborize.services.MovimentacaoMudaService;
import br.com.arborize.services.TokenService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MovimentacaoMudaController.class)
@AutoConfigureMockMvc(addFilters = false)
class MovimentacaoMudaControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MovimentacaoMudaService service;

    @MockitoBean
    private TokenService tokenService;

    @Test
    void listaComFiltrosEPaginacao() throws Exception {
        var registro = response(12L, TipoMovimentacaoMuda.SAIDA_PLANTIO, 4);
        when(service.listar(eq(TipoMovimentacaoMuda.SAIDA_PLANTIO), eq(7L), eq(LocalDate.of(2026, 6, 1)),
                eq(LocalDate.of(2026, 6, 30)), eq("praça"), any()))
                .thenReturn(new PageImpl<>(List.of(registro), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/viveiro/movimentacoes")
                        .param("tipo", "SAIDA_PLANTIO")
                        .param("especieId", "7")
                        .param("dataInicio", "2026-06-01")
                        .param("dataFim", "2026-06-30")
                        .param("busca", "praça")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].tipo").value("SAIDA_PLANTIO"))
                .andExpect(jsonPath("$.content[0].quantidade").value(4));

        verify(service).listar(eq(TipoMovimentacaoMuda.SAIDA_PLANTIO), eq(7L), eq(LocalDate.of(2026, 6, 1)),
                eq(LocalDate.of(2026, 6, 30)), eq("praça"), any());
    }

    @Test
    void rejeitaTamanhoDePaginaForaDoLimiteComProblemDetail() throws Exception {
        mockMvc.perform(get("/api/viveiro/movimentacoes").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("movimentacao-invalida"));
    }

    @Test
    void retornaResumoMensal() throws Exception {
        when(service.resumo()).thenReturn(new MovimentacaoMudaResumoResponseDTO(41, 6));
        mockMvc.perform(get("/api/viveiro/movimentacoes/resumo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saidasNoMes").value(41))
                .andExpect(jsonPath("$.perdasNoMes").value(6));
    }

    @Test
    void detalheInexistenteERetornadoComoProblemDetail() throws Exception {
        when(service.buscar(404L)).thenThrow(new EntityNotFoundException("Movimentação não encontrada."));

        mockMvc.perform(get("/api/viveiro/movimentacoes/404"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("recurso-nao-encontrado"));
    }

    @Test
    void comprovanteAusenteERetornadoComoProblemDetail() throws Exception {
        when(service.buscarComprovante(404L)).thenThrow(new EntityNotFoundException("Comprovante não encontrado."));

        mockMvc.perform(get("/api/viveiro/movimentacoes/404/comprovante"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Esta movimentação não possui comprovante."));
    }

    @Test
    void rejeitaQuantidadeInvalidaComProblemDetail() throws Exception {
        mockMvc.perform(multipart("/api/viveiro/movimentacoes")
                        .param("tipo", "ENTRADA_GERMINACAO")
                        .param("especieId", "7")
                        .param("quantidade", "0")
                        .param("data", "2026-06-14"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void recebeMultipartParaCriarEEditarERetornaConflitoDeSaldoComoProblemDetail() throws Exception {
        var registro = response(19L, TipoMovimentacaoMuda.ENTRADA_GERMINACAO, 10);
        when(service.criar(any())).thenReturn(registro);
        mockMvc.perform(multipart("/api/viveiro/movimentacoes")
                        .file(new MockMultipartFile("comprovante", "guia.pdf", "application/pdf", "%PDF-1.7".getBytes()))
                        .param("tipo", "ENTRADA_GERMINACAO")
                        .param("especieId", "7")
                        .param("quantidade", "10")
                        .param("data", "2026-06-14"))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.id").value(19));

        when(service.atualizar(eq(19L), any())).thenThrow(new EstoqueInsuficienteException());
        mockMvc.perform(multipart("/api/viveiro/movimentacoes/19")
                        .file(new MockMultipartFile("comprovante", "", "application/octet-stream", new byte[0]))
                        .with(request -> { request.setMethod("PUT"); return request; })
                        .param("tipo", "SAIDA_PLANTIO")
                        .param("especieId", "7")
                        .param("quantidade", "100")
                        .param("data", "2026-06-14")
                        .param("destino", "Praça"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("estoque-insuficiente"));
    }

    @Test
    void comprovanteRetornaSomenteBytesEConteudoPrivadoSemCache() throws Exception {
        when(service.buscarComprovante(22L)).thenReturn(new MovimentacaoMudaService.DocumentoComprovante(
                "%PDF-1.7".getBytes(), "application/pdf", "guia.pdf"));

        mockMvc.perform(get("/api/viveiro/movimentacoes/22/comprovante"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("guia.pdf")));
    }

    private MovimentacaoMudaResponseDTO response(Long id, TipoMovimentacaoMuda tipo, int quantidade) {
        return new MovimentacaoMudaResponseDTO(id, tipo, 7L, "Ipê-amarelo", "Handroanthus albus", quantidade,
                LocalDate.of(2026, 6, 14), "Praça", "Equipe", null, null);
    }
}
