import { createTheme, type Theme } from '@mui/material/styles';

export interface BrandingColors {
  primaryColor: string;
  accentColor: string;
}

export function createTenantTheme(branding?: Partial<BrandingColors>): Theme {
  const primary = branding?.primaryColor ?? '#1B4F8A';
  const secondary = branding?.accentColor ?? '#2E86C1';

  return createTheme({
    palette: {
      mode: 'light',
      primary: {
        main: primary,
        light: `${primary}33`,
        contrastText: '#ffffff',
      },
      secondary: {
        main: secondary,
        contrastText: '#ffffff',
      },
      success: { main: '#2E7D32' },
      warning: { main: '#ED6C02' },
      error: { main: '#D32F2F' },
      info: { main: '#0288D1' },
      background: {
        default: '#F4F6F8',
        paper: '#FFFFFF',
      },
      grey: {
        50: '#F8FAFC',
        100: '#F1F5F9',
        200: '#E2E8F0',
        300: '#CBD5E1',
        400: '#94A3B8',
        500: '#64748B',
        600: '#475569',
        700: '#334155',
        800: '#1E293B',
        900: '#0F172A',
      },
    },
    typography: {
      fontFamily: '"Inter", "Roboto", "Helvetica Neue", sans-serif',
      h1: { fontWeight: 700, fontSize: '2rem', lineHeight: 1.2 },
      h2: { fontWeight: 700, fontSize: '1.75rem', lineHeight: 1.2 },
      h3: { fontWeight: 600, fontSize: '1.5rem', lineHeight: 1.2 },
      h4: { fontWeight: 600, fontSize: '1.25rem', lineHeight: 1.2 },
      h5: { fontWeight: 600, fontSize: '1.125rem', lineHeight: 1.2 },
      h6: { fontWeight: 600, fontSize: '1rem', lineHeight: 1.2 },
      body1: { lineHeight: 1.6, fontSize: '0.9375rem' },
      body2: { lineHeight: 1.6, fontSize: '0.875rem' },
    },
    shape: { borderRadius: 8 },
    components: {
      MuiButton: {
        styleOverrides: {
          root: {
            textTransform: 'none',
            fontWeight: 600,
            borderRadius: 8,
            padding: '8px 20px',
          },
        },
      },
      MuiCard: {
        styleOverrides: {
          root: {
            borderRadius: 12,
            boxShadow: '0 1px 3px rgba(0,0,0,0.08), 0 1px 2px rgba(0,0,0,0.04)',
            border: '1px solid rgba(0,0,0,0.06)',
          },
        },
      },
      MuiPaper: {
        styleOverrides: {
          root: { borderRadius: 12 },
          elevation1: {
            boxShadow: '0 1px 3px rgba(0,0,0,0.08), 0 1px 2px rgba(0,0,0,0.04)',
          },
        },
      },
      MuiChip: {
        styleOverrides: {
          root: { fontWeight: 600, borderRadius: 6 },
        },
      },
      MuiAppBar: {
        styleOverrides: {
          root: {
            boxShadow: '0 1px 3px rgba(0,0,0,0.08)',
            borderBottom: '1px solid #E2E8F0',
          },
        },
      },
      MuiLinearProgress: {
        styleOverrides: {
          root: { borderRadius: 4, height: 8 },
        },
      },
      MuiTextField: {
        defaultProps: { size: 'small' },
      },
      MuiSelect: {
        defaultProps: { size: 'small' },
      },
    },
  });
}
