package ca.optimusAI.pv.tenant.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Tracks how a tenant splits its total zone budget.
 *
 *   totalZones   — total limit granted by CLIENT_ADMIN / ADMIN
 *   tenantDirect — zones the tenant reserves for its own direct use
 *   subTenant    — zones the tenant reserves for sub-tenants
 *
 * Invariant: tenantDirect + subTenant <= totalZones  (also enforced by DB CHECK)
 */
@Entity
@Table(name = "zone_allocations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ZoneAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, unique = true)
    private UUID tenantId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    /** Total zone limit granted by CLIENT_ADMIN / ADMIN */
    @Column(name = "total_zones", nullable = false)
    @Builder.Default
    private int totalZones = 0;

    /** Zones reserved for the tenant's own direct use */
    @Column(name = "tenant_direct", nullable = false)
    @Builder.Default
    private int tenantDirect = 0;

    /** Zones reserved for sub-tenants */
    @Column(name = "sub_tenant", nullable = false)
    @Builder.Default
    private int subTenant = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    protected void preUpdate() {
        updatedAt = Instant.now();
    }
}
