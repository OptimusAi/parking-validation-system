package ca.optimusAI.tms.notification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Records every notification attempt — SMS, email, or WebSocket push.
 * Written after each send attempt regardless of success.
 * No Hibernate tenant filter — notifications are written cross-tenant by the listener.
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "session_id")
    private UUID sessionId;

    /** SMS | EMAIL | WEBSOCKET */
    @Column(nullable = false, length = 20)
    private String channel;

    @Column(length = 255)
    private String recipient;

    @Column(name = "event_type", length = 50)
    private String eventType;

    /** SENT | FAILED */
    @Column(nullable = false, length = 10)
    @Builder.Default
    private String status = "SENT";

    /** JSON blob of the event payload for audit purposes */
    @Column(columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String payload = "{}";

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean isDeleted = false;

    @PrePersist
    protected void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
