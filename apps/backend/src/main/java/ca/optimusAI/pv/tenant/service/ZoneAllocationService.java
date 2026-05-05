package ca.optimusAI.pv.tenant.service;

import ca.optimusAI.pv.shared.TenantContext;
import ca.optimusAI.pv.shared.exception.UnauthorizedTenantAccessException;
import ca.optimusAI.pv.tenant.entity.ZoneAllocation;
import ca.optimusAI.pv.tenant.repository.ZoneAllocationRepository;
import ca.optimusAI.pv.tenant.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ZoneAllocationService {

    private final ZoneAllocationRepository allocationRepository;
    private final ZoneRepository zoneRepository;

    // ── Get (or default) ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ZoneAllocation get(UUID tenantId, UUID clientId) {
        return allocationRepository.findByTenantId(tenantId)
                .orElseGet(() -> ZoneAllocation.builder()
                        .tenantId(tenantId)
                        .clientId(clientId)
                        .totalZones(0)
                        .tenantDirect(0)
                        .subTenant(0)
                        .build());
    }

    // ── Set total limit (CLIENT_ADMIN / ADMIN only) ───────────────────────────

    @Transactional
    public ZoneAllocation setTotal(UUID tenantId, UUID clientId, int totalZones) {
        if (!TenantContext.hasRole("ADMIN") && !TenantContext.hasRole("CLIENT_ADMIN")) {
            throw new UnauthorizedTenantAccessException("Only ADMIN or CLIENT_ADMIN can set the total zone limit");
        }
        if (totalZones < 0) throw new IllegalArgumentException("totalZones must be >= 0");

        ZoneAllocation alloc = allocationRepository.findByTenantId(tenantId)
                .orElseGet(() -> ZoneAllocation.builder()
                        .tenantId(tenantId)
                        .clientId(clientId)
                        .build());

        // Clamp existing split to new total
        int newDirect = Math.min(alloc.getTenantDirect(), totalZones);
        int newSub    = Math.min(alloc.getSubTenant(), totalZones - newDirect);

        alloc.setTotalZones(totalZones);
        alloc.setTenantDirect(newDirect);
        alloc.setSubTenant(newSub);
        return allocationRepository.save(alloc);
    }

    // ── Adjust split (TENANT_ADMIN / CLIENT_ADMIN / ADMIN) ───────────────────

    @Transactional
    public ZoneAllocation setSplit(UUID tenantId, UUID clientId, int tenantDirect, int subTenant) {
        if (tenantDirect < 0 || subTenant < 0) {
            throw new IllegalArgumentException("Zone counts must be >= 0");
        }

        ZoneAllocation alloc = allocationRepository.findByTenantId(tenantId)
                .orElseGet(() -> ZoneAllocation.builder()
                        .tenantId(tenantId)
                        .clientId(clientId)
                        .build());

        if (tenantDirect + subTenant > alloc.getTotalZones()) {
            throw new IllegalArgumentException(
                    String.format("tenantDirect(%d) + subTenant(%d) = %d exceeds totalZones(%d)",
                            tenantDirect, subTenant, tenantDirect + subTenant, alloc.getTotalZones()));
        }

        alloc.setTenantDirect(tenantDirect);
        alloc.setSubTenant(subTenant);
        return allocationRepository.save(alloc);
    }

    // ── Used counts ──────────────────────────────────────────────────────────

    /** Number of active (non-deleted) zones for the tenant (direct, no sub_tenant_id). */
    public long usedDirect(UUID tenantId) {
        return zoneRepository.countByTenantIdAndIsDeletedFalse(tenantId);
    }
}
