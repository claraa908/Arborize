package br.com.arborize.exceptions;

import br.com.arborize.controllers.SolicitacaoMudaController;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = SolicitacaoMudaController.class)
public class SolicitacaoMudaExceptionHandler {
    @ExceptionHandler(SolicitacaoMudaException.class)
    public ResponseEntity<ProblemDetail> handleDomain(SolicitacaoMudaException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
        problem.setType(URI.create("about:blank"));
        problem.setTitle(exception.getStatus().is4xxClientError() ? "Solicitação inválida" : "Falha na solicitação");
        problem.setProperty("code", exception.getCode());
        problem.setProperty("errors", exception.getErrors());
        return ResponseEntity.status(exception.getStatus()).body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleBeanValidation(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(error.getField(), safeMessage(error));
        }
        return problem(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Revise os campos informados.", errors);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ProblemDetail> handleMalformedRequest(Exception exception) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Revise os campos informados.", Map.of("request", "Corpo ou parâmetro ausente/inválido."));
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String code, String detail, Map<String, String> errors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("about:blank"));
        problem.setTitle(status == HttpStatus.BAD_REQUEST ? "Dados inválidos" : "Falha na solicitação");
        problem.setProperty("code", code);
        problem.setProperty("errors", errors);
        return ResponseEntity.status(status).body(problem);
    }

    private String safeMessage(FieldError error) {
        return switch (error.getField()) {
            case "cpf" -> "Informe um CPF válido.";
            case "telefone" -> "Informe um telefone com DDD.";
            case "nomeSolicitante" -> "Informe seu nome completo.";
            case "localPlantio" -> "Selecione Calçada ou Quintal.";
            case "quantidadeRecebida" -> "Informe uma quantidade inteira válida de garrafas PET.";
            case "motivo" -> "Informe um motivo de até 255 caracteres.";
            default -> "Preencha este campo com um valor válido.";
        };
    }
}
