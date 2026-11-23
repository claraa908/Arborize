package br.com.arborize.exceptions;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class SubstituicaoBotanicaException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final Map<String, String> errors;

    public SubstituicaoBotanicaException(HttpStatus status, String code, String message, Map<String, String> errors) {
        super(message);
        this.status = status;
        this.code = code;
        this.errors = Map.copyOf(errors);
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
    public Map<String, String> getErrors() { return errors; }
}
