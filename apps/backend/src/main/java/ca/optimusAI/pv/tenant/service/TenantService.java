package ca.optimusAI.pv.tenant.service;

import ca.optimusAI.pv.shared.PageResponse;
import ca.optimusAI.pv.shared.TenantContext;
import ca.optimusAI.pv.shared.exception.TenantNotFoundException;
import ca.optimusAI.pv.shared.exception.UnauthorizedTenantAccessException;
import ca.optimusAI.pv.tenant.entity.ClientAdminAssignment;
import ca.optimusAI.pv.tenant.entity.Tenant;
import ca.optimusAI.pv.tenant.entity.TenantBranding;
import ca.optimusAI.pv.tenant.entity.TenantResponse;
import ca.optimusAI.pv.tenant.repository.ClientAdminAssignmentRepository;
import ca.optimusAI.pv.tenant.repository.SubTenantRepository;
import ca.optimusAI.pv.tenant.repository.TenantRepository;
import ca.optimusAI.pv.tenant.repository.ZoneRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final ClientAdminAssignmentRepository clientAdminAssignmentRepository;
    private final ZoneRepository zoneRepository;
    private final SubTenantRepository subTenantRepository;
    private final AwsS3Service awsS3Service;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public PageResponse<Tenant> list(int page, int size) {
        PageRequest pr = PageRequest.of(page, size, Sort.by("createdAt").descending());

        if (TenantContext.hasRole("ADMIN")) {
            return PageResponse.of(tenantRepository.findAllByIsDeletedFalse(pr));
        }
        if (TenantContext.hasRole("CLIENT_ADMIN")) {
            List<UUID> assignedTenants = TenantContext.assignedTenants();
            if (assignedTenants.isEmpty()) {
                // Fall back to clientId
                UUID clientId = TenantContext.clientId();
                if (clientId == null) throw new UnauthorizedTenantAccessException("No client assigned");
                return PageResponse.of(tenantRepository.findAllByClientIdAndIsDeletedFalse(clientId, pr));
            }
            return PageResponse.of(tenantRepository.findAllByIdInAndIsDeletedFalse(assignedTenants, pr));
        }
        // TENANT_ADMIN — Hibernate filter already restricts to their tenantId
        return PageResponse.of(tenantRepository.findAllByIsDeletedFalse(pr));
    }

    @Transactional(readOnly = true)
    public PageResponse<TenantResponse> listEnriched(int page, int size) {
        PageRequest pr = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Tenant> tenantPage;
        if (TenantContext.hasRole("ADMIN")) {
            tenantPage = tenantRepository.findAllByIsDeletedFalse(pr);
        } else if (TenantContext.hasRole("CLIENT_ADMIN")) {
            List<UUID> assigned = TenantContext.assignedTenants();
            if (assigned.isEmpty()) {
                UUID clientId = TenantContext.clientId();
                if (clientId == null) throw new UnauthorizedTenantAccessException("No client assigned");
                tenantPage = tenantRepository.findAllByClientIdAndIsDeletedFalse(clientId, pr);
            } else {
                tenantPage = tenantRepository.findAllByIdInAndIsDeletedFalse(assigned, pr);
            }
        } else {
            tenantPage = tenantRepository.findAllByIsDeletedFalse(pr);
        }
        return PageResponse.of(tenantPage.map(this::toResponse));
    }

    private TenantResponse toResponse(Tenant t) {
        long zones = zoneRepository.countByTenantIdAndIsDeletedFalse(t.getId());
        long subs  = subTenantRepository.countByTenantIdAndIsDeletedFalse(t.getId());
        TenantBranding b = extractBranding(t.getSettings());
        return new TenantResponse(
                t.getId(), t.getClientId(), t.getName(), t.isActive(),
                zones, subs,
                new TenantResponse.BrandingDto(b.logoUrl(), b.primaryColor(), b.accentColor()),
                t.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public Tenant get(UUID id) {
        Tenant tenant = tenantRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + id));
        assertAccess(tenant);
        return tenant;
    }

    @Transactional
    public Tenant create(UUID clientId, String name) {
        UUID resolvedClientId = clientId != null ? clientId : TenantContext.clientId();
        if (resolvedClientId == null) {
            throw new UnauthorizedTenantAccessException("clientId required");
        }
        Tenant tenant = Tenant.builder()
                .clientId(resolvedClientId)
                .name(name)
                .settings("{\"branding\":{\"logoUrl\":\"\",\"primaryColor\":\"#1B4F8A\",\"accentColor\":\"#2E86C1\"}}")
                .build();
        Tenant saved = tenantRepository.save(tenant);
        // Ensure tenant_id mirrors id
        saved.setTenantId(saved.getId());
        saved = tenantRepository.save(saved);

        // If CLIENT_ADMIN created this tenant, auto-assign them to it
        if (TenantContext.hasRole("CLIENT_ADMIN")) {
            String userIdStr = TenantContext.userId();
            UUID callerClientId = TenantContext.clientId();
            if (userIdStr != null && callerClientId != null) {
                try {
                    clientAdminAssignmentRepository.save(ClientAdminAssignment.builder()
                            .userId(UUID.fromString(userIdStr))
                            .clientId(callerClientId)
                            .tenantId(saved.getId())
                            .build());
                } catch (Exception ignored) {
                    // May fail if userId is not a UUID (authProviderUserId) — best effort
                }
            }
        }
        return saved;
    }

    @Transactional
    public Tenant update(UUID id, String name) {
        Tenant tenant = get(id);
        if (name != null) tenant.setName(name);
        return tenantRepository.save(tenant);
    }

    @Transactional
    public void delete(UUID id) {
        Tenant tenant = get(id);
        tenant.setDeleted(true);
        tenantRepository.save(tenant);
    }

    // ── CLIENT_ADMIN assignment ───────────────────────────────────────────────

    @Transactional
    public void assignAdmin(UUID tenantId, UUID userId) {
        Tenant tenant = tenantRepository.findByIdAndIsDeletedFalse(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + tenantId));
        clientAdminAssignmentRepository.findByUserIdAndTenantId(userId, tenantId).ifPresentOrElse(
                existing -> log.debug("Assignment already exists for userId={} tenantId={}", userId, tenantId),
                () -> clientAdminAssignmentRepository.save(
                        ClientAdminAssignment.builder()
                                .userId(userId)
                                .clientId(tenant.getClientId())
                                .tenantId(tenantId)
                                .build())
        );
    }

    @Transactional
    public void unassignAdmin(UUID tenantId, UUID userId) {
        clientAdminAssignmentRepository.deleteByUserIdAndTenantId(userId, tenantId);
    }

    // ── Branding ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TenantBranding getBranding(UUID tenantId) {
        Tenant tenant = tenantRepository.findByIdAndIsDeletedFalse(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + tenantId));
        return extractBranding(tenant.getSettings());
    }

    @Transactional
    public TenantBranding updateBranding(UUID tenantId, MultipartFile logoFile,
                                         String primaryColor, String accentColor) throws IOException {
        Tenant tenant = tenantRepository.findByIdAndIsDeletedFalse(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + tenantId));
        assertAccess(tenant);

        Map<String, Object> settings = parseSettings(tenant.getSettings());
        @SuppressWarnings("unchecked")
        Map<String, Object> branding = (Map<String, Object>)
                settings.computeIfAbsent("branding", k -> new HashMap<>());

        if (logoFile != null && !logoFile.isEmpty()) {
            String logoUrl = awsS3Service.uploadTenantLogo(tenantId, logoFile);
            if (logoUrl != null) branding.put("logoUrl", logoUrl);
        }
        if (primaryColor != null) branding.put("primaryColor", primaryColor);
        if (accentColor != null)  branding.put("accentColor", accentColor);

        settings.put("branding", branding);
        tenant.setSettings(objectMapper.writeValueAsString(settings));
        tenantRepository.save(tenant);

        return extractBranding(tenant.getSettings());
    }

    // ── Access control ────────────────────────────────────────────────────────

    private void assertAccess(Tenant tenant) {
        if (TenantContext.hasRole("ADMIN")) return;
        if (TenantContext.hasRole("CLIENT_ADMIN")) {
            if (TenantContext.canAccessTenant(tenant.getId())) return;
            // Fall back: check clientId match
            UUID callerClientId = TenantContext.clientId();
            if (callerClientId != null && callerClientId.equals(tenant.getClientId())) return;
            throw new UnauthorizedTenantAccessException("Access denied to tenant: " + tenant.getId());
        }
        if (TenantContext.hasRole("TENANT_ADMIN")) {
            if (TenantContext.canAccessTenant(tenant.getId())) return;
            throw new UnauthorizedTenantAccessException("Access denied to tenant: " + tenant.getId());
        }
        throw new UnauthorizedTenantAccessException("Insufficient role");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Map<String, Object> parseSettings(String json) {
        if (json == null || json.isBlank()) return new HashMap<>();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse tenant settings JSON: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private TenantBranding extractBranding(String settingsJson) {
        Map<String, Object> settings = parseSettings(settingsJson);
        Object raw = settings.get("branding");
        if (!(raw instanceof Map)) return TenantBranding.defaults();

        @SuppressWarnings("unchecked")
        Map<String, Object> b = (Map<String, Object>) raw;
        return new TenantBranding(
                (String) b.getOrDefault("logoUrl", ""),
                (String) b.getOrDefault("primaryColor", "#1B4F8A"),
                (String) b.getOrDefault("accentColor", "#2E86C1")
        );
    }
}
