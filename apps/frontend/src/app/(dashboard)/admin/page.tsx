'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { Box, Typography, Card, CardContent, CardActionArea } from '@mui/material';
import { Business, People, Assessment, Settings, LocationOn } from '@mui/icons-material';
import { useTenantStore } from '@/store/tenantStore';

const ADMIN_SECTIONS = [
  {
    label: 'Clients',
    description: 'Manage clients and their tenant assignments.',
    icon: Business,
    color: '#1565C0',
    bg: '#E3F2FD',
    href: '/admin/clients',
  },
  {
    label: 'Users',
    description: 'View and manage all system users.',
    icon: People,
    color: '#2E7D32',
    bg: '#E8F5E9',
    href: '/admin/users',
  },
  {
    label: 'Zones',
    description: 'Assign and manage parking zones for any tenant.',
    icon: LocationOn,
    color: '#00695C',
    bg: '#E0F2F1',
    href: '/admin/zones',
  },
  {
    label: 'Reports',
    description: 'System-wide validation and quota reports.',
    icon: Assessment,
    color: '#E65100',
    bg: '#FFF3E0',
    href: '#', // future: /admin/reports
  },
  {
    label: 'Settings',
    description: 'Global platform configuration.',
    icon: Settings,
    color: '#6A1B9A',
    bg: '#F3E5F5',
    href: '#', // future: /admin/settings
  },
] as const;

export default function AdminPage() {
  const router = useRouter();
  const role   = useTenantStore((s) => s.role);

  // Guard: only ADMIN role may access this page
  useEffect(() => {
    if (role && role !== 'ADMIN') {
      router.replace('/no-access');
    }
  }, [role, router]);

  const name = useTenantStore((s) => s.currentUserName);

  return (
    <Box>
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" sx={{ fontWeight: 700 }}>
          System Administration
        </Typography>
        <Typography variant="body1" color="text.secondary" sx={{ mt: 0.5 }}>
          Welcome{name ? `, ${name}` : ''}. You have full system access.
        </Typography>
      </Box>

      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', md: 'repeat(4, 1fr)' }, gap: 3 }}>
        {ADMIN_SECTIONS.map((section) => {
          const Icon = section.icon;
          return (
            <Card
              key={section.label}
              sx={{
                border: '1px solid',
                borderColor: 'divider',
                '&:hover': { boxShadow: 4 },
                transition: 'box-shadow 0.2s',
              }}
            >
              <CardActionArea
                href={section.href}
                sx={{ p: 1 }}
              >
                <CardContent>
                  <Box
                    sx={{
                      display: 'inline-flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      width: 48,
                      height: 48,
                      borderRadius: 2,
                      bgcolor: section.bg,
                      mb: 2,
                    }}
                  >
                    <Icon sx={{ color: section.color, fontSize: 26 }} />
                  </Box>
                  <Typography variant="h6" sx={{ fontWeight: 600, mb: 0.5 }}>
                    {section.label}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    {section.description}
                  </Typography>
                </CardContent>
              </CardActionArea>
            </Card>
          );
        })}
      </Box>
    </Box>
  );
}
