package ca.optimusAI.tms.report.service;

import ca.optimusAI.tms.validation.entity.ValidationSession;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class ReportFileGenerator {

    static final String[] HEADERS = {
            "Session ID", "License Plate", "Zone Number", "Zone Name",
            "Sub-Tenant", "Start Time", "End Time", "Duration (min)",
            "Status", "Extended Count"
    };

    public byte[] generateCsv(List<ValidationSession> rows,
                               Map<UUID, String> zoneNumbers,
                               Map<UUID, String> zoneNames,
                               Map<UUID, String> subTenantNames) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", HEADERS)).append("\n");
        for (ValidationSession s : rows) {
            sb.append(csv(s.getId().toString())).append(",");
            sb.append(csv(s.getLicensePlate())).append(",");
            sb.append(csv(zoneNumbers.getOrDefault(s.getZoneId(), ""))).append(",");
            sb.append(csv(zoneNames.getOrDefault(s.getZoneId(), ""))).append(",");
            sb.append(csv(s.getSubTenantId() != null
                    ? subTenantNames.getOrDefault(s.getSubTenantId(), "") : "")).append(",");
            sb.append(csv(s.getStartTime() != null ? s.getStartTime().toString() : "")).append(",");
            sb.append(csv(s.getEndTime() != null ? s.getEndTime().toString() : "")).append(",");
            sb.append(durationMinutes(s)).append(",");
            sb.append(csv(s.getStatus())).append(",");
            sb.append(s.getExtendedCount()).append("\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] generateExcel(List<ValidationSession> rows,
                                 Map<UUID, String> zoneNumbers,
                                 Map<UUID, String> zoneNames,
                                 Map<UUID, String> subTenantNames) throws IOException {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(100)) {
            SXSSFSheet sheet = wb.createSheet("Validations");

            Row hdr = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) hdr.createCell(i).setCellValue(HEADERS[i]);

            int r = 1;
            for (ValidationSession s : rows) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(s.getId().toString());
                row.createCell(1).setCellValue(s.getLicensePlate());
                row.createCell(2).setCellValue(zoneNumbers.getOrDefault(s.getZoneId(), ""));
                row.createCell(3).setCellValue(zoneNames.getOrDefault(s.getZoneId(), ""));
                row.createCell(4).setCellValue(s.getSubTenantId() != null
                        ? subTenantNames.getOrDefault(s.getSubTenantId(), "") : "");
                row.createCell(5).setCellValue(s.getStartTime() != null
                        ? s.getStartTime().toString() : "");
                row.createCell(6).setCellValue(s.getEndTime() != null
                        ? s.getEndTime().toString() : "");
                row.createCell(7).setCellValue(durationMinutes(s));
                row.createCell(8).setCellValue(s.getStatus());
                row.createCell(9).setCellValue(s.getExtendedCount());
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            wb.dispose();
            return out.toByteArray();
        }
    }

    public byte[] generatePdf(List<ValidationSession> rows,
                               Map<UUID, String> zoneNumbers,
                               Map<UUID, String> zoneNames,
                               Map<UUID, String> subTenantNames) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PdfDocument pdf = new PdfDocument(new PdfWriter(out));
             Document doc = new Document(pdf)) {

            doc.add(new Paragraph("Validation Sessions Report")
                    .setFontSize(16).setBold());
            doc.add(new Paragraph(" "));

            float[] colWidths = {2f, 1.5f, 1f, 1.5f, 1.5f, 2f, 2f, 1f, 1.2f, 1f};
            Table table = new Table(colWidths);
            table.setWidth(UnitValue.createPercentValue(100));

            for (String h : HEADERS) {
                table.addHeaderCell(new Cell().add(new Paragraph(h).setBold().setFontSize(8)));
            }

            for (ValidationSession s : rows) {
                table.addCell(cell(s.getId().toString()));
                table.addCell(cell(s.getLicensePlate()));
                table.addCell(cell(zoneNumbers.getOrDefault(s.getZoneId(), "")));
                table.addCell(cell(zoneNames.getOrDefault(s.getZoneId(), "")));
                table.addCell(cell(s.getSubTenantId() != null
                        ? subTenantNames.getOrDefault(s.getSubTenantId(), "") : ""));
                table.addCell(cell(s.getStartTime() != null ? s.getStartTime().toString() : ""));
                table.addCell(cell(s.getEndTime() != null ? s.getEndTime().toString() : ""));
                table.addCell(cell(String.valueOf(durationMinutes(s))));
                table.addCell(cell(s.getStatus()));
                table.addCell(cell(String.valueOf(s.getExtendedCount())));
            }

            doc.add(table);
        }
        return out.toByteArray();
    }

    private Cell cell(String value) {
        return new Cell().add(new Paragraph(value != null ? value : "").setFontSize(7));
    }

    private long durationMinutes(ValidationSession s) {
        if (s.getStartTime() == null || s.getEndTime() == null) return 0;
        return Duration.between(s.getStartTime(), s.getEndTime()).toMinutes();
    }

    private String csv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
