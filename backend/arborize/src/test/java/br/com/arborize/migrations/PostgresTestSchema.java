package br.com.arborize.migrations;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.sql.Connection;
import java.sql.Statement;
import java.util.UUID;

public final class PostgresTestSchema {
    private PostgresTestSchema() { }

    public static String configure(DynamicPropertyRegistry registry, String prefix) throws Exception {
        String url = System.getenv("ARBORIZE_MIGRATION_TEST_URL");
        String user = System.getenv("ARBORIZE_MIGRATION_TEST_USER");
        String password = System.getenv("ARBORIZE_MIGRATION_TEST_PASSWORD");
        String schema = prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        try (Connection connection = new DriverManagerDataSource(url, user, password).getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
        }

        String schemaUrl = url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema;
        registry.add("spring.datasource.url", () -> schemaUrl);
        registry.add("spring.datasource.username", () -> user);
        registry.add("spring.datasource.password", () -> password);
        registry.add("spring.flyway.schemas", () -> schema);
        registry.add("spring.flyway.default-schema", () -> schema);
        registry.add("spring.flyway.create-schemas", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        return schema;
    }

    public static void remove(String schema) throws Exception {
        if (schema == null || schema.isBlank()) return;
        String url = System.getenv("ARBORIZE_MIGRATION_TEST_URL");
        String user = System.getenv("ARBORIZE_MIGRATION_TEST_USER");
        String password = System.getenv("ARBORIZE_MIGRATION_TEST_PASSWORD");
        if (url == null || user == null || password == null) return;

        try (Connection connection = new DriverManagerDataSource(url, user, password).getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        }
    }
}
