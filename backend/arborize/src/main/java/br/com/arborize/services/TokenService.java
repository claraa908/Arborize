package br.com.arborize.services;

import br.com.arborize.entities.Usuario;
import br.com.arborize.enums.UsuarioRole;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.JWTVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

@Service
public class TokenService {

    private static final String ISSUER = "arborize-api";
    private static final long TOKEN_EXPIRATION_HOURS = 24;
    private static final Logger logger = LoggerFactory.getLogger(TokenService.class);

    private final Algorithm algorithm;
    private final JWTVerifier verifier;

    public TokenService(@Value("${app.jwt.secret:}") String configuredSecret, Environment environment) {
        String secret = resolveSecret(configuredSecret, environment);
        this.algorithm = Algorithm.HMAC256(secret.getBytes(StandardCharsets.UTF_8));
        this.verifier = JWT.require(algorithm).withIssuer(ISSUER).build();
    }

    private String resolveSecret(String configuredSecret, Environment environment) {
        if (configuredSecret != null && !configuredSecret.isBlank()) {
            if (configuredSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
                throw new IllegalStateException("JWT_SECRET deve possuir ao menos 32 bytes.");
            }
            return configuredSecret;
        }

        if (environment.acceptsProfiles(Profiles.of("prod"))) {
            throw new IllegalStateException("Configure JWT_SECRET com ao menos 32 bytes antes de iniciar em produção.");
        }

        if (environment.acceptsProfiles(Profiles.of("local", "test"))) {
            byte[] temporarySecret = new byte[32];
            new SecureRandom().nextBytes(temporarySecret);
            logger.warn("JWT_SECRET ausente: foi gerada uma chave temporária para este processo local; sessões serão invalidadas ao reiniciar a API.");
            return Base64.getEncoder().encodeToString(temporarySecret);
        }

        throw new IllegalStateException("Configure JWT_SECRET com ao menos 32 bytes antes de iniciar a API.");
    }

    public String gerarToken(Usuario usuario) {
        Instant agora = Instant.now();
        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(usuario.getId().toString())
                .withClaim("nome", usuario.getNome())
                .withClaim("role", usuario.getRole().getAuthority())
                .withIssuedAt(Date.from(agora))
                .withExpiresAt(Date.from(agora.plus(TOKEN_EXPIRATION_HOURS, ChronoUnit.HOURS)))
                .sign(algorithm);
    }

    public Long validarEObterUsuarioId(String token) {
        return Long.valueOf(verifier.verify(token).getSubject());
    }

    public String validarEObterRole(String token) {
        String authority = verifier.verify(token).getClaim("role").asString();
        return UsuarioRole.fromAuthority(authority).getAuthority();
    }
}
