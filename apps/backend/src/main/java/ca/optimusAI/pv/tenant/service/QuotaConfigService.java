package ca.optimusAI.pv.tenant.service;

import ca.optimusAI.pv.shared.PageResponse;
import ca.optimusAI.pv.shared.TenantContext;
import ca.optimusAI.pv.shared.exception.ResourceNotFoundException;
import ca.optimusAI.pv.shared.exception.UnauthorizedTenantAccessException;
import ca.optimusAI.pv.tenant.entity.QuotaConfig;
import ca.optimusAI.pv.tenant.repository.QuotaConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuotaConfigService {

    private final QuotaConfigRepository quotaConfigRepository;

    @Transactional(readOnly = true)
    public PageResponse<QuotaConfig> list(int page, int size) {
        PageRequest pr = PageRequest.of(page, size, Sort.by("createdAt").descending());
        // Hibernate filter already scopes by tenantId when active
        return PageResponse.of(quotaConfigRepository.findAllByIsDeletedFalse(pr));
    }

    @Transactional(readOnly = true)
    public QuotaConfig get(UUID id) {
        return quotaConfigRepository.findById(id)
                .filter(q -> !q.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("QuotaConfig not found: " + id));
    }

    @Transactional
    public QuotaConfig create(UUID tenantId, UUID clientId, UUID subTenantId,
                              String scope, String period, int maxCount) {
        UUID resolvedTenantId = tenantId != null ? tenantId : TenantContext.tenantId();
        UUID resolvedClientId = clientId != null ? clientId : TenantContext.clientId();
        if (resolvedTenantId == null) throw new UnauthorizedTenantAccessException("tenantId required");
        if (resolvedClientId == null) throw new UnauthorizedTenantAccessException("clientId required");

        QuotaConfig config = QuotaConfig.builder()
                .tenantId(resolvedTenantId)
                .clientId(resolvedClientId)
                .subTenantId(subTenantId)
                .scope(scope)
                .period(period)
                .maxCount(maxCount)
                .build();
        return quotaConfigRepository.save(config);
    }

    /**
     * Update — no delete on QuotaConfig. Only maxCount and scope can be changed.
     * As per agents.md: CRU (no delete endpoint).
     */
    @Transactional
    public QuotaConfig update(UUID id, Integer maxCount, String scope) {
        QuotaConfig config = get(id);
        assertWriteAccess(config);
        if (maxCount != null) config.setMaxCount(maxCount);
        if (scope != null) config.setScope(scope);
        return quotaConfigRepository.save(config);
    }

    private void assertWriteAccess(QuotaConfig config) {
        if (TenantContext.hasRole("ADMIN") || TenantContext.hasRole("CLIENT_ADMIN")) return;
        UUID callerTenantId = TenantContext.tenantId();
        if (callerTenantId == null || !callerTenantId.equals(config.getTenantId())) {
            throw new UnauthorizedTenantAccessException("Access denied to quota config: " + config.getId());
        }
    }
}
