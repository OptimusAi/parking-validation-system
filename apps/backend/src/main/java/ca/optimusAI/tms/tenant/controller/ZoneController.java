package ca.optimusAI.tms.tenant.controller;

import ca.optimusAI.tms.shared.PageResponse;
import ca.optimusAI.tms.tenant.entity.Zone;
import ca.optimusAI.tms.tenant.service.ZoneService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/zones")
@RequiredArgsConstructor
public class ZoneController {

    private final ZoneService zoneService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN', 'SUBTENANT_USER', 'VIEWER')")
    public ResponseEntity<PageResponse<Zone>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID tenantId) {
        if (tenantId != null) {
            return ResponseEntity.ok(zoneService.listByTenant(tenantId, page, size));
        }
        return ResponseEntity.ok(zoneService.list(page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Zone> get(@PathVariable UUID id) {
        return ResponseEntity.ok(zoneService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<Zone> create(@Valid @RequestBody CreateZoneRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(zoneService.create(
                        req.tenantId(), req.clientId(),
                        req.zoneNumber(), req.name(),
                        req.defaultDurationMinutes(),
                        req.maxDurationMinutes()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<Zone> update(@PathVariable UUID id,
                                        @Valid @RequestBody UpdateZoneRequest req) {
        return ResponseEntity.ok(zoneService.update(
                id, req.zoneNumber(), req.name(),
                req.defaultDurationMinutes(), req.maxDurationMinutes()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        zoneService.delete(id);
        return ResponseEntity.noContent().build();
    }

    public record CreateZoneRequest(
            UUID tenantId,
            UUID clientId,
            @NotBlank(message = "zoneNumber is required") String zoneNumber,
            @NotBlank(message = "name is required") String name,
            @Min(value = 1, message = "defaultDurationMinutes must be > 0") int defaultDurationMinutes,
            @Min(value = 1, message = "maxDurationMinutes must be > 0") int maxDurationMinutes
    ) {}

    public record UpdateZoneRequest(
            String zoneNumber,
            String name,
            Integer defaultDurationMinutes,
            Integer maxDurationMinutes
    ) {}
}
