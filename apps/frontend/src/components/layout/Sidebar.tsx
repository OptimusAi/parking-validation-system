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
  Apartment, AdminPanelSettings,
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

function buildNavItems(clientId: string | null, tenantId: string | null): NavItem[] {
  const tenantItems: NavItem[] = tenantId ? [
    { label: 'Dashboard', href: clientId ? `/${clientId}/${tenantId}/dashboard` : '#', icon: Dashboard, roles: ['ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN', 'SUBTENANT_USER'] },
    { label: 'Validations', href: clientId ? `/${clientId}/${tenantId}/validations` : '#', icon: DirectionsCar, roles: ['ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN', 'SUBTENANT_USER'] },
    { label: 'Links', href: clientId ? `/${clientId}/${tenantId}/links` : '#', icon: QrCode2, roles: ['ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN', 'SUBTENANT_USER'] },
    { label: 'Zones', href: clientId ? `/${clientId}/${tenantId}/zones` : '#', icon: LocationOn, roles: ['ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN'] },
    { label: 'Sub-Tenants', href: clientId ? `/${clientId}/${tenantId}/sub-tenants` : '#', icon: Apartment, roles: ['ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN'] },
    { label: 'Reports', href: clientId ? `/${clientId}/${tenantId}/reports` : '#', icon: Assessment, roles: ['ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN'] },
    { label: 'Quota', href: clientId ? `/${clientId}/${tenantId}/quota` : '#', icon: PieChart, roles: ['ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN'] },
    { label: 'Audit Logs', href: clientId ? `/${clientId}/${tenantId}/audit-logs` : '#', icon: History, roles: ['ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN'] },
    { label: 'Branding', href: clientId ? `/${clientId}/${tenantId}/settings/branding` : '#', icon: Palette, roles: ['ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN'] },
    { label: 'Users', href: clientId ? `/${clientId}/${tenantId}/users` : '#', icon: People, roles: ['ADMIN', 'CLIENT_ADMIN'] },
  ] : [];
  return [
    // Admin-scoped (always visible to ADMIN regardless of tenant context)
    { label: 'Admin', href: '/admin', icon: AdminPanelSettings, roles: ['ADMIN'] },
    { label: 'Clients', href: '/admin/clients', icon: Business, roles: ['ADMIN'] },
    { label: 'Tenants', href: '/admin/tenants', icon: Business, roles: ['ADMIN'] },
    { label: 'Users', href: '/admin/users', icon: People, roles: ['ADMIN'] },
    { label: 'Zones', href: '/admin/zones', icon: LocationOn, roles: ['ADMIN'] },
    // CLIENT_ADMIN tenant list
    { label: 'Tenants', href: clientId ? `/${clientId}/tenants` : '#', icon: Business, roles: ['CLIENT_ADMIN'] },
    // Tenant-scoped (shown when a tenant is selected)
    ...tenantItems,
  ];
}

interface SidebarContentProps {
  onClose?: () => void;
}

function SidebarContent({ onClose }: SidebarContentProps) {
  const pathname = usePathname();
  const router = useRouter();
  const params = useParams<{ clientId?: string; tenantId?: string }>();
  const storeClientId = useTenantStore((s) => s.clientId);
  const storeTenantId = useTenantStore((s) => s.tenantId);
  // For tenant-scoped nav, tenantId is enough — clientId resolved from tenant object on switch
  const clientId = params?.clientId ?? (storeClientId || null);
  const tenantId = params?.tenantId ?? (storeTenantId || null);

  const { currentUserEmail, currentUserName, role, branding, switchTenant } = useTenantStore();
  const signOut = useAuthStore((s) => s.signOut);

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
    const tenant = tenants?.find((t) => t.id === newTenantId);
    const resolvedClientId = tenant?.clientId ?? clientId;
    switchTenant(newTenantId, resolvedClientId, undefined);
    if (resolvedClientId) {
      router.push(`/${resolvedClientId}/${newTenantId}/dashboard`);
    }
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
          <Typography variant="body2" sx={{ fontWeight: 700 }} color="text.primary" noWrap>
            TMS Parking
          </Typography>
          <Typography variant="caption" color="text.secondary" noWrap>
            Validation Platform
          </Typography>
        </Box>
      </Box>

      {/* Tenant Switcher (ADMIN / CLIENT_ADMIN only) */}
      {(role === 'ADMIN' || role === 'CLIENT_ADMIN') && (
        <Box sx={{ px: 2, pt: 2 }}>
          <FormControl fullWidth size="small">
            <InputLabel>Tenant</InputLabel>
            <Select
              label="Tenant"
              value={tenantId ?? ''}
              displayEmpty
              onChange={(e) => handleTenantSwitch(e.target.value)}
            >
              {!tenants?.length && (
                <MenuItem value="" disabled>
                  {tenants ? 'No tenants found' : 'Loading…'}
                </MenuItem>
              )}
              {tenants?.map((t) => (
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
          {visibleItems.map((item, index) => {
            const Icon = item.icon;
            // For "Admin" href, only exact match is active (not all /admin/* pages)
            const active = item.href === '/admin'
              ? pathname === '/admin'
              : pathname === item.href || pathname.startsWith(item.href + '/');
            // Show divider before first tenant-scoped item (Dashboard) when role is ADMIN
            const showDivider = role === 'ADMIN' && item.label === 'Dashboard' && index > 0;
            return (
              <Box key={item.label + item.href}>
                {showDivider && (
                  <Box sx={{ px: 2, py: 0.5 }}>
                    <Typography variant="caption" color="text.disabled" sx={{ fontWeight: 600, letterSpacing: 0.5, textTransform: 'uppercase', fontSize: '0.65rem' }}>
                      Tenant
                    </Typography>
                  </Box>
                )}
                <ListItem disablePadding sx={{ px: 1, mb: 0.25 }}>
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
                      slotProps={{
                        primary: {
                          sx: { fontSize: '0.875rem', fontWeight: active ? 600 : 400 },
                        },
                      }}
                    />
                  </ListItemButton>
                </ListItem>
              </Box>
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
            <Typography variant="body2" sx={{ fontWeight: 600 }} noWrap>
              {currentUserName ?? 'User'}
            </Typography>
            <Tooltip title={currentUserEmail ?? ''}>
              <Typography variant="caption" color="text.secondary" noWrap sx={{ display: 'block' }}>
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
          onClick={() => { onClose?.(); signOut(); }}
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
