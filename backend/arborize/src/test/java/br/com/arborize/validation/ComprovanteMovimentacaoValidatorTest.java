package br.com.arborize.validation;

import br.com.arborize.exceptions.ComprovanteMovimentacaoInvalidoException;
import br.com.arborize.exceptions.ComprovanteMovimentacaoMuitoGrandeException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ComprovanteMovimentacaoValidatorTest {
    private final ComprovanteMovimentacaoValidator validator = new ComprovanteMovimentacaoValidator();

    @Test
    void aceitaPdfPngEJpegComAssinaturaCorrespondente() {
        assertDoesNotThrow(() -> validator.validar(arquivo("guia.pdf", "application/pdf", "%PDF-1.7\n".getBytes())));
        assertDoesNotThrow(() -> validator.validar(arquivo("folha.png", "image/png", new byte[]{(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0})));
        assertDoesNotThrow(() -> validator.validar(arquivo("foto.jpg", "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0})));
    }

    @Test
    void rejeitaMimeExtensaoOuAssinaturaQueNaoCorrespondem() {
        assertThrows(ComprovanteMovimentacaoInvalidoException.class,
                () -> validator.validar(arquivo("guia.pdf", "image/png", "%PDF-1.7".getBytes())));
        assertThrows(ComprovanteMovimentacaoInvalidoException.class,
                () -> validator.validar(arquivo("guia.png", "image/png", "conteudo".getBytes())));
    }

    @Test
    void rejeitaArquivoAcimaDeDezMegabytes() {
        byte[] bytes = new byte[(int) ComprovanteMovimentacaoValidator.LIMITE_BYTES + 1];
        assertThrows(ComprovanteMovimentacaoMuitoGrandeException.class,
                () -> validator.validar(arquivo("guia.pdf", "application/pdf", bytes)));
    }

    private MockMultipartFile arquivo(String nome, String mime, byte[] conteudo) {
        return new MockMultipartFile("comprovante", nome, mime, conteudo);
    }
}
