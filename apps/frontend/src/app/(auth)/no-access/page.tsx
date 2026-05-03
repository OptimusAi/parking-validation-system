'use client';

import { Box, Typography, Button, Paper } from '@mui/material';
import { LocalParking, ErrorOutlined } from '@mui/icons-material';
import { useAuthStore } from '@/store/authStore';

export default function NoAccessPage() {
  const signOut = useAuthStore((s) => s.signOut);

  const handleSignOut = () => {
    signOut();
  };

  return (
    <Box
      sx={{
        minHeight: '100vh',
        bgcolor: '#e8e8e8',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        p: 3,
      }}
    >
      <Paper
        elevation={2}
        sx={{
          maxWidth: 480,
          width: '100%',
          p: 5,
          textAlign: 'center',
          borderRadius: 3,
        }}
      >
        {/* App icon */}
        <Box
          sx={{
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            width: 64,
            height: 64,
            borderRadius: '16px',
            bgcolor: '#1B4F8A',
            mb: 3,
          }}
        >
          <LocalParking sx={{ fontSize: 36, color: 'white' }} />
        </Box>

        {/* Warning icon */}
        <Box sx={{ mb: 2 }}>
          <ErrorOutlined sx={{ fontSize: 56, color: '#E65100' }} />
        </Box>

        <Typography variant="h5" sx={{ fontWeight: 700, mb: 1.5, color: '#2c3e50' }}>
          No Tenant Assigned
        </Typography>

        <Typography variant="body1" color="text.secondary" sx={{ mb: 1 }}>
          Your account is not assigned to any tenant yet.
        </Typography>

        <Typography variant="body2" color="text.secondary" sx={{ mb: 4 }}>
          Please contact your administrator to request access.
        </Typography>

        <Button
          variant="contained"
          onClick={handleSignOut}
          sx={{
            bgcolor: '#1B4F8A',
            '&:hover': { bgcolor: '#163d6e' },
            px: 4,
            py: 1.2,
            fontWeight: 600,
          }}
        >
          Sign Out
        </Button>
      </Paper>
    </Box>
  );
}
