package ca.optimusAI.pv.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Application user — persisted in the app_user table.
 *
 * auth_provider_user_id links back to the OAuth provider's subject claim (sub).
 * role is one of: ADMIN | CLIENT_ADMIN | TENANT_ADMIN | SUB_TENANT_ADMIN | USER
 *
 * New users get role=USER and null tenant/client/subTenant — an ADMIN must
 * promote them via PUT /api/v1/users/{id}/role + PUT /api/v1/users/{id}/tenant.
 */
@Entity
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "auth_provider_user_id", nullable = false, unique = true, length = 200)
    private String authProviderUserId;

    @Column(length = 200)
    private String email;

    @Column(length = 200)
    private String name;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "sub_tenant_id")
    private UUID subTenantId;

    /** ADMIN | CLIENT_ADMIN | TENANT_ADMIN | SUB_TENANT_ADMIN | USER */
    @Column(nullable = false, length = 30)
    @Builder.Default
    private String role = "USER";

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    /** BCrypt-hashed password for local/dev authentication. Null in production (OAuth2 only). */
    @Column(name = "password_hash", length = 256)
    private String passwordHash;

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
