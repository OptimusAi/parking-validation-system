'use client';

import { usePathname, useParams } from 'next/navigation';
import NextLink from 'next/link';
import {
  AppBar, Toolbar, IconButton, Box, Breadcrumbs, Link,
  Typography, Avatar, Chip, Tooltip,
} from '@mui/material';
import { Menu as MenuIcon, NavigateNext, DarkMode, LightMode } from '@mui/icons-material';
import { useTenantStore } from '@/store/tenantStore';

const SEGMENT_LABELS: Record<string, string> = {
  dashboard: 'Dashboard',
  validations: 'Validations',
  links: 'Links',
  zones: 'Zones',
  'sub-tenants': 'Sub-Tenants',
  reports: 'Reports',
  quota: 'Quota',
  'audit-logs': 'Audit Logs',
  settings: 'Settings',
  branding: 'Branding',
  users: 'Users',
  tenants: 'Tenants',
  admin: 'Admin',
  clients: 'Clients',
};

const ROLE_LABELS_DARK: Record<string, { label: string; color: string; bg: string }> = {
  ADMIN:            { label: 'Admin',        color: '#86EFAC', bg: 'rgba(34,197,94,0.12)'  },
  CLIENT_ADMIN:     { label: 'Client Admin', color: '#93C5FD', bg: 'rgba(59,130,246,0.12)' },
  TENANT_ADMIN:     { label: 'Tenant Admin', color: '#FDE68A', bg: 'rgba(245,158,11,0.12)' },
  SUB_TENANT_ADMIN: { label: 'Sub-Tenant',  color: '#FCA5A5', bg: 'rgba(239,68,68,0.12)'  },
};

const ROLE_LABELS_LIGHT: Record<string, { label: string; color: string; bg: string }> = {
  ADMIN:            { label: 'Admin',        color: '#16A34A', bg: 'rgba(22,163,74,0.10)'  },
  CLIENT_ADMIN:     { label: 'Client Admin', color: '#2563EB', bg: 'rgba(37,99,235,0.10)'  },
  TENANT_ADMIN:     { label: 'Tenant Admin', color: '#D97706', bg: 'rgba(217,119,6,0.10)'  },
  SUB_TENANT_ADMIN: { label: 'Sub-Tenant',  color: '#DC2626', bg: 'rgba(220,38,38,0.10)'  },
};

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

interface TopBarProps {
  onMenuClick: () => void;
  sidebarWidth: number;
}

export function TopBar({ onMenuClick, sidebarWidth }: TopBarProps) {
  const pathname = usePathname();
  const { currentUserName, role, colorMode, toggleColorMode } = useTenantStore();

  const ROLE_LABELS = colorMode === 'dark' ? ROLE_LABELS_DARK : ROLE_LABELS_LIGHT;

  const initials = currentUserName
    ? currentUserName.split(' ').map((n) => n[0]).join('').slice(0, 2)
    : 'U';

  const roleInfo = role ? ROLE_LABELS[role] : null;

  // Build breadcrumbs — skip raw UUID segments
  const segments = pathname.split('/').filter(Boolean);
  const breadcrumbs: { label: string; href: string }[] = [];
  let accumulated = '';
  for (const seg of segments) {
    accumulated += `/${seg}`;
    if (UUID_RE.test(seg)) continue;
    const label = SEGMENT_LABELS[seg] ?? seg;
    breadcrumbs.push({ label, href: accumulated });
  }
  const visibleCrumbs = breadcrumbs.slice(-3);

  return (
    <AppBar
      position="fixed"
      elevation={0}
      sx={{
        bgcolor: 'background.default',
        color: 'text.primary',
        borderBottom: '1px solid',
        borderColor: 'divider',
        left: { md: sidebarWidth },
        width: { md: `calc(100% - ${sidebarWidth}px)` },
        zIndex: (theme) => theme.zIndex.appBar,
        transition: 'left 0.2s ease, width 0.2s ease',
      }}
    >
      <Toolbar sx={{ minHeight: 64, gap: 2 }}>
        {/* Mobile menu button */}
        <IconButton
          sx={{ display: { md: 'none' } }}
          onClick={onMenuClick}
          edge="start"
          aria-label="Open navigation menu"
        >
          <MenuIcon />
        </IconButton>

        {/* Breadcrumbs */}
        <Box sx={{ flex: 1, minWidth: 0, overflow: 'hidden' }}>
          <Breadcrumbs
            separator={<NavigateNext sx={{ fontSize: 16 }} />}
            aria-label="breadcrumb"
            sx={{ '& .MuiBreadcrumbs-ol': { flexWrap: 'nowrap' } }}
          >
            {visibleCrumbs.map((crumb, idx) => {
              const isLast = idx === visibleCrumbs.length - 1;
              return isLast ? (
                <Typography
                  key={crumb.href}
                  variant="body2"
                  sx={{ fontWeight: 600 }}
                  color="text.primary"
                  noWrap
                >
                  {crumb.label}
                </Typography>
              ) : (
                <Link
                  key={crumb.href}
                  component={NextLink}
                  href={crumb.href}
                  variant="body2"
                  color="text.secondary"
                  underline="hover"
                  noWrap
                >
                  {crumb.label}
                </Link>
              );
            })}
          </Breadcrumbs>
        </Box>

        {/* Right side: role chip + dark mode toggle + avatar */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, flexShrink: 0 }}>
          {roleInfo && (
            <Chip
              label={roleInfo.label}
              size="small"
              sx={{
                bgcolor: roleInfo.bg,
                color: roleInfo.color,
                fontWeight: 600,
                fontSize: '0.6875rem',
                height: 22,
                borderRadius: '6px',
                border: `1px solid ${roleInfo.color}33`,
                display: { xs: 'none', sm: 'flex' },
              }}
            />
          )}

          <Tooltip title={colorMode === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}>
            <IconButton
              size="small"
              aria-label={colorMode === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
              onClick={toggleColorMode}
              sx={{ color: 'text.secondary', '&:hover': { color: 'text.primary' } }}
            >
              {colorMode === 'dark'
                ? <LightMode sx={{ fontSize: 20 }} />
                : <DarkMode   sx={{ fontSize: 20 }} />}
            </IconButton>
          </Tooltip>

          <Tooltip title={currentUserName ?? 'User'}>
            <Avatar sx={{
              width: 34, height: 34,
              bgcolor: 'primary.main',
              fontSize: '0.8rem',
              fontWeight: 700,
              color: colorMode === 'dark' ? '#000' : '#fff',
              cursor: 'pointer',
            }}>
              {initials}
            </Avatar>
          </Tooltip>
        </Box>
      </Toolbar>
    </AppBar>
  );
}
