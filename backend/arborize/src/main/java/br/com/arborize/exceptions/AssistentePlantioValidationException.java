package br.com.arborize.exceptions;

import java.util.Map;

public class AssistentePlantioValidationException extends RuntimeException {
    private final Map<String, String> errors;

    public AssistentePlantioValidationException(Map<String, String> errors) {
        super("As respostas do assistente de plantio são inválidas.");
        this.errors = Map.copyOf(errors);
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
