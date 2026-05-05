-- ─── zone_allocations ────────────────────────────────────────────────────────
-- Tracks how a tenant splits its total zone budget between direct use and
-- sub-tenants. One row per tenant (UNIQUE on tenant_id).
--
--   total_zones   — total zone limit granted by CLIENT_ADMIN / ADMIN
--   tenant_direct — zones reserved for the tenant's own direct use
--   sub_tenant    — zones reserved for sub-tenants
--
-- Invariant (enforced in service): tenant_direct + sub_tenant <= total_zones

CREATE TABLE zone_allocations (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID        NOT NULL UNIQUE REFERENCES tenants(id),
    client_id       UUID        NOT NULL,
    total_zones     INT         NOT NULL DEFAULT 0,
    tenant_direct   INT         NOT NULL DEFAULT 0,
    sub_tenant      INT         NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_zone_alloc_non_negative
        CHECK (total_zones >= 0 AND tenant_direct >= 0 AND sub_tenant >= 0),
    CONSTRAINT chk_zone_alloc_within_total
        CHECK (tenant_direct + sub_tenant <= total_zones)
);

CREATE INDEX idx_zone_allocations_tenant ON zone_allocations(tenant_id);
