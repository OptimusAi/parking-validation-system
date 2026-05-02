package ca.optimusAI.pv.tenant.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Maps a CLIENT_ADMIN user to one of the tenants they are allowed to manage.
 * Carries the parent client_id so the CLIENT_ADMIN's client context is always known.
 * UNIQUE(user_id, tenant_id) — one CLIENT_ADMIN can manage many tenants.
 */
@Entity
@Table(
    name = "client_admin_assignments",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_client_admin_assignment",
        columnNames = {"user_id", "tenant_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientAdminAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;

    @PrePersist
    protected void prePersist() {
        if (assignedAt == null) assignedAt = Instant.now();
    }
}
