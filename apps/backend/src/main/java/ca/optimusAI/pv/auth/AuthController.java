package ca.optimusAI.pv.auth;

import ca.optimusAI.pv.shared.exception.InvalidTokenException;
import ca.optimusAI.pv.shared.AuthTokens;
import ca.optimusAI.pv.shared.AuthUser;
import ca.optimusAI.pv.shared.TenantContext;
import ca.optimusAI.pv.shared.TenantInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Auth endpoints.
 *
 *  POST /api/auth/login    → AuthTokens   (OAuth2 password grant → TMS internal token)
 *  POST /api/auth/refresh  → AuthTokens   (rotate refresh token + re-sign internal token)
 *  POST /api/auth/logout   → 204          (revoke refresh token)
 *  GET  /api/auth/me       → AuthUser     (current user from TenantContext)
 *
 * Token-switching flow (mirrors old TmsAuthenticationSuccessHandler):
 *   1. Call OAuth server with username/password → get OAuth access + refresh tokens.
 *   2. Validate OAuth access token (RSA signature via OAuthAdapter).
 *   3. Extract user_name, email, firstName, lastName from OAuth JWT.
 *   4. Load/provision the user in our DB → get role, tenantId, clientId, subTenantId.
 *   5. Sign a NEW TMS-internal JWT (HMAC-SHA256) that embeds the full tenant context.
 *   6. Return the internal JWT to the client (the OAuth token stays server-side).
 *
 * Subsequent requests carry the internal JWT. JwtAuthenticationFilter validates
 * it and populates TenantContext directly from its claims — no DB round-trip.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final OAuthAdapter oAuthAdapter;
    private final InternalTokenService internalTokenService;
    private final RedisRefreshTokenStore tokenStore;
    private final UserRoleService userRoleService;

    // ── POST /api/auth/login ─────────────────────────────────────────────────
    /**
     * Token-switching login — two modes, same endpoint (mirrors old OauthTokenFilter +
     * TmsAuthenticationSuccessHandler flow):
     *
     * MODE 1 — OAuth Bearer token (old-code behaviour):
     *   Send:  Authorization: Bearer <oauth_rs256_token>   (no body needed)
     *   The endpoint validates the RS256 token, provisions the user, and returns
     *   a TMS-internal HS256 JWT — same as the old TmsAuthenticationSuccessHandler.
     *
     * MODE 2 — Username/password (new convenience flow):
     *   Send:  JSON body { "username": "...", "password": "..." }
     *   The endpoint calls the OAuth server on the client's behalf, then performs
     *   the same token-switch and returns a TMS-internal HS256 JWT + refresh token.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthTokens> login(
            HttpServletRequest httpRequest,
            @RequestBody(required = false) LoginRequest req) {

        // ── MODE 1: OAuth Bearer token passed directly ────────────────────────
        // Matches old OauthTokenFilter → TmsAuthenticationSuccessHandler behaviour.
        // User obtained an OAuth RS256 token from the OAuth server and passes it
        // as Authorization: Bearer header.
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String oauthToken = authHeader.substring(7);
            return ResponseEntity.ok(exchangeOAuthToken(oauthToken));
        }

        // ── MODE 2: Username + password in request body ───────────────────────
        if (req == null || req.username() == null || req.password() == null) {
            throw new InvalidTokenException(
                    "Provide either 'Authorization: Bearer <oauth_token>' header " +
                    "or JSON body {\"username\": \"...\", \"password\": \"...\"}");
        }

        // 1. Call OAuth server (password grant)
        OAuthTokenResponse oauthTokens = oAuthAdapter.login(req.username(), req.password());

        // 2 & 3. Validate OAuth token + extract claims
        JwtClaims oauthClaims = oAuthAdapter.validateAndExtractClaims(oauthTokens.accessToken());

        // 4. Load / provision user in our DB + upsert login record
        String displayName = buildName(oauthTokens.firstName(), oauthTokens.lastName());
        String loginProvider = oauthTokens.provider() != null ? oauthTokens.provider() : "oauth2";
        UserRecord user = userRoleService.loginAndLoad(
                loginProvider,
                oauthClaims.userId(),
                oauthTokens.email() != null ? oauthTokens.email() : oauthClaims.email(),
                displayName,
                oauthTokens.accessToken()
        );

        // 5. Sign TMS-internal JWT (token switch)
        InternalClaims internalClaims = new InternalClaims(
                oauthClaims.userId(),
                user.email(),
                user.role(),
                user.tenantId(),
                user.clientId(),
                user.subTenantId()
        );
        String internalToken = internalTokenService.sign(internalClaims);

        // 6. Store OAuth refresh token in Redis for later rotation
        if (oauthTokens.refreshToken() != null) {
            tokenStore.save(oauthClaims.userId(), oauthTokens.refreshToken());
        }

        log.debug("Token issued for userId={} role={}", oauthClaims.userId(), user.role());

        return ResponseEntity.ok(new AuthTokens(
                internalToken,
                oauthTokens.refreshToken(),
                oauthTokens.expiresIn()));
    }

    // ── POST /api/auth/token-exchange ────────────────────────────────────────
    /**
     * Convenience alias: accepts the OAuth token in the JSON body instead of
     * the Authorization header. Useful when a client cannot set custom headers.
     *
     * Internally delegates to the same logic as MODE 1 of /api/auth/login.
     */
    @PostMapping("/token-exchange")
    public ResponseEntity<AuthTokens> tokenExchange(@Valid @RequestBody TokenExchangeRequest req) {
        return ResponseEntity.ok(exchangeOAuthToken(req.oauthToken()));
    }

    // ── Shared token-switch logic ─────────────────────────────────────────────
    /**
     * Core of the old TmsAuthenticationSuccessHandler:
     *   validate OAuth token → upsert login row → load user → sign TMS-internal JWT.
     */
    private AuthTokens exchangeOAuthToken(String oauthToken) {
        // Validate OAuth RS256 token + extract claims
        JwtClaims oauthClaims = oAuthAdapter.validateAndExtractClaims(oauthToken);

        // Upsert login row + load/provision user
        UserRecord user = userRoleService.loginAndLoad(
                "oauth2",
                oauthClaims.userId(),
                oauthClaims.email(),
                oauthClaims.name(),
                oauthToken
        );

        // Sign TMS-internal JWT
        String internalToken = internalTokenService.sign(new InternalClaims(
                oauthClaims.userId(),
                user.email(),
                user.role(),
                user.tenantId(),
                user.clientId(),
                user.subTenantId()
        ));

        log.debug("Token-exchange for userId={} role={}", oauthClaims.userId(), user.role());

        // No refresh token — caller only supplied the access token
        return new AuthTokens(internalToken, null, 28800L);
    }


    /**
     * Refresh: rotate OAuth refresh token, then re-sign a new internal JWT
     * so any role/tenant changes made since last login are picked up.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthTokens> refresh(@Valid @RequestBody RefreshRequest req) {
        tokenStore.detectReuse(req.userId(), req.refreshToken());

        // Refresh OAuth tokens
        OAuthTokenResponse newOauthTokens = oAuthAdapter.refreshToken(req.refreshToken());

        // Re-load user to pick up any role/tenant changes
        UserRecord user = userRoleService.loadByAuth0Id(req.userId(), null, null);

        // Re-sign internal JWT
        InternalClaims internalClaims = new InternalClaims(
                req.userId(),
                user.email(),
                user.role(),
                user.tenantId(),
                user.clientId(),
                user.subTenantId()
        );
        String newInternalToken = internalTokenService.sign(internalClaims);

        String rotatedRefresh = newOauthTokens.refreshToken() != null
                ? newOauthTokens.refreshToken()
                : req.refreshToken();
        tokenStore.rotate(req.userId(), rotatedRefresh);

        return ResponseEntity.ok(new AuthTokens(
                newInternalToken,
                rotatedRefresh,
                newOauthTokens.expiresIn()));
    }

    // ── POST /api/auth/logout ────────────────────────────────────────────────
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> logout() {
        TenantInfo info = TenantContext.get();
        if (info != null) {
            tokenStore.load(info.userId()).ifPresent(refreshToken ->
                    oAuthAdapter.revokeToken(refreshToken));
            tokenStore.revoke(info.userId());
        }
        return ResponseEntity.noContent().build();
    }

    // ── GET /api/auth/me ─────────────────────────────────────────────────────
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AuthUser> me() {
        TenantInfo info = TenantContext.get();
        UserRecord record = userRoleService.loadByAuth0Id(info.userId(), info.email(), null);

        return ResponseEntity.ok(new AuthUser(
                info.userId(),
                record.email(),
                record.name(),
                info.tenantId(),
                info.clientId(),
                info.subTenantId(),
                info.roles(),
                null
        ));
    }

    // ── DTOs ──────────────────────────────────────────────────────────────────

    public record LoginRequest(
            @NotBlank(message = "username is required") String username,
            @NotBlank(message = "password is required") String password
    ) {}

    public record RefreshRequest(
            @NotBlank(message = "userId is required")       String userId,
            @NotBlank(message = "refreshToken is required") String refreshToken
    ) {}

    public record TokenExchangeRequest(
            @NotBlank(message = "oauthToken is required") String oauthToken
    ) {}

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String buildName(String firstName, String lastName) {
        if (firstName == null && lastName == null) return null;
        if (firstName == null) return lastName;
        if (lastName == null) return firstName;
        return firstName + " " + lastName;
    }
}
