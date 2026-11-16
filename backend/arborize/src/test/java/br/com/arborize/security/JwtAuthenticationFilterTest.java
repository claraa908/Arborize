package br.com.arborize.security;

import br.com.arborize.services.TokenService;
import com.auth0.jwt.exceptions.JWTVerificationException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private TokenService tokenService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void autenticaComIdEPapelDoTokenAntesDeContinuarARequisicao() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenService);
        MockHttpServletRequest request = requestWithBearer("jwt-valido");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(tokenService.validarEObterUsuarioId("jwt-valido")).thenReturn(64L);
        when(tokenService.validarEObterRole("jwt-valido")).thenReturn("ROLE_FUNCIONARIO_AMMA");
        FilterChain chain = (req, res) -> {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            assertEquals(64L, authentication.getPrincipal());
            assertEquals("ROLE_FUNCIONARIO_AMMA", authentication.getAuthorities().iterator().next().getAuthority());
        };

        filter.doFilterInternal(request, response, chain);

        verify(tokenService).validarEObterUsuarioId("jwt-valido");
        verify(tokenService).validarEObterRole("jwt-valido");
    }

    @Test
    void tokenInvalidoLimpaAutenticacaoAnteriorEMantemARequisicaoNoFiltro() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenService);
        MockHttpServletRequest request = requestWithBearer("jwt-invalido");
        MockHttpServletResponse response = new MockHttpServletResponse();
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("anterior", "credencial"));
        when(tokenService.validarEObterUsuarioId("jwt-invalido"))
                .thenThrow(new JWTVerificationException("token inválido"));
        FilterChain chain = (req, res) -> assertNull(SecurityContextHolder.getContext().getAuthentication());

        filter.doFilterInternal(request, response, chain);

        verify(tokenService, never()).validarEObterRole("jwt-invalido");
    }

    @Test
    void papelInvalidoNoTokenTambemLimpaAutenticacaoAnterior() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenService);
        MockHttpServletRequest request = requestWithBearer("jwt-papel-invalido");
        MockHttpServletResponse response = new MockHttpServletResponse();
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("anterior", "credencial"));
        when(tokenService.validarEObterUsuarioId("jwt-papel-invalido")).thenReturn(64L);
        when(tokenService.validarEObterRole("jwt-papel-invalido"))
                .thenThrow(new IllegalArgumentException("papel inválido"));
        FilterChain chain = (req, res) -> assertNull(SecurityContextHolder.getContext().getAuthentication());

        filter.doFilterInternal(request, response, chain);

        verify(tokenService).validarEObterRole("jwt-papel-invalido");
    }

    @Test
    void requisicaoSemBearerSegueAnonimaESemValidarToken() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> assertNull(SecurityContextHolder.getContext().getAuthentication());

        filter.doFilterInternal(request, response, chain);

        verify(tokenService, never()).validarEObterUsuarioId(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void esquemaDeAutorizacaoDiferenteDeBearerNaoAutentica() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic credencial");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, (req, res) ->
                assertNull(SecurityContextHolder.getContext().getAuthentication()));

        verify(tokenService, never()).validarEObterUsuarioId(org.mockito.ArgumentMatchers.anyString());
    }

    private MockHttpServletRequest requestWithBearer(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }
}
