package br.com.arborize.controllers;

import br.com.arborize.exceptions.EntradaInventarioInvalidaException;
import br.com.arborize.exceptions.FotografiaMuitoGrandeException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private ProblemDetail problem(HttpStatusCode status, String code, String detail) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setTitle(status.is5xxServerError() ? "Falha na operação" : "Requisição não concluída");
        body.setType(URI.create("urn:arborize:inventario:" + code));
        body.setProperty("code", code);
        return body;
    }

    private ResponseEntity<Object> response(HttpStatus status, String code, String detail, Object errors) {
        ProblemDetail body = problem(status, code, detail);
        if (errors != null) {
            body.setProperty("errors", errors);
        }
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(body);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", error.getDefaultMessage() == null ? "Valor inválido." : error.getDefaultMessage()))
                .toList();
        return response(HttpStatus.BAD_REQUEST, "entrada-invalida", "Confira os campos informados.", errors);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex,
            Object body,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        boolean serverError = status.is5xxServerError();
        ProblemDetail safe = problem(
                status,
                serverError ? "erro-interno" : "requisicao-invalida",
                serverError
                        ? "Não foi possível concluir a operação. Tente novamente."
                        : "A requisição não pôde ser processada. Confira o formato e os dados enviados.");
        return new ResponseEntity<>(safe, headers, status);
    }

    @ExceptionHandler(EntradaInventarioInvalidaException.class)
    ResponseEntity<Object> invalidInput(EntradaInventarioInvalidaException ex) {
        return response(
                HttpStatus.BAD_REQUEST,
                "entrada-invalida",
                "Confira os campos informados.",
                List.of(Map.of("field", ex.getCampo(), "message", ex.getMessage())));
    }

    @ExceptionHandler(FotografiaMuitoGrandeException.class)
    ResponseEntity<Object> photoTooLarge(FotografiaMuitoGrandeException ex) {
        return response(
                HttpStatus.CONTENT_TOO_LARGE,
                "fotografia-muito-grande",
                ex.getMessage(),
                List.of(Map.of("field", ex.getCampo(), "message", ex.getMessage())));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<Object> constraintViolation(ConstraintViolationException ex) {
        List<Map<String, String>> errors = ex.getConstraintViolations().stream()
                .map(error -> Map.of(
                        "field", error.getPropertyPath().toString(),
                        "message", error.getMessage()))
                .toList();
        return response(HttpStatus.BAD_REQUEST, "entrada-invalida", "Confira os campos informados.", errors);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    ResponseEntity<Object> notFound(EntityNotFoundException ex) {
        return response(
                HttpStatus.NOT_FOUND,
                "recurso-nao-encontrado",
                "O recurso solicitado não foi encontrado.",
                null);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> unexpected(Exception ex) {
        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "erro-interno",
                "Não foi possível concluir a operação. Tente novamente.",
                null);
    }
}
