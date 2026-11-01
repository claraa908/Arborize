package br.com.arborize.controllers;

import br.com.arborize.dtos.request.DashboardReportFormat;
import br.com.arborize.dtos.response.DashboardGoalsResponse;
import br.com.arborize.dtos.response.DashboardOverviewResponse;
import br.com.arborize.services.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @Test
    void retornaIndicadoresConsolidados() throws Exception {
        when(dashboardService.getOverview()).thenReturn(
                new DashboardOverviewResponse(10, 8, 24, 50, 2, List.of(), List.of())
        );

        mockMvc.perform(get("/api/dashboard/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mudasDoadas").value(10))
                .andExpect(jsonPath("$.garrafasPetArrecadadas").value(50));
    }

    @Test
    void retornaMetasFiltradasPorZonaAnoETrimestre() throws Exception {
        when(dashboardService.getGoals("Zona Centro", 2026, 3)).thenReturn(
                new DashboardGoalsResponse(2026, 3, "Zona Centro", List.of("Zona Centro"), List.of(2026), List.of())
        );

        mockMvc.perform(get("/api/dashboard/goals")
                        .param("zone", "Zona Centro")
                        .param("year", "2026")
                        .param("quarter", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ano").value(2026))
                .andExpect(jsonPath("$.trimestre").value(3));
    }

    @Test
    void rejeitaPeriodoDeRelatorioInvalido() throws Exception {
        mockMvc.perform(post("/api/dashboard/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dataInicial": "2026-09-30",
                                  "dataFinal": "2026-01-01",
                                  "dados": ["MUDAS_DOADAS"],
                                  "formato": "PDF"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requisição inválida"));
    }

    @Test
    void devolveArquivoExportadoComoDownload() throws Exception {
        when(dashboardService.generateReport(any())).thenReturn(new byte[]{37, 80, 68, 70});

        mockMvc.perform(post("/api/dashboard/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dataInicial": "2026-01-01",
                                  "dataFinal": "2026-09-30",
                                  "dados": ["MUDAS_DOADAS"],
                                  "formato": "PDF"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("relatorio-impacto-ambiental_2026-01-01_2026-09-30.pdf")));
    }
}
