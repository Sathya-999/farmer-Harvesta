import React from 'react'
import type { Metadata } from 'next'
import AgriculturalSplashScreen from '@/components/splash/AgriculturalSplashScreen'

export const metadata: Metadata = {
  title: 'Harvest(a) — Agricultural Intelligence & Coordination Platform',
  description:
    'Bringing farmers, cultivation insights, and connected agricultural data together to build a smarter, more informed, and resilient farming future.',
}

export default function HomePage() {
  return <AgriculturalSplashScreen />
}
