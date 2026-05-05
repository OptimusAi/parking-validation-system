'use client';

import { useParams } from 'next/navigation';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Card, CardContent, Grid, Typography, LinearProgress,
  Paper, Skeleton, TextField, Button, Stack, Divider, Alert,
} from '@mui/material';
import { DataGrid, type GridColDef } from '@mui/x-data-grid';
import * as api from '@/lib/api';
import { mockApi } from '@/lib/api';
import { PageHeader } from '@/components/common/PageHeader';
import { useAuthStore } from '@/store/authStore';
import { useTenantStore } from '@/store/tenantStore';
import { useState } from 'react';

function QuotaCard({ title, used, limit }: { title: string; used: number; limit: number }) {
  const pct = Math.round((used / limit) * 100);
  const color = pct >= 90 ? 'error' : pct >= 70 ? 'warning' : 'success';
  const textColor = pct >= 90 ? '#D32F2F' : pct >= 70 ? '#ED6C02' : '#2E7D32';

  return (
    <Card>
      <CardContent sx={{ p: 2.5 }}>
        <Typography variant="body2" color="text.secondary" sx={{ fontWeight: 500 }} gutterBottom>
          {title}
        </Typography>
        <Box sx={{ display: 'flex', alignItems: 'baseline', gap: 1, mb: 1.5 }}>
          <Typography variant="h4" sx={{ fontWeight: 700 }} color={textColor}>
            {pct}%
          </Typography>
          <Typography variant="body2" color="text.secondary">
            used
          </Typography>
        </Box>
        <LinearProgress variant="determinate" value={pct} color={color} sx={{ mb: 1 }} />
        <Typography variant="caption" color="text.secondary">
          {used.toLocaleString()} of {limit.toLocaleString()} validations
        </Typography>
      </CardContent>
    </Card>
  );
}

export default function QuotaPage() {
  const { tenantId, clientId } = useParams<{ tenantId: string; clientId: string }>();
  const role = useAuthStore((s) => s.role);
  const storeClientId = useTenantStore((s) => s.clientId);
  const resolvedClientId = clientId ?? storeClientId ?? '';
  const qc = useQueryClient();

  const { data: quota, isLoading } = useQuery({
    queryKey: ['quota', tenantId],
    queryFn: () => mockApi.getQuotaUsage(tenantId),
  });

  const { data: zoneAlloc, isLoading: allocLoading } = useQuery({
    queryKey: ['zone-allocation', tenantId],
    queryFn: () => api.getZoneAllocation(tenantId),
    enabled: !!tenantId,
  });

  // Local state for allocation inputs
  const [totalInput, setTotalInput] = useState<string>('');
  const [directInput, setDirectInput] = useState<string>('');
  const [subInput, setSubInput] = useState<string>('');
  const [splitError, setSplitError] = useState<string | null>(null);

  const totalMutation = useMutation({
    mutationFn: (v: number) => api.setZoneAllocationTotal(tenantId, resolvedClientId, v),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['zone-allocation', tenantId] }); setTotalInput(''); },
  });

  const splitMutation = useMutation({
    mutationFn: ({ d, s }: { d: number; s: number }) =>
      api.setZoneAllocationSplit(tenantId, resolvedClientId, d, s),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['zone-allocation', tenantId] }); setSplitError(null); },
    onError: (e: Error) => setSplitError(e.message),
  });

  function handleSetTotal() {
    const v = parseInt(totalInput, 10);
    if (!Number.isNaN(v) && v >= 0) totalMutation.mutate(v);
  }

  function handleSetSplit() {
    const d = parseInt(directInput, 10);
    const s = parseInt(subInput, 10);
    if (Number.isNaN(d) || Number.isNaN(s) || d < 0 || s < 0) return;
    const total = zoneAlloc?.totalZones ?? 0;
    if (d + s > total) { setSplitError(`Sum (${d + s}) exceeds total (${total})`); return; }
    setSplitError(null);
    splitMutation.mutate({ d, s });
  }

  const canSetTotal = role === 'ADMIN' || role === 'CLIENT_ADMIN';
  const canSetSplit = role === 'ADMIN' || role === 'CLIENT_ADMIN' || role === 'TENANT_ADMIN';

  const zoneColumns: GridColDef[] = [
    { field: 'zoneName', headerName: 'Zone', flex: 1, minWidth: 140 },
    { field: 'countToday', headerName: 'Count Today', width: 130 },
    {
      field: 'pct', headerName: '% of Daily', width: 160,
      renderCell: ({ row }) => {
        const pct = quota ? Math.round((row.countToday / quota.daily.used) * 100) : 0;
        return (
          <Box sx={{ width: '100%', py: 1 }}>
            <Typography variant="caption">{pct}%</Typography>
            <LinearProgress variant="determinate" value={pct} sx={{ mt: 0.5 }} />
          </Box>
        );
      },
    },
  ];

  const subColumns: GridColDef[] = [
    { field: 'name', headerName: 'Sub-Tenant', flex: 1, minWidth: 160 },
    { field: 'count', headerName: 'Validations', width: 130 },
    {
      field: 'quotaUsed', headerName: 'Quota Used', width: 200,
      renderCell: ({ value }) => (
        <Box sx={{ width: '100%', py: 1 }}>
          <Typography variant="caption">{value}%</Typography>
          <LinearProgress
            variant="determinate"
            value={value}
            color={value >= 90 ? 'error' : value >= 70 ? 'warning' : 'success'}
            sx={{ mt: 0.5 }}
          />
        </Box>
      ),
    },
  ];

  return (
    <Box>
      <PageHeader title="Quota Usage" subtitle="Monitor validation quota consumption across periods" />

      {/* Quota Cards */}
      <Grid container spacing={2.5} sx={{ mb: 3 }}>
        {isLoading ? (
          [0, 1, 2].map((i) => (
            <Grid size={{ xs: 12, sm: 4 }} key={i}>
              <Skeleton variant="rectangular" height={130} sx={{ borderRadius: 2 }} />
            </Grid>
          ))
        ) : (
          <>
            <Grid size={{ xs: 12, sm: 4 }}>
              <QuotaCard title="Daily Quota" used={quota?.daily?.used ?? 0} limit={quota?.daily?.limit ?? 1} />
            </Grid>
            <Grid size={{ xs: 12, sm: 4 }}>
              <QuotaCard title="Weekly Quota" used={quota?.weekly?.used ?? 0} limit={quota?.weekly?.limit ?? 1} />
            </Grid>
            <Grid size={{ xs: 12, sm: 4 }}>
              <QuotaCard title="Monthly Quota" used={quota?.monthly?.used ?? 0} limit={quota?.monthly?.limit ?? 1} />
            </Grid>
          </>
        )}
      </Grid>

      {/* Zone Allocation */}
      <Paper sx={{ p: 2.5, mb: 3 }}>
        <Typography variant="h6" sx={{ fontWeight: 600 }} gutterBottom>Zone Allocation</Typography>
        {allocLoading ? (
          <Skeleton variant="rectangular" height={80} sx={{ borderRadius: 1 }} />
        ) : (
          <Stack spacing={2}>
            {/* Summary row */}
            <Stack direction="row" spacing={4} flexWrap="wrap">
              <Box>
                <Typography variant="caption" color="text.secondary">Total Budget</Typography>
                <Typography variant="h5" sx={{ fontWeight: 700 }}>{zoneAlloc?.totalZones ?? 0}</Typography>
              </Box>
              <Box>
                <Typography variant="caption" color="text.secondary">Direct (limit)</Typography>
                <Typography variant="h5" sx={{ fontWeight: 700 }}>{zoneAlloc?.tenantDirect ?? 0}</Typography>
              </Box>
              <Box>
                <Typography variant="caption" color="text.secondary">Direct (used)</Typography>
                <Typography variant="h5" sx={{ fontWeight: 700 }}>{zoneAlloc?.usedDirect ?? 0}</Typography>
              </Box>
              <Box>
                <Typography variant="caption" color="text.secondary">Sub-Tenant Budget</Typography>
                <Typography variant="h5" sx={{ fontWeight: 700 }}>{zoneAlloc?.subTenant ?? 0}</Typography>
              </Box>
            </Stack>

            {/* Admin / CLIENT_ADMIN: set total */}
            {canSetTotal && (
              <>
                <Divider />
                <Typography variant="body2" sx={{ fontWeight: 600 }}>Set Total Zone Budget</Typography>
                <Stack direction="row" spacing={1} alignItems="center">
                  <TextField
                    size="small" label="Total zones" type="number"
                    inputProps={{ min: 0 }}
                    value={totalInput}
                    onChange={(e) => setTotalInput(e.target.value)}
                    sx={{ width: 160 }}
                  />
                  <Button
                    variant="contained" size="small"
                    onClick={handleSetTotal}
                    disabled={totalMutation.isPending || totalInput === ''}
                  >
                    Save
                  </Button>
                </Stack>
              </>
            )}

            {/* TENANT_ADMIN: adjust split */}
            {canSetSplit && (
              <>
                <Divider />
                <Typography variant="body2" sx={{ fontWeight: 600 }}>Adjust Split</Typography>
                {splitError && <Alert severity="error" sx={{ py: 0 }}>{splitError}</Alert>}
                <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap">
                  <TextField
                    size="small" label="Direct zones" type="number"
                    inputProps={{ min: 0 }}
                    value={directInput}
                    onChange={(e) => setDirectInput(e.target.value)}
                    placeholder={String(zoneAlloc?.tenantDirect ?? 0)}
                    sx={{ width: 160 }}
                  />
                  <TextField
                    size="small" label="Sub-tenant zones" type="number"
                    inputProps={{ min: 0 }}
                    value={subInput}
                    onChange={(e) => setSubInput(e.target.value)}
                    placeholder={String(zoneAlloc?.subTenant ?? 0)}
                    sx={{ width: 160 }}
                  />
                  <Button
                    variant="contained" size="small"
                    onClick={handleSetSplit}
                    disabled={splitMutation.isPending || (directInput === '' && subInput === '')}
                  >
                    Save Split
                  </Button>
                </Stack>
                <Typography variant="caption" color="text.secondary">
                  Sum of Direct + Sub-Tenant must not exceed Total Budget ({zoneAlloc?.totalZones ?? 0})
                </Typography>
              </>
            )}
          </Stack>
        )}
      </Paper>

      {/* Zone Breakdown */}
      <Grid container spacing={2.5}>
        <Grid size={{ xs: 12, md: 6 }}>
          <Paper sx={{ p: 2.5 }}>
            <Typography variant="h6" sx={{ fontWeight: 600 }} gutterBottom>Zone Breakdown</Typography>
            <DataGrid
              rows={(quota?.byZone ?? []).map((z) => ({ id: z.zoneId, ...z }))}
              columns={zoneColumns}
              autoHeight
              disableRowSelectionOnClick
              hideFooter
              sx={{ border: 'none' }}
            />
          </Paper>
        </Grid>

        <Grid size={{ xs: 12, md: 6 }}>
          <Paper sx={{ p: 2.5 }}>
            <Typography variant="h6" sx={{ fontWeight: 600 }} gutterBottom>Sub-Tenant Breakdown</Typography>
            <DataGrid
              rows={(quota?.bySubTenant ?? []).map((s) => ({ id: s.subTenantId, ...s }))}
              columns={subColumns}
              autoHeight
              disableRowSelectionOnClick
              hideFooter
              sx={{ border: 'none' }}
            />
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
}
