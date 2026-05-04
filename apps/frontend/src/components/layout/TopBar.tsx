'use client';

import { usePathname, useParams } from 'next/navigation';
import NextLink from 'next/link';
import {
  AppBar, Toolbar, IconButton, Box, Breadcrumbs, Link,
  Typography, Avatar, Chip, Tooltip,
} from '@mui/material';
import { Menu as MenuIcon, NavigateNext } from '@mui/icons-material';
import { useTenantStore } from '@/store/tenantStore';
import { SIDEBAR_WIDTH } from './Sidebar';

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
};

const ROLE_LABELS: Record<string, { label: string; color: string; bg: string }> = {
  ADMIN: { label: 'Admin', color: '#D32F2F', bg: '#FFEBEE' },
  CLIENT_ADMIN: { label: 'Client Admin', color: '#1565C0', bg: '#E3F2FD' },
  TENANT_ADMIN: { label: 'Tenant Admin', color: '#2E7D32', bg: '#E8F5E9' },
  SUB_TENANT_ADMIN: { label: 'Sub-Tenant', color: '#E65100', bg: '#FFF3E0' },
};

interface TopBarProps {
  onMenuClick: () => void;
}

export function TopBar({ onMenuClick }: TopBarProps) {
  const pathname = usePathname();
  const params = useParams<{ clientId?: string; tenantId?: string }>();
  const { currentUserName, role } = useTenantStore();

  const initials = currentUserName
    ? currentUserName.split(' ').map((n) => n[0]).join('').slice(0, 2)
    : 'U';

  const roleInfo = role ? ROLE_LABELS[role] : null;

  // Build breadcrumbs from pathname
  const segments = pathname.split('/').filter(Boolean);
  const breadcrumbs: { label: string; href: string }[] = [];
  let accumulated = '';
  for (const seg of segments) {
    accumulated += `/${seg}`;
    const label = SEGMENT_LABELS[seg] ?? (params?.tenantId === seg ? 'Tenant' : seg);
    breadcrumbs.push({ label, href: accumulated });
  }
  // Show only last 3
  const visibleCrumbs = breadcrumbs.slice(-3);

  return (
    <AppBar
      position="fixed"
      elevation={0}
      sx={{
        bgcolor: 'background.paper',
        color: 'text.primary',
        borderBottom: '1px solid',
        borderColor: 'divider',
        left: { md: SIDEBAR_WIDTH },
        width: { md: `calc(100% - ${SIDEBAR_WIDTH}px)` },
        zIndex: (theme) => theme.zIndex.drawer - 1,
      }}
    >
      <Toolbar sx={{ minHeight: 64, gap: 2 }}>
        {/* Mobile menu button */}
        <IconButton
          sx={{ display: { md: 'none' } }}
          onClick={onMenuClick}
          edge="start"
        >
          <MenuIcon />
        </IconButton>

        {/* Breadcrumbs */}
        <Box sx={{ flex: 1, minWidth: 0 }}>
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

        {/* User Info */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, flexShrink: 0 }}>
          {roleInfo && (
            <Chip
              label={roleInfo.label}
              size="small"
              sx={{
                bgcolor: roleInfo.bg,
                color: roleInfo.color,
                fontWeight: 600,
                fontSize: '0.7rem',
                display: { xs: 'none', sm: 'flex' },
              }}
            />
          )}
          <Tooltip title={currentUserName ?? 'User'}>
            <Avatar sx={{ width: 36, height: 36, bgcolor: 'primary.main', fontSize: '0.8rem', cursor: 'pointer' }}>
              {initials}
            </Avatar>
          </Tooltip>
        </Box>
      </Toolbar>
    </AppBar>
  );
}
