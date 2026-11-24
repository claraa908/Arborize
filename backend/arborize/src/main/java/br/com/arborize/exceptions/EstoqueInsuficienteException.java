package br.com.arborize.exceptions;

public class EstoqueInsuficienteException extends RuntimeException {
    public EstoqueInsuficienteException() {
        super("A quantidade informada excede o estoque disponível para esta espécie.");
    }
}
