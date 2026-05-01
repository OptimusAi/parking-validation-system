package ca.optimusAI.pv.shared.interceptor;

import ca.optimusAI.pv.shared.TenantContext;
import ca.optimusAI.pv.shared.TenantInfo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Activates the Hibernate tenantFilter for TENANT_ADMIN, SUB_TENANT_ADMIN, and USER.
 *
 * ADMIN:        filter NOT enabled — sees everything across all tenants.
 * CLIENT_ADMIN: filter NOT enabled — service layer handles multi-tenant scope
 *               via TenantContext.assignedTenants().
 * TENANT_ADMIN: filter enabled with their tenantId.
 * SUB_TENANT_ADMIN / USER: filter enabled with their tenantId.
 */
@Component
public class TenantFilterInterceptor implements HandlerInterceptor {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public boolean preHandle(HttpServletRequest req,
                             HttpServletResponse res,
                             Object handler) {
        TenantInfo info = TenantContext.get();
        if (info == null) return true;

        String role = info.roles().isEmpty() ? null : info.roles().get(0);

        // ADMIN sees everything; CLIENT_ADMIN scope is handled in the service layer
        if ("ADMIN".equals(role) || "CLIENT_ADMIN".equals(role)) {
            return true;
        }

        // For all other roles, enable filter if tenantId is present
        if (info.tenantId() != null) {
            entityManager.unwrap(Session.class)
                    .enableFilter("tenantFilter")
                    .setParameter("tenantId", info.tenantId());
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest req,
                                HttpServletResponse res,
                                Object handler,
                                Exception ex) {
        TenantContext.clear();
    }
}
