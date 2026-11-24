package br.com.arborize.security;

import br.com.arborize.controllers.CompensacaoController;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.*;

class CompensacaoSecurityProblemHandlerTest {
    @Test void controllerRestringeOperacoesAosDoisPerfisInternos() {
        var auth = CompensacaoController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(auth);
        assertEquals("hasAnyRole('FUNCIONARIO_AMMA', 'ADMINISTRADOR_AMMA')", auth.value());
    }

    @Test void requisicaoSemPerfilRecebeProblemDetail403() throws Exception {
        var handler = new CompensacaoSecurityProblemHandler();
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        handler.accessDeniedHandler().handle(request, response, new org.springframework.security.access.AccessDeniedException("denied"));
        assertEquals(403, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/problem+json"));
        assertTrue(response.getContentAsString().contains("sem-permissao"));
    }
}
