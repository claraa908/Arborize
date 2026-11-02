package br.com.arborize.exceptions;

import br.com.arborize.controllers.IfceController;
import br.com.arborize.services.IfceBolsistaService.BolsistaDuplicadoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = IfceController.class)
public class IfceExceptionHandler {

    @ExceptionHandler(BolsistaDuplicadoException.class)
    public ProblemDetail handleBolsistaDuplicado() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Este E-mail ou CPF já está em uso no sistema."
        );
        problem.setTitle("Dados já utilizados");
        problem.setProperty("code", "BOLSISTA_DUPLICADO");
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidacao(MethodArgumentNotValidException exception) {
        String detail = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .findFirst()
                .orElse("Verifique os dados informados.");
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Dados inválidos");
        problem.setProperty("code", "DADOS_INVALIDOS");
        return problem;
    }
}
