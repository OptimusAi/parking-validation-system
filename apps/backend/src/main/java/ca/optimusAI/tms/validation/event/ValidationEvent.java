package ca.optimusAI.tms.validation.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base record for all validation domain events published to RabbitMQ.
 * The {@code eventType} field is used by downstream listeners to route handling.
 */
public record ValidationEvent(
        String eventType,
        UUID sessionId,
        UUID tenantId,
        UUID clientId,
        UUID subTenantId,
        UUID zoneId,
        String licensePlate,
        String status,
        Instant startTime,
        Instant endTime,
        String endUserEmail,
        String endUserPhone,
        Instant occurredAt
) {
    /** SESSION_CREATED */
    public static ValidationEvent created(ca.optimusAI.tms.validation.entity.ValidationSession s) {
        return new ValidationEvent("SESSION_CREATED", s.getId(), s.getTenantId(),
                s.getClientId(), s.getSubTenantId(), s.getZoneId(), s.getLicensePlate(),
                s.getStatus(), s.getStartTime(), s.getEndTime(),
                s.getEndUserEmail(), s.getEndUserPhone(), Instant.now());
    }

    /** SESSION_EXTENDED */
    public static ValidationEvent extended(ca.optimusAI.tms.validation.entity.ValidationSession s) {
        return new ValidationEvent("SESSION_EXTENDED", s.getId(), s.getTenantId(),
                s.getClientId(), s.getSubTenantId(), s.getZoneId(), s.getLicensePlate(),
                s.getStatus(), s.getStartTime(), s.getEndTime(),
                s.getEndUserEmail(), s.getEndUserPhone(), Instant.now());
    }

    /** SESSION_CANCELLED */
    public static ValidationEvent cancelled(ca.optimusAI.tms.validation.entity.ValidationSession s) {
        return new ValidationEvent("SESSION_CANCELLED", s.getId(), s.getTenantId(),
                s.getClientId(), s.getSubTenantId(), s.getZoneId(), s.getLicensePlate(),
                s.getStatus(), s.getStartTime(), s.getEndTime(),
                s.getEndUserEmail(), s.getEndUserPhone(), Instant.now());
    }

    /** SESSION_EXPIRING_SOON */
    public static ValidationEvent expiringSoon(ca.optimusAI.tms.validation.entity.ValidationSession s) {
        return new ValidationEvent("SESSION_EXPIRING_SOON", s.getId(), s.getTenantId(),
                s.getClientId(), s.getSubTenantId(), s.getZoneId(), s.getLicensePlate(),
                s.getStatus(), s.getStartTime(), s.getEndTime(),
                s.getEndUserEmail(), s.getEndUserPhone(), Instant.now());
    }
}
