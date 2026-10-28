'use client'

import React, { useState, useEffect } from 'react'
import SidebarNav from '@/components/dashboard/SidebarNav'
import FieldHeader from '@/components/dashboard/FieldHeader'
import FieldSummaryCards from '@/components/dashboard/FieldSummaryCards'
import TopRightControls from '@/components/dashboard/TopRightControls'
import RecommendationPanel from '@/components/dashboard/RecommendationPanel'
import AssistantBar from '@/components/dashboard/AssistantBar'
import NpkAnalysisCard from '@/components/dashboard/NpkAnalysisCard'
import LeafAreaCard from '@/components/dashboard/LeafAreaCard'
import SoilMoistureCard from '@/components/dashboard/SoilMoistureCard'
import FieldMapBackground from '@/components/dashboard/FieldMapBackground'
import { FarmerProfileDto } from '@/lib/farmerService'
import { PlotResponseDto } from '@/lib/plotService'
import { CultivationResponseDto } from '@/lib/cultivationService'

interface Props {
  profile?: FarmerProfileDto | null
  plots?: PlotResponseDto[]
  selectedPlot?: PlotResponseDto | null
  cultivations?: CultivationResponseDto[]
  unreadCount?: number
  farmerAnalytics?: any
}

export default function HarvestaDashboard({
  profile,
  plots = [],
  selectedPlot = null,
  cultivations = [],
  unreadCount = 0,
  farmerAnalytics = null,
}: Props) {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)

  const activeCultivation = selectedPlot
    ? cultivations.find((c) => c.plotId === selectedPlot.id) || cultivations[0] || null
    : cultivations[0] || null

  const resolvedLocation = selectedPlot?.districtName || profile?.districtName || profile?.stateName || 'India'

  return (
    <div
      style={{
        position: 'relative',
        minHeight: '100svh',
        background: '#ECEAE1',
        overflowX: 'hidden',
        fontFamily: 'var(--font-body, system-ui, -apple-system, sans-serif)',
      }}
    >
      {/* ── Left Sidebar ── */}
      <SidebarNav
        profile={profile}
        unreadCount={unreadCount}
        mobileOpen={mobileMenuOpen}
        onToggleMobile={() => setMobileMenuOpen(!mobileMenuOpen)}
      />

      {/* ── Main Agricultural Farmland Canvas ── */}
      <main
        className="main-canvas"
        style={{
          marginLeft: 68,
          position: 'relative',
          minHeight: '100svh',
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'space-between',
          padding: '20px clamp(16px, 2.2vw, 28px)',
          boxSizing: 'border-box',
        }}
      >
        {/* Background Aerial Farmland & 3 Topographic Contour Plots */}
        <FieldMapBackground />

        {/* ── Content Container Over Farmland Canvas ── */}
        <div
          style={{
            position: 'relative',
            zIndex: 10,
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
            gap: 18,
            flex: 1,
          }}
        >
          {/* ── Upper Zone: Header (Left) & Controls (Right) ── */}
          <div
            style={{
              display: 'flex',
              alignItems: 'flex-start',
              justifyContent: 'space-between',
              gap: 16,
              flexWrap: 'wrap',
            }}
          >
            <FieldHeader selectedPlot={selectedPlot} />

            <TopRightControls
              locationLabel={resolvedLocation}
              plotsCount={plots.length}
              farmerAnalytics={farmerAnalytics}
              onZoomIn={() => {}}
              onZoomOut={() => {}}
              onToggleLayer={() => {}}
              onCenter={() => {}}
            />
          </div>

          {/* ── Middle Floating Zone: Summary Cards (Left) & Advisor Section (Right) ── */}
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              gap: 24,
              flexWrap: 'wrap',
              margin: 'auto 0',
            }}
          >
            {/* Top-Left Cluster: Real planted area, plots, and cultivation breakdown */}
            <FieldSummaryCards
              selectedPlot={selectedPlot}
              plots={plots}
              cultivations={cultivations}
              activeCultivation={activeCultivation}
              farmerAnalytics={farmerAnalytics}
            />

            {/* Right Cluster: Floating Advisory Text, See details Button, Farmer Cutout and Assistant Bar */}
            <div
              style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'flex-end',
                gap: 16,
                maxWidth: 400,
              }}
            >
              <RecommendationPanel />
              <AssistantBar fieldName={selectedPlot?.label || 'Agricultural Plot'} />
            </div>
          </div>

          {/* ── Lower Analytics Zone: 3 Bottom Cards Across Width ── */}
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
              gap: 16,
              width: '100%',
              marginTop: 4,
            }}
          >
            {/* Bottom Card 1: Land Capacity & Cultivation Allocation */}
            <NpkAnalysisCard farmerAnalytics={farmerAnalytics} plots={plots} />

            {/* Bottom Card 2: Cultivation Lifecycle Distribution */}
            <LeafAreaCard cultivations={cultivations} />

            {/* Bottom Card 3: Season & Holding Utilization Status */}
            <SoilMoistureCard farmerAnalytics={farmerAnalytics} cultivations={cultivations} />
          </div>
        </div>
      </main>

      {/* ── Strict Responsive Overrides ── */}
      <style jsx global>{`
        @media (min-width: 769px) {
          .main-canvas {
            margin-left: 68px !important;
            padding-top: 20px !important;
          }
        }
        @media (max-width: 768px) {
          .main-canvas {
            margin-left: 0 !important;
            padding-top: 68px !important;
          }
        }
      `}</style>
    </div>
  )
}
