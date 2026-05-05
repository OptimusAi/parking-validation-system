'use client';

import { useMemo } from 'react';
import NextLink from 'next/link';
import { useParams, usePathname, useRouter } from 'next/navigation';
import {
  Box, Drawer, List, ListItem, ListItemButton, ListItemIcon,
  ListItemText, Typography, Avatar, Divider, Select, IconButton,
  MenuItem, FormControl, InputLabel, Tooltip,
} from '@mui/material';
import {
  Dashboard, DirectionsCar, QrCode2, LocationOn, Business,
  Assessment, PieChart, History, Palette, People, Logout,
  Apartment, AdminPanelSettings, LocalParking,
  ChevronLeft, ChevronRight,
} from '@mui/icons-material';
import { useQuery } from '@tanstack/react-query';
import { useTenantStore } from '@/store/tenantStore';
import { useAuthStore } from '@/store/authStore';
import { mockApi } from '@/lib/api';

export const DRAWER_WIDTH      = 240;
export const DRAWER_WIDTH_MINI = 64;

interface NavItem {
  label: string;
  href: string;
  icon: React.ElementType;
  roles: string[];
  section: string;
}

function buildNavItems(clientId: string | null, tenantId: string | null): NavItem[] {
  const tenantItems: NavItem[] = tenantId ? [
    { label: 'Dashboard',   href: clientId ? `/${clientId}/${tenantId}/dashboard`         : '#', icon: Dashboard,     roles: ['ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN', 'SUB_TENANT_ADMIN'], section: 'OPERATIONS' },
    { label: 'Validations', href: clientId ? `/${clientId}/${tenantId}/validations`       : '#', icon: DirectionsCar, roles: ['ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN', 'SUB_TENANT_ADMIN'], section: 'OPERATIONS' },
    { label: 'Links',       href: clientId ? `/${clientId}/${tenantId}/links`             : '#', icon: QrCode2,       roles: ['ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN', 'SUB_TENANT_ADMIN'], section: 'OPERATIONS' },
    { label: 'Zones',       href: clientId ? `/${clientId}/${tenantId}/zones`             : '#', icon: LocationOn,    roles: ['ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN'],                     section: 'OPERATIONS' },
    { label: 'Sub-Tenants', href: clientId ? `/${clientId}/${tenantId}/sub-tenants`       : '#', icon: Apartment,     roles: ['ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN'],                     section: 'OPERATIONS' },
    { label: 'Reports',     href: clientId ? `/${clientId}/${tenantId}/reports`           : '#', icon: Assessment,    roles: ['ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN'],                     section: 'ANALYTICS'  },
    { label: 'Quota',       href: clientId ? `/${clientId}/${tenantId}/quota`             : '#', icon: PieChart,      roles: ['ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN'],                     section: 'ANALYTICS'  },
    { label: 'Audit Logs',  href: clientId ? `/${clientId}/${tenantId}/audit-logs`        : '#', icon: History,       roles: ['ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN'],                     section: 'ANALYTICS'  },
    { label: 'Branding',    href: clientId ? `/${clientId}/${tenantId}/settings/branding` : '#', icon: Palette,       roles: ['ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN'],                     section: 'SETTINGS'   },
    { label: 'Users',       href: clientId ? `/${clientId}/${tenantId}/users`             : '#', icon: People,        roles: ['ADMIN', 'CLIENT_ADMIN'],                                     section: 'SETTINGS'   },
  ] : [];
  return [
    { label: 'Admin',   href: '/admin',                                        icon: AdminPanelSettings, roles: ['ADMIN'],         section: 'SYSTEM'    },
    { label: 'Clients', href: '/admin/clients',                                icon: Business,           roles: ['ADMIN'],         section: 'SYSTEM'    },
    { label: 'Tenants', href: '/admin/tenants',                                icon: Business,           roles: ['ADMIN'],         section: 'SYSTEM'    },
    { label: 'Users',   href: '/admin/users',                                  icon: People,             roles: ['ADMIN'],         section: 'SYSTEM'    },
    { label: 'Zones',   href: '/admin/zones',                                  icon: LocationOn,         roles: ['ADMIN'],         section: 'SYSTEM'    },
    { label: 'Tenants', href: clientId ? `/${clientId}/tenants` : '#',         icon: Business,           roles: ['CLIENT_ADMIN'],  section: 'WORKSPACE' },
    ...tenantItems,
  ];
}

interface SidebarContentProps {
  collapsed: boolean;
  onToggle: () => void;
  onClose?: () => void;
}

function SidebarContent({ collapsed, onToggle, onClose }: SidebarContentProps) {
  const pathname = usePathname();
  const router   = useRouter();
  const params   = useParams<{ clientId?: string; tenantId?: string }>();
  const storeClientId = useTenantStore((s) => s.clientId);
  const storeTenantId = useTenantStore((s) => s.tenantId);
  const clientId = params?.clientId ?? (storeClientId || null);
  const tenantId = params?.tenantId ?? (storeTenantId || null);

  const { currentUserName, role, branding, switchTenant } = useTenantStore();
  const signOut = useAuthStore((s) => s.signOut);

  const { data: tenants } = useQuery({
    queryKey: ['tenants'],
    queryFn: mockApi.getTenants,
    enabled: role === 'ADMIN' || role === 'CLIENT_ADMIN',
  });

  const navItems     = useMemo(() => buildNavItems(clientId, tenantId), [clientId, tenantId]);
  const visibleItems = useMemo(() => {
    const filtered = navItems.filter((item) => !role || item.roles.includes(role));
    return filtered.map((item, i) => ({
      ...item,
      firstInSection: i === 0 || item.section !== filtered[i - 1].section,
    }));
  }, [navItems, role]);

  const initials = currentUserName
    ? currentUserName.split(' ').map((n) => n[0]).join('').slice(0, 2)
    : 'U';

  const handleTenantSwitch = (newTenantId: string) => {
    const tenant = tenants?.find((t) => t.id === newTenantId);
    const resolvedClientId = tenant?.clientId ?? clientId;
    switchTenant(newTenantId, resolvedClientId, undefined);
    if (resolvedClientId) router.push(`/${resolvedClientId}/${newTenantId}/dashboard`);
    onClose?.();
  };

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%', overflow: 'hidden' }}>

      {/* Logo row */}
      <Box sx={{
        px: collapsed ? 1 : 2, py: 0,
        display: 'flex', alignItems: 'center',
        justifyContent: collapsed ? 'center' : 'flex-start',
        borderBottom: '1px solid', borderColor: 'divider',
        minHeight: 64, gap: 1,
      }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, minWidth: 0 }}>
          <Box sx={{
            width: 36, height: 36, borderRadius: '10px', bgcolor: 'primary.main',
            display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
          }}>
            <LocalParking sx={{ fontSize: 22, color: 'black' }} />
          </Box>
          {!collapsed && (
            <Box sx={{ minWidth: 0 }}>
              <Typography sx={{ fontWeight: 700, fontSize: '0.9375rem', color: 'text.primary', lineHeight: 1.2 }} noWrap>
                TMS Parking
              </Typography>
              <Typography sx={{ fontSize: '0.6875rem', color: 'text.secondary', textTransform: 'uppercase', letterSpacing: '0.06em', fontWeight: 600 }} noWrap>
                Dashboard
              </Typography>
            </Box>
          )}
        </Box>
      </Box>

      {/* Tenant Switcher */}
      {!collapsed && (role === 'ADMIN' || role === 'CLIENT_ADMIN') && (
        <Box sx={{ px: 2, pt: 1.5 }}>
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
                <MenuItem key={t.id} value={t.id}>{t.name}</MenuItem>
              ))}
            </Select>
          </FormControl>
        </Box>
      )}

      {/* Navigation */}
      <Box sx={{ flex: 1, overflowY: 'auto', overflowX: 'hidden', py: 1 }}>
        <List dense disablePadding>
          {visibleItems.map((item) => {
            const Icon   = item.icon;
            const active = item.href === '/admin'
              ? pathname === '/admin'
              : pathname === item.href || pathname.startsWith(item.href + '/');

            return (
              <Box key={item.label + item.href}>
                {item.firstInSection && !collapsed && (
                  <Box sx={{ px: 2, pt: 1.5, pb: 0.25 }}>
                    <Typography sx={{ fontSize: '0.6rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.08em', color: 'text.disabled' }}>
                      {item.section}
                    </Typography>
                  </Box>
                )}
                <ListItem disablePadding sx={{ px: 0.75, mb: 0.25 }}>
                  <Tooltip title={collapsed ? item.label : ''} placement="right" enterDelay={0}>
                    <ListItemButton
                      component={NextLink}
                      href={item.href}
                      onClick={onClose}
                      selected={active}
                      sx={{
                        borderRadius: '8px',
                        py: 0.75,
                        px: 1.25,
                        justifyContent: collapsed ? 'center' : 'flex-start',
                        minHeight: 40,
                        '&.Mui-selected': {
                          bgcolor: 'action.selected',
                          '& .MuiListItemIcon-root': { color: 'text.primary' },
                          '&:hover': { bgcolor: 'action.selected' },
                        },
                        '&:hover': { bgcolor: 'action.hover' },
                      }}
                    >
                      <ListItemIcon sx={{
                        minWidth: collapsed ? 0 : 34,
                        color: active ? 'text.primary' : 'text.secondary',
                        justifyContent: 'center',
                      }}>
                        <Icon sx={{ fontSize: 20 }} />
                      </ListItemIcon>
                      {!collapsed && (
                        <ListItemText
                          primary={item.label}
                          slotProps={{
                            primary: {
                              sx: { fontSize: '0.875rem', fontWeight: active ? 600 : 400, color: active ? 'text.primary' : 'text.secondary' },
                            },
                          }}
                        />
                      )}
                    </ListItemButton>
                  </Tooltip>
                </ListItem>
              </Box>
            );
          })}
        </List>
      </Box>

      {/* User Footer */}
      <Box sx={{ p: collapsed ? 1 : 2, borderTop: '1px solid', borderColor: 'divider' }}>
        {collapsed ? (
          <Tooltip title={`${currentUserName ?? 'User'} · Sign out`} placement="right" enterDelay={0}>
            <IconButton onClick={() => { onClose?.(); signOut(); }} sx={{ p: 0.5 }}>
              <Avatar sx={{ width: 34, height: 34, bgcolor: 'primary.main', fontSize: '0.75rem', fontWeight: 700, color: '#000' }}>
                {initials}
              </Avatar>
            </IconButton>
          </Tooltip>
        ) : (
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
            <Avatar sx={{ width: 34, height: 34, bgcolor: 'primary.main', fontSize: '0.8rem', fontWeight: 700, color: '#000' }}>
              {initials}
            </Avatar>
            <Box sx={{ flex: 1, minWidth: 0 }}>
              <Typography sx={{ fontSize: '0.875rem', fontWeight: 600, color: 'text.primary' }} noWrap>
                {currentUserName ?? 'User'}
              </Typography>
              <Typography sx={{ fontSize: '0.75rem', color: 'text.secondary' }} noWrap>
                {role?.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase()) ?? 'User'}
              </Typography>
            </Box>
            <Tooltip title="Sign out">
              <IconButton size="small" onClick={() => { onClose?.(); signOut(); }} sx={{ color: 'text.secondary', '&:hover': { color: 'text.primary' } }}>
                <Logout sx={{ fontSize: 18 }} />
              </IconButton>
            </Tooltip>
          </Box>
        )}
      </Box>
    </Box>
  );
}

interface SidebarProps {
  mobileOpen: boolean;
  onMobileClose: () => void;
  collapsed: boolean;
  onToggle: () => void;
}

export function Sidebar({ mobileOpen, onMobileClose, collapsed, onToggle }: SidebarProps) {
  const width = collapsed ? DRAWER_WIDTH_MINI : DRAWER_WIDTH;

  return (
    <>
      {/* Mobile Drawer — always full width */}
      <Drawer
        variant="temporary"
        open={mobileOpen}
        onClose={onMobileClose}
        ModalProps={{ keepMounted: true }}
        sx={{
          display: { xs: 'block', md: 'none' },
          '& .MuiDrawer-paper': { width: DRAWER_WIDTH, boxSizing: 'border-box', bgcolor: 'background.default' },
        }}
      >
        <SidebarContent collapsed={false} onToggle={onToggle} onClose={onMobileClose} />
      </Drawer>

      {/* Desktop Drawer — collapsible */}
      <Box
        sx={{
          display: { xs: 'none', md: 'block' },
          position: 'relative',
          flexShrink: 0,
          width,
          transition: 'width 0.2s ease',
        }}
      >
        <Drawer
          variant="permanent"
          sx={{
            width,
            flexShrink: 0,
            transition: 'width 0.2s ease',
            '& .MuiDrawer-paper': {
              width,
              boxSizing: 'border-box',
              borderRight: '1px solid',
              borderColor: 'divider',
              bgcolor: 'background.default',
              overflow: 'hidden',
              transition: 'width 0.2s ease',
            },
          }}
          open
        >
          <SidebarContent collapsed={collapsed} onToggle={onToggle} />
        </Drawer>

        {/* Floating collapse toggle — right edge of sidebar */}
        <Tooltip title={collapsed ? 'Expand sidebar' : 'Collapse sidebar'} placement="right" enterDelay={0}>
          <IconButton
            size="small"
            aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
            onClick={onToggle}
            sx={{
              position: 'absolute',
              top: 72,
              right: -14,
              zIndex: (theme) => theme.zIndex.drawer + 1,
              width: 28, height: 28,
              bgcolor: 'background.paper',
              border: '1px solid',
              borderColor: 'divider',
              borderRadius: '50%',
              color: 'text.secondary',
              boxShadow: '0 1px 4px rgba(0,0,0,0.25)',
              '&:hover': { bgcolor: 'action.hover', color: 'text.primary' },
            }}
          >
            {collapsed
              ? <ChevronRight sx={{ fontSize: 16 }} />
              : <ChevronLeft  sx={{ fontSize: 16 }} />}
          </IconButton>
        </Tooltip>
      </Box>
    </>
  );
}

export const SIDEBAR_WIDTH      = DRAWER_WIDTH;
export const SIDEBAR_WIDTH_MINI = DRAWER_WIDTH_MINI;


