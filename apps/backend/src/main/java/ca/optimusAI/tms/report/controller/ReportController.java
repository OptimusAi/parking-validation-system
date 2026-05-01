package ca.optimusAI.tms.report.controller;

import ca.optimusAI.tms.report.entity.ReportJob;
import ca.optimusAI.tms.report.service.ReportService;
import ca.optimusAI.tms.shared.PageResponse;
import ca.optimusAI.tms.shared.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<ReportJob> queue(@Valid @RequestBody QueueReportRequest req) {
        UUID tenantId   = TenantContext.tenantId();
        UUID clientId   = TenantContext.clientId();
        UUID requestedBy = TenantContext.userId() != null ? null : null;

        ReportJob job = reportService.queueReport(
                req.reportType(), req.format(), req.filters(),
                tenantId, clientId, requestedBy);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(job);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<ReportJob> get(@PathVariable UUID id) {
        return ResponseEntity.ok(reportService.getJob(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<PageResponse<ReportJob>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(reportService.list(page, size));
    }

    public record QueueReportRequest(
            @NotBlank(message = "reportType is required") String reportType,
            @NotBlank(message = "format is required") String format,
            Map<String, Object> filters
    ) {}
}
