package br.com.arborize.controllers;

import br.com.arborize.migrations.PostgresTestSchema;
import br.com.arborize.entities.Especie;
import br.com.arborize.entities.SolicitacaoMuda;
import br.com.arborize.repositories.EspecieRepository;
import br.com.arborize.repositories.SolicitacaoMudaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "ARBORIZE_MIGRATION_TEST_URL", matches = ".+")
class SolicitacaoMudaFlowIntegrationTest {
    private static final String BASE = "/api/solicitacoes-muda";
    private static final String GESTAO = BASE + "/gestao";
    private static final String TEST_CPF = "52998224725";
    private static final String TEST_PHONE = "85999999999";
    private static String schema;

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) throws Exception {
        schema = PostgresTestSchema.configure(registry, "arborize_request_test_");
    }

    @AfterAll
    static void removeSchema() throws Exception {
        PostgresTestSchema.remove(schema);
    }

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
                .andExpect(jsonPath("$.errors.cpf").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertFalse(responseBody.contains("00000000000"));
        assertFalse(responseBody.contains(TEST_PHONE));
    }

    @ParameterizedTest
    @ValueSource(strings = {"AMMA", "ADMINISTRADOR_AMMA", "FUNCIONARIO_AMMA",
            "BOLSISTA_IFCE", "IFCE_BOLSISTA", "GESTOR_IFCE", "IFCE_GESTOR"})
    void perfisInstitucionaisConsultamERegistramRecebimento(String papel) throws Exception {
        String protocolo = criarPedido("CALCADA");
        var operador = user("operador").roles(papel);
        mockMvc.perform(get(GESTAO).with(operador).param("protocolo", " " + protocolo.toLowerCase(java.util.Locale.ROOT) + " "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].protocolo").value(protocolo))
                .andExpect(jsonPath("$.content[0].cpf").doesNotExist())
                .andExpect(jsonPath("$.content[0].telefone").doesNotExist());
        mockMvc.perform(get(GESTAO + "/{protocolo}", protocolo).with(operador))
                .andExpect(status().isOk()).andExpect(jsonPath("$.localPlantio").value("CALCADA"))
                .andExpect(jsonPath("$.cpf").doesNotExist()).andExpect(jsonPath("$.telefone").doesNotExist());
        mockMvc.perform(post(GESTAO + "/{protocolo}/recebimento", protocolo).with(operador)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantidadeRecebida\":11}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APROVADA"))
                .andExpect(jsonPath("$.quantidadeRecebida").value(11));
        mockMvc.perform(post(BASE + "/acompanhar").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("protocolo", protocolo, "cpf", TEST_CPF))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APROVADA"));
    }

    @Test
    void protecaoDaGestaoERejeicaoComMotivo() throws Exception {
        String protocolo = criarPedido("QUINTAL");
        var amma = user("gestora").roles("AMMA");
        String rota = GESTAO + "/" + protocolo + "/rejeicao";
        mockMvc.perform(get(GESTAO)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(GESTAO).with(user("cidadão").roles("CIDADAO"))).andExpect(status().isForbidden());
        mockMvc.perform(get(GESTAO + "/{protocolo}", protocolo).with(amma))
                .andExpect(status().isOk()).andExpect(jsonPath("$.localPlantio").value("QUINTAL"));
        mockMvc.perform(get(GESTAO).with(amma).param("page", "0").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].protocolo").value(protocolo));
        mockMvc.perform(get(GESTAO + "/SOL-2000-0001").with(amma)).andExpect(status().isNotFound());
        mockMvc.perform(get(GESTAO).param("protocolo", "inexistente").with(amma))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty());
        mockMvc.perform(post(rota).with(user("bolsista").roles("IFCE_BOLSISTA"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Motivo de teste\"}"))
                .andExpect(status().isForbidden());
        for (String motivo : new String[]{" ", "a".repeat(256)}) {
            mockMvc.perform(post(rota).with(amma).contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("motivo", motivo))))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.motivo").isString());
        }
        mockMvc.perform(post(rota).with(amma).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"  Motivo de teste  \"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJEITADA"))
                .andExpect(jsonPath("$.motivoRejeicao").value("Motivo de teste"));
        assertEquals("Motivo de teste", solicitacaoRepository.findById(createdSolicitacaoId).orElseThrow().getMotivoRejeicao());
    }

    @Test
    void quantidadeInvalidaNaoPersisteEConcorrenciaSoAprovaUmaVez() throws Exception {
        String protocolo = criarPedido(null);
        String rota = GESTAO + "/" + protocolo + "/recebimento";
        for (String quantidade : new String[]{"null", "9", "-1", "10.5", "2147483648"}) {
            mockMvc.perform(post(rota).with(user("operador").roles("GESTOR_IFCE"))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"quantidadeRecebida\":" + quantidade + "}"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.quantidadeRecebida").isString());
        }
        var pendente = solicitacaoRepository.findById(createdSolicitacaoId).orElseThrow();
        assertNull(pendente.getQuantidadeRecebida());
        assertNull(pendente.getLocalPlantio());
        assertEquals("PENDENTE_RECICLAVEIS", pendente.getStatus().name());

        var inicio = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Integer> operacao = () -> {
                inicio.await();
                return mockMvc.perform(post(rota).with(user("operador").roles("GESTOR_IFCE"))
                                .contentType(MediaType.APPLICATION_JSON).content("{\"quantidadeRecebida\":10}"))
                        .andReturn().getResponse().getStatus();
            };
            var primeiro = executor.submit(operacao);
            var segundo = executor.submit(operacao);
            inicio.countDown();
            assertEquals(java.util.Set.of(200, 409),
                    java.util.Set.of(primeiro.get(60, TimeUnit.SECONDS), segundo.get(60, TimeUnit.SECONDS)));
        }
        assertEquals(10, solicitacaoRepository.findById(createdSolicitacaoId).orElseThrow().getQuantidadeRecebida());
    }

    @Test
    void acompanhamentoPreservaPrivacidadeEExibeRetiradaConcluida() throws Exception {
        String protocolo = criarPedido(null);
        SolicitacaoMuda item = solicitacaoRepository.findById(createdSolicitacaoId).orElseThrow();
        item.setStatus(br.com.arborize.enums.SolicitacaoMudaStatus.RETIRADA_CONCLUIDA);
        solicitacaoRepository.save(item);

        mockMvc.perform(post(BASE + "/acompanhar").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("protocolo", protocolo, "cpf", TEST_CPF))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RETIRADA_CONCLUIDA"));

        String cpfIncorreto = consultarNaoEncontrado(protocolo, "11144477735");
        String protocoloInexistente = consultarNaoEncontrado("SOL-2000-0001", TEST_CPF);
        assertEquals(objectMapper.readTree(cpfIncorreto), objectMapper.readTree(protocoloInexistente));
    }

    private String consultarNaoEncontrado(String protocolo, String cpf) throws Exception {
        return mockMvc.perform(post(BASE + "/acompanhar").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("protocolo", protocolo, "cpf", cpf))))
                .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();
    }

    private String criarPedido(String localPlantio) throws Exception {
        String resposta = mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dadosPedido(localPlantio))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        JsonNode dados = objectMapper.readTree(resposta);
        createdSolicitacaoId = dados.get("id").asLong();
        return dados.get("protocolo").asText();
    }

    @Test
    void localDePlantioInvalidoRetornaErroDeCampo() throws Exception {
        long antes = solicitacaoRepository.count();
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dadosPedido("OUTRO"))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.localPlantio").isString());
        assertEquals(antes, solicitacaoRepository.count());
    }

    private Map<String, Object> dadosPedido(String localPlantio) {
        var campos = new java.util.HashMap<String, Object>(Map.of(
                "especieId", especie.getId(), "quantidadeMudas", 2, "logradouro", "Rua de teste",
                "numeroEndereco", "1", "bairro", "Centro", "pontoReferencia", "Praça de teste",
                "nomeSolicitante", "Pessoa de Teste", "cpf", TEST_CPF, "telefone", TEST_PHONE));
        if (localPlantio != null) campos.put("localPlantio", localPlantio);
        return campos;
    }
}
