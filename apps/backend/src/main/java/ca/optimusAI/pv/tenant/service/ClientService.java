package ca.optimusAI.pv.tenant.service;

import ca.optimusAI.pv.shared.PageResponse;
import ca.optimusAI.pv.shared.TenantContext;
import ca.optimusAI.pv.shared.exception.ResourceNotFoundException;
import ca.optimusAI.pv.shared.exception.UnauthorizedTenantAccessException;
import ca.optimusAI.pv.tenant.entity.Client;
import ca.optimusAI.pv.tenant.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;

    @Transactional(readOnly = true)
    public PageResponse<Client> list(int page, int size) {
        PageRequest pr = PageRequest.of(page, size, Sort.by("createdAt").descending());

        if (TenantContext.hasRole("ADMIN")) {
            return PageResponse.of(clientRepository.findAllByIsDeletedFalse(pr));
        }

        // CLIENT_ADMIN — restrict to their own client only
        UUID clientId = TenantContext.clientId();
        if (clientId == null) {
            throw new UnauthorizedTenantAccessException("No client assigned to this user");
        }
        return PageResponse.of(clientRepository.findByIdAndIsDeletedFalse(clientId, pr));
    }

    @Transactional(readOnly = true)
    public Client get(UUID id) {
        Client client = clientRepository.findById(id)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + id));
        assertAccess(client);
        return client;
    }

    @Transactional
    public Client create(String name, String plan) {
        Client client = Client.builder()
                .name(name)
                .plan(plan != null ? plan : "STANDARD")
                .build();
        return clientRepository.save(client);
    }

    @Transactional
    public Client update(UUID id, String name, String plan) {
        Client client = get(id);
        if (name != null) client.setName(name);
        if (plan != null) client.setPlan(plan);
        return clientRepository.save(client);
    }

    @Transactional
    public void delete(UUID id) {
        Client client = get(id);
        client.setDeleted(true);
        clientRepository.save(client);
    }

    private void assertAccess(Client client) {
        if (TenantContext.hasRole("ADMIN")) return;
        UUID callerClientId = TenantContext.clientId();
        if (callerClientId == null || !callerClientId.equals(client.getId())) {
            throw new UnauthorizedTenantAccessException("Access denied to client: " + client.getId());
        }
    }
}
