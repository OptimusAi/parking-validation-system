'use client';

import { useMemo } from 'react';
import NextLink from 'next/link';
import { useParams, usePathname, useRouter } from 'next/navigation';
import {
  Box, Drawer, List, ListItem, ListItemButton, ListItemIcon,
  ListItemText, Typography, Avatar, Button, Divider, Select,
  MenuItem, FormControl, InputLabel, Tooltip,
} from '@mui/material';
import {
  Dashboard, DirectionsCar, QrCode2, LocationOn, Business,
  Assessment, PieChart, History, Palette, People, Logout,
  Apartment,
} from '@mui/icons-material';
import { useQuery } from '@tanstack/react-query';
import { useTenantStore } from '@/store/tenantStore';
import { useAuthStore } from '@/store/authStore';
import { mockApi } from '@/lib/api';

const DRAWER_WIDTH = 260;

interface NavItem {
  label: string;
  href: string;
  icon: React.ElementType;
  roles: string[];
}

function buildNavItems(clientId: string, tenantId: string): NavItem[] {
  return [
    { label: 'Dashboard', href: `/${clientId}/${tenantId}/dashboard`, icon: Dashboard, roles: ['ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN', 'SUBTENANT_USER'] },
    { label: 'Validations', href: `/${clientId}/${tenantId}/validations`, icon: DirectionsCar, roles: ['ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN', 'SUBTENANT_USER'] },
    { label: 'Links', href: `/${clientId}/${tenantId}/links`, icon: QrCode2, roles: ['ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN', 'SUBTENANT_USER'] },
    { label: 'Zones', href: `/${clientId}/${tenantId}/zones`, icon: LocationOn, roles: ['ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN'] },
    { label: 'Sub-Tenants', href: `/${clientId}/${tenantId}/sub-tenants`, icon: Apartment, roles: ['ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN'] },
    { label: 'Reports', href: `/${clientId}/${tenantId}/reports`, icon: Assessment, roles: ['ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN'] },
    { label: 'Quota', href: `/${clientId}/${tenantId}/quota`, icon: PieChart, roles: ['ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN'] },
    { label: 'Audit Logs', href: `/${clientId}/${tenantId}/audit-logs`, icon: History, roles: ['ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN'] },
    { label: 'Branding', href: `/${clientId}/${tenantId}/settings/branding`, icon: Palette, roles: ['ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN'] },
    { label: 'Users', href: `/${clientId}/${tenantId}/users`, icon: People, roles: ['ADMIN', 'CLIENT_ADMIN'] },
    { label: 'Tenants', href: `/${clientId}/tenants`, icon: Business, roles: ['ADMIN', 'CLIENT_ADMIN'] },
  ];
}

interface SidebarContentProps {
  onClose?: () => void;
}

function SidebarContent({ onClose }: SidebarContentProps) {
  const pathname = usePathname();
  const router = useRouter();
  const params = useParams<{ clientId?: string; tenantId?: string }>();
  const clientId = params?.clientId ?? '11111111-1111-1111-1111-111111111111';
  const tenantId = params?.tenantId ?? '22222222-2222-2222-2222-222222222222';

  const { currentUserEmail, currentUserName, role, branding, switchTenant } = useTenantStore();
  const logout = useAuthStore((s) => s.logout);

  const { data: tenants } = useQuery({
    queryKey: ['tenants'],
    queryFn: mockApi.getTenants,
    enabled: role === 'ADMIN' || role === 'CLIENT_ADMIN',
  });

  const navItems = useMemo(
    () => buildNavItems(clientId, tenantId),
    [clientId, tenantId]
  );

  const visibleItems = navItems.filter((item) => !role || item.roles.includes(role));

  const initials = currentUserName
    ? currentUserName.split(' ').map((n) => n[0]).join('').slice(0, 2)
    : 'U';

  const handleTenantSwitch = (newTenantId: string) => {
    switchTenant(newTenantId, undefined);
    router.push(`/${clientId}/${newTenantId}/dashboard`);
    onClose?.();
  };

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      {/* Logo/Brand */}
      <Box
        sx={{
          p: 2.5,
          display: 'flex',
          alignItems: 'center',
          gap: 1.5,
          borderBottom: '1px solid',
          borderColor: 'divider',
          minHeight: 72,
        }}
      >
        <Avatar
          sx={{
            bgcolor: branding.primaryColor,
            width: 40,
            height: 40,
            fontSize: '1rem',
            fontWeight: 700,
            flexShrink: 0,
          }}
        >
          P
        </Avatar>
        <Box>
          <Typography variant="body2" fontWeight={700} color="text.primary" noWrap>
            TMS Parking
          </Typography>
          <Typography variant="caption" color="text.secondary" noWrap>
            Validation Platform
          </Typography>
        </Box>
      </Box>

      {/* Tenant Switcher (ADMIN only) */}
      {(role === 'ADMIN' || role === 'CLIENT_ADMIN') && tenants && (
        <Box sx={{ px: 2, pt: 2 }}>
          <FormControl fullWidth size="small">
            <InputLabel>Tenant</InputLabel>
            <Select
              label="Tenant"
              value={tenantId}
              onChange={(e) => handleTenantSwitch(e.target.value)}
            >
              {tenants.map((t) => (
                <MenuItem key={t.id} value={t.id}>
                  {t.name}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        </Box>
      )}

      {/* Navigation */}
      <Box sx={{ flex: 1, overflow: 'auto', py: 1 }}>
        <List dense disablePadding>
          {visibleItems.map((item) => {
            const Icon = item.icon;
            const active = pathname === item.href || pathname.startsWith(item.href + '/');
            return (
              <ListItem key={item.href} disablePadding sx={{ px: 1, mb: 0.25 }}>
                <ListItemButton
                  component={NextLink}
                  href={item.href}
                  onClick={onClose}
                  selected={active}
                  sx={{
                    borderRadius: 1.5,
                    py: 0.875,
                    '&.Mui-selected': {
                      bgcolor: 'primary.light',
                      color: 'primary.main',
                      '& .MuiListItemIcon-root': { color: 'primary.main' },
                      '&:hover': { bgcolor: 'primary.light' },
                    },
                    '&:hover': { bgcolor: 'action.hover' },
                  }}
                >
                  <ListItemIcon sx={{ minWidth: 36, color: active ? 'primary.main' : 'text.secondary' }}>
                    <Icon sx={{ fontSize: 20 }} />
                  </ListItemIcon>
                  <ListItemText
                    primary={item.label}
                    primaryTypographyProps={{
                      fontSize: '0.875rem',
                      fontWeight: active ? 600 : 400,
                    }}
                  />
                </ListItemButton>
              </ListItem>
            );
          })}
        </List>
      </Box>

      {/* User Footer */}
      <Box sx={{ p: 2, borderTop: '1px solid', borderColor: 'divider' }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 1.5 }}>
          <Avatar sx={{ width: 36, height: 36, bgcolor: 'primary.main', fontSize: '0.8rem' }}>
            {initials}
          </Avatar>
          <Box sx={{ flex: 1, minWidth: 0 }}>
            <Typography variant="body2" fontWeight={600} noWrap>
              {currentUserName ?? 'User'}
            </Typography>
            <Tooltip title={currentUserEmail ?? ''}>
              <Typography variant="caption" color="text.secondary" noWrap display="block">
                {currentUserEmail}
              </Typography>
            </Tooltip>
          </Box>
        </Box>
        <Button
          fullWidth
          variant="outlined"
          size="small"
          startIcon={<Logout sx={{ fontSize: 16 }} />}
          onClick={() => { logout(); router.push('/login'); onClose?.(); }}
          sx={{ fontSize: '0.75rem' }}
        >
          Sign Out
        </Button>
      </Box>
    </Box>
  );
}

interface SidebarProps {
  mobileOpen: boolean;
  onMobileClose: () => void;
}

export function Sidebar({ mobileOpen, onMobileClose }: SidebarProps) {
  return (
    <>
      {/* Mobile Drawer */}
      <Drawer
        variant="temporary"
        open={mobileOpen}
        onClose={onMobileClose}
        ModalProps={{ keepMounted: true }}
        sx={{
          display: { xs: 'block', md: 'none' },
          '& .MuiDrawer-paper': { width: DRAWER_WIDTH, boxSizing: 'border-box' },
        }}
      >
        <SidebarContent onClose={onMobileClose} />
      </Drawer>

      {/* Desktop Drawer */}
      <Drawer
        variant="permanent"
        sx={{
          display: { xs: 'none', md: 'block' },
          width: DRAWER_WIDTH,
          flexShrink: 0,
          '& .MuiDrawer-paper': {
            width: DRAWER_WIDTH,
            boxSizing: 'border-box',
            borderRight: '1px solid',
            borderColor: 'divider',
          },
        }}
        open
      >
        <SidebarContent />
      </Drawer>
    </>
  );
}

export const SIDEBAR_WIDTH = DRAWER_WIDTH;
