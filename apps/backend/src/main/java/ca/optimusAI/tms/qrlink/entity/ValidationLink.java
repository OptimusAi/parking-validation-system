package ca.optimusAI.tms.qrlink.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "validation_links")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId AND is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "zone_id", nullable = false)
    private UUID zoneId;

    /** URL | QR */
    @Column(name = "link_type", nullable = false, length = 10)
    private String linkType;

    /** HMAC-signed token stored in the DB */
    @Column(nullable = false, length = 500)
    private String token;

    @Column(length = 255)
    private String label;

    @Column(name = "default_duration_minutes", nullable = false)
    @Builder.Default
    private int defaultDurationMinutes = 60;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "scan_count", nullable = false)
    @Builder.Default
    private int scanCount = 0;

    @Column(name = "created_by")
    private UUID createdBy;

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
    }

    @PreUpdate
    protected void preUpdate() {
        updatedAt = Instant.now();
    }
}
