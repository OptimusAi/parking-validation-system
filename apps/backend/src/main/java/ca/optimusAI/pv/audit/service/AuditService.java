package ca.optimusAI.pv.audit.service;

import ca.optimusAI.pv.audit.entity.AuditLog;
import ca.optimusAI.pv.audit.repository.AuditLogRepository;
import ca.optimusAI.pv.shared.PageResponse;
import ca.optimusAI.pv.shared.TenantContext;
import ca.optimusAI.pv.shared.exception.UnauthorizedTenantAccessException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public PageResponse<AuditLog> list(String action, String entityType,
                                       Instant from, Instant to,
                                       int page, int size) {
        PageRequest pr = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Specification<AuditLog> spec = buildSpec(action, entityType, from, to);
        return PageResponse.of(auditLogRepository.findAll(spec, pr));
    }

    private Specification<AuditLog> buildSpec(String action, String entityType,
                                               Instant from, Instant to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (TenantContext.hasRole("ADMIN")) {
                // no tenant filter — ADMIN sees all tenants
            } else if (TenantContext.hasRole("CLIENT_ADMIN")) {
                UUID clientId = TenantContext.clientId();
                if (clientId == null) {
                    throw new UnauthorizedTenantAccessException("No client assigned to current user");
                }
                predicates.add(cb.equal(root.get("clientId"), clientId));
            } else if (TenantContext.hasRole("TENANT_ADMIN")) {
                UUID tenantId = TenantContext.tenantId();
                if (tenantId == null) {
                    throw new UnauthorizedTenantAccessException("No tenant assigned to current user");
                }
                predicates.add(cb.equal(root.get("tenantId"), tenantId));
            } else {
                throw new UnauthorizedTenantAccessException("Insufficient role to view audit logs");
            }

            if (action != null && !action.isBlank()) {
                predicates.add(cb.equal(root.get("action"), action));
            }
            if (entityType != null && !entityType.isBlank()) {
                predicates.add(cb.equal(root.get("entityType"), entityType));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
