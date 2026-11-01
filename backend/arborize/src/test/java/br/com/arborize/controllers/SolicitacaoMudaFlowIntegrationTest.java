package br.com.arborize.controllers;

import br.com.arborize.entities.Especie;
import br.com.arborize.entities.SolicitacaoMuda;
import br.com.arborize.repositories.EspecieRepository;
import br.com.arborize.repositories.SolicitacaoMudaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SolicitacaoMudaFlowIntegrationTest {
    private static final String TEST_CPF = "52998224725";
    private static final String TEST_PHONE = "85999999999";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EspecieRepository especieRepository;
    @Autowired
    private SolicitacaoMudaRepository solicitacaoRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private Especie especie;
    private Long createdSolicitacaoId;

    @BeforeEach
    void setUp() {
        especie = new Especie();
        especie.setNomePopular("Ipê de integração");
        especie.setNomeCientifico("Handroanthus integrationis");
        especie.setCategoria("Nativa da Caatinga");
        especie.setPorteArvore("Pequeno");
        especie.setRecomendadaCalcadas(true);
        especie = especieRepository.save(especie);
    }

    @AfterEach
    void cleanUp() {
        if (createdSolicitacaoId != null) solicitacaoRepository.deleteById(createdSolicitacaoId);
        if (especie != null && especieRepository.existsById(especie.getId())) especieRepository.deleteById(especie.getId());
    }

    @Test
    void conversionAndSubmissionPersistRequestLinkedToSelectedSpecies() throws Exception {
        mockMvc.perform(get("/api/solicitacoes-muda/conversao").param("quantidade", "2"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.garrafasPorMuda").value(5))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.totalGarrafas").value(10));

        String requestBody = objectMapper.writeValueAsString(Map.of(
                "especieId", especie.getId(),
                "quantidadeMudas", 2,
                "logradouro", "Rua das Flores",
                "numeroEndereco", "25",
                "bairro", "Centro",
                "pontoReferencia", "Praça central",
                "nomeSolicitante", "Maria Silva",
                "cpf", TEST_CPF,
                "telefone", TEST_PHONE
        ));

        String responseBody = mockMvc.perform(post("/api/solicitacoes-muda")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(responseBody);
        createdSolicitacaoId = response.get("id").asLong();
        assertEquals("PENDENTE_RECICLAVEIS", response.get("status").asText());
        assertEquals("SOL-" + java.time.Year.now().getValue() + "-" + String.format("%04d", createdSolicitacaoId), response.get("protocolo").asText());
        assertEquals(especie.getId(), response.at("/especie/id").asLong());
        assertEquals(10, response.get("totalRecicaveis").asInt());
        assertFalse(responseBody.contains(TEST_CPF));
        assertFalse(responseBody.contains(TEST_PHONE));

        SolicitacaoMuda saved = solicitacaoRepository.findById(createdSolicitacaoId).orElseThrow();
        assertEquals(especie.getId(), saved.getEspecie().getId());
        assertEquals(TEST_CPF, saved.getCpf());
        assertEquals(TEST_PHONE, saved.getTelefone());
    }

    @Test
    void invalidPersonalDataReturnsProblemDetailWithoutEchoingValues() throws Exception {
        String requestBody = objectMapper.writeValueAsString(Map.of(
                "especieId", especie.getId(),
                "quantidadeMudas", 1,
                "logradouro", "Rua das Flores",
                "numeroEndereco", "25",
                "bairro", "Centro",
                "pontoReferencia", "Praça central",
                "nomeSolicitante", "Maria Silva",
                "cpf", "00000000000",
                "telefone", TEST_PHONE
        ));

        String responseBody = mockMvc.perform(post("/api/solicitacoes-muda")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.errors.cpf").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertFalse(responseBody.contains("00000000000"));
        assertFalse(responseBody.contains(TEST_PHONE));
    }
}
