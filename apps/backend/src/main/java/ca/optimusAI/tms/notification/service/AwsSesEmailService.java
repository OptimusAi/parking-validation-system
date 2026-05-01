package ca.optimusAI.tms.notification.service;

import ca.optimusAI.tms.tenant.entity.TenantBranding;
import ca.optimusAI.tms.tenant.service.TenantService;
import ca.optimusAI.tms.validation.event.ValidationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Sends HTML email notifications via AWS SES.
 * Only sends if {@code endUserEmail} is present on the event.
 * Fetches tenant branding (direct Java call) to embed logo in the email header.
 * Gracefully skips if SES is not reachable (local/test environments).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AwsSesEmailService {

    private final SesClient sesClient;
    private final TenantService tenantService;

    @Value("${aws.ses.from-email}")
    private String fromEmail;

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a 'UTC'").withZone(ZoneOffset.UTC);

    /**
     * Sends an HTML email based on the event type.
     *
     * @param event the validation event
     */
    public void send(ValidationEvent event) {
        if (!StringUtils.hasText(event.endUserEmail())) {
            log.debug("Email skipped — no email address for session {}", event.sessionId());
            return;
        }

        TenantBranding branding = loadBranding(event);
        String subject = buildSubject(event);
        String html = buildHtmlBody(event, branding);

        try {
            SendEmailRequest req = SendEmailRequest.builder()
                    .source(fromEmail)
                    .destination(Destination.builder()
                            .toAddresses(event.endUserEmail())
                            .build())
                    .message(software.amazon.awssdk.services.ses.model.Message.builder()
                            .subject(Content.builder()
                                    .data(subject)
                                    .charset(StandardCharsets.UTF_8.name())
                                    .build())
                            .body(Body.builder()
                                    .html(Content.builder()
                                            .data(html)
                                            .charset(StandardCharsets.UTF_8.name())
                                            .build())
                                    .build())
                            .build())
                    .build();

            SendEmailResponse response = sesClient.sendEmail(req);
            log.info("Email sent messageId={} to={} eventType={}",
                    response.messageId(), event.endUserEmail(), event.eventType());

        } catch (Exception e) {
            log.error("Email send failed for session {} to {}: {}",
                    event.sessionId(), event.endUserEmail(), e.getMessage());
            throw e;
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private TenantBranding loadBranding(ValidationEvent event) {
        try {
            return tenantService.getBranding(event.tenantId());
        } catch (Exception e) {
            log.warn("Could not load branding for tenant {}: {}", event.tenantId(), e.getMessage());
            return TenantBranding.defaults();
        }
    }

    private String buildSubject(ValidationEvent event) {
        return switch (event.eventType()) {
            case "SESSION_CREATED"      -> "Parking Validation Confirmed — " + event.licensePlate();
            case "SESSION_EXTENDED"     -> "Parking Extended — " + event.licensePlate();
            case "SESSION_EXPIRING_SOON"-> "Parking Expiring Soon — " + event.licensePlate();
            case "SESSION_CANCELLED"    -> "Parking Session Cancelled — " + event.licensePlate();
            default                     -> "Parking Update — " + event.licensePlate();
        };
    }

    private String buildHtmlBody(ValidationEvent event, TenantBranding branding) {
        String primaryColor = branding != null && StringUtils.hasText(branding.primaryColor())
                ? branding.primaryColor() : "#1B4F8A";

        String logoHtml = "";
        if (branding != null && StringUtils.hasText(branding.logoUrl())) {
            logoHtml = "<img src=\"" + escapeHtml(branding.logoUrl()) +
                       "\" alt=\"Logo\" style=\"max-height:60px;margin-bottom:16px;\"/><br/>";
        }

        String headingText = buildSubject(event);
        String bodyText = buildEmailBodyText(event);

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
                <body style="margin:0;padding:0;font-family:Arial,sans-serif;background:#f4f4f4;">
                  <table width="100%%" cellpadding="0" cellspacing="0">
                    <tr>
                      <td align="center" style="padding:24px 0;">
                        <table width="600" cellpadding="0" cellspacing="0"
                               style="background:#ffffff;border-radius:8px;overflow:hidden;">
                          <tr>
                            <td style="background:%s;padding:24px;text-align:center;">
                              %s
                              <h1 style="color:#ffffff;margin:0;font-size:22px;">%s</h1>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:32px;">
                              %s
                            </td>
                          </tr>
                          <tr>
                            <td style="background:#f4f4f4;padding:16px;text-align:center;
                                        font-size:12px;color:#888888;">
                              This is an automated message from the Calgary Parking Authority.
                              Session ID: %s
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(primaryColor, logoHtml, escapeHtml(headingText),
                              bodyText, event.sessionId());
    }

    private String buildEmailBodyText(ValidationEvent event) {
        String endTimeStr = event.endTime() != null ? TIME_FMT.format(event.endTime()) : "N/A";
        return switch (event.eventType()) {
            case "SESSION_CREATED" -> """
                    <p>Your parking has been successfully validated.</p>
                    <table style="border-collapse:collapse;width:100%%;">
                      <tr><td style="padding:8px;font-weight:bold;color:#555;">License Plate</td>
                          <td style="padding:8px;">%s</td></tr>
                      <tr><td style="padding:8px;font-weight:bold;color:#555;">Valid Until</td>
                          <td style="padding:8px;">%s</td></tr>
                    </table>
                    """.formatted(event.licensePlate(), endTimeStr);
            case "SESSION_EXTENDED" -> """
                    <p>Your parking session has been extended.</p>
                    <table style="border-collapse:collapse;width:100%%;">
                      <tr><td style="padding:8px;font-weight:bold;color:#555;">License Plate</td>
                          <td style="padding:8px;">%s</td></tr>
                      <tr><td style="padding:8px;font-weight:bold;color:#555;">New Expiry</td>
                          <td style="padding:8px;">%s</td></tr>
                    </table>
                    """.formatted(event.licensePlate(), endTimeStr);
            case "SESSION_EXPIRING_SOON" -> """
                    <p style="color:#cc6600;font-weight:bold;">
                      Your parking is expiring soon. Please extend your session if needed.</p>
                    <table style="border-collapse:collapse;width:100%%;">
                      <tr><td style="padding:8px;font-weight:bold;color:#555;">License Plate</td>
                          <td style="padding:8px;">%s</td></tr>
                      <tr><td style="padding:8px;font-weight:bold;color:#555;">Expires At</td>
                          <td style="padding:8px;">%s</td></tr>
                    </table>
                    """.formatted(event.licensePlate(), endTimeStr);
            case "SESSION_CANCELLED" -> """
                    <p>Your parking session has been cancelled.</p>
                    <table style="border-collapse:collapse;width:100%%;">
                      <tr><td style="padding:8px;font-weight:bold;color:#555;">License Plate</td>
                          <td style="padding:8px;">%s</td></tr>
                    </table>
                    """.formatted(event.licensePlate());
            default -> "<p>There has been an update to your parking session for plate "
                    + escapeHtml(event.licensePlate()) + ".</p>";
        };
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
