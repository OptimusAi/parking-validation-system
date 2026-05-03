'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Select, MenuItem, Switch, Alert, Snackbar, Paper, Typography,
} from '@mui/material';
import { DataGrid, type GridColDef } from '@mui/x-data-grid';
import { useState } from 'react';
import { mockApi } from '@/lib/api';
import type { User, Role } from '@/lib/types';
import { useTenantStore } from '@/store/tenantStore';
import { PageHeader } from '@/components/common/PageHeader';

const ALL_ROLES: Role[] = ['ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN', 'SUBTENANT_USER'];

export default function UsersPage() {
  const qc = useQueryClient();
  const role = useTenantStore((s) => s.role);
  const [toast, setToast] = useState<{ msg: string; sev: 'success' | 'error' } | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['users'],
    queryFn: () => mockApi.getUsers(),
    enabled: role === 'ADMIN' || role === 'CLIENT_ADMIN',
  });

  const roleMutation = useMutation({
    mutationFn: ({ id, newRole }: { id: string; newRole: Role }) => mockApi.updateUserRole(id, newRole),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['users'] }); setToast({ msg: 'Role updated', sev: 'success' }); },
    onError: () => setToast({ msg: 'Failed to update role', sev: 'error' }),
  });

  const activeMutation = useMutation({
    mutationFn: ({ id, isActive }: { id: string; isActive: boolean }) => mockApi.updateUserActive(id, isActive),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['users'] }),
  });

  if (role !== 'ADMIN' && role !== 'CLIENT_ADMIN') {
    return (
      <Box>
        <PageHeader title="Users" />
        <Alert severity="error">You do not have permission to manage users.</Alert>
      </Box>
    );
  }

  const users = data?.content ?? [];

  const allowedRoles = (currentUserRole: Role): Role[] => {
    if (currentUserRole === 'ADMIN') return ALL_ROLES;
    // CLIENT_ADMIN cannot assign ADMIN or CLIENT_ADMIN
    return ALL_ROLES.filter((r) => r !== 'ADMIN' && r !== 'CLIENT_ADMIN');
  };

  const columns: GridColDef<User>[] = [
    { field: 'name', headerName: 'Name', flex: 1, minWidth: 140 },
    { field: 'email', headerName: 'Email', flex: 1, minWidth: 200 },
    {
      field: 'role', headerName: 'Role', width: 200,
      renderCell: ({ row }) => (
        <Select
          value={row.role}
          size="small"
          onChange={(e) => roleMutation.mutate({ id: row.id, newRole: e.target.value as Role })}
          sx={{ fontSize: '0.8rem', minWidth: 160 }}
        >
          {ALL_ROLES.map((r) => (
            <MenuItem
              key={r}
              value={r}
              disabled={!allowedRoles(role!).includes(r)}
            >
              {r.replace(/_/g, ' ')}
            </MenuItem>
          ))}
        </Select>
      ),
    },
    { field: 'tenantName', headerName: 'Tenant', width: 160, valueGetter: (_, row) => row.tenantName ?? '—' },
    {
      field: 'isActive', headerName: 'Active', width: 90,
      renderCell: ({ row }) => (
        <Switch
          size="small"
          checked={row.isActive}
          onChange={(e) => activeMutation.mutate({ id: row.id, isActive: e.target.checked })}
        />
      ),
    },
  ];

  return (
    <Box>
      <PageHeader
        title="Users"
        subtitle="Manage user accounts and roles"
      />

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

      <Snackbar open={!!toast} autoHideDuration={3000} onClose={() => setToast(null)}>
        <Alert severity={toast?.sev} onClose={() => setToast(null)}>{toast?.msg}</Alert>
      </Snackbar>
    </Box>
  );
}
