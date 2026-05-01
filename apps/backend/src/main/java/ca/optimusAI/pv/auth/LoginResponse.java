package ca.optimusAI.pv.auth;

import java.util.List;
import java.util.UUID;

/**
 * Response body returned by POST /api/auth/login and POST /api/auth/token-exchange.
 *
 * The client must carry {@code tmsToken} as {@code Authorization: Bearer {tmsToken}}
 * on all subsequent authenticated requests.
 *
 * {@code assignedTenants} is populated for CLIENT_ADMIN only (their accessible tenants).
 */
public record LoginResponse(
        String tmsToken,
        String role,
        UUID tenantId,
        UUID clientId,
        UUID subTenantId,
        List<UUID> assignedTenants,
        String email,
        String name,
        String message
) {}
