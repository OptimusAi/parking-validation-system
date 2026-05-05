package ca.optimusAI.pv.tenant.service;

import ca.optimusAI.pv.shared.PageResponse;
import ca.optimusAI.pv.shared.TenantContext;
import ca.optimusAI.pv.shared.exception.ResourceNotFoundException;
import ca.optimusAI.pv.shared.exception.UnauthorizedTenantAccessException;
import ca.optimusAI.pv.tenant.entity.SubTenant;
import ca.optimusAI.pv.tenant.entity.SubTenantZoneAssignment;
import ca.optimusAI.pv.tenant.entity.Zone;
import ca.optimusAI.pv.tenant.repository.SubTenantRepository;
import ca.optimusAI.pv.tenant.repository.SubTenantZoneAssignmentRepository;
import ca.optimusAI.pv.tenant.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubTenantService {

    private final SubTenantRepository subTenantRepository;
    private final SubTenantZoneAssignmentRepository assignmentRepository;
    private final ZoneRepository zoneRepository;

    @Transactional(readOnly = true)
    public PageResponse<SubTenant> list(int page, int size) {
        PageRequest pr = PageRequest.of(page, size, Sort.by("createdAt").descending());
        // Hibernate filter restricts results to callers tenantId (if set)
        // ADMIN override: pass explicit tenantId query param (handled in controller)
        return PageResponse.of(subTenantRepository.findAllByIsDeletedFalse(pr));
    }

    @Transactional(readOnly = true)
    public PageResponse<SubTenant> listByTenant(UUID tenantId, int page, int size) {
        PageRequest pr = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return PageResponse.of(subTenantRepository.findAllByTenantIdAndIsDeletedFalse(tenantId, pr));
    }

    @Transactional(readOnly = true)
    public SubTenant get(UUID id) {
        return subTenantRepository.findById(id)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Sub-tenant not found: " + id));
    }

    @Transactional
    public SubTenant create(UUID tenantId, UUID clientId, String name) {
        UUID resolvedTenantId = tenantId != null ? tenantId : TenantContext.tenantId();
        UUID resolvedClientId = clientId != null ? clientId : TenantContext.clientId();
        if (resolvedTenantId == null) throw new UnauthorizedTenantAccessException("tenantId required");
        if (resolvedClientId == null) throw new UnauthorizedTenantAccessException("clientId required");

        SubTenant sub = SubTenant.builder()
                .tenantId(resolvedTenantId)
                .clientId(resolvedClientId)
                .name(name)
                .build();
        return subTenantRepository.save(sub);
    }

    @Transactional
    public SubTenant update(UUID id, String name) {
        SubTenant sub = get(id);
        assertWriteAccess(sub);
        if (name != null) sub.setName(name);
        return subTenantRepository.save(sub);
    }

    @Transactional
    public void delete(UUID id) {
        SubTenant sub = get(id);
        assertWriteAccess(sub);
        sub.setDeleted(true);
        subTenantRepository.save(sub);
    }

    private void assertWriteAccess(SubTenant sub) {
        if (TenantContext.hasRole("ADMIN") || TenantContext.hasRole("CLIENT_ADMIN")) return;
        UUID callerTenantId = TenantContext.tenantId();
        if (callerTenantId == null || !callerTenantId.equals(sub.getTenantId())) {
            throw new UnauthorizedTenantAccessException("Access denied to sub-tenant: " + sub.getId());
        }
    }

    // ── Zone assignment ───────────────────────────────────────────────────────

    /** Returns the IDs of zones currently assigned to a sub-tenant. */
    @Transactional(readOnly = true)
    public List<UUID> getAssignedZoneIds(UUID subTenantId) {
        return assignmentRepository.findBySubTenantId(subTenantId)
                .stream().map(SubTenantZoneAssignment::getZoneId).collect(Collectors.toList());
    }

    /** Returns the full Zone objects assigned to a sub-tenant. */
    @Transactional(readOnly = true)
    public List<Zone> getAssignedZones(UUID subTenantId) {
        List<UUID> zoneIds = getAssignedZoneIds(subTenantId);
        if (zoneIds.isEmpty()) return List.of();
        return zoneRepository.findAllById(zoneIds).stream()
                .filter(z -> !z.isDeleted()).collect(Collectors.toList());
    }

    /**
     * Replace the full zone assignment set for a sub-tenant.
     * Passing an empty list removes all assignments.
     */
    @Transactional
    public List<UUID> setAssignedZones(UUID subTenantId, List<UUID> zoneIds) {
        SubTenant sub = get(subTenantId);
        assertWriteAccess(sub);

        // Remove all current assignments
        assignmentRepository.deleteBySubTenantId(subTenantId);

        // Add the new set
        if (zoneIds != null && !zoneIds.isEmpty()) {
            List<SubTenantZoneAssignment> assignments = zoneIds.stream()
                    .distinct()
                    .map(zoneId -> SubTenantZoneAssignment.builder()
                            .subTenantId(subTenantId)
                            .zoneId(zoneId)
                            .build())
                    .collect(Collectors.toList());
            assignmentRepository.saveAll(assignments);
        }

        return getAssignedZoneIds(subTenantId);
    }
}
