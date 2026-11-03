package br.com.arborize.services;

import br.com.arborize.dtos.request.DashboardReportFormat;
import br.com.arborize.dtos.request.DashboardReportRequest;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardReportExporterTest {

    private final DashboardReportExporter exporter = new DashboardReportExporter();

    @Test
    void exportaPdfLegivel() throws Exception {
        byte[] report = exporter.export(request(DashboardReportFormat.PDF), rows());

        assertTrue(new String(report, 0, 4).startsWith("%PDF"));
    }

    @Test
    void exportaCsvEmUtf8ComCabecalho() throws Exception {
        byte[] report = exporter.export(request(DashboardReportFormat.CSV), rows());
        String csv = new String(report, java.nio.charset.StandardCharsets.UTF_8);

        assertTrue(csv.startsWith("\uFEFFRelatório de Impacto Ambiental;AMMA"));
        assertTrue(csv.contains("Período;01/01/2026 a 30/09/2026"));
        assertTrue(csv.contains("Indicador;Quantidade"));
        assertTrue(csv.contains("\"Mudas doadas\";12"));
    }

    @Test
    void exportaXlsxComPlanilhaValida() throws Exception {
        byte[] report = exporter.export(request(DashboardReportFormat.XLSX), rows());
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(report))) {
            boolean containsSheet = false;
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if ("xl/worksheets/sheet1.xml".equals(entry.getName())) {
                    containsSheet = true;
                }
            }
            assertTrue(containsSheet);
        }
    }

    private DashboardReportRequest request(DashboardReportFormat format) {
        return new DashboardReportRequest(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 9, 30),
                Set.of(),
                format
        );
    }

    private List<DashboardReportExporter.ReportRow> rows() {
        return List.of(new DashboardReportExporter.ReportRow("Mudas doadas", 12));
    }
}
