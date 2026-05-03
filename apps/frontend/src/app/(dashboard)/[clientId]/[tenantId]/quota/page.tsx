'use client';

import { useParams } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import {
  Box, Card, CardContent, Grid, Typography, LinearProgress,
  Paper, Skeleton,
} from '@mui/material';
import { DataGrid, type GridColDef } from '@mui/x-data-grid';
import { mockApi } from '@/lib/api';
import { PageHeader } from '@/components/common/PageHeader';

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
  const { tenantId } = useParams<{ tenantId: string }>();

  const { data: quota, isLoading } = useQuery({
    queryKey: ['quota', tenantId],
    queryFn: () => mockApi.getQuotaUsage(tenantId),
  });

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
              <QuotaCard title="Daily Quota" used={quota?.daily.used ?? 0} limit={quota?.daily.limit ?? 1} />
            </Grid>
            <Grid size={{ xs: 12, sm: 4 }}>
              <QuotaCard title="Weekly Quota" used={quota?.weekly.used ?? 0} limit={quota?.weekly.limit ?? 1} />
            </Grid>
            <Grid size={{ xs: 12, sm: 4 }}>
              <QuotaCard title="Monthly Quota" used={quota?.monthly.used ?? 0} limit={quota?.monthly.limit ?? 1} />
            </Grid>
          </>
        )}
      </Grid>

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
