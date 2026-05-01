package ca.optimusAI.tms.tenant.controller;

import ca.optimusAI.tms.shared.PageResponse;
import ca.optimusAI.tms.tenant.entity.Client;
import ca.optimusAI.tms.tenant.service.ClientService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT_ADMIN')")
    public ResponseEntity<PageResponse<Client>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(clientService.list(page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT_ADMIN')")
    public ResponseEntity<Client> get(@PathVariable UUID id) {
        return ResponseEntity.ok(clientService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT_ADMIN')")
    public ResponseEntity<Client> create(@Valid @RequestBody CreateClientRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(clientService.create(req.name(), req.plan()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT_ADMIN')")
    public ResponseEntity<Client> update(@PathVariable UUID id,
                                         @Valid @RequestBody UpdateClientRequest req) {
        return ResponseEntity.ok(clientService.update(id, req.name(), req.plan()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        clientService.delete(id);
        return ResponseEntity.noContent().build();
    }

    public record CreateClientRequest(
            @NotBlank(message = "name is required") String name,
            String plan
    ) {}

    public record UpdateClientRequest(String name, String plan) {}
}
