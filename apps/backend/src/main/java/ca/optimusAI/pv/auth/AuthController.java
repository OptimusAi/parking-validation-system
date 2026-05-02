package ca.optimusAI.pv.auth;

import ca.optimusAI.pv.shared.AuthUser;
import ca.optimusAI.pv.shared.TenantContext;
import ca.optimusAI.pv.shared.TenantInfo;
import ca.optimusAI.pv.shared.exception.InvalidTokenException;
import ca.optimusAI.pv.tenant.repository.ClientAdminAssignmentRepository;
import ca.optimusAI.pv.tenant.repository.TenantAdminAssignmentRepository;
import ca.optimusAI.pv.tenant.repository.SubTenantAdminAssignmentRepository;
import ca.optimusAI.pv.user.entity.AppUser;
import ca.optimusAI.pv.user.entity.UserRole;
import ca.optimusAI.pv.user.repository.AppUserRepository;
import ca.optimusAI.pv.user.repository.UserRoleRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final ClientAdminAssignmentRepository  clientAdminAssignmentRepo;
    private final TenantAdminAssignmentRepository   tenantAdminAssignmentRepo;
    private final SubTenantAdminAssignmentRepository subTenantAdminAssignmentRepo;
    private final AppUserRepository                 appUserRepository;
    private final UserRoleRepository                userRoleRepository;

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

    private LoginResponse processOAuthToken(String oauthToken) {
        JwtClaims oauthClaims = oAuthAdapter.validateAndExtractClaims(oauthToken);

        AppUser user = loginService.upsertLogin(
                "oauth2",
                oauthClaims.userId(),
                oauthClaims.email(),
                oauthClaims.firstName(),
                oauthClaims.lastName()
        );

        if (!user.isActive()) {
            throw new InvalidTokenException("Account is disabled");
        }

        UserRole userRole = userRoleRepository.findByUserId(user.getId()).orElse(null);
        return buildLoginResponse(user, userRole);
    }

    private LoginResponse buildLoginResponse(AppUser user, UserRole userRole) {
        String role = userRole != null ? userRole.getRole() : "USER";

        // Each role loads scope from its dedicated assignment table
        UUID clientId = "CLIENT_ADMIN".equals(role)
                ? clientAdminAssignmentRepo.findClientIdByUserId(user.getId()).orElse(null)
                : null;

        UUID tenantId = switch (role) {
            case "TENANT_ADMIN"     -> tenantAdminAssignmentRepo.findTenantIdByUserId(user.getId()).orElse(null);
            case "SUB_TENANT_ADMIN" -> subTenantAdminAssignmentRepo.findTenantIdByUserId(user.getId()).orElse(null);
            default                 -> null;
        };

        UUID subTenantId = "SUB_TENANT_ADMIN".equals(role)
                ? subTenantAdminAssignmentRepo.findSubTenantIdByUserId(user.getId()).orElse(null)
                : null;

        List<UUID> assignedTenants = "CLIENT_ADMIN".equals(role)
                ? clientAdminAssignmentRepo.findTenantIdsByUserId(user.getId())
                : List.of();

        TmsTokenClaims tokenClaims = new TmsTokenClaims(
                user.getAuthProviderUserId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                role,
                tenantId,
                clientId,
                subTenantId,
                assignedTenants
        );
        String tmsToken = tmsTokenService.sign(tokenClaims);

        log.debug("TMS token issued for userId={} role={}", user.getId(), role);

        return new LoginResponse(
                tmsToken,
                role,
                tenantId,
                clientId,
                subTenantId,
                assignedTenants,
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
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
