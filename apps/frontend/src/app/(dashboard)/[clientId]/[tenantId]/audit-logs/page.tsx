'use client';

import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Box, Chip, Stack, TextField, Select, MenuItem, FormControl,
  InputLabel, Paper, Typography, Collapse, Grid, Alert,
  InputAdornment,
} from '@mui/material';
import { DataGrid, type GridColDef } from '@mui/x-data-grid';
import { Search, ExpandMore, ExpandLess } from '@mui/icons-material';
import { format } from 'date-fns';
import { mockApi } from '@/lib/api';
import type { AuditLog } from '@/lib/types';
import { useTenantStore } from '@/store/tenantStore';
import { PageHeader } from '@/components/common/PageHeader';

const ACTION_COLORS: Record<string, 'success' | 'info' | 'warning' | 'error' | 'default'> = {
  SESSION_CREATED: 'success',
  SESSION_EXTENDED: 'info',
  SESSION_CANCELLED: 'warning',
  ROLE_CHANGED: 'error',
  LINK_CREATED: 'success',
  LINK_DEACTIVATED: 'warning',
  ZONE_CREATED: 'success',
  BRANDING_UPDATED: 'info',
  USER_UPDATED: 'info',
};

function JsonDisplay({ data, bg }: { data: Record<string, unknown>; bg: string }) {
  return (
    <Box
      component="pre"
      sx={{
        bgcolor: bg,
        p: 1.5,
        borderRadius: 1,
        fontSize: '0.75rem',
        overflow: 'auto',
        m: 0,
        fontFamily: 'monospace',
        maxHeight: 200,
      }}
    >
      {JSON.stringify(data, null, 2)}
    </Box>
  );
}

function AuditDetailRow({ row }: { row: AuditLog }) {
  const [expanded, setExpanded] = useState(false);

  return (
    <>
      <Box
        onClick={() => setExpanded((e) => !e)}
        sx={{
          display: 'flex',
          alignItems: 'center',
          px: 2,
          py: 1,
          cursor: 'pointer',
          '&:hover': { bgcolor: 'action.hover' },
          borderBottom: '1px solid',
          borderColor: 'divider',
        }}
      >
        <Box sx={{ flex: 1, display: 'flex', alignItems: 'center', gap: 2, flexWrap: 'wrap' }}>
          <Typography variant="caption" color="text.secondary" sx={{ minWidth: 130 }}>
            {format(new Date(row.timestamp), 'MMM d, h:mm:ss a')}
          </Typography>
          <Typography variant="body2" sx={{ minWidth: 140 }}>{row.actorEmail}</Typography>
          <Chip label={row.action.replace(/_/g, ' ')} size="small" color={ACTION_COLORS[row.action] ?? 'default'} />
          <Typography variant="body2" color="text.secondary">{row.entityType}</Typography>
          <Typography variant="caption" fontFamily="monospace" color="text.secondary">{row.ipAddress}</Typography>
        </Box>
        {expanded ? <ExpandLess sx={{ fontSize: 18, color: 'text.secondary' }} /> : <ExpandMore sx={{ fontSize: 18, color: 'text.secondary' }} />}
      </Box>
      <Collapse in={expanded}>
        <Box sx={{ px: 2, py: 2, bgcolor: 'grey.50', borderBottom: '1px solid', borderColor: 'divider' }}>
          <Grid container spacing={2}>
            <Grid item xs={12} sm={6}>
              <Typography variant="caption" fontWeight={600} color="error.main" gutterBottom display="block">BEFORE</Typography>
              <JsonDisplay data={row.before ?? {}} bg="#FFEBEE" />
            </Grid>
            <Grid item xs={12} sm={6}>
              <Typography variant="caption" fontWeight={600} color="success.main" gutterBottom display="block">AFTER</Typography>
              <JsonDisplay data={row.after ?? {}} bg="#E8F5E9" />
            </Grid>
          </Grid>
        </Box>
      </Collapse>
    </>
  );
}

export default function AuditLogsPage() {
  const role = useTenantStore((s) => s.role);
  const [actionFilter, setActionFilter] = useState('');
  const [actorSearch, setActorSearch] = useState('');

  const { data, isLoading } = useQuery({
    queryKey: ['audit-logs', actionFilter, actorSearch],
    queryFn: () => mockApi.getAuditLogs({ action: actionFilter || undefined, actor: actorSearch || undefined }),
    enabled: ['ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN'].includes(role ?? ''),
  });

  if (!['ADMIN', 'CLIENT_ADMIN', 'TENANT_ADMIN'].includes(role ?? '')) {
    return (
      <Box>
        <PageHeader title="Audit Logs" />
        <Alert severity="error">You do not have permission to view audit logs.</Alert>
      </Box>
    );
  }

  const logs = data?.items ?? [];

  return (
    <Box>
      <PageHeader title="Audit Logs" subtitle="Full history of actions taken in the platform" />

      {/* Filters */}
      <Paper sx={{ p: 2, mb: 2 }}>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
          <TextField
            placeholder="Search by actor..."
            value={actorSearch}
            onChange={(e) => setActorSearch(e.target.value)}
            InputProps={{ startAdornment: <InputAdornment position="start"><Search sx={{ fontSize: 18 }} /></InputAdornment> }}
            sx={{ minWidth: 220 }}
          />
          <FormControl sx={{ minWidth: 200 }}>
            <InputLabel>Action</InputLabel>
            <Select value={actionFilter} label="Action" onChange={(e) => setActionFilter(e.target.value)}>
              <MenuItem value="">All Actions</MenuItem>
              {['SESSION_CREATED', 'SESSION_EXTENDED', 'SESSION_CANCELLED', 'ROLE_CHANGED', 'LINK_CREATED', 'LINK_DEACTIVATED', 'ZONE_CREATED', 'BRANDING_UPDATED', 'USER_UPDATED'].map((a) => (
                <MenuItem key={a} value={a}>{a.replace(/_/g, ' ')}</MenuItem>
              ))}
            </Select>
          </FormControl>
        </Stack>
      </Paper>

      {/* Log List */}
      <Paper sx={{ overflow: 'hidden' }}>
        {isLoading ? (
          <Box sx={{ p: 3, textAlign: 'center' }}>
            <Typography color="text.secondary">Loading audit logs...</Typography>
          </Box>
        ) : logs.length === 0 ? (
          <Box sx={{ p: 3, textAlign: 'center' }}>
            <Typography color="text.secondary">No audit logs found.</Typography>
          </Box>
        ) : (
          <Box>
            {/* Header */}
            <Box sx={{ display: 'flex', px: 2, py: 1, bgcolor: 'grey.50', borderBottom: '1px solid', borderColor: 'divider' }}>
              <Typography variant="caption" fontWeight={700} color="text.secondary" sx={{ minWidth: 130 }}>TIMESTAMP</Typography>
              <Typography variant="caption" fontWeight={700} color="text.secondary" sx={{ minWidth: 140 }}>ACTOR</Typography>
              <Typography variant="caption" fontWeight={700} color="text.secondary" sx={{ minWidth: 160 }}>ACTION</Typography>
              <Typography variant="caption" fontWeight={700} color="text.secondary">ENTITY</Typography>
            </Box>
            {logs.map((log) => (
              <AuditDetailRow key={log.id} row={log} />
            ))}
          </Box>
        )}
      </Paper>
    </Box>
  );
}
