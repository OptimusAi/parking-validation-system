-- Removes all data inserted by the now-deleted V3__seed_data.sql.
-- Safe to run even if V3 was never applied (DELETE WHERE id IN ... will just affect 0 rows).

-- ─── Quota configs ────────────────────────────────────────────────────────────
DELETE FROM quota_configs WHERE id IN (
    'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee',
    'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee2',
    'ffffffff-ffff-ffff-ffff-ffffffffffff',
    'ffffffff-ffff-ffff-ffff-fffffffffffe'
);

-- ─── Zones ────────────────────────────────────────────────────────────────────
DELETE FROM zones WHERE id IN (
    'cccccccc-cccc-cccc-cccc-cccccccccccc',
    'cccccccc-cccc-cccc-cccc-cccccccccccd',
    'cccccccc-cccc-cccc-cccc-ccccccccccce',
    'dddddddd-dddd-dddd-dddd-dddddddddddd',
    'dddddddd-dddd-dddd-dddd-ddddddddddde',
    'dddddddd-dddd-dddd-dddd-dddddddddddf'
);

-- ─── Sub-tenants ──────────────────────────────────────────────────────────────
DELETE FROM sub_tenants WHERE id IN (
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaab',
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2'
);

-- ─── Tenants ──────────────────────────────────────────────────────────────────
DELETE FROM tenants WHERE id IN (
    '22222222-2222-2222-2222-222222222222',
    '33333333-3333-3333-3333-333333333333'
);

-- ─── Clients ──────────────────────────────────────────────────────────────────
DELETE FROM clients WHERE id = '11111111-1111-1111-1111-111111111111';
