import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { useTenantStore } from './tenantStore';
import { apiClient, setAuthToken, clearAuthToken } from '@/lib/apiClient';

// ── Backend response shape ────────────────────────────────────────────────────

interface LoginResponse {
  tmsToken: string;
  role: string;
  tenantId: string | null;
  clientId: string | null;
  subTenantId: string | null;
  assignedTenants: string[];
  email: string;
  firstName: string | null;
  lastName: string | null;
}

// ── Exported result type (used by login page for redirects) ──────────────────

export interface LoginResult {
  clientId: string | null;
  tenantId: string | null;
  role: 'ADMIN' | 'CLIENT_ADMIN' | 'TENANT_ADMIN' | 'SUB_TENANT_ADMIN' | string;
  assignedTenants: string[];
}

/**
 * Resolve the best dashboard URL from the login result.
 *
 * Role routing:
 *   ADMIN          → /admin                          (system-wide, no client/tenant scope)
 *   CLIENT_ADMIN   → /{clientId}/{firstTenant}/dashboard  or /{clientId}/tenants
 *   TENANT_ADMIN   → /{clientId}/{tenantId}/dashboard
 *   SUB_TENANT_ADMIN → /{clientId}/{tenantId}/dashboard
 *   anything else  → /no-access
 */
export function resolveRedirectPath(result: LoginResult): string {
  const { clientId, tenantId, role, assignedTenants } = result;

  switch (role) {
    case 'ADMIN':
      return '/admin';

    case 'CLIENT_ADMIN':
      if (!clientId) return '/no-access';
      if (tenantId) return `/${clientId}/${tenantId}/dashboard`;
      if (assignedTenants.length > 0) return `/${clientId}/${assignedTenants[0]}/dashboard`;
      return `/${clientId}/tenants`;

    case 'TENANT_ADMIN':
    case 'SUB_TENANT_ADMIN':
      if (clientId && tenantId) return `/${clientId}/${tenantId}/dashboard`;
      return '/no-access';

    default:
      return '/no-access';
  }
}

// ── Auth store ────────────────────────────────────────────────────────────────

interface AuthState {
  isLoggedIn: boolean;
  currentEmail: string | null;
  error: string | null;
  isLoading: boolean;
  /** Local dev / password-grant login */
  login: (username: string, password: string) => Promise<LoginResult>;
  /** Production: exchange an OAuth implicit-flow access_token for a TMS JWT.
   *  Mirrors TMS oauth.jsx → ApiHelper.login(token) → POST /login with Bearer header. */
  loginWithOAuthToken: (oauthToken: string) => Promise<LoginResult>;
  logout: () => void;
}

// ── Shared helper: apply a successful LoginResponse ──────────────────────────

function applyLoginResponse(data: LoginResponse): LoginResult {
  setAuthToken(data.tmsToken);

  const name = [data.firstName, data.lastName].filter(Boolean).join(' ') || data.email;
  const role = data.role as 'ADMIN' | 'CLIENT_ADMIN' | 'TENANT_ADMIN' | 'SUBTENANT_USER';
  const assignedTenants: string[] = data.assignedTenants ?? [];

  useTenantStore.getState().setContext({
    email: data.email,
    name,
    clientId: data.clientId ?? '',
    tenantId: data.tenantId ?? assignedTenants[0] ?? '',
    role,
    branding: { logoUrl: null, primaryColor: '#1B4F8A', accentColor: '#2E86C1' },
  });

  return {
    clientId: data.clientId,
    tenantId: data.tenantId ?? assignedTenants[0] ?? null,
    role,
    assignedTenants,
  };
}

function extractErrorMessage(err: unknown): string {
  return (err as { response?: { data?: { message?: string } } })?.response?.data?.message
    ?? (err as Error)?.message
    ?? 'Login failed';
}

// ── Store ─────────────────────────────────────────────────────────────────────

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      isLoggedIn: false,
      currentEmail: null,
      error: null,
      isLoading: false,

      // ── Local dev / password grant ────────────────────────────────────────
      login: async (username, password) => {
        set({ isLoading: true, error: null });
        try {
          const { data } = await apiClient.post<LoginResponse>('/api/auth/login', {
            username,
            password,
          });
          const result = applyLoginResponse(data);
          set({ isLoggedIn: true, currentEmail: data.email, isLoading: false });
          return result;
        } catch (err) {
          const message = extractErrorMessage(err);
          set({ isLoading: false, error: message });
          throw new Error(message);
        }
      },

      // ── OAuth implicit flow: send Bearer token, get TMS JWT back ──────────
      // Mirrors TMS: ApiHelper.login(token) → POST /api/login { Authorization: Bearer <token> }
      loginWithOAuthToken: async (oauthToken) => {
        set({ isLoading: true, error: null });
        try {
          const { data } = await apiClient.post<LoginResponse>(
            '/api/auth/login',
            null,
            { headers: { Authorization: `Bearer ${oauthToken}` } },
          );
          const result = applyLoginResponse(data);
          set({ isLoggedIn: true, currentEmail: data.email, isLoading: false });
          return result;
        } catch (err) {
          const message = extractErrorMessage(err);
          set({ isLoading: false, error: message });
          throw new Error(message);
        }
      },

      logout: () => {
        clearAuthToken();
        useTenantStore.getState().clearContext();
        set({ isLoggedIn: false, currentEmail: null, error: null });
      },
    }),
    { name: 'tms-auth-store' }
  )
);
