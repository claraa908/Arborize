package br.com.arborize.validation;

import br.com.arborize.exceptions.EntradaInventarioInvalidaException;
import br.com.arborize.exceptions.FotografiaMuitoGrandeException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

@Component
public class FotoArvoreValidator {
    public static final int MAX_BYTES = 2 * 1024 * 1024;
    private final long maxPixels;

    // Proteção técnica de memória na decodificação, configurável sem alterar regras institucionais.
    public FotoArvoreValidator(@Value("${inventario.fotos.max-pixels:25000000}") long maxPixels) {
        if (maxPixels <= 0) throw new IllegalArgumentException("Limite de pixels inválido.");
        this.maxPixels = maxPixels;
    }

    public record Imagem(byte[] conteudo, String contentType, String extensao) { }

    public Imagem validar(MultipartFile file) {
        if (file == null || file.isEmpty()) throw invalida("Selecione uma fotografia não vazia.");
        if (file.getSize() > MAX_BYTES) throw new FotografiaMuitoGrandeException();
        byte[] conteudo;
        try (var stream = file.getInputStream()) {
            conteudo = stream.readNBytes(MAX_BYTES + 1);
        } catch (IOException ex) {
            throw invalida("Não foi possível ler a fotografia enviada.");
        }
        if (conteudo.length == 0) throw invalida("Selecione uma fotografia não vazia.");
        if (conteudo.length > MAX_BYTES) throw new FotografiaMuitoGrandeException();
        try (var stream = new MemoryCacheImageInputStream(new ByteArrayInputStream(conteudo))) {
            var readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw invalida("O arquivo não contém uma imagem JPG, PNG ou WebP válida.");
            var reader = readers.next();
            try {
                reader.setInput(stream);
                String formato = reader.getFormatName().toLowerCase(Locale.ROOT);
                var tipos = Map.of("jpeg", "image/jpeg", "jpg", "image/jpeg", "png", "image/png", "webp", "image/webp");
                String tipo = tipos.get(formato);
                if (tipo == null) throw invalida("São permitidas apenas fotografias JPG, PNG ou WebP.");
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                if (pixels <= 0 || pixels > maxPixels) throw invalida("A resolução da fotografia excede o limite seguro de processamento.");
                boolean[] aviso = {false};
                reader.addIIOReadWarningListener((source, warning) -> aviso[0] = true);
                var imagem = reader.read(0);
                if (imagem == null || aviso[0]) throw invalida("A fotografia está inválida ou incompleta.");
                imagem.flush();
                return new Imagem(conteudo, tipo, tipo.equals("image/jpeg") ? "jpg" : formato);
            } finally {
                reader.dispose();
            }
        } catch (IOException ex) {
            throw invalida("A fotografia está inválida ou incompleta.");
        } catch (RuntimeException ex) {
            if (ex instanceof EntradaInventarioInvalidaException entrada) throw entrada;
            throw invalida("A fotografia está inválida ou incompleta.");
        }
    }

    private EntradaInventarioInvalidaException invalida(String mensagem) {
        return new EntradaInventarioInvalidaException("fotografias", mensagem);
    }
}
