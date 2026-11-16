package br.com.arborize.services;

import br.com.arborize.entities.Usuario;
import br.com.arborize.enums.UsuarioRole;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.env.MockEnvironment;

import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenServiceTest {
    private static final String SECRET = "chave-de-teste-com-pelo-menos-32-bytes";

    private final TokenService service = new TokenService(SECRET, new StandardEnvironment());

    @Test
    void geraTokenComIdentidadePapelEmissorEValidadeDeVinteEQuatroHoras() {
        Usuario usuario = new Usuario();
        usuario.setId(42L);
        usuario.setNome("Usuário de teste");
        usuario.setRole(UsuarioRole.GESTOR_IFCE);

        String token = service.gerarToken(usuario);
        var decoded = JWT.require(Algorithm.HMAC256(SECRET)).withIssuer("arborize-api").build().verify(token);

        assertEquals("42", decoded.getSubject());
        assertEquals("Usuário de teste", decoded.getClaim("nome").asString());
        assertEquals("ROLE_GESTOR_IFCE", decoded.getClaim("role").asString());
        assertEquals(42L, service.validarEObterUsuarioId(token));
        assertEquals("ROLE_GESTOR_IFCE", service.validarEObterRole(token));
        assertTrue(decoded.getExpiresAtAsInstant().isAfter(Instant.now().plusSeconds(23 * 60 * 60)));
    }

    @Test
    void rejeitaTokenAssinadoComOutraChaveOuEmissor() {
        String token = JWT.create()
                .withIssuer("outro-emissor")
                .withSubject("42")
                .sign(Algorithm.HMAC256("outra-chave-com-tamanho-suficiente-para-hmac"));

        assertThrows(JWTVerificationException.class, () -> service.validarEObterUsuarioId(token));
    }

    @Test
    void rejeitaTokenExpirado() {
        String token = JWT.create()
                .withIssuer("arborize-api")
                .withSubject("42")
                .withClaim("role", "ROLE_CIDADAO")
                .withExpiresAt(Date.from(Instant.now().minusSeconds(60)))
                .sign(Algorithm.HMAC256(SECRET));

        assertThrows(JWTVerificationException.class, () -> service.validarEObterUsuarioId(token));
    }

    @Test
    void rejeitaPapelQueNaoExisteNoDominio() {
        String token = JWT.create()
                .withIssuer("arborize-api")
                .withSubject("42")
                .withClaim("role", "ROLE_ROOT")
                .withExpiresAt(Date.from(Instant.now().plusSeconds(60)))
                .sign(Algorithm.HMAC256(SECRET));

        assertThrows(IllegalArgumentException.class, () -> service.validarEObterRole(token));
    }

    @Test
    void exigeSegredoConfiguradoForaDosPerfisLocais() {
        MockEnvironment production = new MockEnvironment();
        production.setActiveProfiles("prod");

        assertThrows(IllegalStateException.class, () -> new TokenService("", production));
        assertThrows(IllegalStateException.class, () -> new TokenService("curto", new StandardEnvironment()));
    }
}
