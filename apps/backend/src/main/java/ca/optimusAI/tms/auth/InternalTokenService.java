package ca.optimusAI.tms.auth;

import ca.optimusAI.tms.shared.exception.InvalidTokenException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * Issues and validates TMS-internal JWTs (HMAC-SHA256).
 *
 * Flow (mirrors old TmsAuthenticationSuccessHandler):
 *   1. OAuth server token is verified by OAuthAdapter (RSA public key).
 *   2. User is loaded/provisioned from DB (role, tenantId, etc.).
 *   3. THIS service signs a new short-lived internal JWT containing all tenant context.
 *   4. JwtAuthenticationFilter validates this internal token on every request and
 *      populates TenantContext directly from its claims — no DB/Redis hit needed.
 *
 * Claims in the internal token:
 *   sub         → userId (UUID in DB)
 *   email       → user email
 *   role        → ADMIN | CLIENT_ADMIN | TENANT_ADMIN | SUBTENANT_USER | VIEWER
 *   tenantId    → UUID (may be absent for unassigned users)
 *   clientId    → UUID
 *   subTenantId → UUID
 */
@Slf4j
@Service
public class InternalTokenService {

    @Value("${internal.jwt.secret:change-me-this-tms-secret-must-be-at-least-32-chars}")
    private String secret;

    @Value("${internal.jwt.expiry-hours:8}")
    private int expiryHours;

    private byte[] secretBytes;

    @PostConstruct
    void init() {
        secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                    "internal.jwt.secret must be at least 32 characters long");
        }
        log.info("Internal JWT service initialised (expiry={}h, HMAC-SHA256)", expiryHours);
    }

    // ── Sign ──────────────────────────────────────────────────────────────────

    /**
     * Sign a new internal TMS JWT containing the user's full tenant context.
     */
    public String sign(InternalClaims claims) {
        try {
            long now = System.currentTimeMillis();
            long exp = now + (long) expiryHours * 3_600_000L;

            JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                    .subject(claims.userId())
                    .issueTime(new Date(now))
                    .expirationTime(new Date(exp))
                    .claim("email",  claims.email())
                    .claim("role",   claims.role());

            if (claims.tenantId()    != null) builder.claim("tenantId",    claims.tenantId().toString());
            if (claims.clientId()    != null) builder.claim("clientId",    claims.clientId().toString());
            if (claims.subTenantId() != null) builder.claim("subTenantId", claims.subTenantId().toString());

            JWSSigner signer = new MACSigner(secretBytes);
            SignedJWT jwt    = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), builder.build());
            jwt.sign(signer);

            return jwt.serialize();

        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign internal JWT: " + e.getMessage(), e);
        }
    }

    // ── Verify ────────────────────────────────────────────────────────────────

    /**
     * Verify the HMAC signature, check expiry, and extract claims.
     *
     * @throws InvalidTokenException on any validation failure
     */
    public InternalClaims verify(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);

            JWSVerifier verifier = new MACVerifier(secretBytes);
            if (!jwt.verify(verifier)) {
                throw new InvalidTokenException("Invalid internal token signature");
            }

            JWTClaimsSet c = jwt.getJWTClaimsSet();

            Date expiry = c.getExpirationTime();
            if (expiry == null || expiry.before(new Date())) {
                throw new InvalidTokenException("Internal token expired");
            }

            String userId = c.getSubject();
            if (userId == null || userId.isBlank()) {
                throw new InvalidTokenException("Missing sub claim in internal token");
            }

            String role = c.getStringClaim("role");

            UUID tenantId    = parseUuid(c.getStringClaim("tenantId"));
            UUID clientId    = parseUuid(c.getStringClaim("clientId"));
            UUID subTenantId = parseUuid(c.getStringClaim("subTenantId"));

            return new InternalClaims(
                    userId,
                    c.getStringClaim("email"),
                    role,
                    tenantId,
                    clientId,
                    subTenantId
            );

        } catch (InvalidTokenException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidTokenException("Internal token validation failed: " + e.getMessage());
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) return null;
        try { return UUID.fromString(value); } catch (Exception e) { return null; }
    }
}

