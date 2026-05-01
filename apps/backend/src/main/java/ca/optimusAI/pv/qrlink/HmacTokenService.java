package ca.optimusAI.pv.qrlink;

import ca.optimusAI.pv.shared.exception.InvalidTokenException;
import ca.optimusAI.pv.shared.exception.LinkExpiredException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Creates and verifies HMAC-SHA256 QR/URL tokens.
 *
 * Token format: sig.payload
 *   payload = Base64URL( "{tenantId}:{zoneId}:{durationMin}:{expiresAtEpoch}:{nonce}" )
 *   sig     = Base64URL( HMAC-SHA256(payload, QR_HMAC_SECRET) )
 */
@Slf4j
@Service
public class HmacTokenService {

    private static final String ALGO = "HmacSHA256";

    @Value("${tms.qr.hmac-secret:qr-hmac-secret-change-me-in-production-min32chars}")
    private String hmacSecret;

    public record QrClaims(UUID tenantId, UUID zoneId, int durationMinutes, Instant expiresAt) {}

    public String generate(UUID tenantId, UUID zoneId, int durationMinutes, Instant expiresAt) {
        String nonce = UUID.randomUUID().toString();
        String payload = tenantId + ":" + zoneId + ":" + durationMinutes + ":" +
                         expiresAt.getEpochSecond() + ":" + nonce;
        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String sig = hmac(encodedPayload);
        return sig + "." + encodedPayload;
    }

    public QrClaims verify(String token) {
        if (token == null || !token.contains(".")) {
            throw new InvalidTokenException("Malformed QR token");
        }
        int dot = token.indexOf('.');
        String sig            = token.substring(0, dot);
        String encodedPayload = token.substring(dot + 1);

        String expectedSig = hmac(encodedPayload);
        if (!constantTimeEquals(sig, expectedSig)) {
            throw new InvalidTokenException("Invalid QR token signature");
        }

        String payload = new String(Base64.getUrlDecoder().decode(encodedPayload), StandardCharsets.UTF_8);
        String[] parts = payload.split(":");
        if (parts.length < 5) {
            throw new InvalidTokenException("Malformed QR token payload");
        }

        Instant expiresAt = Instant.ofEpochSecond(Long.parseLong(parts[3]));
        if (Instant.now().isAfter(expiresAt)) {
            throw new LinkExpiredException("QR token has expired");
        }

        return new QrClaims(
                UUID.fromString(parts[0]),
                UUID.fromString(parts[1]),
                Integer.parseInt(parts[2]),
                expiresAt
        );
    }

    private String hmac(String data) {
        try {
            Mac mac = Mac.getInstance(ALGO);
            mac.init(new SecretKeySpec(hmacSecret.getBytes(StandardCharsets.UTF_8), ALGO));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC computation failed", e);
        }
    }

    /** Constant-time string comparison to prevent timing attacks. */
    private boolean constantTimeEquals(String a, String b) {
        byte[] ba = a.getBytes(StandardCharsets.UTF_8);
        byte[] bb = b.getBytes(StandardCharsets.UTF_8);
        if (ba.length != bb.length) return false;
        int result = 0;
        for (int i = 0; i < ba.length; i++) {
            result |= ba[i] ^ bb[i];
        }
        return result == 0;
    }
}
