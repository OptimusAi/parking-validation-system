package ca.optimusAI.pv.auth;

import ca.optimusAI.pv.shared.exception.InvalidTokenException;
import ca.optimusAI.pv.user.entity.AppUser;
import ca.optimusAI.pv.user.entity.UserRole;
import ca.optimusAI.pv.user.repository.UserRoleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/**
 * Loads a user record by auth provider user ID.
 * Uses a Redis cache (5-min TTL) to avoid DB round-trips on every request.
 * Cache is evicted on role changes or deactivation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserRoleService {

    private final UserProvisioner   userProvisioner;
    private final LoginService       loginService;
    private final UserRoleRepository userRoleRepository;
    private final RedisTemplate<String, String> redis;
    private final ObjectMapper       objectMapper;

    @Value("${oauth.user-role-cache-minutes:5}")
    private int cacheTtlMinutes;

    private static final String CACHE_PREFIX = "user:role:";

    /**
     * Called on every POST /api/auth/login.
     * Upserts the login row, resolves user, caches, and returns UserRecord.
     */
    @Transactional
    public UserRecord loginAndLoad(String loginProvider,
                                   String providerUserId,
                                   String email,
                                   String firstName,
                                   String lastName) {
        AppUser user = loginService.upsertLogin(loginProvider, providerUserId, email, firstName, lastName);

        if (!user.isActive()) {
            throw new InvalidTokenException("Account disabled");
        }

        UserRole userRole = userRoleRepository.findByUserId(user.getId()).orElse(null);
        UserRecord record = UserRecord.from(user, userRole);
        cacheRecord(providerUserId, record);
        return record;
    }

    /**
     * Load (or provision) the user matching the given auth provider user ID.
     * Used for token-refresh and /me — does NOT touch the login table.
     */
    @Transactional
    public UserRecord loadByAuth0Id(String authProviderUserId, String email, String firstName, String lastName) {
        String cacheKey = CACHE_PREFIX + authProviderUserId;

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
                log.warn("Corrupted cache entry for user {}; evicting", authProviderUserId);
                redis.delete(cacheKey);
            }
        }

        AppUser user = userProvisioner.findOrCreate(authProviderUserId, email, firstName, lastName);

        if (!user.isActive()) {
            throw new InvalidTokenException("Account disabled");
        }

        UserRole userRole = userRoleRepository.findByUserId(user.getId()).orElse(null);
        UserRecord record = UserRecord.from(user, userRole);
        cacheRecord(authProviderUserId, record);
        return record;
    }

    /** Evict the Redis cache entry for a user after role/tenant changes. */
    public void evictCache(String authProviderUserId) {
        redis.delete(CACHE_PREFIX + authProviderUserId);
        log.debug("Evicted role cache for authProviderUserId={}", authProviderUserId);
    }

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
