package br.com.arborize.storage;

import br.com.arborize.exceptions.ArmazenamentoFotografiaException;
import io.minio.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FotoArvoreStorageTest {
    private MinioClient minio;
    private FotoArvoreStorage storage;
    @BeforeEach void prepare() {minio=mock(MinioClient.class);storage=new FotoArvoreStorage(minio,"bucket-de-teste");}
    @Test void enviaAoBucketConfiguradoComTipoDetectado() throws Exception {
        storage.enviar("inventario/arvores/42/uuid.png",new byte[]{1,2},"image/png");
        var args=org.mockito.ArgumentCaptor.forClass(PutObjectArgs.class);verify(minio).putObject(args.capture());
        assertEquals("bucket-de-teste",args.getValue().bucket());assertEquals("inventario/arvores/42/uuid.png",args.getValue().object());
        assertEquals("image/png",args.getValue().contentType());
    }
    @Test void removeSomenteObjetoSolicitado() throws Exception {
        storage.remover("inventario/arvores/42/uuid.png");
        var args=org.mockito.ArgumentCaptor.forClass(RemoveObjectArgs.class);verify(minio).removeObject(args.capture());
        assertEquals("inventario/arvores/42/uuid.png",args.getValue().object());
    }
    @Test void recuperaConteudoEFechaStream() throws Exception {
        var entrada=spy(new java.io.ByteArrayInputStream(new byte[]{1,2,3}));
        when(minio.getObject(any())).thenReturn(new GetObjectResponse(new okhttp3.Headers.Builder().build(),
                "bucket-de-teste",null,"objeto",entrada));
        assertArrayEquals(new byte[]{1,2,3},storage.recuperar("objeto"));verify(entrada).close();
    }
    @Test void rejeitaObjetoArmazenadoAcimaDoLimite() throws Exception {
        var entrada=new java.io.ByteArrayInputStream(new byte[2*1024*1024+1]);
        when(minio.getObject(any())).thenReturn(new GetObjectResponse(new okhttp3.Headers.Builder().build(),
                "bucket-de-teste",null,"objeto",entrada));
        assertThrows(ArmazenamentoFotografiaException.class,()->storage.recuperar("objeto"));
    }
    @Test void falhasExternasTemMensagemPublicaGenerica() throws Exception {
        when(minio.putObject(any())).thenThrow(new IllegalStateException("SEGREDO_INTERNO"));
        var ex=assertThrows(ArmazenamentoFotografiaException.class,() -> storage.enviar("objeto",new byte[]{1},"image/png"));
        assertFalse(ex.getMessage().contains("SEGREDO_INTERNO"));
        when(minio.getObject(any())).thenThrow(new IllegalStateException("SEGREDO_INTERNO"));
        assertThrows(ArmazenamentoFotografiaException.class,() -> storage.recuperar("objeto"));
        doThrow(new IllegalStateException("SEGREDO_INTERNO")).when(minio).removeObject(any());
        assertThrows(ArmazenamentoFotografiaException.class,() -> storage.remover("objeto"));
    }
}
