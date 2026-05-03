/**
 * Real API layer — every function calls the backend via apiClient.
 * Keeps the same function signatures as mockApi.ts so all page components
 * can import from '@/lib/api' without any other changes.
 */

import { apiClient } from './apiClient';
import type {
  PageResponse, PageParams, ValidationSession, ValidationLink,
  ReportJob, AuditLog, User, Zone, Tenant, TenantBranding, QuotaUsage, SubTenant, Client,
} from './types';
import { useTenantStore } from '@/store/tenantStore';

// ─── Helpers ──────────────────────────────────────────────────────────────────

function tenantId() {
  return useTenantStore.getState().tenantId!;
}

function clientId() {
  return useTenantStore.getState().clientId!;
}

function pageParams(params?: PageParams) {
  return {
    page: params?.page ?? 0,
    pageSize: params?.pageSize ?? 50,
  };
}

// ─── Validations ─────────────────────────────────────────────────────────────

export async function getValidations(
  params?: PageParams & { status?: string; zoneId?: string; plate?: string },
): Promise<PageResponse<ValidationSession>> {
  const { data } = await apiClient.get(`/api/v1/validations`, {
    params: {
      tenantId: tenantId(),
      ...pageParams(params),
      ...(params?.status && { status: params.status }),
      ...(params?.zoneId && { zoneId: params.zoneId }),
      ...(params?.plate && { plate: params.plate }),
    },
  });
  return data;
}

export async function createSession(data: {
  licensePlate: string;
  zoneId: string;
  zoneName: string;
  durationMinutes: number;
  subTenantId?: string;
}): Promise<ValidationSession> {
  const { data: result } = await apiClient.post(`/api/v1/validations`, {
    ...data,
    tenantId: tenantId(),
  });
  return result;
}

export async function extendSession(id: string, additionalMinutes: number): Promise<ValidationSession> {
  const { data } = await apiClient.put(`/api/v1/validations/${id}/extend`, { additionalMinutes });
  return data;
}

export async function cancelSession(id: string): Promise<ValidationSession> {
  const { data } = await apiClient.put(`/api/v1/validations/${id}/cancel`);
  return data;
}

// ─── Validation Links ─────────────────────────────────────────────────────────

export async function getLinks(params?: PageParams): Promise<PageResponse<ValidationLink>> {
  const { data } = await apiClient.get(`/api/v1/links`, {
    params: { tenantId: tenantId(), ...pageParams(params) },
  });
  return data;
}

export async function createLink(data: {
  zoneId: string;
  zoneName: string;
  type: 'QR' | 'URL';
  durationMinutes: number;
  label?: string;
  expiresAt?: string;
}): Promise<ValidationLink> {
  const { data: result } = await apiClient.post(`/api/v1/links`, {
    ...data,
    tenantId: tenantId(),
  });
  return result;
}

export async function deactivateLink(id: string): Promise<void> {
  await apiClient.put(`/api/v1/links/${id}/deactivate`);
}

export async function generateQrPdf(id: string): Promise<Blob> {
  const { data } = await apiClient.get(`/api/v1/links/${id}/qr-pdf`, {
    responseType: 'blob',
  });
  return data;
}

// ─── Public Validation ───────────────────────────────────────────────────────

export async function getPublicLink(token: string): Promise<ValidationLink> {
  const { data } = await apiClient.get(`/api/v1/links/by-token/${token}`);
  return data;
}

export async function submitPublicValidation(
  token: string,
  licensePlate: string,
): Promise<{ validUntil: string; zoneName: string }> {
  const { data } = await apiClient.post(`/api/v1/validations/public/${token}`, { licensePlate });
  return data;
}

// ─── Branding ─────────────────────────────────────────────────────────────────

export async function getTenantBranding(tenantId: string): Promise<TenantBranding> {
  const { data } = await apiClient.get(`/api/v1/tenants/${tenantId}/branding`);
  return data;
}

export async function updateBranding(tenantId: string, formData: FormData | Partial<TenantBranding>): Promise<TenantBranding> {
  const isFormData = formData instanceof FormData;
  const { data } = await apiClient.put(`/api/v1/tenants/${tenantId}/branding`, formData, {
    headers: isFormData ? { 'Content-Type': 'multipart/form-data' } : {},
  });
  return data;
}

// ─── Reports ─────────────────────────────────────────────────────────────────

export async function getReports(params?: PageParams): Promise<PageResponse<ReportJob>> {
  const { data } = await apiClient.get(`/api/v1/reports`, {
    params: { tenantId: tenantId(), ...pageParams(params) },
  });
  return data;
}

export async function queueReport(data: {
  type: string;
  format: string;
  dateFrom?: string;
  dateTo?: string;
}): Promise<{ jobId: string }> {
  const { data: result } = await apiClient.post(`/api/v1/reports`, {
    ...data,
    tenantId: tenantId(),
  });
  return result;
}

export async function getReportJob(jobId: string): Promise<ReportJob> {
  const { data } = await apiClient.get(`/api/v1/reports/${jobId}`);
  return data;
}

// ─── Audit Logs ───────────────────────────────────────────────────────────────

export async function getAuditLogs(
  params?: PageParams & { action?: string; actor?: string },
): Promise<PageResponse<AuditLog>> {
  const { data } = await apiClient.get(`/api/v1/audit-logs`, {
    params: {
      tenantId: tenantId(),
      ...pageParams(params),
      ...(params?.action && { action: params.action }),
      ...(params?.actor && { actor: params.actor }),
    },
  });
  return data;
}

// ─── Users ────────────────────────────────────────────────────────────────────

export async function getUsers(params?: PageParams): Promise<PageResponse<User>> {
  const { data } = await apiClient.get(`/api/v1/users`, {
    params: { tenantId: tenantId(), ...pageParams(params) },
  });
  return data;
}

export async function updateUserRole(id: string, role: User['role']): Promise<User> {
  const { data } = await apiClient.put(`/api/v1/users/${id}/role`, { role });
  return data;
}

export async function updateUserActive(id: string, isActive: boolean): Promise<User> {
  const { data } = await apiClient.put(`/api/v1/users/${id}`, { isActive });
  return data;
}

// ─── Quota ────────────────────────────────────────────────────────────────────

export async function getQuotaUsage(tid: string): Promise<QuotaUsage> {
  const { data } = await apiClient.get(`/api/v1/quota-configs`, {
    params: { tenantId: tid },
  });
  return data;
}

// ─── Zones ────────────────────────────────────────────────────────────────────

export async function getZones(): Promise<Zone[]> {
  const { data } = await apiClient.get(`/api/v1/zones`, {
    params: { tenantId: tenantId() },
  });
  // Backend returns PageResponse or array; normalise to array
  return Array.isArray(data) ? data : data.content ?? [];
}

export async function createZone(data: Omit<Zone, 'id' | 'activeSessions'>): Promise<Zone> {
  const { data: result } = await apiClient.post(`/api/v1/zones`, {
    ...data,
    tenantId: tenantId(),
  });
  return result;
}

export async function updateZone(id: string, data: Partial<Zone>): Promise<Zone> {
  const { data: result } = await apiClient.put(`/api/v1/zones/${id}`, data);
  return result;
}

export async function deleteZone(id: string): Promise<void> {
  await apiClient.delete(`/api/v1/zones/${id}`);
}

// ─── Clients ─────────────────────────────────────────────────────────────────

export async function listClients(params?: PageParams): Promise<PageResponse<Client>> {
  const { data } = await apiClient.get(`/api/v1/clients`, {
    params: { page: params?.page ?? 0, pageSize: params?.pageSize ?? 50 },
  });
  return data;
}

export async function createClient(data: { name: string; plan?: string }): Promise<Client> {
  const { data: result } = await apiClient.post(`/api/v1/clients`, data);
  return result;
}

// ─── Tenants ─────────────────────────────────────────────────────────────────

export async function getTenants(): Promise<Tenant[]> {
  const { role, clientId: cid } = useTenantStore.getState();
  const params: Record<string, unknown> = { pageSize: 200 };
  // Only filter by clientId for CLIENT_ADMIN (ADMIN sees all tenants)
  if (role !== 'ADMIN' && cid) params.clientId = cid;
  const { data } = await apiClient.get(`/api/v1/tenants`, { params });
  return Array.isArray(data) ? data : data.content ?? [];
}

export async function getTenantsForClient(cid: string): Promise<Tenant[]> {
  const { data } = await apiClient.get(`/api/v1/tenants`, {
    params: { clientId: cid, pageSize: 200 },
  });
  return Array.isArray(data) ? data : data.content ?? [];
}

export async function createTenant(data: {
  name: string;
  primaryColor?: string;
  accentColor?: string;
  clientId?: string;
}): Promise<Tenant> {
  const resolvedClientId = data.clientId || clientId();
  const { data: result } = await apiClient.post(`/api/v1/tenants`, {
    name: data.name,
    clientId: resolvedClientId || null,
  });
  return result;
}

// ─── Sub-Tenants ──────────────────────────────────────────────────────────────

export async function getSubTenants(tid: string): Promise<SubTenant[]> {
  const { data } = await apiClient.get(`/api/v1/sub-tenants`, {
    params: { tenantId: tid },
  });
  return Array.isArray(data) ? data : data.content ?? [];
}

export async function createSubTenant(data: { name: string; tenantId: string }): Promise<SubTenant> {
  const { data: result } = await apiClient.post(`/api/v1/sub-tenants`, data);
  return result;
}

export async function updateSubTenant(id: string, data: Partial<SubTenant>): Promise<SubTenant> {
  const { data: result } = await apiClient.put(`/api/v1/sub-tenants/${id}`, data);
  return result;
}

// ─── Named export object (drop-in replacement for mockApi) ───────────────────

export const api = {
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
  getTenantsForClient,
  createTenant,
  getSubTenants,
  createSubTenant,
  updateSubTenant,
  listClients,
  createClient,
};

/** Alias kept for backward-compatibility with pages that import { mockApi } */
export const mockApi = api;

export default api;
