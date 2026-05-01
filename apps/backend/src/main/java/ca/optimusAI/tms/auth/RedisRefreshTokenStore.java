package ca.optimusAI.tms.auth;

import ca.optimusAI.tms.shared.exception.InvalidTokenException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Server-side refresh token store backed by Redis.
 * Key: auth:refresh:{auth0UserId}   TTL: configured refresh-token-ttl-hours
 *
 * Implements rotation with reuse detection:
 *  - On each refresh, the old token is replaced with a new one.
 *  - If the old token is presented again (reuse), the entire session is revoked.
 */
@Component
@RequiredArgsConstructor
public class RedisRefreshTokenStore {

    private final RedisTemplate<String, String> redis;

    @Value("${oauth.refresh-token-ttl-hours:8}")
    private int ttlHours;

    private static final String PREFIX = "auth:refresh:";

    /** Persist a refresh token for the given user. */
    public void save(String userId, String token) {
        redis.opsForValue().set(PREFIX + userId, token, Duration.ofHours(ttlHours));
    }

    /** Load the stored refresh token, or empty if no session exists. */
    public Optional<String> load(String userId) {
        return Optional.ofNullable(redis.opsForValue().get(PREFIX + userId));
    }

    /**
     * Validates the presented token matches the stored token.
     * If mismatched (reuse detected), revokes the session immediately.
     *
     * @throws InvalidTokenException if session expired or reuse detected
     */
    public void detectReuse(String userId, String presented) {
        String stored = redis.opsForValue().get(PREFIX + userId);
        if (stored == null) {
            throw new InvalidTokenException("Session expired — please log in again");
        }
        if (!stored.equals(presented)) {
            redis.delete(PREFIX + userId);
            throw new InvalidTokenException("Refresh token reuse detected — session revoked");
        }
    }

    /** Replace the stored token with a new one (rotation). TTL is reset. */
    public void rotate(String userId, String newToken) {
        redis.opsForValue().set(PREFIX + userId, newToken, Duration.ofHours(ttlHours));
    }

    /** Invalidate the stored refresh token. */
    public void revoke(String userId) {
        redis.delete(PREFIX + userId);
    }
}
