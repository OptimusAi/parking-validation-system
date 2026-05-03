package ca.optimusAI.pv.user;

import ca.optimusAI.pv.auth.UserRoleService;
import ca.optimusAI.pv.shared.PageResponse;
import ca.optimusAI.pv.shared.TenantContext;
import ca.optimusAI.pv.shared.exception.ResourceNotFoundException;
import ca.optimusAI.pv.shared.exception.UnauthorizedTenantAccessException;
import ca.optimusAI.pv.tenant.entity.ClientAdminAssignment;
import ca.optimusAI.pv.tenant.entity.SubTenantAdminAssignment;
import ca.optimusAI.pv.tenant.entity.TenantAdminAssignment;
import ca.optimusAI.pv.tenant.repository.ClientAdminAssignmentRepository;
import ca.optimusAI.pv.tenant.repository.SubTenantAdminAssignmentRepository;
import ca.optimusAI.pv.tenant.repository.TenantAdminAssignmentRepository;
import ca.optimusAI.pv.user.entity.AppUser;
import ca.optimusAI.pv.user.entity.UserRole;
import ca.optimusAI.pv.user.repository.AppUserRepository;
import ca.optimusAI.pv.user.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final AppUserRepository                 userRepository;
    private final UserRoleRepository                 userRoleRepository;
    private final UserRoleService                    userRoleService;
    private final ClientAdminAssignmentRepository    clientAdminAssignmentRepository;
    private final TenantAdminAssignmentRepository    tenantAdminAssignmentRepository;
    private final SubTenantAdminAssignmentRepository subTenantAdminAssignmentRepository;

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
            if (assignedTenants.isEmpty()) {
                throw new UnauthorizedTenantAccessException("No tenants assigned to this CLIENT_ADMIN");
            }
            List<UUID> userIds = clientAdminAssignmentRepository.findUserIdsByTenantIdIn(assignedTenants);
            return PageResponse.of(userRepository.findByIdIn(userIds, pr));
        }

        throw new UnauthorizedTenantAccessException("Insufficient role to list users");
    }

    /**
     * Enriched list — same scoping as list() but returns UserResponse with role + tenant info.
     */
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listEnriched(int page, int size) {
        PageRequest pr = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<AppUser> userPage;
        if (TenantContext.hasRole("ADMIN")) {
            userPage = userRepository.findAll(pr);
        } else if (TenantContext.hasRole("CLIENT_ADMIN")) {
            List<UUID> assignedTenants = TenantContext.assignedTenants();
            if (assignedTenants.isEmpty()) {
                throw new UnauthorizedTenantAccessException("No tenants assigned to this CLIENT_ADMIN");
            }
            List<UUID> userIds = clientAdminAssignmentRepository.findUserIdsByTenantIdIn(assignedTenants);
            userPage = userRepository.findByIdIn(userIds, pr);
        } else {
            throw new UnauthorizedTenantAccessException("Insufficient role to list users");
        }

        List<UUID> userIds = userPage.stream().map(AppUser::getId).toList();

        // Bulk-fetch roles
        Map<UUID, UserRole> roleMap = userRoleRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(UserRole::getUserId, Function.identity()));

        // Bulk-fetch client-admin assignments (for clientId + tenantId)
        Map<UUID, ClientAdminAssignment> clientAssignMap =
                clientAdminAssignmentRepository.findAllByUserIdIn(userIds).stream()
                        .collect(Collectors.toMap(ClientAdminAssignment::getUserId, Function.identity(), (a, b) -> a));

        // Bulk-fetch tenant-admin assignments
        Map<UUID, TenantAdminAssignment> tenantAssignMap =
                tenantAdminAssignmentRepository.findAllByUserIdIn(userIds).stream()
                        .collect(Collectors.toMap(TenantAdminAssignment::getUserId, Function.identity(), (a, b) -> a));

        List<UserResponse> content = userPage.stream().map(u -> {
            UserRole ur = roleMap.get(u.getId());
            String role = ur != null ? ur.getRole() : "USER";
            UUID tenantId = null;
            UUID clientId = null;
            if ("CLIENT_ADMIN".equals(role)) {
                ClientAdminAssignment ca = clientAssignMap.get(u.getId());
                if (ca != null) { tenantId = ca.getTenantId(); clientId = ca.getClientId(); }
            } else if ("TENANT_ADMIN".equals(role)) {
                TenantAdminAssignment ta = tenantAssignMap.get(u.getId());
                if (ta != null) tenantId = ta.getTenantId();
            }
            return new UserResponse(u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(),
                    u.getFullName(), u.isActive(), role, tenantId, clientId, u.getCreatedAt());
        }).toList();

        return new PageResponse<>(content, userPage.getNumber(), userPage.getSize(),
                userPage.getTotalElements(), userPage.getTotalPages(), userPage.isLast());
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

        UserRole userRole = userRoleRepository.findByUserId(id).orElse(null);
        String role = userRole != null ? userRole.getRole() : "USER";

        switch (role) {
            case "CLIENT_ADMIN" -> {
                if (clientId != null && tenantId != null) {
                    clientAdminAssignmentRepository.findByUserIdAndTenantId(id, tenantId).ifPresentOrElse(
                        existing -> {},
                        () -> clientAdminAssignmentRepository.save(
                                ClientAdminAssignment.builder()
                                        .userId(id).clientId(clientId).tenantId(tenantId).build())
                    );
                }
            }
            case "TENANT_ADMIN" -> {
                if (tenantId != null) {
                    TenantAdminAssignment a = tenantAdminAssignmentRepository.findByUserId(id)
                            .orElseGet(() -> TenantAdminAssignment.builder().userId(id).build());
                    a.setTenantId(tenantId);
                    tenantAdminAssignmentRepository.save(a);
                }
            }
            case "SUB_TENANT_ADMIN" -> {
                if (tenantId != null && subTenantId != null) {
                    SubTenantAdminAssignment a = subTenantAdminAssignmentRepository.findByUserId(id)
                            .orElseGet(() -> SubTenantAdminAssignment.builder().userId(id).build());
                    a.setTenantId(tenantId);
                    a.setSubTenantId(subTenantId);
                    subTenantAdminAssignmentRepository.save(a);
                }
            }
            default -> log.warn("assignTenant called for role={} — no assignment table for this role", role);
        }

        userRoleService.evictCache(user.getAuthProviderUserId());
        log.info("Assigned scope tenantId={} clientId={} subTenantId={} to userId={} role={}",
                tenantId, clientId, subTenantId, id, role);
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

    /** Activate a user. */
    @Transactional
    public AppUser activate(UUID id) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        user.setActive(true);
        AppUser saved = userRepository.save(user);
        userRoleService.evictCache(user.getAuthProviderUserId());
        log.info("Activated userId={}", id);
        return saved;
    }

    /** Get user by id. */
    @Transactional(readOnly = true)
    public AppUser getById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }
}
