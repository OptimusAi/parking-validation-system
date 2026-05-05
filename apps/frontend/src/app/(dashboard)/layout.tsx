'use client';

import { useState } from 'react';
import { Box, Toolbar } from '@mui/material';
import { Sidebar, SIDEBAR_WIDTH, SIDEBAR_WIDTH_MINI } from '@/components/layout/Sidebar';
import { TopBar } from '@/components/layout/TopBar';

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  const [mobileOpen, setMobileOpen] = useState(false);
  const [collapsed,  setCollapsed]  = useState(true);   // icon-only by default

  const sidebarWidth = collapsed ? SIDEBAR_WIDTH_MINI : SIDEBAR_WIDTH;

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh', bgcolor: 'background.default' }}>
      <Sidebar
        mobileOpen={mobileOpen}
        onMobileClose={() => setMobileOpen(false)}
        collapsed={collapsed}
        onToggle={() => setCollapsed((v) => !v)}
      />
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          width: { md: `calc(100% - ${sidebarWidth}px)` },
          minWidth: 0,
          transition: 'width 0.2s ease',
        }}
      >
        <TopBar
          onMenuClick={() => setMobileOpen(true)}
          sidebarWidth={sidebarWidth}
        />
        <Toolbar sx={{ minHeight: 64 }} />
        <Box sx={{ p: { xs: 2, sm: 3 }, maxWidth: 1400, mx: 'auto' }}>
          {children}
        </Box>
      </Box>
    </Box>
  );
}
