'use client';

import { useEffect } from 'react';
import { useParams } from 'next/navigation';
import { useTenantStore } from '@/store/tenantStore';

/**
 * Syncs the [clientId] and [tenantId] URL params into the Zustand store so
 * every API call in child pages uses the correct tenant scope, regardless of
 * what was set at login time (e.g. CLIENT_ADMIN navigating between tenants).
 */
export default function TenantScopedLayout({ children }: { children: React.ReactNode }) {
  const { clientId, tenantId } = useParams<{ clientId: string; tenantId: string }>();
  const switchTenant = useTenantStore((s) => s.switchTenant);

  useEffect(() => {
    if (clientId && tenantId) {
      switchTenant(tenantId, clientId);
    }
  }, [clientId, tenantId, switchTenant]);

  return <>{children}</>;
}
