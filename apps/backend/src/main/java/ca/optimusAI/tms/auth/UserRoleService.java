package ca.optimusAI.tms.auth;

import ca.optimusAI.tms.shared.exception.InvalidTokenException;
import ca.optimusAI.tms.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/**
 * Loads a user record by Auth0 user ID.
 *
 * Strategy:
 *   1. Try Redis cache (5-min TTL by default).
 *   2. If miss, query the database.
 *   3. If not in DB (first login), create with role=SUBTENANT_USER, no tenant assigned.
 *   4. Reject if is_active=false.
 *   5. Write the record back to Redis before returning.
 *
 * Cache is evicted on role changes or deactivation (see evictCache).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserRoleService {

    private final UserProvisioner userProvisioner;
    private final LoginService loginService;
    private final RedisTemplate<String, String> redis;
    private final ObjectMapper objectMapper;

    @Value("${oauth.user-role-cache-minutes:5}")
    private int cacheTtlMinutes;

    private static final String CACHE_PREFIX = "user:role:";

    // ── Login path: upsert login row + load user ──────────────────────────────

    /**
     * Called on every POST /api/auth/login.
     *
     * Upserts the {@code login} row (create on first login, update on subsequent ones),
     * resolves roles from the linked {@code users} row, caches the result, and returns
     * the {@link UserRecord}.
     *
     * @param loginProvider  identity-provider name, e.g. "auth0"
     * @param providerUserId OAuth subject / uid from the access token
     * @param email          email from the token claims
     * @param displayName    full name from the token claims (may be null)
     * @param gtoken         raw OAuth access-token to store in the login row
     * @return the resolved {@link UserRecord}
     * @throws InvalidTokenException if the linked account is disabled
     */
    @Transactional
    public UserRecord loginAndLoad(String loginProvider,
                                   String providerUserId,
                                   String email,
                                   String displayName,
                                   String gtoken) {
        User user = loginService.upsertLogin(loginProvider, providerUserId, email, displayName, gtoken);

        if (!user.isActive()) {
            throw new InvalidTokenException("Account disabled");
        }

        UserRecord record = UserRecord.from(user);
        cacheRecord(providerUserId, record);
        return record;
    }

    // ── Non-login path: token refresh / /me ──────────────────────────────────

    /**
     * Load (or provision) the user matching the given Auth0 user ID.
     * Used for token-refresh and /me — does NOT touch the login table.
     *
     * @param auth0UserId  Auth0 subject/user ID — never null
     * @param email        email from JWT claims — may be null on access tokens
     * @param name         display name from JWT claims — may be null
     * @return             the resolved UserRecord
     * @throws InvalidTokenException if the account is disabled
     */
    @Transactional
    public UserRecord loadByAuth0Id(String auth0UserId, String email, String name) {
        String cacheKey = CACHE_PREFIX + auth0UserId;

        // ── 1. Redis cache ────────────────────────────────────────────────────
        String cached = redis.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                UserRecord record = objectMapper.readValue(cached, UserRecord.class);
                if (!record.isActive()) {
                    throw new InvalidTokenException("Account disabled");
                }
                return record;
            } catch (InvalidTokenException e) {
                throw e;
            } catch (Exception e) {
                // Corrupted cache entry — evict and fall through to DB
                log.warn("Corrupted cache entry for user {}; evicting", auth0UserId);
                redis.delete(cacheKey);
            }
        }

        // ── 2 & 3. Database lookup / first-login provisioning ─────────────────
        // UserProvisioner runs in REQUIRES_NEW so concurrent threads won't race.
        User user = userProvisioner.findOrCreate(auth0UserId, email, name);

        // ── 4. Reject disabled accounts ───────────────────────────────────────
        if (!user.isActive()) {
            throw new InvalidTokenException("Account disabled");
        }

        // ── 5. Write back to cache ─────────────────────────────────────────────
        UserRecord record = UserRecord.from(user);
        cacheRecord(auth0UserId, record);
        return record;
    }

    // ── Cache eviction ────────────────────────────────────────────────────────

    /**
     * Evict the Redis cache entry for a user.
     * Must be called after any role change or deactivation so the next request
     * picks up the updated record from the database.
     */
    public void evictCache(String auth0UserId) {
        redis.delete(CACHE_PREFIX + auth0UserId);
        log.debug("Evicted role cache for auth0UserId={}", auth0UserId);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void cacheRecord(String key, UserRecord record) {
        try {
            redis.opsForValue().set(
                    CACHE_PREFIX + key,
                    objectMapper.writeValueAsString(record),
                    Duration.ofMinutes(cacheTtlMinutes));
        } catch (Exception e) {
            log.warn("Failed to cache UserRecord for {}: {}", key, e.getMessage());
        }
    }
}
