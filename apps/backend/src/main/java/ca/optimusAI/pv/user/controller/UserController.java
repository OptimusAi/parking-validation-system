package ca.optimusAI.pv.user.controller;

import ca.optimusAI.pv.shared.PageResponse;
import ca.optimusAI.pv.tenant.entity.ClientAdminAssignment;
import ca.optimusAI.pv.tenant.repository.ClientAdminAssignmentRepository;
import ca.optimusAI.pv.user.UserService;
import ca.optimusAI.pv.user.entity.AppUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ClientAdminAssignmentRepository clientAdminAssignmentRepository;

    /** List users — ADMIN: all, CLIENT_ADMIN: assigned tenants only. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT_ADMIN')")
    public ResponseEntity<PageResponse<AppUser>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(userService.list(page, size));
    }

    /** Return the currently authenticated user. */
    @GetMapping("/me")
    public ResponseEntity<AppUser> me() {
        return ResponseEntity.ok(userService.getMe());
    }

    /**
     * Assign a role to a user.
     * CLIENT_ADMIN cannot assign ADMIN or CLIENT_ADMIN.
     */
    @PutMapping("/{id}/role")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT_ADMIN')")
    public ResponseEntity<Map<String, Object>> assignRole(
            @PathVariable UUID id,
            @Valid @RequestBody AssignRoleRequest req) {
        AppUser updated = userService.assignRole(id, req.role());
        return ResponseEntity.ok(Map.of(
                "user", updated,
                "message", "User must re-login for role change to take effect"
        ));
    }

    /**
     * Assign tenant + client + optional sub-tenant to a user. ADMIN only.
     */
    @PutMapping("/{id}/tenant")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> assignTenant(
            @PathVariable UUID id,
            @Valid @RequestBody AssignTenantRequest req) {
        AppUser updated = userService.assignTenant(id, req.tenantId(), req.clientId(), req.subTenantId());
        return ResponseEntity.ok(Map.of(
                "user", updated,
                "message", "User must re-login for changes to take effect"
        ));
    }

    /** Deactivate a user. */
    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT_ADMIN')")
    public ResponseEntity<AppUser> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.deactivate(id));
    }

    /** Get tenants assigned to a CLIENT_ADMIN user. */
    @GetMapping("/{id}/assigned-tenants")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT_ADMIN')")
    public ResponseEntity<List<ClientAdminAssignment>> assignedTenants(@PathVariable UUID id) {
        return ResponseEntity.ok(clientAdminAssignmentRepository.findAllByUserId(id));
    }

    // ── Request records ───────────────────────────────────────────────────────

    public record AssignRoleRequest(
            @NotBlank(message = "role is required") String role
    ) {}

    public record AssignTenantRequest(
            UUID tenantId,
            UUID clientId,
            UUID subTenantId
    ) {}
}
