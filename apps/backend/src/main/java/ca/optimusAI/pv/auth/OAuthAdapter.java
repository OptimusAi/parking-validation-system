package ca.optimusAI.pv.auth;

import ca.optimusAI.pv.shared.exception.InvalidTokenException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

/**
 * OAuth 2.0 password-grant adapter.
 *
 * Authenticates users against the OAuth2 authorisation server at {@code oauth.host}
 * using the Resource Owner Password Credentials grant.
 *
 * JWT signatures are verified with the RSA public key loaded from
 * {@code oauth.jwt.public-key-path} (defaults to classpath:tms-jwt-pub.jks).
 */
@Slf4j
@Service
public class OAuthAdapter {

    @Value("${oauth.host:http://localhost:9090}")
    private String host;

    @Value("${oauth.client-id:cpa-tms-client}")
    private String clientId;

    @Value("${oauth.client-secret:cpat3n@ntm@n@g3m3nt5y5t3mcl13nts3cr3t}")
    private String clientSecret;

    @Value("${oauth.jwt.public-key-path:classpath:tms-jwt-pub.jks}")
    private String publicKeyPath;

    private final ApplicationContext applicationContext;
    private RSAPublicKey rsaPublicKey;

    private final RestClient restClient = RestClient.create();

    public OAuthAdapter(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    void loadPublicKey() {
        try {
            Resource resource = applicationContext.getResource(publicKeyPath);
            String pem = new String(resource.getInputStream().readAllBytes());
            // Strip PEM headers/footers and all whitespace
            String base64 = pem
                    .replaceAll("-----BEGIN PUBLIC KEY-----", "")
                    .replaceAll("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] decoded = Base64.getDecoder().decode(base64);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            rsaPublicKey = (RSAPublicKey) kf.generatePublic(new X509EncodedKeySpec(decoded));
            log.info("OAuth JWT public key loaded from: {}", publicKeyPath);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load OAuth JWT public key from: " + publicKeyPath, e);
        }
    }

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

            // Signature verification against the loaded RSA public key
            JWSVerifier verifier = new RSASSAVerifier(rsaPublicKey);
            if (!signed.verify(verifier)) {
                throw new InvalidTokenException("Invalid JWT signature");
            }

            JWTClaimsSet claims = signed.getJWTClaimsSet();

            // Expiry check
            Date expiry = claims.getExpirationTime();
            if (expiry != null && expiry.before(new Date())) {
                throw new InvalidTokenException("Token expired");
            }

            // userId — Spring OAuth2 stores the principal as "user_name"
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

