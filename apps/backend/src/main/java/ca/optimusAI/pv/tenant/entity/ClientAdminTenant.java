package ca.optimusAI.pv.tenant.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Join table: tracks which tenants a CLIENT_ADMIN user is allowed to manage.
 * One row per (user_id, tenant_id) pair.
 */
@Entity
@Table(
    name = "client_admin_tenants",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_client_admin_tenant",
        columnNames = {"user_id", "tenant_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientAdminTenant {

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
