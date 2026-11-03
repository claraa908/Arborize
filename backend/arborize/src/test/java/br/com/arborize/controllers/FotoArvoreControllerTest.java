package br.com.arborize.controllers;

import br.com.arborize.dtos.response.*;
import br.com.arborize.exceptions.*;
import br.com.arborize.services.FotoArvoreService;
import br.com.arborize.validation.FotoArvoreValidatorTest;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import java.time.LocalDateTime;
import java.util.List;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/** Contratos HTTP com Service simulado; não testa PostgreSQL, MinIO nem atomicidade distribuída. */
class FotoArvoreControllerTest {
    private FotoArvoreService service;
    private MockMvc mvc;

    @BeforeEach
    void prepare() {
        service=mock(FotoArvoreService.class);
        mvc=standaloneSetup(new FotoArvoreController(service)).setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private FotoArvoreResponseDTO dto(String tipo, long size) {
        return new FotoArvoreResponseDTO(7L,42L,tipo,size,LocalDateTime.of(2026,1,1,12,0));
    }

    private ResultMatcher problema(int status, String code) {
        return result -> {
            status().is(status).match(result);
            content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON).match(result);
            jsonPath("$.status").value(status).match(result);
            jsonPath("$.code").value(code).match(result);
            jsonPath("$.type").value("urn:arborize:inventario:"+code).match(result);
            jsonPath("$.title").isNotEmpty().match(result);
            jsonPath("$.detail").isNotEmpty().match(result);
            jsonPath("$.trace").doesNotExist().match(result);
            jsonPath("$.exception").doesNotExist().match(result);
            content().string(not(containsString("SEGREDO_INTERNO"))).match(result);
        };
    }

    private void uploadValido(String formato, String tipo) throws Exception {
        byte[] bytes=FotoArvoreValidatorTest.imagem(formato);
        var file=new MockMultipartFile("file","foto."+formato,"application/octet-stream",bytes);
        when(service.adicionar(42L,file)).thenReturn(dto(tipo,bytes.length));
        mvc.perform(multipart("/api/arvores/42/fotos").file(file))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location","http://localhost/api/arvores/42/fotos/7"))
                .andExpect(jsonPath("$.id").value(7)).andExpect(jsonPath("$.arvoreId").value(42))
                .andExpect(jsonPath("$.contentType").value(tipo)).andExpect(jsonPath("$.sizeBytes").value(bytes.length))
                .andExpect(jsonPath("$.criadoEm").isNotEmpty())
                .andExpect(jsonPath("$.objeto").doesNotExist()).andExpect(jsonPath("$.bucket").doesNotExist())
                .andExpect(jsonPath("$.url").doesNotExist());
        verify(service).adicionar(42L,file);verifyNoMoreInteractions(service);
    }

    @Test void uploadJpgRetorna201MetadadosELocation() throws Exception {uploadValido("jpeg","image/jpeg");}
    @Test void uploadPngRetorna201MetadadosELocation() throws Exception {uploadValido("png","image/png");}
    @Test void uploadWebpRetorna201MetadadosELocation() throws Exception {uploadValido("webp","image/webp");}

    @Test
    void campoFileObrigatorioRetorna400SemService() throws Exception {
        mvc.perform(multipart("/api/arvores/42/fotos")).andExpect(problema(400,"requisicao-invalida"));
        mvc.perform(multipart("/api/arvores/42/fotos").file(new MockMultipartFile("foto",new byte[]{1})))
                .andExpect(problema(400,"requisicao-invalida"));
        verifyNoInteractions(service);
    }

    @Test
    void arquivoVazioInvalidoOuQuartaFotoRetornam400() throws Exception {
        for (String mensagem : List.of("Selecione uma fotografia não vazia.",
                "O arquivo não contém uma imagem JPG, PNG ou WebP válida.","Cada árvore pode possuir até três fotografias.")) {
            var file=new MockMultipartFile("file",mensagem.startsWith("Selecione")?new byte[0]
                    :mensagem.startsWith("Cada")?FotoArvoreValidatorTest.imagem("png"):new byte[]{1,2,3});
            doThrow(new EntradaInventarioInvalidaException("fotografias",mensagem)).when(service).adicionar(42L,file);
            mvc.perform(multipart("/api/arvores/42/fotos").file(file))
                    .andExpect(problema(400,"entrada-invalida"))
                    .andExpect(jsonPath("$.errors[0].field").value("fotografias"))
                    .andExpect(jsonPath("$.errors[0].message").value(mensagem));
        }
    }

    @Test
    void tamanhoAcimaDoLimiteRetorna413() throws Exception {
        var file=new MockMultipartFile("file",new byte[2*1024*1024+1]);
        doThrow(new FotografiaMuitoGrandeException()).when(service).adicionar(42L,file);
        mvc.perform(multipart("/api/arvores/42/fotos").file(file))
                .andExpect(problema(413,"fotografia-muito-grande"))
                .andExpect(jsonPath("$.errors[0].field").value("fotografias"));
    }

    @Test
    void uploadDeArvoreInexistenteRetorna404() throws Exception {
        var file=new MockMultipartFile("file",FotoArvoreValidatorTest.imagem("png"));
        doThrow(new EntityNotFoundException("SEGREDO_INTERNO")).when(service).adicionar(42L,file);
        mvc.perform(multipart("/api/arvores/42/fotos").file(file))
                .andExpect(problema(404,"recurso-nao-encontrado"));
    }

    @Test
    void recuperacaoRespeitaMimeEConfiguraCabecalhosSemExporArmazenamento() throws Exception {
        for (String[] formato : new String[][]{{"jpeg","image/jpeg"},{"png","image/png"},{"webp","image/webp"}}) {
            byte[] bytes=FotoArvoreValidatorTest.imagem(formato[0]);
            when(service.recuperar(42L,7L)).thenReturn(new FotoArvoreConteudoDTO(dto(formato[1],bytes.length),bytes));
            mvc.perform(get("/api/arvores/42/fotos/7"))
                    .andExpect(status().isOk()).andExpect(content().contentType(formato[1]))
                    .andExpect(content().bytes(bytes)).andExpect(header().longValue("Content-Length",bytes.length))
                    .andExpect(header().string("X-Content-Type-Options","nosniff"))
                    .andExpect(header().string("Cache-Control","no-store"))
                    .andExpect(header().string("Content-Disposition","inline; filename=\"arvore-42-foto-7\""))
                    .andExpect(header().doesNotExist("Location"));
        }
        verify(service,times(3)).recuperar(42L,7L);verifyNoMoreInteractions(service);
    }

    @Test
    void fotoInexistenteEDeOutraArvoreRetornamMesmo404() throws Exception {
        for (String mensagem : List.of("Fotografia não encontrada.","SEGREDO_INTERNO de outra árvore")) {
            doThrow(new EntityNotFoundException(mensagem)).when(service).recuperar(42L,7L);
            mvc.perform(get("/api/arvores/42/fotos/7"))
                    .andExpect(problema(404,"recurso-nao-encontrado"))
                    .andExpect(jsonPath("$.detail").value("O recurso solicitado não foi encontrado."));
        }
        verify(service,times(2)).recuperar(42L,7L);
    }

    @Test
    void galeriaRetornaSomenteMetadadosOuListaVazia() throws Exception {
        when(service.listar(42L)).thenReturn(List.of(dto("image/png",100)));
        mvc.perform(get("/api/arvores/42/fotos")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7)).andExpect(jsonPath("$[0].arvoreId").value(42))
                .andExpect(jsonPath("$[0].conteudo").doesNotExist()).andExpect(jsonPath("$[0].objeto").doesNotExist())
                .andExpect(jsonPath("$[0].bucket").doesNotExist()).andExpect(header().string("Cache-Control","no-store"));
        when(service.listar(42L)).thenReturn(List.of());
        mvc.perform(get("/api/arvores/42/fotos")).andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(service,times(2)).listar(42L);verifyNoMoreInteractions(service);
    }

    @Test
    void galeriaDeArvoreInexistenteRetorna404() throws Exception {
        when(service.listar(42L)).thenThrow(new EntityNotFoundException("SEGREDO_INTERNO"));
        mvc.perform(get("/api/arvores/42/fotos")).andExpect(problema(404,"recurso-nao-encontrado"));
    }

    @Test
    void remocaoExplicitaRetorna204SemConteudo() throws Exception {
        mvc.perform(delete("/api/arvores/42/fotos/7")).andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(service).remover(42L,7L);verifyNoMoreInteractions(service);
    }

    @Test
    void remocaoDeFotoInexistenteOuOutraArvoreRetorna404() throws Exception {
        doThrow(new EntityNotFoundException("SEGREDO_INTERNO")).when(service).remover(42L,7L);
        mvc.perform(delete("/api/arvores/42/fotos/7")).andExpect(problema(404,"recurso-nao-encontrado"));
    }

    @Test
    void identificadoresMalformadosRetornam400SemService() throws Exception {
        for (String path : List.of("/api/arvores/texto/fotos","/api/arvores/42/fotos/texto")) {
            mvc.perform(get(path)).andExpect(problema(400,"requisicao-invalida"));
        }
        mvc.perform(multipart("/api/arvores/texto/fotos").file(new MockMultipartFile("file",new byte[]{1})))
                .andExpect(problema(400,"requisicao-invalida"));
        mvc.perform(delete("/api/arvores/42/fotos/texto")).andExpect(problema(400,"requisicao-invalida"));
        verifyNoInteractions(service);
    }

    @Test
    void falhasDeArmazenamentoNaoInformamSucessoNemDetalhesInternos() throws Exception {
        var falha=new ArmazenamentoFotografiaException("SEGREDO_INTERNO",new RuntimeException("senha /srv/minio"));
        var file=new MockMultipartFile("file",FotoArvoreValidatorTest.imagem("png"));
        doThrow(falha).when(service).adicionar(42L,file);
        doThrow(falha).when(service).recuperar(42L,7L);
        doThrow(falha).when(service).remover(42L,7L);
        mvc.perform(multipart("/api/arvores/42/fotos").file(file)).andExpect(problema(500,"erro-interno"));
        mvc.perform(get("/api/arvores/42/fotos/7")).andExpect(problema(500,"erro-interno"));
        mvc.perform(delete("/api/arvores/42/fotos/7")).andExpect(problema(500,"erro-interno"));
    }

    @Test
    void falhaInesperadaNaoViraErroDeValidacao() throws Exception {
        when(service.listar(42L)).thenThrow(new IllegalArgumentException("SEGREDO_INTERNO"));
        mvc.perform(get("/api/arvores/42/fotos")).andExpect(problema(500,"erro-interno"));
    }
}
