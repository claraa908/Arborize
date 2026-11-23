package br.com.arborize.migrations;

import br.com.arborize.dtos.request.MovimentacaoMudaRequestDTO;
import br.com.arborize.entities.Especie;
import br.com.arborize.enums.TipoMovimentacaoMuda;
import br.com.arborize.exceptions.EstoqueInsuficienteException;
import br.com.arborize.repositories.EspecieRepository;
import br.com.arborize.repositories.MovimentacaoMudaRepository;
import br.com.arborize.services.MovimentacaoMudaService;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EnabledIfEnvironmentVariable(named = "ARBORIZE_MIGRATION_TEST_URL", matches = ".+")
class FlywaySchemaValidationPostgresTest {
    private static final String SCHEMA = "arborize_validate_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

    @Autowired
    private Flyway flyway;

    @Autowired
    private MovimentacaoMudaService service;

    @Autowired
    private EspecieRepository especies;

    @Autowired
    private MovimentacaoMudaRepository movimentacoes;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void limparDadosDeTestesAnteriores() {
        jdbc.update("DELETE FROM movimentacoes_mudas");
        jdbc.update("DELETE FROM especies");
    }

    @DynamicPropertySource
    static void configurePostgresSchema(DynamicPropertyRegistry registry) throws Exception {
        String url = System.getenv("ARBORIZE_MIGRATION_TEST_URL");
        String user = System.getenv("ARBORIZE_MIGRATION_TEST_USER");
        String password = System.getenv("ARBORIZE_MIGRATION_TEST_PASSWORD");
        try (Connection connection = new DriverManagerDataSource(url, user, password).getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + SCHEMA);
        }
        String schemaUrl = url + (url.contains("?") ? "&" : "?") + "currentSchema=" + SCHEMA;
        registry.add("spring.datasource.url", () -> schemaUrl);
        registry.add("spring.datasource.username", () -> user);
        registry.add("spring.datasource.password", () -> password);
        registry.add("spring.flyway.schemas", () -> SCHEMA);
        registry.add("spring.flyway.default-schema", () -> SCHEMA);
        registry.add("spring.flyway.create-schemas", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Test
    void flywayAplicaAsMigrationsAntesDeHibernateValidarOModelo() {
        assertEquals("5", flyway.info().current().getVersion().getVersion());
    }

    @Test
    void bloqueioDeEspecieImpedeDuasSaidasConcorrentesDeConsumiremMesmoSaldo() throws Exception {
        Especie especie = criarEspecie(10);
        CountDownLatch prontas = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> primeira = executor.submit(() -> tentarSaida(especie.getId(), prontas, iniciar));
            Future<Boolean> segunda = executor.submit(() -> tentarSaida(especie.getId(), prontas, iniciar));
            org.junit.jupiter.api.Assertions.assertTrue(prontas.await(5, TimeUnit.SECONDS));
            iniciar.countDown();

            boolean resultadoUm = primeira.get(15, TimeUnit.SECONDS);
            boolean resultadoDois = segunda.get(15, TimeUnit.SECONDS);
            org.junit.jupiter.api.Assertions.assertNotEquals(resultadoUm, resultadoDois);
            assertEquals(2, especies.findById(especie.getId()).orElseThrow().getMudasDisponiveis());
            assertEquals(1L, contarMovimentacoes(especie.getId()));
        } finally {
            iniciar.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void falhaAoSalvarMovimentacaoReverteEstoqueETambemPreservaEdicaoAnterior() {
        Especie especie = criarEspecie(10);
        MovimentacaoMudaRequestDTO entradaInvalida = request(
                TipoMovimentacaoMuda.ENTRADA_GERMINACAO, especie.getId(), 5);
        entradaInvalida.setDestino("x".repeat(181));
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> service.criar(entradaInvalida));
        assertEquals(10, especies.findById(especie.getId()).orElseThrow().getMudasDisponiveis());
        assertEquals(0L, contarMovimentacoes(especie.getId()));

        MovimentacaoMudaRequestDTO saida = request(TipoMovimentacaoMuda.SAIDA_PLANTIO, especie.getId(), 2);
        saida.setDestino("Praça de teste");
        var criada = service.criar(saida);
        assertEquals(8, especies.findById(especie.getId()).orElseThrow().getMudasDisponiveis());

        MovimentacaoMudaRequestDTO edicaoInvalida = request(TipoMovimentacaoMuda.SAIDA_PLANTIO, especie.getId(), 3);
        edicaoInvalida.setDestino("x".repeat(181));
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> service.atualizar(criada.id(), edicaoInvalida));

        assertEquals(8, especies.findById(especie.getId()).orElseThrow().getMudasDisponiveis());
        assertEquals(2, movimentacoes.findById(criada.id()).orElseThrow().getQuantidade());
        assertEquals("Praça de teste", movimentacoes.findById(criada.id()).orElseThrow().getDestino());
    }

    @Test
    void filtrosPaginacaoEResumoMensalUsamDadosPersistidos() {
        Especie especieUm = criarEspecie(20);
        Especie especieDois = criarEspecie(20);
        LocalDate hoje = LocalDate.now(ZoneId.of("America/Fortaleza"));
        LocalDate primeiroDiaDoMes = hoje.withDayOfMonth(1);
        LocalDate outroDiaDoMes = hoje.getDayOfMonth() > 1 ? hoje.minusDays(1) : hoje;

        service.criar(requestComDestino(TipoMovimentacaoMuda.SAIDA_PLANTIO, especieUm.getId(), 2,
                outroDiaDoMes, "Praça Cedro"));
        service.criar(requestComDestino(TipoMovimentacaoMuda.SAIDA_PLANTIO, especieUm.getId(), 3,
                hoje, "Praça Acácia"));
        service.criar(request(TipoMovimentacaoMuda.BAIXA_PERDA, especieUm.getId(), 4, hoje));
        service.criar(requestComDestino(TipoMovimentacaoMuda.SAIDA_PLANTIO, especieUm.getId(), 1,
                primeiroDiaDoMes.minusDays(1), "Praça fora do mês"));
        service.criar(requestComDestino(TipoMovimentacaoMuda.SAIDA_PLANTIO, especieDois.getId(), 5,
                hoje, "Praça de outra espécie"));

        var paginaUm = service.listar(TipoMovimentacaoMuda.SAIDA_PLANTIO, especieUm.getId(),
                primeiroDiaDoMes, hoje, "praça", PageRequest.of(0, 1,
                        Sort.by(Sort.Order.desc("data"), Sort.Order.desc("id"))));
        var paginaDois = service.listar(TipoMovimentacaoMuda.SAIDA_PLANTIO, especieUm.getId(),
                primeiroDiaDoMes, hoje, "praça", PageRequest.of(1, 1,
                        Sort.by(Sort.Order.desc("data"), Sort.Order.desc("id"))));

        assertEquals(2, paginaUm.getTotalElements());
        assertEquals(1, paginaUm.getContent().size());
        assertEquals(0, paginaUm.getNumber());
        assertEquals(2, paginaUm.getTotalPages());
        assertEquals(hoje, paginaUm.getContent().getFirst().data());
        assertEquals(outroDiaDoMes, paginaDois.getContent().getFirst().data());
        assertEquals(10L, service.resumo().saidasNoMes());
        assertEquals(4L, service.resumo().perdasNoMes());
    }

    private boolean tentarSaida(Long especieId, CountDownLatch prontas, CountDownLatch iniciar) throws Exception {
        prontas.countDown();
        org.junit.jupiter.api.Assertions.assertTrue(iniciar.await(10, TimeUnit.SECONDS));
        try {
            service.criar(request(TipoMovimentacaoMuda.SAIDA_PLANTIO, especieId, 8));
            return true;
        } catch (EstoqueInsuficienteException estoqueInsuficiente) {
            return false;
        }
    }

    private Especie criarEspecie(int estoque) {
        Especie especie = new Especie();
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        especie.setNomePopular("Espécie teste " + sufixo);
        especie.setNomeCientifico("Species test " + sufixo);
        especie.setCategoria("Nativa");
        especie.setPorteArvore("Médio");
        especie.setRecomendadaCalcadas(Boolean.TRUE);
        especie.setMudasDisponiveis(estoque);
        return especies.saveAndFlush(especie);
    }

    private MovimentacaoMudaRequestDTO request(TipoMovimentacaoMuda tipo, Long especieId, int quantidade) {
        return request(tipo, especieId, quantidade, LocalDate.now(ZoneId.of("America/Fortaleza")));
    }

    private MovimentacaoMudaRequestDTO request(TipoMovimentacaoMuda tipo, Long especieId, int quantidade, LocalDate data) {
        MovimentacaoMudaRequestDTO request = new MovimentacaoMudaRequestDTO();
        request.setTipo(tipo);
        request.setEspecieId(especieId);
        request.setQuantidade(quantidade);
        request.setData(data);
        if (tipo == TipoMovimentacaoMuda.SAIDA_PLANTIO) request.setDestino("Destino de teste");
        return request;
    }

    private MovimentacaoMudaRequestDTO requestComDestino(
            TipoMovimentacaoMuda tipo, Long especieId, int quantidade, LocalDate data, String destino) {
        MovimentacaoMudaRequestDTO request = request(tipo, especieId, quantidade, data);
        request.setDestino(destino);
        return request;
    }

    private long contarMovimentacoes(Long especieId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM movimentacoes_mudas WHERE especie_id = ?", Long.class, especieId);
    }

    @AfterAll
    static void removerSchemaDeTeste() throws Exception {
        String url = System.getenv("ARBORIZE_MIGRATION_TEST_URL");
        String user = System.getenv("ARBORIZE_MIGRATION_TEST_USER");
        String password = System.getenv("ARBORIZE_MIGRATION_TEST_PASSWORD");
        if (url == null || user == null || password == null) return;
        DataSource dataSource = new DriverManagerDataSource(url, user, password);
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
        }
    }
}
