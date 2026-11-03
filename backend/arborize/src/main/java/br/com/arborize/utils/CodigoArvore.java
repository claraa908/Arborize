package br.com.arborize.utils;

import java.util.Locale;
import java.util.Objects;

public final class CodigoArvore {
    public static final String PREFIXO = "ARV-QXD-";
    public static final int DIGITOS_MINIMOS = 6;

    private CodigoArvore() {
    }

    public static String formatar(Long id) {
        Objects.requireNonNull(id, "O ID permanente da árvore deve ser informado.");
        if (id <= 0) {
            throw new IllegalArgumentException("O ID permanente da árvore deve ser positivo.");
        }
        return PREFIXO + String.format(Locale.ROOT, "%06d", id);
    }
}
