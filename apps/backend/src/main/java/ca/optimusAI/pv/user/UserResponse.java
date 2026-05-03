package ca.optimusAI.pv.user;

import java.time.Instant;
import java.util.UUID;

/**
 * Enriched user response — combines AppUser + UserRole + first assignment.
 * Returned by GET /api/v1/users so the frontend can show role and tenant scope.
 */
public record UserResponse(
        UUID    id,
        String  email,
        String  firstName,
        String  lastName,
        String  fullName,
        boolean isActive,
        String  role,
        UUID    tenantId,
        UUID    clientId,
        Instant createdAt
) {}
