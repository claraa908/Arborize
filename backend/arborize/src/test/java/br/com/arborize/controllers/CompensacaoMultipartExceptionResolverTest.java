package br.com.arborize.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import static org.junit.jupiter.api.Assertions.*;

class CompensacaoMultipartExceptionResolverTest {
    @Test void limiteDeMultipartRetornaProblemDetailSomenteNasRotasDaCapacidade() throws Exception {
        var resolver = new CompensacaoMultipartExceptionResolver();
        var request = new MockHttpServletRequest("POST", "/api/termos-compensacao/7/entregas");
        var response = new MockHttpServletResponse();
        var result = resolver.resolveException(request, response, null, new MaxUploadSizeExceededException(100));
        assertNotNull(result);
        assertEquals(413, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/problem+json"));
        assertTrue(response.getContentAsString().contains("arquivo-muito-grande"));

        var unrelated = new MockHttpServletRequest("POST", "/api/arvores/1/fotos");
        assertNull(resolver.resolveException(unrelated, new MockHttpServletResponse(), null,
                new MaxUploadSizeExceededException(100)));
    }
}
