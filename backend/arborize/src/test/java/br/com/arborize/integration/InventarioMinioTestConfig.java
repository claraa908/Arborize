package br.com.arborize.integration;

import br.com.arborize.storage.FotoArvoreStorage;
import io.minio.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import java.net.URI;

@TestConfiguration(proxyBeanMethods = false)
class InventarioMinioTestConfig {
    static String bucket() {return System.getenv("ARBORIZE_IT_MINIO_BUCKET");}
    static String endpoint() {
        String url=System.getenv("ARBORIZE_IT_MINIO_URL");
        if(!"true".equals(System.getenv("ARBORIZE_IT_EPHEMERAL")) || url==null
                || !url.matches("http://127\\.0\\.0\\.1:[0-9]{4,5}") || bucket()==null || !bucket().matches("arborize-it-[0-9a-f]{12}"))
            throw new IllegalStateException("MinIO deve ser exclusivo e efêmero.");
        int port=URI.create(url).getPort();if(port<=1024 || port==9000 || port==9001) throw new IllegalStateException("Porta MinIO inválida.");
        return url;
    }
    static MinioClient client(String url) {
        return MinioClient.builder().endpoint(url).credentials(System.getenv("ARBORIZE_IT_MINIO_ACCESS_KEY"),
                System.getenv("ARBORIZE_IT_MINIO_SECRET_KEY")).build();
    }
    @Bean MinioClient minioAdmin() throws Exception {
        var client=client(endpoint());
        if(!client.bucketExists(BucketExistsArgs.builder().bucket(bucket()).build())) client.makeBucket(MakeBucketArgs.builder().bucket(bucket()).build());
        return client;
    }
    @Bean(destroyMethod="close") MinioFaultProxy minioProxy() throws Exception {return new MinioFaultProxy(URI.create(endpoint()));}
    @Bean FotoArvoreStorage fotoArvoreStorage(MinioFaultProxy proxy,MinioClient minioAdmin) {
        String override=System.getenv("ARBORIZE_IT_MINIO_PROXY_URL");
        if(override!=null && !override.matches("http://127\\.0\\.0\\.1:[0-9]{4,5}")) throw new IllegalStateException("Proxy de teste inválido.");
        return new FotoArvoreStorage(client(override==null?proxy.endpoint():override),bucket());
    }
}
