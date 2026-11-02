package br.com.arborize.controllers;

import br.com.arborize.dtos.request.ArvoreAtualizacaoRequestDTO;
import br.com.arborize.dtos.response.*;
import br.com.arborize.enums.*;
import br.com.arborize.services.ArvoreService;
import br.com.arborize.exceptions.EntradaInventarioInvalidaException;
import org.springframework.test.web.servlet.ResultMatcher;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/** Contrato funcional isolado; não representa liberação de acesso na configuração real. */
class ArvoreAtualizacaoControllerTest {
    private ArvoreService service;
    private MockMvc mvc;
    private static final String VALID_JSON="""
            {"especieId":8,"latitude":-4.98,"longitude":-39.02,
             "classificacaoTerritorial":"BAIRRO_NAO_IDENTIFICADO",
             "condicaoFitossanitaria":"MORTA","observacoes":null}
            """;

    @BeforeEach
    void prepare() {
        service=mock(ArvoreService.class);
        var validator=new LocalValidatorFactoryBean();validator.setMessageInterpolator(new ParameterMessageInterpolator());
        validator.afterPropertiesSet();
        mvc=standaloneSetup(new ArvoreController(service)).setValidator(validator)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private ArvoreResponseDTO response() {
        var criado=LocalDateTime.of(2025,1,1,10,0);
        return new ArvoreResponseDTO(42L,"ARV-QXD-000042",new EspecieIdentificacaoResponseDTO(8L,"Espécie de teste","Nome de teste",null),
                new BigDecimal("-4.98"),new BigDecimal("-39.02"),ClassificacaoTerritorial.BAIRRO_NAO_IDENTIFICADO,
                null,null,null,null,null,CondicaoFitossanitaria.MORTA,null,criado,criado.plusDays(1));
    }

    private ResultMatcher problema(int expectedStatus, String code) {
        return result -> {
            status().is(expectedStatus).match(result);
            content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON).match(result);
            jsonPath("$.status").value(expectedStatus).match(result);
            jsonPath("$.type").value("urn:arborize:inventario:" + code).match(result);
            jsonPath("$.title").isNotEmpty().match(result);
            jsonPath("$.detail").isNotEmpty().match(result);
            jsonPath("$.code").value(code).match(result);
            jsonPath("$.trace").doesNotExist().match(result);
            jsonPath("$.stackTrace").doesNotExist().match(result);
            jsonPath("$.exception").doesNotExist().match(result);
        };
    }

    @Test
    void putRetorna200ComFichaAtualizadaEDelegaIdDaRotaEDto() throws Exception {
        when(service.atualizarArvore(eq(42L),any())).thenReturn(response());
        mvc.perform(put("/api/arvores/42").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.especie.id").value(8)).andExpect(jsonPath("$.latitude").value(-4.98))
                .andExpect(jsonPath("$.longitude").value(-39.02))
                .andExpect(jsonPath("$.condicaoFitossanitaria").value("MORTA"));
        var captor=ArgumentCaptor.forClass(ArvoreAtualizacaoRequestDTO.class);
        verify(service).atualizarArvore(eq(42L),captor.capture());assertNull(captor.getValue().observacoes());
        assertNull(captor.getValue().dataPlantio());assertEquals(8L,captor.getValue().especieId());
        verifyNoMoreInteractions(service);
    }

    @Test
    void putTerritorialTextualAceitaNomeECepSemBairroId() throws Exception {
        var criado=LocalDateTime.of(2025,1,1,10,0);
        when(service.atualizarArvore(eq(42L),any())).thenReturn(new ArvoreResponseDTO(
                42L,"ARV-QXD-000042",new EspecieIdentificacaoResponseDTO(8L,"Espécie de teste","Nome de teste",null),
                new BigDecimal("-4.98"),new BigDecimal("-39.02"),ClassificacaoTerritorial.BAIRRO_IDENTIFICADO,
                null,"Centro","06390-000",null,null,null,null,CondicaoFitossanitaria.MORTA,null,criado,criado.plusDays(1)));
        String json="""
                {"especieId":8,"latitude":-4.98,"longitude":-39.02,
                 "classificacaoTerritorial":"BAIRRO_IDENTIFICADO","bairroNome":"Centro",
                 "cep":"06390-000","condicaoFitossanitaria":"MORTA"}
                """;

        mvc.perform(put("/api/arvores/42").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bairroNome").value("Centro"))
                .andExpect(jsonPath("$.cep").value("06390-000"))
                .andExpect(jsonPath("$.bairro").doesNotExist());
        var captor=ArgumentCaptor.forClass(ArvoreAtualizacaoRequestDTO.class);
        verify(service).atualizarArvore(eq(42L),captor.capture());
        assertNull(captor.getValue().bairroId());
        assertEquals("Centro",captor.getValue().bairroNome());
        assertEquals("06390-000",captor.getValue().cep());
        verifyNoMoreInteractions(service);
    }

    @Test
    void idEMetadadosDoCorpoNaoAlteramIdDaRotaOuFichaRetornada() throws Exception {
        when(service.atualizarArvore(eq(42L),any())).thenReturn(response());
        mvc.perform(put("/api/arvores/42").contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_JSON.replace("}",",\"id\":999,\"criadoEm\":\"1900-01-01T00:00:00\"}")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.criadoEm").value(org.hamcrest.Matchers.startsWith("2025-01-01")));
        verify(service).atualizarArvore(eq(42L),any(ArvoreAtualizacaoRequestDTO.class));
    }

    @Test
    void camposInvalidosRetornam400SemService() throws Exception {
        for (String json : new String[]{"{}",VALID_JSON.replace("-4.98","91"),VALID_JSON.replace("-39.02","181"),
                VALID_JSON.replace("}",",\"dapCm\":0}"),VALID_JSON.replace("}",",\"alturaEstimadaM\":-1}"),
                VALID_JSON.replace("BAIRRO_NAO_IDENTIFICADO","BAIRRO_IDENTIFICADO")}) {
            mvc.perform(put("/api/arvores/42").contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(problema(400, "entrada-invalida"))
                    .andExpect(jsonPath("$.errors[0].field").isNotEmpty())
                    .andExpect(jsonPath("$.errors[0].message").isNotEmpty())
                    .andExpect(jsonPath("$.errors[0].rejectedValue").doesNotExist());
        }
        verifyNoInteractions(service);
    }

    @Test
    void idEnumEJsonMalformadosRetornam400SemService() throws Exception {
        mvc.perform(put("/api/arvores/id-invalido").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(problema(400, "requisicao-invalida"));
        for (String json : new String[]{"{",VALID_JSON.replace("MORTA","INVALIDA")}) {
            mvc.perform(put("/api/arvores/42").contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(problema(400, "requisicao-invalida"));
        }
        verifyNoInteractions(service);
    }

    @Test
    void arvoreEspecieOuBairroInexistentesRetornam404() throws Exception {
        for (String recurso : new String[]{"Árvore","Espécie","Bairro"}) {
            doThrow(new EntityNotFoundException(recurso+" não encontrado")).when(service).atualizarArvore(eq(42L),any());
            mvc.perform(put("/api/arvores/42").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                    .andExpect(problema(404, "recurso-nao-encontrado"))
                    .andExpect(content().string(not(containsString(recurso+" não encontrado"))));
        }
    }

    @Test
    void falhaInesperadaRetorna500SemDetalhesInternos() throws Exception {
        when(service.atualizarArvore(eq(42L),any())).thenThrow(new RuntimeException("SEGREDO_INTERNO jdbc:postgresql://servidor/banco /srv/arborize senha=interna"));
        mvc.perform(put("/api/arvores/42").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(problema(500, "erro-interno"))
                .andExpect(content().string(not(containsString("SEGREDO_INTERNO"))))
                .andExpect(content().string(not(containsString("jdbc:postgresql"))))
                .andExpect(content().string(not(containsString("/srv/arborize"))))
                .andExpect(content().string(not(containsString("senha="))));
    }

    @Test
    void bairroIndisponivelRetorna400ComProblemaECampo() throws Exception {
        when(service.atualizarArvore(eq(42L),any())).thenThrow(new EntradaInventarioInvalidaException(
                "bairroId", "O bairro informado não está disponível para associação."));
        String json=VALID_JSON.replace("BAIRRO_NAO_IDENTIFICADO", "BAIRRO_IDENTIFICADO")
                .replace("}", ",\"bairroId\":2}");
        mvc.perform(put("/api/arvores/42").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(problema(400, "entrada-invalida"))
                .andExpect(jsonPath("$.errors[0].field").value("bairroId"))
                .andExpect(jsonPath("$.errors[0].message")
                        .value("O bairro informado não está disponível para associação."));
        verify(service).atualizarArvore(eq(42L),any(ArvoreAtualizacaoRequestDTO.class));
        verifyNoMoreInteractions(service);
    }

    @Test
    void identificadorNumericoNaoPositivoRetorna400ComProblemaECampo() throws Exception {
        for (long id : new long[]{0,-1}) {
            doThrow(new EntradaInventarioInvalidaException(
                    "id", "O identificador da árvore deve ser positivo."))
                    .when(service).atualizarArvore(eq(id),any());
            mvc.perform(put("/api/arvores/"+id).contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                    .andExpect(problema(400, "entrada-invalida"))
                    .andExpect(jsonPath("$.errors[0].field").value("id"))
                    .andExpect(jsonPath("$.errors[0].message").isNotEmpty());
            verify(service).atualizarArvore(eq(id),any(ArvoreAtualizacaoRequestDTO.class));
        }
        verifyNoMoreInteractions(service);
    }

}
