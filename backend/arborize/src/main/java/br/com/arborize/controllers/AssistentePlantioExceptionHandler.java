package br.com.arborize.controllers;

import br.com.arborize.exceptions.AssistentePlantioValidationException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(assignableTypes = AssistentePlantioController.class)
public class AssistentePlantioExceptionHandler {
    @ExceptionHandler(AssistentePlantioValidationException.class)
    public ResponseEntity<ProblemDetail> handleInvalidAnswers(AssistentePlantioValidationException exception) {
        return badRequest(exception.getErrors());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleBeanValidation(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(error.getField(), "Valor ausente ou inválido.");
        }
        if (errors.isEmpty()) errors.put("respostas", "Informe as 10 respostas do questionário.");
        return badRequest(errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getConstraintViolations().forEach(violation ->
                errors.put(violation.getPropertyPath().toString(), "Valor ausente ou inválido."));
        return badRequest(errors);
    }

    private ResponseEntity<ProblemDetail> badRequest(Map<String, String> errors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Revise as respostas informadas no questionário."
        );
        problem.setType(URI.create("about:blank"));
        problem.setTitle("Respostas inválidas");
        problem.setProperty("code", "ASSISTENTE_PLANTIO_INVALID_INPUT");
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }
}
