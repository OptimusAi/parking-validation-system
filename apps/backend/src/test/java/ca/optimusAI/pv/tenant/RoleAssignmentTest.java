package ca.optimusAI.pv.tenant;

import ca.optimusAI.pv.auth.UserRoleService;
import ca.optimusAI.pv.shared.TenantContext;
import ca.optimusAI.pv.shared.TenantInfo;
import ca.optimusAI.pv.shared.exception.UnauthorizedTenantAccessException;
import ca.optimusAI.pv.user.UserService;
import ca.optimusAI.pv.user.entity.AppUser;
import ca.optimusAI.pv.user.repository.AppUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Verifies role assignment restrictions:
 * - CLIENT_ADMIN cannot assign ADMIN or CLIENT_ADMIN role → 403
 * - ADMIN can assign any role
 */
@ExtendWith(MockitoExtension.class)
class RoleAssignmentTest {

    @Mock AppUserRepository appUserRepository;
    @Mock UserRoleService userRoleService;

    @InjectMocks UserService userService;

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    private void setContext(String role) {
        TenantContext.set(new TenantInfo(
                UUID.randomUUID(), UUID.randomUUID(), null,
                UUID.randomUUID().toString(), "admin@test.com",
                List.of(role), List.of()
        ));
    }

    private AppUser buildUser(String currentRole) {
        AppUser u = new AppUser();
        u.setId(UUID.randomUUID());
        u.setRole(currentRole);
        u.setActive(true);
        return u;
    }

    @Test
    void givenClientAdmin_whenAssignAdminRole_thenThrow403() {
        setContext("CLIENT_ADMIN");
        AppUser target = buildUser("USER");

        assertThrows(UnauthorizedTenantAccessException.class,
                () -> userService.assignRole(target.getId(), "ADMIN"));
    }

    @Test
    void givenClientAdmin_whenAssignClientAdminRole_thenThrow403() {
        setContext("CLIENT_ADMIN");
        AppUser target = buildUser("USER");

        assertThrows(UnauthorizedTenantAccessException.class,
                () -> userService.assignRole(target.getId(), "CLIENT_ADMIN"));
    }

    @Test
    void givenAdmin_whenAssignAnyRole_thenSucceeds() {
        setContext("ADMIN");
        AppUser target = buildUser("USER");
        UUID targetId = target.getId();
        when(appUserRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(appUserRepository.save(any())).thenReturn(target);

        for (String role : List.of("ADMIN", "CLIENT_ADMIN", "TENANT_ADMIN", "SUB_TENANT_ADMIN", "USER")) {
            target.setRole("USER"); // reset
            assertDoesNotThrow(() -> userService.assignRole(targetId, role),
                    "ADMIN should be able to assign role: " + role);
        }
    }

    @Test
    void givenClientAdmin_whenAssignTenantAdminRole_thenSucceeds() {
        setContext("CLIENT_ADMIN");
        AppUser target = buildUser("USER");
        when(appUserRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(appUserRepository.save(any())).thenReturn(target);

        assertDoesNotThrow(() -> userService.assignRole(target.getId(), "TENANT_ADMIN"));
    }
}
