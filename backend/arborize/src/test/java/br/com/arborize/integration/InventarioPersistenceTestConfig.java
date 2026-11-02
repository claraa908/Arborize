package br.com.arborize.integration;

import br.com.arborize.mappers.ArvoreMapper;
import br.com.arborize.repositories.ArvoreRepository;
import br.com.arborize.services.ArvoreService;
import br.com.arborize.services.FotoArvoreService;
import br.com.arborize.storage.FotoArvoreStorage;
import br.com.arborize.validation.FotoArvoreValidator;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.springframework.context.annotation.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;


/** Contexto opt-in: não importa application*.properties, MinioConfig, SecurityConfig ou servidor HTTP. */
@TestConfiguration(proxyBeanMethods = false)
@EnableJpaRepositories(basePackageClasses = ArvoreRepository.class)
@EnableTransactionManagement
@Import({ArvoreService.class, ArvoreMapper.class, FotoArvoreService.class})
class InventarioPersistenceTestConfig {
    static final String PROFILE = "inventario-persistencia-it";

    @Bean
    DataSource dataSource(Environment environment) {
        String url = System.getenv("ARBORIZE_IT_JDBC_URL");
        String user = System.getenv("ARBORIZE_IT_DB_USER");
        String password = System.getenv("ARBORIZE_IT_DB_PASSWORD");
        if (!environment.matchesProfiles(PROFILE)
                || !"true".equals(System.getenv("ARBORIZE_IT_EPHEMERAL"))
                || url == null || !url.matches("jdbc:postgresql://127\\.0\\.0\\.1:[0-9]{4,5}/arborize_it_[0-9a-f]{12}")
                || !"postgres".equals(user) || password == null || password.isBlank()) {
            throw new IllegalStateException("Teste exige PostgreSQL efêmero exclusivo e perfil explícito; não há datasource padrão.");
        }
        int port = Integer.parseInt(url.substring(url.indexOf("127.0.0.1:") + 10, url.lastIndexOf('/')));
        if (port <= 1024 || port == 5432 || port == 5433) {
            throw new IllegalStateException("Teste exige porta dinâmica diferente das portas normais da aplicação.");
        }
        return new DriverManagerDataSource(url, user, password);
    }

    @Bean
    LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        var factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setPackagesToScan("br.com.arborize.entities");
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        String action = System.getProperty("arborize.it.schema-action", "create-drop");
        if (!Set.of("create-drop", "validate").contains(action)) throw new IllegalStateException("Schema de teste inválido.");
        factory.setJpaPropertyMap(Map.of(
                "hibernate.hbm2ddl.auto", action,
                "hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect",
                "hibernate.generate_statistics", "true",
                "hibernate.session_factory.statement_inspector", new SqlCapture()));
        return factory;
    }

    @Bean
    PlatformTransactionManager transactionManager(EntityManagerFactory factory) {
        return new JpaTransactionManager(factory);
    }

    @Bean
    LocalValidatorFactoryBean validator() {
        var validator = new LocalValidatorFactoryBean();
        validator.setMessageInterpolator(new ParameterMessageInterpolator());
        return validator;
    }

    @Bean
    FotoArvoreValidator fotoArvoreValidator() { return new FotoArvoreValidator(25_000_000); }


    public static class SqlCapture implements StatementInspector {
        private static final List<String> ALL = new CopyOnWriteArrayList<>();
        private static final List<String> CURRENT = new CopyOnWriteArrayList<>();
        @Override
        public String inspect(String sql) { ALL.add(sql); CURRENT.add(sql); return sql; }
        static List<String> all() { return List.copyOf(ALL); }
        static List<String> current() { return List.copyOf(CURRENT); }
        static void clearCurrent() { CURRENT.clear(); }
    }
}
