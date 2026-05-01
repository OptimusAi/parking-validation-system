package ca.optimusAI.tms.tenant.service;

import ca.optimusAI.tms.shared.PageResponse;
import ca.optimusAI.tms.shared.TenantContext;
import ca.optimusAI.tms.shared.exception.ResourceNotFoundException;
import ca.optimusAI.tms.shared.exception.UnauthorizedTenantAccessException;
import ca.optimusAI.tms.shared.exception.ZoneHasActiveSessionsException;
import ca.optimusAI.tms.tenant.entity.Zone;
import ca.optimusAI.tms.tenant.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ZoneService {

    private final ZoneRepository zoneRepository;

    @Transactional(readOnly = true)
    public PageResponse<Zone> list(int page, int size) {
        PageRequest pr = PageRequest.of(page, size, Sort.by("zoneNumber").ascending());
        return PageResponse.of(zoneRepository.findAllByIsDeletedFalse(pr));
    }

    @Transactional(readOnly = true)
    public PageResponse<Zone> listByTenant(UUID tenantId, int page, int size) {
        PageRequest pr = PageRequest.of(page, size, Sort.by("zoneNumber").ascending());
        return PageResponse.of(zoneRepository.findAllByTenantIdAndIsDeletedFalse(tenantId, pr));
    }

    @Transactional(readOnly = true)
    public Zone get(UUID id) {
        return zoneRepository.findById(id)
                .filter(z -> !z.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found: " + id));
    }

    @Transactional
    public Zone create(UUID tenantId, UUID clientId, String zoneNumber, String name,
                       int defaultDurationMinutes, int maxDurationMinutes) {
        UUID resolvedTenantId = tenantId != null ? tenantId : TenantContext.tenantId();
        UUID resolvedClientId = clientId != null ? clientId : TenantContext.clientId();
        if (resolvedTenantId == null) throw new UnauthorizedTenantAccessException("tenantId required");
        if (resolvedClientId == null) throw new UnauthorizedTenantAccessException("clientId required");

        Zone zone = Zone.builder()
                .tenantId(resolvedTenantId)
                .clientId(resolvedClientId)
                .zoneNumber(zoneNumber)
                .name(name)
                .defaultDurationMinutes(defaultDurationMinutes > 0 ? defaultDurationMinutes : 60)
                .maxDurationMinutes(maxDurationMinutes > 0 ? maxDurationMinutes : 1440)
                .build();
        return zoneRepository.save(zone);
    }

    @Transactional
    public Zone update(UUID id, String zoneNumber, String name,
                       Integer defaultDurationMinutes, Integer maxDurationMinutes) {
        Zone zone = get(id);
        assertWriteAccess(zone);
        if (zoneNumber != null) zone.setZoneNumber(zoneNumber);
        if (name != null) zone.setName(name);
        if (defaultDurationMinutes != null) zone.setDefaultDurationMinutes(defaultDurationMinutes);
        if (maxDurationMinutes != null) zone.setMaxDurationMinutes(maxDurationMinutes);
        return zoneRepository.save(zone);
    }

    /**
     * Soft-deletes a zone. Rejects if any ACTIVE or EXTENDED session exists in this zone.
     * Feature 6: "Cannot delete zone with active sessions"
     */
    @Transactional
    public void delete(UUID id) {
        Zone zone = get(id);
        assertWriteAccess(zone);

        if (zoneRepository.hasActiveSessions(id)) {
            throw new ZoneHasActiveSessionsException(
                    "Cannot delete zone with active sessions: " + zone.getName());
        }

        zone.setDeleted(true);
        zoneRepository.save(zone);
    }

    private void assertWriteAccess(Zone zone) {
        if (TenantContext.hasRole("ADMIN") || TenantContext.hasRole("CLIENT_ADMIN")) return;
        UUID callerTenantId = TenantContext.tenantId();
        if (callerTenantId == null || !callerTenantId.equals(zone.getTenantId())) {
            throw new UnauthorizedTenantAccessException("Access denied to zone: " + zone.getId());
        }
    }
}
