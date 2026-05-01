package ca.optimusAI.pv.user;

import ca.optimusAI.pv.auth.UserRoleService;
import ca.optimusAI.pv.shared.PageResponse;
import ca.optimusAI.pv.shared.TenantContext;
import ca.optimusAI.pv.shared.exception.ResourceNotFoundException;
import ca.optimusAI.pv.shared.exception.UnauthorizedTenantAccessException;
import ca.optimusAI.pv.user.entity.AppUser;
import ca.optimusAI.pv.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final AppUserRepository userRepository;
    private final UserRoleService userRoleService;

    private static final List<String> VALID_ROLES =
            List.of("ADMIN", "CLIENT_ADMIN", "TENANT_ADMIN", "SUB_TENANT_ADMIN", "USER");

    /**
     * List users scoped by role.
     * ADMIN: all users.
     * CLIENT_ADMIN: users where tenant_id in assignedTenants.
     * Others: 403.
     */
    @Transactional(readOnly = true)
    public PageResponse<AppUser> list(int page, int size) {
        PageRequest pr = PageRequest.of(page, size, Sort.by("createdAt").descending());

        if (TenantContext.hasRole("ADMIN")) {
            return PageResponse.of(userRepository.findAll(pr));
        }

        if (TenantContext.hasRole("CLIENT_ADMIN")) {
            List<UUID> assignedTenants = TenantContext.assignedTenants();
            if (assignedTenants.isEmpty()) {
                // Fall back to clientId-based filter
                UUID clientId = TenantContext.clientId();
                if (clientId == null) throw new UnauthorizedTenantAccessException("No client assigned");
                return PageResponse.of(userRepository.findByClientId(clientId, pr));
            }
            return PageResponse.of(userRepository.findByTenantIdIn(assignedTenants, pr));
        }

        throw new UnauthorizedTenantAccessException("Insufficient role to list users");
    }

    /** Return the currently authenticated user from TenantContext. */
    @Transactional(readOnly = true)
    public AppUser getMe() {
        String authProviderUserId = TenantContext.userId();
        if (authProviderUserId == null) {
            throw new UnauthorizedTenantAccessException("Not authenticated");
        }
        return userRepository.findByAuthProviderUserId(authProviderUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for current session"));
    }

    /**
     * Assign a role to a user.
     * CLIENT_ADMIN cannot assign ADMIN or CLIENT_ADMIN roles.
     */
    @Transactional
    public AppUser assignRole(UUID id, String role) {
        if (!VALID_ROLES.contains(role)) {
            throw new UnauthorizedTenantAccessException("Invalid role: " + role);
        }

        // CLIENT_ADMIN restriction
        if (TenantContext.hasRole("CLIENT_ADMIN")) {
            if ("ADMIN".equals(role) || "CLIENT_ADMIN".equals(role)) {
                throw new UnauthorizedTenantAccessException(
                        "CLIENT_ADMIN cannot assign ADMIN or CLIENT_ADMIN roles");
            }
        }

        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        user.setRole(role);
        AppUser saved = userRepository.save(user);
        userRoleService.evictCache(user.getAuthProviderUserId());
        log.info("Assigned role={} to userId={}", role, id);
        return saved;
    }

    /**
     * Assign tenant + client (+ optional sub-tenant) to a user. ADMIN only.
     */
    @Transactional
    public AppUser assignTenant(UUID id, UUID tenantId, UUID clientId, UUID subTenantId) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        user.setTenantId(tenantId);
        user.setClientId(clientId);
        user.setSubTenantId(subTenantId);
        AppUser saved = userRepository.save(user);
        userRoleService.evictCache(user.getAuthProviderUserId());
        log.info("Assigned tenantId={} clientId={} subTenantId={} to userId={}", tenantId, clientId, subTenantId, id);
        return saved;
    }

    /** Deactivate a user. */
    @Transactional
    public AppUser deactivate(UUID id) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        user.setActive(false);
        AppUser saved = userRepository.save(user);
        userRoleService.evictCache(user.getAuthProviderUserId());
        log.info("Deactivated userId={}", id);
        return saved;
    }

    /** Get user by id. */
    @Transactional(readOnly = true)
    public AppUser getById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }
}
