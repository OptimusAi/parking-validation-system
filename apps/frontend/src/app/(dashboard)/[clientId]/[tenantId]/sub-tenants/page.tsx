'use client';

import { useState } from 'react';
import { useParams } from 'next/navigation';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Button, Switch, Stack, Dialog, DialogTitle, DialogContent,
  DialogActions, TextField, Typography, Paper, Snackbar, Alert, CircularProgress, Chip, LinearProgress,
} from '@mui/material';
import { DataGrid, type GridColDef } from '@mui/x-data-grid';
import { Add, Edit } from '@mui/icons-material';
import { mockApi } from '@/lib/api';
import type { SubTenant } from '@/lib/types';
import { PageHeader } from '@/components/common/PageHeader';

export default function SubTenantsPage() {
  const { tenantId } = useParams<{ tenantId: string }>();
  const qc = useQueryClient();

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editSub, setEditSub] = useState<SubTenant | null>(null);
  const [name, setName] = useState('');
  const [isActive, setIsActive] = useState(true);
  const [toast, setToast] = useState<{ msg: string; sev: 'success' | 'error' } | null>(null);

  const { data: subs, isLoading } = useQuery({
    queryKey: ['sub-tenants', tenantId],
    queryFn: () => mockApi.getSubTenants(tenantId),
  });

  const createMutation = useMutation({
    mutationFn: () => mockApi.createSubTenant({ name, tenantId }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['sub-tenants'] }); setDialogOpen(false); setName(''); setToast({ msg: 'Sub-tenant created', sev: 'success' }); },
    onError: () => setToast({ msg: 'Failed to create', sev: 'error' }),
  });

  const updateMutation = useMutation({
    mutationFn: () => mockApi.updateSubTenant(editSub!.id, { name, isActive }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['sub-tenants'] }); setDialogOpen(false); setEditSub(null); setToast({ msg: 'Sub-tenant updated', sev: 'success' }); },
    onError: () => setToast({ msg: 'Failed to update', sev: 'error' }),
  });

  const toggleMutation = useMutation({
    mutationFn: ({ id, active }: { id: string; active: boolean }) => mockApi.updateSubTenant(id, { isActive: active }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['sub-tenants'] }),
  });

  const openCreate = () => { setEditSub(null); setName(''); setIsActive(true); setDialogOpen(true); };
  const openEdit = (s: SubTenant) => { setEditSub(s); setName(s.name); setIsActive(s.isActive); setDialogOpen(true); };

  const columns: GridColDef<SubTenant>[] = [
    { field: 'name', headerName: 'Name', flex: 1, minWidth: 160 },
    {
      field: 'isActive', headerName: 'Status', width: 110,
      renderCell: ({ row }) => (
        <Switch
          size="small"
          checked={row.isActive}
          onChange={(e) => toggleMutation.mutate({ id: row.id, active: e.target.checked })}
        />
      ),
    },
    { field: 'sessionsToday', headerName: 'Sessions Today', width: 140, valueGetter: (_, row) => row.sessionsToday ?? 0 },
    {
      field: 'quotaUsed', headerName: 'Quota Used', width: 180,
      renderCell: ({ row }) => (
        <Box sx={{ width: '100%', py: 1 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
            <Typography variant="caption">{row.quotaUsed ?? 0}%</Typography>
          </Box>
          <LinearProgress
            variant="determinate"
            value={row.quotaUsed ?? 0}
            color={(row.quotaUsed ?? 0) >= 90 ? 'error' : (row.quotaUsed ?? 0) >= 70 ? 'warning' : 'success'}
          />
        </Box>
      ),
    },
    {
      field: 'actions', headerName: 'Actions', width: 90, sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" alignItems="center" height="100%">
          <Button size="small" variant="outlined" sx={{ minWidth: 0, px: 1 }} onClick={() => openEdit(row)}><Edit sx={{ fontSize: 16 }} /></Button>
        </Stack>
      ),
    },
  ];

  return (
    <Box>
      <PageHeader
        title="Sub-Tenants"
        subtitle="Manage sub-tenants for this parking location"
        action={<Button variant="contained" startIcon={<Add />} onClick={openCreate}>Add Sub-Tenant</Button>}
      />

      <Paper sx={{ overflow: 'hidden' }}>
        <DataGrid
          rows={subs ?? []}
          columns={columns}
          loading={isLoading}
          autoHeight
          disableRowSelectionOnClick
          pageSizeOptions={[20, 50, 100]}
          sx={{ border: 'none' }}
        />
      </Paper>

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>{editSub ? 'Edit Sub-Tenant' : 'Add Sub-Tenant'}</DialogTitle>
        <DialogContent>
          <Stack spacing={2.5} sx={{ mt: 1 }}>
            <TextField label="Name" value={name} onChange={(e) => setName(e.target.value)} required />
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <Switch checked={isActive} onChange={(e) => setIsActive(e.target.checked)} />
              <Typography variant="body2">{isActive ? 'Active' : 'Inactive'}</Typography>
            </Box>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            disabled={!name || createMutation.isPending || updateMutation.isPending}
            onClick={() => editSub ? updateMutation.mutate() : createMutation.mutate()}
          >
            {(createMutation.isPending || updateMutation.isPending) ? <CircularProgress size={20} /> : editSub ? 'Save' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar open={!!toast} autoHideDuration={3000} onClose={() => setToast(null)}>
        <Alert severity={toast?.sev} onClose={() => setToast(null)}>{toast?.msg}</Alert>
      </Snackbar>
    </Box>
  );
}
