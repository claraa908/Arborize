package br.com.arborize.exceptions;

public class ArmazenamentoFotografiaException extends RuntimeException {
    public ArmazenamentoFotografiaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
