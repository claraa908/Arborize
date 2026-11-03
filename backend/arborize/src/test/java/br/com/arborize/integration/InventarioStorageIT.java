package br.com.arborize.integration;

import br.com.arborize.controllers.*;
import br.com.arborize.dtos.request.ArvoreCadastroRequestDTO;
import br.com.arborize.entities.*;
import br.com.arborize.enums.ClassificacaoTerritorial;
import br.com.arborize.exceptions.*;
import br.com.arborize.repositories.*;
import br.com.arborize.services.*;
import br.com.arborize.validation.FotoArvoreValidatorTest;
import io.minio.*;
import jakarta.persistence.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.read.ListAppender;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/** PostgreSQL + MinIO + SDK + repositories e serviço reais. Falhas injetadas apenas na rede/SQL. */
@SpringJUnitConfig({InventarioPersistenceTestConfig.class,InventarioMinioTestConfig.class})
@ActiveProfiles(InventarioPersistenceTestConfig.PROFILE)
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class InventarioStorageIT {
    @Autowired ArvoreService arvores;
    @Autowired FotoArvoreService service;
    @Autowired FotoArvoreRepository fotos;
    @Autowired ArvoreRepository repo;
    @Autowired MinioClient minioAdmin;
    @Autowired MinioFaultProxy proxy;
    @Autowired DataSource dataSource;
    @Autowired PlatformTransactionManager manager;
    @PersistenceContext EntityManager em;
    private TransactionTemplate tx;
    private Long owner;
    private byte[] png;
    private ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> logs;
    private static final List<String> EVIDENCE=new ArrayList<>();
    private String bucket(){return InventarioMinioTestConfig.bucket();}
    private <T>T tx(Supplier<T> work){return tx.execute(status->work.get());}
    private MockMultipartFile file(byte[] bytes){return new MockMultipartFile("file","../../arquivo.exe","application/octet-stream",bytes);}
    private Long tree(){return arvores.criarArvore(new ArvoreCadastroRequestDTO(tx(()->em.createQuery("select e.id from Especie e",Long.class).getSingleResult()),
            BigDecimal.ZERO,BigDecimal.ZERO,ClassificacaoTerritorial.BAIRRO_NAO_IDENTIFICADO,null,null,null,null,null,null,null)).id();}
    private Set<String> objects() throws Exception {
        var keys=new TreeSet<String>();
        for(var item:minioAdmin.listObjects(ListObjectsArgs.builder().bucket(bucket()).recursive(true).build()))keys.add(item.get().objectName());
        return keys;
    }
    private long rows(){return tx(()->fotos.countByArvoreId(owner));}
    private String key(Long id){return tx(()->fotos.findByIdAndArvoreId(id,owner).orElseThrow().getObjeto());}
    private byte[] bytes(String key) throws Exception {try(var stream=minioAdmin.getObject(GetObjectArgs.builder().bucket(bucket()).object(key).build())){return stream.readAllBytes();}}
    private void ddl(String sql) throws Exception {try(var c=dataSource.getConnection();var s=c.createStatement()){s.execute(sql);}}
    private void trigger(String operation) throws Exception {
        ddl("CREATE FUNCTION falha_foto_it() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'Falha SQL controlada de teste'; END $$");
        ddl("CREATE TRIGGER falha_foto_it BEFORE "+operation+" ON fotos_arvores FOR EACH ROW EXECUTE FUNCTION falha_foto_it()");
    }
    private void evidence(String scenario) throws Exception {
        boolean pending=logs.list.stream().anyMatch(e->e.getFormattedMessage().contains("Compensação de fotografia pendente"));
        EVIDENCE.add(scenario+": metadados="+rows()+", objetos="+objects().size()+", compensacao_pendente_registrada="+pending);
    }
    @BeforeEach void setup() throws Exception {
        proxy.reset();tx=new TransactionTemplate(manager);tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        ddl("DROP FUNCTION IF EXISTS falha_foto_it() CASCADE");
        if(!minioAdmin.bucketExists(BucketExistsArgs.builder().bucket(bucket()).build()))minioAdmin.makeBucket(MakeBucketArgs.builder().bucket(bucket()).build());
        for(String key:objects())minioAdmin.removeObject(RemoveObjectArgs.builder().bucket(bucket()).object(key).build());
        tx(()->{em.createQuery("delete from FotoArvore").executeUpdate();em.createQuery("delete from Arvore").executeUpdate();
            em.createQuery("delete from Bairro").executeUpdate();em.createQuery("delete from Especie").executeUpdate();
            var e=new Especie();e.setNomePopular("Espécie exclusivamente de teste");e.setNomeCientifico("Nome de teste");
            e.setCategoria("TESTE");e.setPorteArvore("TESTE");e.setRecomendadaCalcadas(false);em.persist(e);em.flush();return null;});
        owner=tree();png=FotoArvoreValidatorTest.imagem("png");
        logs=new ListAppender<>();logs.start();((Logger)org.slf4j.LoggerFactory.getLogger(FotoArvoreService.class)).addAppender(logs);
    }
    @AfterEach void cleanup() throws Exception {
        proxy.reset();ddl("DROP FUNCTION IF EXISTS falha_foto_it() CASCADE");
        ((Logger)org.slf4j.LoggerFactory.getLogger(FotoArvoreService.class)).detachAppender(logs);logs.stop();
    }
    @AfterAll static void saveEvidence() throws Exception {Files.write(Path.of("target/minio-it-evidence.txt"),EVIDENCE);}

    private void upload(String format,String mime) throws Exception {
        byte[] input=FotoArvoreValidatorTest.imagem(format);var result=service.adicionar(owner,file(input));
        assertEquals(owner,result.arvoreId());assertEquals(mime,result.contentType());assertEquals(input.length,result.sizeBytes());
        assertNotNull(result.id());assertNotNull(result.criadoEm());assertEquals(1,rows());assertEquals(1,objects().size());
        String key=key(result.id());assertTrue(key.matches("inventario/arvores/"+owner+"/[0-9a-f-]{36}\\.(jpg|png|webp)"));
        assertArrayEquals(input,bytes(key));assertArrayEquals(input,service.recuperar(owner,result.id()).conteudo());
        assertEquals(mime,minioAdmin.statObject(StatObjectArgs.builder().bucket(bucket()).object(key).build()).contentType());
        assertEquals(List.of(result.id()),service.listar(owner).stream().map(f->f.id()).toList());
        assertFalse(Arrays.stream(result.getClass().getRecordComponents()).anyMatch(c->Set.of("objeto","bucket","accessKey","secretKey","url").contains(c.getName())));
    }
    @Test void jpgReal() throws Exception {upload("jpeg","image/jpeg");}
    @Test void pngReal() throws Exception {upload("png","image/png");}
    @Test void webpReal() throws Exception {upload("webp","image/webp");}

    @Test void rejeicoesNaoCriamObjetosOuMetadados() throws Exception {
        for(var input:List.of(file(new byte[0]),file("conteudo falso".getBytes()),file(FotoArvoreValidatorTest.imagem("gif")),
                file(new byte[2*1024*1024+1]),new MockMultipartFile("file",new byte[2*1024*1024+1]){@Override public long getSize(){return 1;}})) {
            assertThrows(EntradaInventarioInvalidaException.class,()->service.adicionar(owner,input));
            assertEquals(0,rows());assertTrue(objects().isEmpty());
        }
        assertThrows(EntityNotFoundException.class,()->service.adicionar(Long.MAX_VALUE,file(png)));
        assertEquals(0,rows());assertTrue(objects().isEmpty());
    }
    @Test void quartaFotoNaoDeixaObjetoExcedente() throws Exception {
        for(int i=0;i<3;i++)service.adicionar(owner,file(png));Set<String> before=objects();
        assertThrows(EntradaInventarioInvalidaException.class,()->service.adicionar(owner,file(png)));
        assertEquals(3,rows());assertEquals(before,objects());
    }
    @Test void outraArvoreNaoConsultaNemRemoveFoto() throws Exception {
        var photo=service.adicionar(owner,file(png));Long other=tree();Set<String> before=objects();
        assertThrows(EntityNotFoundException.class,()->service.recuperar(other,photo.id()));
        assertThrows(EntityNotFoundException.class,()->service.remover(other,photo.id()));
        assertEquals(before,objects());assertEquals(1,rows());
    }
    @Test void remocaoRealPreservaOutraFotoEArvore() throws Exception {
        var a=service.adicionar(owner,file(png));var b=service.adicionar(owner,file(png));String remaining=key(b.id());
        service.remover(owner,a.id());assertEquals(1,rows());assertEquals(Set.of(remaining),objects());
        assertArrayEquals(png,bytes(remaining));assertEquals(owner,arvores.buscarPorId(owner).id());
        assertThrows(EntityNotFoundException.class,()->service.remover(owner,a.id()));assertEquals(Set.of(remaining),objects());
    }
    @Test void aMinioRejeitaUploadEmBucketAusente() throws Exception {
        minioAdmin.removeBucket(RemoveBucketArgs.builder().bucket(bucket()).build());
        assertThrows(ArmazenamentoFotografiaException.class,()->service.adicionar(owner,file(png)));
        assertEquals(0,rows());assertFalse(minioAdmin.bucketExists(BucketExistsArgs.builder().bucket(bucket()).build()));
        minioAdmin.makeBucket(MakeBucketArgs.builder().bucket(bucket()).build());evidence("A upload rejeitado pelo MinIO");
    }
    @Test void bFalhaSqlAposUploadRemoveObjetoReal() throws Exception {
        trigger("INSERT");assertThrows(RuntimeException.class,()->service.adicionar(owner,file(png)));
        assertEquals(0,rows());assertTrue(objects().isEmpty());evidence("B INSERT falha apos upload");
    }
    @Test void cFalhaNaRemocaoDoObjetoPreservaBancoEConteudo() throws Exception {
        var photo=service.adicionar(owner,file(png));String key=key(photo.id());proxy.mode=MinioFaultProxy.Mode.DENY_DELETE;
        assertThrows(ArmazenamentoFotografiaException.class,()->service.remover(owner,photo.id()));
        assertEquals(1,rows());assertArrayEquals(png,bytes(key));assertTrue(proxy.denied.get()>0);evidence("C DELETE negado no transporte");
    }
    @Test void dRollbackDeDeleteRestauraObjetoReal() throws Exception {
        var photo=service.adicionar(owner,file(png));String key=key(photo.id());trigger("DELETE");
        assertThrows(RuntimeException.class,()->service.remover(owner,photo.id()));
        assertEquals(1,rows());assertArrayEquals(png,bytes(key));assertEquals("image/png",minioAdmin.statObject(StatObjectArgs.builder().bucket(bucket()).object(key).build()).contentType());
        evidence("D DELETE SQL falha e objeto restaurado");
    }
    @Test void eCompensacaoDeUploadFalhaEDeixaOrfaoComDiagnostico() throws Exception {
        trigger("INSERT");proxy.mode=MinioFaultProxy.Mode.DENY_DELETE;
        assertThrows(RuntimeException.class,()->service.adicionar(owner,file(png)));
        assertEquals(0,rows());assertEquals(1,objects().size());
        assertTrue(logs.list.stream().anyMatch(e->e.getFormattedMessage().contains("Compensação de fotografia pendente")));
        evidence("E falha de compensacao deixa objeto orfao");
    }
    @Test void eCompensacaoDeRemocaoFalhaEDeixaMetadadoSemObjeto() throws Exception {
        var photo=service.adicionar(owner,file(png));trigger("DELETE");proxy.mode=MinioFaultProxy.Mode.DENY_PUT;
        assertThrows(RuntimeException.class,()->service.remover(owner,photo.id()));
        assertEquals(1,rows());assertTrue(objects().isEmpty());
        assertTrue(logs.list.stream().anyMatch(e->e.getFormattedMessage().contains("Compensação de fotografia pendente")));
        evidence("E falha de restauracao deixa metadado sem objeto");
    }
    @Test void bucketEObjetosNaoPermitemLeituraAnonima() throws Exception {
        var photo=service.adicionar(owner,file(png));String key=key(photo.id());
        var client=HttpClient.newHttpClient();
        for(String suffix:List.of("/"+bucket(),"/"+bucket()+"/"+key)) {
            var response=client.send(HttpRequest.newBuilder(URI.create(InventarioMinioTestConfig.endpoint()+suffix)).GET().build(),HttpResponse.BodyHandlers.discarding());
            assertEquals(403,response.statusCode());
        }
    }
    @Test void falhaRealDeArmazenamentoTemRespostaHttpSemSegredos() throws Exception {
        proxy.mode=MinioFaultProxy.Mode.DENY_PUT;
        var mvc=standaloneSetup(new FotoArvoreController(service)).setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(multipart("/api/arvores/"+owner+"/fotos").file(file(png))).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("erro-interno"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(bucket()))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(System.getenv("ARBORIZE_IT_MINIO_SECRET_KEY")))));
        assertEquals(0,rows());assertTrue(objects().isEmpty());
    }
    @Test void concorrenciaRealNaoDeixaQuartaFotoOuMetadadoDuplicado() throws Exception {
        service.adicionar(owner,file(png));service.adicionar(owner,file(png));proxy.mode=MinioFaultProxy.Mode.GATE_PUT;
        var workers=Executors.newFixedThreadPool(2);
        try {
            var first=workers.submit(()->service.adicionar(owner,file(png)));
            assertTrue(proxy.reached.await(10,TimeUnit.SECONDS));assertEquals(3,objects().size());
            var second=workers.submit(()->{try{service.adicionar(owner,file(png));return false;}catch(EntradaInventarioInvalidaException e){return true;}});
            boolean blocked=false;long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
            try(var c=dataSource.getConnection();var s=c.createStatement()) {
                while(System.nanoTime()<deadline) {
                    try(var rs=s.executeQuery("select count(*) from pg_stat_activity where datname=current_database() and wait_event_type='Lock' and cardinality(pg_blocking_pids(pid))>0")) {
                        rs.next();if(rs.getInt(1)>0){blocked=true;break;}
                    }Thread.sleep(10);
                }
            }
            assertTrue(blocked);proxy.release.countDown();assertNotNull(first.get(10,TimeUnit.SECONDS));assertTrue(second.get(10,TimeUnit.SECONDS));
            assertEquals(3,rows());assertEquals(3,objects().size());
            Set<String> persisted=tx(()->new HashSet<>(fotos.findAllByArvoreIdOrderByIdAsc(owner).stream().map(FotoArvore::getObjeto).toList()));
            assertEquals(persisted,objects());evidence("Concorrencia PG e MinIO reais");
        } finally {proxy.release.countDown();workers.shutdownNow();assertTrue(workers.awaitTermination(10,TimeUnit.SECONDS));}
    }
    @Test void concorrenciaAposRollbackRemoveObjetoDaPerdedoraEConfirmaSucessora() throws Exception {
        service.adicionar(owner,file(png));service.adicionar(owner,file(png));
        Set<String> initial=objects();proxy.mode=MinioFaultProxy.Mode.GATE_PUT;
        var workers=Executors.newFixedThreadPool(2);
        try {
            var first=workers.submit(()->tx.execute(status->{
                var result=service.adicionar(owner,file(png));status.setRollbackOnly();return result;
            }));
            assertTrue(proxy.reached.await(10,TimeUnit.SECONDS));
            var uploaded=new HashSet<>(objects());uploaded.removeAll(initial);assertEquals(1,uploaded.size());
            var second=workers.submit(()->service.adicionar(owner,file(png)));
            boolean blocked=false;long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
            try(var c=dataSource.getConnection();var s=c.createStatement()) {
                while(System.nanoTime()<deadline) {
                    try(var rs=s.executeQuery("select count(*) from pg_stat_activity where datname=current_database() and wait_event_type='Lock' and cardinality(pg_blocking_pids(pid))>0")) {
                        rs.next();if(rs.getInt(1)>0){blocked=true;break;}
                    }Thread.sleep(10);
                }
            }
            assertTrue(blocked);proxy.release.countDown();
            Long rolledBack=first.get(10,TimeUnit.SECONDS).id();Long committed=second.get(10,TimeUnit.SECONDS).id();
            assertNotEquals(rolledBack,committed);assertEquals(3,rows());assertEquals(3,objects().size());
            assertTrue(Collections.disjoint(uploaded,objects()));
            Set<String> persisted=tx(()->new HashSet<>(fotos.findAllByArvoreIdOrderByIdAsc(owner).stream().map(FotoArvore::getObjeto).toList()));
            assertEquals(persisted,objects());evidence("Concorrencia com rollback da primeira e commit da sucessora");
        } finally {proxy.release.countDown();workers.shutdownNow();assertTrue(workers.awaitTermination(10,TimeUnit.SECONDS));}
    }
    private void crash(String operation,Long photoId) throws Exception {
        String cp=System.getProperty("surefire.test.class.path",System.getProperty("java.class.path"));
        var args=new ArrayList<>(List.of(Path.of(System.getProperty("java.home"),"bin","java").toString(),
                "-Darborize.it.schema-action=validate","-cp",cp,FotoArvoreCrashProbe.class.getName(),operation,owner.toString()));
        if(photoId!=null)args.add(photoId.toString());
        var builder=new ProcessBuilder(args).redirectErrorStream(true).redirectOutput(Path.of("target/minio-it-crash-"+operation+".log").toFile());
        builder.environment().put("ARBORIZE_IT_MINIO_PROXY_URL",proxy.endpoint());
        Process process=builder.start();
        try {
            assertTrue(proxy.reached.await(20,TimeUnit.SECONDS),"Processo filho deve alcançar operação real no MinIO antes da interrupção");
            process.destroyForcibly();assertTrue(process.waitFor(10,TimeUnit.SECONDS));
        } finally {if(process.isAlive())process.destroyForcibly();proxy.release.countDown();}
        tx(()->{em.createNativeQuery("set local lock_timeout='5s'").executeUpdate();repo.findByIdParaFotografias(owner).orElseThrow();return null;});
    }
    @Test void fInterrupcaoAposUploadRealDeixaOrfaoSemCompensacaoEmMemoria() throws Exception {
        proxy.mode=MinioFaultProxy.Mode.GATE_PUT;crash("upload",null);
        assertEquals(0,rows());assertEquals(1,objects().size());evidence("F processo encerrado apos upload antes do ACK");
    }
    @Test void fInterrupcaoAposRemocaoRealDeixaMetadadoSemObjeto() throws Exception {
        var photo=service.adicionar(owner,file(png));proxy.mode=MinioFaultProxy.Mode.GATE_DELETE;crash("remove",photo.id());
        assertEquals(1,rows());assertTrue(objects().isEmpty());evidence("F processo encerrado apos DELETE antes do ACK");
    }
}
