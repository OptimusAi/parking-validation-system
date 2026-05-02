'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import {
  Box, Card, CardContent, Typography, TextField, Button,
  Alert, CircularProgress, InputAdornment, IconButton,
  Avatar, Chip, Stack, Divider,
} from '@mui/material';
import {
  LocalParking, Visibility, VisibilityOff,
  AdminPanelSettings, Business, AccountTree, Person,
} from '@mui/icons-material';
import { useAuthStore } from '@/store/authStore';

// Seeded demo users (local-auth-enabled=true, all share password Admin1234!)
const DEMO_USERS = [
  {
    email: 'venu.kannuri@optimus-ai.com',
    name: 'Venu Kannuri',
    role: 'ADMIN' as const,
    label: 'System Admin',
    icon: AdminPanelSettings,
    color: '#D32F2F',
    bg: '#FFEBEE',
  },
  {
    email: 'venukannuri.cloud@gmail.com',
    name: 'Venu Kannuri (Cloud)',
    role: 'CLIENT_ADMIN' as const,
    label: 'Client Admin',
    icon: Business,
    color: '#1565C0',
    bg: '#E3F2FD',
  },
  {
    email: 'cpatest1100@gmail.com',
    name: 'CPA Test – Tenant Admin',
    role: 'TENANT_ADMIN' as const,
    label: 'Tenant Admin',
    icon: AccountTree,
    color: '#2E7D32',
    bg: '#E8F5E9',
  },
  {
    email: 'cpatest8963@gmail.com',
    name: 'CPA Test – Sub-Tenant',
    role: 'SUBTENANT_USER' as const,
    label: 'Sub-Tenant User',
    icon: Person,
    color: '#E65100',
    bg: '#FFF3E0',
  },
] as const;

const DEMO_PASSWORD = 'Admin1234!';

export default function LoginPage() {
  const router = useRouter();
  const login = useAuthStore((s) => s.login);

  const [loadingEmail, setLoadingEmail] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Manual form state
  const [showManual, setShowManual] = useState(false);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [formLoading, setFormLoading] = useState(false);

  const doLogin = async (email: string, pwd: string) => {
    setError(null);
    try {
      const { clientId, tenantId, role, assignedTenants } = await login(email, pwd);

      // Build the best redirect based on what the backend returned
      if (clientId && tenantId) {
        router.push(`/${clientId}/${tenantId}/dashboard`);
      } else if (clientId && assignedTenants.length > 0) {
        router.push(`/${clientId}/${assignedTenants[0]}/dashboard`);
      } else if (clientId) {
        router.push(`/${clientId}/tenants`);
      } else if (role === 'ADMIN') {
        // ADMIN is system-wide with no assigned client/tenant.
        // Redirect to the seeded default — sidebar tenant-switcher lets them navigate.
        router.push('/11111111-1111-1111-1111-111111111111/22222222-2222-2222-2222-222222222222/dashboard');
      } else {
        setError('No tenant assigned to this account. Contact your administrator.');
      }
    } catch (err: unknown) {
      setError((err as Error).message ?? 'Login failed');
    }
  };

  const handleCardClick = async (email: string) => {
    if (loadingEmail) return;
    setLoadingEmail(email);
    await doLogin(email, DEMO_PASSWORD);
    setLoadingEmail(null);
  };

  const handleManualSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormLoading(true);
    await doLogin(username, password);
    setFormLoading(false);
  };

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        p: 2,
        background: 'linear-gradient(135deg, #1B4F8A15 0%, #2E86C115 100%)',
      }}
    >
      <Box sx={{ width: '100%', maxWidth: 480 }}>
        {/* Header */}
        <Box sx={{ textAlign: 'center', mb: 4 }}>
          <Box
            sx={{
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              width: 64,
              height: 64,
              borderRadius: '16px',
              bgcolor: 'primary.main',
              mb: 2,
              boxShadow: '0 4px 20px rgba(27,79,138,0.3)',
            }}
          >
            <LocalParking sx={{ fontSize: 36, color: 'white' }} />
          </Box>
          <Typography variant="h4" sx={{ fontWeight: 700 }} color="text.primary">
            TMS Parking
          </Typography>
          <Typography variant="body1" color="text.secondary" sx={{ mt: 0.5 }}>
            Validation Platform
          </Typography>
        </Box>

        <Card sx={{ overflow: 'visible' }}>
          <CardContent sx={{ p: 3 }}>
            {error && (
              <Alert severity="error" sx={{ mb: 2 }}>
                {error}
              </Alert>
            )}

            {!showManual ? (
              <>
                <Typography variant="h6" sx={{ fontWeight: 600 }} gutterBottom>
                  Select a Demo User
                </Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
                  Click any role card to sign in with seeded local credentials.
                </Typography>

                <Stack spacing={1.5}>
                  {DEMO_USERS.map((user) => {
                    const Icon = user.icon;
                    const isLoading = loadingEmail === user.email;
                    const anyLoading = !!loadingEmail;
                    return (
                      <Box
                        key={user.email}
                        onClick={() => handleCardClick(user.email)}
                        sx={{
                          display: 'flex',
                          alignItems: 'center',
                          gap: 2,
                          p: 2,
                          borderRadius: 2,
                          border: '1px solid',
                          borderColor: 'divider',
                          cursor: anyLoading ? 'default' : 'pointer',
                          opacity: anyLoading && !isLoading ? 0.5 : 1,
                          transition: 'all 0.15s ease',
                          ...(!anyLoading && {
                            '&:hover': {
                              borderColor: 'primary.main',
                              bgcolor: 'action.hover',
                              transform: 'translateY(-1px)',
                              boxShadow: '0 4px 12px rgba(0,0,0,0.08)',
                            },
                          }),
                        }}
                      >
                        <Avatar sx={{ bgcolor: user.bg, width: 44, height: 44, flexShrink: 0 }}>
                          {isLoading
                            ? <CircularProgress size={20} sx={{ color: user.color }} />
                            : <Icon sx={{ color: user.color, fontSize: 22 }} />}
                        </Avatar>
                        <Box sx={{ flex: 1, minWidth: 0 }}>
                          <Typography variant="body2" sx={{ fontWeight: 600 }} noWrap>
                            {user.name}
                          </Typography>
                          <Typography variant="caption" color="text.secondary" noWrap sx={{ display: 'block' }}>
                            {user.email}
                          </Typography>
                        </Box>
                        <Chip
                          label={user.label}
                          size="small"
                          sx={{
                            bgcolor: user.bg,
                            color: user.color,
                            '& .MuiChip-label': { fontWeight: 600 },
                            fontSize: '0.7rem',
                            flexShrink: 0,
                          }}
                        />
                      </Box>
                    );
                  })}
                </Stack>

                <Divider sx={{ my: 3 }} />
                <Button
                  fullWidth
                  variant="text"
                  size="small"
                  onClick={() => { setShowManual(true); setError(null); }}
                  sx={{ color: 'text.secondary', fontSize: '0.8rem' }}
                >
                  Sign in with email & password
                </Button>
              </>
            ) : (
              <>
                <Typography variant="h6" sx={{ fontWeight: 600 }} gutterBottom>
                  Sign In
                </Typography>

                <Box component="form" onSubmit={handleManualSubmit} noValidate>
                  <TextField
                    label="Email"
                    type="email"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    fullWidth
                    required
                    autoComplete="username"
                    sx={{ mb: 2 }}
                    disabled={formLoading}
                  />
                  <TextField
                    label="Password"
                    type={showPassword ? 'text' : 'password'}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    fullWidth
                    required
                    autoComplete="current-password"
                    sx={{ mb: 3 }}
                    disabled={formLoading}
                    slotProps={{
                      input: {
                        endAdornment: (
                          <InputAdornment position="end">
                            <IconButton
                              onClick={() => setShowPassword((v) => !v)}
                              edge="end"
                              aria-label={showPassword ? 'Hide password' : 'Show password'}
                            >
                              {showPassword ? <VisibilityOff /> : <Visibility />}
                            </IconButton>
                          </InputAdornment>
                        ),
                      },
                    }}
                  />
                  <Button
                    type="submit"
                    variant="contained"
                    fullWidth
                    size="large"
                    disabled={formLoading || !username || !password}
                    startIcon={formLoading ? <CircularProgress size={18} color="inherit" /> : null}
                  >
                    {formLoading ? 'Signing in…' : 'Sign In'}
                  </Button>
                </Box>

                <Divider sx={{ my: 3 }} />
                <Button
                  fullWidth
                  variant="text"
                  size="small"
                  onClick={() => { setShowManual(false); setError(null); }}
                  sx={{ color: 'text.secondary', fontSize: '0.8rem' }}
                >
                  ← Back to demo users
                </Button>
              </>
            )}
          </CardContent>
        </Card>
      </Box>
    </Box>
  );
}
