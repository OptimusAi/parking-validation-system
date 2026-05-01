package ca.optimusAI.tms.tenant.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Parking operator entity.
 * The tenant_id column mirrors id — required for the shared Hibernate filter.
 * When a TENANT_ADMIN is authenticated (tenantId set), the filter
 * returns only this specific tenant. CLIENT_ADMIN and ADMIN bypass via service layer.
 */
@Entity
@Table(name = "tenants")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId AND is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    /**
     * Mirrors id — required for the Hibernate tenantFilter to work consistently.
     */
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 255)
    private String name;

    /**
     * JSONB containing branding:
     * {"branding":{"logoUrl":"...","primaryColor":"#1B4F8A","accentColor":"#2E86C1"}}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String settings = "{}";

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean isDeleted = false;

    @PrePersist
    protected void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        // Ensure tenant_id always mirrors id after JPA assigns the UUID
        if (tenantId == null && id != null) tenantId = id;
    }

    @PreUpdate
    protected void preUpdate() {
        updatedAt = Instant.now();
    }
}
