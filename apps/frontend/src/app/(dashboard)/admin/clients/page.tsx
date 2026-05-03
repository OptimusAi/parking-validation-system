'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Button, Chip, Dialog, DialogActions, DialogContent, DialogTitle,
  MenuItem, Paper, Select, Snackbar, Alert, Stack, TextField, Typography,
  CircularProgress, InputLabel, FormControl,
} from '@mui/material';
import { DataGrid, type GridColDef } from '@mui/x-data-grid';
import { Add } from '@mui/icons-material';
import { listClients, createClient } from '@/lib/api';
import type { Client } from '@/lib/types';
import { PageHeader } from '@/components/common/PageHeader';
import { useTenantStore } from '@/store/tenantStore';

const PLAN_OPTIONS = ['STANDARD', 'ENTERPRISE'];

export default function AdminClientsPage() {
  const router = useRouter();
  const qc = useQueryClient();
  const role = useTenantStore((s) => s.role);

  const [dialogOpen, setDialogOpen] = useState(false);
  const [form, setForm] = useState({ name: '', plan: 'STANDARD' });
  const [toast, setToast] = useState<{ msg: string; sev: 'success' | 'error' } | null>(null);

  useEffect(() => {
    if (role && role !== 'ADMIN') {
      router.replace('/no-access');
    }
  }, [role, router]);

  const { data, isLoading } = useQuery({
    queryKey: ['clients'],
    queryFn: () => listClients(),
  });

  const createMutation = useMutation({
    mutationFn: () => createClient(form),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['clients'] });
      setDialogOpen(false);
      setForm({ name: '', plan: 'STANDARD' });
      setToast({ msg: 'Client created successfully', sev: 'success' });
    },
    onError: () => setToast({ msg: 'Failed to create client', sev: 'error' }),
  });

  const rows: Client[] = data?.content ?? [];

  const columns: GridColDef<Client>[] = [
    {
      field: 'name', headerName: 'Name', flex: 1, minWidth: 200,
      renderCell: ({ value }) => (
        <Typography variant="body2" sx={{ fontWeight: 600 }}>{value}</Typography>
      ),
    },
    {
      field: 'plan', headerName: 'Plan', width: 130,
      renderCell: ({ value }) => (
        <Chip
          label={value ?? 'STANDARD'}
          size="small"
          color={value === 'ENTERPRISE' ? 'primary' : 'default'}
        />
      ),
    },
    {
      field: 'isActive', headerName: 'Status', width: 100,
      renderCell: ({ value }) => (
        <Chip label={value === false ? 'INACTIVE' : 'ACTIVE'} size="small" color={value === false ? 'default' : 'success'} />
      ),
    },
    {
      field: 'createdAt', headerName: 'Created', width: 180,
      valueFormatter: (value: string) => value ? new Date(value).toLocaleDateString() : '—',
    },
    {
      field: 'id', headerName: 'ID', width: 300,
      renderCell: ({ value }) => (
        <Typography variant="caption" color="text.secondary" sx={{ fontFamily: 'monospace' }}>{value}</Typography>
      ),
    },
  ];

  return (
    <Box>
      <PageHeader
        title="Clients"
        subtitle="Manage top-level client organisations"
        action={
          <Button variant="contained" startIcon={<Add />} onClick={() => setDialogOpen(true)}>
            Add Client
          </Button>
        }
      />

      <Paper sx={{ overflow: 'hidden' }}>
        <DataGrid
          rows={rows}
          columns={columns}
          loading={isLoading}
          autoHeight
          disableRowSelectionOnClick
          pageSizeOptions={[20, 50, 100]}
          sx={{ border: 'none' }}
        />
      </Paper>

      {/* Create Dialog */}
      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Add Client</DialogTitle>
        <DialogContent>
          <Stack spacing={2.5} sx={{ mt: 1 }}>
            <TextField
              label="Client Name"
              value={form.name}
              onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
              required
              fullWidth
              placeholder="e.g. Calgary Parking Authority"
            />
            <FormControl fullWidth>
              <InputLabel id="plan-label">Plan</InputLabel>
              <Select
                labelId="plan-label"
                label="Plan"
                value={form.plan}
                onChange={(e) => setForm((f) => ({ ...f, plan: e.target.value }))}
              >
                {PLAN_OPTIONS.map((p) => (
                  <MenuItem key={p} value={p}>{p}</MenuItem>
                ))}
              </Select>
            </FormControl>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            disabled={!form.name.trim() || createMutation.isPending}
            onClick={() => createMutation.mutate()}
          >
            {createMutation.isPending ? <CircularProgress size={20} /> : 'Create Client'}
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar
        open={!!toast}
        autoHideDuration={toast?.sev === 'error' ? 5000 : 3000}
        onClose={() => setToast(null)}
      >
        <Alert severity={toast?.sev} onClose={() => setToast(null)}>{toast?.msg}</Alert>
      </Snackbar>
    </Box>
  );
}
