'use client'

import React from 'react'
import { Cloud, Flag, Plus, Minus, Maximize2 } from 'lucide-react'

interface Props {
  onZoomIn?: () => void
  onZoomOut?: () => void
  onToggleLayer?: () => void
  onCenter?: () => void
  locationLabel?: string
  plotsCount?: number
  farmerAnalytics?: any
}

export default function TopRightControls({
  onZoomIn,
  onZoomOut,
  onToggleLayer,
  locationLabel = 'India',
  plotsCount = 0,
  farmerAnalytics = null,
}: Props) {
  const activeLandValue = farmerAnalytics?.currentlyCultivatedAcres != null && farmerAnalytics.currentlyCultivatedAcres > 0
    ? `${farmerAnalytics.currentlyCultivatedAcres} ac`
    : `${plotsCount} ${plotsCount === 1 ? 'Plot' : 'Plots'}`

  return (
    <div style={{ display: 'flex', alignItems: 'flex-start', gap: 16 }}>
      {/* ── Floating Info Chips ── */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
        {/* Active Land Chip */}
        <div
          style={{
            background: 'rgba(246, 244, 235, 0.94)',
            backdropFilter: 'blur(12px)',
            borderRadius: 18,
            padding: '8px 18px 8px 12px',
            display: 'flex',
            alignItems: 'center',
            gap: 10,
            boxShadow: '0 4px 16px rgba(0, 0, 0, 0.08)',
          }}
        >
          <div
            style={{
              width: 32,
              height: 32,
              borderRadius: '50%',
              background: '#EAE7DB',
              color: '#555C4D',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <Cloud size={16} strokeWidth={2} />
          </div>
          <div style={{ display: 'flex', flexDirection: 'column' }}>
            <span style={{ fontSize: 10.5, color: '#7E8376', fontWeight: 500 }}>
              Active Holdings
            </span>
            <span style={{ fontSize: 16, fontWeight: 700, color: '#22281D', lineHeight: 1.15 }}>
              {activeLandValue}
            </span>
          </div>
        </div>

        {/* Location Nebraska Chip */}
        <div
          style={{
            background: 'rgba(246, 244, 235, 0.94)',
            backdropFilter: 'blur(12px)',
            borderRadius: 18,
            padding: '8px 18px 8px 12px',
            display: 'flex',
            alignItems: 'center',
            gap: 10,
            boxShadow: '0 4px 16px rgba(0, 0, 0, 0.08)',
          }}
        >
          <div
            style={{
              width: 32,
              height: 32,
              borderRadius: '50%',
              background: '#EAE7DB',
              color: '#555C4D',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <Flag size={15} strokeWidth={2} />
          </div>
          <div style={{ display: 'flex', flexDirection: 'column' }}>
            <span style={{ fontSize: 10.5, color: '#7E8376', fontWeight: 500 }}>
              Location
            </span>
            <span style={{ fontSize: 16, fontWeight: 700, color: '#22281D', lineHeight: 1.15 }}>
              {locationLabel}
            </span>
          </div>
        </div>
      </div>

      {/* ── Vertical Button Stack (Fullscreen, +, -) ── */}
      <div
        style={{
          display: 'flex',
          flexDirection: 'column',
          gap: 6,
        }}
      >
        <button
          onClick={onToggleLayer}
          title="Fullscreen / Expand"
          style={{
            width: 36,
            height: 36,
            borderRadius: '50%',
            background: 'rgba(246, 244, 235, 0.94)',
            border: 'none',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#22281D',
            cursor: 'pointer',
            boxShadow: '0 2px 8px rgba(0,0,0,0.12)',
            transition: 'transform 0.15s',
          }}
        >
          <Maximize2 size={16} strokeWidth={2.2} />
        </button>

        <button
          onClick={onZoomIn}
          title="Zoom In"
          style={{
            width: 36,
            height: 36,
            borderRadius: '50%',
            background: 'rgba(246, 244, 235, 0.94)',
            border: 'none',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#22281D',
            cursor: 'pointer',
            boxShadow: '0 2px 8px rgba(0,0,0,0.12)',
            transition: 'transform 0.15s',
          }}
        >
          <Plus size={18} strokeWidth={2.4} />
        </button>

        <button
          onClick={onZoomOut}
          title="Zoom Out"
          style={{
            width: 36,
            height: 36,
            borderRadius: '50%',
            background: 'rgba(246, 244, 235, 0.94)',
            border: 'none',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#22281D',
            cursor: 'pointer',
            boxShadow: '0 2px 8px rgba(0,0,0,0.12)',
            transition: 'transform 0.15s',
          }}
        >
          <Minus size={18} strokeWidth={2.4} />
        </button>
      </div>
    </div>
  )
}
