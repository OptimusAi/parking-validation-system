package ca.optimusAI.pv.quota;

import ca.optimusAI.pv.shared.exception.QuotaExceededException;
import ca.optimusAI.pv.tenant.entity.QuotaConfig;
import ca.optimusAI.pv.tenant.repository.QuotaConfigRepository;
import ca.optimusAI.pv.validation.QuotaEnforcementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class QuotaEnforcementTest {

    @Mock RedisTemplate<String, String> redis;
    @Mock ValueOperations<String, String> valueOps;
    @Mock QuotaConfigRepository quotaConfigRepository;

    @InjectMocks QuotaEnforcementService quotaService;

    @Test
    void givenOverDailyLimit_whenEnforce_thenThrowAndDecrement() {
        UUID tenantId = UUID.randomUUID();

        when(redis.opsForValue()).thenReturn(valueOps);
        // Return 101 for daily key (over limit of 100)
        when(valueOps.increment(anyString())).thenReturn(101L);

        QuotaConfig dailyCfg = new QuotaConfig();
        dailyCfg.setMaxCount(100);
        dailyCfg.setPeriod("DAY");
        when(quotaConfigRepository.findTenantQuota(eq(tenantId), anyString()))
                .thenAnswer(inv -> "DAY".equals(inv.getArgument(1))
                        ? Optional.of(dailyCfg) : Optional.empty());

        assertThrows(QuotaExceededException.class,
                () -> quotaService.enforce(tenantId, null, "ABC123"));

        // Verify rollback — decrement should be called for incremented keys
        verify(valueOps, atLeastOnce()).decrement(anyString());
    }

    @Test
    void givenMaxCountZero_whenEnforce_thenUnlimited() {
        UUID tenantId = UUID.randomUUID();

        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(99999L);

        // maxCount=0 means unlimited
        QuotaConfig unlimitedCfg = new QuotaConfig();
        unlimitedCfg.setMaxCount(0);
        unlimitedCfg.setPeriod("DAY");
        when(quotaConfigRepository.findTenantQuota(eq(tenantId), any()))
                .thenReturn(Optional.of(unlimitedCfg));

        assertDoesNotThrow(() -> quotaService.enforce(tenantId, null, "ABC123"));
    }

    @Test
    void givenNoQuotaConfig_whenEnforce_thenPassThrough() {
        UUID tenantId = UUID.randomUUID();

        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(1L);
        when(quotaConfigRepository.findTenantQuota(any(), any()))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(() -> quotaService.enforce(tenantId, null, "ABC123"));
    }
}
