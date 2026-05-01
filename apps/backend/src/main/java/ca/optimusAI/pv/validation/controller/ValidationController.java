package ca.optimusAI.pv.validation.controller;

import ca.optimusAI.pv.shared.PageResponse;
import ca.optimusAI.pv.validation.ValidationService;
import ca.optimusAI.pv.validation.ValidationService.CreateSessionRequest;
import ca.optimusAI.pv.validation.entity.ValidationSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/validations")
@RequiredArgsConstructor
public class ValidationController {

    private final ValidationService validationService;

    // ── POST /api/v1/validations ──────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ValidationSession> create(@Valid @RequestBody CreateRequest req) {
        ValidationSession session = validationService.createSession(
                new CreateSessionRequest(
                        req.zoneId(),
                        req.licensePlate(),
                        req.durationMinutes(),
                        req.endUserEmail(),
                        req.endUserPhone()));
        return ResponseEntity.status(HttpStatus.CREATED).body(session);
    }

    // ── GET /api/v1/validations ───────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<ValidationSession>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID zoneId,
            @RequestParam(required = false) String licensePlate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                validationService.list(status, zoneId, licensePlate, from, to, page, size));
    }

    // ── GET /api/v1/validations/{id} ──────────────────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ValidationSession> get(@PathVariable UUID id) {
        return ResponseEntity.ok(validationService.get(id));
    }

    // ── PUT /api/v1/validations/{id}/extend ───────────────────────────────────

    @PutMapping("/{id}/extend")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ValidationSession> extend(
            @PathVariable UUID id,
            @Valid @RequestBody ExtendRequest req) {
        return ResponseEntity.ok(validationService.extendSession(id, req.extraMinutes()));
    }

    // ── PUT /api/v1/validations/{id}/cancel ───────────────────────────────────

    @PutMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ValidationSession> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(validationService.cancelSession(id));
    }

    // ── POST /api/v1/validations/public/{token} ───────────────────────────────

    @PostMapping("/public/{token}")
    public ResponseEntity<ValidationSession> createPublic(
            @PathVariable String token,
            @Valid @RequestBody PublicCreateRequest req) {
        ValidationSession session = validationService.createPublicSession(
                token, req.licensePlate());
        return ResponseEntity.status(HttpStatus.CREATED).body(session);
    }

    // ── Request records ───────────────────────────────────────────────────────

    public record CreateRequest(
            @NotNull(message = "zoneId is required") UUID zoneId,
            @NotBlank(message = "licensePlate is required")
            @Size(max = 10, message = "licensePlate max 10 chars")
            @Pattern(regexp = "[A-Za-z0-9]+", message = "licensePlate must be alphanumeric")
            String licensePlate,
            Integer durationMinutes,
            String endUserEmail,
            String endUserPhone
    ) {}

    public record ExtendRequest(
            @NotNull(message = "extraMinutes is required") Integer extraMinutes
    ) {}

    public record PublicCreateRequest(
            @NotBlank(message = "licensePlate is required")
            @Size(max = 10, message = "licensePlate max 10 chars")
            @Pattern(regexp = "[A-Za-z0-9]+", message = "licensePlate must be alphanumeric")
            String licensePlate
    ) {}
}
