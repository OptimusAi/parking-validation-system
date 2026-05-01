package ca.optimusAI.pv.validation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "validation_sessions")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId AND is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "sub_tenant_id")
    private UUID subTenantId;

    @Column(name = "zone_id", nullable = false)
    private UUID zoneId;

    @Column(name = "license_plate", nullable = false, length = 10)
    private String licensePlate;

    @Column(name = "start_time")
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    /** ACTIVE | EXTENDED | CANCELLED | EXPIRED */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "extended_count", nullable = false)
    @Builder.Default
    private int extendedCount = 0;

    @Column(name = "end_user_email", length = 255)
    private String endUserEmail;

    @Column(name = "end_user_phone", length = 30)
    private String endUserPhone;

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
