'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import {
  Box, Typography, Button, CircularProgress, Alert,
} from '@mui/material';
import { LocalParking } from '@mui/icons-material';
import { useAuthStore, resolveRedirectPath, LoginResult } from '@/store/authStore';

/** Build OAuth2 implicit-flow authorize URL. Always computed fresh at click time. */
function buildOAuthUrl(): string {
  const oauthHost  = process.env.NEXT_PUBLIC_OAUTH_HOST ?? 'http://localhost:9090';
  const clientId   = process.env.NEXT_PUBLIC_OAUTH_CLIENT_ID ?? 'cpa-tms-client';
  const redirectUri = `${window.location.origin}/login`;
  return (
    `${oauthHost}/oauth/authorize` +
    `?response_type=token` +
    `&client_id=${encodeURIComponent(clientId)}` +
    `&redirect_uri=${encodeURIComponent(redirectUri)}` +
    `&scope=read+write`
  );
}

export default function LoginPage() {
  const router              = useRouter();
  const loginWithOAuthToken = useAuthStore((s) => s.loginWithOAuthToken);
  const isLoggedIn          = useAuthStore((s) => s.isLoggedIn);

  const [processing, setProcessing] = useState(false);
  const [error,      setError]      = useState<string | null>(null);

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

        <Typography variant="body1" sx={{ color: '#555', mb: 1, textAlign: 'center', maxWidth: 560 }}>
          Welcome to the Parking Validation Management System.
          Please click the &ldquo;Welcome&rdquo; button to sign in to your account.
        </Typography>

        {error && (
          <Alert severity="error" sx={{ my: 2, maxWidth: 480, width: '100%' }} onClose={() => setError(null)}>
            {error}
          </Alert>
        )}

        <Box sx={{ mt: 3 }}>
          <Button
            variant="contained"
            size="large"
            onClick={() => { window.location.href = buildOAuthUrl(); }}
            sx={{
              bgcolor: '#2E7D6B',
              '&:hover': { bgcolor: '#245f54' },
              px: 6,
              py: 1.5,
              fontSize: '1.1rem',
              fontWeight: 600,
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
