package br.com.arborize.services;

import br.com.arborize.entities.DocumentEntities;
import io.minio.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class DocumentService {

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucket.name}")
    private String bucketName;

    // Faz o upload para o MinIO e retorna a entidade Document pronta para ser salva
    public DocumentEntities uploadAndCreateDocument(MultipartFile file) {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }

            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : "";
            String uniqueObjectName = UUID.randomUUID().toString() + extension;

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(uniqueObjectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            return DocumentEntities.builder()
                    .originalFilename(originalFilename)
                    .filePath(uniqueObjectName)
                    .contentType(file.getContentType())
                    .extension(extension)
                    .sizeBytes(file.getSize())
                    .createdAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Erro ao enviar arquivo para o MinIO: " + e.getMessage(), e);
        }
    }

    // Busca os bytes de qualquer documento no MinIO para download
    public byte[] getFileBytes(String filePath) {
        try {
            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(filePath)
                            .build()
            );
            byte[] bytes = stream.readAllBytes();
            stream.close();
            return bytes;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao baixar arquivo do MinIO: " + e.getMessage(), e);
        }
    }
}