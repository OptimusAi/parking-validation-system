package ca.optimusAI.pv.tenant.controller;

import ca.optimusAI.pv.shared.PageResponse;
import ca.optimusAI.pv.tenant.entity.Tenant;
import ca.optimusAI.pv.tenant.entity.TenantBranding;
import ca.optimusAI.pv.tenant.service.TenantService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<PageResponse<Tenant>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(tenantService.list(page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<Tenant> get(@PathVariable UUID id) {
        return ResponseEntity.ok(tenantService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT_ADMIN')")
    public ResponseEntity<Tenant> create(@Valid @RequestBody CreateTenantRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tenantService.create(req.clientId(), req.name()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT_ADMIN')")
    public ResponseEntity<Tenant> update(@PathVariable UUID id,
                                          @Valid @RequestBody UpdateTenantRequest req) {
        return ResponseEntity.ok(tenantService.update(id, req.name()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        tenantService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── Branding endpoints ────────────────────────────────────────────────────

    /**
     * Public — no auth. Used by the QR scan page to fetch tenant branding.
     * SecurityConfig explicitly permits GET /api/v1/tenants/{id}/branding.
     */
    @GetMapping("/{id}/branding")
    public ResponseEntity<TenantBranding> getBranding(@PathVariable UUID id) {
        return ResponseEntity.ok(tenantService.getBranding(id));
    }

    /**
     * Multipart upload: logoFile (optional) + primaryColor + accentColor.
     * All three are optional — only provided fields are updated.
     */
    @PutMapping(value = "/{id}/branding", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<TenantBranding> updateBranding(
            @PathVariable UUID id,
            @RequestPart(required = false) MultipartFile logoFile,
            @RequestParam(required = false) String primaryColor,
            @RequestParam(required = false) String accentColor) throws IOException {
        return ResponseEntity.ok(tenantService.updateBranding(id, logoFile, primaryColor, accentColor));
    }

    public record CreateTenantRequest(
            UUID clientId,
            @NotBlank(message = "name is required") String name
    ) {}

    public record UpdateTenantRequest(String name) {}
}
