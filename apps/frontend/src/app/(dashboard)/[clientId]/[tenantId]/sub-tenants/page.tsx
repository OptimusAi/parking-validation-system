'use client';

import { useState, useMemo } from 'react';
import { useParams } from 'next/navigation';
import { useQuery, useQueries, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Button, Switch, Stack, Dialog, DialogTitle, DialogContent,
  DialogActions, TextField, Typography, Paper, Snackbar, Alert, CircularProgress, Chip, LinearProgress,
  FormControl, InputLabel, Select, MenuItem, Checkbox, ListItemText, OutlinedInput,
} from '@mui/material';
import { DataGrid, type GridColDef } from '@mui/x-data-grid';
import { Add, Edit, ZoomIn } from '@mui/icons-material';
import { mockApi, listClients, getTenantsForClient, getZones, getSubTenantZones, setSubTenantZones } from '@/lib/api';
import type { SubTenant, Zone } from '@/lib/types';
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

  // Zone-assignment dialog state
  const [zoneDialogSub, setZoneDialogSub] = useState<SubTenant | null>(null);
  const [pendingZoneIds, setPendingZoneIds] = useState<string[]>([]);

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

  // All zones for this tenant — loaded once, used in column + dialog
  const { data: tenantZones = [] } = useQuery<Zone[]>({
    queryKey: ['zones', urlTenantId],
    queryFn: () => getZones(),
    enabled: !!urlTenantId,
  });

  // Lookup map: zoneId → zone
  const zoneMap = useMemo(
    () => new Map(tenantZones.map((z) => [z.id, z])),
    [tenantZones],
  );

  // Fetch assignments for every sub-tenant in one shot (parallel)
  const subIds = (subs ?? []).map((s) => s.id);
  const assignmentQueries = useQueries({
    queries: subIds.map((sid) => ({
      queryKey: ['sub-tenant-zones', sid] as const,
      queryFn: () => getSubTenantZones(sid),
      enabled: subIds.length > 0,
      staleTime: 30_000,
    })),
  });

  // subZoneMap: subTenantId → Zone[]
  const subZoneMap = useMemo(() => {
    const m = new Map<string, Zone[]>();
    subIds.forEach((sid, i) => {
      m.set(sid, assignmentQueries[i]?.data ?? []);
    });
    return m;
  }, [subIds, assignmentQueries]);

  // Currently assigned zones for the selected sub-tenant (dialog)
  const dialogAssignedZones: Zone[] = zoneDialogSub ? (subZoneMap.get(zoneDialogSub.id) ?? []) : [];

  const saveZonesMutation = useMutation({
    mutationFn: () => setSubTenantZones(zoneDialogSub!.id, pendingZoneIds),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['sub-tenant-zones', zoneDialogSub?.id] });
      qc.invalidateQueries({ queryKey: ['sub-tenant-zones'] });
      setZoneDialogSub(null);
      setToast({ msg: 'Zone assignments saved', sev: 'success' });
    },
    onError: () => setToast({ msg: 'Failed to save zone assignments', sev: 'error' }),
  });

  function openZoneDialog(sub: SubTenant) {
    // Seed selection from already-fetched data
    setPendingZoneIds((subZoneMap.get(sub.id) ?? []).map((z) => z.id));
    setZoneDialogSub(sub);
  }

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
    { field: 'name', headerName: 'Name', width: 180, minWidth: 140 },
    {
      field: 'zones', headerName: 'Assigned Zones', flex: 1, minWidth: 200, sortable: false,
      renderCell: ({ row }) => {
        const zones = subZoneMap.get(row.id) ?? [];
        if (zones.length === 0) return <Typography variant="caption" color="text.disabled">None</Typography>;
        return (
          <Stack direction="row" spacing={0.5} flexWrap="wrap" sx={{ py: 0.5 }}>
            {zones.map((z) => (
              <Chip key={z.id} label={`${z.zoneNumber} – ${z.name}`} size="small" variant="outlined" />
            ))}
          </Stack>
        );
      },
    },
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
      field: 'actions', headerName: 'Actions', width: 180, sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" spacing={0.5} sx={{ alignItems: 'center', height: '100%' }}>
          <Button size="small" variant="outlined" sx={{ minWidth: 0, px: 1 }} onClick={() => openEdit(row)}><Edit sx={{ fontSize: 16 }} /></Button>
          <Button size="small" variant="outlined" color="secondary" sx={{ fontSize: 11, px: 1 }} onClick={() => openZoneDialog(row)}>Zones</Button>
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

      {/* Zone Assignment Dialog */}
      <Dialog
        open={!!zoneDialogSub}
        onClose={() => setZoneDialogSub(null)}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>
          Manage Zones — <strong>{zoneDialogSub?.name}</strong>
        </DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Select which zones this sub-tenant can use. Unselected zones remain tenant-direct.
          </Typography>
          {tenantZones.length === 0 ? (
            <Typography variant="body2" color="text.secondary">No zones found for this tenant.</Typography>
          ) : (
            <Stack spacing={1}>
              {tenantZones.map((zone) => {
                const checked = pendingZoneIds.includes(zone.id);
                return (
                  <Box
                    key={zone.id}
                    onClick={() =>
                      setPendingZoneIds((prev) =>
                        checked ? prev.filter((id) => id !== zone.id) : [...prev, zone.id]
                      )
                    }
                    sx={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: 1.5,
                      p: 1.5,
                      border: '1px solid',
                      borderColor: checked ? 'primary.main' : 'divider',
                      borderRadius: 1,
                      cursor: 'pointer',
                      bgcolor: checked ? 'primary.50' : 'transparent',
                      '&:hover': { bgcolor: 'action.hover' },
                    }}
                  >
                    <Checkbox checked={checked} size="small" sx={{ p: 0 }} />
                    <Box>
                      <Typography variant="body2" sx={{ fontWeight: 600 }}>
                        {zone.zoneNumber} — {zone.name}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        Default {zone.defaultDurationMinutes} min · Max {zone.maxDurationMinutes} min
                      </Typography>
                    </Box>
                  </Box>
                );
              })}
            </Stack>
          )}
        </DialogContent>
        <DialogActions>
          <Typography variant="caption" color="text.secondary" sx={{ flex: 1, pl: 1 }}>
            {pendingZoneIds.length} zone(s) selected
          </Typography>
          <Button onClick={() => setZoneDialogSub(null)}>Cancel</Button>
          <Button
            variant="contained"
            disabled={saveZonesMutation.isPending}
            onClick={() => saveZonesMutation.mutate()}
          >
            {saveZonesMutation.isPending ? <CircularProgress size={20} /> : 'Save'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}

