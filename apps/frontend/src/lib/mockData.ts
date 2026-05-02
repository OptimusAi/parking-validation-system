import { subDays, subHours, addHours, addMinutes, format } from 'date-fns';
import type {
  Client, Tenant, SubTenant, Zone, ValidationSession,
  ValidationLink, ReportJob, AuditLog, User, QuotaUsage,
} from './types';

// ─── Clients ──────────────────────────────────────────────────────────────────
export const MOCK_CLIENTS: Client[] = [
  { id: '11111111-1111-1111-1111-111111111111', name: 'CPA Demo Client' },
];

// ─── Tenants ─────────────────────────────────────────────────────────────────
export const MOCK_TENANTS: Tenant[] = [
  {
    id: '22222222-2222-2222-2222-222222222222',
    name: 'Downtown Parking',
    clientId: '11111111-1111-1111-1111-111111111111',
    branding: { logoUrl: null, primaryColor: '#1B4F8A', accentColor: '#2E86C1' },
    zones: 3,
    subTenants: 2,
    status: 'ACTIVE',
  },
  {
    id: '33333333-3333-3333-3333-333333333333',
    name: 'Airport Parking',
    clientId: '11111111-1111-1111-1111-111111111111',
    branding: { logoUrl: null, primaryColor: '#117A65', accentColor: '#1ABC9C' },
    zones: 2,
    subTenants: 1,
    status: 'ACTIVE',
  },
];

// ─── Sub-Tenants ──────────────────────────────────────────────────────────────
export const MOCK_SUB_TENANTS: SubTenant[] = [
  { id: 'sub-001', name: 'Level 1 Retail', tenantId: '22222222-2222-2222-2222-222222222222', isActive: true, sessionsToday: 12, quotaUsed: 48 },
  { id: 'sub-002', name: 'Level 2 Office', tenantId: '22222222-2222-2222-2222-222222222222', isActive: true, sessionsToday: 8, quotaUsed: 32 },
  { id: 'sub-003', name: 'Terminal A', tenantId: '33333333-3333-3333-3333-333333333333', isActive: true, sessionsToday: 22, quotaUsed: 71 },
];

// ─── Zones ────────────────────────────────────────────────────────────────────
export const MOCK_ZONES: Zone[] = [
  { id: 'zone-001', zoneNumber: 'A', name: 'Ground Floor', defaultDurationMinutes: 60, maxDurationMinutes: 480, activeSessions: 8 },
  { id: 'zone-002', zoneNumber: 'B', name: 'Upper Level', defaultDurationMinutes: 120, maxDurationMinutes: 720, activeSessions: 5 },
  { id: 'zone-003', zoneNumber: 'ROOF', name: 'Rooftop', defaultDurationMinutes: 240, maxDurationMinutes: 1440, activeSessions: 5 },
];

// ─── Validation Sessions ──────────────────────────────────────────────────────
const plates = ['ABC123', 'XYZ789', 'DEF456', 'GHI012', 'JKL345', 'MNO678', 'PQR901', 'STU234', 'VWX567', 'YZA890'];
const statuses: ValidationSession['status'][] = ['ACTIVE', 'EXTENDED', 'CANCELLED', 'EXPIRED'];
const zoneAssignments = [
  { id: 'zone-001', name: 'Ground Floor' },
  { id: 'zone-002', name: 'Upper Level' },
  { id: 'zone-003', name: 'Rooftop' },
];

function makeSession(index: number): ValidationSession {
  const daysBack = Math.floor(index / 3);
  const start = subHours(subDays(new Date(), daysBack), index % 8);
  const duration = [60, 120, 240][index % 3];
  const status = index < 4 ? 'ACTIVE' : statuses[index % 4];
  const zone = zoneAssignments[index % 3];
  return {
    id: `sess-${String(index + 1).padStart(3, '0')}`,
    tenantId: '22222222-2222-2222-2222-222222222222',
    subTenantId: index % 3 === 0 ? 'sub-001' : index % 3 === 1 ? 'sub-002' : undefined,
    subTenantName: index % 3 === 0 ? 'Level 1 Retail' : index % 3 === 1 ? 'Level 2 Office' : undefined,
    zoneId: zone.id,
    zoneName: zone.name,
    licensePlate: plates[index % plates.length],
    startTime: start.toISOString(),
    endTime: addMinutes(start, duration).toISOString(),
    durationMinutes: duration,
    status: status === 'ACTIVE' && addMinutes(start, duration) < new Date() ? 'EXPIRED' : status,
  };
}

export const MOCK_SESSIONS: ValidationSession[] = Array.from({ length: 20 }, (_, i) => makeSession(i));

// ─── Validation Links ─────────────────────────────────────────────────────────
export const MOCK_LINKS: ValidationLink[] = [
  {
    id: 'link-001', tenantId: '22222222-2222-2222-2222-222222222222',
    zoneId: 'zone-001', zoneName: 'Ground Floor',
    label: 'Main Entrance QR', type: 'QR', token: 'tok-abc-001',
    url: 'https://tms.cpa.ca/validate/tok-abc-001',
    durationMinutes: 60, scans: 142, isActive: true,
    createdAt: subDays(new Date(), 30).toISOString(),
  },
  {
    id: 'link-002', tenantId: '22222222-2222-2222-2222-222222222222',
    zoneId: 'zone-002', zoneName: 'Upper Level',
    label: 'Office Reception', type: 'URL', token: 'tok-def-002',
    url: 'https://tms.cpa.ca/validate/tok-def-002',
    durationMinutes: 120, scans: 87, isActive: true,
    expiresAt: addHours(new Date(), 72).toISOString(),
    createdAt: subDays(new Date(), 14).toISOString(),
  },
  {
    id: 'link-003', tenantId: '22222222-2222-2222-2222-222222222222',
    zoneId: 'zone-003', zoneName: 'Rooftop',
    label: 'Event Parking', type: 'QR', token: 'tok-ghi-003',
    url: 'https://tms.cpa.ca/validate/tok-ghi-003',
    durationMinutes: 240, scans: 33, isActive: false,
    createdAt: subDays(new Date(), 60).toISOString(),
  },
  {
    id: 'link-004', tenantId: '22222222-2222-2222-2222-222222222222',
    zoneId: 'zone-001', zoneName: 'Ground Floor',
    label: 'Side Entrance', type: 'QR', token: 'tok-jkl-004',
    url: 'https://tms.cpa.ca/validate/tok-jkl-004',
    durationMinutes: 60, scans: 201, isActive: true,
    createdAt: subDays(new Date(), 45).toISOString(),
  },
  {
    id: 'link-005', tenantId: '22222222-2222-2222-2222-222222222222',
    zoneId: 'zone-002', zoneName: 'Upper Level',
    label: 'Weekend Access', type: 'URL', token: 'tok-mno-005',
    url: 'https://tms.cpa.ca/validate/tok-mno-005',
    durationMinutes: 180, scans: 15, isActive: true,
    expiresAt: addHours(new Date(), 168).toISOString(),
    createdAt: subDays(new Date(), 7).toISOString(),
  },
];

// ─── Report Jobs ─────────────────────────────────────────────────────────────
export const MOCK_REPORTS: ReportJob[] = [
  {
    id: 'rpt-001', tenantId: '22222222-2222-2222-2222-222222222222',
    type: 'VALIDATION_SESSIONS', format: 'CSV', status: 'COMPLETED',
    requestedAt: subHours(new Date(), 5).toISOString(),
    completedAt: subHours(new Date(), 4).toISOString(),
    fileUrl: 'https://example.com/reports/rpt-001.csv',
    dateFrom: subDays(new Date(), 7).toISOString(),
    dateTo: new Date().toISOString(),
  },
  {
    id: 'rpt-002', tenantId: '22222222-2222-2222-2222-222222222222',
    type: 'QUOTA_USAGE', format: 'EXCEL', status: 'PROCESSING',
    requestedAt: subHours(new Date(), 1).toISOString(),
    dateFrom: subDays(new Date(), 30).toISOString(),
    dateTo: new Date().toISOString(),
  },
  {
    id: 'rpt-003', tenantId: '22222222-2222-2222-2222-222222222222',
    type: 'ZONE_SUMMARY', format: 'PDF', status: 'FAILED',
    requestedAt: subDays(new Date(), 1).toISOString(),
    dateFrom: subDays(new Date(), 14).toISOString(),
    dateTo: subDays(new Date(), 1).toISOString(),
  },
];

// ─── Audit Logs ───────────────────────────────────────────────────────────────
export const MOCK_AUDIT_LOGS: AuditLog[] = [
  {
    id: 'audit-001', tenantId: '22222222-2222-2222-2222-222222222222',
    actorEmail: 'admin@cpa.com', action: 'SESSION_CREATED',
    entityType: 'ValidationSession', entityId: 'sess-001',
    ipAddress: '192.168.1.10', timestamp: subHours(new Date(), 2).toISOString(),
    before: {}, after: { licensePlate: 'ABC123', status: 'ACTIVE' },
  },
  {
    id: 'audit-002', tenantId: '22222222-2222-2222-2222-222222222222',
    actorEmail: 'tenant@cpa.com', action: 'SESSION_EXTENDED',
    entityType: 'ValidationSession', entityId: 'sess-002',
    ipAddress: '10.0.0.5', timestamp: subHours(new Date(), 3).toISOString(),
    before: { endTime: subHours(new Date(), 4).toISOString() },
    after: { endTime: subHours(new Date(), 2).toISOString() },
  },
  {
    id: 'audit-003', tenantId: '22222222-2222-2222-2222-222222222222',
    actorEmail: 'admin@cpa.com', action: 'ROLE_CHANGED',
    entityType: 'User', entityId: 'user-003',
    ipAddress: '192.168.1.10', timestamp: subHours(new Date(), 6).toISOString(),
    before: { role: 'SUBTENANT_USER' }, after: { role: 'TENANT_ADMIN' },
  },
  {
    id: 'audit-004', tenantId: '22222222-2222-2222-2222-222222222222',
    actorEmail: 'tenant@cpa.com', action: 'LINK_CREATED',
    entityType: 'ValidationLink', entityId: 'link-004',
    ipAddress: '10.0.0.5', timestamp: subDays(new Date(), 1).toISOString(),
    before: {}, after: { label: 'Side Entrance', type: 'QR' },
  },
  {
    id: 'audit-005', tenantId: '22222222-2222-2222-2222-222222222222',
    actorEmail: 'client@cpa.com', action: 'BRANDING_UPDATED',
    entityType: 'Tenant', entityId: '22222222-2222-2222-2222-222222222222',
    ipAddress: '172.16.0.1', timestamp: subDays(new Date(), 2).toISOString(),
    before: { primaryColor: '#000000' }, after: { primaryColor: '#1B4F8A' },
  },
  {
    id: 'audit-006', tenantId: '22222222-2222-2222-2222-222222222222',
    actorEmail: 'admin@cpa.com', action: 'SESSION_CANCELLED',
    entityType: 'ValidationSession', entityId: 'sess-005',
    ipAddress: '192.168.1.10', timestamp: subDays(new Date(), 2).toISOString(),
    before: { status: 'ACTIVE' }, after: { status: 'CANCELLED' },
  },
  {
    id: 'audit-007', tenantId: '22222222-2222-2222-2222-222222222222',
    actorEmail: 'tenant@cpa.com', action: 'ZONE_CREATED',
    entityType: 'Zone', entityId: 'zone-003',
    ipAddress: '10.0.0.5', timestamp: subDays(new Date(), 3).toISOString(),
    before: {}, after: { zoneNumber: 'ROOF', name: 'Rooftop' },
  },
  {
    id: 'audit-008', tenantId: '22222222-2222-2222-2222-222222222222',
    actorEmail: 'admin@cpa.com', action: 'LINK_DEACTIVATED',
    entityType: 'ValidationLink', entityId: 'link-003',
    ipAddress: '192.168.1.10', timestamp: subDays(new Date(), 4).toISOString(),
    before: { isActive: true }, after: { isActive: false },
  },
  {
    id: 'audit-009', tenantId: '22222222-2222-2222-2222-222222222222',
    actorEmail: 'client@cpa.com', action: 'USER_UPDATED',
    entityType: 'User', entityId: 'user-004',
    ipAddress: '172.16.0.1', timestamp: subDays(new Date(), 5).toISOString(),
    before: { isActive: false }, after: { isActive: true },
  },
  {
    id: 'audit-010', tenantId: '22222222-2222-2222-2222-222222222222',
    actorEmail: 'tenant@cpa.com', action: 'SESSION_CREATED',
    entityType: 'ValidationSession', entityId: 'sess-010',
    ipAddress: '10.0.0.5', timestamp: subDays(new Date(), 6).toISOString(),
    before: {}, after: { licensePlate: 'PQR901', status: 'ACTIVE' },
  },
];

// ─── Users ────────────────────────────────────────────────────────────────────
export const MOCK_USERS: User[] = [
  { id: 'user-001', email: 'admin@cpa.com', name: 'Alex Admin', role: 'ADMIN', isActive: true },
  { id: 'user-002', email: 'client@cpa.com', name: 'Claire Client', role: 'CLIENT_ADMIN', tenantId: '11111111-1111-1111-1111-111111111111', tenantName: 'CPA Demo Client', isActive: true },
  { id: 'user-003', email: 'tenant@cpa.com', name: 'Tom Tenant', role: 'TENANT_ADMIN', tenantId: '22222222-2222-2222-2222-222222222222', tenantName: 'Downtown Parking', isActive: true },
  { id: 'user-004', email: 'subtenant@cpa.com', name: 'Sara Sub', role: 'SUBTENANT_USER', tenantId: '22222222-2222-2222-2222-222222222222', tenantName: 'Downtown Parking', isActive: true },
];

// ─── Quota ────────────────────────────────────────────────────────────────────
export const MOCK_QUOTA: QuotaUsage = {
  daily: { used: 342, limit: 500 },
  weekly: { used: 1240, limit: 2000 },
  monthly: { used: 3890, limit: 8000 },
  byZone: [
    { zoneId: 'zone-001', zoneName: 'Ground Floor', countToday: 158 },
    { zoneId: 'zone-002', zoneName: 'Upper Level', countToday: 112 },
    { zoneId: 'zone-003', zoneName: 'Rooftop', countToday: 72 },
  ],
  bySubTenant: [
    { subTenantId: 'sub-001', name: 'Level 1 Retail', count: 180, quotaUsed: 48 },
    { subTenantId: 'sub-002', name: 'Level 2 Office', count: 120, quotaUsed: 32 },
  ],
};

// ─── Chart Data ───────────────────────────────────────────────────────────────
export function getChartDataLast7Days() {
  return Array.from({ length: 7 }, (_, i) => {
    const date = subDays(new Date(), 6 - i);
    return {
      date: format(date, 'MMM d'),
      'Ground Floor': 20 + Math.floor(Math.random() * 40),
      'Upper Level': 15 + Math.floor(Math.random() * 30),
      'Rooftop': 5 + Math.floor(Math.random() * 20),
    };
  });
}

export function getChartDataLast30Days() {
  return Array.from({ length: 30 }, (_, i) => {
    const date = subDays(new Date(), 29 - i);
    return {
      date: format(date, 'MMM d'),
      validations: 30 + Math.floor(Math.random() * 60),
    };
  });
}
