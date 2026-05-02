package ca.optimusAI.pv.user;

import ca.optimusAI.pv.auth.UserRoleService;
import ca.optimusAI.pv.shared.PageResponse;
import ca.optimusAI.pv.shared.TenantContext;
import ca.optimusAI.pv.shared.exception.ResourceNotFoundException;
import ca.optimusAI.pv.shared.exception.UnauthorizedTenantAccessException;
import ca.optimusAI.pv.user.entity.AppUser;
import ca.optimusAI.pv.user.entity.UserRole;
import ca.optimusAI.pv.user.repository.AppUserRepository;
import ca.optimusAI.pv.user.repository.UserRoleRepository;
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

    private final AppUserRepository  userRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserRoleService    userRoleService;

    private static final List<String> VALID_ROLES =
            List.of("ADMIN", "CLIENT_ADMIN", "TENANT_ADMIN", "SUB_TENANT_ADMIN", "USER");

    /**
     * List users scoped by caller role.
     * ADMIN: all users.
     * CLIENT_ADMIN: users whose user_role.client_id or user_role.tenant_id matches.
     */
    @Transactional(readOnly = true)
    public PageResponse<AppUser> list(int page, int size) {
        PageRequest pr = PageRequest.of(page, size, Sort.by("createdAt").descending());

        if (TenantContext.hasRole("ADMIN")) {
            return PageResponse.of(userRepository.findAll(pr));
        }

        if (TenantContext.hasRole("CLIENT_ADMIN")) {
            List<UUID> assignedTenants = TenantContext.assignedTenants();
            if (!assignedTenants.isEmpty()) {
                List<UUID> userIds = userRoleRepository.findUserIdsByTenantIdIn(assignedTenants);
                return PageResponse.of(userRepository.findByIdIn(userIds, pr));
            }
            UUID clientId = TenantContext.clientId();
            if (clientId == null) throw new UnauthorizedTenantAccessException("No client assigned");
            List<UUID> userIds = userRoleRepository.findUserIdsByClientId(clientId);
            return PageResponse.of(userRepository.findByIdIn(userIds, pr));
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
     * Assign a role to a user (upserts user_role row).
     * CLIENT_ADMIN cannot assign ADMIN or CLIENT_ADMIN roles.
     */
    @Transactional
    public AppUser assignRole(UUID id, String role) {
        if (!VALID_ROLES.contains(role)) {
            throw new UnauthorizedTenantAccessException("Invalid role: " + role);
        }
        if (TenantContext.hasRole("CLIENT_ADMIN")) {
            if ("ADMIN".equals(role) || "CLIENT_ADMIN".equals(role)) {
                throw new UnauthorizedTenantAccessException(
                        "CLIENT_ADMIN cannot assign ADMIN or CLIENT_ADMIN roles");
            }
        }

        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));

        UserRole userRole = userRoleRepository.findByUserId(id)
                .orElseGet(() -> UserRole.builder().userId(id).build());
        userRole.setRole(role);
        userRoleRepository.save(userRole);

        userRoleService.evictCache(user.getAuthProviderUserId());
        log.info("Assigned role={} to userId={}", role, id);
        return user;
    }

    /**
     * Assign tenant + client (+ optional sub-tenant) to a user. ADMIN only.
     */
    @Transactional
    public AppUser assignTenant(UUID id, UUID tenantId, UUID clientId, UUID subTenantId) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));

        UserRole userRole = userRoleRepository.findByUserId(id)
                .orElseGet(() -> UserRole.builder().userId(id).role("USER").build());
        userRole.setTenantId(tenantId);
        userRole.setClientId(clientId);
        userRole.setSubTenantId(subTenantId);
        userRoleRepository.save(userRole);

        userRoleService.evictCache(user.getAuthProviderUserId());
        log.info("Assigned tenantId={} clientId={} subTenantId={} to userId={}", tenantId, clientId, subTenantId, id);
        return user;
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
