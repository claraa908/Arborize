package br.com.arborize.controllers;

import br.com.arborize.dtos.request.ArvoreCadastroRequestDTO;
import br.com.arborize.dtos.response.ArvoreResponseDTO;
import br.com.arborize.dtos.response.EspecieIdentificacaoResponseDTO;
import br.com.arborize.enums.ClassificacaoTerritorial;
import br.com.arborize.enums.CondicaoFitossanitaria;
import br.com.arborize.services.ArvoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

// Contrato HTTP isolado, sem filtros. A segurança real é verificada em InventarioSecurityTest.
class ArvoreControllerTest {
    private ArvoreService service;
    private MockMvc mvc;
    private static final String VALID_JSON = """
            {"especieId": 7, "latitude": -4.97, "longitude": -39.01,
             "classificacaoTerritorial": "BAIRRO_NAO_IDENTIFICADO"}
            """;

    @BeforeEach
    void prepare() {
        service = mock(ArvoreService.class);
        var validator = new LocalValidatorFactoryBean();
        validator.setMessageInterpolator(new org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator());
        validator.afterPropertiesSet();
        mvc = standaloneSetup(new ArvoreController(service)).setControllerAdvice(new GlobalExceptionHandler()).setValidator(validator).build();
    }

    private ArvoreResponseDTO response() {
        var timestamp = LocalDateTime.of(2026, 10, 31, 12, 0);
        return new ArvoreResponseDTO(42L, "ARV-QXD-000042", new EspecieIdentificacaoResponseDTO(7L, "Espécie de teste", "Nome de teste", null),
                new BigDecimal("-4.97"), new BigDecimal("-39.01"),
                ClassificacaoTerritorial.BAIRRO_NAO_IDENTIFICADO, null, null, null, null, null,
                CondicaoFitossanitaria.NAO_AVALIADA, null, timestamp, timestamp);
    }

    @Test
    void cadastroRetornaCreatedIdGeradoELocationEDelegaDtoAoService() throws Exception {
        when(service.criarArvore(any())).thenReturn(response());
        mvc.perform(post("/api/arvores").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/arvores/42"))
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.codigo").value("ARV-QXD-000042"))
                .andExpect(jsonPath("$.especie.id").value(7))
                .andExpect(jsonPath("$.condicaoFitossanitaria").value("NAO_AVALIADA"));
        var captor = ArgumentCaptor.forClass(ArvoreCadastroRequestDTO.class);
        verify(service).criarArvore(captor.capture());
        assertEquals(7L, captor.getValue().especieId());
        assertEquals(new BigDecimal("-4.97"), captor.getValue().latitude());
        assertNull(captor.getValue().dapCm());
        verifyNoMoreInteractions(service);
    }

    @Test
    void cadastroTerritorialTextualNaoExigeBairroIdEDevolveBairroECEP() throws Exception {
        var textual = new ArvoreResponseDTO(42L, "ARV-QXD-000042",
                new EspecieIdentificacaoResponseDTO(7L, "Espécie de teste", "Nome de teste", null),
                new BigDecimal("-4.97"), new BigDecimal("-39.01"), ClassificacaoTerritorial.BAIRRO_IDENTIFICADO,
                null, "Centro", "06390-000", null, null, null, null,
                CondicaoFitossanitaria.NAO_AVALIADA, null,
                LocalDateTime.of(2026, 10, 31, 12, 0), LocalDateTime.of(2026, 10, 31, 12, 0));
        when(service.criarArvore(any())).thenReturn(textual);
        String json = """
                {"especieId":7,"latitude":-4.97,"longitude":-39.01,
                 "classificacaoTerritorial":"BAIRRO_IDENTIFICADO",
                 "bairroNome":"Centro","cep":"06390-000"}
                """;

        mvc.perform(post("/api/arvores").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bairroNome").value("Centro"))
                .andExpect(jsonPath("$.cep").value("06390-000"))
                .andExpect(jsonPath("$.bairro").doesNotExist());
        var captor = ArgumentCaptor.forClass(ArvoreCadastroRequestDTO.class);
        verify(service).criarArvore(captor.capture());
        assertNull(captor.getValue().bairroId());
        assertEquals("Centro", captor.getValue().bairroNome());
        assertEquals("06390-000", captor.getValue().cep());
        verifyNoMoreInteractions(service);
    }

    @Test
    void consultaRetornaDetalhesEDelegaIdentificadorAoService() throws Exception {
        when(service.buscarPorId(42L)).thenReturn(response());
        mvc.perform(get("/api/arvores/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.codigo").value("ARV-QXD-000042"))
                .andExpect(jsonPath("$.especie.id").value(7))
                .andExpect(jsonPath("$.classificacaoTerritorial").value("BAIRRO_NAO_IDENTIFICADO"));
        verify(service).buscarPorId(42L);
        verifyNoMoreInteractions(service);
    }

    @Test
    void beanValidationRejeitaEntradasInvalidasSemChamarService() throws Exception {
        for (String json : new String[]{"{}", VALID_JSON.replace("-4.97", "91"),
                VALID_JSON.replace("-39.01", "181"),
                VALID_JSON.replace("7", "0"),
                VALID_JSON.replace("BAIRRO_NAO_IDENTIFICADO", "BAIRRO_IDENTIFICADO"),
                VALID_JSON.replace("}", ", \"dapCm\": 0}")}) {
            mvc.perform(post("/api/arvores").contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }

    @Test
    void rejeitaCorpoAusenteOuJsonMalformadoSemChamarService() throws Exception {
        for (String json : new String[]{"", "{"}) {
            mvc.perform(post("/api/arvores").contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }

    @Test
    void identificadorEnviadoNaoControlaIdentificadorGerado() throws Exception {
        when(service.criarArvore(any())).thenReturn(response());
        mvc.perform(post("/api/arvores").contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_JSON.replace("}", ", \"id\": 999}")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(42));
        verify(service).criarArvore(any(ArvoreCadastroRequestDTO.class));
        assertFalse(java.util.Arrays.stream(ArvoreCadastroRequestDTO.class.getRecordComponents())
                .anyMatch(component -> component.getName().equals("id")));
        verifyNoMoreInteractions(service);
    }
}
