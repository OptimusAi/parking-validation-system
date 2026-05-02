import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { useTenantStore } from './tenantStore';
import { apiClient, setAuthToken, clearAuthToken } from '@/lib/apiClient';

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

interface AuthState {
  isLoggedIn: boolean;
  currentEmail: string | null;
  error: string | null;
  isLoading: boolean;
  login: (username: string, password: string) => Promise<{
    clientId: string | null;
    tenantId: string | null;
    role: string;
    assignedTenants: string[];
  }>;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      isLoggedIn: false,
      currentEmail: null,
      error: null,
      isLoading: false,

      login: async (username: string, password: string) => {
        set({ isLoading: true, error: null });
        try {
          const { data } = await apiClient.post<LoginResponse>('/api/auth/login', {
            username,
            password,
          });

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

          set({ isLoggedIn: true, currentEmail: data.email, isLoading: false });
          return {
            clientId: data.clientId,
            tenantId: data.tenantId ?? assignedTenants[0] ?? null,
            role,
            assignedTenants,
          };
        } catch (err: unknown) {
          const message =
            (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
            'Invalid credentials';
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
