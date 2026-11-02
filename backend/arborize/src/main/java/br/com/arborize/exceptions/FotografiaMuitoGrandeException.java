package br.com.arborize.exceptions;

/** Distingue o limite de bytes das demais rejeições de entrada para a resposta HTTP 413. */
public class FotografiaMuitoGrandeException extends EntradaInventarioInvalidaException {
    public FotografiaMuitoGrandeException() {
        super("fotografias", "Cada fotografia deve possuir no máximo 2 MB.");
    }
}
