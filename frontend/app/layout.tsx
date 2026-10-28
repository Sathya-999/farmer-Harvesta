import type { Metadata } from 'next'
import './globals.css'
import SceneLightingRig from '@/components/ecosystem/SceneLightingRig'
import AutoHidingScrollbar from '@/components/ui/AutoHidingScrollbar'

export const metadata: Metadata = {
  title: 'Smart Farmer — Farm Management Platform',
  description: 'The farm management platform built for Indian farmers. Crop tracking, live mandi prices, government schemes, weather — all in one place.',
  keywords: ['Smart Farmer', 'farmer app', 'mandi prices', 'crop management', 'PM-KISAN', 'India agriculture'],
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" style={{ margin: 0, padding: 0, height: '100%' }}>
      <head>
        <link rel="preconnect" href="https://fonts.googleapis.com" />
        <link rel="preconnect" href="https://fonts.gstatic.com" crossOrigin="anonymous" />
        <link
          href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&family=Fraunces:opsz,wght@9..144,400;9..144,600;9..144,700;9..144,800&family=Inter:wght@300;400;500;600;700&family=Fragment+Mono&display=swap"
          rel="stylesheet"
        />
      </head>
      <body style={{ margin: 0, padding: 0, minHeight: '100%' }}>
        <AutoHidingScrollbar width={6} rightOffset={3} />
        <SceneLightingRig />
        {children}
      </body>
    </html>
  )
}
