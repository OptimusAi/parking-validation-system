package ca.optimusAI.pv.tenant.controller;

import ca.optimusAI.pv.shared.PageResponse;
import ca.optimusAI.pv.tenant.entity.SubTenant;
import ca.optimusAI.pv.tenant.service.SubTenantService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sub-tenants")
@RequiredArgsConstructor
public class SubTenantController {

    private final SubTenantService subTenantService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<PageResponse<SubTenant>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID tenantId) {
        if (tenantId != null) {
            return ResponseEntity.ok(subTenantService.listByTenant(tenantId, page, size));
        }
        return ResponseEntity.ok(subTenantService.list(page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<SubTenant> get(@PathVariable UUID id) {
        return ResponseEntity.ok(subTenantService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<SubTenant> create(@Valid @RequestBody CreateSubTenantRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subTenantService.create(req.tenantId(), req.clientId(), req.name()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<SubTenant> update(@PathVariable UUID id,
                                             @Valid @RequestBody UpdateSubTenantRequest req) {
        return ResponseEntity.ok(subTenantService.update(id, req.name()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        subTenantService.delete(id);
        return ResponseEntity.noContent().build();
    }

    public record CreateSubTenantRequest(
            UUID tenantId,
            UUID clientId,
            @NotBlank(message = "name is required") String name
    ) {}

    public record UpdateSubTenantRequest(String name) {}
}
