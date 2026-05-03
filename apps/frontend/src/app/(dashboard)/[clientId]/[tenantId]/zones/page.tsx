'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Button, Stack, Dialog, DialogTitle, DialogContent, DialogActions,
  DialogContentText, TextField, Typography, Paper, Snackbar, Alert, CircularProgress, Chip,
} from '@mui/material';
import { DataGrid, type GridColDef } from '@mui/x-data-grid';
import { Add, Edit, Delete } from '@mui/icons-material';
import { mockApi } from '@/lib/api';
import type { Zone } from '@/lib/types';
import { PageHeader } from '@/components/common/PageHeader';

const EMPTY_FORM = { zoneNumber: '', name: '', defaultDurationMinutes: 60, maxDurationMinutes: 480 };

export default function ZonesPage() {
  const qc = useQueryClient();
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editZone, setEditZone] = useState<Zone | null>(null);
  const [deleteZone, setDeleteZone] = useState<Zone | null>(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [toast, setToast] = useState<{ msg: string; sev: 'success' | 'error' } | null>(null);

  const { data: zones, isLoading } = useQuery({ queryKey: ['zones'], queryFn: mockApi.getZones });

  const createMutation = useMutation({
    mutationFn: () => mockApi.createZone(form),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['zones'] }); setDialogOpen(false); setForm(EMPTY_FORM); setToast({ msg: 'Zone created', sev: 'success' }); },
    onError: () => setToast({ msg: 'Failed to create zone', sev: 'error' }),
  });

  const updateMutation = useMutation({
    mutationFn: () => mockApi.updateZone(editZone!.id, form),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['zones'] }); setEditZone(null); setDialogOpen(false); setForm(EMPTY_FORM); setToast({ msg: 'Zone updated', sev: 'success' }); },
    onError: () => setToast({ msg: 'Failed to update zone', sev: 'error' }),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => mockApi.deleteZone(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['zones'] }); setDeleteZone(null); setToast({ msg: 'Zone deleted', sev: 'success' }); },
    onError: (e: Error) => { setDeleteZone(null); setToast({ msg: e.message || 'Cannot delete zone', sev: 'error' }); },
  });

  const openCreate = () => { setEditZone(null); setForm(EMPTY_FORM); setDialogOpen(true); };
  const openEdit = (z: Zone) => { setEditZone(z); setForm({ zoneNumber: z.zoneNumber, name: z.name, defaultDurationMinutes: z.defaultDurationMinutes, maxDurationMinutes: z.maxDurationMinutes }); setDialogOpen(true); };

  const columns: GridColDef<Zone>[] = [
    { field: 'zoneNumber', headerName: 'Zone #', width: 90, renderCell: ({ value }) => <Chip label={value} size="small" variant="outlined" /> },
    { field: 'name', headerName: 'Name', flex: 1, minWidth: 140 },
    { field: 'defaultDurationMinutes', headerName: 'Default Duration', width: 150, valueFormatter: (v: number) => `${v} min` },
    { field: 'maxDurationMinutes', headerName: 'Max Duration', width: 140, valueFormatter: (v: number) => `${v} min` },
    { field: 'activeSessions', headerName: 'Active Sessions', width: 140, valueGetter: (_, row) => row.activeSessions ?? 0 },
    {
      field: 'actions', headerName: 'Actions', width: 120, sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" spacing={0.5} sx={{ alignItems: 'center', height: '100%' }}>
          <Button size="small" variant="outlined" sx={{ minWidth: 0, px: 1 }} onClick={() => openEdit(row)}><Edit sx={{ fontSize: 16 }} /></Button>
          <Button size="small" variant="outlined" color="error" sx={{ minWidth: 0, px: 1 }} onClick={() => setDeleteZone(row)}><Delete sx={{ fontSize: 16 }} /></Button>
        </Stack>
      ),
    },
  ];

  return (
    <Box>
      <PageHeader
        title="Zones"
        subtitle="Manage parking zones and their validation settings"
        action={<Button variant="contained" startIcon={<Add />} onClick={openCreate}>Add Zone</Button>}
      />

      <Paper sx={{ overflow: 'hidden' }}>
        <DataGrid
          rows={zones ?? []}
          columns={columns}
          loading={isLoading}
          autoHeight
          disableRowSelectionOnClick
          pageSizeOptions={[20, 50, 100]}
          sx={{ border: 'none' }}
        />
      </Paper>

      {/* Create/Edit Dialog */}
      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>{editZone ? 'Edit Zone' : 'Add Zone'}</DialogTitle>
        <DialogContent>
          <Stack spacing={2.5} sx={{ mt: 1 }}>
            <TextField label="Zone Number" value={form.zoneNumber} onChange={(e) => setForm((f) => ({ ...f, zoneNumber: e.target.value.toUpperCase() }))} placeholder="e.g. A, B, ROOF" required />
            <TextField label="Zone Name" value={form.name} onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))} placeholder="e.g. Ground Floor" required />
            <TextField label="Default Duration (minutes)" type="number" value={form.defaultDurationMinutes} onChange={(e) => setForm((f) => ({ ...f, defaultDurationMinutes: parseInt(e.target.value) || 60 }))} />
            <TextField label="Max Duration (minutes)" type="number" value={form.maxDurationMinutes} onChange={(e) => setForm((f) => ({ ...f, maxDurationMinutes: parseInt(e.target.value) || 480 }))} />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            disabled={!form.zoneNumber || !form.name || createMutation.isPending || updateMutation.isPending}
            onClick={() => editZone ? updateMutation.mutate() : createMutation.mutate()}
          >
            {(createMutation.isPending || updateMutation.isPending) ? <CircularProgress size={20} /> : editZone ? 'Save' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Delete Dialog */}
      <Dialog open={!!deleteZone} onClose={() => setDeleteZone(null)} maxWidth="xs" fullWidth>
        <DialogTitle>Delete Zone?</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Delete zone <strong>{deleteZone?.zoneNumber} — {deleteZone?.name}</strong>?
            {(deleteZone?.activeSessions ?? 0) > 0 && (
              <Typography color="error" variant="body2" sx={{ mt: 1 }}>
                Warning: This zone has {deleteZone?.activeSessions} active sessions.
              </Typography>
            )}
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteZone(null)}>Cancel</Button>
          <Button variant="contained" color="error" disabled={deleteMutation.isPending} onClick={() => deleteZone && deleteMutation.mutate(deleteZone.id)}>
            {deleteMutation.isPending ? <CircularProgress size={20} /> : 'Delete'}
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar open={!!toast} autoHideDuration={toast?.sev === 'error' ? 5000 : 3000} onClose={() => setToast(null)}>
        <Alert severity={toast?.sev} onClose={() => setToast(null)}>{toast?.msg}</Alert>
      </Snackbar>
    </Box>
  );
}
