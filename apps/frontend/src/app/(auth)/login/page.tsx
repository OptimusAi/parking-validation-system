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
          <CircularProgress size={44} color="primary" />
          <Typography variant="body1" color="text.secondary" sx={{ mt: 2 }}>
            Signing you in…
          </Typography>
        </Box>
      </Box>
    );
  }

  return (
    <Box sx={pageStyle}>
      <Box sx={jumbotronStyle}>

        <Typography variant="h4" sx={{ fontWeight: 700, mb: 2, textAlign: 'center' }} color="text.primary">
          Parking Validation System
        </Typography>

        <Box sx={{ mb: 3 }}>
          <Box sx={{
            display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
            width: 80, height: 80, borderRadius: '20px', bgcolor: 'primary.main',
            boxShadow: '0 4px 20px rgba(0,0,0,0.2)',
          }}>
            <LocalParking sx={{ fontSize: 48, color: 'white' }} />
          </Box>
        </Box>

        <Typography variant="body1" color="text.secondary" sx={{ mb: 1, textAlign: 'center', maxWidth: 560 }}>
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
            color="primary"
            size="large"
            onClick={() => { window.location.href = buildOAuthUrl(); }}
            sx={{
              px: 6,
              py: 1.5,
              fontSize: '1.1rem',
              fontWeight: 600,
              borderRadius: '10px',
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
  bgcolor: 'background.default',
  display: 'flex',
  alignItems: 'flex-start',
  justifyContent: 'center',
  pt: 0,
};

const jumbotronStyle = {
  width: '100%',
  bgcolor: 'background.paper',
  py: 6,
  px: 4,
  display: 'flex',
  flexDirection: 'column',
  alignItems: 'center',
  borderBottom: '1px solid',
  borderColor: 'divider',
};
