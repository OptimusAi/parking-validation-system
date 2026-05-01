package ca.optimusAI.pv.shared;

import java.util.List;
import java.util.UUID;

public final class TenantContext {

    private static final ThreadLocal<TenantInfo> CURRENT = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(TenantInfo info)    { CURRENT.set(info); }
    public static TenantInfo get()             { return CURRENT.get(); }
    public static void clear()                 { CURRENT.remove(); }

    public static boolean hasRole(String role) {
        TenantInfo i = CURRENT.get();
        return i != null && i.roles().contains(role);
    }

    public static String role() {
        TenantInfo i = CURRENT.get();
        return (i == null || i.roles().isEmpty()) ? null : i.roles().get(0);
    }

    public static UUID tenantId() {
        TenantInfo i = CURRENT.get();
        return i == null ? null : i.tenantId();
    }

    public static UUID clientId() {
        TenantInfo i = CURRENT.get();
        return i == null ? null : i.clientId();
    }

    public static String userId() {
        TenantInfo i = CURRENT.get();
        return i == null ? null : i.userId();
    }

    public static List<UUID> assignedTenants() {
        TenantInfo i = CURRENT.get();
        return (i == null || i.assignedTenants() == null) ? List.of() : i.assignedTenants();
    }

    /**
     * Returns true if the current user can access the given tenantId.
     *
     * ADMIN:           always true
     * CLIENT_ADMIN:    true if tenantId is in assignedTenants list
     * TENANT_ADMIN:    true if tenantId matches their own tenantId
     * Others:          false
     */
    public static boolean canAccessTenant(UUID tenantId) {
        if (tenantId == null) return false;
        TenantInfo i = CURRENT.get();
        if (i == null) return false;
        if (i.roles().contains("ADMIN")) return true;
        if (i.roles().contains("CLIENT_ADMIN")) {
            return i.assignedTenants() != null && i.assignedTenants().contains(tenantId);
        }
        if (i.roles().contains("TENANT_ADMIN")) {
            return tenantId.equals(i.tenantId());
        }
        return false;
    }
}
