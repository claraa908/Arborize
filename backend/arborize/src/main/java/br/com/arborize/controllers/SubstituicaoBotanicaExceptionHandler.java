package br.com.arborize.controllers;

import br.com.arborize.exceptions.SubstituicaoBotanicaException;
import br.com.arborize.exceptions.ArmazenamentoFotografiaException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.validation.BindException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(assignableTypes = SubstituicaoBotanicaController.class)
public class SubstituicaoBotanicaExceptionHandler {
    @ExceptionHandler(SubstituicaoBotanicaException.class)
    public ResponseEntity<ProblemDetail> handleDomain(SubstituicaoBotanicaException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
        problem.setTitle(exception.getStatus().is4xxClientError() ? "Solicitação não concluída" : "Falha na solicitação");
        problem.setType(URI.create("urn:arborize:substituicao:" + exception.getCode().toLowerCase()));
        problem.setProperty("code", exception.getCode());
        if (!exception.getErrors().isEmpty()) problem.setProperty("errors", exception.getErrors());
        return ResponseEntity.status(exception.getStatus()).body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error -> errors.putIfAbsent(
                error.getField(), error.getDefaultMessage() == null ? "Valor inválido." : error.getDefaultMessage()));
        return problem(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Revise os campos informados.", errors);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ProblemDetail> handleBinding(BindException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error -> errors.putIfAbsent(
                error.getField(), error.getDefaultMessage() == null ? "Valor inválido." : error.getDefaultMessage()));
        return problem(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Revise os campos informados.", errors);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ProblemDetail> handleTooLarge(MaxUploadSizeExceededException exception) {
        return problem(HttpStatus.CONTENT_TOO_LARGE, "FILE_TOO_LARGE", "O arquivo excede o limite permitido.", Map.of("arquivo", "Envie um arquivo dentro do limite de tamanho."));
    }

    @ExceptionHandler(ArmazenamentoFotografiaException.class)
    public ResponseEntity<ProblemDetail> handleStorage(ArmazenamentoFotografiaException exception) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Não foi possível processar a solicitação agora.", Map.of());
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String code, String detail, Map<String, String> errors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.is4xxClientError() ? "Dados inválidos" : "Falha na solicitação");
        problem.setType(URI.create("urn:arborize:substituicao:" + code.toLowerCase()));
        problem.setProperty("code", code);
        if (!errors.isEmpty()) problem.setProperty("errors", errors);
        return ResponseEntity.status(status).body(problem);
    }
}
