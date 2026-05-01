package ca.optimusAI.tms.auth;

import java.util.UUID;

/**
 * Claims carried inside the TMS-internal JWT.
 *
 * Signed by InternalTokenService (HMAC-SHA256) after:
 *   1. The OAuth server token has been validated.
 *   2. The user has been loaded/provisioned from the local DB.
 *
 * JwtAuthenticationFilter extracts these on every request and populates
 * TenantContext directly — no DB or Redis round-trip needed.
 */
public record InternalClaims(
        String userId,      // sub — UUID of the user in our DB
        String email,
        String role,        // ADMIN | CLIENT_ADMIN | TENANT_ADMIN | SUBTENANT_USER | VIEWER
        UUID   tenantId,    // may be null for unassigned users
        UUID   clientId,
        UUID   subTenantId
) {}

