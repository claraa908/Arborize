package br.com.arborize.exceptions;

public class ComprovanteMovimentacaoMuitoGrandeException extends RuntimeException {
    public ComprovanteMovimentacaoMuitoGrandeException() {
        super("O comprovante deve ter no máximo 10 MB.");
    }
}
