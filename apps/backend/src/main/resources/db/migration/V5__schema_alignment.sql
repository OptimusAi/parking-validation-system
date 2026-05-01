-- ═══════════════════════════════════════════════════════════════════════════
-- V5 — Schema alignment with TMS spec
--   1. Rename users → app_user, auth0_user_id → auth_provider_user_id
--   2. Create client_admin_tenants join table
--   3. Add sub_tenant_id to validation_links
--   4. Fix role default + add CHECK constraint
--   5. Data-migrate old role values
--   6. Add missing indexes
-- ═══════════════════════════════════════════════════════════════════════════

-- ─── 1. Rename table and column ───────────────────────────────────────────────
ALTER TABLE users RENAME TO app_user;
ALTER TABLE app_user RENAME COLUMN auth0_user_id TO auth_provider_user_id;

-- Update unique constraint (drop old, recreate)
ALTER TABLE app_user DROP CONSTRAINT IF EXISTS uq_users_auth0_user_id;
ALTER TABLE app_user ADD CONSTRAINT uq_app_user_auth_provider_user_id
    UNIQUE (auth_provider_user_id);

-- Drop old index (will be recreated below)
DROP INDEX IF EXISTS idx_users_auth0_user_id;

-- ─── 2. Fix login table FK to point at renamed table ──────────────────────────
ALTER TABLE login DROP CONSTRAINT IF EXISTS login_user_fk;
ALTER TABLE login ADD CONSTRAINT login_user_fk
    FOREIGN KEY (user_id) REFERENCES app_user(id);

-- ─── 3. Create client_admin_tenants join table ────────────────────────────────
CREATE TABLE IF NOT EXISTS client_admin_tenants (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    tenant_id   UUID         NOT NULL,
    assigned_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_client_admin_tenant UNIQUE (user_id, tenant_id)
);

-- ─── 4. Add sub_tenant_id to validation_links (if not present) ────────────────
ALTER TABLE validation_links
    ADD COLUMN IF NOT EXISTS sub_tenant_id UUID;

-- ─── 5. Fix role default and add CHECK constraint ─────────────────────────────
ALTER TABLE app_user ALTER COLUMN role SET DEFAULT 'USER';

-- Remove old CHECK constraint if any exists
ALTER TABLE app_user DROP CONSTRAINT IF EXISTS chk_app_user_role;

ALTER TABLE app_user ADD CONSTRAINT chk_app_user_role
    CHECK (role IN ('ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN', 'SUB_TENANT_ADMIN', 'USER'));

-- ─── 6. Data-migrate old role values ─────────────────────────────────────────
UPDATE app_user SET role = 'USER' WHERE role IN ('SUBTENANT_USER', 'VIEWER');

-- ─── 7. Add missing indexes ───────────────────────────────────────────────────

-- app_user indexes
CREATE INDEX IF NOT EXISTS idx_app_user_auth_provider_user_id
    ON app_user (auth_provider_user_id);

CREATE INDEX IF NOT EXISTS idx_app_user_tenant_id_is_active
    ON app_user (tenant_id, is_active);

CREATE INDEX IF NOT EXISTS idx_app_user_client_id
    ON app_user (client_id);

-- client_admin_tenants indexes
CREATE INDEX IF NOT EXISTS idx_cat_user_id
    ON client_admin_tenants (user_id);

CREATE INDEX IF NOT EXISTS idx_cat_tenant_id
    ON client_admin_tenants (tenant_id);
