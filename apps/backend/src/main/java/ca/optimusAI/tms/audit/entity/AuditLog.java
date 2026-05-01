package ca.optimusAI.tms.audit.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    private UUID id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "actor_id", length = 100)
    private String actorId;

    @Column(name = "actor_email", length = 255)
    private String actorEmail;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "before_state", columnDefinition = "jsonb")
    private String beforeState;

    @Column(name = "after_state", columnDefinition = "jsonb")
    private String afterState;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void prePersist() {
        if (id != null) {
            throw new IllegalStateException("AuditLog is append-only — update is not permitted");
        }
        id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }

    @PreUpdate
    protected void preUpdate() {
        throw new IllegalStateException("AuditLog is append-only — update is not permitted");
    }
}
