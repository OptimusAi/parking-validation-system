-- ─── app_user ─────────────────────────────────────────────────────────────────
CREATE UNIQUE INDEX IF NOT EXISTS idx_app_user_auth_provider_user_id
    ON app_user (auth_provider_user_id);

CREATE INDEX IF NOT EXISTS idx_app_user_email
    ON app_user (email);

-- ─── user_role ────────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_user_role_user_id
    ON user_role (user_id);

CREATE INDEX IF NOT EXISTS idx_user_role_tenant_id
    ON user_role (tenant_id);

CREATE INDEX IF NOT EXISTS idx_user_role_client_id
    ON user_role (client_id);

-- ─── login ────────────────────────────────────────────────────────────────────
CREATE UNIQUE INDEX IF NOT EXISTS uq_login_provider_user_id
    ON login (login_provider, provider_user_id);

CREATE INDEX IF NOT EXISTS idx_login_email
    ON login (email);

-- ─── client_admin_tenants ─────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_cat_user_id
    ON client_admin_tenants (user_id);

CREATE INDEX IF NOT EXISTS idx_cat_tenant_id
    ON client_admin_tenants (tenant_id);

-- ─── tenant_admin_tenants ─────────────────────────────────────────────────────
CREATE UNIQUE INDEX IF NOT EXISTS idx_tat_user_id
    ON tenant_admin_tenants (user_id);

CREATE INDEX IF NOT EXISTS idx_tat_tenant_id
    ON tenant_admin_tenants (tenant_id);

-- ─── clients ──────────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_clients_is_deleted
    ON clients (is_deleted);

-- ─── tenants ──────────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_tenants_tenant_id_is_deleted
    ON tenants (tenant_id, is_deleted);

CREATE INDEX IF NOT EXISTS idx_tenants_client_id
    ON tenants (client_id);

-- ─── sub_tenants ──────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_sub_tenants_tenant_id_is_deleted
    ON sub_tenants (tenant_id, is_deleted);

-- ─── zones ────────────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_zones_tenant_id_is_deleted
    ON zones (tenant_id, is_deleted);

-- ─── quota_configs ────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_quota_configs_tenant_id_is_deleted
    ON quota_configs (tenant_id, is_deleted);

-- ─── validation_sessions ──────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_vsessions_tenant_id_is_deleted
    ON validation_sessions (tenant_id, is_deleted);

CREATE INDEX IF NOT EXISTS idx_vsessions_license_plate_tenant_id
    ON validation_sessions (license_plate, tenant_id);

CREATE INDEX IF NOT EXISTS idx_vsessions_zone_id_start_time
    ON validation_sessions (zone_id, start_time);

CREATE INDEX IF NOT EXISTS idx_vsessions_status_end_time
    ON validation_sessions (status, end_time);

-- ─── validation_links ─────────────────────────────────────────────────────────
CREATE UNIQUE INDEX IF NOT EXISTS idx_vlinks_token
    ON validation_links (token);

CREATE INDEX IF NOT EXISTS idx_vlinks_tenant_id_is_active
    ON validation_links (tenant_id, is_active);

CREATE INDEX IF NOT EXISTS idx_vlinks_tenant_id_is_deleted
    ON validation_links (tenant_id, is_deleted);

-- ─── notifications ────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_notifications_tenant_id_is_deleted
    ON notifications (tenant_id, is_deleted);

CREATE INDEX IF NOT EXISTS idx_notifications_session_id
    ON notifications (session_id);

-- ─── report_jobs ──────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_report_jobs_tenant_id_is_deleted
    ON report_jobs (tenant_id, is_deleted);

CREATE INDEX IF NOT EXISTS idx_report_jobs_status
    ON report_jobs (status);

-- ─── audit_logs ───────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_audit_logs_tenant_id_created_at
    ON audit_logs (tenant_id, created_at);

CREATE INDEX IF NOT EXISTS idx_audit_logs_entity_id
    ON audit_logs (entity_id);

CREATE INDEX IF NOT EXISTS idx_audit_logs_actor_id
    ON audit_logs (actor_id);
