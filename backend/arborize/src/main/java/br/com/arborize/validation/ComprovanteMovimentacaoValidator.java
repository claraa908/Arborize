package br.com.arborize.validation;

import br.com.arborize.exceptions.ComprovanteMovimentacaoInvalidoException;
import br.com.arborize.exceptions.ComprovanteMovimentacaoMuitoGrandeException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;

@Component
public class ComprovanteMovimentacaoValidator {
    public static final long LIMITE_BYTES = 10L * 1024 * 1024;

    public void validar(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) return;
        if (arquivo.getSize() > LIMITE_BYTES) {
            throw new ComprovanteMovimentacaoMuitoGrandeException();
        }

        String nome = arquivo.getOriginalFilename();
        String extensao = nome != null && nome.lastIndexOf('.') >= 0
                ? nome.substring(nome.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT)
                : "";
        String mime = arquivo.getContentType();
        try {
            byte[] prefixo = arquivo.getBytes();
            boolean pdf = "pdf".equals(extensao)
                    && "application/pdf".equalsIgnoreCase(mime)
                    && startsWith(prefixo, new byte[]{'%', 'P', 'D', 'F', '-'});
            boolean png = "png".equals(extensao)
                    && "image/png".equalsIgnoreCase(mime)
                    && startsWith(prefixo, new byte[]{(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10});
            boolean jpg = ("jpg".equals(extensao) || "jpeg".equals(extensao))
                    && "image/jpeg".equalsIgnoreCase(mime)
                    && prefixo.length >= 3
                    && (prefixo[0] & 0xFF) == 0xFF
                    && (prefixo[1] & 0xFF) == 0xD8
                    && (prefixo[2] & 0xFF) == 0xFF;
            if (!pdf && !png && !jpg) {
                throw new ComprovanteMovimentacaoInvalidoException("Envie um comprovante PDF, PNG ou JPG válido.");
            }
        } catch (IOException exception) {
            throw new ComprovanteMovimentacaoInvalidoException("Não foi possível validar o comprovante enviado.");
        }
    }

    private boolean startsWith(byte[] content, byte[] signature) {
        if (content.length < signature.length) return false;
        for (int index = 0; index < signature.length; index++) {
            if (content[index] != signature[index]) return false;
        }
        return true;
    }
}
