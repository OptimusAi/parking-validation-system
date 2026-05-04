'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Button, Chip, Stack, Dialog, DialogTitle, DialogContent,
  DialogActions, DialogContentText, TextField, Select, MenuItem,
  FormControl, InputLabel, RadioGroup, FormControlLabel, Radio,
  Switch, FormLabel, Typography, Paper, Snackbar, Alert, CircularProgress,
} from '@mui/material';
import { DataGrid, type GridColDef } from '@mui/x-data-grid';
import { QrCode2, ContentCopy, PictureAsPdf, Block, Add } from '@mui/icons-material';
import { QRCodeSVG } from 'qrcode.react';
import { format } from 'date-fns';
import { mockApi } from '@/lib/api';
import type { ValidationLink } from '@/lib/types';
import { PageHeader } from '@/components/common/PageHeader';

export default function LinksPage() {
  const qc = useQueryClient();

  const [createOpen, setCreateOpen] = useState(false);
  const [qrLink, setQrLink] = useState<ValidationLink | null>(null);
  const [deactivateLink, setDeactivateLink] = useState<ValidationLink | null>(null);
  const [toast, setToast] = useState<{ msg: string; sev: 'success' | 'error' | 'info' } | null>(null);

  const [form, setForm] = useState({
    zoneId: '', type: 'QR' as 'QR' | 'URL', durationMinutes: 60, label: '', expiresAt: '',
  });

  const { data, isLoading } = useQuery({
    queryKey: ['links'],
    queryFn: () => mockApi.getLinks(),
  });

  const { data: zones } = useQuery({ queryKey: ['zones'], queryFn: mockApi.getZones });

  const createMutation = useMutation({
    mutationFn: () => {
      const zone = zones?.find((z) => z.id === form.zoneId);
      return mockApi.createLink({
        ...form,
        zoneName: zone?.name ?? '',
        expiresAt: form.expiresAt ? `${form.expiresAt}T00:00:00Z` : undefined,
      });
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['links'] });
      setCreateOpen(false);
      setForm({ zoneId: '', type: 'QR', durationMinutes: 60, label: '', expiresAt: '' });
      setToast({ msg: 'Link created successfully', sev: 'success' });
    },
  });

  const deactivateMutation = useMutation({
    mutationFn: (id: string) => mockApi.deactivateLink(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['links'] });
      setDeactivateLink(null);
      setToast({ msg: 'Link deactivated', sev: 'success' });
    },
  });

  const handleCopy = (url: string) => {
    navigator.clipboard.writeText(url).catch(() => {});
    setToast({ msg: 'URL copied to clipboard!', sev: 'info' });
  };

  const handlePdf = async (id: string) => {
    setToast({ msg: 'Generating PDF...', sev: 'info' });
    await mockApi.generateQrPdf(id);
    setToast({ msg: 'PDF ready!', sev: 'success' });
  };

  const links = data?.content ?? [];

  const columns: GridColDef<ValidationLink>[] = [
    { field: 'label', headerName: 'Label', flex: 1, minWidth: 140, valueGetter: (_, row) => row.label ?? '—' },
    {
      field: 'linkType', headerName: 'Type', width: 90,
      renderCell: ({ value }) => (
        <Chip label={value} size="small" color={value === 'QR' ? 'primary' : 'secondary'} icon={<QrCode2 sx={{ fontSize: 14 }} />} />
      ),
    },
    { field: 'zoneName', headerName: 'Zone', width: 130, valueGetter: (_, row) => row.zoneName ?? '—' },
    { field: 'defaultDurationMinutes', headerName: 'Duration', width: 100, valueFormatter: (v: number) => `${v}m` },
    { field: 'scanCount', headerName: 'Scans', width: 80 },
    { field: 'expiresAt', headerName: 'Expires', width: 140, valueFormatter: (v?: string) => v ? format(new Date(v), 'MMM d, yyyy') : 'Never' },
    {
      field: 'isActive', headerName: 'Active', width: 80,
      renderCell: ({ value }) => (
        <Chip label={value ? 'Active' : 'Inactive'} size="small" color={value ? 'success' : 'default'} />
      ),
    },
    {
      field: 'actions', headerName: 'Actions', width: 180, sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" spacing={0.5} sx={{ height: '100%', alignItems: 'center' }}>
          <Button size="small" variant="outlined" sx={{ minWidth: 0, px: 1 }} onClick={() => setQrLink(row)}>
            <QrCode2 sx={{ fontSize: 16 }} />
          </Button>
          <Button size="small" variant="outlined" sx={{ minWidth: 0, px: 1 }} onClick={() => handleCopy(`${window.location.origin}/validate/${row.token}`)}>
            <ContentCopy sx={{ fontSize: 16 }} />
          </Button>
          <Button size="small" variant="outlined" sx={{ minWidth: 0, px: 1 }} onClick={() => handlePdf(row.id)}>
            <PictureAsPdf sx={{ fontSize: 16 }} />
          </Button>
          {row.isActive && (
            <Button size="small" variant="outlined" color="error" sx={{ minWidth: 0, px: 1 }} onClick={() => setDeactivateLink(row)}>
              <Block sx={{ fontSize: 16 }} />
            </Button>
          )}
        </Stack>
      ),
    },
  ];

  return (
    <Box>
      <PageHeader
        title="Validation Links"
        subtitle="QR codes and URLs for parking validation"
        action={
          <Button variant="contained" startIcon={<Add />} onClick={() => setCreateOpen(true)}>
            Generate New Link
          </Button>
        }
      />

      <Paper sx={{ overflow: 'hidden' }}>
        <DataGrid
          rows={links}
          columns={columns}
          loading={isLoading}
          autoHeight
          disableRowSelectionOnClick
          pageSizeOptions={[20, 50, 100]}
          initialState={{ pagination: { paginationModel: { pageSize: 20 } } }}
          sx={{ border: 'none' }}
        />
      </Paper>

      {/* Create Dialog */}
      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Generate New Link</DialogTitle>
        <DialogContent>
          <Stack spacing={2.5} sx={{ mt: 1 }}>
            <FormControl fullWidth>
              <InputLabel>Zone</InputLabel>
              <Select value={form.zoneId} label="Zone" onChange={(e) => setForm((f) => ({ ...f, zoneId: e.target.value }))}>
                {zones?.map((z) => <MenuItem key={z.id} value={z.id}>{z.name}</MenuItem>)}
              </Select>
            </FormControl>
            <FormControl>
              <FormLabel>Type</FormLabel>
              <RadioGroup row value={form.type} onChange={(e) => setForm((f) => ({ ...f, type: e.target.value as 'QR' | 'URL' }))}>
                <FormControlLabel value="QR" control={<Radio />} label="QR Code" />
                <FormControlLabel value="URL" control={<Radio />} label="URL Link" />
              </RadioGroup>
            </FormControl>
            <TextField
              label="Duration (minutes)"
              type="number"
              value={form.durationMinutes}
              onChange={(e) => setForm((f) => ({ ...f, durationMinutes: parseInt(e.target.value) || 60 }))}
              slotProps={{ htmlInput: { min: 1, max: 1440 } }}
            />
            <TextField
              label="Label (optional)"
              value={form.label}
              onChange={(e) => setForm((f) => ({ ...f, label: e.target.value }))}
              placeholder="e.g. Main Entrance"
            />
            <TextField
              label="Expiry Date (optional)"
              type="date"
              value={form.expiresAt}
              onChange={(e) => setForm((f) => ({ ...f, expiresAt: e.target.value }))}
              slotProps={{ inputLabel: { shrink: true } }}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            disabled={!form.zoneId || createMutation.isPending}
            onClick={() => createMutation.mutate()}
          >
            {createMutation.isPending ? <CircularProgress size={20} /> : 'Create Link'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* QR Dialog */}
      <Dialog open={!!qrLink} onClose={() => setQrLink(null)} maxWidth="xs" fullWidth>
        <DialogTitle>QR Code — {qrLink?.label ?? qrLink?.zoneName}</DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', py: 2, gap: 2 }}>
            <QRCodeSVG value={qrLink?.url ?? 'https://tms.cpa.ca/validate/mock-token-123'} size={256} />
            <Typography variant="caption" color="text.secondary" sx={{ textAlign: 'center', maxWidth: 280, wordBreak: 'break-all' }}>
              {qrLink?.url}
            </Typography>
            <Button
              variant="outlined"
              startIcon={<ContentCopy />}
              onClick={() => qrLink && handleCopy(qrLink.url)}
            >
              Copy URL
            </Button>
          </Box>
        </DialogContent>
      </Dialog>

      {/* Deactivate Dialog */}
      <Dialog open={!!deactivateLink} onClose={() => setDeactivateLink(null)} maxWidth="xs" fullWidth>
        <DialogTitle>Deactivate Link?</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Deactivate <strong>{deactivateLink?.label ?? deactivateLink?.zoneName}</strong>? Users will no longer be able to validate parking using this link.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeactivateLink(null)}>Keep Active</Button>
          <Button
            variant="contained"
            color="error"
            disabled={deactivateMutation.isPending}
            onClick={() => deactivateLink && deactivateMutation.mutate(deactivateLink.id)}
          >
            {deactivateMutation.isPending ? <CircularProgress size={20} /> : 'Deactivate'}
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar open={!!toast} autoHideDuration={toast?.sev === 'error' ? 5000 : 3000} onClose={() => setToast(null)}>
        <Alert severity={toast?.sev} onClose={() => setToast(null)}>{toast?.msg}</Alert>
      </Snackbar>
    </Box>
  );
}
