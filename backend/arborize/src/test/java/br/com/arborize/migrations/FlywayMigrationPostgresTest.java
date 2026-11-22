package br.com.arborize.migrations;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class FlywayMigrationPostgresTest {
    @Test
    void migrationsPostgresCriamBancoLimpoEBaselinePreservaSaldoExistente() throws Exception {
        String url = System.getenv("ARBORIZE_MIGRATION_TEST_URL");
        String usuario = System.getenv("ARBORIZE_MIGRATION_TEST_USER");
        String senha = System.getenv("ARBORIZE_MIGRATION_TEST_PASSWORD");
        assumeTrue(url != null && !url.isBlank() && usuario != null && senha != null,
                "Configure um banco PostgreSQL descartável com ARBORIZE_MIGRATION_TEST_URL/USER/PASSWORD.");

        DataSource base = new DriverManagerDataSource(url, usuario, senha);
        String schemaLimpo = novoSchema();
        String schemaExistente = novoSchema();
        criarSchema(base, schemaLimpo);
        criarSchema(base, schemaExistente);
        try {
            DataSource limpo = porSchema(url, usuario, senha, schemaLimpo);
            Flyway flywayLimpo = flyway(limpo, schemaLimpo);
            flywayLimpo.migrate();
            assertEquals("2", flywayLimpo.info().current().getVersion().getVersion());
            assertEquals(0, contar(limpo, "movimentacoes_mudas"));
            verificarEstruturaMovimentacoes(limpo);

            DataSource existente = porSchema(url, usuario, senha, schemaExistente);
            try (Connection connection = existente.getConnection()) {
                ScriptUtils.executeSqlScript(connection,
                        new EncodedResource(new ClassPathResource("db/migration/V1__baseline.sql")));
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("""
                            INSERT INTO especies (nome_popular, nome_cientifico, categoria, porte_arvore,
                                recomendada_calcadas, mudas_disponiveis)
                            VALUES ('Ipê migrado', 'Handroanthus test', 'Nativa', 'Médio', TRUE, 37)
                            """);
                }
            }

            Flyway flywayExistente = flyway(existente, schemaExistente);
            flywayExistente.baseline();
            flywayExistente.migrate();
            assertEquals("2", flywayExistente.info().current().getVersion().getVersion());
            assertEquals(37, consultarSaldo(existente));
            assertEquals(0, contar(existente, "movimentacoes_mudas"));
        } finally {
            removerSchema(base, schemaLimpo);
            removerSchema(base, schemaExistente);
        }
    }

    private Flyway flyway(DataSource dataSource, String schema) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .schemas(schema)
                .defaultSchema(schema)
                .baselineOnMigrate(false)
                .load();
    }

    private DataSource porSchema(String url, String usuario, String senha, String schema) {
        String separador = url.contains("?") ? "&" : "?";
        return new DriverManagerDataSource(url + separador + "currentSchema=" + schema, usuario, senha);
    }

    private void criarSchema(DataSource dataSource, String schema) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
        }
    }

    private void removerSchema(DataSource dataSource, String schema) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        }
    }

    private int contar(DataSource dataSource, String tabela) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();
             var result = statement.executeQuery("SELECT count(*) FROM " + tabela)) {
            result.next();
            return result.getInt(1);
        }
    }

    private int consultarSaldo(DataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();
             var result = statement.executeQuery("SELECT mudas_disponiveis FROM especies WHERE nome_popular = 'Ipê migrado'")) {
            assertNotNull(result);
            result.next();
            return result.getInt(1);
        }
    }

    private void verificarEstruturaMovimentacoes(DataSource dataSource) throws Exception {
        Set<String> indicesEsperados = Set.of(
                "idx_movimentacao_data", "idx_movimentacao_especie", "idx_movimentacao_tipo");
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            try (var result = statement.executeQuery("""
                    SELECT indexname FROM pg_indexes
                    WHERE schemaname = current_schema() AND tablename = 'movimentacoes_mudas'
                    """)) {
                Set<String> indices = new java.util.HashSet<>();
                while (result.next()) indices.add(result.getString(1));
                org.junit.jupiter.api.Assertions.assertTrue(indices.containsAll(indicesEsperados));
            }

            try (var result = statement.executeQuery("""
                    SELECT contype, count(*) FROM pg_constraint
                    WHERE connamespace = current_schema()::regnamespace
                      AND conrelid = 'movimentacoes_mudas'::regclass
                    GROUP BY contype
                    """)) {
                java.util.Map<String, Long> constraints = new java.util.HashMap<>();
                while (result.next()) constraints.put(result.getString(1), result.getLong(2));
                assertEquals(2L, constraints.get("f"));
                assertEquals(2L, constraints.get("c"));
                assertEquals(1L, constraints.get("u"));
            }

            statement.executeUpdate("""
                    INSERT INTO especies (nome_popular, nome_cientifico, categoria, porte_arvore,
                        recomendada_calcadas, mudas_disponiveis)
                    VALUES ('Ipê de teste', 'Handroanthus test', 'Nativa', 'Médio', TRUE, 4)
                    """);
        }

        assertSqlState(dataSource, """
                INSERT INTO movimentacoes_mudas (tipo, quantidade, data, especie_id)
                VALUES ('ENTRADA_GERMINACAO', 0, CURRENT_DATE, 1)
                """, "23514");
        assertSqlState(dataSource, """
                INSERT INTO movimentacoes_mudas (tipo, quantidade, data, especie_id)
                VALUES ('TIPO_INVALIDO', 1, CURRENT_DATE, 1)
                """, "23514");
        assertSqlState(dataSource, """
                INSERT INTO movimentacoes_mudas (tipo, quantidade, data, especie_id)
                VALUES ('ENTRADA_GERMINACAO', 1, CURRENT_DATE, 999999)
                """, "23503");
    }

    private void assertSqlState(DataSource dataSource, String sql, String expectedSqlState) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            SQLException erro = org.junit.jupiter.api.Assertions.assertThrows(SQLException.class,
                    () -> statement.executeUpdate(sql));
            assertEquals(expectedSqlState, erro.getSQLState());
        }
    }

    private String novoSchema() {
        return "arborize_mig_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
