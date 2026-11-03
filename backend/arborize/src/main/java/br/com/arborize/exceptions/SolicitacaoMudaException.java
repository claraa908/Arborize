package br.com.arborize.exceptions;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class SolicitacaoMudaException extends RuntimeException {
    private final HttpStatus status;
    private final Map<String, String> errors;
    private final String code;

    public SolicitacaoMudaException(HttpStatus status, String code, String message, Map<String, String> errors) {
        super(message);
        this.status = status;
        this.code = code;
        this.errors = Map.copyOf(errors);
    }

    public HttpStatus getStatus() { return status; }
    public Map<String, String> getErrors() { return errors; }
    public String getCode() { return code; }
}
