'use client';

import { useState, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Button, Chip, Stack, Dialog, DialogTitle, DialogContent,
  DialogActions, TextField, Typography, Paper, Snackbar, Alert,
  CircularProgress, Avatar, FormControl, InputLabel, Select, MenuItem,
} from '@mui/material';
import { DataGrid, type GridColDef } from '@mui/x-data-grid';
import { Add, CloudUpload, ArrowForward } from '@mui/icons-material';
import { mockApi, listClients, updateBranding } from '@/lib/api';
import type { Tenant } from '@/lib/types';
import { PageHeader } from '@/components/common/PageHeader';

export default function AdminTenantsPage() {
  const router = useRouter();
  const qc = useQueryClient();

  const [dialogOpen, setDialogOpen] = useState(false);
  const [form, setForm] = useState({ name: '', primaryColor: '#1B4F8A', accentColor: '#2E86C1', clientId: '' });
  const [logoPreview, setLogoPreview] = useState<string | null>(null);
  const [logoFile, setLogoFile] = useState<File | null>(null);
  const [dragOver, setDragOver] = useState(false);
  const [toast, setToast] = useState<{ msg: string; sev: 'success' | 'error' } | null>(null);

  const { data: tenants, isLoading } = useQuery({
    queryKey: ['tenants'],
    queryFn: mockApi.getTenants,
  });

  const { data: clientsPage } = useQuery({
    queryKey: ['clients'],
    queryFn: () => listClients({ pageSize: 200 }),
  });
  const clientOptions = clientsPage?.content ?? [];

  const createMutation = useMutation({
    mutationFn: async () => {
      const tenant = await mockApi.createTenant({ name: form.name, clientId: form.clientId });
      // Save branding (logo + colors) in a follow-up call
      const brandingData = new FormData();
      brandingData.append('primaryColor', form.primaryColor);
      brandingData.append('accentColor', form.accentColor);
      if (logoFile) brandingData.append('logoFile', logoFile);
      await updateBranding(tenant.id, brandingData);
      return tenant;
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['tenants'] });
      setDialogOpen(false);
      setForm({ name: '', primaryColor: '#1B4F8A', accentColor: '#2E86C1', clientId: '' });
      setLogoPreview(null);
      setLogoFile(null);
      setToast({ msg: 'Tenant created successfully', sev: 'success' });
    },
    onError: () => setToast({ msg: 'Failed to create tenant', sev: 'error' }),
  });

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(false);
    const file = e.dataTransfer.files[0];
    if (file && ['image/png', 'image/svg+xml', 'image/jpeg'].includes(file.type) && file.size <= 2 * 1024 * 1024) {
      setLogoFile(file);
      const reader = new FileReader();
      reader.onload = (ev) => setLogoPreview(ev.target?.result as string);
      reader.readAsDataURL(file);
    } else {
      setToast({ msg: 'Invalid file (PNG/SVG/JPG, max 2MB)', sev: 'error' });
    }
  }, []);

  const handleFileInput = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (!['image/png', 'image/svg+xml', 'image/jpeg'].includes(file.type) || file.size > 2 * 1024 * 1024) {
      setToast({ msg: 'Invalid file (PNG/SVG/JPG, max 2MB)', sev: 'error' });
      return;
    }
    setLogoFile(file);
    const reader = new FileReader();
    reader.onload = (ev) => setLogoPreview(ev.target?.result as string);
    reader.readAsDataURL(file);
  };

  const tenantList = tenants ?? [];

  const columns: GridColDef<Tenant>[] = [
    {
      field: 'name', headerName: 'Name', flex: 1, minWidth: 180,
      renderCell: ({ row }) => (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
          <Avatar
            src={row.branding?.logoUrl || undefined}
            sx={{ bgcolor: row.branding?.primaryColor ?? '#1B4F8A', width: 32, height: 32, fontSize: '0.75rem', fontWeight: 700 }}
          >
            {row.name.slice(0, 2).toUpperCase()}
          </Avatar>
          <Typography variant="body2" sx={{ fontWeight: 600 }}>{row.name}</Typography>
        </Box>
      ),
    },
    {
      field: 'clientId', headerName: 'Client', width: 220,
      renderCell: ({ value }) => {
        const client = clientOptions.find((c) => c.id === value);
        return <Typography variant="body2">{client?.name ?? value ?? '—'}</Typography>;
      },
    },
    { field: 'zones', headerName: 'Zones', width: 80 },
    { field: 'subTenants', headerName: 'Sub-Tenants', width: 120 },
    {
      field: 'isActive', headerName: 'Status', width: 100,
      renderCell: ({ row }) => {
        const active = row.status ? row.status === 'ACTIVE' : row.isActive !== false;
        return <Chip label={active ? 'ACTIVE' : 'INACTIVE'} size="small" color={active ? 'success' : 'default'} />;
      },
    },
    {
      field: 'actions', headerName: '', width: 120, sortable: false,
      renderCell: ({ row }) => (
        <Button
          size="small"
          endIcon={<ArrowForward sx={{ fontSize: 16 }} />}
          onClick={(e) => { e.stopPropagation(); router.push(`/${row.clientId}/${row.id}/dashboard`); }}
        >
          Open
        </Button>
      ),
    },
  ];

  return (
    <Box>
      <PageHeader
        title="Tenants"
        subtitle="Manage parking tenant locations"
        action={<Button variant="contained" startIcon={<Add />} onClick={() => setDialogOpen(true)}>Add Tenant</Button>}
      />

      <Paper sx={{ overflow: 'hidden' }}>
        <DataGrid
          rows={tenantList}
          columns={columns}
          loading={isLoading}
          autoHeight
          disableRowSelectionOnClick
          pageSizeOptions={[20, 50, 100]}
          onRowClick={(params) => router.push(`/${params.row.clientId}/${params.row.id}/dashboard`)}
          sx={{ border: 'none', '& .MuiDataGrid-row': { cursor: 'pointer' } }}
        />
      </Paper>

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Add Tenant</DialogTitle>
        <DialogContent>
          <Stack spacing={2.5} sx={{ mt: 1 }}>
            <FormControl fullWidth required>
              <InputLabel id="client-label">Client</InputLabel>
              <Select
                labelId="client-label"
                label="Client"
                value={form.clientId}
                onChange={(e) => setForm((f) => ({ ...f, clientId: e.target.value }))}
              >
                {clientOptions.map((c) => (
                  <MenuItem key={c.id} value={c.id}>{c.name}</MenuItem>
                ))}
              </Select>
            </FormControl>

            <TextField
              label="Tenant Name"
              value={form.name}
              onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
              required
              fullWidth
            />

            <Box>
              <Typography variant="body2" sx={{ fontWeight: 500 }} gutterBottom>Logo (optional)</Typography>
              <Box
                onDrop={handleDrop}
                onDragOver={(e) => { e.preventDefault(); setDragOver(true); }}
                onDragLeave={() => setDragOver(false)}
                onClick={() => document.getElementById('admin-tenant-logo')?.click()}
                sx={{
                  border: '2px dashed',
                  borderColor: dragOver ? 'primary.main' : 'divider',
                  borderRadius: 2,
                  p: 2.5,
                  textAlign: 'center',
                  bgcolor: dragOver ? 'primary.light' : 'background.default',
                  cursor: 'pointer',
                }}
              >
                {logoPreview ? (
                  <img src={logoPreview} alt="preview" style={{ maxHeight: 60, maxWidth: 180 }} />
                ) : (
                  <Box>
                    <CloudUpload sx={{ fontSize: 32, color: 'text.secondary' }} />
                    <Typography variant="caption" sx={{ display: 'block' }} color="text.secondary">
                      PNG, SVG, or JPG — max 2MB
                    </Typography>
                  </Box>
                )}
              </Box>
              <input id="admin-tenant-logo" type="file" accept="image/png,image/svg+xml,image/jpeg" style={{ display: 'none' }} onChange={handleFileInput} />
            </Box>

            <Stack direction="row" spacing={2}>
              <Box>
                <Typography variant="body2" sx={{ fontWeight: 500 }} gutterBottom>Primary Color</Typography>
                <TextField type="color" value={form.primaryColor} onChange={(e) => setForm((f) => ({ ...f, primaryColor: e.target.value }))} sx={{ width: 100 }} />
              </Box>
              <Box>
                <Typography variant="body2" sx={{ fontWeight: 500 }} gutterBottom>Accent Color</Typography>
                <TextField type="color" value={form.accentColor} onChange={(e) => setForm((f) => ({ ...f, accentColor: e.target.value }))} sx={{ width: 100 }} />
              </Box>
            </Stack>

            <Box sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 2, overflow: 'hidden' }}>
              <Box sx={{ p: 1.5, bgcolor: form.primaryColor, color: 'white', textAlign: 'center' }}>
                <Typography variant="body2" sx={{ fontWeight: 700 }}>{form.name || 'Tenant Name'}</Typography>
              </Box>
              <Box sx={{ p: 1.5, display: 'flex', gap: 1, justifyContent: 'center' }}>
                <Chip label="QR Code" size="small" sx={{ bgcolor: form.primaryColor, color: 'white' }} />
                <Chip label="Active" size="small" sx={{ bgcolor: form.accentColor, color: 'white' }} />
              </Box>
            </Box>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            disabled={!form.name || !form.clientId || createMutation.isPending}
            onClick={() => createMutation.mutate()}
          >
            {createMutation.isPending ? <CircularProgress size={20} /> : 'Create Tenant'}
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar open={!!toast} autoHideDuration={toast?.sev === 'error' ? 5000 : 3000} onClose={() => setToast(null)}>
        <Alert severity={toast?.sev} onClose={() => setToast(null)}>{toast?.msg}</Alert>
      </Snackbar>
    </Box>
  );
}
