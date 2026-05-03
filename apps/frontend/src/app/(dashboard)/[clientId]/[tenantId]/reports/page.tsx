'use client';

import { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Button, Chip, Stack, Dialog, DialogTitle, DialogContent,
  DialogActions, Select, MenuItem, FormControl, InputLabel,
  Typography, Paper, Snackbar, Alert, CircularProgress,
} from '@mui/material';
import { DataGrid, type GridColDef } from '@mui/x-data-grid';
import { Add, Download } from '@mui/icons-material';
import { format } from 'date-fns';
import { mockApi } from '@/lib/api';
import type { ReportJob } from '@/lib/types';
import { PageHeader } from '@/components/common/PageHeader';

const STATUS_CHIP: Record<string, { color: 'warning' | 'info' | 'success' | 'error'; label: string }> = {
  QUEUED: { color: 'warning', label: 'Queued' },
  PROCESSING: { color: 'info', label: 'Processing' },
  COMPLETED: { color: 'success', label: 'Completed' },
  FAILED: { color: 'error', label: 'Failed' },
};

function downloadCsv(fileUrl: string) {
  const a = document.createElement('a');
  a.href = fileUrl;
  a.download = `report-${format(new Date(), 'yyyy-MM-dd')}.csv`;
  a.click();
}

export default function ReportsPage() {
  const qc = useQueryClient();
  const [dialogOpen, setDialogOpen] = useState(false);
  const [pollingJobId, setPollingJobId] = useState<string | null>(null);
  const [toast, setToast] = useState<{ msg: string; sev: 'success' | 'error' | 'info' } | null>(null);
  const [form, setForm] = useState({ type: 'VALIDATION_SESSIONS', format: 'CSV', dateFrom: '', dateTo: '' });

  const { data, isLoading } = useQuery({
    queryKey: ['reports'],
    queryFn: () => mockApi.getReports(),
    refetchInterval: pollingJobId ? 2000 : false,
  });

  const queueMutation = useMutation({
    mutationFn: () => mockApi.queueReport(form),
    onSuccess: async ({ jobId }) => {
      qc.invalidateQueries({ queryKey: ['reports'] });
      setDialogOpen(false);
      setToast({ msg: 'Report queued', sev: 'info' });
      // Auto-progress simulation
      setTimeout(() => qc.invalidateQueries({ queryKey: ['reports'] }), 1000);
      setPollingJobId(jobId);
      setTimeout(() => {
        mockApi.getReportJob(jobId).then(() => {
          qc.invalidateQueries({ queryKey: ['reports'] });
          setPollingJobId(null);
          setToast({ msg: 'Report completed! Ready to download.', sev: 'success' });
        });
      }, 3000);
    },
  });

  const reports = data?.items ?? [];

  const columns: GridColDef<ReportJob>[] = [
    { field: 'type', headerName: 'Type', width: 200, valueFormatter: (v: string) => v.replace(/_/g, ' ') },
    { field: 'format', headerName: 'Format', width: 90 },
    {
      field: 'status', headerName: 'Status', width: 130,
      renderCell: ({ value }) => {
        const cfg = STATUS_CHIP[value as string] ?? { color: 'default', label: value };
        return (
          <Chip
            label={cfg.label}
            size="small"
            color={cfg.color}
            sx={value === 'PROCESSING' ? { animation: 'pulse 1.5s ease-in-out infinite' } : {}}
          />
        );
      },
    },
    { field: 'requestedAt', headerName: 'Requested', width: 160, valueFormatter: (v: string) => format(new Date(v), 'MMM d, h:mm a') },
    { field: 'completedAt', headerName: 'Completed', width: 160, valueFormatter: (v?: string) => v ? format(new Date(v), 'MMM d, h:mm a') : '—' },
    {
      field: 'actions', headerName: 'Download', width: 120, sortable: false,
      renderCell: ({ row }) => (
        <Stack alignItems="center" height="100%" justifyContent="center">
          {row.status === 'COMPLETED' && (
            <Button
              size="small"
              variant="contained"
              startIcon={<Download sx={{ fontSize: 16 }} />}
              onClick={() => row.fileUrl && downloadCsv(row.fileUrl)}
            >
              CSV
            </Button>
          )}
        </Stack>
      ),
    },
  ];

  return (
    <Box>
      <style>{`@keyframes pulse { 0%,100% { opacity: 1 } 50% { opacity: 0.5 } }`}</style>
      <PageHeader
        title="Reports"
        subtitle="Generate and download validation reports"
        action={<Button variant="contained" startIcon={<Add />} onClick={() => setDialogOpen(true)}>Generate Report</Button>}
      />

      <Paper sx={{ overflow: 'hidden' }}>
        <DataGrid
          rows={reports}
          columns={columns}
          loading={isLoading}
          autoHeight
          disableRowSelectionOnClick
          pageSizeOptions={[20, 50, 100]}
          sx={{ border: 'none' }}
        />
      </Paper>

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Generate Report</DialogTitle>
        <DialogContent>
          <Stack spacing={2.5} sx={{ mt: 1 }}>
            <FormControl fullWidth>
              <InputLabel>Report Type</InputLabel>
              <Select value={form.type} label="Report Type" onChange={(e) => setForm((f) => ({ ...f, type: e.target.value }))}>
                <MenuItem value="VALIDATION_SESSIONS">Validation Sessions</MenuItem>
                <MenuItem value="QUOTA_USAGE">Quota Usage</MenuItem>
                <MenuItem value="ZONE_SUMMARY">Zone Summary</MenuItem>
              </Select>
            </FormControl>
            <FormControl fullWidth>
              <InputLabel>Format</InputLabel>
              <Select value={form.format} label="Format" onChange={(e) => setForm((f) => ({ ...f, format: e.target.value }))}>
                <MenuItem value="CSV">CSV</MenuItem>
                <MenuItem value="EXCEL">Excel</MenuItem>
                <MenuItem value="PDF">PDF</MenuItem>
              </Select>
            </FormControl>
            <Stack direction="row" spacing={2}>
              <TextField label="From Date" type="date" value={form.dateFrom} onChange={(e) => setForm((f) => ({ ...f, dateFrom: e.target.value }))} InputLabelProps={{ shrink: true }} fullWidth />
              <TextField label="To Date" type="date" value={form.dateTo} onChange={(e) => setForm((f) => ({ ...f, dateTo: e.target.value }))} InputLabelProps={{ shrink: true }} fullWidth />
            </Stack>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" disabled={queueMutation.isPending} onClick={() => queueMutation.mutate()}>
            {queueMutation.isPending ? <CircularProgress size={20} /> : 'Generate'}
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar open={!!toast} autoHideDuration={toast?.sev === 'error' ? 5000 : 4000} onClose={() => setToast(null)}>
        <Alert severity={toast?.sev} onClose={() => setToast(null)}>{toast?.msg}</Alert>
      </Snackbar>
    </Box>
  );
}
