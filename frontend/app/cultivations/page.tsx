'use client'

import React, { useState, useEffect, useCallback } from 'react'
import Link from 'next/link'
import { motion } from 'framer-motion'
import { cultivationService, CultivationResponseDto, CultivationStatus } from '@/lib/cultivationService'
import AppLayout from '@/components/ui/AppLayout'
import {
  Layers,
  Plus,
  Calendar,
  MapPin,
  ChevronRight,
  AlertCircle,
  RefreshCw,
  Clock,
  CheckCircle2,
} from 'lucide-react'

export default function CultivationsPage() {
  const [cultivations, setCultivations] = useState<CultivationResponseDto[]>([])
  const [loading, setLoading] = useState(true)
  const [errorMsg, setErrorMsg] = useState<string | null>(null)
  const [statusFilter, setStatusFilter] = useState<string>('ALL')

  const fetchCultivations = useCallback(async () => {
    setLoading(true)
    setErrorMsg(null)
    try {
      const res = await cultivationService.listCultivations({
        page: 0,
        size: 50,
        status: statusFilter === 'ALL' ? undefined : statusFilter,
      })
      setCultivations(res?.content || [])
    } catch (err: any) {
      setErrorMsg(err.message || 'Failed to retrieve cultivation records.')
    } finally {
      setLoading(false)
    }
  }, [statusFilter])

  useEffect(() => {
    fetchCultivations()
  }, [fetchCultivations])

  const totalAcreage = cultivations.reduce((acc, c) => acc + (c.cultivatedAreaAcres || 0), 0)
  const activeCount = cultivations.filter((c) => c.status === 'STARTED' || c.status === 'PLANNED').length

  return (
    <AppLayout>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 16 }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4 }}>
              <span style={{ fontSize: 11, fontFamily: 'var(--font-mono, monospace)', color: 'var(--canopy-700, #2F5233)', fontWeight: 700, textTransform: 'uppercase' }}>
                Crop Production Cycles
              </span>
            </div>
            <h1 style={{ fontFamily: 'var(--font-display, "Plus Jakarta Sans", sans-serif)', fontSize: 28, fontWeight: 700, color: 'var(--text-primary, #1A1408)', margin: 0 }}>
              Cultivation Cycles
            </h1>
            <p style={{ margin: '4px 0 0', fontSize: 13.5, color: 'var(--text-muted, #9A7562)' }}>
              Manage crop planning, active cycles, and state transitions across your registered plots.
            </p>
          </div>

          <Link
            href="/cultivations/new"
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: 8,
              padding: '11px 20px',
              borderRadius: 12,
              background: 'linear-gradient(140deg, #163A24 0%, #2E7D32 100%)',
              color: '#FFFFFF',
              fontSize: 14,
              fontWeight: 600,
              textDecoration: 'none',
              boxShadow: '0 4px 14px rgba(46, 125, 50, 0.3)',
            }}
          >
            <Plus size={16} />
            <span>Register Cultivation</span>
          </Link>
        </div>

        {/* Aggregate Metrics Bar */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: 14 }}>
          <div style={{ padding: '16px 20px', borderRadius: 16, background: 'rgba(255, 255, 255, 0.85)', border: '1px solid var(--border-soft, #D9E1D8)' }}>
            <span style={{ fontSize: 11, fontWeight: 600, color: 'var(--text-muted, #9A7562)', textTransform: 'uppercase' }}>
              Total Cycles
            </span>
            <p style={{ fontFamily: 'var(--font-mono, monospace)', fontSize: 26, fontWeight: 600, color: 'var(--canopy-700, #2F5233)', margin: '4px 0 0' }}>
              {loading ? '…' : cultivations.length}
            </p>
          </div>

          <div style={{ padding: '16px 20px', borderRadius: 16, background: 'rgba(255, 255, 255, 0.85)', border: '1px solid var(--border-soft, #D9E1D8)' }}>
            <span style={{ fontSize: 11, fontWeight: 600, color: 'var(--text-muted, #9A7562)', textTransform: 'uppercase' }}>
              Active In Ground
            </span>
            <p style={{ fontFamily: 'var(--font-mono, monospace)', fontSize: 26, fontWeight: 600, color: '#2E7D32', margin: '4px 0 0' }}>
              {loading ? '…' : activeCount}
            </p>
          </div>

          <div style={{ padding: '16px 20px', borderRadius: 16, background: 'rgba(255, 255, 255, 0.85)', border: '1px solid var(--border-soft, #D9E1D8)' }}>
            <span style={{ fontSize: 11, fontWeight: 600, color: 'var(--text-muted, #9A7562)', textTransform: 'uppercase' }}>
              Cultivated Acreage
            </span>
            <p style={{ fontFamily: 'var(--font-mono, monospace)', fontSize: 26, fontWeight: 600, color: 'var(--canopy-700, #2F5233)', margin: '4px 0 0' }}>
              {loading ? '…' : `${totalAcreage.toFixed(2)} acres`}
            </p>
          </div>
        </div>

        {/* Filter Tabs */}
        <div style={{ display: 'flex', gap: 8, overflowX: 'auto', paddingBottom: 4 }}>
          {['ALL', 'PLANNED', 'STARTED', 'COMPLETED', 'CANCELLED', 'ABANDONED'].map((st) => (
            <button
              key={st}
              onClick={() => setStatusFilter(st)}
              style={{
                padding: '8px 14px',
                borderRadius: 20,
                border: statusFilter === st ? '1px solid var(--canopy-700, #2F5233)' : '1px solid var(--border-soft, #D9E1D8)',
                background: statusFilter === st ? 'var(--canopy-700, #2F5233)' : 'rgba(255, 255, 255, 0.8)',
                color: statusFilter === st ? '#FFFFFF' : 'var(--text-muted, #9A7562)',
                fontSize: 12,
                fontWeight: 600,
                cursor: 'pointer',
                whiteSpace: 'nowrap',
              }}
            >
              {st}
            </button>
          ))}
        </div>

        {/* Error Alert */}
        {errorMsg && (
          <div style={{ padding: '14px 18px', borderRadius: 12, background: 'rgba(183, 28, 28, 0.08)', color: '#B71C1C', fontSize: 13, display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <AlertCircle size={18} />
              <span>{errorMsg}</span>
            </div>
            <button onClick={fetchCultivations} style={{ background: 'transparent', border: 'none', color: '#B71C1C', cursor: 'pointer', fontWeight: 600 }}>
              Retry
            </button>
          </div>
        )}

        {/* Loading */}
        {loading && (
          <div style={{ padding: 48, textAlign: 'center', color: 'var(--text-muted, #9A7562)', fontSize: 14 }}>
            Loading cultivation cycles…
          </div>
        )}

        {/* Empty State */}
        {!loading && cultivations.length === 0 && !errorMsg && (
          <div style={{ padding: '56px 20px', textAlign: 'center', borderRadius: 24, background: 'rgba(255, 255, 255, 0.8)', border: '1.5px dashed var(--border-soft, #D9E1D8)' }}>
            <Layers size={32} style={{ color: 'var(--canopy-600, #3D6B42)', margin: '0 auto 12px' }} />
            <h3 style={{ fontSize: 18, fontWeight: 700, color: 'var(--text-primary, #1A1408)', margin: '0 0 6px' }}>
              No Cultivation Cycles Found
            </h3>
            <p style={{ fontSize: 13.5, color: 'var(--text-muted, #9A7562)', maxWidth: 420, margin: '0 auto 20px' }}>
              Register your first crop cultivation cycle on an active plot holding to track planting milestones and harvest forecasts.
            </p>
            <Link
              href="/cultivations/new"
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
              <span>Register Cultivation</span>
            </Link>
          </div>
        )}

        {/* Cultivation Grid */}
        {!loading && cultivations.length > 0 && (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: 18 }}>
            {cultivations.map((c, i) => (
              <motion.div
                key={c.id}
                initial={{ opacity: 0, y: 14 }}
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
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 10 }}>
                    <div>
                      <span style={{ fontSize: 11, fontFamily: 'var(--font-mono, monospace)', color: 'var(--text-muted, #9A7562)', textTransform: 'uppercase' }}>
                        {c.seasonName || 'Season'} · {c.cultivationYear}
                      </span>
                      <h3 style={{ fontSize: 20, fontWeight: 700, color: 'var(--text-primary, #1A1408)', margin: '2px 0 0' }}>
                        {c.cropName || 'Primary Crop'}
                      </h3>
                      {c.varietyName && (
                        <p style={{ margin: '2px 0 0', fontSize: 12, color: 'var(--canopy-700, #2F5233)', fontWeight: 600 }}>
                          Variety: {c.varietyName}
                        </p>
                      )}
                    </div>

                    <span style={getStatusBadgeStyle(c.status)}>
                      {c.status}
                    </span>
                  </div>

                  {/* Cultivated Area */}
                  <div style={{ display: 'flex', alignItems: 'baseline', gap: 6, margin: '14px 0 10px' }}>
                    <span style={{ fontFamily: 'var(--font-mono, monospace)', fontSize: 28, fontWeight: 700, color: 'var(--canopy-700, #2F5233)', lineHeight: 1 }}>
                      {c.cultivatedAreaAcres}
                    </span>
                    <span style={{ fontSize: 13, color: 'var(--text-muted, #9A7562)' }}>acres cultivated</span>
                  </div>

                  {/* Plot Attached */}
                  <div style={{ padding: '8px 12px', borderRadius: 10, background: 'rgba(251, 244, 228, 0.65)', border: '1px solid rgba(217, 225, 216, 0.6)', marginBottom: 14 }}>
                    <p style={{ margin: 0, fontSize: 12, color: 'var(--text-secondary, #6B4E3D)' }}>
                      <strong>Plot:</strong> {c.plotLabel || `Plot #${c.plotId.substring(0, 8)}`}
                    </p>
                  </div>
                </div>

                <div style={{ borderTop: '1px solid var(--border-soft, #D9E1D8)', paddingTop: 14 }}>
                  <Link
                    href={`/cultivations/${c.id}`}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      gap: 6,
                      padding: '9px 14px',
                      borderRadius: 10,
                      background: 'rgba(47, 82, 51, 0.08)',
                      border: '1px solid rgba(47, 82, 51, 0.2)',
                      color: 'var(--canopy-700, #2F5233)',
                      fontSize: 13,
                      fontWeight: 600,
                      textDecoration: 'none',
                    }}
                  >
                    <span>View Lifecycle & Events</span>
                    <ChevronRight size={14} />
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

function getStatusBadgeStyle(status: CultivationStatus): React.CSSProperties {
  switch (status) {
    case 'STARTED':
      return { padding: '3px 8px', borderRadius: 12, fontSize: 11, fontWeight: 700, background: 'rgba(46, 125, 50, 0.15)', color: '#2E7D32' }
    case 'PLANNED':
      return { padding: '3px 8px', borderRadius: 12, fontSize: 11, fontWeight: 700, background: 'rgba(242, 200, 75, 0.25)', color: '#8D6E14' }
    case 'COMPLETED':
      return { padding: '3px 8px', borderRadius: 12, fontSize: 11, fontWeight: 700, background: 'rgba(77, 131, 82, 0.15)', color: '#2F5233' }
    case 'CANCELLED':
    case 'ABANDONED':
      return { padding: '3px 8px', borderRadius: 12, fontSize: 11, fontWeight: 700, background: 'rgba(183, 28, 28, 0.1)', color: '#B71C1C' }
    default:
      return { padding: '3px 8px', borderRadius: 12, fontSize: 11, fontWeight: 700, background: '#E0E0E0', color: '#424242' }
  }
}
