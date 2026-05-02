package ca.optimusAI.pv.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Persists the role for an {@link AppUser}.
 *
 * One row per user (enforced by the unique constraint on user_id).
 * Tenant/client/sub-tenant scope lives in the dedicated assignment tables:
 * client_admin_assignments, tenant_admin_assignments, sub_tenant_admin_assignments.
 */
@Entity
@Table(name = "user_role")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** FK to app_user.id */
    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    /** ADMIN | CLIENT_ADMIN | TENANT_ADMIN | SUB_TENANT_ADMIN | USER */
    @Column(nullable = false, length = 30)
    @Builder.Default
    private String role = "USER";

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

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
