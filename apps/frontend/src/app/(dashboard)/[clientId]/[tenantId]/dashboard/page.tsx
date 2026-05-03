'use client';

import { useState } from 'react';
import { useParams } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import {
  Box, Grid, Card, CardContent, Typography, Skeleton, Paper,
} from '@mui/material';
import {
  DirectionsCar, CheckCircle, CalendarMonth, Speed,
} from '@mui/icons-material';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend,
  LineChart, Line, ResponsiveContainer, Area, AreaChart, ReferenceLine,
} from 'recharts';
import { mockApi } from '@/lib/api';

import { useTenantStore } from '@/store/tenantStore';
import { PageHeader } from '@/components/common/PageHeader';

interface MetricCardProps {
  title: string;
  value: string | number;
  icon: React.ElementType;
  color: string;
  bg: string;
  subtitle?: string;
}

function MetricCard({ title, value, icon: Icon, color, bg, subtitle }: MetricCardProps) {
  return (
    <Card>
      <CardContent sx={{ p: 2.5 }}>
        <Box sx={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between' }}>
          <Box>
            <Typography variant="body2" color="text.secondary" fontWeight={500} gutterBottom>
              {title}
            </Typography>
            <Typography variant="h4" fontWeight={700} color="text.primary">
              {value}
            </Typography>
            {subtitle && (
              <Typography variant="caption" color="text.secondary">
                {subtitle}
              </Typography>
            )}
          </Box>
          <Box sx={{ p: 1.25, borderRadius: 2, bgcolor: bg }}>
            <Icon sx={{ fontSize: 26, color }} />
          </Box>
        </Box>
      </CardContent>
    </Card>
  );
}

export default function DashboardPage() {
  const { tenantId } = useParams<{ tenantId: string }>();
  const branding = useTenantStore((s) => s.branding);

  const { data: quota, isLoading } = useQuery({
    queryKey: ['quota', tenantId],
    queryFn: () => mockApi.getQuotaUsage(tenantId),
  });

  const barData: { date: string; count: number }[] = [];
  const lineData: { date: string; count: number }[] = [];

  const quotaPct = quota ? Math.round((quota.daily.used / quota.daily.limit) * 100) : 0;
  const quotaColor = quotaPct >= 90 ? '#D32F2F' : quotaPct >= 70 ? '#ED6C02' : '#2E7D32';

  return (
    <Box>
      <PageHeader title="Dashboard" subtitle="Overview of your parking validation activity" />

      {/* Metrics */}
      <Grid container spacing={2.5} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={3}>
          {isLoading ? (
            <Skeleton variant="rectangular" height={110} sx={{ borderRadius: 2 }} />
          ) : (
            <MetricCard
              title="Validations Today"
              value={42}
              icon={DirectionsCar}
              color="#1565C0"
              bg="#E3F2FD"
            />
          )}
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          {isLoading ? (
            <Skeleton variant="rectangular" height={110} sx={{ borderRadius: 2 }} />
          ) : (
            <MetricCard
              title="Active Sessions"
              value={18}
              icon={CheckCircle}
              color="#2E7D32"
              bg="#E8F5E9"
            />
          )}
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          {isLoading ? (
            <Skeleton variant="rectangular" height={110} sx={{ borderRadius: 2 }} />
          ) : (
            <MetricCard
              title="This Month"
              value={387}
              icon={CalendarMonth}
              color="#E65100"
              bg="#FFF3E0"
            />
          )}
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          {isLoading ? (
            <Skeleton variant="rectangular" height={110} sx={{ borderRadius: 2 }} />
          ) : (
            <MetricCard
              title="Quota Used"
              value={`${quotaPct}%`}
              icon={Speed}
              color={quotaColor}
              bg={`${quotaColor}18`}
              subtitle={`${quota?.daily.used ?? 0} of ${quota?.daily.limit ?? 0} daily`}
            />
          )}
        </Grid>
      </Grid>

      {/* Charts */}
      <Grid container spacing={2.5}>
        <Grid item xs={12} lg={7}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" fontWeight={600} gutterBottom>
              Validations per Zone — Last 7 Days
            </Typography>
            <Box sx={{ height: 280, mt: 2 }}>
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={barData} margin={{ top: 4, right: 16, left: -16, bottom: 4 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                  <XAxis dataKey="date" tick={{ fontSize: 12 }} />
                  <YAxis tick={{ fontSize: 12 }} />
                  <Tooltip />
                  <Legend wrapperStyle={{ fontSize: 12 }} />
                  <Bar dataKey="Ground Floor" stackId="a" fill={branding.primaryColor} radius={[0, 0, 0, 0]} />
                  <Bar dataKey="Upper Level" stackId="a" fill={branding.accentColor} radius={[0, 0, 0, 0]} />
                  <Bar dataKey="Rooftop" stackId="a" fill={`${branding.primaryColor}80`} radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </Box>
          </Paper>
        </Grid>

        <Grid item xs={12} lg={5}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" fontWeight={600} gutterBottom>
              Daily Validations — Last 30 Days
            </Typography>
            <Box sx={{ height: 280, mt: 2 }}>
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={lineData} margin={{ top: 4, right: 16, left: -16, bottom: 4 }}>
                  <defs>
                    <linearGradient id="colorVal" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor={branding.primaryColor} stopOpacity={0.3} />
                      <stop offset="95%" stopColor={branding.primaryColor} stopOpacity={0} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                  <XAxis dataKey="date" tick={{ fontSize: 10 }} interval={6} />
                  <YAxis tick={{ fontSize: 12 }} />
                  <Tooltip />
                  <Area
                    type="monotone"
                    dataKey="validations"
                    stroke={branding.primaryColor}
                    strokeWidth={2}
                    fill="url(#colorVal)"
                  />
                </AreaChart>
              </ResponsiveContainer>
            </Box>
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
}
