package br.com.arborize.controllers;

import br.com.arborize.exceptions.EntradaInventarioInvalidaException;
import br.com.arborize.services.*;
import br.com.arborize.dtos.request.ArvoreCadastroRequestDTO;
import br.com.arborize.enums.ClassificacaoTerritorial;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class GlobalExceptionHandlerTest {
    private ArvoreService service;
    private MockMvc mvc;
    private LocalValidatorFactoryBean validator;
    private static final String VALID_JSON = """
            {"especieId":7,"latitude":-4.97,"longitude":-39.01,
             "classificacaoTerritorial":"BAIRRO_NAO_IDENTIFICADO"}
            """;

    @BeforeEach
    void prepare() {
        service = mock(ArvoreService.class);
        validator = new LocalValidatorFactoryBean();
        validator.setMessageInterpolator(new ParameterMessageInterpolator());
        validator.afterPropertiesSet();
        mvc = standaloneSetup(new ArvoreController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).setValidator(validator).build();
    }

    @Test
    void validacaoRetornaProblemaComCamposSemValoresRejeitados() throws Exception {
        for (String json : new String[]{"{}", VALID_JSON.replace("-4.97", "91"),
                VALID_JSON.replace("-39.01", "181"),
                VALID_JSON.replace("}", ",\"dapCm\":0}"),
                VALID_JSON.replace("}", ",\"alturaEstimadaM\":-1}"),
                VALID_JSON.replace("BAIRRO_NAO_IDENTIFICADO", "BAIRRO_IDENTIFICADO")}) {
            mvc.perform(post("/api/arvores").contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.code").value("entrada-invalida"))
                    .andExpect(jsonPath("$.type").value("urn:arborize:inventario:entrada-invalida"))
                    .andExpect(jsonPath("$.detail").isNotEmpty())
                    .andExpect(jsonPath("$.errors[0].field").isNotEmpty())
                    .andExpect(jsonPath("$.errors[0].message").isNotEmpty())
                    .andExpect(jsonPath("$.errors[0].rejectedValue").doesNotExist());
        }
        verifyNoInteractions(service);
    }

    @Test
    void jsonEnumEIdentificadorMalformadosRetornam400Sanitizado() throws Exception {
        for (String json : new String[]{"{", VALID_JSON.replace("BAIRRO_NAO_IDENTIFICADO", "SEGREDO_INTERNO"),
                VALID_JSON.replace("-4.97", "\"SEGREDO_INTERNO\"")}) {
            mvc.perform(post("/api/arvores").contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.code").value("requisicao-invalida"))
                    .andExpect(content().string(not(containsString("SEGREDO_INTERNO"))))
                    .andExpect(content().string(not(containsString("br.com.arborize"))));
        }
        mvc.perform(get("/api/arvores/identificador-invalido"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        verifyNoInteractions(service);
    }

    @Test
    void arvoreEspecieEBairroInexistentesRetornam404SemExporMensagemDaExcecao() throws Exception {
        when(service.buscarPorId(42L)).thenThrow(new EntityNotFoundException("SEGREDO_INTERNO"));
        mvc.perform(get("/api/arvores/42")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("recurso-nao-encontrado"))
                .andExpect(content().string(not(containsString("SEGREDO_INTERNO"))));
        for (String recurso : List.of("Espécie", "Bairro")) {
            doThrow(new EntityNotFoundException(recurso + " SEGREDO_INTERNO")).when(service).criarArvore(any());
            mvc.perform(post("/api/arvores").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.code").value("recurso-nao-encontrado"))
                    .andExpect(content().string(not(containsString("SEGREDO_INTERNO"))));
        }
    }

    @Test
    void rejeicaoEsperadaDeNegocioRetorna400ComCampo() throws Exception {
        when(service.criarArvore(any())).thenThrow(new EntradaInventarioInvalidaException(
                "bairroId", "O bairro informado não está disponível para associação."));
        mvc.perform(post("/api/arvores").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("bairroId"));
        when(service.buscarPorId(0L)).thenThrow(new EntradaInventarioInvalidaException(
                "id", "O identificador da árvore deve ser positivo."));
        mvc.perform(get("/api/arvores/0")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("id"));
    }

    @Test
    void constraintViolationDoServiceRetorna400ComCampos() throws Exception {
        var invalid = new ArvoreCadastroRequestDTO(null, null, null,
                ClassificacaoTerritorial.BAIRRO_NAO_IDENTIFICADO, null, null, null, null, null, null, null);
        when(service.criarArvore(any())).thenThrow(new ConstraintViolationException(validator.validate(invalid)));
        mvc.perform(post("/api/arvores").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("entrada-invalida"))
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    void falhaInesperadaInclusiveIllegalArgumentNaoViraErroDeEntrada() throws Exception {
        for (RuntimeException failure : new RuntimeException[]{new RuntimeException("senha=SEGREDO_INTERNO /srv/banco"),
                new IllegalArgumentException("SEGREDO_INTERNO"), new IllegalStateException("SEGREDO_INTERNO")}) {
            doThrow(failure).when(service).buscarPorId(42L);
            mvc.perform(get("/api/arvores/42")).andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.code").value("erro-interno"))
                    .andExpect(jsonPath("$.detail").value("Não foi possível concluir a operação. Tente novamente."))
                    .andExpect(jsonPath("$.trace").doesNotExist())
                    .andExpect(content().string(not(containsString("SEGREDO_INTERNO"))))
                    .andExpect(content().string(not(containsString("/srv/banco"))));
        }
    }

    @Test
    void handlerGlobalPreservaSucessoLegadoEPadronizaFalhasNaoTratadas() throws Exception {
        var especies = mock(EspecieService.class);
        var eventos = mock(EventosService.class);
        var materiais = mock(EducationalMaterialService.class);
        var especieController = new EspecieController();
        var eventoController = new EventosController();
        var materialController = new EducationalMaterialController();
        org.springframework.test.util.ReflectionTestUtils.setField(especieController, "service", especies);
        org.springframework.test.util.ReflectionTestUtils.setField(eventoController, "service", eventos);
        org.springframework.test.util.ReflectionTestUtils.setField(materialController, "service", materiais);
        var publicMvc = standaloneSetup(especieController, eventoController, materialController)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        when(especies.listarTodos()).thenReturn(List.of());
        when(eventos.listarTodos()).thenReturn(List.of());
        when(materiais.getPublicMaterials(null, null)).thenReturn(List.of());
        for (String path : List.of("/especies", "/eventos", "/api/materials")) {
            publicMvc.perform(get(path)).andExpect(status().isOk()).andExpect(content().json("[]"));
        }
        when(materiais.getMaterialById(42L)).thenThrow(new EntityNotFoundException("Erro original"));
        publicMvc.perform(get("/api/materials/42"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("recurso-nao-encontrado"))
                .andExpect(content().string(not(containsString("Erro original"))));
    }
}
