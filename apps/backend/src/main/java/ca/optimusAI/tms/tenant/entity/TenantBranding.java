package ca.optimusAI.tms.tenant.entity;

/**
 * Branding data extracted from a Tenant's settings JSONB.
 * Returned by the public GET /api/v1/tenants/{id}/branding endpoint.
 * Used by QR PDF generation, email templates, and the frontend theme.
 */
public record TenantBranding(String logoUrl, String primaryColor, String accentColor) {

    public static TenantBranding defaults() {
        return new TenantBranding("", "#1B4F8A", "#2E86C1");
    }
}
