package br.com.arborize;

import br.com.arborize.migrations.PostgresTestSchema;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "ARBORIZE_MIGRATION_TEST_URL", matches = ".+")
class ArborizeApplicationTests {
	private static String schema;

	@DynamicPropertySource
	static void configurePostgres(DynamicPropertyRegistry registry) throws Exception {
		schema = PostgresTestSchema.configure(registry, "arborize_app_test_");
	}

	@AfterAll
	static void removeSchema() throws Exception {
		PostgresTestSchema.remove(schema);
	}

	@Test
	void contextLoads() {
	}

}
