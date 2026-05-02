'use client';

import { useState } from 'react';
import { useParams } from 'next/navigation';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Button, Chip, TextField, Select, MenuItem, FormControl,
  InputLabel, Stack, Dialog, DialogTitle, DialogContent, DialogActions,
  DialogContentText, Typography, CircularProgress, LinearProgress,
  Snackbar, Alert, InputAdornment, Paper,
} from '@mui/material';
import { DataGrid, type GridColDef } from '@mui/x-data-grid';
import {
  Add, Search, Download, Extension, Cancel, AccessTime,
} from '@mui/icons-material';
import { format, addMinutes } from 'date-fns';
import { mockApi } from '@/lib/api';
import type { ValidationSession } from '@/lib/types';
import { PageHeader } from '@/components/common/PageHeader';

const STATUS_COLORS: Record<string, 'success' | 'info' | 'error' | 'default'> = {
  ACTIVE: 'success',
  EXTENDED: 'info',
  CANCELLED: 'error',
  EXPIRED: 'default',
};

function generateCsv(sessions: ValidationSession[]): void {
  const headers = ['ID', 'License Plate', 'Zone', 'Sub-Tenant', 'Start Time', 'End Time', 'Duration (min)', 'Status'];
  const rows = sessions.map((s) => [
    s.id, s.licensePlate, s.zoneName, s.subTenantName ?? '',
    format(new Date(s.startTime), 'yyyy-MM-dd HH:mm'),
    format(new Date(s.endTime), 'yyyy-MM-dd HH:mm'),
    s.durationMinutes, s.status,
  ]);
  const csv = [headers, ...rows].map((r) => r.join(',')).join('\n');
  const blob = new Blob([csv], { type: 'text/csv' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `validations-${format(new Date(), 'yyyy-MM-dd')}.csv`;
  a.click();
  URL.revokeObjectURL(url);
}

export default function ValidationsPage() {
  const { tenantId } = useParams<{ tenantId: string }>();
  const qc = useQueryClient();

  const [statusFilter, setStatusFilter] = useState('');
  const [zoneFilter, setZoneFilter] = useState('');
  const [plateSearch, setPlateSearch] = useState('');

  const [extendId, setExtendId] = useState<string | null>(null);
  const [extendMins, setExtendMins] = useState(30);
  const [cancelId, setCancelId] = useState<string | null>(null);
  const [cancelPlate, setCancelPlate] = useState('');

  const [exportOpen, setExportOpen] = useState(false);
  const [exportProgress, setExportProgress] = useState(false);
  const [exportReady, setExportReady] = useState(false);

  const [toast, setToast] = useState<{ msg: string; sev: 'success' | 'error' } | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['validations', tenantId, statusFilter, zoneFilter, plateSearch],
    queryFn: () => mockApi.getValidations({ status: statusFilter || undefined, zoneId: zoneFilter || undefined, plate: plateSearch || undefined }),
  });

  const { data: zones } = useQuery({ queryKey: ['zones'], queryFn: mockApi.getZones });

  const extendMutation = useMutation({
    mutationFn: ({ id, mins }: { id: string; mins: number }) => mockApi.extendSession(id, mins),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['validations'] });
      setExtendId(null);
      setToast({ msg: 'Session extended successfully', sev: 'success' });
    },
    onError: () => setToast({ msg: 'Failed to extend session', sev: 'error' }),
  });

  const cancelMutation = useMutation({
    mutationFn: (id: string) => mockApi.cancelSession(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['validations'] });
      setCancelId(null);
      setToast({ msg: 'Session cancelled', sev: 'success' });
    },
    onError: () => setToast({ msg: 'Failed to cancel session', sev: 'error' }),
  });

  const sessions = data?.items ?? [];

  const extendSession = sessions.find((s) => s.id === extendId);
  const newEndTime = extendSession
    ? addMinutes(new Date(extendSession.endTime), extendMins)
    : null;

  const handleExport = async () => {
    setExportProgress(true);
    setExportReady(false);
    await new Promise((r) => setTimeout(r, 3000));
    setExportProgress(false);
    setExportReady(true);
  };

  const columns: GridColDef<ValidationSession>[] = [
    { field: 'licensePlate', headerName: 'License Plate', width: 140, renderCell: ({ value }) => <Typography fontFamily="monospace" fontWeight={600}>{value}</Typography> },
    { field: 'zoneName', headerName: 'Zone', width: 130 },
    { field: 'subTenantName', headerName: 'Sub-Tenant', width: 140, valueGetter: (_, row) => row.subTenantName ?? '—' },
    { field: 'startTime', headerName: 'Start', width: 150, valueFormatter: (v: string) => format(new Date(v), 'MMM d, h:mm a') },
    { field: 'endTime', headerName: 'End', width: 150, valueFormatter: (v: string) => format(new Date(v), 'MMM d, h:mm a') },
    { field: 'durationMinutes', headerName: 'Duration', width: 100, valueFormatter: (v: number) => `${v}m` },
    {
      field: 'status', headerName: 'Status', width: 110,
      renderCell: ({ value }) => (
        <Chip label={value} size="small" color={STATUS_COLORS[value as string] ?? 'default'} />
      ),
    },
    {
      field: 'actions', headerName: 'Actions', width: 140, sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" spacing={0.5} alignItems="center" height="100%">
          {(row.status === 'ACTIVE' || row.status === 'EXTENDED') && (
            <>
              <Button
                size="small" variant="outlined" sx={{ minWidth: 0, px: 1 }}
                onClick={() => { setExtendId(row.id); setExtendMins(30); }}
              >
                <AccessTime sx={{ fontSize: 16 }} />
              </Button>
              <Button
                size="small" variant="outlined" color="error" sx={{ minWidth: 0, px: 1 }}
                onClick={() => { setCancelId(row.id); setCancelPlate(row.licensePlate); }}
              >
                <Cancel sx={{ fontSize: 16 }} />
              </Button>
            </>
          )}
        </Stack>
      ),
    },
  ];

  return (
    <Box>
      <PageHeader
        title="Validations"
        subtitle="Manage parking validation sessions"
        action={
          <Stack direction="row" spacing={1}>
            <Button
              variant="outlined"
              startIcon={<Download />}
              onClick={() => { setExportOpen(true); setExportReady(false); setExportProgress(false); }}
            >
              Export
            </Button>
          </Stack>
        }
      />

      {/* Filters */}
      <Paper sx={{ p: 2, mb: 2 }}>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
          <TextField
            placeholder="Search plate..."
            value={plateSearch}
            onChange={(e) => setPlateSearch(e.target.value)}
            InputProps={{ startAdornment: <InputAdornment position="start"><Search sx={{ fontSize: 18 }} /></InputAdornment> }}
            sx={{ minWidth: 200 }}
          />
          <FormControl sx={{ minWidth: 140 }}>
            <InputLabel>Status</InputLabel>
            <Select value={statusFilter} label="Status" onChange={(e) => setStatusFilter(e.target.value)}>
              <MenuItem value="">All Statuses</MenuItem>
              <MenuItem value="ACTIVE">Active</MenuItem>
              <MenuItem value="EXTENDED">Extended</MenuItem>
              <MenuItem value="CANCELLED">Cancelled</MenuItem>
              <MenuItem value="EXPIRED">Expired</MenuItem>
            </Select>
          </FormControl>
          <FormControl sx={{ minWidth: 140 }}>
            <InputLabel>Zone</InputLabel>
            <Select value={zoneFilter} label="Zone" onChange={(e) => setZoneFilter(e.target.value)}>
              <MenuItem value="">All Zones</MenuItem>
              {zones?.map((z) => <MenuItem key={z.id} value={z.id}>{z.name}</MenuItem>)}
            </Select>
          </FormControl>
        </Stack>
      </Paper>

      {/* DataGrid */}
      <Paper sx={{ overflow: 'hidden' }}>
        <DataGrid
          rows={sessions}
          columns={columns}
          loading={isLoading}
          autoHeight
          disableRowSelectionOnClick
          pageSizeOptions={[20, 50, 100]}
          initialState={{ pagination: { paginationModel: { pageSize: 20 } } }}
          sx={{ border: 'none' }}
        />
      </Paper>

      {/* Extend Dialog */}
      <Dialog open={!!extendId} onClose={() => setExtendId(null)} maxWidth="xs" fullWidth>
        <DialogTitle>Extend Session</DialogTitle>
        <DialogContent>
          <TextField
            fullWidth
            type="number"
            label="Additional Minutes"
            value={extendMins}
            onChange={(e) => setExtendMins(Math.min(480, Math.max(1, parseInt(e.target.value) || 1)))}
            inputProps={{ min: 1, max: 480 }}
            sx={{ mt: 1 }}
          />
          {newEndTime && (
            <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
              New end time: <strong>{format(newEndTime, 'h:mm a, MMM d')}</strong>
            </Typography>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setExtendId(null)}>Cancel</Button>
          <Button
            variant="contained"
            disabled={extendMutation.isPending}
            onClick={() => extendId && extendMutation.mutate({ id: extendId, mins: extendMins })}
          >
            {extendMutation.isPending ? <CircularProgress size={20} /> : 'Extend'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Cancel Dialog */}
      <Dialog open={!!cancelId} onClose={() => setCancelId(null)} maxWidth="xs" fullWidth>
        <DialogTitle>Cancel Session?</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Are you sure you want to cancel the session for plate{' '}
            <strong style={{ fontFamily: 'monospace' }}>{cancelPlate}</strong>?
            This action cannot be undone.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCancelId(null)}>Keep</Button>
          <Button
            variant="contained"
            color="error"
            disabled={cancelMutation.isPending}
            onClick={() => cancelId && cancelMutation.mutate(cancelId)}
          >
            {cancelMutation.isPending ? <CircularProgress size={20} /> : 'Cancel Session'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Export Dialog */}
      <Dialog open={exportOpen} onClose={() => setExportOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Export Validations</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Export your validation sessions data.
          </Typography>
          {exportProgress && (
            <Box sx={{ mt: 2 }}>
              <Typography variant="body2" sx={{ mb: 1 }}>Generating report...</Typography>
              <LinearProgress />
            </Box>
          )}
          {exportReady && (
            <Alert
              severity="success"
              action={
                <Button
                  size="small"
                  onClick={() => { generateCsv(sessions); setExportOpen(false); }}
                >
                  Download CSV
                </Button>
              }
            >
              Report is ready!
            </Alert>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setExportOpen(false)}>Close</Button>
          {!exportReady && (
            <Button
              variant="contained"
              disabled={exportProgress}
              onClick={handleExport}
            >
              Generate
            </Button>
          )}
        </DialogActions>
      </Dialog>

      {/* Toast */}
      <Snackbar open={!!toast} autoHideDuration={toast?.sev === 'error' ? 5000 : 3000} onClose={() => setToast(null)}>
        <Alert severity={toast?.sev} onClose={() => setToast(null)}>{toast?.msg}</Alert>
      </Snackbar>
    </Box>
  );
}
