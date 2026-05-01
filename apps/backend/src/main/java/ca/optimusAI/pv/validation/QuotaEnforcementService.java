package ca.optimusAI.pv.validation;

import ca.optimusAI.pv.shared.exception.QuotaExceededException;
import ca.optimusAI.pv.tenant.entity.QuotaConfig;
import ca.optimusAI.pv.tenant.repository.QuotaConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Enforces per-tenant and per-license-plate quota limits using Redis counters.
 *
 * Keys (all expire at period boundary):
 *   quota:{tenantId}:DAY              — tenant daily count
 *   quota:{tenantId}:WEEK             — tenant weekly count
 *   quota:{tenantId}:MONTH            — tenant monthly count
 *   quota:lp:{licensePlate}:{tenantId}:24h — per-plate per-tenant rolling 24h count
 *
 * Counters are incremented atomically; if any limit is exceeded all increments
 * are rolled back before throwing QuotaExceededException.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuotaEnforcementService {

    private final RedisTemplate<String, String> redis;
    private final QuotaConfigRepository quotaConfigRepository;

    public void enforce(UUID tenantId, UUID subTenantId, String licensePlate) {
        List<QuotaEntry> entries = buildEntries(tenantId, licensePlate);
        List<String> incremented = new ArrayList<>();

        try {
            for (QuotaEntry entry : entries) {
                Long count = redis.opsForValue().increment(entry.key());
                if (count == null) count = 1L;
                if (count == 1L) {
                    redis.expire(entry.key(), Duration.ofSeconds(entry.ttlSeconds()));
                }
                incremented.add(entry.key());

                // Check against configured limit for this period
                Optional<QuotaConfig> cfg = findConfig(tenantId, subTenantId, entry.period());
                if (cfg.isPresent() && cfg.get().getMaxCount() > 0
                        && count > cfg.get().getMaxCount()) {
                    throw new QuotaExceededException(
                            "Quota exceeded for period " + entry.period()
                            + " (limit=" + cfg.get().getMaxCount() + ")");
                }
            }
        } catch (QuotaExceededException e) {
            // Roll back all increments performed so far
            incremented.forEach(k -> redis.opsForValue().decrement(k));
            throw e;
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Optional<QuotaConfig> findConfig(UUID tenantId, UUID subTenantId, String period) {
        if (subTenantId != null) {
            Optional<QuotaConfig> sub = quotaConfigRepository
                    .findSubTenantQuota(tenantId, period, subTenantId);
            if (sub.isPresent()) return sub;
        }
        return quotaConfigRepository.findTenantQuota(tenantId, period);
    }

    private List<QuotaEntry> buildEntries(UUID tenantId, String licensePlate) {
        return List.of(
                new QuotaEntry("quota:" + tenantId + ":DAY",   "DAY",   secondsUntilMidnightUtc()),
                new QuotaEntry("quota:" + tenantId + ":WEEK",  "WEEK",  secondsUntilSundayMidnightUtc()),
                new QuotaEntry("quota:" + tenantId + ":MONTH", "MONTH", secondsUntilMonthEndUtc()),
                new QuotaEntry("quota:lp:" + licensePlate + ":" + tenantId + ":24h", null, 86400L)
        );
    }

    private long secondsUntilMidnightUtc() {
        Instant now = Instant.now();
        Instant midnight = LocalDate.now(ZoneOffset.UTC)
                .plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        return Math.max(1, midnight.getEpochSecond() - now.getEpochSecond());
    }

    private long secondsUntilSundayMidnightUtc() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate nextSunday = today.with(DayOfWeek.SUNDAY).plusWeeks(
                today.getDayOfWeek() == DayOfWeek.SUNDAY ? 1 : 0);
        Instant cutoff = nextSunday.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        return Math.max(1, cutoff.getEpochSecond() - Instant.now().getEpochSecond());
    }

    private long secondsUntilMonthEndUtc() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate firstOfNext = today.withDayOfMonth(1).plusMonths(1);
        Instant cutoff = firstOfNext.atStartOfDay(ZoneOffset.UTC).toInstant();
        return Math.max(1, cutoff.getEpochSecond() - Instant.now().getEpochSecond());
    }

    private record QuotaEntry(String key, String period, long ttlSeconds) {}
}
