import type { Metadata } from 'next';
import { Inter } from 'next/font/google';
import { MuiThemeProvider } from '@/providers/MuiThemeProvider';
import { QueryProvider } from '@/providers/QueryProvider';
import './globals.css';

const inter = Inter({ subsets: ['latin'] });

export const metadata: Metadata = {
  title: 'TMS Parking Validation',
  description: 'Multi-tenant SaaS parking validation platform',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body className={inter.className}>
        <QueryProvider>
          <MuiThemeProvider>{children}</MuiThemeProvider>
        </QueryProvider>
      </body>
    </html>
  );
}
