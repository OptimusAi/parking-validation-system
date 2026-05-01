package ca.optimusAI.pv.shared;

import java.util.List;
import java.util.UUID;

/**
 * Immutable snapshot of the authenticated user's tenant context, populated
 * from the TMS HS256 JWT on every request (no DB round-trip).
 *
 * assignedTenants is non-null only for CLIENT_ADMIN — it lists every tenant
 * the user is allowed to manage via client_admin_tenants.
 */
public record TenantInfo(
        UUID tenantId,
        UUID clientId,
        UUID subTenantId,
        String userId,
        String email,
        List<String> roles,
        List<UUID> assignedTenants
) {
    /** Convenience constructor for roles where assignedTenants is empty. */
    public TenantInfo(UUID tenantId, UUID clientId, UUID subTenantId,
                      String userId, String email, List<String> roles) {
        this(tenantId, clientId, subTenantId, userId, email, roles, List.of());
    }
}
