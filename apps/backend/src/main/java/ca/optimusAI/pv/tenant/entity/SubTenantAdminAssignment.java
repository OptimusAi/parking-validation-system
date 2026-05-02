package ca.optimusAI.pv.tenant.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Maps a SUB_TENANT_ADMIN user to their single managed sub-tenant.
 * UNIQUE(user_id) enforces one-to-one: one SUB_TENANT_ADMIN → one sub-tenant.
 */
@Entity
@Table(
    name = "sub_tenant_admin_assignments",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_sub_tenant_admin_assignment",
        columnNames = {"user_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubTenantAdminAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "sub_tenant_id", nullable = false)
    private UUID subTenantId;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;

    @PrePersist
    protected void prePersist() {
        if (assignedAt == null) assignedAt = Instant.now();
    }
}
