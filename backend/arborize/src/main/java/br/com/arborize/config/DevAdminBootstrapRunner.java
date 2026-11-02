package br.com.arborize.config;

import br.com.arborize.entities.Usuario;
import br.com.arborize.enums.UsuarioRole;
import br.com.arborize.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@Profile("local")
@ConditionalOnProperty(
        prefix = "arborize.dev.admin-bootstrap",
        name = "enabled",
        havingValue = "true"
)
public class DevAdminBootstrapRunner implements ApplicationRunner {

    private static final String DEVELOPMENT_CPF = "00000000000";
    private static final String DEVELOPMENT_PHONE = "00000000000";

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;
    private final String configuredEmail;
    private final String configuredPassword;

    public DevAdminBootstrapRunner(
            UsuarioRepository repository,
            PasswordEncoder passwordEncoder,
            Environment environment,
            @Value("${arborize.dev.admin-bootstrap.email:}") String configuredEmail,
            @Value("${arborize.dev.admin-bootstrap.password:}") String configuredPassword
    ) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
        this.configuredEmail = configuredEmail;
        this.configuredPassword = configuredPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (environment.acceptsProfiles(Profiles.of("prod"))) {
            throw new IllegalStateException("O bootstrap administrativo de desenvolvimento é proibido no perfil prod.");
        }

        String email = normalizeEmail(configuredEmail);

        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.invalid$")) {
            throw new IllegalStateException("Use um e-mail fictício com domínio reservado .invalid para o bootstrap local.");
        }

        if (repository.existsByEmailIgnoreCase(email)) {
            return;
        }

        String password = requireSecret(configuredPassword, "ARBORIZA_DEV_ADMIN_PASSWORD");
        if (repository.existsByCpf(DEVELOPMENT_CPF)) {
            throw new IllegalStateException("O CPF fictício reservado ao bootstrap local já está cadastrado.");
        }

        Usuario usuario = new Usuario();
        usuario.setNome("Administrador de Desenvolvimento");
        usuario.setCpf(DEVELOPMENT_CPF);
        usuario.setEmail(email);
        usuario.setWhatsapp(DEVELOPMENT_PHONE);
        usuario.setEndereco("Ambiente local de desenvolvimento");
        usuario.setSenha(passwordEncoder.encode(password));
        usuario.setRole(UsuarioRole.ADMINISTRADOR_AMMA);
        repository.saveAndFlush(usuario);
    }

    private String normalizeEmail(String email) {
        return requireValue(email, "ARBORIZA_DEV_ADMIN_EMAIL").toLowerCase(Locale.ROOT);
    }

    private String requireSecret(String secret, String variableName) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("Configure " + variableName + " para habilitar o bootstrap local.");
        }
        return secret;
    }

    private String requireValue(String value, String variableName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Configure " + variableName + " para habilitar o bootstrap local.");
        }
        return value.trim();
    }
}
