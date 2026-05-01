-- Extensions (idempotent — also run at container init)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ─── users ────────────────────────────────────────────────────────────────────
-- No tenant_id Hibernate filter — ADMIN queries across all tenants
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    auth0_user_id   VARCHAR(100) NOT NULL,
    email           VARCHAR(255),
    name            VARCHAR(255),
    tenant_id       UUID,
    client_id       UUID,
    sub_tenant_id   UUID,
    role            VARCHAR(30)  NOT NULL DEFAULT 'SUBTENANT_USER',
    is_active       BOOLEAN      NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_auth0_user_id UNIQUE (auth0_user_id)
);

-- ─── clients ──────────────────────────────────────────────────────────────────
-- No Hibernate filter — ADMIN queries across all clients
CREATE TABLE clients (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    plan        VARCHAR(50)  NOT NULL DEFAULT 'STANDARD',
    settings    JSONB        NOT NULL DEFAULT '{}',
    is_active   BOOLEAN      NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    is_deleted  BOOLEAN      NOT NULL DEFAULT false
);

-- ─── tenants ──────────────────────────────────────────────────────────────────
-- tenant_id mirrors id — required for Hibernate filter consistency
CREATE TABLE tenants (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id   UUID         NOT NULL REFERENCES clients(id),
    tenant_id   UUID         NOT NULL,   -- equals id; used by Hibernate filter
    name        VARCHAR(255) NOT NULL,
    settings    JSONB        NOT NULL DEFAULT '{}',   -- contains branding object
    is_active   BOOLEAN      NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    is_deleted  BOOLEAN      NOT NULL DEFAULT false
);

-- ─── sub_tenants ──────────────────────────────────────────────────────────────
CREATE TABLE sub_tenants (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID         NOT NULL REFERENCES tenants(id),
    client_id   UUID         NOT NULL,
    name        VARCHAR(255) NOT NULL,
    settings    JSONB        NOT NULL DEFAULT '{}',
    is_active   BOOLEAN      NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    is_deleted  BOOLEAN      NOT NULL DEFAULT false
);

-- ─── zones ────────────────────────────────────────────────────────────────────
CREATE TABLE zones (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                UUID         NOT NULL REFERENCES tenants(id),
    client_id                UUID         NOT NULL,
    zone_number              VARCHAR(20)  NOT NULL,
    name                     VARCHAR(255) NOT NULL,
    default_duration_minutes INTEGER      NOT NULL DEFAULT 60,
    max_duration_minutes     INTEGER      NOT NULL DEFAULT 1440,
    is_active                BOOLEAN      NOT NULL DEFAULT true,
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    is_deleted               BOOLEAN      NOT NULL DEFAULT false
);

-- ─── quota_configs ────────────────────────────────────────────────────────────
CREATE TABLE quota_configs (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id      UUID        NOT NULL,
    client_id      UUID        NOT NULL,
    sub_tenant_id  UUID,                    -- NULL = tenant-level quota
    scope          VARCHAR(20) NOT NULL,    -- TENANT | SUBTENANT
    period         VARCHAR(10) NOT NULL,    -- DAY | WEEK | MONTH
    max_count      INTEGER     NOT NULL DEFAULT 0,  -- 0 = unlimited
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_deleted     BOOLEAN     NOT NULL DEFAULT false
);

-- ─── validation_sessions ──────────────────────────────────────────────────────
CREATE TABLE validation_sessions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID         NOT NULL,
    client_id       UUID         NOT NULL,
    sub_tenant_id   UUID,
    zone_id         UUID         NOT NULL REFERENCES zones(id),
    license_plate   VARCHAR(10)  NOT NULL,
    start_time      TIMESTAMPTZ,
    end_time        TIMESTAMPTZ,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE | EXTENDED | CANCELLED | EXPIRED
    extended_count  INTEGER      NOT NULL DEFAULT 0,
    end_user_email  VARCHAR(255),
    end_user_phone  VARCHAR(30),
    created_by      UUID,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    is_deleted      BOOLEAN      NOT NULL DEFAULT false
);

-- ─── validation_links ─────────────────────────────────────────────────────────
CREATE TABLE validation_links (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                UUID         NOT NULL,
    client_id                UUID         NOT NULL,
    zone_id                  UUID         NOT NULL REFERENCES zones(id),
    link_type                VARCHAR(10)  NOT NULL,   -- URL | QR
    token                    VARCHAR(500) NOT NULL,
    label                    VARCHAR(255),
    default_duration_minutes INTEGER      NOT NULL DEFAULT 60,
    expires_at               TIMESTAMPTZ,
    is_active                BOOLEAN      NOT NULL DEFAULT true,
    scan_count               INTEGER      NOT NULL DEFAULT 0,
    created_by               UUID,
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    is_deleted               BOOLEAN      NOT NULL DEFAULT false,
    CONSTRAINT uq_validation_links_token UNIQUE (token)
);

-- ─── notifications ────────────────────────────────────────────────────────────
CREATE TABLE notifications (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID        NOT NULL,
    client_id     UUID        NOT NULL,
    session_id    UUID,
    channel       VARCHAR(20) NOT NULL,   -- SMS | EMAIL | WEBSOCKET
    recipient     VARCHAR(255),
    event_type    VARCHAR(50),
    status        VARCHAR(10) NOT NULL DEFAULT 'SENT',   -- SENT | FAILED
    payload       JSONB       NOT NULL DEFAULT '{}',
    error_message TEXT,
    sent_at       TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_deleted    BOOLEAN     NOT NULL DEFAULT false
);

-- ─── report_jobs ──────────────────────────────────────────────────────────────
CREATE TABLE report_jobs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID        NOT NULL,
    client_id       UUID        NOT NULL,
    report_type     VARCHAR(50) NOT NULL,   -- VALIDATION_SESSIONS | QUOTA_USAGE | ZONE_SUMMARY
    format          VARCHAR(10) NOT NULL,   -- CSV | EXCEL | PDF
    filters         JSONB       NOT NULL DEFAULT '{}',
    status          VARCHAR(20) NOT NULL DEFAULT 'QUEUED',  -- QUEUED | PROCESSING | COMPLETED | FAILED
    file_url        TEXT,
    file_size_bytes BIGINT,
    error_message   TEXT,
    expires_at      TIMESTAMPTZ,
    requested_by    UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_deleted      BOOLEAN     NOT NULL DEFAULT false
);

-- ─── audit_logs ───────────────────────────────────────────────────────────────
-- No is_deleted, no Hibernate filter — all records permanently visible
CREATE TABLE audit_logs (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    UUID,
    client_id    UUID,
    actor_id     VARCHAR(100),   -- auth0_user_id
    actor_email  VARCHAR(255),
    action       VARCHAR(50)  NOT NULL,
    entity_type  VARCHAR(50),
    entity_id    UUID,
    before_state JSONB,
    after_state  JSONB,
    ip_address   VARCHAR(50),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
