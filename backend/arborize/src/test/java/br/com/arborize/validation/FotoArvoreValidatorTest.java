package br.com.arborize.validation;

import br.com.arborize.exceptions.EntradaInventarioInvalidaException;
import br.com.arborize.exceptions.FotografiaMuitoGrandeException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;

public class FotoArvoreValidatorTest {
    public static byte[] imagem(String formato) throws Exception {
        if (formato.equals("webp")) return Base64.getDecoder().decode("UklGRjgAAABXRUJQVlA4ICwAAACwAQCdASoCAAIAAUAmJaACdLoABdQAAP7xTa/i2kdCmQ/+YJ+v3z565AAAAA==");
        var bytes = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), formato, bytes));
        return bytes.toByteArray();
    }
    private final FotoArvoreValidator validator = new FotoArvoreValidator(25_000_000);

    @Test
    void decodificaJpgIgnorandoNomeETipoDeclarado() throws Exception {
        var img = validator.validar(new MockMultipartFile("foto", "../../arquivo.exe", "application/pdf", imagem("jpeg")));
        assertEquals("image/jpeg", img.contentType()); assertEquals("jpg", img.extensao());
    }
    @Test
    void decodificaPng() throws Exception {
        assertEquals("image/png", validator.validar(new MockMultipartFile("foto", imagem("png"))).contentType());
    }
    @Test
    void decodificaWebpReal() throws Exception {
        assertEquals("image/webp", validator.validar(new MockMultipartFile("foto", imagem("webp"))).contentType());
    }
    @Test
    void rejeitaArquivoVazioOuAusente() {
        assertThrows(EntradaInventarioInvalidaException.class, () -> validator.validar(null));
        assertThrows(EntradaInventarioInvalidaException.class, () -> validator.validar(new MockMultipartFile("foto", new byte[0])));
    }
    @Test
    void rejeitaConteudoFalsoTruncadoEFormatoNaoPermitido() throws Exception {
        for (byte[] data : new byte[][]{"arquivo falso".getBytes(), "RIFF0000WEBPVP8 ".getBytes(),
                java.util.Arrays.copyOf(imagem("png"), 16), imagem("gif")}) {
            assertThrows(EntradaInventarioInvalidaException.class, () -> validator.validar(new MockMultipartFile("foto", "foto.png", "image/png", data)));
        }
    }
    @Test
    void rejeitaArquivoAcimaDeDoisMiB() {
        assertThrows(FotografiaMuitoGrandeException.class, () -> validator.validar(new MockMultipartFile("foto", new byte[FotoArvoreValidator.MAX_BYTES + 1])));
    }
    @Test
    void leituraLimitadaNaoConfiaNoTamanhoDeclarado() {
        var file = new MockMultipartFile("foto", new byte[FotoArvoreValidator.MAX_BYTES + 1]) {
            @Override public long getSize() { return 1; }
        };
        assertThrows(FotografiaMuitoGrandeException.class, () -> validator.validar(file));
    }
    @Test
    void protegeDecodificacaoContraResolucaoExcessiva() throws Exception {
        var file = new MockMultipartFile("foto", imagem("png"));
        assertThrows(EntradaInventarioInvalidaException.class, () -> new FotoArvoreValidator(1).validar(file));
    }
}
