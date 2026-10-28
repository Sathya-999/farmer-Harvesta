'use client'

import React from 'react'
import Link from 'next/link'
import { ArrowLeft } from 'lucide-react'
import { PlotResponseDto } from '@/lib/plotService'

interface Props {
  selectedPlot?: PlotResponseDto | null
  plots?: PlotResponseDto[]
  onSelectPlot?: (plot: PlotResponseDto) => void
}

export default function FieldHeader({
  selectedPlot,
}: Props) {
  const plotTitle = selectedPlot?.label || 'Agricultural Workspace'
  const locationSubtitle = selectedPlot
    ? `${selectedPlot.villageName ? selectedPlot.villageName + ', ' : ''}${selectedPlot.districtName || 'Registered Plot'}`
    : 'Harvest(a) Platform'
  const specSubtitle = selectedPlot
    ? `${selectedPlot.totalAreaAcres} ac (${(selectedPlot.totalAreaAcres * 0.404686).toFixed(1)} ha) • ${selectedPlot.isActive ? 'Active Holding' : 'Inactive'}`
    : 'Verified Land Intelligence'

  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
      {/* Circular White Back Button */}
      <Link
        href="/plots"
        title="View Plots"
        style={{
          width: 38,
          height: 38,
          borderRadius: '50%',
          background: '#FFFFFF',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: '#22281D',
          textDecoration: 'none',
          boxShadow: '0 2px 8px rgba(0,0,0,0.12)',
          transition: 'transform 0.15s',
          flexShrink: 0,
        }}
      >
        <ArrowLeft size={18} strokeWidth={2.4} />
      </Link>

      {/* Field Title and Location / Specs */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <h1
          style={{
            fontSize: 30,
            fontWeight: 700,
            color: '#FFFFFF',
            textShadow: '0 2px 8px rgba(0, 0, 0, 0.45)',
            margin: 0,
            lineHeight: 1,
            letterSpacing: '-0.02em',
            fontFamily: 'var(--font-body, system-ui, sans-serif)',
          }}
        >
          {plotTitle}
        </h1>

        <div
          style={{
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'center',
            fontSize: 11,
            fontWeight: 500,
            color: 'rgba(255, 255, 255, 0.92)',
            textShadow: '0 1px 4px rgba(0, 0, 0, 0.55)',
            lineHeight: 1.25,
            paddingTop: 2,
          }}
        >
          <span>{locationSubtitle}</span>
          <span style={{ opacity: 0.85 }}>{specSubtitle}</span>
        </div>
      </div>
    </div>
  )
}
