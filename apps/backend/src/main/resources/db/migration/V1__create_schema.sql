-- Extensions (idempotent — also run at container init)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ─── app_user ─────────────────────────────────────────────────────────────────
-- No tenant_id Hibernate filter — ADMIN queries across all tenants.
-- Role + tenant scope live in user_role (one-to-one).
CREATE TABLE app_user (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    auth_provider_user_id VARCHAR(100) NOT NULL,
    email                 VARCHAR(255),
    first_name            VARCHAR(100),
    last_name             VARCHAR(100),
    is_active             BOOLEAN      NOT NULL DEFAULT true,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_app_user_auth_provider_user_id UNIQUE (auth_provider_user_id)
);

-- ─── user_role ────────────────────────────────────────────────────────────────
-- One row per user; holds role + tenant/client/sub-tenant scope.
CREATE TABLE user_role (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID        NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    role          VARCHAR(30) NOT NULL DEFAULT 'USER',
    tenant_id     UUID,
    client_id     UUID,
    sub_tenant_id UUID,
    is_active     BOOLEAN     NOT NULL DEFAULT true,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_user_role_user_id UNIQUE (user_id),
    CONSTRAINT chk_user_role_role   CHECK  (role IN ('ADMIN','CLIENT_ADMIN','TENANT_ADMIN','SUB_TENANT_ADMIN','USER'))
);

-- ─── login ────────────────────────────────────────────────────────────────────
-- Tracks every identity-provider account that has ever authenticated.
CREATE TABLE login (
    id               VARCHAR(36)  NOT NULL,
    login_provider   VARCHAR(128) NOT NULL,
    provider_user_id VARCHAR(256) NOT NULL,
    email            VARCHAR(128),
    last_login_date  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    user_id          UUID,
    CONSTRAINT login_pkey    PRIMARY KEY (id),
    CONSTRAINT login_user_fk FOREIGN KEY (user_id) REFERENCES app_user(id)
);

-- ─── clients ──────────────────────────────────────────────────────────────────
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
-- tenant_id mirrors id — required for Hibernate filter consistency.
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

-- ─── client_admin_tenants ────────────────────────────────────────────────────
-- Maps CLIENT_ADMIN users to the tenants they manage.
CREATE TABLE client_admin_tenants (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    tenant_id   UUID        NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_client_admin_tenant UNIQUE (user_id, tenant_id)
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
    sub_tenant_id            UUID,
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
