package br.com.arborize.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CodigoArvoreTest {
    @Test
    void preencheComZerosAteSeisDigitosSemCriarOutraSequencia() {
        assertEquals("ARV-QXD-000001", CodigoArvore.formatar(1L));
        assertEquals("ARV-QXD-000042", CodigoArvore.formatar(42L));
        assertEquals("ARV-QXD-123456", CodigoArvore.formatar(123456L));
    }

    @Test
    void preservaIdsMaioresQueSeisDigitosSemTruncamento() {
        assertEquals("ARV-QXD-1234567", CodigoArvore.formatar(1_234_567L));
        assertEquals("ARV-QXD-9223372036854775807", CodigoArvore.formatar(Long.MAX_VALUE));
    }

    @Test
    void rejeitaIdInvalido() {
        assertThrows(NullPointerException.class, () -> CodigoArvore.formatar(null));
        assertThrows(IllegalArgumentException.class, () -> CodigoArvore.formatar(0L));
    }
}
