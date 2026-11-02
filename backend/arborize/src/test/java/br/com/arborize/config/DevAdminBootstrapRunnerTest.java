package br.com.arborize.config;

import br.com.arborize.entities.Usuario;
import br.com.arborize.enums.UsuarioRole;
import br.com.arborize.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DevAdminBootstrapRunnerTest {

    private RepositoryProbe repository;
    private PasswordEncoder passwordEncoder;
    private MockEnvironment environment;
    private String testPassword;

    @BeforeEach
    void setUp() {
        repository = new RepositoryProbe();
        passwordEncoder = new BCryptPasswordEncoder();
        environment = new MockEnvironment();
        environment.setActiveProfiles("local");
        testPassword = UUID.randomUUID().toString().replace("-", "");
    }

    @Test
    void criaContaFicticiaComSenhaBCryptEPerfilAdministrativo() throws Exception {
        runner("admin.dev@example.invalid", testPassword).run(new DefaultApplicationArguments());

        Usuario salvo = repository.saved;
        assertThat(salvo).isNotNull();
        assertThat(salvo.getRole()).isEqualTo(UsuarioRole.ADMINISTRADOR_AMMA);
        assertThat(salvo.getEmail()).isEqualTo("admin.dev@example.invalid");
        assertThat(salvo.getSenha()).isNotEqualTo(testPassword);
        assertThat(passwordEncoder.matches(testPassword, salvo.getSenha())).isTrue();
    }

    @Test
    void contaExistentePermaneceIntactaMesmoSemSenhaDeCriacao() throws Exception {
        repository.emailExists = true;

        runner("admin.dev@example.invalid", "").run(new DefaultApplicationArguments());

        assertThat(repository.saved).isNull();
        assertThat(repository.cpfChecks).isZero();
    }

    @Test
    void rejeitaDominioQueNaoSejaReservadoParaTeste() {
        assertThatThrownBy(() -> runner("admin@localhost", testPassword)
                .run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(".invalid");
        assertThat(repository.emailChecks).isZero();
        assertThat(repository.saved).isNull();
    }

    @Test
    void impedeBootstrapQuandoPerfilProdTambemEstaAtivo() {
        environment.setActiveProfiles("local", "prod");

        assertThatThrownBy(() -> runner("admin.dev@example.invalid", testPassword)
                .run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("proibido");
        assertThat(repository.emailChecks).isZero();
    }

    @Test
    void componenteNaoERegistradoSemHabilitacaoExplicita() {
        new ApplicationContextRunner()
                .withUserConfiguration(DevAdminBootstrapRunner.class)
                .withPropertyValues("spring.profiles.active=local")
                .run(context -> assertThat(context).doesNotHaveBean(DevAdminBootstrapRunner.class));
    }

    private DevAdminBootstrapRunner runner(String email, String password) {
        return new DevAdminBootstrapRunner(repository.proxy(), passwordEncoder, environment, email, password);
    }

    private static final class RepositoryProbe implements InvocationHandler {
        private boolean emailExists;
        private boolean cpfExists;
        private int emailChecks;
        private int cpfChecks;
        private Usuario saved;

        private UsuarioRepository proxy() {
            return (UsuarioRepository) Proxy.newProxyInstance(
                    UsuarioRepository.class.getClassLoader(),
                    new Class<?>[]{UsuarioRepository.class},
                    this
            );
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "existsByEmailIgnoreCase" -> {
                    emailChecks++;
                    yield emailExists;
                }
                case "existsByCpf" -> {
                    cpfChecks++;
                    yield cpfExists;
                }
                case "saveAndFlush" -> {
                    saved = (Usuario) args[0];
                    yield saved;
                }
                case "toString" -> "UsuarioRepository test probe";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException(method.getName());
            };
        }
    }
}
