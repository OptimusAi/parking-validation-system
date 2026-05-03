import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { Role, TenantBranding } from '@/lib/types';

interface TenantState {
  currentUserEmail: string | null;
  currentUserName: string | null;
  clientId: string | null;
  tenantId: string | null;
  subTenantId: string | null;
  role: Role | null;
  branding: TenantBranding;
  // Actions
  setContext: (ctx: {
    email: string;
    name: string;
    clientId: string | null;
    tenantId: string | null;
    role: Role;
    branding?: TenantBranding;
  }) => void;
  clearContext: () => void;
  setBranding: (branding: TenantBranding) => void;
  switchTenant: (tenantId: string, clientId?: string | null, branding?: TenantBranding) => void;
  // Computed helpers
  isAdmin: () => boolean;
  isClientAdmin: () => boolean;
  isTenantAdmin: () => boolean;
}

const DEFAULT_BRANDING: TenantBranding = {
  logoUrl: null,
  primaryColor: '#1B4F8A',
  accentColor: '#2E86C1',
};

export const useTenantStore = create<TenantState>()(
  persist(
    (set, get) => ({
      currentUserEmail: null,
      currentUserName: null,
      clientId: null,
      tenantId: null,
      subTenantId: null,
      role: null,
      branding: DEFAULT_BRANDING,

      setContext: (ctx) =>
        set({
          currentUserEmail: ctx.email,
          currentUserName: ctx.name,
          clientId: ctx.clientId,
          tenantId: ctx.tenantId,
          role: ctx.role,
          branding: ctx.branding ?? DEFAULT_BRANDING,
        }),

      clearContext: () =>
        set({
          currentUserEmail: null,
          currentUserName: null,
          clientId: null,
          tenantId: null,
          subTenantId: null,
          role: null,
          branding: DEFAULT_BRANDING,
        }),

      setBranding: (branding) => set({ branding }),

      switchTenant: (tenantId, clientId, branding) =>
        set((s) => ({ tenantId, clientId: clientId ?? s.clientId, branding: branding ?? DEFAULT_BRANDING })),

      isAdmin: () => get().role === 'ADMIN',
      isClientAdmin: () => get().role === 'CLIENT_ADMIN',
      isTenantAdmin: () => get().role === 'TENANT_ADMIN',
    }),
    { name: 'tms-tenant-store' }
  )
);
