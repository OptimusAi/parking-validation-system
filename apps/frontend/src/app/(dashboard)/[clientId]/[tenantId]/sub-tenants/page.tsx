'use client';

import { useState } from 'react';
import { useParams } from 'next/navigation';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Button, Switch, Stack, Dialog, DialogTitle, DialogContent,
  DialogActions, TextField, Typography, Paper, Snackbar, Alert, CircularProgress, Chip, LinearProgress,
  FormControl, InputLabel, Select, MenuItem,
} from '@mui/material';
import { DataGrid, type GridColDef } from '@mui/x-data-grid';
import { Add, Edit } from '@mui/icons-material';
import { mockApi, listClients, getTenantsForClient } from '@/lib/api';
import type { SubTenant } from '@/lib/types';
import { PageHeader } from '@/components/common/PageHeader';
import { useTenantStore } from '@/store/tenantStore';

export default function SubTenantsPage() {
  const { tenantId: urlTenantId } = useParams<{ tenantId: string }>();
  const qc = useQueryClient();
  const role = useTenantStore((s) => s.role);
  const isAdmin = role === 'ADMIN';

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editSub, setEditSub] = useState<SubTenant | null>(null);
  const [name, setName] = useState('');
  const [isActive, setIsActive] = useState(true);
  // Admin-only cascade selectors
  const [selectedClientId, setSelectedClientId] = useState('');
  const [selectedTenantId, setSelectedTenantId] = useState('');
  const [toast, setToast] = useState<{ msg: string; sev: 'success' | 'error' } | null>(null);

  // Effective tenantId: ADMIN picks via dropdown; others use URL param
  const effectiveTenantId = isAdmin ? selectedTenantId : urlTenantId;

  const { data: subs, isLoading } = useQuery({
    queryKey: ['sub-tenants', urlTenantId],
    queryFn: () => mockApi.getSubTenants(urlTenantId),
  });

  // ADMIN: fetch clients for first dropdown
  const { data: clientsPage } = useQuery({
    queryKey: ['clients'],
    queryFn: () => listClients({ pageSize: 200 }),
    enabled: isAdmin,
  });
  const clientOptions = clientsPage?.content ?? [];

  // ADMIN: fetch tenants for selected client
  const { data: tenantOptions = [] } = useQuery({
    queryKey: ['tenants-for-client', selectedClientId],
    queryFn: () => getTenantsForClient(selectedClientId),
    enabled: isAdmin && !!selectedClientId,
  });

  const createMutation = useMutation({
    mutationFn: () => mockApi.createSubTenant({ name, tenantId: effectiveTenantId }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['sub-tenants'] });
      setDialogOpen(false);
      setName('');
      setSelectedClientId('');
      setSelectedTenantId('');
      setToast({ msg: 'Sub-tenant created', sev: 'success' });
    },
    onError: () => setToast({ msg: 'Failed to create', sev: 'error' }),
  });

  const updateMutation = useMutation({
    mutationFn: () => mockApi.updateSubTenant(editSub!.id, { name, isActive }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['sub-tenants'] });
      setDialogOpen(false);
      setEditSub(null);
      setToast({ msg: 'Sub-tenant updated', sev: 'success' });
    },
    onError: () => setToast({ msg: 'Failed to update', sev: 'error' }),
  });

  const toggleMutation = useMutation({
    mutationFn: ({ id, active }: { id: string; active: boolean }) => mockApi.updateSubTenant(id, { isActive: active }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['sub-tenants'] }),
  });

  const openCreate = () => {
    setEditSub(null); setName(''); setIsActive(true);
    setSelectedClientId(''); setSelectedTenantId('');
    setDialogOpen(true);
  };
  const openEdit = (s: SubTenant) => { setEditSub(s); setName(s.name); setIsActive(s.isActive); setDialogOpen(true); };

  const isCreateDisabled = !name || !effectiveTenantId || createMutation.isPending;

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
        <Stack direction="row" sx={{ alignItems: 'center', height: '100%' }}>
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
            {/* ADMIN: pick client then tenant */}
            {isAdmin && !editSub && (
              <>
                <FormControl fullWidth required>
                  <InputLabel id="sub-client-label">Client</InputLabel>
                  <Select
                    labelId="sub-client-label"
                    label="Client"
                    value={selectedClientId}
                    onChange={(e) => { setSelectedClientId(e.target.value); setSelectedTenantId(''); }}
                  >
                    {clientOptions.map((c) => (
                      <MenuItem key={c.id} value={c.id}>{c.name}</MenuItem>
                    ))}
                  </Select>
                </FormControl>
                <FormControl fullWidth required disabled={!selectedClientId}>
                  <InputLabel id="sub-tenant-label">Tenant</InputLabel>
                  <Select
                    labelId="sub-tenant-label"
                    label="Tenant"
                    value={selectedTenantId}
                    onChange={(e) => setSelectedTenantId(e.target.value)}
                  >
                    {tenantOptions.map((t) => (
                      <MenuItem key={t.id} value={t.id}>{t.name}</MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </>
            )}
            <TextField label="Name" value={name} onChange={(e) => setName(e.target.value)} required fullWidth />
            {editSub && (
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <Switch checked={isActive} onChange={(e) => setIsActive(e.target.checked)} />
                <Typography variant="body2">{isActive ? 'Active' : 'Inactive'}</Typography>
              </Box>
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            disabled={editSub ? (!name || updateMutation.isPending) : isCreateDisabled}
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

