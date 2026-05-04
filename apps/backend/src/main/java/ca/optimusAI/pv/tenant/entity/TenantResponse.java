package ca.optimusAI.pv.tenant.entity;

import java.time.Instant;
import java.util.UUID;

/**
 * Enriched tenant DTO returned by the list endpoint.
 * Includes zone/sub-tenant counts and flattened branding fields
 * so the frontend doesn't need to parse the settings JSONB.
 */
public record TenantResponse(
        UUID id,
        UUID clientId,
        String name,
        boolean isActive,
        long zones,
        long subTenants,
        BrandingDto branding,
        Instant createdAt
) {
    public record BrandingDto(String logoUrl, String primaryColor, String accentColor) {}
}
