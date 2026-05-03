'use client';

import { useEffect, useState, FormEvent } from 'react';
import { useRouter } from 'next/navigation';
import {
  Box, Typography, Button, CircularProgress, Alert,
  TextField, Divider,
} from '@mui/material';
import { LocalParking } from '@mui/icons-material';
import { useAuthStore, resolveRedirectPath, LoginResult } from '@/store/authStore';

/** Build OAuth2 implicit-flow authorize URL. Always computed fresh so the
 *  force-reauth sessionStorage flag is never stale. */
function buildOAuthUrl(): string {
  const oauthHost  = process.env.NEXT_PUBLIC_OAUTH_HOST ?? 'http://localhost:9090';
  const clientId   = process.env.NEXT_PUBLIC_OAUTH_CLIENT_ID ?? 'cpa-tms-client';
  const redirectUri = `${window.location.origin}/login`;
  const forceReauth = sessionStorage.getItem('tms-force-reauth') === '1';
  if (forceReauth) sessionStorage.removeItem('tms-force-reauth');
  return (
    `${oauthHost}/oauth/authorize` +
    `?response_type=token` +
    `&client_id=${encodeURIComponent(clientId)}` +
    `&redirect_uri=${encodeURIComponent(redirectUri)}` +
    `&scope=read+write` +
    (forceReauth ? '&prompt=login' : '')
  );
}

export default function LoginPage() {
  const router              = useRouter();
  const login               = useAuthStore((s) => s.login);
  const loginWithOAuthToken = useAuthStore((s) => s.loginWithOAuthToken);
  const isLoggedIn          = useAuthStore((s) => s.isLoggedIn);

  const [processing, setProcessing] = useState(false);
  const [error,      setError]      = useState<string | null>(null);
  const [username,   setUsername]   = useState('');
  const [password,   setPassword]   = useState('');

  const redirect = (result: LoginResult) => router.replace(resolveRedirectPath(result));

  // Redirect already-authenticated users away from the login page
  useEffect(() => {
    if (isLoggedIn && !window.location.hash.includes('access_token')) {
      router.replace('/admin');
    }
  }, [isLoggedIn, router]);

  // Handle OAuth implicit-flow callback (token in URL hash)
  useEffect(() => {
    const hash  = window.location.hash.substring(1);
    const match = hash.match(/access_token=([^&]+)/);
    if (match) {
      window.history.replaceState(null, '', window.location.pathname);
      setProcessing(true);
      loginWithOAuthToken(match[1])
        .then(redirect)
        .catch((err: Error) => {
          setError(err.message ?? 'Authentication failed. Please try again.');
          setProcessing(false);
        });
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // Username / password submit
  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!username.trim() || !password) return;
    setProcessing(true);
    setError(null);
    try {
      const result = await login(username.trim(), password);
      redirect(result);
    } catch (err) {
      setError((err as Error).message ?? 'Login failed');
      setProcessing(false);
    }
  };

  // ── Processing spinner ────────────────────────────────────────────────────
  if (processing) {
    return (
      <Box sx={pageStyle}>
        <Box sx={jumbotronStyle}>
          <CircularProgress size={44} sx={{ color: '#2E7D6B' }} />
          <Typography variant="body1" sx={{ mt: 2, color: '#555' }}>
            Signing you in…
          </Typography>
        </Box>
      </Box>
    );
  }

  return (
    <Box sx={pageStyle}>
      <Box sx={jumbotronStyle}>

        <Typography variant="h4" sx={{ fontWeight: 700, color: '#2c3e50', mb: 2, textAlign: 'center' }}>
          Parking Validation System
        </Typography>

        <Box sx={{ mb: 3 }}>
          <Box sx={{
            display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
            width: 80, height: 80, borderRadius: '20px', bgcolor: '#1B4F8A',
            boxShadow: '0 4px 20px rgba(27,79,138,0.25)',
          }}>
            <LocalParking sx={{ fontSize: 48, color: 'white' }} />
          </Box>
        </Box>

        {error && (
          <Alert severity="error" sx={{ mb: 2, maxWidth: 400, width: '100%' }} onClose={() => setError(null)}>
            {error}
          </Alert>
        )}

        {/* ── Username / password form ── */}
        <Box component="form" onSubmit={handleSubmit} sx={{ width: '100%', maxWidth: 400 }}>
          <TextField
            label="Username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            fullWidth
            size="small"
            autoComplete="username"
            sx={{ mb: 2 }}
          />
          <TextField
            label="Password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            fullWidth
            size="small"
            autoComplete="current-password"
            sx={{ mb: 2 }}
          />
          <Button
            type="submit"
            variant="contained"
            fullWidth
            disabled={!username.trim() || !password}
            sx={{
              bgcolor: '#1B4F8A', '&:hover': { bgcolor: '#163f6e' },
              py: 1.2, fontWeight: 600, borderRadius: '6px',
            }}
          >
            Sign In
          </Button>
        </Box>

        {/* ── OAuth divider + button ── */}
        <Divider sx={{ my: 3, width: '100%', maxWidth: 400 }}>
          <Typography variant="caption" color="text.secondary">OR</Typography>
        </Divider>

        <Button
          variant="outlined"
          size="large"
          onClick={() => { window.location.href = buildOAuthUrl(); }}
          sx={{
            borderColor: '#2E7D6B', color: '#2E7D6B',
            '&:hover': { borderColor: '#245f54', bgcolor: 'rgba(46,125,107,0.06)' },
            px: 6, py: 1.2, fontWeight: 600, borderRadius: '6px',
          }}
        >
          Sign in with SSO
        </Button>

      </Box>
    </Box>
  );
}

// ── Styles (mirrors TMS .container-fluid > .jumbotron) ───────────────────────

const pageStyle = {
  minHeight: '100vh',
  bgcolor: '#e8e8e8',
  display: 'flex',
  alignItems: 'flex-start',
  justifyContent: 'center',
  pt: 0,
};

const jumbotronStyle = {
  width: '100%',
  bgcolor: '#f0f0f0',
  py: 6,
  px: 4,
  display: 'flex',
  flexDirection: 'column',
  alignItems: 'center',
  boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
};
