'use client'

import React, { useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import AuthGuard from '@/components/auth/AuthGuard'
import HarvestaDashboard from '@/components/dashboard/HarvestaDashboard'
import { farmerService, FarmerProfileDto } from '@/lib/farmerService'
import { plotService, PlotResponseDto } from '@/lib/plotService'
import { cultivationService, CultivationResponseDto } from '@/lib/cultivationService'
import { notificationService } from '@/lib/notificationService'
import { isAuthenticated } from '@/lib/authService'

export default function DashboardPage() {
  const router = useRouter()

  const [profile, setProfile] = useState<FarmerProfileDto | null>(null)
  const [plots, setPlots] = useState<PlotResponseDto[]>([])
  const [selectedPlot, setSelectedPlot] = useState<PlotResponseDto | null>(null)
  const [cultivations, setCultivations] = useState<CultivationResponseDto[]>([])
  const [unreadCount, setUnreadCount] = useState<number>(0)
  const [farmerAnalytics, setFarmerAnalytics] = useState<any>(null)

  useEffect(() => {
    if (!isAuthenticated()) {
      router.replace('/login')
      return
    }

    async function loadDashboard() {
      try {
        const [profRes, plotsRes, cultivationsRes, unreadRes, analyticsRes] = await Promise.allSettled([
          farmerService.getProfile(),
          plotService.listPlots(0, 50, 'ACTIVE'),
          cultivationService.listCultivations({ page: 0, size: 50 }),
          notificationService.getUnreadCount(),
          (await import('@/lib/analyticsService')).analyticsService.getFarmerSummary(),
        ])

        if (profRes.status === 'fulfilled' && profRes.value) {
          if (!profRes.value.onboardingCompleted) {
            router.replace('/onboarding')
            return
          }
          setProfile(profRes.value)
        }

        if (plotsRes.status === 'fulfilled') {
          const list = plotsRes.value.content || []
          setPlots(list)
          if (list.length > 0) {
            setSelectedPlot(list[0])
          }
        }

        if (cultivationsRes.status === 'fulfilled') {
          setCultivations(cultivationsRes.value.content || [])
        }

        if (unreadRes.status === 'fulfilled' && typeof unreadRes.value === 'number') {
          setUnreadCount(unreadRes.value)
        }

        if (analyticsRes.status === 'fulfilled' && analyticsRes.value) {
          setFarmerAnalytics(analyticsRes.value)
        }
      } catch (err) {
        console.error('Error initializing agricultural dashboard:', err)
      }
    }

    loadDashboard()
  }, [router])

  return (
    <AuthGuard>
      <HarvestaDashboard
        profile={profile}
        plots={plots}
        selectedPlot={selectedPlot}
        cultivations={cultivations}
        unreadCount={unreadCount}
        farmerAnalytics={farmerAnalytics}
      />
    </AuthGuard>
  )
}
