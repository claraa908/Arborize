package br.com.arborize.integration;

import br.com.arborize.dtos.request.*;
import br.com.arborize.controllers.ArvoreController;
import br.com.arborize.controllers.GlobalExceptionHandler;
import br.com.arborize.entities.*;
import br.com.arborize.enums.*;
import br.com.arborize.exceptions.EntradaInventarioInvalidaException;
import br.com.arborize.repositories.*;
import br.com.arborize.services.*;
import br.com.arborize.storage.FotoArvoreStorage;
import br.com.arborize.validation.FotoArvoreValidatorTest;
import jakarta.persistence.*;
import jakarta.validation.ConstraintViolationException;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.*;

/** Executado explicitamente pelo Maven (-Dtest=InventarioPersistenciaIT), somente em PostgreSQL descartável. */
@SpringJUnitConfig({InventarioPersistenceTestConfig.class, InventarioPersistenciaIT.MockStorageConfig.class})
@ActiveProfiles(InventarioPersistenceTestConfig.PROFILE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class InventarioPersistenciaIT {
    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
    static class MockStorageConfig {
        @org.springframework.context.annotation.Bean
        FotoArvoreStorage fotoArvoreStorage() { return mock(FotoArvoreStorage.class); }
    }
    @Autowired ArvoreService service;
    @Autowired FotoArvoreService photoService;
    @Autowired ArvoreRepository arvores;
    @Autowired FotoArvoreRepository fotos;
    @Autowired FotoArvoreStorage storage;
    @Autowired EntityManagerFactory emf;
    @Autowired PlatformTransactionManager manager;
    @Autowired DataSource dataSource;
    @Autowired org.springframework.validation.Validator validator;
    @PersistenceContext EntityManager em;
    private MockMvc http;
    private TransactionTemplate tx;
    private Especie caju, literal;
    private Bairro bairro;
    private byte[] png;

    private <T> T tx(Supplier<T> action) { return tx.execute(status -> action.get()); }

    @BeforeAll
    static void resetSql() { InventarioPersistenceTestConfig.SqlCapture.clearCurrent(); }

    @BeforeEach
    void prepare() throws Exception {
        tx = new TransactionTemplate(manager);
        tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        reset(storage);
        png = FotoArvoreValidatorTest.imagem("png");
        when(storage.recuperar(anyString())).thenReturn(png);
        tx(() -> {
            em.createQuery("delete from FotoArvore").executeUpdate();
            em.createQuery("delete from Arvore").executeUpdate();
            em.createQuery("delete from Bairro").executeUpdate();
            em.createQuery("delete from Especie").executeUpdate();
            caju = especie("Cajueiro de teste", "Anacardium teste");
            literal = especie("Teste 100%_literal", "Nome científico exclusivo");
            bairro = new Bairro(); bairro.setNome("Bairro exclusivamente de teste"); em.persist(bairro);
            em.flush(); return null;
        });
        InventarioPersistenceTestConfig.SqlCapture.clearCurrent();
        http = MockMvcBuilders.standaloneSetup(new ArvoreController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    private Especie especie(String popular, String cientifico) {
        var e = new Especie(); e.setNomePopular(popular); e.setNomeCientifico(cientifico);
        e.setCategoria("TESTE"); e.setPorteArvore("TESTE"); e.setRecomendadaCalcadas(false); em.persist(e); return e;
    }

    private ArvoreCadastroRequestDTO cadastro(Long especieId, ClassificacaoTerritorial territorio, Long bairroId) {
        return new ArvoreCadastroRequestDTO(especieId, new BigDecimal("-4.97"), new BigDecimal("-39.01"),
                territorio, bairroId, null, null, null, null, null, null);
    }
    private Long criar(Long especieId, ClassificacaoTerritorial territorio, Long bairroId) {
        return service.criarArvore(cadastro(especieId, territorio, bairroId)).id();
    }
    private Long criarTextual(Long especieId, ClassificacaoTerritorial territorio, String bairroNome, String cep) {
        var dto = new ArvoreCadastroRequestDTO(especieId, new BigDecimal("-4.97"), new BigDecimal("-39.01"),
                territorio, null, bairroNome, cep, null, null, null, null,
                CondicaoFitossanitaria.NAO_AVALIADA, null);
        return service.criarArvore(dto).id();
    }
    private Long criar() { return criar(caju.getId(), ClassificacaoTerritorial.BAIRRO_NAO_IDENTIFICADO, null); }
    private ArvoreAtualizacaoRequestDTO update(Long especieId, ClassificacaoTerritorial territorio, Long bairroId,
                                              CondicaoFitossanitaria condicao, String observacoes) {
        return new ArvoreAtualizacaoRequestDTO(especieId, new BigDecimal("-4.98"), new BigDecimal("-39.02"),
                territorio, bairroId, null, null, null, null, condicao, observacoes);
    }
    private MockMultipartFile arquivo() { return new MockMultipartFile("file", "foto.png", "image/png", png); }
    private List<Long> mapa(String search, Long bairroId, Long especieId, CondicaoFitossanitaria condicao) {
        return service.consultarMapa(search, bairroId, especieId, condicao).stream().map(a -> a.id()).toList();
    }
    private List<Long> mapa(String search, String bairroNome, Long bairroId, Long especieId,
                            CondicaoFitossanitaria condicao) {
        return service.consultarMapa(search, bairroNome, bairroId, especieId, condicao)
                .stream().map(a -> a.id()).toList();
    }

    @Test
    void bancoEContextoSaoExclusivosDeTeste() throws Exception {
        try (var c = dataSource.getConnection(); var s = c.createStatement();
             var rs = s.executeQuery("select current_database(), current_user, version()")) {
            assertTrue(rs.next()); assertTrue(rs.getString(1).matches("arborize_it_[0-9a-f]{12}"));
            assertEquals("postgres", rs.getString(2)); assertTrue(rs.getString(3).startsWith("PostgreSQL 15."));
            System.out.println("Banco integrado: " + rs.getString(3));
        }
    }

    @Test
    void cargaDeBairrosReferenciaEIdempotenteEPreservaIdsEAssociacoes() throws Exception {
        var existentes = tx(() -> {
            Bairro centro = new Bairro();
            centro.setNome("Centro");
            centro.setAtivo(false);
            em.persist(centro);

            Bairro boto = new Bairro();
            boto.setNome("  BÔTO   ");
            boto.setAtivo(false);
            em.persist(boto);

            Arvore arvore = new Arvore();
            arvore.setEspecie(caju);
            arvore.setBairro(centro);
            arvore.setClassificacaoTerritorial(ClassificacaoTerritorial.BAIRRO_IDENTIFICADO);
            arvore.setLatitude(new BigDecimal("-4.97"));
            arvore.setLongitude(new BigDecimal("-39.01"));
            em.persist(arvore);
            em.flush();
            return List.of(centro.getId(), boto.getId(), arvore.getId());
        });

        var script = new ClassPathResource("db/manual/bairros-referencia-quixada.sql");
        for (int repeticao = 0; repeticao < 2; repeticao++) {
            try (Connection connection = dataSource.getConnection()) {
                ScriptUtils.executeSqlScript(connection, script);
            }
        }

        var nomes = List.of("Alto da Boa Vista", "Alto São Francisco", "Baviera", "Bôto",
                "Campo Novo", "Campo Velho", "Carrascal I", "Carrascal II", "Cedro", "Centro",
                "Cohab", "Combate", "Curicaca", "Herval", "Irajá", "Jardim dos Monólitos",
                "Monte Alegre", "Nova Jerusalém", "Planalto Renascer", "Planalto Universitário",
                "Putiú", "Residencial", "São João", "Triângulo");
        try (Connection connection = dataSource.getConnection();
             PreparedStatement quantidade = connection.prepareStatement("""
                     SELECT COUNT(*) FROM bairros
                     WHERE translate(lower(regexp_replace(btrim(nome), '[[:space:]]+', ' ', 'g')),
                         'áàâãäéèêëíìîïóòôõöúùûüç', 'aaaaaeeeeiiiiooooouuuuc') =
                       translate(lower(regexp_replace(btrim(?), '[[:space:]]+', ' ', 'g')),
                         'áàâãäéèêëíìîïóòôõöúùûüç', 'aaaaaeeeeiiiiooooouuuuc')
                     """)) {
            for (String nome : nomes) {
                quantidade.setString(1, nome);
                try (ResultSet resultado = quantidade.executeQuery()) {
                    assertTrue(resultado.next());
                    assertEquals(1, resultado.getInt(1), nome);
                }
            }
            try (PreparedStatement centro = connection.prepareStatement(
                    "SELECT id, ativo FROM bairros WHERE id = ?")) {
                centro.setLong(1, existentes.get(0));
                try (ResultSet resultado = centro.executeQuery()) {
                    assertTrue(resultado.next());
                    assertFalse(resultado.getBoolean("ativo"));
                }
            }
            try (PreparedStatement boto = connection.prepareStatement(
                    "SELECT id, nome, ativo FROM bairros WHERE id = ?")) {
                boto.setLong(1, existentes.get(1));
                try (ResultSet resultado = boto.executeQuery()) {
                    assertTrue(resultado.next());
                    assertEquals("  BÔTO   ", resultado.getString("nome"));
                    assertFalse(resultado.getBoolean("ativo"));
                }
            }
            try (PreparedStatement relacao = connection.prepareStatement(
                    "SELECT bairro_id FROM arvores WHERE id = ?")) {
                relacao.setLong(1, existentes.get(2));
                try (ResultSet resultado = relacao.executeQuery()) {
                    assertTrue(resultado.next());
                    assertEquals(existentes.get(0), resultado.getLong("bairro_id"));
                }
            }
        }
    }

    @Test
    void backfillTerritorialSoPreencheNomeAusenteEPreservaDadosLegados() throws Exception {
        var ids = tx(() -> {
            Arvore semNome = new Arvore();
            semNome.setEspecie(caju);
            semNome.setBairro(bairro);
            semNome.setClassificacaoTerritorial(ClassificacaoTerritorial.BAIRRO_IDENTIFICADO);
            semNome.setLatitude(new BigDecimal("-4.97"));
            semNome.setLongitude(new BigDecimal("-39.01"));
            em.persist(semNome);

            Arvore nomeManual = new Arvore();
            nomeManual.setEspecie(caju);
            nomeManual.setBairro(bairro);
            nomeManual.setBairroNome("Nome territorial anterior");
            nomeManual.setCep("06390-123");
            nomeManual.setClassificacaoTerritorial(ClassificacaoTerritorial.BAIRRO_IDENTIFICADO);
            nomeManual.setLatitude(new BigDecimal("-4.98"));
            nomeManual.setLongitude(new BigDecimal("-39.02"));
            em.persist(nomeManual);
            em.flush();
            return List.of(semNome.getId(), nomeManual.getId(), bairro.getId());
        });

        var script = new ClassPathResource("db/manual/territorio-arvore-bairro-cep.sql");
        for (int repeticao = 0; repeticao < 2; repeticao++) {
            try (Connection connection = dataSource.getConnection()) {
                ScriptUtils.executeSqlScript(connection, script);
            }
        }

        try (Connection connection = dataSource.getConnection();
             PreparedStatement arvore = connection.prepareStatement(
                     "SELECT bairro_id, bairro_nome, cep FROM arvores WHERE id = ?")) {
            arvore.setLong(1, ids.get(0));
            try (ResultSet resultado = arvore.executeQuery()) {
                assertTrue(resultado.next());
                assertEquals(ids.get(2), resultado.getLong("bairro_id"));
                assertEquals(bairro.getNome(), resultado.getString("bairro_nome"));
                assertNull(resultado.getString("cep"));
            }
            arvore.setLong(1, ids.get(1));
            try (ResultSet resultado = arvore.executeQuery()) {
                assertTrue(resultado.next());
                assertEquals(ids.get(2), resultado.getLong("bairro_id"));
                assertEquals("Nome territorial anterior", resultado.getString("bairro_nome"));
                assertEquals("06390-123", resultado.getString("cep"));
            }
        }
    }

    @Test
    void contratosUrbanoERuralPersistemELeituraLegadaPermaneceDisponivel() {
        var urbano = service.criarArvore(new ArvoreCadastroRequestDTO(
                caju.getId(), new BigDecimal("-4.97"), new BigDecimal("-39.01"), null,
                bairro.getId(), null, "06390-000", "Praça central", null, null, null,
                CondicaoFitossanitaria.NAO_AVALIADA, null, false, null));
        assertEquals(ClassificacaoTerritorial.BAIRRO_IDENTIFICADO, urbano.classificacaoTerritorial());
        assertFalse(urbano.zonaRural());
        assertEquals(bairro.getId(), urbano.bairro().id());
        assertEquals(bairro.getNome(), urbano.bairroNome());
        assertNull(urbano.localidadeRural());

        var rural = service.criarArvore(new ArvoreCadastroRequestDTO(
                caju.getId(), new BigDecimal("-4.98"), new BigDecimal("-39.02"), null,
                null, null, null, "Estrada municipal", null, null, null,
                CondicaoFitossanitaria.NAO_AVALIADA, null, true, "Sítio Boa Vista"));
        assertEquals(ClassificacaoTerritorial.FORA_DA_AREA_URBANA, rural.classificacaoTerritorial());
        assertTrue(rural.zonaRural());
        assertNull(rural.bairro());
        assertNull(rural.bairroNome());
        assertEquals("Sítio Boa Vista", rural.localidadeRural());

        var legado = service.criarArvore(cadastro(caju.getId(), ClassificacaoTerritorial.BAIRRO_IDENTIFICADO,
                bairro.getId()));
        assertEquals(bairro.getId(), legado.bairro().id());
        assertEquals(bairro.getNome(), legado.bairroNome());
        assertFalse(legado.zonaRural());

        var criadoEm = legado.criadoEm();
        var atualizado = service.atualizarArvore(legado.id(), new ArvoreAtualizacaoRequestDTO(
                caju.getId(), legado.latitude(), legado.longitude(), null, null, null, null,
                null, null, null, null, CondicaoFitossanitaria.NAO_AVALIADA, null,
                true, "Comunidade de teste"));
        assertEquals(legado.id(), atualizado.id());
        assertEquals(criadoEm.withNano((criadoEm.getNano() / 1_000) * 1_000), atualizado.criadoEm());
        assertNull(atualizado.bairro());
        assertEquals(ClassificacaoTerritorial.FORA_DA_AREA_URBANA, atualizado.classificacaoTerritorial());
        assertEquals("Comunidade de teste", atualizado.localidadeRural());
    }

    @Test
    void contratosHttpNovoValidamCadastroEdicaoERejeitamTerritoriosIncompletos() throws Exception {
        var urbano = http.perform(post("/api/arvores")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"especieId":%d,"latitude":-4.97,"longitude":-39.01,
                                 "zonaRural":false,"bairroId":%d,"condicaoFitossanitaria":"NAO_AVALIADA"}
                                """.formatted(caju.getId(), bairro.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.zonaRural").value(false))
                .andExpect(jsonPath("$.bairro.id").value(bairro.getId()))
                .andExpect(jsonPath("$.bairroNome").value(bairro.getNome()))
                .andReturn();
        Long idUrbana = mapperId(urbano.getResponse().getContentAsString());

        http.perform(post("/api/arvores")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"especieId":%d,"latitude":-4.98,"longitude":-39.02,
                                 "zonaRural":true,"localidadeRural":"Comunidade de teste",
                                 "cep":"06390-000","condicaoFitossanitaria":"NAO_AVALIADA"}
                                """.formatted(caju.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.zonaRural").value(true))
                .andExpect(jsonPath("$.bairro").doesNotExist())
                .andExpect(jsonPath("$.bairroNome").doesNotExist())
                .andExpect(jsonPath("$.localidadeRural").value("Comunidade de teste"));

        http.perform(put("/api/arvores/{id}", idUrbana)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"especieId":%d,"latitude":-4.97,"longitude":-39.01,
                                 "zonaRural":true,"localidadeRural":"Sítio de teste",
                                 "condicaoFitossanitaria":"NAO_AVALIADA"}
                                """.formatted(caju.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(idUrbana))
                .andExpect(jsonPath("$.zonaRural").value(true))
                .andExpect(jsonPath("$.bairro").doesNotExist())
                .andExpect(jsonPath("$.localidadeRural").value("Sítio de teste"));

        http.perform(post("/api/arvores")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"especieId":%d,"latitude":-4.97,"longitude":-39.01,
                                 "zonaRural":false,"condicaoFitossanitaria":"NAO_AVALIADA"}
                                """.formatted(caju.getId())))
                .andExpect(status().isBadRequest());

        http.perform(post("/api/arvores")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"especieId":%d,"latitude":-4.97,"longitude":-39.01,
                                 "zonaRural":true,"condicaoFitossanitaria":"NAO_AVALIADA"}
                                """.formatted(caju.getId())))
                .andExpect(status().isBadRequest());
    }

    private Long mapperId(String json) throws Exception {
        return new com.fasterxml.jackson.databind.ObjectMapper().readTree(json).get("id").asLong();
    }

    @Test
    void filtroDeBairroRetornaMesmoConjuntoNaTabelaENoMapaEExcluiLocalidadeRural() {
        var urbano = service.criarArvore(new ArvoreCadastroRequestDTO(
                caju.getId(), new BigDecimal("-4.97"), new BigDecimal("-39.01"), null,
                bairro.getId(), null, null, null, null, null, null,
                CondicaoFitossanitaria.NAO_AVALIADA, null, false, null));
        service.criarArvore(new ArvoreCadastroRequestDTO(
                caju.getId(), new BigDecimal("-4.98"), new BigDecimal("-39.02"), null,
                null, null, null, null, null, null, null,
                CondicaoFitossanitaria.NAO_AVALIADA, null, true, bairro.getNome()));

        var pagina = service.listar(null, bairro.getNome().substring(2, 7), null, caju.getId(),
                CondicaoFitossanitaria.NAO_AVALIADA, PageRequest.of(0, 20));
        var mapa = service.consultarMapa(null, bairro.getNome().substring(2, 7), null, caju.getId(),
                CondicaoFitossanitaria.NAO_AVALIADA);

        assertEquals(List.of(urbano.id()), pagina.getContent().stream().map(a -> a.id()).toList());
        assertEquals(pagina.getContent().stream().map(a -> a.id()).toList(),
                mapa.stream().map(a -> a.id()).toList());
    }

    @Test
    void cadastroGeraIdExecutaCallbacksEPersisteOpcionaisNulos() {
        Long id = criar(); assertNotNull(id); assertTrue(id > 0);
        var a = service.buscarPorId(id);
        assertEquals(id, a.id()); assertEquals(caju.getId(), a.especie().id());
        assertNull(a.dataPlantio()); assertNull(a.dapCm()); assertNull(a.alturaEstimadaM()); assertNull(a.observacoes());
        assertEquals(CondicaoFitossanitaria.NAO_AVALIADA, a.condicaoFitossanitaria());
        assertNotNull(a.criadoEm()); assertNotNull(a.atualizadoEm()); assertEquals(a.criadoEm(), a.atualizadoEm());
    }

    @Test
    void cadastroTextualSemCatalogoPreservaCepEEstadosTerritoriais() {
        Long bairroTextual = criarTextual(caju.getId(), ClassificacaoTerritorial.BAIRRO_IDENTIFICADO,
                "Centro", "06390-000");
        Long semBairro = criarTextual(caju.getId(), ClassificacaoTerritorial.BAIRRO_NAO_IDENTIFICADO,
                null, "06390-001");
        Long fora = criarTextual(caju.getId(), ClassificacaoTerritorial.FORA_DA_AREA_URBANA,
                null, null);

        var ficha = service.buscarPorId(bairroTextual);
        assertNull(ficha.bairro());
        assertEquals("Centro", ficha.bairroNome());
        assertEquals("06390-000", ficha.cep());
        assertNull(service.buscarPorId(semBairro).bairroNome());
        assertEquals("06390-001", service.buscarPorId(semBairro).cep());
        assertEquals(ClassificacaoTerritorial.BAIRRO_NAO_IDENTIFICADO,
                service.buscarPorId(semBairro).classificacaoTerritorial());
        assertEquals(ClassificacaoTerritorial.FORA_DA_AREA_URBANA,
                service.buscarPorId(fora).classificacaoTerritorial());
        assertNull(service.buscarPorId(fora).bairroNome());
    }

    @Test
    void putTextualSemBairroIdAtualizaNomeECepEPreservaAssociacaoAntiga() {
        Long id = criar(caju.getId(), ClassificacaoTerritorial.BAIRRO_IDENTIFICADO, bairro.getId());
        var dto = new ArvoreAtualizacaoRequestDTO(caju.getId(), new BigDecimal("-4.98"),
                new BigDecimal("-39.02"), ClassificacaoTerritorial.BAIRRO_IDENTIFICADO,
                null, "Bairro revisado", "06390-123", null, null, null, null,
                CondicaoFitossanitaria.NAO_AVALIADA, null);

        var resposta = service.atualizarArvore(id, dto);
        Arvore persistida = tx(() -> em.find(Arvore.class, id));

        assertEquals(bairro.getId(), resposta.bairro().id());
        assertEquals("Bairro revisado", resposta.bairroNome());
        assertEquals("06390-123", resposta.cep());
        assertEquals(bairro.getId(), persistida.getBairro().getId());
        assertEquals("Bairro revisado", persistida.getBairroNome());
        assertEquals("06390-123", persistida.getCep());
    }

    @Test
    void filtroTextualParcialTemFallbackLegadoEIgualdadeEntreTabelaEMapa() {
        Long textual = criarTextual(caju.getId(), ClassificacaoTerritorial.BAIRRO_IDENTIFICADO,
                "Centro das árvores", "06390-000");
        Long outraEspecie = criarTextual(literal.getId(), ClassificacaoTerritorial.BAIRRO_IDENTIFICADO,
                "Centro das árvores", null);
        Long legado = criar(caju.getId(), ClassificacaoTerritorial.BAIRRO_IDENTIFICADO, bairro.getId());
        tx(() -> {
            em.find(Arvore.class, legado).setBairroNome(null);
            return null;
        });

        var criterios = PageRequest.of(0, 20);
        var tabela = service.listar(" " + service.buscarPorId(textual).codigo().toLowerCase(Locale.ROOT).substring(4, 12)
                        + " ", " centro ", null, caju.getId(), CondicaoFitossanitaria.NAO_AVALIADA, criterios)
                .getContent().stream().map(a -> a.id()).toList();
        var mapa = mapa(" " + service.buscarPorId(textual).codigo().toLowerCase(Locale.ROOT).substring(4, 12)
                + " ", " centro ", null, caju.getId(), CondicaoFitossanitaria.NAO_AVALIADA);

        assertEquals(List.of(textual), tabela);
        assertEquals(tabela, mapa);
        assertEquals(List.of(legado), mapa(null, "EXCLUSIVAMENTE", null, null, null));
        assertTrue(mapa(null, "centro", null, literal.getId(), CondicaoFitossanitaria.MORTA).isEmpty());
    }

    @Test
    void persisteTresSituacoesTerritoriaisSemBairrosArtificiais() {
        for (var territorio : ClassificacaoTerritorial.values()) {
            Long id = criar(caju.getId(), territorio, territorio == ClassificacaoTerritorial.BAIRRO_IDENTIFICADO ? bairro.getId() : null);
            var a = service.buscarPorId(id); assertEquals(territorio, a.classificacaoTerritorial());
            if (territorio == ClassificacaoTerritorial.BAIRRO_IDENTIFICADO) assertEquals(bairro.getId(), a.bairro().id());
            else assertNull(a.bairro());
        }
    }

    @Test
    void migracaoAdicionaCamposEFazBackfillSemRemoverAssociacoes() throws Exception {
        Long comBairroExistente = criar(caju.getId(), ClassificacaoTerritorial.BAIRRO_IDENTIFICADO, bairro.getId());
        Bairro outroBairro = tx(() -> {
            Bairro novo = new Bairro();
            novo.setNome("Outro bairro exclusivamente de teste");
            em.persist(novo);
            em.flush();
            return novo;
        });
        Long comOutroBairro = criar(caju.getId(), ClassificacaoTerritorial.BAIRRO_IDENTIFICADO, outroBairro.getId());
        Long semBairro = criar();

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE arvores DROP COLUMN bairro_nome");
            statement.execute("ALTER TABLE arvores DROP COLUMN cep");
            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("db/manual/territorio-arvore-bairro-cep.sql"));
        }

        assertTerritoryRow(comBairroExistente, bairro.getId(), "Bairro exclusivamente de teste", null);
        assertTerritoryRow(comOutroBairro, outroBairro.getId(), "Outro bairro exclusivamente de teste", null);
        assertTerritoryRow(semBairro, null, null, null);

        var fichaLegada = service.buscarPorId(comBairroExistente);
        assertEquals(bairro.getId(), fichaLegada.bairro().id());
        assertEquals("Bairro exclusivamente de teste", fichaLegada.bairroNome());
        assertNull(fichaLegada.cep());

        tx(() -> {
            em.clear();
            assertEquals(bairro.getId(), em.find(Arvore.class, comBairroExistente).getBairro().getId());
            assertEquals(outroBairro.getId(), em.find(Arvore.class, comOutroBairro).getBairro().getId());
            assertNull(em.find(Arvore.class, semBairro).getBairro());
            return null;
        });
    }

    @Test
    void putSemBairroNemCepLimpaCamposTerritoriaisOpcionais() {
        Long id = criarTextual(caju.getId(), ClassificacaoTerritorial.BAIRRO_IDENTIFICADO,
                "Bairro temporário de teste", "06390-000");
        var dto = new ArvoreAtualizacaoRequestDTO(caju.getId(), new BigDecimal("-4.98"),
                new BigDecimal("-39.02"), ClassificacaoTerritorial.BAIRRO_NAO_IDENTIFICADO,
                null, null, null, null, null, CondicaoFitossanitaria.NAO_AVALIADA, null);

        var resposta = service.atualizarArvore(id, dto);
        Arvore persistida = tx(() -> em.find(Arvore.class, id));

        assertEquals(ClassificacaoTerritorial.BAIRRO_NAO_IDENTIFICADO, resposta.classificacaoTerritorial());
        assertNull(resposta.bairro());
        assertNull(resposta.bairroNome());
        assertNull(resposta.cep());
        assertNull(persistida.getBairro());
        assertNull(persistida.getBairroNome());
        assertNull(persistida.getCep());
    }

    @Test
    void fluxoHttpTerritorialPersisteConsultaEditaEFiltraComPostgres() throws Exception {
        String cadastro = """
                {"especieId":%d,"latitude":-4.97,"longitude":-39.01,
                 "classificacaoTerritorial":"BAIRRO_IDENTIFICADO",
                 "bairroNome":"Centro inicial","cep":"06390-000",
                 "condicaoFitossanitaria":"NAO_AVALIADA"}
                """.formatted(caju.getId());

        var respostaCriacao = http.perform(post("/api/arvores")
                        .contentType(APPLICATION_JSON).content(cadastro))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bairroNome").value("Centro inicial"))
                .andExpect(jsonPath("$.cep").value("06390-000"))
                .andReturn();
        long id = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(respostaCriacao.getResponse().getContentAsString()).get("id").asLong();

        http.perform(get("/api/arvores/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bairroNome").value("Centro inicial"));

        String atualizacao = """
                {"especieId":%d,"latitude":-4.98,"longitude":-39.02,
                 "classificacaoTerritorial":"BAIRRO_IDENTIFICADO",
                 "bairroNome":"Centro corrigido","cep":null,
                 "condicaoFitossanitaria":"NAO_AVALIADA"}
                """.formatted(caju.getId());
        http.perform(put("/api/arvores/{id}", id)
                        .contentType(APPLICATION_JSON).content(atualizacao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bairroNome").value("Centro corrigido"))
                .andExpect(jsonPath("$.cep").doesNotExist());

        http.perform(get("/api/arvores")
                        .param("search", "QXD")
                        .param("bairro", "corrigido")
                        .param("especieId", caju.getId().toString())
                        .param("condicao", "NAO_AVALIADA")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id))
                .andExpect(jsonPath("$.content[0].bairroNome").value("Centro corrigido"));

        http.perform(get("/api/arvores/mapa")
                        .param("search", "QXD")
                        .param("bairro", "corrigido")
                        .param("especieId", caju.getId().toString())
                        .param("condicao", "NAO_AVALIADA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].bairroNome").value("Centro corrigido"));

        String semTerritorio = """
                {"especieId":%d,"latitude":-4.97,"longitude":-39.01,
                 "classificacaoTerritorial":"BAIRRO_NAO_IDENTIFICADO",
                 "bairroNome":null,"cep":null,
                 "condicaoFitossanitaria":"NAO_AVALIADA"}
                """.formatted(caju.getId());
        http.perform(post("/api/arvores")
                        .contentType(APPLICATION_JSON).content(semTerritorio))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bairroNome").doesNotExist())
                .andExpect(jsonPath("$.cep").doesNotExist())
                .andExpect(jsonPath("$.classificacaoTerritorial").value("BAIRRO_NAO_IDENTIFICADO"));
    }

    @Test
    void migracaoNaoSubstituiNomeTextualEDeixaCepDesconhecidoNulo() throws Exception {
        Long nomeConfirmado = criar(caju.getId(), ClassificacaoTerritorial.BAIRRO_IDENTIFICADO, bairro.getId());
        Long nomeAusente = criar(caju.getId(), ClassificacaoTerritorial.BAIRRO_IDENTIFICADO, bairro.getId());
        tx(() -> {
            Arvore arvore = em.find(Arvore.class, nomeConfirmado);
            arvore.setBairroNome("Nome corrigido pelo operador");
            return null;
        });

        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("db/manual/territorio-arvore-bairro-cep.sql"));
        }

        assertTerritoryRow(nomeConfirmado, bairro.getId(), "Nome corrigido pelo operador", null);
        assertTerritoryRow(nomeAusente, bairro.getId(), "Bairro exclusivamente de teste", null);
    }

    private void assertTerritoryRow(Long arvoreId, Long bairroId, String bairroNome, String cep) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT bairro_id, bairro_nome, cep FROM arvores WHERE id = ?")) {
            statement.setLong(1, arvoreId);
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                long actualBairroId = result.getLong("bairro_id");
                assertEquals(bairroId == null ? null : bairroId.longValue(),
                        result.wasNull() ? null : actualBairroId);
                assertEquals(bairroNome, result.getString("bairro_nome"));
                assertEquals(cep, result.getString("cep"));
                assertFalse(result.next());
            }
        }
    }

    @Test
    void pesquisaParcialPorIdExecutaCastRealSemConverterTextoParaNumero() {
        var ids = new ArrayList<Long>(); for (int i=0;i<12;i++) ids.add(criar());
        String termo = ids.getLast().toString().substring(0,1);
        assertEquals(ids.stream().filter(id -> id.toString().contains(termo)).toList(), mapa(termo,null,null,null));
        assertTrue(mapa("texto não numérico",null,null,null).isEmpty());
        assertTrue(InventarioPersistenceTestConfig.SqlCapture.current().stream()
                .anyMatch(sql -> sql.toLowerCase(Locale.ROOT).contains("cast(") && sql.contains("varchar")));
    }

    @Test
    void pesquisaParcialPorCodigoPreservaIdEFormatoAposAtualizacao() {
        Long id = criar();
        String codigo = service.buscarPorId(id).codigo();

        assertEquals(List.of(id), mapa(" " + codigo.toLowerCase(Locale.ROOT).substring(0, 12) + " ", null, null, null));
        assertEquals(codigo, service.atualizarArvore(id, update(caju.getId(),
                ClassificacaoTerritorial.BAIRRO_NAO_IDENTIFICADO, null,
                CondicaoFitossanitaria.NAO_AVALIADA, null)).codigo());
        assertEquals(codigo, service.listar(null, null, null, null,
                PageRequest.of(0, 20)).getContent().getFirst().codigo());
    }

    @Test
    void pesquisaPopularCientificaNormalizaCaixaEspacosECuringasLiterais() {
        Long a = criar(); Long b = criar(literal.getId(), ClassificacaoTerritorial.FORA_DA_AREA_URBANA, null);
        assertEquals(List.of(a), mapa("  CAJUEIRO  ",null,null,null));
        assertEquals(List.of(a), mapa("anacARDium",null,null,null));
        assertEquals(List.of(b), mapa("%_",null,null,null));
        assertEquals(List.of(b), mapa("100%_literal",null,null,null));
        assertEquals(List.of(a,b), mapa("   ",null,null,null));
        assertTrue(mapa("inexistente",null,null,null).isEmpty());
    }

    @Test
    void filtrosIndividuaisCombinadosEPesquisaUsamIntersecao() {
        Long a = criar(caju.getId(), ClassificacaoTerritorial.BAIRRO_IDENTIFICADO, bairro.getId());
        Long b = criar(literal.getId(), ClassificacaoTerritorial.BAIRRO_NAO_IDENTIFICADO, null);
        Long c = criar(); service.atualizarArvore(c, update(caju.getId(),ClassificacaoTerritorial.BAIRRO_NAO_IDENTIFICADO,null,CondicaoFitossanitaria.MORTA,null));
        assertEquals(List.of(a), mapa(null,bairro.getId(),null,null));
        assertEquals(List.of(a,c), mapa(null,null,caju.getId(),null));
        assertEquals(List.of(c), mapa(null,null,null,CondicaoFitossanitaria.MORTA));
        assertEquals(List.of(a), mapa(null,bairro.getId(),caju.getId(),CondicaoFitossanitaria.NAO_AVALIADA));
        assertEquals(List.of(a), mapa("cajueiro",bairro.getId(),caju.getId(),null));
        assertTrue(mapa("cajueiro",null,literal.getId(),null).isEmpty());
        assertEquals(List.of(b), mapa(null,null,literal.getId(),null));
    }

    @Test
    void paginacaoContaEOrdenaSemAlterarUniversoDoMapa() {
        var ids = new ArrayList<Long>(); for (int i=0;i<5;i++) ids.add(criar());
        var primeira = service.listar(null,null,null,null,PageRequest.of(0,2,Sort.by(Sort.Direction.DESC,"id")));
        var segunda = service.listar(null,null,null,null,PageRequest.of(1,2));
        var terceira = service.listar(null,null,null,null,PageRequest.of(2,2));
        assertEquals(5, primeira.getTotalElements()); assertEquals(3, primeira.getTotalPages());
        assertEquals(ids.subList(0,2), primeira.stream().map(a->a.id()).toList());
        assertEquals(ids.subList(2,4), segunda.stream().map(a->a.id()).toList());
        assertEquals(ids.subList(4,5), terceira.stream().map(a->a.id()).toList());
        assertEquals(ids, mapa(null,null,null,null));
        assertTrue(InventarioPersistenceTestConfig.SqlCapture.current().stream().anyMatch(sql->sql.toLowerCase(Locale.ROOT).contains("count(")));
    }

    @Test
    void grafoCarregaEspecieEBairroSemColecoesDeDocumentosOuNMaisUm() {
        criar(caju.getId(),ClassificacaoTerritorial.BAIRRO_IDENTIFICADO,bairro.getId()); criar();
        tx(() -> {
            em.clear();
            var pagina = arvores.findAll(ArvoreSpecifications.filtrar(null,null,null,null),PageRequest.of(0,10));
            for (var a : pagina) {
                assertTrue(emf.getPersistenceUnitUtil().isLoaded(a,"especie"));
                assertTrue(emf.getPersistenceUnitUtil().isLoaded(a,"bairro"));
                assertFalse(emf.getPersistenceUnitUtil().isLoaded(a.getEspecie(),"documents"));
            }
            return null;
        });
        var stats = emf.unwrap(SessionFactory.class).getStatistics(); stats.clear();
        InventarioPersistenceTestConfig.SqlCapture.clearCurrent();
        service.consultarMapa(null,null,null,null);
        assertEquals(2, stats.getPrepareStatementCount()); assertEquals(0, stats.getCollectionFetchCount());
        assertEquals(0, stats.getEntityFetchCount());
    }

    @Test
    void resumoEscolheFotoDeMenorIdEmConsultaEmLoteSemNMaisUm() {
        Long comFotos = criar();
        Long semFoto = criar();
        var primeira = photoService.adicionar(comFotos, arquivo());
        photoService.adicionar(comFotos, arquivo());
        InventarioPersistenceTestConfig.SqlCapture.clearCurrent();
        var stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();

        var resultados = service.consultarMapa(null, null, null, null);

        var comCapa = resultados.stream().filter(a -> a.id().equals(comFotos)).findFirst().orElseThrow();
        var semCapa = resultados.stream().filter(a -> a.id().equals(semFoto)).findFirst().orElseThrow();
        assertEquals(primeira.id(), comCapa.fotoCapa().id());
        assertEquals("image/png", comCapa.fotoCapa().contentType());
        assertNull(semCapa.fotoCapa());
        assertEquals(2, stats.getPrepareStatementCount());
        assertEquals(0, stats.getCollectionFetchCount());
        assertEquals(0, stats.getEntityFetchCount());
    }

    @Test
    void putPreservaIdCriacaoAtualizaCallbackELimpaOpcionais() {
        Long id = service.criarArvore(new ArvoreCadastroRequestDTO(caju.getId(),BigDecimal.ZERO,BigDecimal.ONE,
                ClassificacaoTerritorial.BAIRRO_IDENTIFICADO,bairro.getId(),"Referência",java.time.LocalDate.of(2020,1,1),
                BigDecimal.TEN,BigDecimal.ONE,null,"Observações")).id();
        var antes=service.buscarPorId(id);
        var resposta=service.atualizarArvore(id,update(literal.getId(),ClassificacaoTerritorial.FORA_DA_AREA_URBANA,null,CondicaoFitossanitaria.MORTA,null));
        var depois=service.buscarPorId(id);
        assertEquals(id,depois.id()); assertEquals(antes.criadoEm(),depois.criadoEm());
        assertTrue(depois.atualizadoEm().isAfter(antes.atualizadoEm()));
        assertNotNull(resposta.atualizadoEm()); assertTrue(resposta.atualizadoEm().isAfter(antes.atualizadoEm()));
        assertEquals(literal.getId(),depois.especie().id()); assertNull(depois.bairro());
        assertEquals(ClassificacaoTerritorial.FORA_DA_AREA_URBANA,depois.classificacaoTerritorial());
        assertNull(depois.dapCm()); assertNull(depois.alturaEstimadaM()); assertNull(depois.dataPlantio());
        assertNull(depois.referenciaLocalizacao()); assertNull(depois.observacoes());
        assertEquals(CondicaoFitossanitaria.MORTA,depois.condicaoFitossanitaria());
        assertEquals(0,new BigDecimal("-4.98").compareTo(depois.latitude()));
        service.atualizarArvore(id,update(literal.getId(),ClassificacaoTerritorial.BAIRRO_IDENTIFICADO,bairro.getId(),CondicaoFitossanitaria.MORTA,null));
        assertEquals(bairro.getId(),service.buscarPorId(id).bairro().id());
        service.atualizarArvore(id,update(literal.getId(),ClassificacaoTerritorial.BAIRRO_NAO_IDENTIFICADO,null,CondicaoFitossanitaria.MORTA,null));
        assertNull(service.buscarPorId(id).bairro());
        assertEquals(ClassificacaoTerritorial.BAIRRO_NAO_IDENTIFICADO,service.buscarPorId(id).classificacaoTerritorial());
    }

    @Test
    void rollbackRealDesfazAtualizacaoMesmoDepoisDoFlush() {
        Long id=criar(); var antes=service.buscarPorId(id);
        assertThrows(IllegalStateException.class,()->tx(()-> {
            service.atualizarArvore(id,update(literal.getId(),ClassificacaoTerritorial.BAIRRO_NAO_IDENTIFICADO,null,CondicaoFitossanitaria.MORTA,"Não deve persistir"));
            em.flush(); throw new IllegalStateException("Rollback de teste");
        }));
        var depois=service.buscarPorId(id);
        assertEquals(antes.especie().id(),depois.especie().id()); assertEquals(antes.atualizadoEm(),depois.atualizadoEm());
        assertEquals(antes.condicaoFitossanitaria(),depois.condicaoFitossanitaria()); assertNull(depois.observacoes());
    }

    @Test
    void entradasInvalidasEspecieERegistroInexistentesNaoPersistem() {
        assertThrows(EntityNotFoundException.class,()->criar(Long.MAX_VALUE,ClassificacaoTerritorial.BAIRRO_NAO_IDENTIFICADO,null));
        assertThrows(EntityNotFoundException.class,()->service.buscarPorId(Long.MAX_VALUE));
        for (var dto : List.of(new ArvoreCadastroRequestDTO(caju.getId(),new BigDecimal("91"),BigDecimal.ZERO,
                ClassificacaoTerritorial.BAIRRO_NAO_IDENTIFICADO,null,null,null,null,null,null,null),
                new ArvoreCadastroRequestDTO(caju.getId(),BigDecimal.ZERO,BigDecimal.ZERO,
                ClassificacaoTerritorial.BAIRRO_NAO_IDENTIFICADO,null,null,null,BigDecimal.ZERO,new BigDecimal("-1"),null,null))) {
            assertThrows(ConstraintViolationException.class,()->service.criarArvore(dto));
        }
        assertTrue(mapa(null,null,null,null).isEmpty());
        for (String lat : List.of("-90","90")) for (String lon : List.of("-180","180")) {
            assertNotNull(service.criarArvore(new ArvoreCadastroRequestDTO(caju.getId(),new BigDecimal(lat),new BigDecimal(lon),
                    ClassificacaoTerritorial.BAIRRO_NAO_IDENTIFICADO,null,null,null,null,null,null,null)).id());
        }
    }

    @Test
    void metadadosDeFotografiasPersistemComCallbackPropriedadeEContagem() {
        Long id=criar(); Long outra=criar();
        var foto=photoService.adicionar(id,arquivo()); assertNotNull(foto.id()); assertNotNull(foto.criadoEm());
        assertEquals(id,foto.arvoreId()); assertEquals("image/png",foto.contentType()); assertEquals(png.length,foto.sizeBytes());
        assertEquals(1,tx(()->fotos.countByArvoreId(id)));
        assertTrue(tx(()->fotos.findByIdAndArvoreId(foto.id(),outra)).isEmpty());
        assertThrows(EntityNotFoundException.class,()->photoService.recuperar(outra,foto.id()));
        assertThrows(EntityNotFoundException.class,()->photoService.remover(outra,foto.id()));
        assertArrayEquals(png,photoService.recuperar(id,foto.id()).conteudo());
        assertEquals(1,photoService.listar(id).size()); assertTrue(photoService.listar(outra).isEmpty());
    }

    @Test
    void removerMetadadosPreservaArvoreEOutrasFotosEEdicaoNaoRemoveFotos() {
        Long id=criar(); var a=photoService.adicionar(id,arquivo()); var b=photoService.adicionar(id,arquivo());
        service.atualizarArvore(id,update(caju.getId(),ClassificacaoTerritorial.BAIRRO_NAO_IDENTIFICADO,null,CondicaoFitossanitaria.MORTA,null));
        assertEquals(2,photoService.listar(id).size()); photoService.remover(id,a.id());
        assertEquals(List.of(b.id()),photoService.listar(id).stream().map(f->f.id()).toList());
        assertEquals(id,service.buscarPorId(id).id());
    }

    @Test
    void chaveEstrangeiraImpedeExcluirArvoreReferenciadaSemCascata() {
        Long id=criar();photoService.adicionar(id,arquivo());
        assertThrows(RuntimeException.class,()->tx(()-> {em.remove(em.find(Arvore.class,id));em.flush();return null;}));
        assertEquals(id,service.buscarPorId(id).id()); assertEquals(1,photoService.listar(id).size());
    }

    @Test
    void rollbackRealDeMetadadosAcionaCompensacaoSomenteNoStorageSimulado() {
        Long id=criar();
        assertThrows(IllegalStateException.class,()->tx(()-> {
            photoService.adicionar(id,arquivo());throw new IllegalStateException("Rollback de teste");
        }));
        assertTrue(photoService.listar(id).isEmpty()); verify(storage).remover(anyString());
    }

    @Test
    void duasTransacoesReaisConcorrentesNaoUltrapassamTresFotos() throws Exception {
        Long id=criar();photoService.adicionar(id,arquivo());photoService.adicionar(id,arquivo());
        clearInvocations(storage);
        var entrouNoStorage=new CountDownLatch(1);var liberarPrimeira=new CountDownLatch(1);
        doAnswer(inv -> {
            entrouNoStorage.countDown();
            if (!liberarPrimeira.await(10,TimeUnit.SECONDS)) throw new IllegalStateException("Timeout de teste");
            return null;
        }).when(storage).enviar(anyString(),any(),anyString());
        var workers=Executors.newFixedThreadPool(2);
        try {
            var primeira=workers.submit(()->photoService.adicionar(id,arquivo()));
            assertTrue(entrouNoStorage.await(5,TimeUnit.SECONDS));
            var segunda=workers.submit(()-> {
                try {photoService.adicionar(id,arquivo());return false;}
                catch (EntradaInventarioInvalidaException esperado) {return true;}
            });
            boolean bloqueada=false;long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
            try (var c=dataSource.getConnection();var s=c.createStatement()) {
                while (System.nanoTime()<deadline) {
                    try (var rs=s.executeQuery("select count(*) from pg_stat_activity where datname=current_database() "
                            +"and wait_event_type='Lock' and cardinality(pg_blocking_pids(pid)) > 0")) {
                        rs.next();if(rs.getInt(1)>0) {bloqueada=true;break;}
                    }
                    Thread.sleep(10);
                }
            }
            assertTrue(bloqueada,"A segunda transação deve aguardar bloqueio real no PostgreSQL");
            liberarPrimeira.countDown();assertNotNull(primeira.get(10,TimeUnit.SECONDS));assertTrue(segunda.get(10,TimeUnit.SECONDS));
            assertEquals(3,photoService.listar(id).size()); verify(storage,times(1)).enviar(anyString(),any(),anyString());
        } finally {liberarPrimeira.countDown();workers.shutdownNow();assertTrue(workers.awaitTermination(10,TimeUnit.SECONDS));}
    }

    @AfterAll
    static void salvarSqlExecutado() throws Exception {
        var output=Path.of("target/inventario-it-sql.log");Files.createDirectories(output.getParent());
        Files.write(output,InventarioPersistenceTestConfig.SqlCapture.all());
    }
}
