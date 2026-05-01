package ca.optimusAI.pv.qrlink.pdf;

import ca.optimusAI.pv.qrlink.entity.ValidationLink;
import ca.optimusAI.pv.tenant.entity.TenantBranding;
import ca.optimusAI.pv.tenant.service.TenantService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Generates a branded QR PDF for a ValidationLink.
 * Fetches tenant branding via TenantService (direct Java call — no HTTP).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QrPdfGenerator {

    private final TenantService tenantService;

    @Value("${qr.base-url}")
    private String baseUrl;

    private static final DateTimeFormatter EXPIRY_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    /**
     * Generates a PDF byte array containing the QR code and zone info.
     *
     * @param link     the validation link to encode
     * @return PDF bytes
     */
    public byte[] generate(ValidationLink link) {
        TenantBranding branding = loadBranding(link);
        String publicUrl = baseUrl + "/validate/" + link.getToken();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (PdfDocument pdf = new PdfDocument(new PdfWriter(out));
                 Document doc = new Document(pdf, PageSize.A4)) {

                // ── Tenant logo ───────────────────────────────────────────────
                if (branding != null && StringUtils.hasText(branding.logoUrl())) {
                    try {
                        Image logo = new Image(
                                ImageDataFactory.create(new URL(branding.logoUrl())));
                        logo.setWidth(120)
                                .setHorizontalAlignment(HorizontalAlignment.CENTER);
                        doc.add(logo);
                        doc.add(new Paragraph("\n"));
                    } catch (Exception e) {
                        log.warn("Could not load branding logo from {}: {}",
                                branding.logoUrl(), e.getMessage());
                    }
                }

                // ── Primary-colour header bar ─────────────────────────────────
                String primaryHex = branding != null && StringUtils.hasText(branding.primaryColor())
                        ? branding.primaryColor() : "#1B4F8A";
                DeviceRgb headerColor = hexToRgb(primaryHex);

                Paragraph header = new Paragraph("Parking Validation")
                        .setFontSize(20)
                        .setBold()
                        .setFontColor(headerColor)
                        .setTextAlignment(TextAlignment.CENTER);
                doc.add(header);
                doc.add(new Paragraph("\n").setFontSize(6));

                // ── QR code PNG ───────────────────────────────────────────────
                BitMatrix matrix = new QRCodeWriter()
                        .encode(publicUrl, BarcodeFormat.QR_CODE, 400, 400);
                BufferedImage qrImg = MatrixToImageWriter.toBufferedImage(matrix);
                ByteArrayOutputStream qrBytes = new ByteArrayOutputStream();
                ImageIO.write(qrImg, "PNG", qrBytes);

                Image qr = new Image(ImageDataFactory.create(qrBytes.toByteArray()));
                qr.setWidth(260).setHorizontalAlignment(HorizontalAlignment.CENTER);
                doc.add(qr);
                doc.add(new Paragraph("\n").setFontSize(4));

                // ── Label ─────────────────────────────────────────────────────
                if (StringUtils.hasText(link.getLabel())) {
                    doc.add(new Paragraph(link.getLabel())
                            .setFontSize(16)
                            .setBold()
                            .setTextAlignment(TextAlignment.CENTER));
                }

                // ── Duration ──────────────────────────────────────────────────
                doc.add(new Paragraph("Duration: " + link.getDefaultDurationMinutes() + " minutes")
                        .setFontSize(12)
                        .setTextAlignment(TextAlignment.CENTER));

                // ── Expiry ────────────────────────────────────────────────────
                String expiryLine = link.getExpiresAt() != null
                        ? "Valid until: " + EXPIRY_FMT.format(link.getExpiresAt())
                        : "No expiry date";
                doc.add(new Paragraph(expiryLine)
                        .setFontSize(10)
                        .setTextAlignment(TextAlignment.CENTER));

                // ── URL (small footer) ────────────────────────────────────────
                doc.add(new Paragraph("\n").setFontSize(4));
                doc.add(new Paragraph(publicUrl)
                        .setFontSize(8)
                        .setFontColor(new DeviceRgb(120, 120, 120))
                        .setTextAlignment(TextAlignment.CENTER));
            }
            return out.toByteArray();

        } catch (Exception e) {
            log.error("Failed to generate QR PDF for link {}: {}", link.getId(), e.getMessage(), e);
            throw new IllegalStateException("PDF generation failed: " + e.getMessage(), e);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private TenantBranding loadBranding(ValidationLink link) {
        try {
            return tenantService.getBranding(link.getTenantId());
        } catch (Exception e) {
            log.warn("Could not load branding for tenant {}: {}", link.getTenantId(), e.getMessage());
            return TenantBranding.defaults();
        }
    }

    private DeviceRgb hexToRgb(String hex) {
        try {
            String h = hex.startsWith("#") ? hex.substring(1) : hex;
            int r = Integer.parseInt(h.substring(0, 2), 16);
            int g = Integer.parseInt(h.substring(2, 4), 16);
            int b = Integer.parseInt(h.substring(4, 6), 16);
            return new DeviceRgb(r, g, b);
        } catch (Exception e) {
            return new DeviceRgb(27, 79, 138); // #1B4F8A default
        }
    }
}
