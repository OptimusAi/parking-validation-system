package ca.optimusAI.pv.user;

import ca.optimusAI.pv.auth.UserRoleService;
import ca.optimusAI.pv.shared.PageResponse;
import ca.optimusAI.pv.shared.TenantContext;
import ca.optimusAI.pv.shared.exception.ResourceNotFoundException;
import ca.optimusAI.pv.shared.exception.UnauthorizedTenantAccessException;
import ca.optimusAI.pv.user.entity.User;
import ca.optimusAI.pv.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserRoleService userRoleService;

    /**
     * List users.
     * ADMIN: all users across all clients.
     * CLIENT_ADMIN: only users belonging to their clientId.
     */
    @Transactional(readOnly = true)
    public PageResponse<User> list(int page, int size) {
        PageRequest pr = PageRequest.of(page, size, Sort.by("createdAt").descending());

        if (TenantContext.hasRole("ADMIN")) {
            return PageResponse.of(userRepository.findAll(pr));
        }

        UUID clientId = TenantContext.clientId();
        if (clientId == null) {
            throw new UnauthorizedTenantAccessException("No client assigned to current user");
        }
        return PageResponse.of(userRepository.findByClientId(clientId, pr));
    }

    /**
     * Return the currently authenticated user, resolved from TenantContext.
     */
    @Transactional(readOnly = true)
    public User getMe() {
        String auth0UserId = TenantContext.userId();
        if (auth0UserId == null) {
            throw new UnauthorizedTenantAccessException("Not authenticated");
        }
        return userRepository.findByAuth0UserId(auth0UserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for current session"));
    }

    /**
     * Assign a role to a user.
     * After saving, the Redis cache is evicted so the next request picks up the new role immediately.
     */
    @Transactional
    public User assignRole(UUID id, String role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        user.setRole(role);
        User saved = userRepository.save(user);
        userRoleService.evictCache(user.getAuth0UserId());
        log.info("Assigned role={} to userId={} auth0UserId={}", role, id, user.getAuth0UserId());
        return saved;
    }

    /**
     * Assign a tenant + client to a user (ADMIN only).
     * Cache is evicted so the next token validation picks up the updated tenant context.
     */
    @Transactional
    public User assignTenant(UUID id, UUID tenantId, UUID clientId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        user.setTenantId(tenantId);
        user.setClientId(clientId);
        User saved = userRepository.save(user);
        userRoleService.evictCache(user.getAuth0UserId());
        log.info("Assigned tenantId={} clientId={} to userId={}", tenantId, clientId, id);
        return saved;
    }
}
