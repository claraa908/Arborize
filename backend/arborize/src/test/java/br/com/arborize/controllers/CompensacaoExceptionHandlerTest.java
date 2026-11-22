package br.com.arborize.controllers;

import br.com.arborize.exceptions.CompensacaoException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.*;

class CompensacaoExceptionHandlerTest {
    private final CompensacaoExceptionHandler handler = new CompensacaoExceptionHandler();

    @Test void respostaDeDominioNaoIncluiCausaOuStackTrace() {
        var response = handler.handleDomain(new CompensacaoException(HttpStatus.SERVICE_UNAVAILABLE,
                "armazenamento-indisponivel", "Não foi possível armazenar o documento."));
        assertEquals(503, response.getStatusCode().value());
        assertEquals("armazenamento-indisponivel", response.getBody().getProperties().get("code"));
        assertFalse(response.getBody().getDetail().contains("Exception"));
    }

    @Test void naoEncontradoUsaProblemdetailSemMensagemInterna() {
        var response = handler.handleNotFound();
        assertEquals(404, response.getStatusCode().value());
        assertEquals("O termo ou recibo solicitado não foi encontrado.", response.getBody().getDetail());
    }
}
