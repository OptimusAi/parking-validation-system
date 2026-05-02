package ca.optimusAI.pv.auth;

import java.util.List;
import java.util.UUID;

/**
 * Claims carried inside the TMS-internal HS256 JWT.
 *
 * Signed by TmsTokenService at login. Validated by JwtAuthenticationFilter
 * on every request — no DB or Redis round-trip needed.
 */
public record TmsTokenClaims(
        String userId,           // sub — authProviderUserId
        String email,
        String firstName,
        String lastName,
        String role,             // ADMIN | CLIENT_ADMIN | TENANT_ADMIN | SUB_TENANT_ADMIN | USER
        UUID   tenantId,         // nullable
        UUID   clientId,         // nullable
        UUID   subTenantId,      // nullable
        List<UUID> assignedTenants  // CLIENT_ADMIN only — their accessible tenant list
) {}
