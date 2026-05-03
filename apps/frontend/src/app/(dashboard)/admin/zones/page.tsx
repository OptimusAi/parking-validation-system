'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Button, Chip, Stack, Dialog, DialogTitle, DialogContent, DialogActions,
  DialogContentText, TextField, Typography, Paper, Snackbar, Alert, CircularProgress,
  FormControl, InputLabel, Select, MenuItem,
} from '@mui/material';
import { DataGrid, type GridColDef } from '@mui/x-data-grid';
import { Add, Edit, Delete } from '@mui/icons-material';
import {
  listClients, getTenantsForClient, getZonesAdmin, createZoneAdmin,
  updateZone, deleteZone,
} from '@/lib/api';
import type { Zone, Client, Tenant } from '@/lib/types';
import { PageHeader } from '@/components/common/PageHeader';
import { useTenantStore } from '@/store/tenantStore';

const EMPTY_FORM = { zoneNumber: '', name: '', defaultDurationMinutes: 60, maxDurationMinutes: 480 };

export default function AdminZonesPage() {
  const router = useRouter();
  const qc = useQueryClient();
  const role = useTenantStore((s) => s.role);

  // Filters
  const [selectedClientId, setSelectedClientId] = useState('');
  const [selectedTenantId, setSelectedTenantId] = useState('');

  // Dialog state
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editZone, setEditZone] = useState<Zone | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<Zone | null>(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [toast, setToast] = useState<{ msg: string; sev: 'success' | 'error' } | null>(null);

  // Guard: ADMIN only
  useEffect(() => {
    if (role && role !== 'ADMIN') router.replace('/no-access');
  }, [role, router]);

  // Clients list
  const { data: clientsPage } = useQuery({
    queryKey: ['clients'],
    queryFn: () => listClients({ pageSize: 200 }),
    enabled: role === 'ADMIN',
  });
  const clients: Client[] = clientsPage?.content ?? [];

  // Tenants for selected client
  const { data: tenants = [] } = useQuery<Tenant[]>({
    queryKey: ['tenants-for-client', selectedClientId],
    queryFn: () => getTenantsForClient(selectedClientId),
    enabled: !!selectedClientId,
  });

  // Zones for selected tenant
  const { data: zones = [], isLoading: zonesLoading } = useQuery<Zone[]>({
    queryKey: ['admin-zones', selectedTenantId],
    queryFn: () => getZonesAdmin(selectedTenantId),
    enabled: !!selectedTenantId,
  });

  const createMutation = useMutation({
    mutationFn: () => createZoneAdmin(selectedTenantId, selectedClientId, form),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin-zones', selectedTenantId] });
      setDialogOpen(false);
      setForm(EMPTY_FORM);
      setToast({ msg: 'Zone created successfully', sev: 'success' });
    },
    onError: () => setToast({ msg: 'Failed to create zone', sev: 'error' }),
  });

  const updateMutation = useMutation({
    mutationFn: () => updateZone(editZone!.id, form),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin-zones', selectedTenantId] });
      setEditZone(null);
      setDialogOpen(false);
      setForm(EMPTY_FORM);
      setToast({ msg: 'Zone updated', sev: 'success' });
    },
    onError: () => setToast({ msg: 'Failed to update zone', sev: 'error' }),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteZone(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin-zones', selectedTenantId] });
      setDeleteTarget(null);
      setToast({ msg: 'Zone deleted', sev: 'success' });
    },
    onError: (e: Error) => {
      setDeleteTarget(null);
      setToast({ msg: e.message || 'Cannot delete zone (may have active sessions)', sev: 'error' });
    },
  });

  const openCreate = () => {
    setEditZone(null);
    setForm(EMPTY_FORM);
    setDialogOpen(true);
  };

  const openEdit = (z: Zone) => {
    setEditZone(z);
    setForm({
      zoneNumber: z.zoneNumber,
      name: z.name,
      defaultDurationMinutes: z.defaultDurationMinutes,
      maxDurationMinutes: z.maxDurationMinutes,
    });
    setDialogOpen(true);
  };

  const handleSave = () => {
    if (editZone) updateMutation.mutate();
    else createMutation.mutate();
  };

  const isSaving = createMutation.isPending || updateMutation.isPending;

  const columns: GridColDef<Zone>[] = [
    {
      field: 'zoneNumber', headerName: 'Zone #', width: 90,
      renderCell: ({ value }) => <Chip label={value} size="small" variant="outlined" />,
    },
    { field: 'name', headerName: 'Name', flex: 1, minWidth: 140 },
    {
      field: 'defaultDurationMinutes', headerName: 'Default Duration', width: 160,
      valueFormatter: (v: number) => `${v} min`,
    },
    {
      field: 'maxDurationMinutes', headerName: 'Max Duration', width: 140,
      valueFormatter: (v: number) => `${v} min`,
    },
    {
      field: 'activeSessions', headerName: 'Active Sessions', width: 140,
      valueGetter: (_, row) => row.activeSessions ?? 0,
    },
    {
      field: 'actions', headerName: 'Actions', width: 130, sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" spacing={0.5} sx={{ alignItems: 'center', height: '100%' }}>
          <Button
            size="small" variant="outlined"
            sx={{ minWidth: 0, px: 1 }}
            onClick={() => openEdit(row)}
          >
            <Edit sx={{ fontSize: 16 }} />
          </Button>
          <Button
            size="small" variant="outlined" color="error"
            sx={{ minWidth: 0, px: 1 }}
            onClick={() => setDeleteTarget(row)}
          >
            <Delete sx={{ fontSize: 16 }} />
          </Button>
        </Stack>
      ),
    },
  ];

  const canCreate = !!selectedTenantId && !!selectedClientId;

  return (
    <Box>
      <PageHeader
        title="Zones"
        subtitle="Assign and manage parking zones for any tenant"
        action={
          <Button
            variant="contained"
            startIcon={<Add />}
            onClick={openCreate}
            disabled={!canCreate}
            title={!canCreate ? 'Select a client and tenant first' : undefined}
          >
            Add Zone
          </Button>
        }
      />

      {/* Client → Tenant filter */}
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ mb: 3 }}>
        <FormControl size="small" sx={{ minWidth: 220 }}>
          <InputLabel>Client</InputLabel>
          <Select
            label="Client"
            value={selectedClientId}
            onChange={(e) => {
              setSelectedClientId(e.target.value);
              setSelectedTenantId('');
            }}
          >
            <MenuItem value=""><em>— Select Client —</em></MenuItem>
            {clients.map((c) => (
              <MenuItem key={c.id} value={c.id}>{c.name}</MenuItem>
            ))}
          </Select>
        </FormControl>

        <FormControl size="small" sx={{ minWidth: 220 }} disabled={!selectedClientId}>
          <InputLabel>Tenant</InputLabel>
          <Select
            label="Tenant"
            value={selectedTenantId}
            onChange={(e) => setSelectedTenantId(e.target.value)}
          >
            <MenuItem value=""><em>— Select Tenant —</em></MenuItem>
            {tenants.map((t) => (
              <MenuItem key={t.id} value={t.id}>{t.name}</MenuItem>
            ))}
          </Select>
        </FormControl>

        {selectedTenantId && (
          <Typography variant="body2" color="text.secondary" sx={{ alignSelf: 'center' }}>
            {zones.length} zone{zones.length !== 1 ? 's' : ''}
          </Typography>
        )}
      </Stack>

      {/* Zone list */}
      <Paper sx={{ overflow: 'hidden' }}>
        {!selectedTenantId ? (
          <Box sx={{ p: 6, textAlign: 'center' }}>
            <Typography color="text.secondary">
              Select a client and tenant above to view and manage their zones.
            </Typography>
          </Box>
        ) : (
          <DataGrid
            rows={zones}
            columns={columns}
            loading={zonesLoading}
            autoHeight
            disableRowSelectionOnClick
            pageSizeOptions={[20, 50, 100]}
            initialState={{ pagination: { paginationModel: { pageSize: 20 } } }}
            sx={{ border: 'none' }}
          />
        )}
      </Paper>

      {/* Create / Edit dialog */}
      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>
          {editZone ? 'Edit Zone' : 'Add Zone'}
          {!editZone && selectedTenantId && (
            <Typography variant="body2" color="text.secondary">
              {tenants.find((t) => t.id === selectedTenantId)?.name}
            </Typography>
          )}
        </DialogTitle>
        <DialogContent>
          <Stack spacing={2.5} sx={{ mt: 1 }}>
            <TextField
              label="Zone Number"
              value={form.zoneNumber}
              onChange={(e) => setForm((f) => ({ ...f, zoneNumber: e.target.value.toUpperCase() }))}
              placeholder="e.g. A, B, ROOF"
              required
              fullWidth
            />
            <TextField
              label="Zone Name"
              value={form.name}
              onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
              placeholder="e.g. Ground Floor"
              required
              fullWidth
            />
            <TextField
              label="Default Duration (minutes)"
              type="number"
              value={form.defaultDurationMinutes}
              onChange={(e) => setForm((f) => ({ ...f, defaultDurationMinutes: parseInt(e.target.value) || 60 }))}
              fullWidth
              inputProps={{ min: 1 }}
            />
            <TextField
              label="Max Duration (minutes)"
              type="number"
              value={form.maxDurationMinutes}
              onChange={(e) => setForm((f) => ({ ...f, maxDurationMinutes: parseInt(e.target.value) || 480 }))}
              fullWidth
              inputProps={{ min: 1 }}
            />
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            disabled={!form.zoneNumber || !form.name || isSaving}
            onClick={handleSave}
          >
            {isSaving ? <CircularProgress size={18} /> : editZone ? 'Save' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Delete confirmation */}
      <Dialog open={!!deleteTarget} onClose={() => setDeleteTarget(null)} maxWidth="xs" fullWidth>
        <DialogTitle>Delete Zone</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Delete <strong>{deleteTarget?.name}</strong> (Zone {deleteTarget?.zoneNumber})? This cannot be undone.
            Zones with active sessions cannot be deleted.
          </DialogContentText>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setDeleteTarget(null)}>Cancel</Button>
          <Button
            variant="contained"
            color="error"
            disabled={deleteMutation.isPending}
            onClick={() => deleteTarget && deleteMutation.mutate(deleteTarget.id)}
          >
            {deleteMutation.isPending ? <CircularProgress size={18} /> : 'Delete'}
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar open={!!toast} autoHideDuration={4000} onClose={() => setToast(null)}>
        <Alert severity={toast?.sev} onClose={() => setToast(null)}>{toast?.msg}</Alert>
      </Snackbar>
    </Box>
  );
}
