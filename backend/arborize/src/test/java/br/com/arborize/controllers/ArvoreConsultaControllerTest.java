package br.com.arborize.controllers;

import br.com.arborize.dtos.response.*;
import br.com.arborize.enums.*;
import br.com.arborize.exceptions.EntradaInventarioInvalidaException;
import br.com.arborize.services.ArvoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/** Contratos funcionais isolados. O bloqueio real é exercitado por InventarioSecurityTest. */
class ArvoreConsultaControllerTest {
    private ArvoreService service;
    private MockMvc mvc;

    @BeforeEach
    void prepare() {
        service=mock(ArvoreService.class);
        mvc=standaloneSetup(new ArvoreController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private ArvoreResumoResponseDTO resumo() {
        return new ArvoreResumoResponseDTO(42L,"ARV-QXD-000042",new EspecieIdentificacaoResponseDTO(7L,"Espécie de teste","Nome de teste",null),
                new BigDecimal("-4.97"),new BigDecimal("-39.01"),ClassificacaoTerritorial.BAIRRO_IDENTIFICADO,
                new BairroResumoResponseDTO(2L,"Referência de teste",true),"Referência",
                CondicaoFitossanitaria.NAO_AVALIADA,null);
    }

    @Test
    void listagemSemParametrosUsaPadroesERetornaContratoExplicito() throws Exception {
        var page=PageRequest.of(0,20);
        when(service.listar(null,null,null,null,null,page)).thenReturn(new PageImpl<>(List.of(resumo()),page,1));
        mvc.perform(get("/api/arvores")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(42))
                .andExpect(jsonPath("$.content[0].codigo").value("ARV-QXD-000042"))
                .andExpect(jsonPath("$.page").value(0)).andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.pageable").doesNotExist());
        verify(service).listar(null,null,null,null,null,page);verifyNoMoreInteractions(service);
    }

    @Test
    void listagemPaginadaTransmitePesquisaEFiltrosSemAlterarCriterios() throws Exception {
        var page=PageRequest.of(1,2);
        when(service.listar(" CAJU ",null,2L,7L,CondicaoFitossanitaria.MORTA,page))
                .thenReturn(new PageImpl<>(List.of(resumo()),page,5));
        mvc.perform(get("/api/arvores").param("search"," CAJU ").param("bairroId","2")
                        .param("especieId","7").param("condicao","MORTA").param("page","1").param("size","2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2)).andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));
        verify(service).listar(" CAJU ",null,2L,7L,CondicaoFitossanitaria.MORTA,page);verifyNoMoreInteractions(service);
    }

    @Test
    void filtrosOpcionaisPodemSerEnviadosIndividualmente() throws Exception {
        when(service.listar(any(),any(),any(),any(),any(),any())).thenReturn(Page.empty(PageRequest.of(0,20)));
        mvc.perform(get("/api/arvores").param("search","caju")).andExpect(status().isOk());
        verify(service).listar("caju",null,null,null,null,PageRequest.of(0,20));
        mvc.perform(get("/api/arvores").param("bairroId","2")).andExpect(status().isOk());
        verify(service).listar(null,null,2L,null,null,PageRequest.of(0,20));
        mvc.perform(get("/api/arvores").param("especieId","7")).andExpect(status().isOk());
        verify(service).listar(null,null,null,7L,null,PageRequest.of(0,20));
        mvc.perform(get("/api/arvores").param("condicao","MORTA")).andExpect(status().isOk());
        verify(service).listar(null,null,null,null,CondicaoFitossanitaria.MORTA,PageRequest.of(0,20));
    }

    @Test
    void bairroTextualAlimentaListagemMapaERespostasSemRemoverBairroLegado() throws Exception {
        var page = PageRequest.of(0, 20);
        var textual = new ArvoreResumoResponseDTO(42L, "ARV-QXD-000042",
                new EspecieIdentificacaoResponseDTO(7L, "Espécie de teste", "Nome de teste", null),
                new BigDecimal("-4.97"), new BigDecimal("-39.01"), ClassificacaoTerritorial.BAIRRO_IDENTIFICADO,
                new BairroResumoResponseDTO(2L, "Bairro legado", true), "Centro", "06390-000", "Referência",
                CondicaoFitossanitaria.NAO_AVALIADA, null);
        when(service.listar(null, " centro ", null, null, null, page))
                .thenReturn(new PageImpl<>(List.of(textual), page, 1));
        when(service.consultarMapa(null, " centro ", null, null, null)).thenReturn(List.of(textual));

        mvc.perform(get("/api/arvores").param("bairro", " centro "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].bairroNome").value("Centro"))
                .andExpect(jsonPath("$.content[0].cep").value("06390-000"))
                .andExpect(jsonPath("$.content[0].bairro.nome").value("Bairro legado"));
        mvc.perform(get("/api/arvores/mapa").param("bairro", " centro "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bairroNome").value("Centro"))
                .andExpect(jsonPath("$[0].cep").value("06390-000"))
                .andExpect(jsonPath("$[0].bairro.id").value(2));

        verify(service).listar(null, " centro ", null, null, null, page);
        verify(service).consultarMapa(null, " centro ", null, null, null);
        verifyNoMoreInteractions(service);
    }

    @Test
    void mapaRecebeFiltrosERetornaMarcadoresSemPaginacao() throws Exception {
        when(service.consultarMapa("caju",null,2L,7L,CondicaoFitossanitaria.MORTA)).thenReturn(List.of(resumo()));
        mvc.perform(get("/api/arvores/mapa").param("search","caju").param("bairroId","2")
                        .param("especieId","7").param("condicao","MORTA").param("page","99").param("size","1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(42))
                .andExpect(jsonPath("$[0].codigo").value("ARV-QXD-000042"))
                .andExpect(jsonPath("$[0].especie.id").value(7)).andExpect(jsonPath("$[0].latitude").value(-4.97))
                .andExpect(jsonPath("$[0].longitude").value(-39.01)).andExpect(jsonPath("$[0].bairro.id").value(2))
                .andExpect(jsonPath("$[0].condicaoFitossanitaria").value("NAO_AVALIADA"))
                .andExpect(jsonPath("$[0].documents").doesNotExist());
        verify(service).consultarMapa("caju",null,2L,7L,CondicaoFitossanitaria.MORTA);verifyNoMoreInteractions(service);
    }

    @Test
    void ausenciaDeResultadosNaoEErro() throws Exception {
        when(service.listar("sem resultado",null,null,null,null,PageRequest.of(0,20)))
                .thenReturn(Page.empty(PageRequest.of(0,20)));
        when(service.consultarMapa(null,null,null,null,null)).thenReturn(List.of());
        mvc.perform(get("/api/arvores").param("search","sem resultado")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty()).andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
        mvc.perform(get("/api/arvores/mapa")).andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(service).consultarMapa(null,null,null,null,null);
        verify(service,never()).buscarPorId(anyLong());
    }

    @Test
    void filtrosCombinadosSemResultadosRetornam200ComColecoesVazias() throws Exception {
        when(service.listar("sem resultado",null,2L,7L,CondicaoFitossanitaria.MORTA,PageRequest.of(0,20)))
                .thenReturn(Page.empty(PageRequest.of(0,20)));
        when(service.consultarMapa("sem resultado",null,2L,7L,CondicaoFitossanitaria.MORTA)).thenReturn(List.of());
        mvc.perform(get("/api/arvores").param("search","sem resultado").param("bairroId","2")
                        .param("especieId","7").param("condicao","MORTA"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/arvores/mapa").param("search","sem resultado").param("bairroId","2")
                        .param("especieId","7").param("condicao","MORTA"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(service).listar("sem resultado",null,2L,7L,CondicaoFitossanitaria.MORTA,PageRequest.of(0,20));
        verify(service).consultarMapa("sem resultado",null,2L,7L,CondicaoFitossanitaria.MORTA);
        verifyNoMoreInteractions(service);
    }

    @Test
    void paginacaoForaDosLimitesRetorna400ComCampo() throws Exception {
        for (String[] params : new String[][]{{"page","-1"},{"size","0"},{"size","-1"},{"size","101"}}) {
            mvc.perform(get("/api/arvores").param(params[0],params[1]))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.code").value("entrada-invalida"))
                    .andExpect(jsonPath("$.errors[0].field").value(params[0]));
        }
        verifyNoInteractions(service);
    }

    @Test
    void limiteMaximoDePaginaEPermitido() throws Exception {
        when(service.listar(null,null,null,null,null,PageRequest.of(0,100))).thenReturn(Page.empty(PageRequest.of(0,100)));
        mvc.perform(get("/api/arvores").param("size","100")).andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void parametrosMalformadosRetornam400EstruturadoSemChamarService() throws Exception {
        for (String endpoint : List.of("/api/arvores","/api/arvores/mapa")) {
            for (String[] params : new String[][]{{"bairroId","texto"},{"especieId","texto"},{"condicao","DESCONHECIDA"}}) {
                mvc.perform(get(endpoint).param(params[0],params[1])).andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.status").value(400)).andExpect(jsonPath("$.code").value("requisicao-invalida"));
            }
        }
        for (String campo : List.of("page","size")) {
            for (String valor : List.of("texto","999999999999999999999999")) {
                mvc.perform(get("/api/arvores").param(campo,valor)).andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.status").value(400));
            }
        }
        verifyNoInteractions(service);
    }

    @Test
    void rejeicaoDeFiltroPeloServiceReutilizaAdvice() throws Exception {
        when(service.consultarMapa(null,null,0L,null,null)).thenThrow(
                new EntradaInventarioInvalidaException("bairroId","O identificador do bairro deve ser positivo."));
        mvc.perform(get("/api/arvores/mapa").param("bairroId","0")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("bairroId"));
        verify(service).consultarMapa(null,null,0L,null,null);
    }

    @Test
    void falhasInesperadasDeConsultaRetornam500SemDetalhesInternos() throws Exception {
        when(service.listar(null,null,null,null,null,PageRequest.of(0,20))).thenThrow(new RuntimeException("SEGREDO_INTERNO"));
        when(service.consultarMapa(null,null,null,null,null)).thenThrow(new RuntimeException("SEGREDO_INTERNO"));
        for (String path : List.of("/api/arvores","/api/arvores/mapa")) {
            mvc.perform(get(path)).andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("erro-interno"))
                    .andExpect(content().string(not(containsString("SEGREDO_INTERNO"))));
        }
    }
}
