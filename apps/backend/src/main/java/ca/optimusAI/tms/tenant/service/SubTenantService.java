package ca.optimusAI.tms.tenant.service;

import ca.optimusAI.tms.shared.PageResponse;
import ca.optimusAI.tms.shared.TenantContext;
import ca.optimusAI.tms.shared.exception.ResourceNotFoundException;
import ca.optimusAI.tms.shared.exception.UnauthorizedTenantAccessException;
import ca.optimusAI.tms.tenant.entity.SubTenant;
import ca.optimusAI.tms.tenant.repository.SubTenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubTenantService {

    private final SubTenantRepository subTenantRepository;

    @Transactional(readOnly = true)
    public PageResponse<SubTenant> list(int page, int size) {
        PageRequest pr = PageRequest.of(page, size, Sort.by("createdAt").descending());
        // Hibernate filter restricts results to callers tenantId (if set)
        // ADMIN override: pass explicit tenantId query param (handled in controller)
        return PageResponse.of(subTenantRepository.findAllByIsDeletedFalse(pr));
    }

    @Transactional(readOnly = true)
    public PageResponse<SubTenant> listByTenant(UUID tenantId, int page, int size) {
        PageRequest pr = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return PageResponse.of(subTenantRepository.findAllByTenantIdAndIsDeletedFalse(tenantId, pr));
    }

    @Transactional(readOnly = true)
    public SubTenant get(UUID id) {
        return subTenantRepository.findById(id)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Sub-tenant not found: " + id));
    }

    @Transactional
    public SubTenant create(UUID tenantId, UUID clientId, String name) {
        UUID resolvedTenantId = tenantId != null ? tenantId : TenantContext.tenantId();
        UUID resolvedClientId = clientId != null ? clientId : TenantContext.clientId();
        if (resolvedTenantId == null) throw new UnauthorizedTenantAccessException("tenantId required");
        if (resolvedClientId == null) throw new UnauthorizedTenantAccessException("clientId required");

        SubTenant sub = SubTenant.builder()
                .tenantId(resolvedTenantId)
                .clientId(resolvedClientId)
                .name(name)
                .build();
        return subTenantRepository.save(sub);
    }

    @Transactional
    public SubTenant update(UUID id, String name) {
        SubTenant sub = get(id);
        assertWriteAccess(sub);
        if (name != null) sub.setName(name);
        return subTenantRepository.save(sub);
    }

    @Transactional
    public void delete(UUID id) {
        SubTenant sub = get(id);
        assertWriteAccess(sub);
        sub.setDeleted(true);
        subTenantRepository.save(sub);
    }

    private void assertWriteAccess(SubTenant sub) {
        if (TenantContext.hasRole("ADMIN") || TenantContext.hasRole("CLIENT_ADMIN")) return;
        UUID callerTenantId = TenantContext.tenantId();
        if (callerTenantId == null || !callerTenantId.equals(sub.getTenantId())) {
            throw new UnauthorizedTenantAccessException("Access denied to sub-tenant: " + sub.getId());
        }
    }
}
