package ca.optimusAI.tms.user.controller;

import ca.optimusAI.tms.shared.PageResponse;
import ca.optimusAI.tms.user.UserService;
import ca.optimusAI.tms.user.entity.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * List users.
     * ADMIN: all users. CLIENT_ADMIN: only users in their client.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT_ADMIN')")
    public ResponseEntity<PageResponse<User>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(userService.list(page, size));
    }

    /**
     * Return the currently authenticated user.
     */
    @GetMapping("/me")
    public ResponseEntity<User> me() {
        return ResponseEntity.ok(userService.getMe());
    }

    /**
     * Assign a role to a user.
     * After save, the Redis cache is evicted → next API call reflects the new role immediately.
     */
    @PutMapping("/{id}/role")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT_ADMIN')")
    public ResponseEntity<User> assignRole(
            @PathVariable UUID id,
            @Valid @RequestBody AssignRoleRequest req) {
        return ResponseEntity.ok(userService.assignRole(id, req.role()));
    }

    /**
     * Assign tenant + client to a user. ADMIN only.
     * Cache is evicted so tenant context updates on next request.
     */
    @PutMapping("/{id}/tenant")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> assignTenant(
            @PathVariable UUID id,
            @Valid @RequestBody AssignTenantRequest req) {
        return ResponseEntity.ok(userService.assignTenant(id, req.tenantId(), req.clientId()));
    }

    // ── Request records ───────────────────────────────────────────────────────

    public record AssignRoleRequest(
            @NotBlank(message = "role is required") String role
    ) {}

    public record AssignTenantRequest(
            UUID tenantId,
            UUID clientId
    ) {}
}
