/**
 * Mock API layer — every function simulates a real backend call.
 * To swap in a real backend, replace each function body with the commented apiClient call.
 *
 * Usage pattern:
 *   const apiClient = axios.create({ baseURL: process.env.NEXT_PUBLIC_API_URL });
 */

import { addMinutes } from 'date-fns';
import type {
  PageResponse, PageParams, ValidationSession, ValidationLink,
  ReportJob, AuditLog, User, Zone, Tenant, TenantBranding, QuotaUsage, SubTenant,
} from './types';
import {
  MOCK_SESSIONS, MOCK_LINKS, MOCK_REPORTS, MOCK_AUDIT_LOGS, MOCK_USERS,
  MOCK_ZONES, MOCK_TENANTS, MOCK_QUOTA, MOCK_SUB_TENANTS,
} from './mockData';

// Mutable copies so mutations work across the session
let sessions = [...MOCK_SESSIONS];
let links = [...MOCK_LINKS];
let reports = [...MOCK_REPORTS];
let users = [...MOCK_USERS];
let zones = [...MOCK_ZONES];
let tenants = [...MOCK_TENANTS];
let subTenants = [...MOCK_SUB_TENANTS];
const reportPollCounts: Record<string, number> = {};

const delay = (ms = 500) =>
  new Promise((r) => setTimeout(r, 300 + Math.random() * 500));

function paginate<T>(items: T[], params?: PageParams): PageResponse<T> {
  const page = params?.page ?? 0;
  const pageSize = params?.pageSize ?? 50;
  const start = page * pageSize;
  return {
    items: items.slice(start, start + pageSize),
    total: items.length,
    page,
    pageSize,
  };
}

// ─── Validations ─────────────────────────────────────────────────────────────

/** TODO: replace with apiClient.get('/api/v1/tenants/:tenantId/validations', { params }) */
export async function getValidations(params?: PageParams & { status?: string; zoneId?: string; plate?: string }): Promise<PageResponse<ValidationSession>> {
  await delay();
  let filtered = [...sessions];
  if (params?.status) filtered = filtered.filter((s) => s.status === params.status);
  if (params?.zoneId) filtered = filtered.filter((s) => s.zoneId === params.zoneId);
  if (params?.plate) filtered = filtered.filter((s) => s.licensePlate.includes(params.plate!.toUpperCase()));
  return paginate(filtered, params);
}

/** TODO: replace with apiClient.post('/api/v1/tenants/:tenantId/validations') */
export async function createSession(data: {
  licensePlate: string;
  zoneId: string;
  zoneName: string;
  durationMinutes: number;
  subTenantId?: string;
}): Promise<ValidationSession> {
  await delay();
  const now = new Date();
  const session: ValidationSession = {
    id: `sess-${Date.now()}`,
    tenantId: '22222222-2222-2222-2222-222222222222',
    subTenantId: data.subTenantId,
    zoneId: data.zoneId,
    zoneName: data.zoneName,
    licensePlate: data.licensePlate.toUpperCase(),
    startTime: now.toISOString(),
    endTime: addMinutes(now, data.durationMinutes).toISOString(),
    durationMinutes: data.durationMinutes,
    status: 'ACTIVE',
  };
  sessions = [session, ...sessions];
  return session;
}

/** TODO: replace with apiClient.post('/api/v1/validations/:id/extend') */
export async function extendSession(id: string, additionalMinutes: number): Promise<ValidationSession> {
  await delay();
  const idx = sessions.findIndex((s) => s.id === id);
  if (idx === -1) throw new Error('Session not found');
  const updated: ValidationSession = {
    ...sessions[idx],
    endTime: addMinutes(new Date(sessions[idx].endTime), additionalMinutes).toISOString(),
    durationMinutes: sessions[idx].durationMinutes + additionalMinutes,
    status: 'EXTENDED',
  };
  sessions = sessions.map((s) => (s.id === id ? updated : s));
  return updated;
}

/** TODO: replace with apiClient.post('/api/v1/validations/:id/cancel') */
export async function cancelSession(id: string): Promise<ValidationSession> {
  await delay();
  const idx = sessions.findIndex((s) => s.id === id);
  if (idx === -1) throw new Error('Session not found');
  const updated: ValidationSession = { ...sessions[idx], status: 'CANCELLED' };
  sessions = sessions.map((s) => (s.id === id ? updated : s));
  return updated;
}

// ─── Validation Links ─────────────────────────────────────────────────────────

/** TODO: replace with apiClient.get('/api/v1/tenants/:tenantId/links', { params }) */
export async function getLinks(params?: PageParams): Promise<PageResponse<ValidationLink>> {
  await delay();
  return paginate(links, params);
}

/** TODO: replace with apiClient.post('/api/v1/tenants/:tenantId/links') */
export async function createLink(data: {
  zoneId: string;
  zoneName: string;
  type: 'QR' | 'URL';
  durationMinutes: number;
  label?: string;
  expiresAt?: string;
}): Promise<ValidationLink> {
  await delay();
  const token = `tok-${Date.now()}`;
  const link: ValidationLink = {
    id: `link-${Date.now()}`,
    tenantId: '22222222-2222-2222-2222-222222222222',
    zoneId: data.zoneId,
    zoneName: data.zoneName,
    label: data.label,
    type: data.type,
    token,
    url: `https://tms.cpa.ca/validate/${token}`,
    durationMinutes: data.durationMinutes,
    scans: 0,
    expiresAt: data.expiresAt,
    isActive: true,
    createdAt: new Date().toISOString(),
  };
  links = [link, ...links];
  return link;
}

/** TODO: replace with apiClient.post('/api/v1/links/:id/deactivate') */
export async function deactivateLink(id: string): Promise<void> {
  await delay();
  links = links.map((l) => (l.id === id ? { ...l, isActive: false } : l));
}

/** TODO: replace with apiClient.get('/api/v1/links/:id/pdf', { responseType: 'blob' }) */
export async function generateQrPdf(id: string): Promise<Blob> {
  await delay(1500);
  // Return a minimal fake PDF blob
  const content = `%PDF-1.4 fake pdf for link ${id}`;
  return new Blob([content], { type: 'application/pdf' });
}

// ─── Public Validation ───────────────────────────────────────────────────────

/** TODO: replace with apiClient.get('/api/v1/public/validate/:token') */
export async function getPublicLink(token: string): Promise<ValidationLink> {
  await delay();
  const link = links.find((l) => l.token === token) ?? links[0];
  return link;
}

/** TODO: replace with apiClient.post('/api/v1/public/validate') */
export async function submitPublicValidation(token: string, licensePlate: string): Promise<{ validUntil: string; zoneName: string }> {
  await delay();
  if (licensePlate.toUpperCase() === 'TAKEN01') {
    throw Object.assign(new Error('Already validated'), { code: 'ALREADY_VALIDATED' });
  }
  if (licensePlate.toUpperCase() === 'QUOTA99') {
    throw Object.assign(new Error('Quota exceeded'), { code: 'QUOTA_EXCEEDED' });
  }
  const link = await getPublicLink(token);
  const validUntil = addMinutes(new Date(), link.durationMinutes);
  return { validUntil: validUntil.toISOString(), zoneName: link.zoneName };
}

// ─── Branding ─────────────────────────────────────────────────────────────────

/** TODO: replace with apiClient.get('/api/v1/tenants/:tenantId/branding') */
export async function getTenantBranding(tenantId: string): Promise<TenantBranding> {
  await delay();
  const tenant = tenants.find((t) => t.id === tenantId);
  return tenant?.branding ?? { logoUrl: null, primaryColor: '#1B4F8A', accentColor: '#2E86C1' };
}

/** TODO: replace with apiClient.put('/api/v1/tenants/:tenantId/branding') */
export async function updateBranding(tenantId: string, data: Partial<TenantBranding>): Promise<TenantBranding> {
  await delay();
  tenants = tenants.map((t) =>
    t.id === tenantId ? { ...t, branding: { ...t.branding, ...data } } : t
  );
  const tenant = tenants.find((t) => t.id === tenantId);
  return tenant!.branding;
}

// ─── Reports ─────────────────────────────────────────────────────────────────

/** TODO: replace with apiClient.get('/api/v1/tenants/:tenantId/reports', { params }) */
export async function getReports(params?: PageParams): Promise<PageResponse<ReportJob>> {
  await delay();
  return paginate(reports, params);
}

/** TODO: replace with apiClient.post('/api/v1/tenants/:tenantId/reports') */
export async function queueReport(data: { type: string; format: string; dateFrom?: string; dateTo?: string }): Promise<{ jobId: string }> {
  await delay();
  const jobId = `rpt-${Date.now()}`;
  const job: ReportJob = {
    id: jobId,
    tenantId: '22222222-2222-2222-2222-222222222222',
    type: data.type as ReportJob['type'],
    format: data.format as ReportJob['format'],
    status: 'QUEUED',
    requestedAt: new Date().toISOString(),
    dateFrom: data.dateFrom,
    dateTo: data.dateTo,
  };
  reports = [job, ...reports];
  reportPollCounts[jobId] = 0;
  return { jobId };
}

/** TODO: replace with apiClient.get('/api/v1/reports/:jobId') */
export async function getReportJob(jobId: string): Promise<ReportJob> {
  await delay();
  const count = (reportPollCounts[jobId] ?? 0) + 1;
  reportPollCounts[jobId] = count;
  const status = count <= 1 ? 'PROCESSING' : 'COMPLETED';
  reports = reports.map((r) =>
    r.id === jobId
      ? {
          ...r,
          status,
          completedAt: status === 'COMPLETED' ? new Date().toISOString() : undefined,
          fileUrl: status === 'COMPLETED' ? `https://example.com/reports/${jobId}.csv` : undefined,
        }
      : r
  );
  return reports.find((r) => r.id === jobId)!;
}

// ─── Audit Logs ───────────────────────────────────────────────────────────────

/** TODO: replace with apiClient.get('/api/v1/tenants/:tenantId/audit-logs', { params }) */
export async function getAuditLogs(params?: PageParams & { action?: string; actor?: string }): Promise<PageResponse<AuditLog>> {
  await delay();
  let filtered = [...MOCK_AUDIT_LOGS];
  if (params?.action) filtered = filtered.filter((l) => l.action === params.action);
  if (params?.actor) filtered = filtered.filter((l) => l.actorEmail.includes(params.actor!));
  return paginate(filtered, params);
}

// ─── Users ────────────────────────────────────────────────────────────────────

/** TODO: replace with apiClient.get('/api/v1/users', { params }) */
export async function getUsers(params?: PageParams): Promise<PageResponse<User>> {
  await delay();
  return paginate(users, params);
}

/** TODO: replace with apiClient.put('/api/v1/users/:id/role') */
export async function updateUserRole(id: string, role: User['role']): Promise<User> {
  await delay();
  users = users.map((u) => (u.id === id ? { ...u, role } : u));
  return users.find((u) => u.id === id)!;
}

/** TODO: replace with apiClient.put('/api/v1/users/:id') */
export async function updateUserActive(id: string, isActive: boolean): Promise<User> {
  await delay();
  users = users.map((u) => (u.id === id ? { ...u, isActive } : u));
  return users.find((u) => u.id === id)!;
}

// ─── Quota ────────────────────────────────────────────────────────────────────

/** TODO: replace with apiClient.get('/api/v1/tenants/:tenantId/quota') */
export async function getQuotaUsage(tenantId: string): Promise<QuotaUsage> {
  await delay();
  void tenantId;
  return MOCK_QUOTA;
}

// ─── Zones ────────────────────────────────────────────────────────────────────

/** TODO: replace with apiClient.get('/api/v1/tenants/:tenantId/zones') */
export async function getZones(): Promise<Zone[]> {
  await delay();
  return zones;
}

/** TODO: replace with apiClient.post('/api/v1/tenants/:tenantId/zones') */
export async function createZone(data: Omit<Zone, 'id' | 'activeSessions'>): Promise<Zone> {
  await delay();
  const zone: Zone = { ...data, id: `zone-${Date.now()}`, activeSessions: 0 };
  zones = [...zones, zone];
  return zone;
}

/** TODO: replace with apiClient.put('/api/v1/zones/:id') */
export async function updateZone(id: string, data: Partial<Zone>): Promise<Zone> {
  await delay();
  zones = zones.map((z) => (z.id === id ? { ...z, ...data } : z));
  return zones.find((z) => z.id === id)!;
}

/** TODO: replace with apiClient.delete('/api/v1/zones/:id') */
export async function deleteZone(id: string): Promise<void> {
  await delay();
  const zone = zones.find((z) => z.id === id);
  if (zone?.activeSessions && zone.activeSessions > 0) {
    throw new Error('Cannot delete zone with active sessions');
  }
  zones = zones.filter((z) => z.id !== id);
}

// ─── Tenants ─────────────────────────────────────────────────────────────────

/** TODO: replace with apiClient.get('/api/v1/clients/:clientId/tenants') */
export async function getTenants(): Promise<Tenant[]> {
  await delay();
  return tenants;
}

/** TODO: replace with apiClient.post('/api/v1/clients/:clientId/tenants') */
export async function createTenant(data: { name: string; primaryColor: string; accentColor: string }): Promise<Tenant> {
  await delay();
  const tenant: Tenant = {
    id: `tenant-${Date.now()}`,
    name: data.name,
    clientId: '11111111-1111-1111-1111-111111111111',
    branding: { logoUrl: null, primaryColor: data.primaryColor, accentColor: data.accentColor },
    zones: 0,
    subTenants: 0,
    status: 'ACTIVE',
  };
  tenants = [...tenants, tenant];
  return tenant;
}

// ─── Sub-Tenants ──────────────────────────────────────────────────────────────

/** TODO: replace with apiClient.get('/api/v1/tenants/:tenantId/sub-tenants') */
export async function getSubTenants(tenantId: string): Promise<SubTenant[]> {
  await delay();
  return subTenants.filter((s) => s.tenantId === tenantId);
}

/** TODO: replace with apiClient.post('/api/v1/tenants/:tenantId/sub-tenants') */
export async function createSubTenant(data: { name: string; tenantId: string }): Promise<SubTenant> {
  await delay();
  const sub: SubTenant = {
    id: `sub-${Date.now()}`,
    name: data.name,
    tenantId: data.tenantId,
    isActive: true,
    sessionsToday: 0,
    quotaUsed: 0,
  };
  subTenants = [...subTenants, sub];
  return sub;
}

/** TODO: replace with apiClient.put('/api/v1/sub-tenants/:id') */
export async function updateSubTenant(id: string, data: Partial<SubTenant>): Promise<SubTenant> {
  await delay();
  subTenants = subTenants.map((s) => (s.id === id ? { ...s, ...data } : s));
  return subTenants.find((s) => s.id === id)!;
}

export const mockApi = {
  getValidations,
  createSession,
  extendSession,
  cancelSession,
  getLinks,
  createLink,
  deactivateLink,
  generateQrPdf,
  getPublicLink,
  submitPublicValidation,
  getTenantBranding,
  updateBranding,
  getReports,
  queueReport,
  getReportJob,
  getAuditLogs,
  getUsers,
  updateUserRole,
  updateUserActive,
  getQuotaUsage,
  getZones,
  createZone,
  updateZone,
  deleteZone,
  getTenants,
  createTenant,
  getSubTenants,
  createSubTenant,
  updateSubTenant,
};
