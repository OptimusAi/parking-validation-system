package ca.optimusAI.tms.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "auth0_user_id", nullable = false, unique = true, length = 100)
    private String auth0UserId;

    @Column(length = 255)
    private String email;

    @Column(length = 255)
    private String name;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "sub_tenant_id")
    private UUID subTenantId;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String role = "SUBTENANT_USER";

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
