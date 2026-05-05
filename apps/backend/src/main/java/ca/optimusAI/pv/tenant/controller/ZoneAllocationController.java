package ca.optimusAI.pv.tenant.controller;

import ca.optimusAI.pv.shared.TenantContext;
import ca.optimusAI.pv.tenant.entity.ZoneAllocation;
import ca.optimusAI.pv.tenant.service.ZoneAllocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/zone-allocations")
@RequiredArgsConstructor
public class ZoneAllocationController {

    private final ZoneAllocationService allocationService;

    /**
     * GET /api/v1/zone-allocations?tenantId=...
     * Returns the allocation for a given tenant (or the caller's own tenant).
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<ZoneAllocationResponse> get(
            @RequestParam(required = false) UUID tenantId) {
        UUID tid = tenantId != null ? tenantId : TenantContext.tenantId();
        UUID cid = TenantContext.clientId();
        ZoneAllocation alloc = allocationService.get(tid, cid);
        long usedDirect = allocationService.usedDirect(tid);
        return ResponseEntity.ok(toResponse(alloc, usedDirect));
    }

    /**
     * PUT /api/v1/zone-allocations/total
     * Set the total zone limit for a tenant. ADMIN / CLIENT_ADMIN only.
     */
    @PutMapping("/total")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT_ADMIN')")
    public ResponseEntity<ZoneAllocationResponse> setTotal(@RequestBody SetTotalRequest req) {
        ZoneAllocation alloc = allocationService.setTotal(req.tenantId(), req.clientId(), req.totalZones());
        long usedDirect = allocationService.usedDirect(req.tenantId());
        return ResponseEntity.ok(toResponse(alloc, usedDirect));
    }

    /**
     * PUT /api/v1/zone-allocations/split
     * Adjust how the total is split between direct tenant use and sub-tenants.
     * TENANT_ADMIN (or higher) can call this.
     */
    @PutMapping("/split")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<ZoneAllocationResponse> setSplit(@RequestBody SetSplitRequest req) {
        UUID tid = req.tenantId() != null ? req.tenantId() : TenantContext.tenantId();
        UUID cid = req.clientId() != null ? req.clientId() : TenantContext.clientId();
        ZoneAllocation alloc = allocationService.setSplit(tid, cid, req.tenantDirect(), req.subTenant());
        long usedDirect = allocationService.usedDirect(tid);
        return ResponseEntity.ok(toResponse(alloc, usedDirect));
    }

    // ── DTOs ─────────────────────────────────────────────────────────────────

    private ZoneAllocationResponse toResponse(ZoneAllocation a, long usedDirect) {
        return new ZoneAllocationResponse(
                a.getTenantId(),
                a.getTotalZones(),
                a.getTenantDirect(),
                a.getSubTenant(),
                (int) usedDirect
        );
    }

    public record ZoneAllocationResponse(
            UUID tenantId,
            int totalZones,
            int tenantDirect,
            int subTenant,
            int usedDirect
    ) {}

    public record SetTotalRequest(
            UUID tenantId,
            UUID clientId,
            int totalZones
    ) {}

    public record SetSplitRequest(
            UUID tenantId,
            UUID clientId,
            int tenantDirect,
            int subTenant
    ) {}
}
