-- ─────────────────────────────────────────────────────────────────────────────
-- V5__demo_seed_data.sql
-- Demo seed data for client presentation
--
-- Hierarchy:
--   ADMIN       venugopal.kannuri@calgary.ca
--
--   CLIENT_ADMIN cpatest1100@gmail.com  →  Client: CPA Downtown Parking
--     TENANT_ADMIN cpatest3300@gmail.com →  Tenant: Downtown Parkade
--       SUB_TENANT_ADMIN cpatest5500 → Sub-Tenant: Level B1 Commuters  (Zone A)
--       SUB_TENANT_ADMIN cpatest6600 → Sub-Tenant: Ground Floor Retail  (Zone C)
--       Zones: A (Level B1 East), B (Level B1 West), C (Ground Floor Retail)
--
--   CLIENT_ADMIN cpatest2200@gmail.com  →  Client: CPA Airport Parking
--     TENANT_ADMIN cpatest4400@gmail.com →  Tenant: Airport Long-Term Lot
--       Zones: P1 (Short Stay), P2 (Long Stay)
--
-- NOTE: All UUIDs use only valid hex characters (0-9, a-f).
-- ─────────────────────────────────────────────────────────────────────────────

-- ─── App Users ───────────────────────────────────────────────────────────────
INSERT INTO app_user (id, auth_provider_user_id, email, first_name, last_name, is_active)
VALUES
  ('aa000001-0000-0000-0000-000000000001', '5dfb7d85-6140-41d8-b5fa-8c64d6d384a8', 'venugopal.kannuri@calgary.ca', 'Venu Gopal', 'Kannuri', true),
  ('aa000001-0000-0000-0000-000000000002', '5d998395-1268-408d-8ca9-24f972a0a8f0', 'cpatest1100@gmail.com',         'Alex',      'Chen',   true),
  ('aa000001-0000-0000-0000-000000000003', 'f5f76618-ded2-4f64-bb1a-9de38ed937ee', 'cpatest2200@gmail.com',         'Maria',     'Santos', true),
  ('aa000001-0000-0000-0000-000000000004', '942e824a-bdb0-4d40-8b63-2630917f1659', 'cpatest3300@gmail.com',         'Jordan',    'Lee',    true),
  ('aa000001-0000-0000-0000-000000000005', 'ed35311c-d4e3-495b-beaf-34bf13602701', 'cpatest4400@gmail.com',         'Sam',       'Patel',  true),
  ('aa000001-0000-0000-0000-000000000006', '2739a7c2-974c-4858-ab9b-4452a8bf2e48', 'cpatest5500@gmail.com',         'Chris',     'Brown',  true),
  ('aa000001-0000-0000-0000-000000000007', 'ed35311c-d4e3-495b-beaf-23bf13602701', 'cpatest6600@gmail.com',         'Taylor',    'Kim',    true)
ON CONFLICT (auth_provider_user_id) DO NOTHING;

-- ─── Login records ───────────────────────────────────────────────────────────
INSERT INTO login (id, login_provider, provider_user_id, email, user_id)
SELECT id, login_provider, provider_user_id, email, user_id FROM (VALUES
  ('ab000001-0000-0000-0000-000000000001', 'oauth2', '5dfb7d85-6140-41d8-b5fa-8c64d6d384a8', 'venugopal.kannuri@calgary.ca', 'aa000001-0000-0000-0000-000000000001'::uuid),
  ('ab000001-0000-0000-0000-000000000002', 'oauth2', '5d998395-1268-408d-8ca9-24f972a0a8f0', 'cpatest1100@gmail.com',         'aa000001-0000-0000-0000-000000000002'::uuid),
  ('ab000001-0000-0000-0000-000000000003', 'oauth2', 'f5f76618-ded2-4f64-bb1a-9de38ed937ee', 'cpatest2200@gmail.com',         'aa000001-0000-0000-0000-000000000003'::uuid),
  ('ab000001-0000-0000-0000-000000000004', 'oauth2', '942e824a-bdb0-4d40-8b63-2630917f1659', 'cpatest3300@gmail.com',         'aa000001-0000-0000-0000-000000000004'::uuid),
  ('ab000001-0000-0000-0000-000000000005', 'oauth2', 'ed35311c-d4e3-495b-beaf-34bf13602701', 'cpatest4400@gmail.com',         'aa000001-0000-0000-0000-000000000005'::uuid),
  ('ab000001-0000-0000-0000-000000000006', 'oauth2', '2739a7c2-974c-4858-ab9b-4452a8bf2e48', 'cpatest5500@gmail.com',         'aa000001-0000-0000-0000-000000000006'::uuid),
  ('ab000001-0000-0000-0000-000000000007', 'oauth2', 'ed35311c-d4e3-495b-beaf-23bf13602701', 'cpatest6600@gmail.com',         'aa000001-0000-0000-0000-000000000007'::uuid)
) AS v(id, login_provider, provider_user_id, email, user_id)
WHERE NOT EXISTS (
  SELECT 1 FROM login l WHERE l.provider_user_id ILIKE v.provider_user_id
);

-- ─── User Roles ──────────────────────────────────────────────────────────────
INSERT INTO user_role (id, user_id, role, is_active)
VALUES
  ('ac000001-0000-0000-0000-000000000001', 'aa000001-0000-0000-0000-000000000001', 'ADMIN',            true),
  ('ac000001-0000-0000-0000-000000000002', 'aa000001-0000-0000-0000-000000000002', 'CLIENT_ADMIN',     true),
  ('ac000001-0000-0000-0000-000000000003', 'aa000001-0000-0000-0000-000000000003', 'CLIENT_ADMIN',     true),
  ('ac000001-0000-0000-0000-000000000004', 'aa000001-0000-0000-0000-000000000004', 'TENANT_ADMIN',     true),
  ('ac000001-0000-0000-0000-000000000005', 'aa000001-0000-0000-0000-000000000005', 'TENANT_ADMIN',     true),
  ('ac000001-0000-0000-0000-000000000006', 'aa000001-0000-0000-0000-000000000006', 'SUB_TENANT_ADMIN', true),
  ('ac000001-0000-0000-0000-000000000007', 'aa000001-0000-0000-0000-000000000007', 'SUB_TENANT_ADMIN', true)
ON CONFLICT (user_id) DO NOTHING;

-- ─── Clients ─────────────────────────────────────────────────────────────────
INSERT INTO clients (id, name, plan, is_active)
VALUES
  ('ba000001-0000-0000-0000-000000000001', 'CPA Downtown Parking', 'PREMIUM',  true),
  ('ba000001-0000-0000-0000-000000000002', 'CPA Airport Parking',  'STANDARD', true)
ON CONFLICT (id) DO NOTHING;

-- ─── Tenants ─────────────────────────────────────────────────────────────────
-- tenant_id column must equal id (Hibernate filter requirement)
INSERT INTO tenants (id, client_id, tenant_id, name, settings, is_active)
VALUES
  (
    'bb000001-0000-0000-0000-000000000001',
    'ba000001-0000-0000-0000-000000000001',
    'bb000001-0000-0000-0000-000000000001',
    'Downtown Parkade',
    '{"branding":{"logoUrl":null,"primaryColor":"#1B4F8A","accentColor":"#F5A623"}}',
    true
  ),
  (
    'bb000001-0000-0000-0000-000000000002',
    'ba000001-0000-0000-0000-000000000002',
    'bb000001-0000-0000-0000-000000000002',
    'Airport Long-Term Lot',
    '{"branding":{"logoUrl":null,"primaryColor":"#2E7D32","accentColor":"#FFC107"}}',
    true
  )
ON CONFLICT (id) DO NOTHING;

-- ─── Client Admin Assignments ────────────────────────────────────────────────
INSERT INTO client_admin_assignments (id, user_id, client_id, tenant_id)
VALUES
  ('bc000001-0000-0000-0000-000000000001', 'aa000001-0000-0000-0000-000000000002', 'ba000001-0000-0000-0000-000000000001', 'bb000001-0000-0000-0000-000000000001'),
  ('bc000001-0000-0000-0000-000000000002', 'aa000001-0000-0000-0000-000000000003', 'ba000001-0000-0000-0000-000000000002', 'bb000001-0000-0000-0000-000000000002')
ON CONFLICT (user_id, tenant_id) DO NOTHING;

-- ─── Tenant Admin Assignments ────────────────────────────────────────────────
INSERT INTO tenant_admin_assignments (id, user_id, tenant_id)
VALUES
  ('bd000001-0000-0000-0000-000000000001', 'aa000001-0000-0000-0000-000000000004', 'bb000001-0000-0000-0000-000000000001'),
  ('bd000001-0000-0000-0000-000000000002', 'aa000001-0000-0000-0000-000000000005', 'bb000001-0000-0000-0000-000000000002')
ON CONFLICT (user_id) DO NOTHING;

-- ─── Sub-Tenants ─────────────────────────────────────────────────────────────
INSERT INTO sub_tenants (id, tenant_id, client_id, name, is_active)
VALUES
  ('be000001-0000-0000-0000-000000000001', 'bb000001-0000-0000-0000-000000000001', 'ba000001-0000-0000-0000-000000000001', 'Level B1 Commuters',  true),
  ('be000001-0000-0000-0000-000000000002', 'bb000001-0000-0000-0000-000000000001', 'ba000001-0000-0000-0000-000000000001', 'Ground Floor Retail', true)
ON CONFLICT (id) DO NOTHING;

-- ─── Sub-Tenant Admin Assignments ────────────────────────────────────────────
INSERT INTO sub_tenant_admin_assignments (id, user_id, tenant_id, sub_tenant_id)
VALUES
  ('bf000001-0000-0000-0000-000000000001', 'aa000001-0000-0000-0000-000000000006', 'bb000001-0000-0000-0000-000000000001', 'be000001-0000-0000-0000-000000000001'),
  ('bf000001-0000-0000-0000-000000000002', 'aa000001-0000-0000-0000-000000000007', 'bb000001-0000-0000-0000-000000000001', 'be000001-0000-0000-0000-000000000002')
ON CONFLICT (user_id) DO NOTHING;

-- ─── Zones ───────────────────────────────────────────────────────────────────
INSERT INTO zones (id, tenant_id, client_id, zone_number, name, default_duration_minutes, max_duration_minutes, is_active)
VALUES
  -- Downtown Parkade
  ('ca000001-0000-0000-0000-000000000001', 'bb000001-0000-0000-0000-000000000001', 'ba000001-0000-0000-0000-000000000001', 'A',  'Level B1 East',       60,  480,  true),
  ('ca000001-0000-0000-0000-000000000002', 'bb000001-0000-0000-0000-000000000001', 'ba000001-0000-0000-0000-000000000001', 'B',  'Level B1 West',       60,  480,  true),
  ('ca000001-0000-0000-0000-000000000003', 'bb000001-0000-0000-0000-000000000001', 'ba000001-0000-0000-0000-000000000001', 'C',  'Ground Floor Retail', 120, 720,  true),
  -- Airport Long-Term Lot
  ('ca000001-0000-0000-0000-000000000004', 'bb000001-0000-0000-0000-000000000002', 'ba000001-0000-0000-0000-000000000002', 'P1', 'Short Stay',          60,  480,  true),
  ('ca000001-0000-0000-0000-000000000005', 'bb000001-0000-0000-0000-000000000002', 'ba000001-0000-0000-0000-000000000002', 'P2', 'Long Stay',           480, 2880, true)
ON CONFLICT (id) DO NOTHING;

-- ─── Zone Allocations ────────────────────────────────────────────────────────
-- Downtown: total=3, 1 direct (Zone B), 2 for sub-tenants (A + C)
-- Airport:  total=2, both direct
INSERT INTO zone_allocations (id, tenant_id, client_id, total_zones, tenant_direct, sub_tenant)
VALUES
  ('cb000001-0000-0000-0000-000000000001', 'bb000001-0000-0000-0000-000000000001', 'ba000001-0000-0000-0000-000000000001', 3, 1, 2),
  ('cb000001-0000-0000-0000-000000000002', 'bb000001-0000-0000-0000-000000000002', 'ba000001-0000-0000-0000-000000000002', 2, 2, 0)
ON CONFLICT (tenant_id) DO NOTHING;

-- ─── Sub-Tenant Zone Assignments ─────────────────────────────────────────────
-- Level B1 Commuters  → Zone A
-- Ground Floor Retail → Zone C
INSERT INTO sub_tenant_zone_assignments (id, sub_tenant_id, zone_id)
VALUES
  ('cc000001-0000-0000-0000-000000000001', 'be000001-0000-0000-0000-000000000001', 'ca000001-0000-0000-0000-000000000001'),
  ('cc000001-0000-0000-0000-000000000002', 'be000001-0000-0000-0000-000000000002', 'ca000001-0000-0000-0000-000000000003')
ON CONFLICT (sub_tenant_id, zone_id) DO NOTHING;

-- ─── Quota Configs ───────────────────────────────────────────────────────────
INSERT INTO quota_configs (id, tenant_id, client_id, sub_tenant_id, scope, period, max_count)
VALUES
  -- Downtown Parkade — tenant-level
  ('cd000001-0000-0000-0000-000000000001', 'bb000001-0000-0000-0000-000000000001', 'ba000001-0000-0000-0000-000000000001', NULL,                                   'TENANT',    'DAY',   100),
  ('cd000001-0000-0000-0000-000000000002', 'bb000001-0000-0000-0000-000000000001', 'ba000001-0000-0000-0000-000000000001', NULL,                                   'TENANT',    'WEEK',  500),
  ('cd000001-0000-0000-0000-000000000003', 'bb000001-0000-0000-0000-000000000001', 'ba000001-0000-0000-0000-000000000001', NULL,                                   'TENANT',    'MONTH', 2000),
  -- Downtown Parkade — sub-tenant-level
  ('cd000001-0000-0000-0000-000000000004', 'bb000001-0000-0000-0000-000000000001', 'ba000001-0000-0000-0000-000000000001', 'be000001-0000-0000-0000-000000000001', 'SUBTENANT', 'DAY',   40),
  ('cd000001-0000-0000-0000-000000000005', 'bb000001-0000-0000-0000-000000000001', 'ba000001-0000-0000-0000-000000000001', 'be000001-0000-0000-0000-000000000002', 'SUBTENANT', 'DAY',   60),
  -- Airport Long-Term Lot — tenant-level
  ('cd000001-0000-0000-0000-000000000006', 'bb000001-0000-0000-0000-000000000002', 'ba000001-0000-0000-0000-000000000002', NULL,                                   'TENANT',    'DAY',   200),
  ('cd000001-0000-0000-0000-000000000007', 'bb000001-0000-0000-0000-000000000002', 'ba000001-0000-0000-0000-000000000002', NULL,                                   'TENANT',    'WEEK',  800),
  ('cd000001-0000-0000-0000-000000000008', 'bb000001-0000-0000-0000-000000000002', 'ba000001-0000-0000-0000-000000000002', NULL,                                   'TENANT',    'MONTH', 3000)
ON CONFLICT (id) DO NOTHING;

-- ─── Validation Links (QR demo) ──────────────────────────────────────────────
INSERT INTO validation_links (id, tenant_id, client_id, sub_tenant_id, zone_id, link_type, token, label, default_duration_minutes, is_active, scan_count)
VALUES
  (
    'ce000001-0000-0000-0000-000000000001',
    'bb000001-0000-0000-0000-000000000001',
    'ba000001-0000-0000-0000-000000000001',
    'be000001-0000-0000-0000-000000000001',
    'ca000001-0000-0000-0000-000000000001',
    'QR', 'demo-qr-downtown-zone-a-b1-commuters', 'Zone A — B1 Commuters QR', 60, true, 12
  ),
  (
    'ce000001-0000-0000-0000-000000000002',
    'bb000001-0000-0000-0000-000000000001',
    'ba000001-0000-0000-0000-000000000001',
    NULL,
    'ca000001-0000-0000-0000-000000000002',
    'QR', 'demo-qr-downtown-zone-b-direct', 'Zone B — Tenant Direct QR', 60, true, 5
  ),
  (
    'ce000001-0000-0000-0000-000000000003',
    'bb000001-0000-0000-0000-000000000002',
    'ba000001-0000-0000-0000-000000000002',
    NULL,
    'ca000001-0000-0000-0000-000000000004',
    'QR', 'demo-qr-airport-zone-p1-shortstay', 'Zone P1 — Short Stay QR', 60, true, 28
  )
ON CONFLICT (token) DO NOTHING;

-- ─── Sample Validation Sessions ──────────────────────────────────────────────
INSERT INTO validation_sessions (id, tenant_id, client_id, sub_tenant_id, zone_id, license_plate, start_time, end_time, status, extended_count)
VALUES
  -- Downtown — Zone A — active
  ('cf000001-0000-0000-0000-000000000001', 'bb000001-0000-0000-0000-000000000001', 'ba000001-0000-0000-0000-000000000001', 'be000001-0000-0000-0000-000000000001', 'ca000001-0000-0000-0000-000000000001', 'ABC1234', now() - interval '30 minutes', NULL,                       'ACTIVE',   0),
  ('cf000001-0000-0000-0000-000000000002', 'bb000001-0000-0000-0000-000000000001', 'ba000001-0000-0000-0000-000000000001', 'be000001-0000-0000-0000-000000000001', 'ca000001-0000-0000-0000-000000000001', 'DEF5678', now() - interval '15 minutes', NULL,                       'ACTIVE',   0),
  -- Downtown — Zone B — direct tenant, expired
  ('cf000001-0000-0000-0000-000000000003', 'bb000001-0000-0000-0000-000000000001', 'ba000001-0000-0000-0000-000000000001', NULL,                                   'ca000001-0000-0000-0000-000000000002', 'GHI9012', now() - interval '3 hours',   now() - interval '2 hours', 'EXPIRED',  0),
  ('cf000001-0000-0000-0000-000000000004', 'bb000001-0000-0000-0000-000000000001', 'ba000001-0000-0000-0000-000000000001', NULL,                                   'ca000001-0000-0000-0000-000000000002', 'JKL3456', now() - interval '5 hours',   now() - interval '4 hours', 'EXPIRED',  1),
  -- Downtown — Zone C — Ground Floor Retail
  ('cf000001-0000-0000-0000-000000000005', 'bb000001-0000-0000-0000-000000000001', 'ba000001-0000-0000-0000-000000000001', 'be000001-0000-0000-0000-000000000002', 'ca000001-0000-0000-0000-000000000003', 'MNO7890', now() - interval '1 hour',    NULL,                       'ACTIVE',   0),
  -- Airport — Zone P1
  ('cf000001-0000-0000-0000-000000000006', 'bb000001-0000-0000-0000-000000000002', 'ba000001-0000-0000-0000-000000000002', NULL,                                   'ca000001-0000-0000-0000-000000000004', 'YYC1111', now() - interval '4 hours',   now() - interval '3 hours', 'EXPIRED',  0),
  ('cf000001-0000-0000-0000-000000000007', 'bb000001-0000-0000-0000-000000000002', 'ba000001-0000-0000-0000-000000000002', NULL,                                   'ca000001-0000-0000-0000-000000000004', 'YYC2222', now() - interval '45 minutes',NULL,                       'ACTIVE',   0),
  -- Airport — Zone P2 — extended
  ('cf000001-0000-0000-0000-000000000008', 'bb000001-0000-0000-0000-000000000002', 'ba000001-0000-0000-0000-000000000002', NULL,                                   'ca000001-0000-0000-0000-000000000005', 'YYC3333', now() - interval '10 hours',  NULL,                       'EXTENDED', 1)
ON CONFLICT (id) DO NOTHING;
