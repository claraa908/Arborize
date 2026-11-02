package br.com.arborize.storage;

import br.com.arborize.exceptions.ArmazenamentoFotografiaException;
import io.minio.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.ByteArrayInputStream;

/** Reutiliza o cliente e o bucket configurados; não modifica política nem publica objetos. */
@Component
public class FotoArvoreStorage {
    private final MinioClient minio;
    private final String bucket;

    public FotoArvoreStorage(MinioClient minio, @Value("${minio.bucket.name}") String bucket) {
        this.minio = minio;
        this.bucket = bucket;
    }

    public void enviar(String objeto, byte[] conteudo, String contentType) {
        try (var stream = new ByteArrayInputStream(conteudo)) {
            minio.putObject(PutObjectArgs.builder().bucket(bucket).object(objeto)
                    .stream(stream, conteudo.length, -1).contentType(contentType).build());
        } catch (Exception ex) {
            throw new ArmazenamentoFotografiaException("Não foi possível armazenar a fotografia.", ex);
        }
    }

    public byte[] recuperar(String objeto) {
        try (var stream = minio.getObject(GetObjectArgs.builder().bucket(bucket).object(objeto).build())) {
            byte[] conteudo = stream.readNBytes(2 * 1024 * 1024 + 1);
            if (conteudo.length > 2 * 1024 * 1024) throw new java.io.IOException("Tamanho armazenado incompatível.");
            return conteudo;
        } catch (Exception ex) {
            throw new ArmazenamentoFotografiaException("Não foi possível recuperar a fotografia.", ex);
        }
    }

    public void remover(String objeto) {
        try {
            minio.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objeto).build());
        } catch (Exception ex) {
            throw new ArmazenamentoFotografiaException("Não foi possível remover a fotografia.", ex);
        }
    }
}
