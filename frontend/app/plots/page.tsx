'use client'

import React, { useState, useEffect, useCallback } from 'react'
import Link from 'next/link'
import { motion } from 'framer-motion'
import { plotService, PlotResponseDto } from '@/lib/plotService'
import AppLayout from '@/components/ui/AppLayout'
import {
  MapPin,
  Plus,
  Layers,
  ChevronRight,
  AlertCircle,
  RefreshCw,
  CheckCircle2,
  XCircle,
  Maximize2,
} from 'lucide-react'

export default function PlotsPage() {
  const [plots, setPlots] = useState<PlotResponseDto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [statusFilter, setStatusFilter] = useState<'ACTIVE' | 'INACTIVE' | 'ALL'>('ACTIVE')

  const fetchPlots = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await plotService.listPlots(0, 50, statusFilter)
      setPlots(res?.content || [])
    } catch (err: any) {
      setError(err.message || 'Failed to retrieve plots from agricultural backend.')
    } finally {
      setLoading(false)
    }
  }, [statusFilter])

  useEffect(() => {
    fetchPlots()
  }, [fetchPlots])

  const totalAcreage = plots.reduce((acc, p) => acc + (p.totalAreaAcres || 0), 0)

  return (
    <AppLayout>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
        {/* Top Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 16 }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4 }}>
              <span style={{ fontSize: 11, fontFamily: 'var(--font-mono, monospace)', color: 'var(--canopy-700, #2F5233)', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.08em' }}>
                Land Holdings Substrate
              </span>
            </div>
            <h1 style={{ fontFamily: 'var(--font-display, "Plus Jakarta Sans", sans-serif)', fontSize: 28, fontWeight: 700, color: 'var(--text-primary, #1A1408)', margin: 0 }}>
              My Agricultural Plots
            </h1>
            <p style={{ margin: '4px 0 0', fontSize: 13.5, color: 'var(--text-muted, #9A7562)' }}>
              Operational land parcels registered under your farmer profile for crop cultivation cycles.
            </p>
          </div>

          <Link
            href="/plots/new"
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: 8,
              padding: '11px 20px',
              borderRadius: 12,
              background: 'linear-gradient(140deg, #163A24 0%, #2E7D32 100%)',
              color: '#FFFFFF',
              fontFamily: 'var(--font-body, "Inter", sans-serif)',
              fontSize: 14,
              fontWeight: 600,
              textDecoration: 'none',
              boxShadow: '0 4px 14px rgba(46, 125, 50, 0.3)',
            }}
          >
            <Plus size={16} />
            <span>Add New Plot</span>
          </Link>
        </div>

        {/* Aggregate Stats Summary Bar */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: 14 }}>
          <div
            style={{
              padding: '16px 20px',
              borderRadius: 16,
              background: 'rgba(255, 255, 255, 0.85)',
              backdropFilter: 'blur(10px)',
              border: '1px solid var(--border-soft, #D9E1D8)',
            }}
          >
            <span style={{ fontSize: 11, fontWeight: 600, color: 'var(--text-muted, #9A7562)', textTransform: 'uppercase' }}>
              Registered Plots
            </span>
            <p style={{ fontFamily: 'var(--font-mono, monospace)', fontSize: 26, fontWeight: 600, color: 'var(--canopy-700, #2F5233)', margin: '4px 0 0' }}>
              {loading ? '…' : plots.length}
            </p>
          </div>

          <div
            style={{
              padding: '16px 20px',
              borderRadius: 16,
              background: 'rgba(255, 255, 255, 0.85)',
              backdropFilter: 'blur(10px)',
              border: '1px solid var(--border-soft, #D9E1D8)',
            }}
          >
            <span style={{ fontSize: 11, fontWeight: 600, color: 'var(--text-muted, #9A7562)', textTransform: 'uppercase' }}>
              Total Land Holding
            </span>
            <p style={{ fontFamily: 'var(--font-mono, monospace)', fontSize: 26, fontWeight: 600, color: 'var(--canopy-700, #2F5233)', margin: '4px 0 0' }}>
              {loading ? '…' : `${totalAcreage.toFixed(2)} acres`}
            </p>
          </div>

          {/* Status Filter Tabs */}
          <div
            style={{
              padding: 12,
              borderRadius: 16,
              background: 'rgba(255, 255, 255, 0.85)',
              border: '1px solid var(--border-soft, #D9E1D8)',
              display: 'flex',
              alignItems: 'center',
              gap: 8,
            }}
          >
            {(['ACTIVE', 'INACTIVE', 'ALL'] as const).map((s) => (
              <button
                key={s}
                onClick={() => setStatusFilter(s)}
                style={{
                  flex: 1,
                  padding: '9px 12px',
                  borderRadius: 10,
                  border: statusFilter === s ? '1px solid rgba(47, 82, 51, 0.25)' : 'none',
                  background: statusFilter === s ? 'var(--canopy-700, #2F5233)' : 'transparent',
                  color: statusFilter === s ? '#FFFFFF' : 'var(--text-muted, #9A7562)',
                  fontFamily: 'var(--font-body, "Inter", sans-serif)',
                  fontSize: 12.5,
                  fontWeight: 600,
                  cursor: 'pointer',
                  transition: 'all 0.15s ease',
                }}
              >
                {s}
              </button>
            ))}
          </div>
        </div>

        {/* Error Alert */}
        {error && (
          <div
            style={{
              padding: '14px 18px',
              borderRadius: 12,
              background: 'rgba(183, 28, 28, 0.08)',
              border: '1px solid rgba(183, 28, 28, 0.25)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              color: '#B71C1C',
              fontSize: 13,
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <AlertCircle size={18} />
              <span>{error}</span>
            </div>
            <button
              onClick={fetchPlots}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 6,
                background: 'transparent',
                border: 'none',
                color: '#B71C1C',
                fontWeight: 600,
                cursor: 'pointer',
              }}
            >
              <RefreshCw size={14} />
              <span>Retry</span>
            </button>
          </div>
        )}

        {/* Loading Skeleton */}
        {loading && (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: 18 }}>
            {[1, 2, 3].map((i) => (
              <div
                key={i}
                style={{
                  height: 180,
                  borderRadius: 20,
                  background: 'rgba(255, 255, 255, 0.6)',
                  border: '1.5px solid var(--border-soft, #D9E1D8)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: 'var(--text-muted, #9A7562)',
                  fontSize: 13,
                }}
              >
                Loading plot record…
              </div>
            ))}
          </div>
        )}

        {/* Empty State */}
        {!loading && plots.length === 0 && !error && (
          <div
            style={{
              padding: '56px 24px',
              textAlign: 'center',
              borderRadius: 24,
              background: 'rgba(255, 255, 255, 0.75)',
              border: '1.5px dashed var(--border-soft, #D9E1D8)',
            }}
          >
            <div
              style={{
                width: 52,
                height: 52,
                borderRadius: 16,
                background: 'rgba(122, 155, 92, 0.15)',
                color: 'var(--canopy-700, #2F5233)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                margin: '0 auto 16px',
              }}
            >
              <MapPin size={26} />
            </div>
            <h3 style={{ fontSize: 18, fontWeight: 700, color: 'var(--text-primary, #1A1408)', margin: '0 0 6px' }}>
              No Registered Plots Found
            </h3>
            <p style={{ fontSize: 13.5, color: 'var(--text-muted, #9A7562)', maxWidth: 420, margin: '0 auto 20px', lineHeight: 1.5 }}>
              Register your first plot holding with its area and location to start planning cultivation cycles and receiving localized field advisories.
            </p>
            <Link
              href="/plots/new"
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: 8,
                padding: '11px 22px',
                borderRadius: 10,
                background: 'var(--canopy-700, #2F5233)',
                color: '#FFFFFF',
                fontSize: 13.5,
                fontWeight: 600,
                textDecoration: 'none',
              }}
            >
              <Plus size={16} />
              <span>Register First Plot</span>
            </Link>
          </div>
        )}

        {/* Plots Grid */}
        {!loading && plots.length > 0 && (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: 18 }}>
            {plots.map((plot, i) => (
              <motion.div
                key={plot.id}
                initial={{ opacity: 0, y: 16 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: i * 0.05 }}
                style={{
                  borderRadius: 20,
                  background: 'rgba(255, 255, 255, 0.88)',
                  backdropFilter: 'blur(12px)',
                  border: '1.5px solid var(--border-soft, #D9E1D8)',
                  padding: '22px 20px',
                  display: 'flex',
                  flexDirection: 'column',
                  justifyContent: 'space-between',
                  boxShadow: '0 4px 18px rgba(26, 20, 8, 0.04)',
                }}
              >
                <div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 12 }}>
                    <div>
                      <span style={{ fontSize: 10.5, fontFamily: 'var(--font-mono, monospace)', color: 'var(--text-muted, #9A7562)', textTransform: 'uppercase' }}>
                        Operational Plot
                      </span>
                      <h3 style={{ fontSize: 18, fontWeight: 700, color: 'var(--text-primary, #1A1408)', margin: '2px 0 0' }}>
                        {plot.label || 'Unnamed Plot'}
                      </h3>
                    </div>

                    <span
                      style={{
                        padding: '4px 10px',
                        borderRadius: 20,
                        fontSize: 11,
                        fontWeight: 700,
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: 4,
                        background: plot.isActive ? 'rgba(46, 125, 50, 0.12)' : 'rgba(183, 28, 28, 0.1)',
                        color: plot.isActive ? '#2E7D32' : '#B71C1C',
                      }}
                    >
                      {plot.isActive ? <CheckCircle2 size={12} /> : <XCircle size={12} />}
                      {plot.isActive ? 'ACTIVE' : 'INACTIVE'}
                    </span>
                  </div>

                  {/* Acreage Metric */}
                  <div style={{ display: 'flex', alignItems: 'baseline', gap: 6, margin: '14px 0' }}>
                    <span style={{ fontFamily: 'var(--font-mono, monospace)', fontSize: 32, fontWeight: 600, color: 'var(--canopy-700, #2F5233)', lineHeight: 1 }}>
                      {plot.totalAreaAcres}
                    </span>
                    <span style={{ fontSize: 14, color: 'var(--text-muted, #9A7562)', fontWeight: 500 }}>acres</span>
                  </div>

                  {/* Administrative Location */}
                  <div style={{ display: 'flex', alignItems: 'flex-start', gap: 8, padding: '10px 12px', borderRadius: 10, background: 'rgba(251, 244, 228, 0.65)', border: '1px solid rgba(217, 225, 216, 0.6)', marginBottom: 16 }}>
                    <MapPin size={15} style={{ color: 'var(--canopy-700, #2F5233)', flexShrink: 0, marginTop: 2 }} />
                    <p style={{ margin: 0, fontSize: 12, color: 'var(--text-secondary, #6B4E3D)', lineHeight: 1.4 }}>
                      {plot.villageName ? `${plot.villageName}, ` : ''}
                      {plot.mandalName ? `${plot.mandalName}, ` : ''}
                      {plot.districtName || 'Registered Location'}
                    </p>
                  </div>
                </div>

                {/* Card Footer Actions */}
                <div style={{ display: 'flex', gap: 8, borderTop: '1px solid var(--border-soft, #D9E1D8)', paddingTop: 14 }}>
                  <Link
                    href={`/plots/${plot.id}`}
                    style={{
                      flex: 1,
                      display: 'inline-flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      gap: 6,
                      padding: '9px 12px',
                      borderRadius: 10,
                      background: 'rgba(47, 82, 51, 0.08)',
                      border: '1px solid rgba(47, 82, 51, 0.18)',
                      color: 'var(--canopy-700, #2F5233)',
                      fontSize: 12.5,
                      fontWeight: 600,
                      textDecoration: 'none',
                    }}
                  >
                    <span>View Details</span>
                    <ChevronRight size={14} />
                  </Link>

                  <Link
                    href={`/cultivations/new?plotId=${plot.id}`}
                    style={{
                      display: 'inline-flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      gap: 6,
                      padding: '9px 14px',
                      borderRadius: 10,
                      background: 'var(--canopy-700, #2F5233)',
                      color: '#FFFFFF',
                      fontSize: 12.5,
                      fontWeight: 600,
                      textDecoration: 'none',
                    }}
                    title="Register Cultivation on Plot"
                  >
                    <Layers size={14} />
                    <span>Plant Crop</span>
                  </Link>
                </div>
              </motion.div>
            ))}
          </div>
        )}
      </div>
    </AppLayout>
  )
}
