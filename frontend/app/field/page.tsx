'use client'

import React, { useState, useEffect } from 'react'
import Link from 'next/link'
import { motion } from 'framer-motion'
import { Droplets, Sprout, ArrowRight, MapPin, Radio, AlertCircle } from 'lucide-react'
import NavRail from '@/components/ui/NavRail'
import AssistantPanel from '@/components/ui/AssistantPanel'
import HorizonScene from '@/components/ecosystem/HorizonScene'
import { plotService, PlotResponseDto } from '@/lib/plotService'
import { cultivationService, CultivationResponseDto } from '@/lib/cultivationService'

export default function FieldPage() {
  const [assistantOpen, setAssistantOpen] = useState(false)
  const [plots, setPlots] = useState<PlotResponseDto[]>([])
  const [cultivations, setCultivations] = useState<CultivationResponseDto[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    async function loadData() {
      try {
        const [pRes, cRes] = await Promise.allSettled([
          plotService.listPlots(0, 50, 'ACTIVE'),
          cultivationService.listCultivations({ page: 0, size: 50 }),
        ])
        if (pRes.status === 'fulfilled') setPlots(pRes.value.content || [])
        if (cRes.status === 'fulfilled') setCultivations(cRes.value.content || [])
      } catch (err) {
        console.error('Failed to load field holdings:', err)
      } finally {
        setLoading(false)
      }
    }
    loadData()
  }, [])

  const selectedPlot = plots[0] || null
  const activeCultivation = selectedPlot
    ? cultivations.find((c) => c.plotId === selectedPlot.id) || cultivations[0] || null
    : cultivations[0] || null

  return (
    <div style={{ position: 'relative', minHeight: '100svh' }}>
      <HorizonScene />
      <NavRail active="/field" onAssistant={() => setAssistantOpen(true)} />

      <main style={{ marginLeft: 72, padding: '32px 28px 80px', position: 'relative', zIndex: 5, maxWidth: 1080 }}>
        {/* Header */}
        <motion.div
          initial={{ opacity: 0, y: -16 }}
          animate={{ opacity: 1, y: 0 }}
          style={{ marginBottom: 28 }}
        >
          <p style={{ fontFamily: 'var(--font-mono)', fontSize: 11, color: 'var(--text-muted)', letterSpacing: '0.1em', textTransform: 'uppercase', margin: 0 }}>
            Field Workspace · Verified Holding
          </p>
          <h1 style={{ fontFamily: 'var(--font-display)', fontSize: 30, fontWeight: 600, color: 'var(--text-primary)', margin: '4px 0 0' }}>
            {selectedPlot?.label || 'Agricultural Plot Workspace'}
          </h1>
          <p style={{ margin: '4px 0 0', fontSize: 13, color: 'var(--text-muted)' }}>
            {selectedPlot ? `${selectedPlot.villageName ? selectedPlot.villageName + ', ' : ''}${selectedPlot.districtName || 'Registered'} • ${selectedPlot.totalAreaAcres} acres` : 'Farmland registry'}
          </p>
        </motion.div>

        {/* Telemetry Status Banner */}
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          style={{
            background: 'rgba(251, 244, 228, 0.95)',
            backdropFilter: 'blur(14px)',
            borderRadius: 20,
            border: '1.5px solid var(--border-soft, #D9E1D8)',
            padding: 24,
            marginBottom: 28,
            boxShadow: '0 8px 28px rgba(26, 20, 8, 0.06)',
            display: 'flex',
            flexDirection: 'column',
            gap: 14,
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <div
              style={{
                width: 40,
                height: 40,
                borderRadius: 12,
                background: 'rgba(47, 82, 51, 0.12)',
                color: 'var(--canopy-700, #2F5233)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                flexShrink: 0,
              }}
            >
              <Radio size={22} />
            </div>
            <div>
              <h2 style={{ fontSize: 17, fontWeight: 700, margin: 0, color: 'var(--text-primary)' }}>
                Physical In-Situ IoT Sensors (Scheduled for Beta V2)
              </h2>
              <span style={{ fontSize: 12, color: 'var(--text-muted)' }}>
                Hardware probe telemetry gateway status
              </span>
            </div>
          </div>

          <p style={{ fontSize: 13.5, color: '#3A3F31', lineHeight: 1.6, margin: 0 }}>
            In accordance with platform integrity rules, live soil moisture percentages, NPK ppm levels, and canopy leaf sensors are not simulated. Field sensor gateways and hardware probes will be linked during Phase Beta V2. Verified administrative boundaries and crop cycles are active below.
          </p>

          <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', paddingTop: 4 }}>
            <Link
              href="/plots"
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: 8,
                padding: '9px 18px',
                borderRadius: 12,
                background: 'var(--canopy-700, #2F5233)',
                color: '#FFFFFF',
                fontSize: 13,
                fontWeight: 600,
                textDecoration: 'none',
              }}
            >
              <MapPin size={16} />
              <span>Manage Field Plots</span>
            </Link>
            <Link
              href="/cultivations"
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: 8,
                padding: '9px 18px',
                borderRadius: 12,
                background: '#FFFFFF',
                border: '1.5px solid var(--border-soft, #D9E1D8)',
                color: 'var(--canopy-700, #2F5233)',
                fontSize: 13,
                fontWeight: 600,
                textDecoration: 'none',
              }}
            >
              <Sprout size={16} />
              <span>Active Cultivations</span>
            </Link>
          </div>
        </motion.div>

        {/* Real Active Cultivation Detail */}
        {activeCultivation && (
          <motion.div
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            style={{
              background: 'rgba(251, 244, 228, 0.95)',
              backdropFilter: 'blur(14px)',
              borderRadius: 20,
              border: '1.5px solid var(--border-soft, #D9E1D8)',
              padding: 24,
              boxShadow: '0 8px 28px rgba(26, 20, 8, 0.06)',
            }}
          >
            <h3 style={{ fontSize: 18, fontWeight: 700, margin: '0 0 14px', color: 'var(--text-primary)' }}>
              Active Crop Holding: {activeCultivation.cropName}
            </h3>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: 16 }}>
              <div>
                <span style={{ fontSize: 11, color: 'var(--text-muted)' }}>Cultivated Area</span>
                <div style={{ fontSize: 18, fontWeight: 700, color: 'var(--text-primary)' }}>
                  {activeCultivation.cultivatedAreaAcres} acres
                </div>
              </div>
              <div>
                <span style={{ fontSize: 11, color: 'var(--text-muted)' }}>Season / Year</span>
                <div style={{ fontSize: 18, fontWeight: 700, color: 'var(--text-primary)' }}>
                  {activeCultivation.seasonName || 'Current'} {activeCultivation.cultivationYear}
                </div>
              </div>
              <div>
                <span style={{ fontSize: 11, color: 'var(--text-muted)' }}>Lifecycle Status</span>
                <div style={{ fontSize: 18, fontWeight: 700, color: 'var(--canopy-700)' }}>
                  {activeCultivation.status}
                </div>
              </div>
            </div>
          </motion.div>
        )}
      </main>

      <AssistantPanel open={assistantOpen} onClose={() => setAssistantOpen(false)} />
    </div>
  )
}
