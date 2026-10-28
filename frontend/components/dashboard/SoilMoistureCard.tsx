import React, { useState } from 'react'
import { CultivationResponseDto } from '@/lib/cultivationService'

interface Props {
  farmerAnalytics?: any
  cultivations?: CultivationResponseDto[]
}

export default function SoilMoistureCard({ farmerAnalytics, cultivations = [] }: Props) {
  const [activeSeason, setActiveSeason] = useState<'Kharif' | 'Rabi'>('Kharif')
  const [selectedCategory, setSelectedCategory] = useState<string>('Crops')

  const categories = ['Crops', 'Cycles', 'Schedules', 'Harvests', 'Holdings', 'Telemetry']

  const calloutLabel = farmerAnalytics?.utilizationPercentage != null
    ? `${Math.round(farmerAnalytics.utilizationPercentage)}% Active`
    : `${cultivations.length} Cycles`

  return (
    <div
      style={{
        background: 'rgba(246, 244, 235, 0.96)',
        backdropFilter: 'blur(14px)',
        borderRadius: 22,
        padding: '16px 20px 12px',
        boxShadow: '0 6px 24px rgba(0, 0, 0, 0.08)',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between',
        height: '100%',
        boxSizing: 'border-box',
      }}
    >
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: 6 }}>
        <div style={{ display: 'flex', flexDirection: 'column' }}>
          <span style={{ fontSize: 15, fontWeight: 700, color: '#22281D', lineHeight: 1.2 }}>
            Season Trajectory
          </span>
          <span style={{ fontSize: 11, color: '#8E9285', marginTop: 2 }}>
            Cultivation Schedule
          </span>
        </div>

        {/* Season Pill Tabs */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            background: '#EAE7DC',
            borderRadius: 999,
            padding: 2,
          }}
        >
          <button
            onClick={() => setActiveSeason('Kharif')}
            style={{
              background: activeSeason === 'Kharif' ? '#D4FD33' : 'transparent',
              color: activeSeason === 'Kharif' ? '#1A2215' : '#7E8376',
              border: 'none',
              borderRadius: 999,
              padding: '3px 12px',
              fontSize: 11,
              fontWeight: activeSeason === 'Kharif' ? 700 : 500,
              cursor: 'pointer',
              transition: 'background 0.15s',
            }}
          >
            Kharif
          </button>
          <button
            onClick={() => setActiveSeason('Rabi')}
            style={{
              background: activeSeason === 'Rabi' ? '#D4FD33' : 'transparent',
              color: activeSeason === 'Rabi' ? '#1A2215' : '#7E8376',
              border: 'none',
              borderRadius: 999,
              padding: '3px 12px',
              fontSize: 11,
              fontWeight: activeSeason === 'Rabi' ? 700 : 500,
              cursor: 'pointer',
              transition: 'background 0.15s',
            }}
          >
            Rabi
          </button>
        </div>
      </div>

      {/* SVG Chart with Floating Tooltip Callout */}
      <div style={{ position: 'relative', width: '100%', height: 68, margin: '2px 0' }}>
        <svg width="100%" height="100%" viewBox="0 0 280 65" preserveAspectRatio="none">
          <defs>
            <linearGradient id="seasonGrad" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="#C9C7B6" stopOpacity="0.45" />
              <stop offset="100%" stopColor="#EAE7DC" stopOpacity="0.05" />
            </linearGradient>
          </defs>

          {/* Area fill */}
          <path
            d="M 0 46 Q 20 48, 35 44 T 70 46 T 100 35 L 105 18 L 115 18 L 125 36 T 160 40 T 195 44 T 225 32 L 235 22 L 245 42 T 280 44 L 280 65 L 0 65 Z"
            fill="url(#seasonGrad)"
          />

          {/* Curve stroke */}
          <path
            d="M 0 46 Q 20 48, 35 44 T 70 46 T 100 35 L 105 18 L 115 18 L 125 36 T 160 40 T 195 44 T 225 32 L 235 22 L 245 42 T 280 44"
            fill="none"
            stroke="#636A5B"
            strokeWidth="1.8"
            strokeLinecap="round"
          />
        </svg>

        {/* Floating Tooltip Callout */}
        <div
          style={{
            position: 'absolute',
            top: 2,
            left: '38%',
            transform: 'translateX(-50%)',
            background: '#FFFFFF',
            borderRadius: 8,
            padding: '2px 8px',
            fontSize: 10,
            fontWeight: 700,
            color: '#22281D',
            boxShadow: '0 2px 8px rgba(0,0,0,0.14)',
            border: '1px solid #EAE7DC',
            whiteSpace: 'nowrap',
          }}
        >
          {calloutLabel}
        </div>
      </div>

      {/* Bottom Category Tabs */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          borderTop: '1px solid #EAE7DC',
          paddingTop: 8,
          fontSize: 10.5,
        }}
      >
        {categories.map((cat) => {
          const isSelected = selectedCategory === cat
          return (
            <button
              key={cat}
              onClick={() => setSelectedCategory(cat)}
              style={{
                background: 'none',
                border: 'none',
                padding: '2px 4px',
                fontSize: 10.5,
                fontWeight: isSelected ? 700 : 500,
                color: isSelected ? '#1A2215' : '#8E9285',
                cursor: 'pointer',
                transition: 'color 0.15s',
              }}
            >
              {cat}
            </button>
          )
        })}
      </div>
    </div>
  )
}
