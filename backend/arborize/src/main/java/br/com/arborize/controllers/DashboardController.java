package br.com.arborize.controllers;

import br.com.arborize.dtos.request.DashboardReportRequest;
import br.com.arborize.dtos.response.DashboardGoalsResponse;
import br.com.arborize.dtos.response.DashboardOverviewResponse;
import br.com.arborize.services.DashboardService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/dashboard")
@Validated
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/overview")
    public ResponseEntity<DashboardOverviewResponse> getOverview() {
        return ResponseEntity.ok(dashboardService.getOverview());
    }

    @GetMapping("/goals")
    public ResponseEntity<DashboardGoalsResponse> getGoals(
            @RequestParam(required = false) String zone,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) @Min(1) @Max(4) Integer quarter
    ) {
        return ResponseEntity.ok(dashboardService.getGoals(zone, year, quarter));
    }

    @PostMapping(value = "/reports", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> generateReport(@Valid @RequestBody DashboardReportRequest request)
            throws IOException {
        byte[] report = dashboardService.generateReport(request);
        MediaType contentType = switch (request.formato()) {
            case PDF -> MediaType.APPLICATION_PDF;
            case CSV -> new MediaType("text", "csv", StandardCharsets.UTF_8);
            case XLSX -> MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            );
        };
        String extension = request.formato().name().toLowerCase();
        String filename = "relatorio-impacto-ambiental_" + request.dataInicial()
                + "_" + request.dataFinal() + "." + extension;

        return ResponseEntity.status(HttpStatus.OK)
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .body(report);
    }
}
