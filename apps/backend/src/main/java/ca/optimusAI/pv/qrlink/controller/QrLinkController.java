package ca.optimusAI.pv.qrlink.controller;

import ca.optimusAI.pv.qrlink.QrLinkService;
import ca.optimusAI.pv.qrlink.QrLinkService.CreateLinkRequest;
import ca.optimusAI.pv.qrlink.entity.ValidationLink;
import ca.optimusAI.pv.qrlink.pdf.QrPdfGenerator;
import ca.optimusAI.pv.shared.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/links")
@RequiredArgsConstructor
public class QrLinkController {

    private final QrLinkService qrLinkService;
    private final QrPdfGenerator qrPdfGenerator;

    // ── POST /api/v1/links ────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN', 'SUBTENANT_USER')")
    public ResponseEntity<ValidationLink> create(@Valid @RequestBody CreateRequest req) {
        ValidationLink link = qrLinkService.createLink(
                new CreateLinkRequest(
                        req.zoneId(),
                        req.linkType(),
                        req.label(),
                        req.defaultDurationMinutes(),
                        req.expiresAt()));
        return ResponseEntity.status(HttpStatus.CREATED).body(link);
    }

    // ── GET /api/v1/links ─────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<ValidationLink>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(qrLinkService.list(page, size));
    }

    // ── GET /api/v1/links/{id} ────────────────────────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ValidationLink> get(@PathVariable UUID id) {
        return ResponseEntity.ok(qrLinkService.getById(id));
    }

    // ── GET /api/v1/links/by-token/{token} ────────────────────────────────────
    // Public — no auth required (used by QR scan page to resolve zone info)

    @GetMapping("/by-token/{token}")
    public ResponseEntity<ValidationLink> getByToken(@PathVariable String token) {
        return ResponseEntity.ok(qrLinkService.getByToken(token));
    }

    // ── PUT /api/v1/links/{id}/deactivate ─────────────────────────────────────

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<ValidationLink> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(qrLinkService.deactivate(id));
    }

    // ── GET /api/v1/links/{id}/qr-pdf ─────────────────────────────────────────

    @GetMapping(value = "/{id}/qr-pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> qrPdf(@PathVariable UUID id) {
        ValidationLink link = qrLinkService.getById(id);
        byte[] pdf = qrPdfGenerator.generate(link);

        String filename = "qr-link-" + id + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // ── Request record ────────────────────────────────────────────────────────

    public record CreateRequest(
            @NotNull(message = "zoneId is required") UUID zoneId,
            String linkType,
            String label,
            Integer defaultDurationMinutes,
            Instant expiresAt
    ) {}
}
