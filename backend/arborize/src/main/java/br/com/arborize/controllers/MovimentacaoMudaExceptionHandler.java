package br.com.arborize.controllers;

import br.com.arborize.exceptions.ComprovanteMovimentacaoInvalidoException;
import br.com.arborize.exceptions.ComprovanteMovimentacaoMuitoGrandeException;
import br.com.arborize.exceptions.EstoqueInsuficienteException;
import br.com.arborize.exceptions.MovimentacaoMudaInvalidaException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.net.URI;

@RestControllerAdvice(assignableTypes = MovimentacaoMudaController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MovimentacaoMudaExceptionHandler {
    @ExceptionHandler(EstoqueInsuficienteException.class)
    ResponseEntity<ProblemDetail> estoqueInsuficiente(EstoqueInsuficienteException exception) {
        return problem(HttpStatus.CONFLICT, "estoque-insuficiente", exception.getMessage());
    }

    @ExceptionHandler(MovimentacaoMudaInvalidaException.class)
    ResponseEntity<ProblemDetail> dadosInvalidos(MovimentacaoMudaInvalidaException exception) {
        return problem(HttpStatus.BAD_REQUEST, "movimentacao-invalida", exception.getMessage());
    }

    @ExceptionHandler(ComprovanteMovimentacaoInvalidoException.class)
    ResponseEntity<ProblemDetail> comprovanteInvalido(ComprovanteMovimentacaoInvalidoException exception) {
        return problem(HttpStatus.BAD_REQUEST, "comprovante-invalido", exception.getMessage());
    }

    @ExceptionHandler({ComprovanteMovimentacaoMuitoGrandeException.class, MaxUploadSizeExceededException.class})
    ResponseEntity<ProblemDetail> comprovanteGrande(Exception exception) {
        return problem(HttpStatus.CONTENT_TOO_LARGE, "comprovante-muito-grande", "O comprovante deve ter no máximo 10 MB.");
    }

    @ExceptionHandler(EntityNotFoundException.class)
    ResponseEntity<ProblemDetail> recursoNaoEncontrado(EntityNotFoundException exception) {
        String detail = exception.getMessage() != null && exception.getMessage().startsWith("Comprovante")
                ? "Esta movimentação não possui comprovante."
                : "A movimentação ou espécie solicitada não foi encontrada.";
        return problem(HttpStatus.NOT_FOUND, "recurso-nao-encontrado", detail);
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ProblemDetail> falhaArmazenamento(IllegalStateException exception) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "falha-comprovante", "Não foi possível armazenar o comprovante.");
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String code, String detail) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setTitle(status.is4xxClientError() ? "Requisição não concluída" : "Falha na operação");
        body.setType(URI.create("urn:arborize:viveiro:" + code));
        body.setProperty("code", code);
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(body);
    }
}
