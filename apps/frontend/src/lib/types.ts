// ─── Domain Types ────────────────────────────────────────────────────────────

export type Role = 'ADMIN' | 'CLIENT_ADMIN' | 'TENANT_ADMIN' | 'SUBTENANT_USER';

export type SessionStatus = 'ACTIVE' | 'EXTENDED' | 'CANCELLED' | 'EXPIRED';
export type LinkType = 'QR' | 'URL';
export type ReportStatus = 'QUEUED' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
export type ReportType = 'VALIDATION_SESSIONS' | 'QUOTA_USAGE' | 'ZONE_SUMMARY';
export type ReportFormat = 'CSV' | 'EXCEL' | 'PDF';
export type AuditAction =
  | 'SESSION_CREATED'
  | 'SESSION_EXTENDED'
  | 'SESSION_CANCELLED'
  | 'ROLE_CHANGED'
  | 'LINK_CREATED'
  | 'LINK_DEACTIVATED'
  | 'ZONE_CREATED'
  | 'BRANDING_UPDATED'
  | 'USER_UPDATED';

export interface TenantBranding {
  logoUrl: string | null;
  primaryColor: string;
  accentColor: string;
}

export interface Client {
  id: string;
  name: string;
  plan?: string;
  isActive?: boolean;
  createdAt?: string;
}

export interface Tenant {
  id: string;
  name: string;
  clientId: string;
  branding: TenantBranding;
  zones?: number;
  subTenants?: number;
  status?: 'ACTIVE' | 'INACTIVE';
}

export interface SubTenant {
  id: string;
  name: string;
  tenantId: string;
  isActive: boolean;
  sessionsToday?: number;
  quotaUsed?: number;
}

export interface Zone {
  id: string;
  zoneNumber: string;
  name: string;
  defaultDurationMinutes: number;
  maxDurationMinutes: number;
  activeSessions?: number;
}

export interface ValidationSession {
  id: string;
  tenantId: string;
  subTenantId?: string;
  subTenantName?: string;
  zoneId: string;
  zoneName: string;
  licensePlate: string;
  startTime: string;
  endTime: string;
  durationMinutes: number;
  status: SessionStatus;
}

export interface ValidationLink {
  id: string;
  tenantId: string;
  zoneId: string;
  zoneName: string;
  label?: string;
  type: LinkType;
  token: string;
  url: string;
  durationMinutes: number;
  scans: number;
  expiresAt?: string;
  isActive: boolean;
  createdAt: string;
}

export interface ReportJob {
  id: string;
  tenantId: string;
  type: ReportType;
  format: ReportFormat;
  status: ReportStatus;
  requestedAt: string;
  completedAt?: string;
  fileUrl?: string;
  dateFrom?: string;
  dateTo?: string;
}

export interface AuditLog {
  id: string;
  tenantId: string;
  actorEmail: string;
  action: AuditAction;
  entityType: string;
  entityId: string;
  ipAddress: string;
  timestamp: string;
  before?: Record<string, unknown>;
  after?: Record<string, unknown>;
}

export interface User {
  id: string;
  email: string;
  name?: string;
  firstName?: string;
  lastName?: string;
  fullName?: string;
  role: Role;
  tenantId?: string | null;
  clientId?: string | null;
  tenantName?: string;
  isActive: boolean;
  createdAt?: string;
}

export interface QuotaUsage {
  daily: { used: number; limit: number };
  weekly: { used: number; limit: number };
  monthly: { used: number; limit: number };
  byZone: { zoneId: string; zoneName: string; countToday: number }[];
  bySubTenant: { subTenantId: string; name: string; count: number; quotaUsed: number }[];
}

// ─── API Response Wrappers ───────────────────────────────────────────────────

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface PageParams {
  page?: number;
  pageSize?: number;
  [key: string]: unknown;
}
