package br.com.arborize.validation;

import br.com.arborize.exceptions.CompensacaoException;
import br.com.arborize.validation.FotoArvoreValidator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Locale;

@Component
public class CompensacaoArquivoValidator {
    public static final int TERMO_MAX_BYTES = 10 * 1024 * 1024;
    public static final int RECIBO_MAX_BYTES = 2 * 1024 * 1024;
    private final FotoArvoreValidator imagemValidator;

    public CompensacaoArquivoValidator(FotoArvoreValidator imagemValidator) {
        this.imagemValidator = imagemValidator;
    }

    public Arquivo validarTermo(MultipartFile file) {
        byte[] bytes = ler(file, TERMO_MAX_BYTES, "termo");
        if (!nome(file).toLowerCase(Locale.ROOT).endsWith(".pdf") || !"application/pdf".equalsIgnoreCase(file.getContentType()) || !pdf(bytes)) {
            throw invalido("O termo deve ser um arquivo PDF válido.");
        }
        return new Arquivo(bytes, "application/pdf", nome(file));
    }

    public Arquivo validarRecibo(MultipartFile file) {
        byte[] bytes = ler(file, RECIBO_MAX_BYTES, "recibo");
        String filename = nome(file).toLowerCase(Locale.ROOT);
        if (filename.endsWith(".pdf") && "application/pdf".equalsIgnoreCase(file.getContentType()) && pdf(bytes)) {
            return new Arquivo(bytes, "application/pdf", nome(file));
        }
        if ((filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".png"))) {
            var imagem = imagemValidator.validar(file);
            if (!imagem.contentType().equalsIgnoreCase(file.getContentType())) throw invalido("O tipo declarado do recibo não corresponde à imagem enviada.");
            return new Arquivo(imagem.conteudo(), imagem.contentType(), nome(file));
        }
        throw invalido("O recibo deve ser PDF, JPG ou PNG válido.");
    }

    private byte[] ler(MultipartFile file, int max, String tipo) {
        if (file == null || file.isEmpty()) throw invalido("Selecione o arquivo de " + tipo + ".");
        if (file.getSize() > max) throw new CompensacaoException(HttpStatus.PAYLOAD_TOO_LARGE, "arquivo-muito-grande", "O arquivo excede o limite permitido.");
        try (var stream = file.getInputStream()) {
            byte[] bytes = stream.readNBytes(max + 1);
            if (bytes.length == 0 || bytes.length > max) throw new CompensacaoException(HttpStatus.PAYLOAD_TOO_LARGE, "arquivo-muito-grande", "O arquivo excede o limite permitido.");
            return bytes;
        } catch (IOException ex) {
            throw invalido("Não foi possível ler o arquivo enviado.");
        }
    }

    private boolean pdf(byte[] bytes) {
        if (!(bytes.length >= 5 && bytes[0] == '%' && bytes[1] == 'P' && bytes[2] == 'D' && bytes[3] == 'F' && bytes[4] == '-')) return false;
        try (var document = org.apache.pdfbox.Loader.loadPDF(bytes)) {
            return document.getNumberOfPages() > 0;
        } catch (IOException | RuntimeException ex) {
            return false;
        }
    }

    private String nome(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original == null) return "arquivo";
        String clean = original.replace('\\', '/');
        clean = clean.substring(clean.lastIndexOf('/') + 1).replaceAll("[^A-Za-z0-9._-]", "_");
        return clean.length() > 255 ? clean.substring(clean.length() - 255) : clean;
    }

    private CompensacaoException invalido(String mensagem) {
        return new CompensacaoException(HttpStatus.BAD_REQUEST, "arquivo-invalido", mensagem);
    }

    public record Arquivo(byte[] conteudo, String contentType, String nome) { }
}
