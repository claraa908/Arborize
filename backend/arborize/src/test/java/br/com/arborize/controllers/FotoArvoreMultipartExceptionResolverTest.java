package br.com.arborize.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;

class FotoArvoreMultipartExceptionResolverTest {
    private final FotoArvoreMultipartExceptionResolver resolver = new FotoArvoreMultipartExceptionResolver();

    @Test
    void erroAntesDoControllerTemContratoEstruturadoSemDetalhesInternos() throws Exception {
        var request = new MockHttpServletRequest("POST", "/arborize/api/arvores/42/fotos");
        request.setContextPath("/arborize");
        var response = new MockHttpServletResponse();
        assertNotNull(resolver.resolveException(request, response, null, new MaxUploadSizeExceededException(123)));
        var json = JsonMapper.builder().build().readTree(response.getContentAsString());
        assertEquals(413, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/problem+json"));
        assertEquals(413, json.path("status").asInt());
        assertEquals("fotografia-muito-grande", json.path("code").asString());
        assertEquals("urn:arborize:inventario:fotografia-muito-grande", json.path("type").asString());
        assertFalse(json.path("title").asString().isBlank());
        assertFalse(json.path("detail").asString().isBlank());
        assertEquals("file", json.path("errors").get(0).path("field").asString());
        assertFalse(response.getContentAsString().contains("123"));
    }

    @Test
    void naoAlteraErrosDeOutrosModulosRotasOuMetodos() {
        for (String path : new String[]{"/api/materiais", "/api/eventos/42/fotos", "/api/arvores/42", "/api/arvores/42/fotos/7"}) {
            var response = new MockHttpServletResponse();
            assertNull(resolver.resolveException(new MockHttpServletRequest("POST", path), response, null,
                    new MaxUploadSizeExceededException(123)));
            assertEquals(200, response.getStatus());
        }
        assertNull(resolver.resolveException(new MockHttpServletRequest("GET", "/api/arvores/42/fotos"),
                new MockHttpServletResponse(), null, new MaxUploadSizeExceededException(123)));
    }

    @Test
    void naoInterceptaOutrasExcecoes() {
        assertNull(resolver.resolveException(new MockHttpServletRequest("POST", "/api/arvores/42/fotos"),
                new MockHttpServletResponse(), null, new IllegalStateException("interno")));
    }
}
