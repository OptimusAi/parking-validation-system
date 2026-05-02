'use client';

import { useState } from 'react';
import { useParams } from 'next/navigation';
import { useQuery, useMutation } from '@tanstack/react-query';
import {
  Box, Card, CardContent, Typography, TextField, Button,
  CircularProgress, Alert, Avatar, Stack, Divider,
} from '@mui/material';
import {
  CheckCircle, LocalParking, ErrorOutline, AccessTime, Place,
} from '@mui/icons-material';
import { format } from 'date-fns';
import { mockApi } from '@/lib/api';

type PageState = 'form' | 'success' | 'error';

export default function ValidatePage() {
  const { token } = useParams<{ token: string }>();
  const [plate, setPlate] = useState('');
  const [pageState, setPageState] = useState<PageState>('form');
  const [errorCode, setErrorCode] = useState<string | null>(null);
  const [validUntil, setValidUntil] = useState<string | null>(null);
  const [zoneName, setZoneName] = useState<string | null>(null);

  const { data: link, isLoading: linkLoading } = useQuery({
    queryKey: ['public-link', token],
    queryFn: () => mockApi.getPublicLink(token),
  });

  const mutation = useMutation({
    mutationFn: () => mockApi.submitPublicValidation(token, plate),
    onSuccess: (data) => {
      setValidUntil(data.validUntil);
      setZoneName(data.zoneName);
      setPageState('success');
    },
    onError: (err: Error & { code?: string }) => {
      setErrorCode(err.code ?? 'UNKNOWN');
      setPageState('error');
    },
  });

  const primaryColor = '#1B4F8A';

  const handlePlateChange = (value: string) => {
    setPlate(value.toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 10));
  };

  if (linkLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh' }}>
        <CircularProgress sx={{ color: primaryColor }} />
      </Box>
    );
  }

  return (
    <Box
      sx={{
        minHeight: '100vh',
        background: `linear-gradient(160deg, ${primaryColor}15 0%, #2E86C115 100%)`,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        p: 2,
      }}
    >
      <Box sx={{ width: '100%', maxWidth: 420 }}>
        {/* Header */}
        <Box sx={{ textAlign: 'center', mb: 3 }}>
          <Avatar
            sx={{ bgcolor: primaryColor, width: 56, height: 56, mx: 'auto', mb: 2 }}
          >
            <LocalParking sx={{ fontSize: 30 }} />
          </Avatar>
          <Typography variant="h5" fontWeight={700} color="text.primary">
            Downtown Parking
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Parking Validation
          </Typography>
        </Box>

        <Card>
          <CardContent sx={{ p: 3 }}>
            {/* Zone Info */}
            {link && (
              <Stack direction="row" spacing={2} sx={{ mb: 3, p: 2, bgcolor: `${primaryColor}08`, borderRadius: 2 }}>
                <Place sx={{ color: primaryColor, mt: 0.25 }} />
                <Box>
                  <Typography variant="body2" fontWeight={600}>{link.zoneName}</Typography>
                  <Typography variant="caption" color="text.secondary">
                    {link.durationMinutes >= 60
                      ? `${link.durationMinutes / 60}h`
                      : `${link.durationMinutes}min`} validation
                  </Typography>
                </Box>
              </Stack>
            )}

            <Divider sx={{ mb: 3 }} />

            {/* Success State */}
            {pageState === 'success' && (
              <Box sx={{ textAlign: 'center', py: 2 }}>
                <CheckCircle sx={{ fontSize: 72, color: 'success.main', mb: 2 }} />
                <Typography variant="h5" fontWeight={700} color="success.main" gutterBottom>
                  Parking Validated!
                </Typography>
                <Typography variant="body1" color="text.secondary" gutterBottom>
                  Your parking is validated.
                </Typography>
                <Box sx={{ mt: 3, p: 2, bgcolor: 'success.main', borderRadius: 2, color: 'white' }}>
                  <Stack direction="row" spacing={1} alignItems="center" justifyContent="center">
                    <AccessTime sx={{ fontSize: 18 }} />
                    <Typography variant="body2" fontWeight={600}>
                      Valid until {validUntil ? format(new Date(validUntil), 'h:mm a, MMM d') : ''}
                    </Typography>
                  </Stack>
                </Box>
                {zoneName && (
                  <Typography variant="caption" color="text.secondary" sx={{ mt: 2, display: 'block' }}>
                    Zone: {zoneName}
                  </Typography>
                )}
                <Button
                  variant="outlined"
                  sx={{ mt: 3 }}
                  onClick={() => { setPageState('form'); setPlate(''); }}
                >
                  Validate Another Vehicle
                </Button>
              </Box>
            )}

            {/* Error State */}
            {pageState === 'error' && (
              <Box sx={{ textAlign: 'center', py: 2 }}>
                <ErrorOutline sx={{ fontSize: 64, color: 'error.main', mb: 2 }} />
                <Typography variant="h6" fontWeight={600} color="error.main" gutterBottom>
                  {errorCode === 'ALREADY_VALIDATED'
                    ? 'Already Validated'
                    : errorCode === 'QUOTA_EXCEEDED'
                    ? 'Quota Exceeded'
                    : 'Validation Failed'}
                </Typography>
                <Alert severity="error" sx={{ mt: 2, textAlign: 'left' }}>
                  {errorCode === 'ALREADY_VALIDATED'
                    ? 'This vehicle has already been validated for this session.'
                    : errorCode === 'QUOTA_EXCEEDED'
                    ? 'The daily validation quota has been reached. Please contact the facility.'
                    : 'An unexpected error occurred. Please try again.'}
                </Alert>
                <Button
                  variant="outlined"
                  sx={{ mt: 3 }}
                  onClick={() => { setPageState('form'); setPlate(''); setErrorCode(null); }}
                >
                  Try Again
                </Button>
              </Box>
            )}

            {/* Form State */}
            {pageState === 'form' && (
              <Box>
                <Typography variant="body1" fontWeight={600} gutterBottom>
                  Enter Your License Plate
                </Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                  Enter the plate number shown on your vehicle.
                </Typography>
                <TextField
                  fullWidth
                  label="License Plate"
                  value={plate}
                  onChange={(e) => handlePlateChange(e.target.value)}
                  placeholder="e.g. ABC123"
                  inputProps={{ style: { textTransform: 'uppercase', fontSize: '1.25rem', letterSpacing: 4, textAlign: 'center' } }}
                  sx={{ mb: 3 }}
                  size="medium"
                />
                <Button
                  fullWidth
                  variant="contained"
                  size="large"
                  disabled={plate.length < 2 || mutation.isPending}
                  onClick={() => mutation.mutate()}
                  sx={{ py: 1.5, bgcolor: primaryColor }}
                >
                  {mutation.isPending ? (
                    <CircularProgress size={24} sx={{ color: 'white' }} />
                  ) : (
                    'Validate Parking'
                  )}
                </Button>
                <Typography variant="caption" color="text.secondary" sx={{ mt: 2, display: 'block', textAlign: 'center' }}>
                  Tip: Try TAKEN01 or QUOTA99 to test error states.
                </Typography>
              </Box>
            )}
          </CardContent>
        </Card>

        <Typography variant="caption" color="text.secondary" textAlign="center" display="block" sx={{ mt: 2 }}>
          Powered by TMS Parking Validation
        </Typography>
      </Box>
    </Box>
  );
}
