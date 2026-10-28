import React, { useState } from 'react'
import { PlotResponseDto } from '@/lib/plotService'

interface Props {
  farmerAnalytics?: any
  plots?: PlotResponseDto[]
}

export default function NpkAnalysisCard({ farmerAnalytics, plots = [] }: Props) {
  const [activeTab, setActiveTab] = useState<'Allocation' | 'Record'>('Allocation')

  const totalAcres = farmerAnalytics?.totalLandAreaAcres || 0
  const cultAcres = farmerAnalytics?.currentlyCultivatedAcres || 0
  const bufferAcres = Math.max(0, totalAcres - cultAcres)
  const cultPct = totalAcres > 0 ? Math.min(100, Math.round((cultAcres / totalAcres) * 100)) : 0
  const bufferPct = totalAcres > 0 ? Math.min(100, Math.round((bufferAcres / totalAcres) * 100)) : 0

  const items = activeTab === 'Allocation'
    ? [
        { label: 'Cultivated Land', value: `${cultAcres} ac`, pct: cultPct },
        { label: 'Available Buffer', value: `${bufferAcres.toFixed(1)} ac`, pct: bufferPct },
        { label: 'Total Holding', value: `${totalAcres} ac`, pct: 100 },
      ]
    : [
        { label: 'Active Cycles', value: `${farmerAnalytics?.activeCultivationsCount || 0}`, pct: (farmerAnalytics?.activeCultivationsCount ? 80 : 10) },
        { label: 'Registered Plots', value: `${plots.length}`, pct: (plots.length ? 100 : 10) },
        { label: 'Primary Crop', value: farmerAnalytics?.primaryCrop || 'None', pct: 60 },
      ]

  return (
    <div
      style={{
        background: 'rgba(246, 244, 235, 0.96)',
        backdropFilter: 'blur(14px)',
        borderRadius: 22,
        padding: '16px 20px',
        boxShadow: '0 6px 24px rgba(0, 0, 0, 0.08)',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between',
        height: '100%',
        boxSizing: 'border-box',
      }}
    >
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: 12 }}>
        <div style={{ display: 'flex', flexDirection: 'column' }}>
          <span style={{ fontSize: 15, fontWeight: 700, color: '#22281D', lineHeight: 1.2 }}>
            Land Allocation
          </span>
          <span style={{ fontSize: 11, color: '#8E9285', marginTop: 2 }}>
            Verified Holdings
          </span>
        </div>

        {/* Allocation / Record Pill Tabs */}
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
            onClick={() => setActiveTab('Allocation')}
            style={{
              background: activeTab === 'Allocation' ? '#D4FD33' : 'transparent',
              color: activeTab === 'Allocation' ? '#1A2215' : '#7E8376',
              border: 'none',
              borderRadius: 999,
              padding: '3px 12px',
              fontSize: 11,
              fontWeight: activeTab === 'Allocation' ? 700 : 500,
              cursor: 'pointer',
              transition: 'background 0.15s',
            }}
          >
            Allocation
          </button>
          <button
            onClick={() => setActiveTab('Record')}
            style={{
              background: activeTab === 'Record' ? '#D4FD33' : 'transparent',
              color: activeTab === 'Record' ? '#1A2215' : '#7E8376',
              border: 'none',
              borderRadius: 999,
              padding: '3px 12px',
              fontSize: 11,
              fontWeight: activeTab === 'Record' ? 700 : 500,
              cursor: 'pointer',
              transition: 'background 0.15s',
            }}
          >
            Record
          </button>
        </div>
      </div>

      {/* 3 Metric Bars */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        {items.map((item) => (
          <div key={item.label} style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ fontSize: 12, fontWeight: 600, color: '#22281D' }}>
                {item.label}
              </span>
              <span style={{ fontSize: 12, fontWeight: 700, color: '#22281D' }}>
                {item.value}
              </span>
            </div>

            {/* Thin Bar */}
            <div
              style={{
                width: '100%',
                height: 4,
                borderRadius: 2,
                background: '#DFDCCF',
                overflow: 'hidden',
              }}
            >
              <div
                style={{
                  width: `${item.pct}%`,
                  height: '100%',
                  borderRadius: 2,
                  background: '#2F5233',
                  transition: 'width 0.3s ease',
                }}
              />
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
