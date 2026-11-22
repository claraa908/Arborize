package br.com.arborize.controllers;

import br.com.arborize.exceptions.CompensacaoException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.net.URI;
import java.util.List;
import java.util.Map;

@RestControllerAdvice(assignableTypes = CompensacaoController.class)
public class CompensacaoExceptionHandler {
    @ExceptionHandler(CompensacaoException.class)
    public ResponseEntity<ProblemDetail> handleDomain(CompensacaoException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        problem.setTitle("Operação de compensação não concluída");
        problem.setType(URI.create("urn:arborize:compensacao:" + ex.getCode()));
        problem.setProperty("code", ex.getCode());
        return ResponseEntity.status(ex.getStatus()).body(problem);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "O termo ou recibo solicitado não foi encontrado.");
        problem.setTitle("Registro não encontrado");
        problem.setType(URI.create("urn:arborize:compensacao:nao-encontrado"));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> Map.of("field", e.getField(), "message", "Confira o valor informado."))
                .distinct().toList();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Há campos inválidos no formulário.");
        problem.setTitle("Dados inválidos");
        problem.setType(URI.create("urn:arborize:compensacao:dados-invalidos"));
        problem.setProperty("code", "dados-invalidos");
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleMalformedBody() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "O conteúdo enviado não corresponde ao formato esperado.");
        problem.setTitle("Requisição inválida");
        problem.setType(URI.create("urn:arborize:compensacao:requisicao-invalida"));
        problem.setProperty("code", "requisicao-invalida");
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Não foi possível concluir a operação agora. Tente novamente.");
        problem.setTitle("Falha interna");
        problem.setType(URI.create("urn:arborize:compensacao:falha-interna"));
        return ResponseEntity.internalServerError().body(problem);
    }
}
