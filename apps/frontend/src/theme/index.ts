import { createTheme, type Theme } from '@mui/material/styles';

export interface BrandingColors {
  primaryColor: string;
  accentColor: string;
}

// ── Dark (Apex) tokens ────────────────────────────────────────────────────────
const DARK_BG       = '#111111';
const DARK_PAPER    = '#1C1C1C';
const DARK_BORDER   = '#2E2E2E';
const DARK_TEXT     = '#E8EAF0';
const DARK_MUTED    = '#888898';
const DARK_DISABLED = '#555565';

// ── Light tokens ──────────────────────────────────────────────────────────────
const LIGHT_BG      = '#F5F6FA';
const LIGHT_PAPER   = '#FFFFFF';
const LIGHT_BORDER  = '#E6EAF0';
const LIGHT_TEXT    = '#1C2B4A';
const LIGHT_MUTED   = '#637381';

// ── Shared accent ─────────────────────────────────────────────────────────────
const GREEN         = '#22C55E';
const GREEN_DIM     = 'rgba(34,197,94,0.12)';

export function createTenantTheme(
  branding?: Partial<BrandingColors>,
  mode: 'light' | 'dark' = 'dark',
): Theme {
  const primary = branding?.primaryColor ?? GREEN;
  const isDark  = mode === 'dark';

  const bg     = isDark ? DARK_BG     : LIGHT_BG;
  const paper  = isDark ? DARK_PAPER  : LIGHT_PAPER;
  const border = isDark ? DARK_BORDER : LIGHT_BORDER;

  return createTheme({
    palette: {
      mode,
      primary: {
        main:         primary,
        light:        GREEN_DIM,
        dark:         '#16A34A',
        contrastText: isDark ? '#000000' : '#ffffff',
      },
      secondary: {
        main:         branding?.accentColor ?? '#16A34A',
        contrastText: isDark ? '#000000' : '#ffffff',
      },
      success:  { main: '#22C55E', contrastText: '#000' },
      warning:  { main: isDark ? '#F59E0B' : '#D97706', contrastText: '#000' },
      error:    { main: isDark ? '#EF4444' : '#DC2626' },
      info:     { main: '#3B82F6' },
      background: { default: bg, paper },
      text: {
        primary:   isDark ? DARK_TEXT     : LIGHT_TEXT,
        secondary: isDark ? DARK_MUTED    : LIGHT_MUTED,
        disabled:  isDark ? DARK_DISABLED : '#9AA3AF',
      },
      divider: border,
      action: {
        hover:    isDark ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.04)',
        selected: isDark ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.06)',
        disabled: isDark ? 'rgba(255,255,255,0.28)' : 'rgba(0,0,0,0.26)',
      },
    },
    typography: {
      fontFamily: '"Inter", "Roboto", "Helvetica Neue", sans-serif',
      h1: { fontWeight: 700, fontSize: '2rem',     lineHeight: 1.2 },
      h2: { fontWeight: 700, fontSize: '1.75rem',  lineHeight: 1.2 },
      h3: { fontWeight: 600, fontSize: '1.5rem',   lineHeight: 1.2 },
      h4: { fontWeight: 600, fontSize: '1.25rem',  lineHeight: 1.2 },
      h5: { fontWeight: 600, fontSize: '1.125rem', lineHeight: 1.2 },
      h6: { fontWeight: 600, fontSize: '1rem',     lineHeight: 1.2 },
      body1: { lineHeight: 1.6, fontSize: '0.9375rem' },
      body2: { lineHeight: 1.6, fontSize: '0.875rem'  },
    },
    shape: { borderRadius: 10 },
    components: {
      MuiCssBaseline: {
        styleOverrides: {
          body: { backgroundColor: bg },
          '::-webkit-scrollbar':       { width: 6, height: 6 },
          '::-webkit-scrollbar-track': { background: 'transparent' },
          '::-webkit-scrollbar-thumb': { background: isDark ? '#363636' : '#CBD5E1', borderRadius: 3 },
          '::-webkit-scrollbar-thumb:hover': { background: isDark ? '#555' : '#94A3B8' },
        },
      },
      MuiButton: {
        styleOverrides: {
          root: { textTransform: 'none', fontWeight: 600, borderRadius: 10, padding: '8px 20px' },
          containedPrimary: {
            color: isDark ? '#000000' : '#ffffff',
            '&:hover': { backgroundColor: '#16A34A' },
          },
          outlinedPrimary: {
            borderColor: GREEN,
            '&:hover': { backgroundColor: GREEN_DIM },
          },
        },
      },
      MuiCard: {
        styleOverrides: {
          root: {
            borderRadius: 10,
            border: `1px solid ${border}`,
            backgroundImage: 'none',
            boxShadow: isDark ? 'none' : '0 1px 4px rgba(0,0,0,0.06)',
          },
        },
      },
      MuiCardContent: {
        styleOverrides: {
          root: { padding: 24, '&:last-child': { paddingBottom: 24 } },
        },
      },
      MuiPaper: {
        styleOverrides: {
          root: { borderRadius: 10, backgroundImage: 'none' },
          elevation1: {
            boxShadow: isDark ? 'none' : '0 1px 4px rgba(0,0,0,0.06)',
            border: `1px solid ${border}`,
          },
        },
      },
      MuiChip: {
        styleOverrides: { root: { fontWeight: 600, borderRadius: 6 } },
      },
      MuiAppBar: {
        styleOverrides: {
          root: { backgroundImage: 'none', boxShadow: 'none', borderBottom: `1px solid ${border}` },
        },
      },
      MuiLinearProgress: {
        styleOverrides: { root: { borderRadius: 4, height: 6 } },
      },
      MuiOutlinedInput: {
        styleOverrides: {
          root: {
            '& .MuiOutlinedInput-notchedOutline': { borderColor: border },
            '&:hover .MuiOutlinedInput-notchedOutline': { borderColor: isDark ? '#555565' : '#94A3B8' },
            '&.Mui-focused .MuiOutlinedInput-notchedOutline': { borderColor: GREEN },
          },
        },
      },
      MuiTextField:  { defaultProps: { size: 'small' } },
      MuiSelect:     { defaultProps: { size: 'small' } },
      MuiDivider:    { styleOverrides: { root: { borderColor: border } } },
      MuiTableHead: {
        styleOverrides: {
          root: {
            '& .MuiTableCell-head': {
              backgroundColor: isDark ? '#1A1A1A' : '#F8FAFC',
              fontWeight: 600,
              fontSize: '0.75rem',
              textTransform: 'uppercase',
              letterSpacing: '0.05em',
              color: isDark ? DARK_MUTED : LIGHT_MUTED,
            },
          },
        },
      },
      MuiTooltip: {
        defaultProps: { enterDelay: 0 },
        styleOverrides: {
          tooltip: {
            fontSize: '0.75rem',
            backgroundColor: isDark ? '#2A2A2A' : '#1C2B4A',
            border: isDark ? `1px solid ${DARK_BORDER}` : 'none',
          },
          arrow: { color: isDark ? '#2A2A2A' : '#1C2B4A' },
        },
      },
      MuiListItemButton: {
        styleOverrides: {
          root: {
            borderRadius: 8,
            '&.Mui-selected': {
              backgroundColor: isDark ? 'rgba(255,255,255,0.08)' : 'rgba(34,197,94,0.10)',
              '&:hover': { backgroundColor: isDark ? 'rgba(255,255,255,0.10)' : 'rgba(34,197,94,0.14)' },
            },
            '&:hover': { backgroundColor: isDark ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.04)' },
          },
        },
      },
    },
  });
}
