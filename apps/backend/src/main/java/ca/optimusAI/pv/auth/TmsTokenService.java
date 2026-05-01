package ca.optimusAI.pv.auth;

import ca.optimusAI.pv.shared.exception.InvalidTokenException;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Issues and validates TMS-internal JWTs (HMAC-SHA256).
 *
 * After the OAuth RS256 token is verified by OAuthAdapter, this service signs
 * a short-lived internal JWT embedding the full tenant context. All subsequent
 * requests carry this token — JwtAuthenticationFilter reads claims directly
 * from it without hitting the database.
 */
@Slf4j
@Service
public class TmsTokenService {

    @Value("${tms.jwt.secret:tms-internal-secret-change-me-in-production-min32}")
    private String secret;

    @Value("${tms.jwt.expiry-hours:8}")
    private int expiryHours;

    private byte[] secretBytes;

    @PostConstruct
    void init() {
        secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                    "tms.jwt.secret must be at least 32 characters long");
        }
        log.info("TmsTokenService initialised (expiry={}h, HMAC-SHA256)", expiryHours);
    }

    // ── Sign ──────────────────────────────────────────────────────────────────

    /**
     * Sign a new TMS JWT containing the user's full tenant context.
     *
     * @param claims all claims to embed (role, tenantId, clientId, subTenantId, assignedTenants)
     * @return signed JWT string
     */
    public String sign(TmsTokenClaims claims) {
        try {
            long now = System.currentTimeMillis();
            long exp = now + (long) expiryHours * 3_600_000L;

            JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                    .subject(claims.userId())
                    .issueTime(new Date(now))
                    .expirationTime(new Date(exp))
                    .claim("email", claims.email())
                    .claim("name",  claims.name())
                    .claim("role",  claims.role());

            if (claims.tenantId()    != null) builder.claim("tenantId",    claims.tenantId().toString());
            if (claims.clientId()    != null) builder.claim("clientId",    claims.clientId().toString());
            if (claims.subTenantId() != null) builder.claim("subTenantId", claims.subTenantId().toString());

            // assignedTenants as list of UUID strings (empty list if not CLIENT_ADMIN)
            List<String> tenantStrings = (claims.assignedTenants() == null)
                    ? List.of()
                    : claims.assignedTenants().stream().map(UUID::toString).toList();
            builder.claim("assignedTenants", tenantStrings);

            JWSSigner signer = new MACSigner(secretBytes);
            SignedJWT jwt    = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), builder.build());
            jwt.sign(signer);

            return jwt.serialize();

        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign TMS JWT: " + e.getMessage(), e);
        }
    }

    // ── Verify ────────────────────────────────────────────────────────────────

    /**
     * Verify the HMAC signature, check expiry, and extract all claims.
     *
     * @throws InvalidTokenException on any validation failure
     */
    public TmsTokenClaims verify(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);

            JWSVerifier verifier = new MACVerifier(secretBytes);
            if (!jwt.verify(verifier)) {
                throw new InvalidTokenException("Invalid TMS token signature");
            }

            JWTClaimsSet c = jwt.getJWTClaimsSet();

            Date expiry = c.getExpirationTime();
            if (expiry == null || expiry.before(new Date())) {
                throw new InvalidTokenException("TMS token expired");
            }

            String userId = c.getSubject();
            if (userId == null || userId.isBlank()) {
                throw new InvalidTokenException("Missing sub claim in TMS token");
            }

            UUID tenantId    = parseUuid(c.getStringClaim("tenantId"));
            UUID clientId    = parseUuid(c.getStringClaim("clientId"));
            UUID subTenantId = parseUuid(c.getStringClaim("subTenantId"));

            // Parse assignedTenants list
            List<UUID> assignedTenants = new ArrayList<>();
            Object raw = c.getClaim("assignedTenants");
            if (raw instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof String s) {
                        UUID parsed = parseUuid(s);
                        if (parsed != null) assignedTenants.add(parsed);
                    }
                }
            }

            return new TmsTokenClaims(
                    userId,
                    c.getStringClaim("email"),
                    c.getStringClaim("name"),
                    c.getStringClaim("role"),
                    tenantId,
                    clientId,
                    subTenantId,
                    List.copyOf(assignedTenants)
            );

        } catch (InvalidTokenException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidTokenException("TMS token validation failed: " + e.getMessage());
        }
    }

    private UUID parseUuid(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
