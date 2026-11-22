package br.com.arborize.storage;

import br.com.arborize.exceptions.ArmazenamentoFotografiaException;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.UUID;

@Component
public class SubstituicaoBotanicaStorage {
    private final MinioClient minio;
    private final String bucket;

    public SubstituicaoBotanicaStorage(MinioClient minio, @Value("${minio.bucket.name}") String bucket) {
        this.minio = minio;
        this.bucket = bucket;
    }

    public String guardar(String prefixo, MultipartFile arquivo, String contentType) {
        String objeto = "substituicao-botanica/" + prefixo + "/" + UUID.randomUUID();
        try (var stream = arquivo.getInputStream()) {
            minio.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objeto)
                    .stream(stream, arquivo.getSize(), -1)
                    .contentType(contentType)
                    .build());
            return objeto;
        } catch (Exception ex) {
            throw new ArmazenamentoFotografiaException("Não foi possível armazenar o documento.", ex);
        }
    }

    public byte[] recuperar(String objeto, long limiteBytes) {
        try (var stream = minio.getObject(GetObjectArgs.builder().bucket(bucket).object(objeto).build())) {
            byte[] conteudo = stream.readNBytes(Math.toIntExact(limiteBytes + 1));
            if (conteudo.length > limiteBytes) throw new java.io.IOException("Tamanho armazenado incompatível.");
            return conteudo;
        } catch (Exception ex) {
            throw new ArmazenamentoFotografiaException("Não foi possível recuperar o documento.", ex);
        }
    }

    public void remover(String objeto) {
        if (objeto == null || objeto.isBlank()) return;
        try {
            minio.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objeto).build());
        } catch (Exception ex) {
            throw new ArmazenamentoFotografiaException("Não foi possível remover o documento.", ex);
        }
    }
}
