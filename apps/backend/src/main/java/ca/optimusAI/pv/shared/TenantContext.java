package ca.optimusAI.pv.shared;

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
}
