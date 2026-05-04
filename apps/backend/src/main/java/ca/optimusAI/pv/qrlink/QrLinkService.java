package ca.optimusAI.pv.qrlink;

import ca.optimusAI.pv.qrlink.entity.ValidationLink;
import ca.optimusAI.pv.qrlink.repository.ValidationLinkRepository;
import ca.optimusAI.pv.shared.PageResponse;
import ca.optimusAI.pv.shared.TenantContext;
import ca.optimusAI.pv.shared.exception.InvalidTokenException;
import ca.optimusAI.pv.shared.exception.ResourceNotFoundException;
import ca.optimusAI.pv.shared.exception.UnauthorizedTenantAccessException;
import ca.optimusAI.pv.tenant.entity.Zone;
import ca.optimusAI.pv.tenant.repository.ZoneRepository;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class QrLinkService {

    private final ValidationLinkRepository linkRepository;
    private final ZoneRepository zoneRepository;

    @Value("${qr.hmac-secret}")
    private String hmacSecret;

    @Value("${qr.base-url}")
    private String baseUrl;

    // ── Create link ───────────────────────────────────────────────────────────

    @Transactional
    public ValidationLink createLink(CreateLinkRequest req) {
        UUID tenantId  = TenantContext.tenantId();
        // CLIENT_ADMIN has no tenantId in JWT — use the one supplied in the request body
        if (tenantId == null && req.tenantId() != null) {
            tenantId = req.tenantId();
        }
        UUID clientId  = TenantContext.clientId();
        // CLIENT_ADMIN may also lack clientId in TenantContext — use request body fallback
        if (clientId == null && req.clientId() != null) {
            clientId = req.clientId();
        }
        UUID createdBy = resolveCreatedBy();

        if (tenantId == null) {
            throw new UnauthorizedTenantAccessException("No tenant assigned to current user");
        }

        final UUID resolvedTenantId = tenantId;

        // Verify the zone belongs to this tenant
        zoneRepository.findById(req.zoneId())
                .filter(z -> !z.isDeleted() && z.isActive() && z.getTenantId().equals(resolvedTenantId))
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found: " + req.zoneId()));

        int duration = req.defaultDurationMinutes() != null ? req.defaultDurationMinutes() : 60;
        Instant expiresAt = req.expiresAt();

        // Generate HMAC token
        String token = generateToken(resolvedTenantId, req.zoneId(), duration, expiresAt);

        ValidationLink link = ValidationLink.builder()
                .tenantId(resolvedTenantId)
                .clientId(clientId)
                .zoneId(req.zoneId())
                .linkType(req.linkType() != null ? req.linkType() : "QR")
                .token(token)
                .label(req.label())
                .defaultDurationMinutes(duration)
                .expiresAt(expiresAt)
                .createdBy(createdBy)
                .build();

        ValidationLink saved = linkRepository.save(link);
        log.info("Created validation link id={} tenantId={} zoneId={}", saved.getId(), tenantId, req.zoneId());
        return saved;
    }

    // ── List ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<ValidationLink> list(int page, int size) {
        PageRequest pr = PageRequest.of(page, size, Sort.by("createdAt").descending());
        var linkPage = linkRepository.findAll(pr);

        // Enrich with zone names
        var zoneIds = linkPage.getContent().stream()
                .map(ValidationLink::getZoneId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, String> zoneNames = zoneRepository.findAllById(zoneIds).stream()
                .collect(Collectors.toMap(Zone::getId, Zone::getName));
        linkPage.getContent().forEach(l -> l.setZoneName(zoneNames.get(l.getZoneId())));

        return PageResponse.of(linkPage);
    }

    // ── Get by id ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ValidationLink getById(UUID id) {
        return getOwnedLink(id);
    }

    // ── Get by token (public) ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ValidationLink getByToken(String token) {
        return linkRepository.findActiveByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Link not found for token"));
    }

    // ── Deactivate ────────────────────────────────────────────────────────────

    @Transactional
    public ValidationLink deactivate(UUID id) {
        ValidationLink link = getOwnedLink(id);
        link.setActive(false);
        return linkRepository.save(link);
    }

    // ── Increment scan count (called by ValidationService on public session) ──

    @Transactional
    public void incrementScanCount(String token) {
        linkRepository.findActiveByToken(token)
                .ifPresent(l -> linkRepository.incrementScanCount(l.getId()));
    }

    // ── Build the public URL for a given token ────────────────────────────────

    public String buildPublicUrl(String token) {
        return baseUrl + "/validate/" + token;
    }

    // ── HMAC token generation (from skills.md pattern) ────────────────────────

    public String generateToken(UUID tenantId, UUID zoneId, int durationMin, Instant expiresAt) {
        try {
            String nonce = UUID.randomUUID().toString();
            long expiryEpoch = expiresAt != null ? expiresAt.getEpochSecond() : 0;
            String payload = String.join(":",
                    tenantId.toString(),
                    zoneId.toString(),
                    String.valueOf(durationMin),
                    String.valueOf(expiryEpoch),
                    nonce);
            String sig = hmacBase64(payload);
            String b64 = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
            return sig + "." + b64;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate HMAC token", e);
        }
    }

    // ── HMAC token validation ─────────────────────────────────────────────────

    public TokenPayload validateToken(String token) {
        String[] parts = token.split("\\.", 2);
        if (parts.length != 2) {
            throw new InvalidTokenException("Malformed QR token");
        }
        try {
            String payload = new String(
                    Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            if (!hmacBase64(payload).equals(parts[0])) {
                throw new InvalidTokenException("Invalid QR token signature");
            }
            String[] fields = payload.split(":", 5);
            if (fields.length < 5) {
                throw new InvalidTokenException("Malformed QR token payload");
            }
            long expiryEpoch = Long.parseLong(fields[3]);
            if (expiryEpoch > 0 && Instant.now().isAfter(Instant.ofEpochSecond(expiryEpoch))) {
                throw new InvalidTokenException("QR token has expired");
            }
            return new TokenPayload(
                    UUID.fromString(fields[0]),
                    UUID.fromString(fields[1]),
                    Integer.parseInt(fields[2]),
                    expiryEpoch > 0 ? Instant.ofEpochSecond(expiryEpoch) : null
            );
        } catch (InvalidTokenException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidTokenException("Failed to validate QR token: " + e.getMessage());
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private ValidationLink getOwnedLink(UUID id) {
        ValidationLink link = linkRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Link not found: " + id));
        UUID tenantId = TenantContext.tenantId();
        if (tenantId != null && !tenantId.equals(link.getTenantId())) {
            throw new UnauthorizedTenantAccessException("Access denied to link: " + id);
        }
        return link;
    }

    private String hmacBase64(String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(
                hmacSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    private UUID resolveCreatedBy() {
        String userId = TenantContext.userId();
        if (userId == null) return null;
        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ── Request / response records ────────────────────────────────────────────

    public record CreateLinkRequest(
            UUID zoneId,
            UUID tenantId,
            UUID clientId,
            String linkType,
            String label,
            Integer defaultDurationMinutes,
            Instant expiresAt
    ) {}

    public record TokenPayload(
            UUID tenantId,
            UUID zoneId,
            int durationMinutes,
            Instant expiresAt
    ) {}
}
