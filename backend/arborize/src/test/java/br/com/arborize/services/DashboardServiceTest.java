package br.com.arborize.services;

import br.com.arborize.dtos.request.DashboardReportDataset;
import br.com.arborize.dtos.request.DashboardReportFormat;
import br.com.arborize.dtos.request.DashboardReportRequest;
import br.com.arborize.entities.MetaZoneamento;
import br.com.arborize.repositories.EcoPontoTransactionRepository;
import br.com.arborize.repositories.EspecieRepository;
import br.com.arborize.repositories.MetaZoneamentoRepository;
import br.com.arborize.repositories.MudaTransactionRepository;
import br.com.arborize.repositories.PlantioRepository;
import br.com.arborize.repositories.SupressaoRepository;
import br.com.arborize.repositories.projections.MonthlyQuantityProjection;
import br.com.arborize.repositories.projections.SpeciesStockProjection;
import br.com.arborize.repositories.projections.ZoneQuantityProjection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private MudaTransactionRepository mudaTransactionRepository;

    @Mock
    private EcoPontoTransactionRepository ecoPontoTransactionRepository;

    @Mock
    private EspecieRepository especieRepository;

    @Mock
    private PlantioRepository plantioRepository;

    @Mock
    private SupressaoRepository supressaoRepository;

    @Mock
    private MetaZoneamentoRepository metaZoneamentoRepository;

    @Mock
    private DashboardReportExporter reportExporter;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void consolidaIndicadoresEPreencheDozeMesesDaEvolucao() {
        when(mudaTransactionRepository.sumConfirmedQuantity()).thenReturn(12L);
        when(plantioRepository.sumConfirmedQuantity()).thenReturn(8L);
        when(especieRepository.sumMudasDisponiveis()).thenReturn(50L);
        when(ecoPontoTransactionRepository.sumConfirmedBottles()).thenReturn(100L);
        when(supressaoRepository.sumAuthorizedReplacementsForSpecies("Azadirachta indica")).thenReturn(3L);
        when(mudaTransactionRepository.findMonthlyConfirmedQuantities(any(), any()))
                .thenReturn(List.of(new MonthTotal(java.time.YearMonth.now().getYear(),
                        java.time.YearMonth.now().getMonthValue(), 4L)));
        when(plantioRepository.findMonthlyConfirmedQuantities(any(), any()))
                .thenReturn(List.of(new MonthTotal(java.time.YearMonth.now().getYear(),
                        java.time.YearMonth.now().getMonthValue(), 5L)));
        when(especieRepository.findStockBySpecies()).thenReturn(List.of(new SpeciesTotal("Ipê Teste", 50L)));

        var result = dashboardService.getOverview();

        assertEquals(12, result.mudasDoadas());
        assertEquals(8, result.mudasPlantadas());
        assertEquals(50, result.estoqueAtual());
        assertEquals(100, result.garrafasPetArrecadadas());
        assertEquals(3, result.substituicoesNim());
        assertEquals(12, result.evolucao().size());
        assertEquals(4, result.evolucao().get(11).mudasDoadas());
        assertEquals(5, result.evolucao().get(11).mudasPlantadas());
        assertEquals("Ipê Teste", result.estoquePorEspecie().get(0).especie());
    }

    @Test
    void calculaSaldoLiquidoEPrioridadeConformeDeficitPercentual() {
        List<MetaZoneamento> targets = List.of(
                new MetaZoneamento(1L, "Zona Centro", "Centro", 2026, 1, 100),
                new MetaZoneamento(2L, "Zona Centro", "Planalto", 2026, 1, 100),
                new MetaZoneamento(3L, "Zona Centro", "Campo Novo", 2026, 1, 100)
        );
        when(metaZoneamentoRepository.findForDashboard("Zona Centro", 2026, 1)).thenReturn(targets);
        when(metaZoneamentoRepository.findDistinctZones()).thenReturn(List.of("Zona Centro"));
        when(metaZoneamentoRepository.findDistinctYears()).thenReturn(List.of(2026));
        when(plantioRepository.sumConfirmedQuantitiesByZoneAndBairro(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(List.of(
                new LocationTotal("Zona Centro", "Centro", 40L),
                new LocationTotal("Zona Centro", "Planalto", 70L),
                new LocationTotal("Zona Centro", "Campo Novo", 80L)
        ));
        when(supressaoRepository.sumAuthorizedQuantitiesByZoneAndBairro(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(List.of(new LocationTotal("Zona Centro", "Centro", 8L)));

        var result = dashboardService.getGoals("Zona Centro", 2026, 1);

        assertEquals(32, result.metas().get(0).saldoLiquido());
        assertEquals(60, result.metas().get(0).deficitPercentual());
        assertEquals("Alta", result.metas().get(0).prioridade());
        assertEquals(70, result.metas().get(1).saldoLiquido());
        assertEquals("Média", result.metas().get(1).prioridade());
        assertEquals(80, result.metas().get(2).saldoLiquido());
        assertEquals("Baixa", result.metas().get(2).prioridade());
    }

    @Test
    void rejeitaRelatorioComFonteDeLaudosIndisponivel() {
        var request = new DashboardReportRequest(
                java.time.LocalDate.of(2026, 1, 1),
                java.time.LocalDate.of(2026, 1, 31),
                Set.of(DashboardReportDataset.LAUDOS_EMITIDOS),
                DashboardReportFormat.PDF
        );

        assertThrows(UnsupportedOperationException.class, () -> dashboardService.generateReport(request));
    }

    @Test
    void geraRelatorioComIntervaloSolicitado() throws Exception {
        var start = java.time.LocalDate.of(2026, 1, 1);
        var end = java.time.LocalDate.of(2026, 9, 30);
        var request = new DashboardReportRequest(
                start,
                end,
                Set.of(DashboardReportDataset.MUDAS_DOADAS, DashboardReportDataset.BALANCO_RECICLAVEIS),
                DashboardReportFormat.CSV
        );
        when(mudaTransactionRepository.sumConfirmedQuantityBetween(start, end)).thenReturn(12L);
        when(ecoPontoTransactionRepository.sumConfirmedBottlesBetween(start, end)).thenReturn(20L);
        when(reportExporter.export(eq(request), any())).thenReturn(new byte[]{1, 2, 3});

        assertArrayEquals(new byte[]{1, 2, 3}, dashboardService.generateReport(request));
        verify(mudaTransactionRepository).sumConfirmedQuantityBetween(start, end);
        verify(ecoPontoTransactionRepository).sumConfirmedBottlesBetween(start, end);
    }

    private record MonthTotal(Integer year, Integer month, Long quantity) implements MonthlyQuantityProjection {
        @Override
        public Integer getYear() {
            return year;
        }

        @Override
        public Integer getMonth() {
            return month;
        }

        @Override
        public Long getQuantity() {
            return quantity;
        }
    }

    private record SpeciesTotal(String species, Long quantity) implements SpeciesStockProjection {
        @Override
        public String getSpecies() {
            return species;
        }

        @Override
        public Long getQuantity() {
            return quantity;
        }
    }

    private record LocationTotal(String zona, String bairro, Long quantity) implements ZoneQuantityProjection {
        @Override
        public String getZona() {
            return zona;
        }

        @Override
        public String getBairro() {
            return bairro;
        }

        @Override
        public Long getQuantity() {
            return quantity;
        }
    }
}
