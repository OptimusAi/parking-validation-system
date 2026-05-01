package ca.optimusAI.tms.notification.listener;

import ca.optimusAI.tms.notification.entity.Notification;
import ca.optimusAI.tms.notification.repository.NotificationRepository;
import ca.optimusAI.tms.notification.service.AwsSesEmailService;
import ca.optimusAI.tms.notification.service.TwilioSmsService;
import ca.optimusAI.tms.notification.service.WebSocketNotificationService;
import ca.optimusAI.tms.validation.event.ValidationEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Consumes events from the {@code validation.notifications} queue.
 *
 * <p>For every event it:
 * <ol>
 *   <li>Routes to Twilio SMS and/or AWS SES email based on event type</li>
 *   <li>Pushes to WebSocket topic for real-time dashboard updates</li>
 *   <li>Saves a {@link Notification} row (SENT or FAILED) to the database</li>
 * </ol>
 *
 * <p>Each channel is handled independently — a failure in SMS does NOT prevent
 * the email attempt or the WebSocket push.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ValidationEventListener {

    private final TwilioSmsService smsService;
    private final AwsSesEmailService emailService;
    private final WebSocketNotificationService webSocketService;
    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "validation.notifications")
    public void onValidationEvent(ValidationEvent event) {
        log.info("Received validation event: type={} sessionId={}", event.eventType(), event.sessionId());

        // Always push to WebSocket first (lightweight, no external dependency)
        webSocketService.push(event);

        // Route SMS and email based on event type
        switch (event.eventType()) {
            case "SESSION_CREATED",
                 "SESSION_EXTENDED",
                 "SESSION_EXPIRING_SOON",
                 "SESSION_CANCELLED" -> {
                handleSms(event);
                handleEmail(event);
            }
            default -> log.warn("Unknown event type: {}", event.eventType());
        }
    }

    // ── Private handlers ──────────────────────────────────────────────────────

    private void handleSms(ValidationEvent event) {
        if (event.endUserPhone() == null || event.endUserPhone().isBlank()) return;

        String status = "SENT";
        String errorMessage = null;

        try {
            smsService.send(event);
        } catch (Exception e) {
            status = "FAILED";
            errorMessage = e.getMessage();
            log.error("SMS failed for session {}: {}", event.sessionId(), e.getMessage());
        }

        saveNotification(event, "SMS", event.endUserPhone(), status, errorMessage);
    }

    private void handleEmail(ValidationEvent event) {
        if (event.endUserEmail() == null || event.endUserEmail().isBlank()) return;

        String status = "SENT";
        String errorMessage = null;

        try {
            emailService.send(event);
        } catch (Exception e) {
            status = "FAILED";
            errorMessage = e.getMessage();
            log.error("Email failed for session {}: {}", event.sessionId(), e.getMessage());
        }

        saveNotification(event, "EMAIL", event.endUserEmail(), status, errorMessage);
    }

    private void saveNotification(ValidationEvent event, String channel,
                                   String recipient, String status, String errorMessage) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            Notification notification = Notification.builder()
                    .tenantId(event.tenantId())
                    .clientId(event.clientId() != null ? event.clientId() : event.tenantId())
                    .sessionId(event.sessionId())
                    .channel(channel)
                    .recipient(recipient)
                    .eventType(event.eventType())
                    .status(status)
                    .payload(payload)
                    .errorMessage(errorMessage)
                    .sentAt(Instant.now())
                    .build();
            notificationRepository.save(notification);
        } catch (Exception e) {
            log.error("Failed to save notification record for session {}: {}",
                    event.sessionId(), e.getMessage());
        }
    }
}
