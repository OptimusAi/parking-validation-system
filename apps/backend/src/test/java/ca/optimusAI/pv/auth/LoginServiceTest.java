package ca.optimusAI.pv.auth;

import ca.optimusAI.pv.auth.entity.Login;
import ca.optimusAI.pv.auth.repository.LoginRepository;
import ca.optimusAI.pv.tenant.repository.ClientAdminTenantRepository;
import ca.optimusAI.pv.user.entity.AppUser;
import ca.optimusAI.pv.user.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock LoginRepository loginRepository;
    @Mock AppUserRepository appUserRepository;
    @Mock UserProvisioner userProvisioner;

    @InjectMocks LoginService loginService;

    private AppUser buildUser(String role, boolean active) {
        AppUser u = new AppUser();
        u.setId(UUID.randomUUID());
        u.setAuthProviderUserId("auth0|test");
        u.setEmail("user@test.com");
        u.setName("Test User");
        u.setRole(role);
        u.setActive(active);
        return u;
    }

    @Test
    void givenFirstLogin_whenUpsert_thenProvisionUserAndCreateLogin() {
        when(loginRepository.findByProviderUserIdIgnoreCase("auth0|new")).thenReturn(Optional.empty());
        AppUser newUser = buildUser("USER", true);
        when(userProvisioner.findOrCreate(any(), any(), any())).thenReturn(newUser);
        Login saved = new Login();
        saved.setUser(newUser);
        when(loginRepository.save(any())).thenReturn(saved);

        AppUser result = loginService.upsertLogin("oauth2", "auth0|new", "user@test.com", "User", "gtoken");

        assertEquals("USER", result.getRole());
        verify(userProvisioner).findOrCreate(eq("auth0|new"), eq("user@test.com"), eq("User"));
        verify(loginRepository).save(any(Login.class));
    }

    @Test
    void givenReturningLogin_whenUpsert_thenReturnExistingUser() {
        AppUser existingUser = buildUser("TENANT_ADMIN", true);
        Login existingLogin = new Login();
        existingLogin.setUser(existingUser);
        existingLogin.setLastLoginDate(Instant.now().minusSeconds(3600));
        when(loginRepository.findByProviderUserIdIgnoreCase("auth0|existing")).thenReturn(Optional.of(existingLogin));
        when(loginRepository.save(any())).thenReturn(existingLogin);

        AppUser result = loginService.upsertLogin("oauth2", "auth0|existing", "user@test.com", "User", "gtoken");

        assertEquals("TENANT_ADMIN", result.getRole());
        verify(userProvisioner, never()).findOrCreate(any(), any(), any());
    }

    @Test
    void givenDisabledUser_whenUpsert_thenThrowException() {
        AppUser disabledUser = buildUser("USER", false);
        Login login = new Login();
        login.setUser(disabledUser);
        when(loginRepository.findByProviderUserIdIgnoreCase("auth0|disabled")).thenReturn(Optional.of(login));

        assertThrows(Exception.class, () ->
                loginService.upsertLogin("oauth2", "auth0|disabled", "d@test.com", "Disabled", "t"));
    }
}
