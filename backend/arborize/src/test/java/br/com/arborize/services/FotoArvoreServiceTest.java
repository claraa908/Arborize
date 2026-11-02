package br.com.arborize.services;

import br.com.arborize.entities.*;
import br.com.arborize.exceptions.*;
import br.com.arborize.repositories.*;
import br.com.arborize.storage.FotoArvoreStorage;
import br.com.arborize.validation.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.*;
import org.springframework.transaction.support.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Sincronizações reais do Spring com gerenciador SEM recursos. Não comprova transações/locks JPA. */
class FotoArvoreServiceTest {
    private ArvoreRepository arvores;
    private FotoArvoreRepository fotos;
    private FotoArvoreStorage storage;
    private FotoArvoreService service;
    private Arvore arvore;
    private FotoArvore foto;
    private byte[] bytes;
    private TransactionTemplate tx;

    @BeforeEach
    void prepare() throws Exception {
        arvores=mock(ArvoreRepository.class);fotos=mock(FotoArvoreRepository.class);storage=mock(FotoArvoreStorage.class);
        service=new FotoArvoreService(arvores,fotos,storage,new FotoArvoreValidator(25_000_000));
        arvore=new Arvore();ReflectionTestUtils.setField(arvore,"id",42L);
        foto=new FotoArvore(arvore,"inventario/arvores/42/objeto.png","image/png",1);
        ReflectionTestUtils.setField(foto,"id",7L);
        bytes=FotoArvoreValidatorTest.imagem("png");
        when(arvores.findById(42L)).thenReturn(Optional.of(arvore));
        when(arvores.findByIdParaFotografias(42L)).thenReturn(Optional.of(arvore));
        when(fotos.findByIdAndArvoreId(7L,42L)).thenReturn(Optional.of(foto));
        when(storage.recuperar(foto.getObjeto())).thenReturn(bytes);
        when(fotos.saveAndFlush(any())).thenAnswer(i -> {
            FotoArvore salva=i.getArgument(0);ReflectionTestUtils.setField(salva,"id",8L);
            ReflectionTestUtils.setField(salva,"criadoEm",LocalDateTime.of(2026,1,1,12,0));return salva;
        });
        tx=new TransactionTemplate(new TestTxManager());
        tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }
    private <T> T tx(Supplier<T> work) { return tx.execute(status -> work.get()); }
    private Object remover() { service.remover(42L,7L);return null; }
    private MockMultipartFile arquivo() { return new MockMultipartFile("foto","../../nao-confiavel.exe","application/octet-stream",bytes); }

    @Test
    void uploadValidoPersisteMetadadosComChaveSeguraSemExporObjeto() {
        var resposta=tx(() -> service.adicionar(42L,arquivo()));
        assertEquals(8L,resposta.id());assertEquals(42L,resposta.arvoreId());
        assertEquals("image/png",resposta.contentType());assertEquals(bytes.length,resposta.sizeBytes());assertNotNull(resposta.criadoEm());
        var captor=org.mockito.ArgumentCaptor.forClass(FotoArvore.class);verify(fotos).saveAndFlush(captor.capture());
        assertSame(arvore,captor.getValue().getArvore());
        assertTrue(captor.getValue().getObjeto().matches("inventario/arvores/42/[0-9a-f-]{36}\\.png"));
        verify(storage).enviar(captor.getValue().getObjeto(),bytes,"image/png");verify(storage,never()).remover(anyString());
    }
    @Test
    void uploadJpgValidoUsaTipoDetectado() throws Exception {
        bytes=FotoArvoreValidatorTest.imagem("jpeg");
        var resultado=tx(() -> service.adicionar(42L,arquivo()));
        assertEquals("image/jpeg",resultado.contentType());
        verify(storage).enviar(endsWith(".jpg"),eq(bytes),eq("image/jpeg"));
    }
    @Test
    void uploadWebpValidoUsaTipoDetectado() throws Exception {
        bytes=FotoArvoreValidatorTest.imagem("webp");
        var resultado=tx(() -> service.adicionar(42L,arquivo()));
        assertEquals("image/webp",resultado.contentType());
        verify(storage).enviar(endsWith(".webp"),eq(bytes),eq("image/webp"));
    }
    @Test
    void uploadsGeramObjetosDistintos() {
        tx(() -> service.adicionar(42L,arquivo()));tx(() -> service.adicionar(42L,arquivo()));
        var captor=org.mockito.ArgumentCaptor.forClass(String.class);verify(storage,times(2)).enviar(captor.capture(),eq(bytes),eq("image/png"));
        assertNotEquals(captor.getAllValues().get(0),captor.getAllValues().get(1));
    }
    @Test
    void limiteTresEVerificadoDepoisDoBloqueioAntesDoUpload() {
        when(fotos.countByArvoreId(42L)).thenReturn(3L);
        assertThrows(EntradaInventarioInvalidaException.class,() -> tx(() -> service.adicionar(42L,arquivo())));
        var ordem=inOrder(arvores,fotos);ordem.verify(arvores).findByIdParaFotografias(42L);ordem.verify(fotos).countByArvoreId(42L);
        verifyNoInteractions(storage);verify(fotos,never()).saveAndFlush(any());
    }
    @Test
    void arvoreInexistenteNaoFazUpload() {
        when(arvores.findByIdParaFotografias(42L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class,() -> tx(() -> service.adicionar(42L,arquivo())));
        verifyNoInteractions(storage,fotos);
    }
    @Test
    void arquivoInvalidoNaoConsultaNemArmazena() {
        assertThrows(EntradaInventarioInvalidaException.class,() -> tx(() -> service.adicionar(42L,new MockMultipartFile("foto",new byte[0]))));
        verifyNoInteractions(arvores,fotos,storage);
    }
    @Test
    void listaSomenteMetadadosDaArvoreSemConsultarObjetos() {
        when(fotos.findAllByArvoreIdOrderByIdAsc(42L)).thenReturn(List.of(foto));
        assertEquals(7L,service.listar(42L).getFirst().id());verifyNoInteractions(storage);
    }
    @Test
    void recuperaSomenteFotografiaDaArvoreIndicada() {
        var resultado=service.recuperar(42L,7L);assertEquals(7L,resultado.metadados().id());assertArrayEquals(bytes,resultado.conteudo());
        verify(fotos).findByIdAndArvoreId(7L,42L);
    }
    @Test
    void fotografiaDeOutraArvoreOuInexistenteNaoPermiteAcessarObjeto() {
        when(fotos.findByIdAndArvoreId(7L,42L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class,() -> service.recuperar(42L,7L));
        assertThrows(EntityNotFoundException.class,() -> tx(this::remover));verifyNoInteractions(storage);
    }
    @Test
    void remocaoExplicitaApagaSomenteFotoSelecionada() {
        tx(this::remover);verify(storage).recuperar(foto.getObjeto());verify(storage).remover(foto.getObjeto());verify(fotos).delete(same(foto));verify(fotos).flush();
        verify(fotos,never()).findAllByArvoreIdOrderByIdAsc(any());verify(storage,never()).enviar(anyString(),any(),anyString());
        verifyNoMoreInteractions(storage);
    }
    @Test
    void falhaDeUploadNaoSalvaMetadadosETentaLimparObjetoDeResultadoIncerto() {
        doThrow(new ArmazenamentoFotografiaException("Falha simulada",null)).when(storage).enviar(anyString(),any(),anyString());
        assertThrows(ArmazenamentoFotografiaException.class,() -> tx(() -> service.adicionar(42L,arquivo())));
        verify(fotos,never()).saveAndFlush(any());verify(storage).remover(anyString());
    }
    @Test
    void rollbackDoBancoDepoisDeUploadCompensaObjeto() {
        doThrow(new IllegalStateException("Banco simulado")).when(fotos).saveAndFlush(any());
        assertThrows(IllegalStateException.class,() -> tx(() -> service.adicionar(42L,arquivo())));
        var chave=org.mockito.ArgumentCaptor.forClass(String.class);verify(storage).enviar(chave.capture(),eq(bytes),eq("image/png"));
        verify(storage).remover(chave.getValue());
    }
    @Test
    void rollbackPosteriorAoRetornoDoServiceTambemCompensaUpload() {
        assertThrows(IllegalStateException.class,() -> tx(() -> {service.adicionar(42L,arquivo());throw new IllegalStateException("Falha posterior");}));
        verify(storage).remover(anyString());
    }
    @Test
    void falhaDeRemocaoPreservaMetadadosERestauraObjetoDeResultadoIncerto() {
        doThrow(new ArmazenamentoFotografiaException("Falha simulada",null)).when(storage).remover(anyString());
        assertThrows(ArmazenamentoFotografiaException.class,() -> tx(this::remover));
        verify(fotos,never()).delete(any());verify(storage).enviar(foto.getObjeto(),bytes,"image/png");
    }
    @Test
    void rollbackDoBancoDepoisDaRemocaoRestauraObjeto() {
        doThrow(new IllegalStateException("Banco simulado")).when(fotos).flush();
        assertThrows(IllegalStateException.class,() -> tx(this::remover));verify(storage).enviar(foto.getObjeto(),bytes,"image/png");
    }
    @Test
    void falhaNaCompensacaoNaoEscondeFalhaOriginal() {
        var original=new IllegalStateException("Banco simulado");doThrow(original).when(fotos).saveAndFlush(any());
        doThrow(new IllegalStateException("Compensação simulada")).when(storage).remover(anyString());
        assertSame(original,assertThrows(IllegalStateException.class,() -> tx(() -> service.adicionar(42L,arquivo()))));
    }
    @Test
    void falhaDeRecuperacaoNaoExecutaRemocaoOuExclusao() {
        when(storage.recuperar(anyString())).thenThrow(new ArmazenamentoFotografiaException("Falha simulada",null));
        assertThrows(ArmazenamentoFotografiaException.class,() -> tx(this::remover));verify(storage,never()).remover(any());verify(fotos,never()).delete(any());
    }
    @Test
    void resultadoDeCommitDesconhecidoNaoExcluiObjetoCegamente() {
        tx=new TransactionTemplate(new TestTxManager() {
            @Override protected void doCommit(DefaultTransactionStatus status) {
                throw new TransactionSystemException("Resultado de commit simulado desconhecido");
            }
        });
        tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        assertThrows(TransactionSystemException.class,()->tx(()->service.adicionar(42L,arquivo())));
        verify(storage,never()).remover(anyString());
    }
    @Test
    void escritaSemTransacaoERejeitadaAntesDeQualquerEfeitoExterno() {
        assertThrows(IllegalStateException.class,() -> service.adicionar(42L,arquivo()));verifyNoInteractions(arvores,fotos,storage);
    }
    @Test
    void contextoTransacionalIncompativelERejeitadoAntesDeArmazenar() {
        tx.setReadOnly(true);
        assertThrows(IllegalStateException.class,()->tx(()->service.adicionar(42L,arquivo())));
        tx.setReadOnly(false);tx.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        assertThrows(IllegalStateException.class,()->tx(()->service.adicionar(42L,arquivo())));
        verifyNoInteractions(arvores,fotos,storage);
    }
    @Test
    void politicaDeBloqueioEIsolamentoEstaDeclarada() throws Exception {
        assertEquals(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE,ArvoreRepository.class.getMethod("findByIdParaFotografias",Long.class)
                .getAnnotation(org.springframework.data.jpa.repository.Lock.class).value());
        assertEquals(org.springframework.transaction.annotation.Isolation.READ_COMMITTED,
                FotoArvoreService.class.getMethod("adicionar",Long.class,org.springframework.web.multipart.MultipartFile.class)
                        .getAnnotation(org.springframework.transaction.annotation.Transactional.class).isolation());
        assertEquals(0,FotoArvore.class.getDeclaredField("arvore").getAnnotation(jakarta.persistence.ManyToOne.class).cascade().length);
    }
    static class TestTxManager extends AbstractPlatformTransactionManager {
        @Override protected Object doGetTransaction() {return new Object();}
        @Override protected void doBegin(Object transaction,TransactionDefinition definition) { }
        @Override protected void doCommit(DefaultTransactionStatus status) { }
        @Override protected void doRollback(DefaultTransactionStatus status) { }
    }
}
