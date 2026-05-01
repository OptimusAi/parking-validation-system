package ca.optimusAI.pv.notification.service;

import ca.optimusAI.pv.validation.event.ValidationEvent;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Sends SMS notifications via Twilio.
 * Only sends if {@code endUserPhone} is present on the event.
 * Gracefully skips if Twilio credentials are not configured (local/test environments).
 */
@Slf4j
@Service
public class TwilioSmsService {

    @Value("${twilio.account-sid:}")
    private String accountSid;

    @Value("${twilio.auth-token:}")
    private String authToken;

    @Value("${twilio.from-number:}")
    private String fromNumber;

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("h:mm a 'UTC'").withZone(ZoneOffset.UTC);

    private boolean configured = false;

    @PostConstruct
    void init() {
        if (StringUtils.hasText(accountSid) && StringUtils.hasText(authToken)
                && StringUtils.hasText(fromNumber)) {
            Twilio.init(accountSid, authToken);
            configured = true;
            log.info("TwilioSmsService initialized with from={}", fromNumber);
        } else {
            log.warn("TwilioSmsService: credentials not configured — SMS sending disabled");
        }
    }

    /**
     * Sends an SMS based on the event type.
     *
     * @param event the validation event
     * @return the Twilio message SID, or null if not sent
     */
    public String send(ValidationEvent event) {
        if (!configured) {
            log.debug("SMS skipped — Twilio not configured");
            return null;
        }
        if (!StringUtils.hasText(event.endUserPhone())) {
            log.debug("SMS skipped — no phone number for session {}", event.sessionId());
            return null;
        }

        String body = buildMessageBody(event);
        try {
            Message message = Message.creator(
                    new PhoneNumber(event.endUserPhone()),
                    new PhoneNumber(fromNumber),
                    body
            ).create();
            log.info("SMS sent sid={} to={} eventType={}",
                    message.getSid(), event.endUserPhone(), event.eventType());
            return message.getSid();
        } catch (Exception e) {
            log.error("SMS send failed for session {} to {}: {}",
                    event.sessionId(), event.endUserPhone(), e.getMessage());
            throw e;
        }
    }

    private String buildMessageBody(ValidationEvent event) {
        return switch (event.eventType()) {
            case "SESSION_CREATED" -> String.format(
                    "Parking validated! Plate %s valid until %s. Session ID: %s",
                    event.licensePlate(),
                    event.endTime() != null ? TIME_FMT.format(event.endTime()) : "N/A",
                    event.sessionId());
            case "SESSION_EXTENDED" -> String.format(
                    "Parking extended! Plate %s now valid until %s.",
                    event.licensePlate(),
                    event.endTime() != null ? TIME_FMT.format(event.endTime()) : "N/A");
            case "SESSION_EXPIRING_SOON" -> String.format(
                    "Reminder: Parking for plate %s expires at %s. Please extend if needed.",
                    event.licensePlate(),
                    event.endTime() != null ? TIME_FMT.format(event.endTime()) : "N/A");
            case "SESSION_CANCELLED" -> String.format(
                    "Parking session for plate %s has been cancelled.",
                    event.licensePlate());
            default -> String.format(
                    "Parking update for plate %s. Session ID: %s",
                    event.licensePlate(), event.sessionId());
        };
    }
}
