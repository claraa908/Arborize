package br.com.arborize.exceptions;

public class MovimentacaoMudaInvalidaException extends RuntimeException {
    public MovimentacaoMudaInvalidaException(String message) {
        super(message);
    }
}
