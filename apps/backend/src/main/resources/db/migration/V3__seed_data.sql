-- ─── Client ───────────────────────────────────────────────────────────────────
INSERT INTO clients (id, name, plan, is_active, settings)
VALUES (
    '11111111-1111-1111-1111-111111111111',
    'CPA Demo Client',
    'ENTERPRISE',
    true,
    '{}'
);

-- ─── Tenants ──────────────────────────────────────────────────────────────────
INSERT INTO tenants (id, client_id, tenant_id, name, is_active, settings)
VALUES (
    '22222222-2222-2222-2222-222222222222',
    '11111111-1111-1111-1111-111111111111',
    '22222222-2222-2222-2222-222222222222',
    'Downtown Parking',
    true,
    '{"branding":{"logoUrl":"","primaryColor":"#1B4F8A","accentColor":"#2E86C1"}}'
);

INSERT INTO tenants (id, client_id, tenant_id, name, is_active, settings)
VALUES (
    '33333333-3333-3333-3333-333333333333',
    '11111111-1111-1111-1111-111111111111',
    '33333333-3333-3333-3333-333333333333',
    'Airport Parking',
    true,
    '{"branding":{"logoUrl":"","primaryColor":"#117A65","accentColor":"#1ABC9C"}}'
);

-- ─── Sub-Tenants — Downtown Parking ───────────────────────────────────────────
INSERT INTO sub_tenants (id, tenant_id, client_id, name, is_active)
VALUES (
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    '22222222-2222-2222-2222-222222222222',
    '11111111-1111-1111-1111-111111111111',
    'Level 1 Retail',
    true
);

INSERT INTO sub_tenants (id, tenant_id, client_id, name, is_active)
VALUES (
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaab',
    '22222222-2222-2222-2222-222222222222',
    '11111111-1111-1111-1111-111111111111',
    'Level 2 Office',
    true
);

-- ─── Sub-Tenants — Airport Parking ────────────────────────────────────────────
INSERT INTO sub_tenants (id, tenant_id, client_id, name, is_active)
VALUES (
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    '33333333-3333-3333-3333-333333333333',
    '11111111-1111-1111-1111-111111111111',
    'Terminal A',
    true
);

INSERT INTO sub_tenants (id, tenant_id, client_id, name, is_active)
VALUES (
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2',
    '33333333-3333-3333-3333-333333333333',
    '11111111-1111-1111-1111-111111111111',
    'Terminal B',
    true
);

-- ─── Zones — Downtown Parking (3 zones, different durations) ──────────────────
INSERT INTO zones (id, tenant_id, client_id, zone_number, name,
                   default_duration_minutes, max_duration_minutes, is_active)
VALUES (
    'cccccccc-cccc-cccc-cccc-cccccccccccc',
    '22222222-2222-2222-2222-222222222222',
    '11111111-1111-1111-1111-111111111111',
    'A1',
    'Ground Floor — Retail',
    60,
    480,
    true
);

INSERT INTO zones (id, tenant_id, client_id, zone_number, name,
                   default_duration_minutes, max_duration_minutes, is_active)
VALUES (
    'cccccccc-cccc-cccc-cccc-cccccccccccd',
    '22222222-2222-2222-2222-222222222222',
    '11111111-1111-1111-1111-111111111111',
    'B1',
    'Level 2 — Office',
    120,
    720,
    true
);

INSERT INTO zones (id, tenant_id, client_id, zone_number, name,
                   default_duration_minutes, max_duration_minutes, is_active)
VALUES (
    'cccccccc-cccc-cccc-cccc-ccccccccccce',
    '22222222-2222-2222-2222-222222222222',
    '11111111-1111-1111-1111-111111111111',
    'ROOF',
    'Rooftop Parking',
    240,
    1440,
    true
);

-- ─── Zones — Airport Parking (3 zones, different durations) ───────────────────
INSERT INTO zones (id, tenant_id, client_id, zone_number, name,
                   default_duration_minutes, max_duration_minutes, is_active)
VALUES (
    'dddddddd-dddd-dddd-dddd-dddddddddddd',
    '33333333-3333-3333-3333-333333333333',
    '11111111-1111-1111-1111-111111111111',
    'P1',
    'Short Term Parking',
    60,
    480,
    true
);

INSERT INTO zones (id, tenant_id, client_id, zone_number, name,
                   default_duration_minutes, max_duration_minutes, is_active)
VALUES (
    'dddddddd-dddd-dddd-dddd-ddddddddddde',
    '33333333-3333-3333-3333-333333333333',
    '11111111-1111-1111-1111-111111111111',
    'P2',
    'Long Term Parking',
    480,
    2880,
    true
);

INSERT INTO zones (id, tenant_id, client_id, zone_number, name,
                   default_duration_minutes, max_duration_minutes, is_active)
VALUES (
    'dddddddd-dddd-dddd-dddd-dddddddddddf',
    '33333333-3333-3333-3333-333333333333',
    '11111111-1111-1111-1111-111111111111',
    'VIP',
    'VIP Parking',
    120,
    1440,
    true
);

-- ─── Quota Configs — Downtown Parking ─────────────────────────────────────────
INSERT INTO quota_configs (id, tenant_id, client_id, scope, period, max_count)
VALUES (
    'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee',
    '22222222-2222-2222-2222-222222222222',
    '11111111-1111-1111-1111-111111111111',
    'TENANT',
    'DAY',
    500
);

INSERT INTO quota_configs (id, tenant_id, client_id, scope, period, max_count)
VALUES (
    'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee2',
    '22222222-2222-2222-2222-222222222222',
    '11111111-1111-1111-1111-111111111111',
    'TENANT',
    'WEEK',
    2000
);

-- ─── Quota Configs — Airport Parking ──────────────────────────────────────────
INSERT INTO quota_configs (id, tenant_id, client_id, scope, period, max_count)
VALUES (
    'ffffffff-ffff-ffff-ffff-ffffffffffff',
    '33333333-3333-3333-3333-333333333333',
    '11111111-1111-1111-1111-111111111111',
    'TENANT',
    'DAY',
    500
);

INSERT INTO quota_configs (id, tenant_id, client_id, scope, period, max_count)
VALUES (
    'ffffffff-ffff-ffff-ffff-fffffffffffe',
    '33333333-3333-3333-3333-333333333333',
    '11111111-1111-1111-1111-111111111111',
    'TENANT',
    'WEEK',
    2000
);
