package br.com.arborize.storage;

import io.minio.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.ByteArrayInputStream;

@Component
public class CompensacaoStorage {
    private final MinioClient minio;
    private final String bucket;

    public CompensacaoStorage(MinioClient minio, @Value("${minio.bucket.name}") String bucket) {
        this.minio = minio;
        this.bucket = bucket;
    }

    public void enviar(String key, byte[] bytes, String contentType) {
        try (var stream = new ByteArrayInputStream(bytes)) {
            minio.putObject(PutObjectArgs.builder().bucket(bucket).object(key)
                    .stream(stream, bytes.length, -1).contentType(contentType).build());
        } catch (Exception ex) {
            throw new br.com.arborize.exceptions.CompensacaoException(
                    org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "armazenamento-indisponivel",
                    "Não foi possível armazenar o documento.");
        }
    }

    public byte[] recuperar(String key, long maxBytes) {
        try (var stream = minio.getObject(GetObjectArgs.builder().bucket(bucket).object(key).build())) {
            byte[] bytes = stream.readNBytes(Math.toIntExact(maxBytes + 1));
            if (bytes.length > maxBytes) throw new java.io.IOException("Stored document exceeds expected size.");
            return bytes;
        } catch (Exception ex) {
            throw new br.com.arborize.exceptions.CompensacaoException(
                    org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "documento-indisponivel",
                    "Não foi possível recuperar o documento.");
        }
    }

    public void remover(String key) {
        try {
            minio.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build());
        } catch (Exception ex) {
            throw new IllegalStateException("Não foi possível remover o documento armazenado.", ex);
        }
    }
}
