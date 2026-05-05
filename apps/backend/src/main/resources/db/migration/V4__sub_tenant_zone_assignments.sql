-- ─── sub_tenant_zone_assignments ─────────────────────────────────────────────
-- Maps which zones a sub-tenant is permitted to use.
-- A zone with NO rows here is a "tenant-direct" zone.
-- A zone MAY be assigned to multiple sub-tenants.
CREATE TABLE sub_tenant_zone_assignments (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    sub_tenant_id UUID        NOT NULL REFERENCES sub_tenants(id) ON DELETE CASCADE,
    zone_id       UUID        NOT NULL REFERENCES zones(id)       ON DELETE CASCADE,
    assigned_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_sub_tenant_zone UNIQUE (sub_tenant_id, zone_id)
);
CREATE INDEX idx_stza_sub_tenant ON sub_tenant_zone_assignments(sub_tenant_id);
CREATE INDEX idx_stza_zone       ON sub_tenant_zone_assignments(zone_id);
