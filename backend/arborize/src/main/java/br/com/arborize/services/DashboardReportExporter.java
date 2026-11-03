package br.com.arborize.services;

import br.com.arborize.dtos.request.DashboardReportFormat;
import br.com.arborize.dtos.request.DashboardReportRequest;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
public class DashboardReportExporter {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] export(DashboardReportRequest request, List<ReportRow> rows) throws IOException {
        return switch (request.formato()) {
            case PDF -> createPdf(request, rows);
            case CSV -> createCsv(request, rows);
            case XLSX -> createXlsx(request, rows);
        };
    }

    private byte[] createPdf(DashboardReportRequest request, List<ReportRow> rows) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
                content.newLineAtOffset(50, 730);
                content.showText("Relatorio de Impacto Ambiental - AMMA");
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                content.newLineAtOffset(0, -24);
                content.showText("Periodo: " + DATE_FORMAT.format(request.dataInicial())
                        + " a " + DATE_FORMAT.format(request.dataFinal()));
                content.newLineAtOffset(0, -18);
                content.showText("Gerado em: " + DATE_FORMAT.format(LocalDateTime.now().toLocalDate()));
                content.newLineAtOffset(0, -34);
                for (ReportRow row : rows) {
                    content.showText(row.indicador() + ": " + row.quantidade());
                    content.newLineAtOffset(0, -20);
                }
                content.endText();
            }

            document.save(output);
            return output.toByteArray();
        }
    }

    private byte[] createCsv(DashboardReportRequest request, List<ReportRow> rows) {
        StringBuilder csv = new StringBuilder("\uFEFFRelatório de Impacto Ambiental;AMMA\r\n");
        csv.append("Período;")
                .append(DATE_FORMAT.format(request.dataInicial()))
                .append(" a ")
                .append(DATE_FORMAT.format(request.dataFinal()))
                .append("\r\nIndicador;Quantidade\r\n");
        for (ReportRow row : rows) {
            csv.append(escapeCsv(row.indicador()))
                    .append(';')
                    .append(row.quantidade())
                    .append("\r\n");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] createXlsx(DashboardReportRequest request, List<ReportRow> rows) throws IOException {
        StringBuilder sheet = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>
                """);
        appendInlineStringRow(sheet, 1, "Relatório de Impacto Ambiental", "AMMA");
        appendInlineStringRow(
                sheet,
                2,
                "Período",
                DATE_FORMAT.format(request.dataInicial()) + " a " + DATE_FORMAT.format(request.dataFinal())
        );
        appendInlineStringRow(sheet, 3, "Indicador", "Quantidade");
        int rowNumber = 4;
        for (ReportRow row : rows) {
            appendInlineStringRow(sheet, rowNumber++, row.indicador(), Long.toString(row.quantidade()));
        }
        sheet.append("</sheetData></worksheet>");

        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            addZipEntry(zip, "[Content_Types].xml", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                    <Default Extension="xml" ContentType="application/xml"/>
                    <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                    <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                    </Types>
                    """);
            addZipEntry(zip, "_rels/.rels", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                    </Relationships>
                    """);
            addZipEntry(zip, "xl/workbook.xml", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                    <sheets><sheet name="Impacto Ambiental" sheetId="1" r:id="rId1"/></sheets>
                    </workbook>
                    """);
            addZipEntry(zip, "xl/_rels/workbook.xml.rels", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                    </Relationships>
                    """);
            addZipEntry(zip, "xl/worksheets/sheet1.xml", sheet.toString());
            zip.finish();
            return output.toByteArray();
        }
    }

    private void appendInlineStringRow(StringBuilder sheet, int row, String first, String second) {
        sheet.append("<row r=\"").append(row).append("\">");
        appendInlineStringCell(sheet, "A" + row, first);
        appendInlineStringCell(sheet, "B" + row, second);
        sheet.append("</row>");
    }

    private void appendInlineStringCell(StringBuilder sheet, String reference, String value) {
        sheet.append("<c r=\"").append(reference).append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                .append(escapeXml(value))
                .append("</t></is></c>");
    }

    private void addZipEntry(ZipOutputStream zip, String name, String contents) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(contents.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private String escapeCsv(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private String escapeXml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    public record ReportRow(String indicador, long quantidade) {
    }
}
