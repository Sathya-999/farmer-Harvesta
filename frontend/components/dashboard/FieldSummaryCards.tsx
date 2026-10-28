'use client'

import React from 'react'
import Image from 'next/image'
import Link from 'next/link'
import { Sprout, Activity, Droplets, Plus } from 'lucide-react'
import { CultivationResponseDto } from '@/lib/cultivationService'
import { PlotResponseDto } from '@/lib/plotService'

interface Props {
  selectedPlot?: PlotResponseDto | null
  plots?: PlotResponseDto[]
  cultivations?: CultivationResponseDto[]
  activeCultivation?: CultivationResponseDto | null
  farmerAnalytics?: any
}

export default function FieldSummaryCards({
  selectedPlot,
  plots = [],
  cultivations = [],
  activeCultivation,
  farmerAnalytics,
}: Props) {
  const plantedAreaVal = farmerAnalytics?.currentlyCultivatedAcres != null
    ? `${farmerAnalytics.currentlyCultivatedAcres} ac`
    : (selectedPlot ? `${selectedPlot.totalAreaAcres} ac` : '0 ac')

  const totalPlotsVal = plots.length > 0
    ? `${plots.length} ${plots.length === 1 ? 'Plot' : 'Plots'}`
    : (selectedPlot ? '1 Plot' : '0 Plots')

  const utilizationVal = farmerAnalytics?.utilizationPercentage != null
    ? `${Math.round(farmerAnalytics.utilizationPercentage)}%`
    : (cultivations.length > 0 ? '100%' : '0%')

  const activeCropName = activeCultivation?.cropName || (cultivations[0]?.cropName) || 'No Active Crop'
  const activeCycleArea = activeCultivation?.cultivatedAreaAcres != null
    ? `${activeCultivation.cultivatedAreaAcres} ac`
    : (selectedPlot ? `${selectedPlot.totalAreaAcres} ac` : '0 ac')

  const cropImages = ['/corn.jpg', '/potato.jpg', '/carrot.jpg']

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12, width: '100%', maxWidth: 440 }}>
      {/* ── Top Row: 3 Rounded Metric Pills ── */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 10 }}>
        {/* Metric 1: Planted area */}
        <div
          style={{
            background: 'rgba(246, 244, 235, 0.94)',
            backdropFilter: 'blur(12px)',
            borderRadius: 18,
            padding: '8px 12px',
            display: 'flex',
            alignItems: 'center',
            gap: 10,
            boxShadow: '0 4px 16px rgba(0, 0, 0, 0.08)',
          }}
        >
          <div
            style={{
              width: 30,
              height: 30,
              borderRadius: '50%',
              background: '#EAE7DB',
              color: '#555C4D',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexShrink: 0,
            }}
          >
            <Sprout size={15} strokeWidth={2} />
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', minWidth: 0 }}>
            <span style={{ fontSize: 10, color: '#7E8376', fontWeight: 500, whiteSpace: 'nowrap' }}>
              Planted area
            </span>
            <span style={{ fontSize: 16, fontWeight: 700, color: '#22281D', lineHeight: 1.15 }}>
              {plantedAreaVal}
            </span>
          </div>
        </div>

        {/* Metric 2: Registered Plots */}
        <div
          style={{
            background: 'rgba(246, 244, 235, 0.94)',
            backdropFilter: 'blur(12px)',
            borderRadius: 18,
            padding: '8px 12px',
            display: 'flex',
            alignItems: 'center',
            gap: 10,
            boxShadow: '0 4px 16px rgba(0, 0, 0, 0.08)',
          }}
        >
          <div
            style={{
              width: 30,
              height: 30,
              borderRadius: '50%',
              background: '#EAE7DB',
              color: '#555C4D',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexShrink: 0,
            }}
          >
            <Activity size={15} strokeWidth={2} />
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', minWidth: 0 }}>
            <span style={{ fontSize: 10, color: '#7E8376', fontWeight: 500, whiteSpace: 'nowrap' }}>
              Holdings
            </span>
            <span style={{ fontSize: 16, fontWeight: 700, color: '#22281D', lineHeight: 1.15 }}>
              {totalPlotsVal}
            </span>
          </div>
        </div>

        {/* Metric 3: Utilization */}
        <div
          style={{
            background: 'rgba(246, 244, 235, 0.94)',
            backdropFilter: 'blur(12px)',
            borderRadius: 18,
            padding: '8px 12px',
            display: 'flex',
            alignItems: 'center',
            gap: 10,
            boxShadow: '0 4px 16px rgba(0, 0, 0, 0.08)',
          }}
        >
          <div
            style={{
              width: 30,
              height: 30,
              borderRadius: '50%',
              background: '#EAE7DB',
              color: '#555C4D',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexShrink: 0,
            }}
          >
            <Droplets size={15} strokeWidth={2} />
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', minWidth: 0 }}>
            <span style={{ fontSize: 10, color: '#7E8376', fontWeight: 500, whiteSpace: 'nowrap' }}>
              Utilization
            </span>
            <span style={{ fontSize: 16, fontWeight: 700, color: '#22281D', lineHeight: 1.15 }}>
              {utilizationVal}
            </span>
          </div>
        </div>
      </div>

      {/* ── Main Feature Card: Tractor, Cultivation Status, and Real Crops Grid ── */}
      <div
        style={{
          background: 'rgba(246, 244, 235, 0.96)',
          backdropFilter: 'blur(14px)',
          borderRadius: 22,
          padding: '16px 18px',
          boxShadow: '0 6px 24px rgba(0, 0, 0, 0.08)',
          display: 'grid',
          gridTemplateColumns: '1.05fr 1.35fr',
          gap: 16,
          alignItems: 'center',
        }}
      >
        {/* Left Side: Green Tractor Topdown & Active Cultivation */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          <div
            style={{
              position: 'relative',
              width: '100%',
              height: 72,
              borderRadius: 14,
              overflow: 'hidden',
            }}
          >
            <Image
              src="/green_tractor.jpg"
              alt="Cultivation Cycle"
              fill
              style={{ objectFit: 'cover' }}
            />
          </div>

          <div style={{ display: 'flex', flexDirection: 'column' }}>
            <span
              style={{
                alignSelf: 'flex-start',
                fontSize: 9.5,
                fontWeight: 700,
                padding: '2px 8px',
                borderRadius: 999,
                background: '#D4FD33',
                color: '#1A2215',
                marginBottom: 4,
              }}
            >
              {activeCultivation?.status || 'LAND READY'}
            </span>
            <div style={{ fontSize: 26, fontWeight: 800, color: '#22281D', lineHeight: 1 }}>
              {activeCycleArea}
            </div>
            <div style={{ fontSize: 11, color: '#7E8376', fontWeight: 500, marginTop: 2 }}>
              {activeCropName}
            </div>
          </div>
        </div>

        {/* Right Side: Real Crops Grid (up to 3 items + Add Crop Button) */}
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(2, 1fr)',
            gap: 8,
          }}
        >
          {cultivations.slice(0, 3).map((item, idx) => (
            <div
              key={item.id}
              style={{
                background: '#FFFFFF',
                borderRadius: 12,
                padding: '6px 8px',
                display: 'flex',
                alignItems: 'center',
                gap: 8,
                border: '1px solid #EAE7DC',
              }}
            >
              <div style={{ width: 34, height: 34, position: 'relative', borderRadius: 8, overflow: 'hidden', flexShrink: 0 }}>
                <Image src={cropImages[idx % cropImages.length]} alt={item.cropName || 'Crop'} fill style={{ objectFit: 'cover' }} />
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', minWidth: 0 }}>
                <span style={{ fontSize: 11, fontWeight: 700, color: '#22281D', lineHeight: 1.1, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                  {item.cropName}
                </span>
                <span style={{ fontSize: 9.5, color: '#7E8376', marginTop: 1 }}>
                  {item.cultivatedAreaAcres} ac
                </span>
              </div>
            </div>
          ))}

          {/* If fewer than 3 cultivations, show placeholder slot */}
          {cultivations.length === 0 && (
            <div
              style={{
                background: '#FFFFFF',
                borderRadius: 12,
                padding: '6px 8px',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'center',
                border: '1px dashed #D5D3C7',
              }}
            >
              <span style={{ fontSize: 10.5, fontWeight: 600, color: '#555C4D' }}>No Cycles</span>
              <span style={{ fontSize: 9, color: '#8E9285' }}>Add below</span>
            </div>
          )}

          {/* + Add Crop Button */}
          <Link
            href="/cultivations/new"
            style={{
              background: '#D4FD33',
              borderRadius: 12,
              padding: '8px 10px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: 4,
              textDecoration: 'none',
              color: '#1A2215',
              fontSize: 11.5,
              fontWeight: 700,
              boxShadow: '0 2px 8px rgba(212, 253, 51, 0.4)',
              transition: 'transform 0.15s',
            }}
          >
            <Plus size={14} strokeWidth={2.8} />
            <span>Add Crop</span>
          </Link>
        </div>
      </div>
    </div>
  )
}
