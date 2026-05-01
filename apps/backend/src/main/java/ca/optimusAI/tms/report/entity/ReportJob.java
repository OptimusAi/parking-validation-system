package ca.optimusAI.tms.report.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "report_jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    /** VALIDATION_SESSIONS */
    @Column(name = "report_type", nullable = false, length = 50)
    private String reportType;

    /** CSV | EXCEL | PDF */
    @Column(nullable = false, length = 10)
    private String format;

    @Column(columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String filters = "{}";

    /** QUEUED | PROCESSING | COMPLETED | FAILED */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "QUEUED";

    @Column(name = "file_url", columnDefinition = "text")
    private String fileUrl;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "requested_by")
    private UUID requestedBy;

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
