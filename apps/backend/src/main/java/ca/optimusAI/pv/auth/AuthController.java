package ca.optimusAI.pv.auth;

import ca.optimusAI.pv.shared.AuthUser;
import ca.optimusAI.pv.shared.TenantContext;
import ca.optimusAI.pv.shared.TenantInfo;
import ca.optimusAI.pv.shared.exception.InvalidTokenException;
import ca.optimusAI.pv.tenant.repository.ClientAdminTenantRepository;
import ca.optimusAI.pv.user.entity.AppUser;
import ca.optimusAI.pv.user.repository.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Auth endpoints.
 *
 *  POST /api/auth/login          → LoginResponse   (OAuth token or user/pass → TMS HS256 JWT)
 *  POST /api/auth/token-exchange → LoginResponse   (OAuth token in body)
 *  POST /api/auth/logout         → 204             (stateless — client discards JWT)
 *  GET  /api/auth/me             → AuthUser        (current user from TenantContext)
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final OAuthAdapter                oAuthAdapter;
    private final TmsTokenService             tmsTokenService;
    private final LoginService                loginService;
    private final ClientAdminTenantRepository clientAdminTenantRepo;
    private final AppUserRepository           appUserRepository;
    private final PasswordEncoder             passwordEncoder;

    @Value("${oauth.local-auth-enabled:false}")
    private boolean localAuthEnabled;

    // ── POST /api/auth/login ─────────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            HttpServletRequest httpRequest,
            @RequestBody(required = false) LoginRequest req) {

        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return ResponseEntity.ok(processOAuthToken(authHeader.substring(7)));
        }

        if (req == null || req.username() == null || req.password() == null) {
            throw new InvalidTokenException(
                    "Provide 'Authorization: Bearer <oauth_token>' header or " +
                    "JSON body {\"username\": \"...\", \"password\": \"...\"}");
        }

        // ── Local dev auth (no OAuth server required) ────────────────────────
        if (localAuthEnabled) {
            return ResponseEntity.ok(processLocalCredentials(req.username(), req.password()));
        }

        // ── Production: OAuth2 password grant ───────────────────────────────
        OAuthTokenResponse oauthResp = oAuthAdapter.login(req.username(), req.password());
        return ResponseEntity.ok(processOAuthToken(oauthResp.accessToken()));
    }

    // ── POST /api/auth/token-exchange ────────────────────────────────────────
    @PostMapping("/token-exchange")
    public ResponseEntity<LoginResponse> tokenExchange(@Valid @RequestBody TokenExchangeRequest req) {
        return ResponseEntity.ok(processOAuthToken(req.oauthToken()));
    }

    // ── POST /api/auth/logout ────────────────────────────────────────────────
    /** Stateless logout — JWT has no server-side state. Frontend discards the token. */
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }

    // ── GET /api/auth/me ─────────────────────────────────────────────────────
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AuthUser> me() {
        TenantInfo info = TenantContext.get();
        return ResponseEntity.ok(new AuthUser(
                info.userId(),
                info.email(),
                null,
                info.tenantId(),
                info.clientId(),
                info.subTenantId(),
                info.roles(),
                info.assignedTenants()
        ));
    }

    // ── Core login logic ─────────────────────────────────────────────────────

    /**
     * Local dev auth: verify email + BCrypt password directly against app_user.
     * Never called in production (guarded by localAuthEnabled flag).
     */
    private LoginResponse processLocalCredentials(String email, String password) {
        AppUser user = appUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new InvalidTokenException("Invalid username or password"));

        if (user.getPasswordHash() == null ||
                !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new InvalidTokenException("Invalid username or password");
        }

        if (!user.isActive()) {
            throw new InvalidTokenException("Account is disabled");
        }

        log.debug("Local auth successful for email={} role={}", email, user.getRole());
        return buildLoginResponse(user);
    }

    private LoginResponse processOAuthToken(String oauthToken) {
        JwtClaims oauthClaims = oAuthAdapter.validateAndExtractClaims(oauthToken);

        AppUser user = loginService.upsertLogin(
                "oauth2",
                oauthClaims.userId(),
                oauthClaims.email(),
                oauthClaims.name(),
                oauthToken
        );

        if (!user.isActive()) {
            throw new InvalidTokenException("Account is disabled");
        }

        return buildLoginResponse(user);
    }

    private LoginResponse buildLoginResponse(AppUser user) {
        List<UUID> assignedTenants = "CLIENT_ADMIN".equals(user.getRole())
                ? clientAdminTenantRepo.findTenantIdsByUserId(user.getId())
                : List.of();

        TmsTokenClaims tokenClaims = new TmsTokenClaims(
                user.getAuthProviderUserId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.getTenantId(),
                user.getClientId(),
                user.getSubTenantId(),
                assignedTenants
        );
        String tmsToken = tmsTokenService.sign(tokenClaims);

        log.debug("TMS token issued for userId={} role={}", user.getId(), user.getRole());

        return new LoginResponse(
                tmsToken,
                user.getRole(),
                user.getTenantId(),
                user.getClientId(),
                user.getSubTenantId(),
                assignedTenants,
                user.getEmail(),
                user.getName(),
                "Login successful"
        );
    }

    // ── Request DTOs ─────────────────────────────────────────────────────────

    public record LoginRequest(
            @NotBlank(message = "username is required") String username,
            @NotBlank(message = "password is required") String password
    ) {}

    public record TokenExchangeRequest(
            @NotBlank(message = "oauthToken is required") String oauthToken
    ) {}
}
