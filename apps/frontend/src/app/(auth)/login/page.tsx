'use client';

/**
 * Login Page — mirrors TMS loginPage.jsx + oauth.jsx exactly.
 *
 * Flow:
 *  1. On mount: check URL hash for #access_token=...  (OAuth implicit-flow callback)
 *     → found  : POST /api/auth/login with Authorization: Bearer <token> → redirect to dashboard
 *     → not found: fetch loginUrl from GET /api/auth/login-url → render "Welcome" button
 *  2. "Welcome" button: <a href={loginUrl}> → OAuth server login page
 *  3. OAuth server redirects back to this page with #access_token=... → step 1
 */

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import {
  Box, Typography, Button, CircularProgress, Alert,
} from '@mui/material';
import { LocalParking } from '@mui/icons-material';
import { useAuthStore, resolveRedirectPath, LoginResult } from '@/store/authStore';

/** Build OAuth2 implicit-flow authorize URL from env vars (same as backend /api/auth/login-url). */
function buildLoginUrl(): string {
  const oauthHost = process.env.NEXT_PUBLIC_OAUTH_HOST ?? 'http://localhost:9090';
  const clientId  = process.env.NEXT_PUBLIC_OAUTH_CLIENT_ID ?? 'cpa-tms-client';
  const redirectUri = typeof window !== 'undefined'
    ? `${window.location.origin}/login`
    : 'http://localhost:3000/login';
  return (
    `${oauthHost}/oauth/authorize` +
    `?response_type=token` +
    `&client_id=${encodeURIComponent(clientId)}` +
    `&redirect_uri=${encodeURIComponent(redirectUri)}` +
    `&scope=read+write`
  );
}

export default function LoginPage() {
  const router               = useRouter();
  const loginWithOAuthToken  = useAuthStore((s) => s.loginWithOAuthToken);

  const [loginUrl]             = useState<string>(() => buildLoginUrl());
  const [processing,   setProcessing]  = useState(false);
  const [error,        setError]       = useState<string | null>(null);

  const redirect = (result: LoginResult) => router.replace(resolveRedirectPath(result));

  useEffect(() => {
    // ── Step 1: check if OAuth server redirected back with a token in the hash ──
    const hash  = window.location.hash.substring(1);
    const match = hash.match(/access_token=([^&]+)/);

    if (match) {
      // Clean hash so pressing Back doesn't replay
      window.history.replaceState(null, '', window.location.pathname);
      setProcessing(true);
      loginWithOAuthToken(match[1])
        .then(redirect)
        .catch((err: Error) => {
          setError(err.message ?? 'Authentication failed. Please try again.');
          setProcessing(false);
        });
      return;
    }

    // ── Step 2: no token — show the Welcome button (loginUrl already built from env vars)
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // ── Processing: token is being exchanged ─────────────────────────────────
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

        {/* ── App title (TMS: <h2>{i18n.t('app_title')}</h2>) ── */}
        <Typography variant="h4" sx={{ fontWeight: 700, color: '#2c3e50', mb: 2, textAlign: 'center' }}>
          Parking Validation System
        </Typography>

        {/* ── Logo ── */}
        <Box sx={{ mb: 3 }}>
          <Box
            sx={{
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              width: 80,
              height: 80,
              borderRadius: '20px',
              bgcolor: '#1B4F8A',
              boxShadow: '0 4px 20px rgba(27,79,138,0.25)',
            }}
          >
            <LocalParking sx={{ fontSize: 48, color: 'white' }} />
          </Box>
        </Box>

        {/* ── Welcome message (TMS: welcome_message + continue text) ── */}
        <Typography variant="body1" sx={{ color: '#555', mb: 1, textAlign: 'center', maxWidth: 560 }}>
          Welcome to the Parking Validation Management System.
          Please click the &ldquo;Welcome&rdquo; button to sign in to your account.
        </Typography>

        {error && (
          <Alert severity="error" sx={{ my: 2, maxWidth: 480, width: '100%' }} onClose={() => setError(null)}>
            {error}
          </Alert>
        )}

        {/* ── Welcome button ── */}
        <Box sx={{ mt: 3 }}>
          <Button
            component="a"
            href={loginUrl}
            variant="contained"
            size="large"
            sx={{
              bgcolor: '#2E7D6B',
              '&:hover': { bgcolor: '#245f54' },
              px: 6,
              py: 1.5,
              fontSize: '1.1rem',
              fontWeight: 600,
              textDecoration: 'none',
              borderRadius: '6px',
              boxShadow: '0 3px 12px rgba(46,125,107,0.35)',
            }}
          >
            Welcome
          </Button>
        </Box>

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
