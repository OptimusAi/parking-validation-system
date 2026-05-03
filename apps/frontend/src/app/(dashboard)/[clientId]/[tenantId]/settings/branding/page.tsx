'use client';

import { useState, useCallback } from 'react';
import { useParams } from 'next/navigation';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Grid, Paper, Typography, Button, TextField, Stack,
  Snackbar, Alert, CircularProgress, Chip, Avatar,
} from '@mui/material';
import { CloudUpload, LocalParking, CheckCircle } from '@mui/icons-material';
import { mockApi } from '@/lib/api';
import { useTenantStore } from '@/store/tenantStore';
import { PageHeader } from '@/components/common/PageHeader';

export default function BrandingPage() {
  const { tenantId } = useParams<{ tenantId: string }>();
  const { branding, setBranding } = useTenantStore();
  const qc = useQueryClient();

  const [primary, setPrimary] = useState(branding.primaryColor);
  const [accent, setAccent] = useState(branding.accentColor);
  const [logoFile, setLogoFile] = useState<File | null>(null);
  const [logoPreview, setLogoPreview] = useState<string | null>(branding.logoUrl);
  const [dragOver, setDragOver] = useState(false);
  const [toast, setToast] = useState<{ msg: string; sev: 'success' | 'error' } | null>(null);

  const saveMutation = useMutation({
    mutationFn: () => mockApi.updateBranding(tenantId, { primaryColor: primary, accentColor: accent, logoUrl: logoPreview }),
    onSuccess: (data) => {
      setBranding(data);
      qc.invalidateQueries({ queryKey: ['branding'] });
      setToast({ msg: 'Branding saved successfully', sev: 'success' });
    },
    onError: () => setToast({ msg: 'Failed to save branding', sev: 'error' }),
  });

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(false);
    const file = e.dataTransfer.files[0];
    if (file && ['image/png', 'image/svg+xml', 'image/jpeg'].includes(file.type)) {
      if (file.size > 2 * 1024 * 1024) {
        setToast({ msg: 'File too large. Max 2MB.', sev: 'error' });
        return;
      }
      setLogoFile(file);
      const reader = new FileReader();
      reader.onload = (ev) => setLogoPreview(ev.target?.result as string);
      reader.readAsDataURL(file);
    } else {
      setToast({ msg: 'Invalid file type. Use PNG, SVG, or JPG.', sev: 'error' });
    }
  }, []);

  const handleFileInput = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (!['image/png', 'image/svg+xml', 'image/jpeg'].includes(file.type)) {
      setToast({ msg: 'Invalid file type. Use PNG, SVG, or JPG.', sev: 'error' });
      return;
    }
    if (file.size > 2 * 1024 * 1024) {
      setToast({ msg: 'File too large. Max 2MB.', sev: 'error' });
      return;
    }
    setLogoFile(file);
    const reader = new FileReader();
    reader.onload = (ev) => setLogoPreview(ev.target?.result as string);
    reader.readAsDataURL(file);
  };

  const handleApplyPreview = () => {
    setBranding({ ...branding, primaryColor: primary, accentColor: accent });
    setToast({ msg: 'Preview applied', sev: 'success' });
  };

  return (
    <Box>
      <PageHeader title="Branding Settings" subtitle="Customize your tenant's visual identity" />

      <Grid container spacing={3}>
        {/* Left: Settings */}
        <Grid size={{ xs: 12, md: 7 }}>
          <Stack spacing={3}>
            {/* Logo Section */}
            <Paper sx={{ p: 3 }}>
              <Typography variant="h6" sx={{ fontWeight: 600 }} gutterBottom>Logo</Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                Upload your logo (PNG, SVG, or JPG, max 2MB)
              </Typography>

              <Box
                onDrop={handleDrop}
                onDragOver={(e) => { e.preventDefault(); setDragOver(true); }}
                onDragLeave={() => setDragOver(false)}
                sx={{
                  border: '2px dashed',
                  borderColor: dragOver ? 'primary.main' : 'divider',
                  borderRadius: 2,
                  p: 3,
                  textAlign: 'center',
                  bgcolor: dragOver ? 'primary.light' : 'background.default',
                  transition: 'all 0.2s ease',
                  cursor: 'pointer',
                  mb: 2,
                }}
                onClick={() => document.getElementById('logo-upload')?.click()}
              >
                {logoPreview ? (
                  <Box>
                    <img src={logoPreview} alt="Logo preview" style={{ maxHeight: 80, maxWidth: 200, objectFit: 'contain' }} />
                    <Typography variant="caption" sx={{ display: 'block', mt: 1 }} color="text.secondary">
                      {logoFile?.name ?? 'Current logo'}
                    </Typography>
                  </Box>
                ) : (
                  <Box>
                    <CloudUpload sx={{ fontSize: 40, color: 'text.secondary', mb: 1 }} />
                    <Typography variant="body2" color="text.secondary">
                      Drop your logo here or click to upload
                    </Typography>
                  </Box>
                )}
              </Box>
              <input id="logo-upload" type="file" accept="image/png,image/svg+xml,image/jpeg" style={{ display: 'none' }} onChange={handleFileInput} />
            </Paper>

            {/* Colors Section */}
            <Paper sx={{ p: 3 }}>
              <Typography variant="h6" sx={{ fontWeight: 600 }} gutterBottom>Brand Colors</Typography>
              <Stack spacing={2.5}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                  <TextField
                    label="Primary Color"
                    type="color"
                    value={primary}
                    onChange={(e) => setPrimary(e.target.value)}
                    sx={{ width: 120 }}
                    slotProps={{ inputLabel: { shrink: true } }}
                  />
                  <Box>
                    <Typography variant="body2" sx={{ fontWeight: 600 }}>{primary}</Typography>
                    <Typography variant="caption" color="text.secondary">Used for buttons, highlights</Typography>
                  </Box>
                  <Box sx={{ width: 40, height: 40, borderRadius: 1, bgcolor: primary, ml: 'auto', border: '1px solid rgba(0,0,0,0.1)' }} />
                </Box>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                  <TextField
                    label="Accent Color"
                    type="color"
                    value={accent}
                    onChange={(e) => setAccent(e.target.value)}
                    sx={{ width: 120 }}
                    slotProps={{ inputLabel: { shrink: true } }}
                  />
                  <Box>
                    <Typography variant="body2" sx={{ fontWeight: 600 }}>{accent}</Typography>
                    <Typography variant="caption" color="text.secondary">Used for secondary elements</Typography>
                  </Box>
                  <Box sx={{ width: 40, height: 40, borderRadius: 1, bgcolor: accent, ml: 'auto', border: '1px solid rgba(0,0,0,0.1)' }} />
                </Box>
              </Stack>
              <Stack direction="row" spacing={1.5} sx={{ mt: 3 }}>
                <Button variant="outlined" onClick={handleApplyPreview}>Preview</Button>
                <Button
                  variant="contained"
                  disabled={saveMutation.isPending}
                  onClick={() => saveMutation.mutate()}
                  startIcon={saveMutation.isPending ? <CircularProgress size={16} sx={{ color: 'white' }} /> : undefined}
                >
                  Save Branding
                </Button>
              </Stack>
            </Paper>
          </Stack>
        </Grid>

        {/* Right: Live Preview */}
        <Grid size={{ xs: 12, md: 5 }}>
          <Paper sx={{ p: 3, position: 'sticky', top: 80 }}>
              <Typography variant="h6" sx={{ fontWeight: 600 }} gutterBottom>Live Preview</Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              This is how your validation page will look.
            </Typography>

            {/* Mock Validation Card */}
            <Box
              sx={{
                border: '1px solid',
                borderColor: 'divider',
                borderRadius: 2,
                overflow: 'hidden',
              }}
            >
              {/* Header */}
              <Box sx={{ p: 2, bgcolor: primary, color: 'white', textAlign: 'center' }}>
                {logoPreview ? (
                  <img src={logoPreview} alt="logo" style={{ height: 40, objectFit: 'contain', marginBottom: 4 }} />
                ) : (
                  <Avatar sx={{ bgcolor: 'rgba(255,255,255,0.2)', mx: 'auto', mb: 1, width: 44, height: 44 }}>
                    <LocalParking />
                  </Avatar>
                )}
                <Typography variant="body1" sx={{ fontWeight: 700 }}>Downtown Parking</Typography>
                <Typography variant="caption" sx={{ opacity: 0.8 }}>Parking Validation</Typography>
              </Box>

              {/* Form Preview */}
              <Box sx={{ p: 2 }}>
                <Box sx={{ p: 1.5, bgcolor: `${primary}12`, borderRadius: 1.5, mb: 2 }}>
                  <Typography variant="caption" color="text.secondary">Zone: Ground Floor • 60 min</Typography>
                </Box>
                <TextField
                  fullWidth
                  label="License Plate"
                  placeholder="ABC 123"
                  size="small"
                  disabled
                  sx={{ mb: 1.5 }}
                />
                <Button
                  fullWidth
                  variant="contained"
                  size="small"
                  sx={{ bgcolor: primary, '&:hover': { bgcolor: primary } }}
                  disabled
                >
                  Validate Parking
                </Button>
              </Box>

              {/* Success Preview */}
              <Box sx={{ p: 2, bgcolor: '#F1F8E9', borderTop: '1px solid', borderColor: 'divider' }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <CheckCircle sx={{ color: '#2E7D32', fontSize: 20 }} />
                  <Typography variant="body2" sx={{ fontWeight: 600 }} color="#2E7D32">Parking Validated!</Typography>
                </Box>
                <Box sx={{ mt: 1, p: 1, bgcolor: accent, borderRadius: 1 }}>
                  <Typography variant="caption" color="white">Valid until 2:30 PM</Typography>
                </Box>
              </Box>
            </Box>
          </Paper>
        </Grid>
      </Grid>

      <Snackbar open={!!toast} autoHideDuration={toast?.sev === 'error' ? 5000 : 3000} onClose={() => setToast(null)}>
        <Alert severity={toast?.sev} onClose={() => setToast(null)}>{toast?.msg}</Alert>
      </Snackbar>
    </Box>
  );
}
