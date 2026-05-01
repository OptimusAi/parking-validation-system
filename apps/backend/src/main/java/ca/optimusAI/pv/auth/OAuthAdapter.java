package ca.optimusAI.pv.auth;

import ca.optimusAI.pv.shared.exception.InvalidTokenException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Date;
import java.util.List;

/**
 * OAuth 2.0 adapter.
 *
 * Verifies incoming OAuth RS256 Bearer tokens by fetching the public key from the
 * OAuth server's JWKS endpoint ({@code oauth.jwks-uri}).
 *
 * In local dev ({@code oauth.local-auth-enabled=true}), if the JWKS endpoint is
 * unreachable or not configured, signature verification is skipped so that test
 * tokens can be used without a running OAuth server.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthAdapter {

    @Value("${oauth.host:http://localhost:9090}")
    private String host;

    @Value("${oauth.client-id:cpa-tms-client}")
    private String clientId;

    @Value("${oauth.client-secret:cpat3n@ntm@n@g3m3nt5y5t3mcl13nts3cr3t}")
    private String clientSecret;

    /** JWKS URI for RS256 token verification. Defaults to Spring Auth Server standard path. */
    @Value("${oauth.jwks-uri:}")
    private String jwksUri;

    /** When true, a failed JWKS fetch degrades to unverified JWT parsing (local dev only). */
    @Value("${oauth.local-auth-enabled:false}")
    private boolean localAuthEnabled;

    private final JWKSCache jwksCache;
    private final RestClient restClient = RestClient.create();

    // ── Login (password grant) ────────────────────────────────────────────────

    /**
     * Authenticate with username + password against the OAuth2 server.
     */
    public OAuthTokenResponse login(String username, String password) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("username", username);
        body.add("password", password);
        body.add("scope", "read write");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);

        try {
            return restClient.post()
                    .uri(host + "/oauth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(OAuthTokenResponse.class);
        } catch (Exception e) {
            log.warn("OAuth login failed: {}", e.getMessage());
            throw new InvalidTokenException("Authentication failed — invalid credentials");
        }
    }

    // ── Token refresh ─────────────────────────────────────────────────────────

    /**
     * Exchange a refresh token for a new access token.
     */
    public OAuthTokenResponse refreshToken(String refreshToken) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("refresh_token", refreshToken);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);

        try {
            return restClient.post()
                    .uri(host + "/oauth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(OAuthTokenResponse.class);
        } catch (Exception e) {
            log.warn("OAuth refresh failed: {}", e.getMessage());
            throw new InvalidTokenException("Token refresh failed — please log in again");
        }
    }

    // ── JWT claim extraction ──────────────────────────────────────────────────

    /**
     * Verify the JWT signature using the loaded RSA public key, validate expiry,
     * and return structured claims.
     *
     * Expected JWT claims (Spring OAuth2 password grant):
     *   user_name   → UUID identifying the resource owner
     *   email       → resource owner e-mail (if present)
     *   firstName   → first name (if present)
     *   lastName    → last name (if present)
     *   authorities → list of granted authorities
     */
    public JwtClaims validateAndExtractClaims(String token) {
        try {
            SignedJWT signed = SignedJWT.parse(token);

            verifySignature(signed);

            JWTClaimsSet claims = signed.getJWTClaimsSet();

            // Expiry check
            Date expiry = claims.getExpirationTime();
            if (expiry != null && expiry.before(new Date())) {
                throw new InvalidTokenException("Token expired");
            }

            // userId — Spring OAuth2 stores the principal as "user_name"; OIDC uses "sub"
            String userId = claims.getStringClaim("user_name");
            if (userId == null) userId = claims.getSubject();
            if (userId == null) throw new InvalidTokenException("Missing user_name / sub claim");

            String email = claims.getStringClaim("email");

            String firstName = claims.getStringClaim("firstName");
            String lastName  = claims.getStringClaim("lastName");
            String name = buildName(firstName, lastName);

            return new JwtClaims(userId, email, name);

        } catch (InvalidTokenException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidTokenException("Token validation failed: " + e.getMessage());
        }
    }

    /**
     * Verify the JWT signature using the JWKS endpoint.
     * In local dev mode, if the JWKS endpoint is unreachable or unconfigured,
     * signature verification is skipped with a warning (allows testing without
     * a running OAuth server).
     */
    private void verifySignature(SignedJWT signed) {
        String effectiveJwksUri = resolveJwksUri();

        if (effectiveJwksUri == null || effectiveJwksUri.isBlank()) {
            if (localAuthEnabled) {
                log.warn("No JWKS URI configured — skipping OAuth token signature verification (local dev mode)");
                return;
            }
            throw new InvalidTokenException("OAuth JWT verification not configured — set oauth.jwks-uri");
        }

        try {
            JWKSet jwkSet = jwksCache.get(effectiveJwksUri);
            String kid = signed.getHeader().getKeyID();

            RSAKey rsaKey = null;
            if (kid != null) {
                rsaKey = (RSAKey) jwkSet.getKeyByKeyId(kid);
            }
            // Fall back to first RSA key if no kid match
            if (rsaKey == null) {
                rsaKey = (RSAKey) jwkSet.getKeys().stream()
                        .filter(k -> k instanceof RSAKey)
                        .findFirst()
                        .orElse(null);
            }
            if (rsaKey == null) {
                throw new InvalidTokenException("No RSA key found in JWKS at: " + effectiveJwksUri);
            }

            JWSVerifier verifier = new RSASSAVerifier(rsaKey.toRSAPublicKey());
            if (!signed.verify(verifier)) {
                throw new InvalidTokenException("Invalid JWT signature");
            }
        } catch (InvalidTokenException e) {
            throw e;
        } catch (Exception e) {
            if (localAuthEnabled) {
                log.warn("JWKS verification failed ({}) — proceeding without signature check (local dev mode)",
                         e.getMessage());
            } else {
                throw new InvalidTokenException("JWT signature verification failed: " + e.getMessage());
            }
        }
    }

    /** Resolve JWKS URI: explicit config takes precedence, then derive from oauth.host. */
    private String resolveJwksUri() {
        if (jwksUri != null && !jwksUri.isBlank()) {
            return jwksUri;
        }
        // Derive from host — Spring Authorization Server standard path
        if (host != null && !host.isBlank()) {
            return host + "/.well-known/jwks.json";
        }
        return null;
    }

    // ── Token revocation ─────────────────────────────────────────────────────

    /**
     * Best-effort token revocation. Failures are swallowed — revocation must not block logout.
     */
    public void revokeToken(String token) {
        try {
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("token", token);
            body.add("token_type_hint", "refresh_token");
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);

            restClient.post()
                    .uri(host + "/oauth/revoke")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ignored) {
            // Best-effort; do not fail logout on server errors
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String buildName(String firstName, String lastName) {
        if (firstName == null && lastName == null) return null;
        if (firstName == null) return lastName;
        if (lastName == null) return firstName;
        return firstName + " " + lastName;
    }
}

