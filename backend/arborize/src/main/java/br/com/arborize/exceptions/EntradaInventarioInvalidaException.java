package br.com.arborize.exceptions;

/** Rejeição esperada de entrada do inventário, distinta de falhas de programação. */
public class EntradaInventarioInvalidaException extends IllegalArgumentException {
    private final String campo;

    public EntradaInventarioInvalidaException(String campo, String mensagem) {
        super(mensagem);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
