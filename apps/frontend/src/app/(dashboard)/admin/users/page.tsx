'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Chip, Dialog, DialogActions, DialogContent, DialogTitle,
  MenuItem, Paper, Select, Snackbar, Alert, Button, Typography,
  InputLabel, FormControl, Switch, FormControlLabel, CircularProgress,
} from '@mui/material';
import { DataGrid, type GridColDef } from '@mui/x-data-grid';
import { Edit } from '@mui/icons-material';
import { mockApi, getTenantsForClient, listClients } from '@/lib/api';
import type { User, Role, Client, Tenant } from '@/lib/types';
import { PageHeader } from '@/components/common/PageHeader';
import { useTenantStore } from '@/store/tenantStore';

const ALL_ROLES: Role[] = ['ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN', 'SUBTENANT_USER'];

const ROLE_COLORS: Record<string, 'error' | 'warning' | 'info' | 'default'> = {
  ADMIN: 'error',
  CLIENT_ADMIN: 'warning',
  TENANT_ADMIN: 'info',
  SUBTENANT_USER: 'default',
};

export default function AdminUsersPage() {
  const router = useRouter();
  const qc = useQueryClient();
  const role = useTenantStore((s) => s.role);

  const [selected, setSelected] = useState<User | null>(null);
  const [editRole, setEditRole] = useState<Role>('TENANT_ADMIN');
  const [editClientId, setEditClientId] = useState<string>('');
  const [editTenantId, setEditTenantId] = useState<string>('');
  const [editActive, setEditActive] = useState(true);
  const [toast, setToast] = useState<{ msg: string; sev: 'success' | 'error' } | null>(null);

  // Guard: ADMIN only
  useEffect(() => {
    if (role && role !== 'ADMIN') router.replace('/no-access');
  }, [role, router]);

  const { data, isLoading } = useQuery({
    queryKey: ['admin-users'],
    queryFn: () => mockApi.getUsers(),
    enabled: role === 'ADMIN',
  });

  const { data: clientsData } = useQuery({
    queryKey: ['clients'],
    queryFn: () => listClients(),
    enabled: role === 'ADMIN',
  });

  const { data: tenantsForClient } = useQuery({
    queryKey: ['tenants-for-client', editClientId],
    queryFn: () => getTenantsForClient(editClientId),
    enabled: !!editClientId,
  });

  const roleMutation = useMutation({
    mutationFn: ({ id, newRole }: { id: string; newRole: Role }) =>
      mockApi.updateUserRole(id, newRole),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin-users'] });
      setToast({ msg: 'Role updated. User must re-login.', sev: 'success' });
    },
    onError: () => setToast({ msg: 'Failed to update role', sev: 'error' }),
  });

  const tenantMutation = useMutation({
    mutationFn: ({ id, tenantId, clientId }: { id: string; tenantId: string; clientId: string }) =>
      mockApi.assignUserTenant(id, tenantId || null, clientId || null),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin-users'] });
      setToast({ msg: 'Tenant assigned. User must re-login.', sev: 'success' });
    },
    onError: () => setToast({ msg: 'Failed to assign tenant', sev: 'error' }),
  });

  const activeMutation = useMutation({
    mutationFn: ({ id, isActive }: { id: string; isActive: boolean }) =>
      mockApi.updateUserActive(id, isActive),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin-users'] }),
    onError: () => setToast({ msg: 'Failed to update status', sev: 'error' }),
  });

  const openEdit = (user: User) => {
    setSelected(user);
    setEditRole((user.role as Role) ?? 'TENANT_ADMIN');
    setEditClientId(user.clientId ?? '');
    setEditTenantId(user.tenantId ?? '');
    setEditActive(user.isActive);
  };

  const handleSave = () => {
    if (!selected) return;
    const promises: Promise<unknown>[] = [];

    if (editRole !== selected.role) {
      promises.push(roleMutation.mutateAsync({ id: selected.id, newRole: editRole }));
    }
    if (editTenantId !== (selected.tenantId ?? '') || editClientId !== (selected.clientId ?? '')) {
      promises.push(tenantMutation.mutateAsync({ id: selected.id, tenantId: editTenantId, clientId: editClientId }));
    }
    if (editActive !== selected.isActive) {
      promises.push(activeMutation.mutateAsync({ id: selected.id, isActive: editActive }));
    }
    Promise.all(promises).then(() => setSelected(null));
  };

  const clients: Client[] = clientsData?.content ?? [];
  const tenants: Tenant[] = tenantsForClient ?? [];
  const users = data?.content ?? [];

  const columns: GridColDef<User>[] = [
    { field: 'fullName', headerName: 'Name', flex: 1, minWidth: 150,
      valueGetter: (_, row) => (row.fullName ?? row.name ?? `${row.firstName ?? ''} ${row.lastName ?? ''}`.trim()) || '—' },
    { field: 'email', headerName: 'Email', flex: 1, minWidth: 200 },
    {
      field: 'role', headerName: 'Role', width: 160,
      renderCell: ({ row }) => (
        <Chip
          label={(row.role ?? 'USER').replace(/_/g, ' ')}
          size="small"
          color={ROLE_COLORS[row.role] ?? 'default'}
          variant="outlined"
        />
      ),
    },
    {
      field: 'tenantId', headerName: 'Tenant', width: 200,
      valueGetter: (_, row) => row.tenantName ?? (row.tenantId ? row.tenantId.slice(0, 8) + '…' : '—'),
    },
    {
      field: 'isActive', headerName: 'Active', width: 80,
      renderCell: ({ row }) => (
        <Chip
          label={row.isActive ? 'Active' : 'Inactive'}
          size="small"
          color={row.isActive ? 'success' : 'default'}
        />
      ),
    },
    {
      field: 'createdAt', headerName: 'Created', width: 120,
      valueGetter: (_, row) => row.createdAt ? new Date(row.createdAt).toLocaleDateString() : '—',
    },
    {
      field: 'actions', headerName: '', width: 80, sortable: false,
      renderCell: ({ row }) => (
        <Button size="small" startIcon={<Edit sx={{ fontSize: 14 }} />} onClick={() => openEdit(row)}>
          Edit
        </Button>
      ),
    },
  ];

  return (
    <Box>
      <PageHeader title="Users" subtitle="Manage all system users, roles and tenant assignments" />

      <Paper sx={{ overflow: 'hidden' }}>
        <DataGrid
          rows={users}
          columns={columns}
          loading={isLoading}
          autoHeight
          disableRowSelectionOnClick
          pageSizeOptions={[20, 50, 100]}
          initialState={{ pagination: { paginationModel: { pageSize: 20 } } }}
          sx={{ border: 'none' }}
        />
      </Paper>

      {/* Edit dialog */}
      <Dialog open={!!selected} onClose={() => setSelected(null)} maxWidth="sm" fullWidth>
        <DialogTitle>
          Edit User
          <Typography variant="body2" color="text.secondary">
            {selected?.email}
          </Typography>
        </DialogTitle>
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2.5, pt: 2 }}>
          {/* Role */}
          <FormControl fullWidth size="small">
            <InputLabel>Role</InputLabel>
            <Select label="Role" value={editRole} onChange={(e) => {
              setEditRole(e.target.value as Role);
              setEditClientId('');
              setEditTenantId('');
            }}>
              {ALL_ROLES.map((r) => (
                <MenuItem key={r} value={r}>{r.replace(/_/g, ' ')}</MenuItem>
              ))}
            </Select>
          </FormControl>

          {/* Client (for CLIENT_ADMIN and TENANT_ADMIN) */}
          {(editRole === 'CLIENT_ADMIN' || editRole === 'TENANT_ADMIN') && (
            <FormControl fullWidth size="small">
              <InputLabel>Client</InputLabel>
              <Select label="Client" value={editClientId} onChange={(e) => {
                setEditClientId(e.target.value);
                setEditTenantId('');
              }}>
                <MenuItem value=""><em>— None —</em></MenuItem>
                {clients.map((c) => (
                  <MenuItem key={c.id} value={c.id}>{c.name}</MenuItem>
                ))}
              </Select>
            </FormControl>
          )}

          {/* Tenant (once a client is picked, or for TENANT_ADMIN) */}
          {(editRole === 'CLIENT_ADMIN' || editRole === 'TENANT_ADMIN') && editClientId && (
            <FormControl fullWidth size="small">
              <InputLabel>Tenant</InputLabel>
              <Select label="Tenant" value={editTenantId} onChange={(e) => setEditTenantId(e.target.value)}>
                <MenuItem value=""><em>— None —</em></MenuItem>
                {tenants.map((t) => (
                  <MenuItem key={t.id} value={t.id}>{t.name}</MenuItem>
                ))}
              </Select>
            </FormControl>
          )}

          {/* Active toggle */}
          <FormControlLabel
            control={<Switch checked={editActive} onChange={(e) => setEditActive(e.target.checked)} />}
            label={editActive ? 'Active' : 'Inactive'}
          />
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setSelected(null)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleSave}
            disabled={roleMutation.isPending || tenantMutation.isPending || activeMutation.isPending}
          >
            {(roleMutation.isPending || tenantMutation.isPending || activeMutation.isPending)
              ? <CircularProgress size={18} />
              : 'Save'}
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar open={!!toast} autoHideDuration={4000} onClose={() => setToast(null)}>
        <Alert severity={toast?.sev} onClose={() => setToast(null)}>{toast?.msg}</Alert>
      </Snackbar>
    </Box>
  );
}
