package ca.optimusAI.pv.validation;

import ca.optimusAI.pv.shared.PageResponse;
import ca.optimusAI.pv.shared.TenantContext;
import ca.optimusAI.pv.shared.exception.DuplicateValidationException;
import ca.optimusAI.pv.shared.exception.InvalidTokenException;
import ca.optimusAI.pv.shared.exception.ResourceNotFoundException;
import ca.optimusAI.pv.shared.exception.UnauthorizedTenantAccessException;
import ca.optimusAI.pv.tenant.entity.Zone;
import ca.optimusAI.pv.tenant.repository.ZoneRepository;
import ca.optimusAI.pv.validation.entity.ValidationSession;
import ca.optimusAI.pv.validation.event.ValidationEvent;
import ca.optimusAI.pv.validation.repository.ValidationRepository;
import ca.optimusAI.pv.validation.repository.ValidationSpec;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ValidationService {

    private static final String EXCHANGE     = "validation.events";
    private static final String ROUTING_CREATED   = "session.created";
    private static final String ROUTING_EXTENDED  = "session.extended";
    private static final String ROUTING_CANCELLED = "session.cancelled";
    private static final String ROUTING_EXPIRING  = "session.expiring";

    private static final int LP_MAX_LENGTH = 10;

    private final ValidationRepository validationRepository;
    private final ZoneRepository zoneRepository;
    private final QuotaEnforcementService quotaService;
    private final RabbitTemplate rabbitTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    // ── Create session (authenticated) ───────────────────────────────────────

    @Transactional
    public ValidationSession createSession(CreateSessionRequest req) {
        validateLicensePlate(req.licensePlate());

        UUID tenantId  = TenantContext.tenantId();
        UUID clientId  = TenantContext.clientId();
        UUID createdBy = resolveCreatedBy();

        if (tenantId == null) {
            throw new UnauthorizedTenantAccessException("No tenant assigned to current user");
        }

        Zone zone = zoneRepository.findById(req.zoneId())
                .filter(z -> !z.isDeleted() && z.isActive())
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found: " + req.zoneId()));

        // Enforce quota (throws QuotaExceededException → 422)
        quotaService.enforce(tenantId, TenantContext.get().subTenantId(), req.licensePlate());

        int durationMinutes = req.durationMinutes() != null
                ? Math.min(req.durationMinutes(), zone.getMaxDurationMinutes())
                : zone.getDefaultDurationMinutes();

        Instant now = Instant.now();
        ValidationSession session = ValidationSession.builder()
                .tenantId(tenantId)
                .clientId(clientId)
                .subTenantId(TenantContext.get().subTenantId())
                .zoneId(zone.getId())
                .licensePlate(req.licensePlate().toUpperCase())
                .startTime(now)
                .endTime(now.plusSeconds((long) durationMinutes * 60))
                .status("ACTIVE")
                .endUserEmail(req.endUserEmail())
                .endUserPhone(req.endUserPhone())
                .createdBy(createdBy)
                .build();

        ValidationSession saved = validationRepository.save(session);
        publish(ROUTING_CREATED, ValidationEvent.created(saved));
        log.info("Created session id={} tenantId={} plate={}", saved.getId(), tenantId, req.licensePlate());
        return saved;
    }

    // ── Create public session (QR token, no auth) ─────────────────────────────

    @Transactional
    public ValidationSession createPublicSession(String tokenStr, String licensePlate) {
        validateLicensePlate(licensePlate);

        // Look up the link by token — validates existence and active status
        // The validation_links table is queried via a native lookup to avoid
        // a circular dependency with the QrLink module (Phase 8).
        // For now we resolve the zone directly using the token stored in the link table.
        Object[] link = findLinkByToken(tokenStr);
        if (link == null) {
            throw new InvalidTokenException("Invalid or expired QR token");
        }

        UUID tenantId  = (UUID) link[0];
        UUID clientId  = (UUID) link[1];
        UUID zoneId    = (UUID) link[2];
        int  duration  = (int)  link[3];

        Zone zone = zoneRepository.findById(zoneId)
                .filter(z -> !z.isDeleted() && z.isActive())
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found: " + zoneId));

        quotaService.enforce(tenantId, null, licensePlate);

        Instant now = Instant.now();
        ValidationSession session = ValidationSession.builder()
                .tenantId(tenantId)
                .clientId(clientId)
                .zoneId(zone.getId())
                .licensePlate(licensePlate.toUpperCase())
                .startTime(now)
                .endTime(now.plusSeconds((long) duration * 60))
                .status("ACTIVE")
                .build();

        ValidationSession saved = validationRepository.save(session);
        publish(ROUTING_CREATED, ValidationEvent.created(saved));
        log.info("Created public session id={} tenantId={} plate={}", saved.getId(), tenantId, licensePlate);
        return saved;
    }

    // ── Extend session ────────────────────────────────────────────────────────

    @Transactional
    public ValidationSession extendSession(UUID id, int extraMinutes) {
        ValidationSession session = getOwnedSession(id);

        if (!"ACTIVE".equals(session.getStatus()) && !"EXTENDED".equals(session.getStatus())) {
            throw new DuplicateValidationException("Cannot extend a " + session.getStatus() + " session");
        }

        Zone zone = zoneRepository.findById(session.getZoneId())
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found: " + session.getZoneId()));

        Instant newEnd = session.getEndTime().plusSeconds((long) extraMinutes * 60);
        long totalSeconds = newEnd.getEpochSecond() - session.getStartTime().getEpochSecond();
        long maxSeconds   = (long) zone.getMaxDurationMinutes() * 60;

        if (totalSeconds > maxSeconds) {
            throw new DuplicateValidationException(
                    "Extension would exceed max duration of " + zone.getMaxDurationMinutes() + " minutes");
        }

        session.setEndTime(newEnd);
        session.setStatus("EXTENDED");
        session.setExtendedCount(session.getExtendedCount() + 1);

        ValidationSession saved = validationRepository.save(session);
        publish(ROUTING_EXTENDED, ValidationEvent.extended(saved));
        return saved;
    }

    // ── Cancel session ────────────────────────────────────────────────────────

    @Transactional
    public ValidationSession cancelSession(UUID id) {
        ValidationSession session = getOwnedSession(id);

        if ("CANCELLED".equals(session.getStatus()) || "EXPIRED".equals(session.getStatus())) {
            throw new DuplicateValidationException("Session is already " + session.getStatus());
        }

        session.setStatus("CANCELLED");
        ValidationSession saved = validationRepository.save(session);
        publish(ROUTING_CANCELLED, ValidationEvent.cancelled(saved));
        return saved;
    }

    // ── List (filtered) ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<ValidationSession> list(
            String status, UUID zoneId, String licensePlate,
            Instant from, Instant to, int page, int size) {
        PageRequest pr = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return PageResponse.of(validationRepository.findAll(
                ValidationSpec.filtered(status, zoneId, licensePlate, from, to), pr));
    }

    // ── Get by id ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ValidationSession get(UUID id) {
        return getOwnedSession(id);
    }

    // ── Publish expiring soon (called by scheduler) ───────────────────────────

    public void publishExpiringSoon(ValidationSession session) {
        publish(ROUTING_EXPIRING, ValidationEvent.expiringSoon(session));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private ValidationSession getOwnedSession(UUID id) {
        ValidationSession session = validationRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + id));

        UUID tenantId = TenantContext.tenantId();
        if (tenantId != null && !tenantId.equals(session.getTenantId())) {
            throw new UnauthorizedTenantAccessException("Access denied to session: " + id);
        }
        return session;
    }

    private void publish(String routingKey, ValidationEvent event) {
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, routingKey, event);
        } catch (Exception e) {
            log.error("Failed to publish {} event for session {}: {}",
                    event.eventType(), event.sessionId(), e.getMessage());
        }
    }

    private void validateLicensePlate(String plate) {
        if (plate == null || plate.isBlank() || plate.length() > LP_MAX_LENGTH
                || !plate.matches("[A-Za-z0-9]+")) {
            throw new IllegalArgumentException(
                    "Invalid license plate — alphanumeric, max " + LP_MAX_LENGTH + " characters");
        }
    }

    private UUID resolveCreatedBy() {
        String userId = TenantContext.userId();
        if (userId == null) return null;
        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            return null; // Auth0 user IDs are not UUIDs — store null
        }
    }

    /**
     * Looks up a validation link by token using a native query to avoid
     * a compile-time dependency on the QrLink module (which is Phase 8).
     * Returns [tenantId, clientId, zoneId, defaultDurationMinutes] or null.
     */
    private Object[] findLinkByToken(String token) {
        try {
            @SuppressWarnings("unchecked")
            var result = (java.util.List<Object[]>) entityManager
                    .createNativeQuery(
                            "SELECT tenant_id, client_id, zone_id, default_duration_minutes " +
                            "FROM validation_links WHERE token = :token " +
                            "AND is_active = true AND is_deleted = false")
                    .setParameter("token", token)
                    .getResultList();
            if (result.isEmpty()) return null;
            Object[] row = (Object[]) result.get(0);
            return new Object[]{
                    UUID.fromString(row[0].toString()),
                    UUID.fromString(row[1].toString()),
                    UUID.fromString(row[2].toString()),
                    ((Number) row[3]).intValue()
            };
        } catch (Exception e) {
            log.warn("Failed to resolve token: {}", e.getMessage());
            return null;
        }
    }

    // ── Request record ────────────────────────────────────────────────────────

    public record CreateSessionRequest(
            UUID zoneId,
            String licensePlate,
            Integer durationMinutes,
            String endUserEmail,
            String endUserPhone
    ) {}
}
