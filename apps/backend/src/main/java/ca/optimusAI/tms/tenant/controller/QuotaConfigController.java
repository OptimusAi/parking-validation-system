package ca.optimusAI.tms.tenant.controller;

import ca.optimusAI.tms.shared.PageResponse;
import ca.optimusAI.tms.tenant.entity.QuotaConfig;
import ca.optimusAI.tms.tenant.service.QuotaConfigService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/quota-configs")
@RequiredArgsConstructor
public class QuotaConfigController {

    private final QuotaConfigService quotaConfigService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<PageResponse<QuotaConfig>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(quotaConfigService.list(page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<QuotaConfig> get(@PathVariable UUID id) {
        return ResponseEntity.ok(quotaConfigService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<QuotaConfig> create(@Valid @RequestBody CreateQuotaConfigRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(quotaConfigService.create(
                        req.tenantId(), req.clientId(), req.subTenantId(),
                        req.scope(), req.period(), req.maxCount()));
    }

    /** CRU — no delete endpoint as per agents.md. */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<QuotaConfig> update(@PathVariable UUID id,
                                               @RequestBody UpdateQuotaConfigRequest req) {
        return ResponseEntity.ok(quotaConfigService.update(id, req.maxCount(), req.scope()));
    }

    public record CreateQuotaConfigRequest(
            UUID tenantId,
            UUID clientId,
            UUID subTenantId,
            @NotBlank(message = "scope is required") String scope,
            @NotBlank(message = "period is required") String period,
            int maxCount
    ) {}

    public record UpdateQuotaConfigRequest(Integer maxCount, String scope) {}
}
