package ca.optimusAI.pv.tenant.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Join table: maps a TENANT_ADMIN user to their single managed tenant.
 * UNIQUE(user_id) enforces the one-to-one constraint — one TENANT_ADMIN → one tenant.
 */
@Entity
@Table(
    name = "tenant_admin_tenants",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_tenant_admin_user",
        columnNames = {"user_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantAdminTenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;

    @PrePersist
    protected void prePersist() {
        if (assignedAt == null) assignedAt = Instant.now();
    }
}
