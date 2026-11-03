package br.com.arborize.services;

import br.com.arborize.dtos.request.DashboardReportDataset;
import br.com.arborize.dtos.request.DashboardReportFormat;
import br.com.arborize.dtos.request.DashboardReportRequest;
import br.com.arborize.dtos.response.DashboardGoalResponse;
import br.com.arborize.dtos.response.DashboardGoalsResponse;
import br.com.arborize.dtos.response.DashboardMonthlyMetricResponse;
import br.com.arborize.dtos.response.DashboardOverviewResponse;
import br.com.arborize.dtos.response.DashboardSpeciesStockResponse;
import br.com.arborize.entities.MetaZoneamento;
import br.com.arborize.repositories.EcoPontoTransactionRepository;
import br.com.arborize.repositories.EspecieRepository;
import br.com.arborize.repositories.MetaZoneamentoRepository;
import br.com.arborize.repositories.MudaTransactionRepository;
import br.com.arborize.repositories.PlantioRepository;
import br.com.arborize.repositories.SupressaoRepository;
import br.com.arborize.repositories.projections.MonthlyQuantityProjection;
import br.com.arborize.repositories.projections.ZoneQuantityProjection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private static final String NIM_INDIANO = "Azadirachta indica";

    private final MudaTransactionRepository mudaTransactionRepository;
    private final EcoPontoTransactionRepository ecoPontoTransactionRepository;
    private final EspecieRepository especieRepository;
    private final PlantioRepository plantioRepository;
    private final SupressaoRepository supressaoRepository;
    private final MetaZoneamentoRepository metaZoneamentoRepository;
    private final DashboardReportExporter reportExporter;

    public DashboardService(
            MudaTransactionRepository mudaTransactionRepository,
            EcoPontoTransactionRepository ecoPontoTransactionRepository,
            EspecieRepository especieRepository,
            PlantioRepository plantioRepository,
            SupressaoRepository supressaoRepository,
            MetaZoneamentoRepository metaZoneamentoRepository,
            DashboardReportExporter reportExporter
    ) {
        this.mudaTransactionRepository = mudaTransactionRepository;
        this.ecoPontoTransactionRepository = ecoPontoTransactionRepository;
        this.especieRepository = especieRepository;
        this.plantioRepository = plantioRepository;
        this.supressaoRepository = supressaoRepository;
        this.metaZoneamentoRepository = metaZoneamentoRepository;
        this.reportExporter = reportExporter;
    }

    @Transactional(readOnly = true)
    public DashboardOverviewResponse getOverview() {
        YearMonth currentMonth = YearMonth.now();
        YearMonth firstMonth = currentMonth.minusMonths(11);
        LocalDate start = firstMonth.atDay(1);
        LocalDate end = currentMonth.atEndOfMonth();

        Map<YearMonth, Long> donationsByMonth = toMonthlyMap(
                mudaTransactionRepository.findMonthlyConfirmedQuantities(start, end)
        );
        Map<YearMonth, Long> plantingsByMonth = toMonthlyMap(
                plantioRepository.findMonthlyConfirmedQuantities(start, end)
        );

        List<DashboardMonthlyMetricResponse> evolution = new ArrayList<>();
        for (YearMonth month = firstMonth; !month.isAfter(currentMonth); month = month.plusMonths(1)) {
            evolution.add(new DashboardMonthlyMetricResponse(
                    month.toString(),
                    donationsByMonth.getOrDefault(month, 0L),
                    plantingsByMonth.getOrDefault(month, 0L)
            ));
        }

        List<DashboardSpeciesStockResponse> stockBySpecies = especieRepository.findStockBySpecies().stream()
                .map(item -> new DashboardSpeciesStockResponse(item.getSpecies(), item.getQuantity()))
                .toList();

        return new DashboardOverviewResponse(
                mudaTransactionRepository.sumConfirmedQuantity(),
                plantioRepository.sumConfirmedQuantity(),
                especieRepository.sumMudasDisponiveis(),
                ecoPontoTransactionRepository.sumConfirmedBottles(),
                supressaoRepository.sumAuthorizedReplacementsForSpecies(NIM_INDIANO),
                evolution,
                stockBySpecies
        );
    }

    @Transactional(readOnly = true)
    public DashboardGoalsResponse getGoals(String zone, Integer year, Integer quarter) {
        LocalDate today = LocalDate.now();
        int selectedYear = year == null ? today.getYear() : year;
        int selectedQuarter = quarter == null ? (today.getMonthValue() - 1) / 3 + 1 : quarter;
        if (selectedQuarter < 1 || selectedQuarter > 4) {
            throw new IllegalArgumentException("O trimestre deve estar entre 1 e 4.");
        }

        String selectedZone = zone == null || zone.isBlank() || zone.equalsIgnoreCase("Todas")
                ? null
                : zone.trim();

        LocalDate periodStart = LocalDate.of(selectedYear, (selectedQuarter - 1) * 3 + 1, 1);
        LocalDate periodEnd = periodStart.plusMonths(3).minusDays(1);
        Map<ZoneKey, Long> plantingsByLocation = toLocationMap(
                plantioRepository.sumConfirmedQuantitiesByZoneAndBairro(periodStart, periodEnd)
        );
        Map<ZoneKey, Long> removalsByLocation = toLocationMap(
                supressaoRepository.sumAuthorizedQuantitiesByZoneAndBairro(periodStart, periodEnd)
        );

        List<DashboardGoalResponse> goals = metaZoneamentoRepository
                .findForDashboard(selectedZone, selectedYear, selectedQuarter)
                .stream()
                .map(meta -> toGoalResponse(meta, plantingsByLocation, removalsByLocation))
                .toList();

        return new DashboardGoalsResponse(
                selectedYear,
                selectedQuarter,
                selectedZone == null ? "Todas" : selectedZone,
                metaZoneamentoRepository.findDistinctZones(),
                metaZoneamentoRepository.findDistinctYears(),
                goals
        );
    }

    @Transactional(readOnly = true)
    public byte[] generateReport(DashboardReportRequest request) throws IOException {
        if (request.dados().contains(DashboardReportDataset.LAUDOS_EMITIDOS)) {
            throw new UnsupportedOperationException(
                    "A exportação de laudos ficará disponível quando existir uma fonte de dados para esse módulo."
            );
        }

        List<DashboardReportExporter.ReportRow> rows = new ArrayList<>();
        if (request.dados().contains(DashboardReportDataset.MUDAS_DOADAS)) {
            rows.add(new DashboardReportExporter.ReportRow(
                    "Mudas doadas",
                    mudaTransactionRepository.sumConfirmedQuantityBetween(request.dataInicial(), request.dataFinal())
            ));
        }
        if (request.dados().contains(DashboardReportDataset.BALANCO_RECICLAVEIS)) {
            rows.add(new DashboardReportExporter.ReportRow(
                    "Garrafas PET arrecadadas",
                    ecoPontoTransactionRepository.sumConfirmedBottlesBetween(
                            request.dataInicial(),
                            request.dataFinal()
                    )
            ));
        }
        return reportExporter.export(request, rows);
    }

    private DashboardGoalResponse toGoalResponse(
            MetaZoneamento meta,
            Map<ZoneKey, Long> plantingsByLocation,
            Map<ZoneKey, Long> removalsByLocation
    ) {
        ZoneKey key = new ZoneKey(meta.getZona(), meta.getBairro());
        long realized = plantingsByLocation.getOrDefault(key, 0L);
        long authorizedRemovals = removalsByLocation.getOrDefault(key, 0L);
        long target = meta.getPlantingTarget();
        double deficit = target == 0
                ? 0
                : Math.max(0, ((target - realized) * 100.0) / target);

        String priority = target == 0
                ? "Sem meta"
                : deficit > 50
                ? "Alta"
                : deficit > 20
                ? "Média"
                : "Baixa";

        return new DashboardGoalResponse(
                meta.getBairro(),
                meta.getZona(),
                target,
                realized,
                realized - authorizedRemovals,
                deficit,
                priority
        );
    }

    private Map<YearMonth, Long> toMonthlyMap(List<MonthlyQuantityProjection> results) {
        Map<YearMonth, Long> quantities = new HashMap<>();
        for (MonthlyQuantityProjection result : results) {
            quantities.put(
                    YearMonth.of(result.getYear(), result.getMonth()),
                    result.getQuantity()
            );
        }
        return quantities;
    }

    private Map<ZoneKey, Long> toLocationMap(List<ZoneQuantityProjection> results) {
        Map<ZoneKey, Long> quantities = new HashMap<>();
        for (ZoneQuantityProjection result : results) {
            quantities.put(new ZoneKey(result.getZona(), result.getBairro()), result.getQuantity());
        }
        return quantities;
    }

    private record ZoneKey(String zone, String neighborhood) {
    }
}
