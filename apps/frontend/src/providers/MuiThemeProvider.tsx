'use client';

import { useMemo } from 'react';
import { ThemeProvider, CssBaseline } from '@mui/material';
import { createTenantTheme } from '@/theme';
import { useTenantStore } from '@/store/tenantStore';

export function MuiThemeProvider({ children }: { children: React.ReactNode }) {
  const branding   = useTenantStore((s) => s.branding);
  const colorMode  = useTenantStore((s) => s.colorMode);

  const theme = useMemo(
    () =>
      createTenantTheme(
        { primaryColor: branding.primaryColor, accentColor: branding.accentColor },
        colorMode,
      ),
    [branding.primaryColor, branding.accentColor, colorMode]
  );

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      {children}
    </ThemeProvider>
  );
}
