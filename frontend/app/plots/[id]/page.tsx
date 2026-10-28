'use client'

import React, { useState, useEffect, useCallback } from 'react'
import { useParams, useRouter } from 'next/navigation'
import Link from 'next/link'
import { motion, AnimatePresence } from 'framer-motion'
import { plotService, PlotResponseDto } from '@/lib/plotService'
import { cultivationService, CultivationResponseDto } from '@/lib/cultivationService'
import AppLayout from '@/components/ui/AppLayout'
import {
  MapPin,
  ArrowLeft,
  Edit3,
  Trash2,
  Plus,
  Layers,
  AlertCircle,
  CheckCircle2,
  XCircle,
  Calendar,
  Clock,
  Maximize2,
  RefreshCw,
} from 'lucide-react'

export default function PlotDetailPage() {
  const params = useParams()
  const router = useRouter()
  const plotId = params?.id as string

  const [plot, setPlot] = useState<PlotResponseDto | null>(null)
  const [cultivations, setCultivations] = useState<CultivationResponseDto[]>([])
  const [loading, setLoading] = useState(true)
  const [errorMsg, setErrorMsg] = useState<string | null>(null)

  // Edit Modal State
  const [showEditModal, setShowEditModal] = useState(false)
  const [editLabel, setEditLabel] = useState('')
  const [editArea, setEditArea] = useState('')
  const [editLoading, setEditLoading] = useState(false)
  const [editError, setEditError] = useState<string | null>(null)

  // Deactivate State
  const [deactivating, setDeactivating] = useState(false)

  const loadData = useCallback(async () => {
    if (!plotId) return
    setLoading(true)
    setErrorMsg(null)
    try {
      const p = await plotService.getPlot(plotId)
      setPlot(p)
      setEditLabel(p.label || '')
      setEditArea(String(p.totalAreaAcres))

      const c = await cultivationService.listCultivations({ plotId })
      setCultivations(c?.content || [])
    } catch (err: any) {
      setErrorMsg(err.message || 'Failed to load plot details.')
    } finally {
      setLoading(false)
    }
  }, [plotId])

  useEffect(() => {
    loadData()
  }, [loadData])

  // Handle Edit Submit with optimistic locking
  const handleEditSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!plot) return
    setEditError(null)

    const areaNum = parseFloat(editArea)
    if (isNaN(areaNum) || areaNum <= 0) {
      setEditError('Plot area must be a positive number greater than 0.')
      return
    }

    setEditLoading(true)
    try {
      const updated = await plotService.updatePlot(plot.id, {
        label: editLabel.trim() || undefined,
        totalAreaAcres: areaNum,
        expectedVersion: plot.version,
      })
      setPlot(updated)
      setShowEditModal(false)
    } catch (err: any) {
      setEditError(err.message || 'Failed to update plot. It may have been modified by another session.')
    } finally {
      setEditLoading(false)
    }
  }

  // Handle Soft-Deactivate
  const handleDeactivate = async () => {
    if (!plot) return
    const confirmed = window.confirm(
      'Are you sure you want to deactivate this plot? Inactive plots cannot host new cultivation cycles. (Note: Deactivation will fail if an active cultivation is in progress).'
    )
    if (!confirmed) return

    setDeactivating(true)
    try {
      await plotService.deactivatePlot(plot.id)
      await loadData()
    } catch (err: any) {
      alert(`Deactivation failed: ${err.message}`)
    } finally {
      setDeactivating(false)
    }
  }

  // Handle Reactivate
  const handleReactivate = async () => {
    if (!plot) return
    try {
      await plotService.reactivatePlot(plot.id)
      await loadData()
    } catch (err: any) {
      alert(`Reactivation failed: ${err.message}`)
    }
  }

  return (
    <AppLayout>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
        {/* Top Breadcrumb & Actions */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
          <Link
            href="/plots"
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: 6,
              color: 'var(--text-muted, #9A7562)',
              fontSize: 13,
              textDecoration: 'none',
              fontWeight: 600,
            }}
          >
            <ArrowLeft size={16} />
            <span>Back to Plots</span>
          </Link>

          {plot && (
            <div style={{ display: 'flex', gap: 10 }}>
              <button
                onClick={() => setShowEditModal(true)}
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: 6,
                  padding: '8px 14px',
                  borderRadius: 10,
                  border: '1.5px solid var(--border-soft, #D9E1D8)',
                  background: '#FFFFFF',
                  color: 'var(--text-primary, #1A1408)',
                  fontSize: 13,
                  fontWeight: 600,
                  cursor: 'pointer',
                }}
              >
                <Edit3 size={14} />
                <span>Edit Plot</span>
              </button>

              {plot.isActive ? (
                <button
                  onClick={handleDeactivate}
                  disabled={deactivating}
                  style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: 6,
                    padding: '8px 14px',
                    borderRadius: 10,
                    border: '1px solid rgba(183, 28, 28, 0.25)',
                    background: 'rgba(183, 28, 28, 0.08)',
                    color: '#B71C1C',
                    fontSize: 13,
                    fontWeight: 600,
                    cursor: deactivating ? 'not-allowed' : 'pointer',
                  }}
                >
                  <Trash2 size={14} />
                  <span>Deactivate</span>
                </button>
              ) : (
                <button
                  onClick={handleReactivate}
                  style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: 6,
                    padding: '8px 14px',
                    borderRadius: 10,
                    border: '1px solid rgba(46, 125, 50, 0.3)',
                    background: 'rgba(46, 125, 50, 0.1)',
                    color: '#2E7D32',
                    fontSize: 13,
                    fontWeight: 600,
                    cursor: 'pointer',
                  }}
                >
                  <RefreshCw size={14} />
                  <span>Reactivate Plot</span>
                </button>
              )}
            </div>
          )}
        </div>

        {errorMsg && (
          <div
            style={{
              padding: '14px 18px',
              borderRadius: 12,
              background: 'rgba(183, 28, 28, 0.08)',
              border: '1px solid rgba(183, 28, 28, 0.25)',
              color: '#B71C1C',
              fontSize: 13,
            }}
          >
            {errorMsg}
          </div>
        )}

        {loading && (
          <div style={{ padding: 48, textAlign: 'center', color: 'var(--text-muted, #9A7562)', fontSize: 14 }}>
            Loading plot details…
          </div>
        )}

        {!loading && plot && (
          <>
            {/* Plot Hero Overview Card */}
            <motion.div
              initial={{ opacity: 0, y: 14 }}
              animate={{ opacity: 1, y: 0 }}
              style={{
                borderRadius: 24,
                background: 'rgba(255, 255, 255, 0.92)',
                backdropFilter: 'blur(14px)',
                border: '1.5px solid var(--border-soft, #D9E1D8)',
                padding: '28px clamp(18px, 4vw, 32px)',
                boxShadow: '0 8px 24px rgba(26, 20, 8, 0.05)',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 16 }}>
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4 }}>
                    <span style={{ fontSize: 11, fontFamily: 'var(--font-mono, monospace)', color: 'var(--canopy-700, #2F5233)', fontWeight: 700, textTransform: 'uppercase' }}>
                      Land Parcel Record
                    </span>
                    <span
                      style={{
                        padding: '3px 8px',
                        borderRadius: 12,
                        fontSize: 10.5,
                        fontWeight: 700,
                        background: plot.isActive ? 'rgba(46, 125, 50, 0.12)' : 'rgba(183, 28, 28, 0.1)',
                        color: plot.isActive ? '#2E7D32' : '#B71C1C',
                      }}
                    >
                      {plot.isActive ? 'ACTIVE HOLDING' : 'INACTIVE'}
                    </span>
                  </div>

                  <h1 style={{ fontFamily: 'var(--font-display, "Plus Jakarta Sans", sans-serif)', fontSize: 28, fontWeight: 700, color: 'var(--text-primary, #1A1408)', margin: 0 }}>
                    {plot.label || 'Unnamed Plot'}
                  </h1>

                  <p style={{ margin: '6px 0 0', fontSize: 13, color: 'var(--text-secondary, #6B4E3D)', display: 'flex', alignItems: 'center', gap: 6 }}>
                    <MapPin size={15} style={{ color: 'var(--canopy-700, #2F5233)' }} />
                    <span>
                      {plot.villageName ? `${plot.villageName}, ` : ''}
                      {plot.mandalName ? `${plot.mandalName}, ` : ''}
                      {plot.districtName ? `${plot.districtName}, ` : ''}
                      {plot.stateName || 'Registered Location'}
                    </span>
                  </p>
                </div>

                <div style={{ textAlign: 'right' }}>
                  <span style={{ fontSize: 11, fontWeight: 600, color: 'var(--text-muted, #9A7562)', textTransform: 'uppercase' }}>
                    Registered Area
                  </span>
                  <div style={{ display: 'flex', alignItems: 'baseline', gap: 4, justifyContent: 'flex-end', marginTop: 2 }}>
                    <span style={{ fontFamily: 'var(--font-mono, monospace)', fontSize: 36, fontWeight: 700, color: 'var(--canopy-700, #2F5233)', lineHeight: 1 }}>
                      {plot.totalAreaAcres}
                    </span>
                    <span style={{ fontSize: 14, color: 'var(--text-muted, #9A7562)' }}>acres</span>
                  </div>
                </div>
              </div>

              {/* Technical Metadata Bar */}
              <div
                style={{
                  marginTop: 24,
                  padding: '14px 18px',
                  borderRadius: 14,
                  background: 'rgba(251, 244, 228, 0.7)',
                  border: '1px solid rgba(217, 225, 216, 0.7)',
                  display: 'flex',
                  gap: 24,
                  flexWrap: 'wrap',
                  fontSize: 12,
                  color: 'var(--text-muted, #9A7562)',
                  fontFamily: 'var(--font-mono, monospace)',
                }}
              >
                <div>
                  <span style={{ color: 'var(--text-secondary, #6B4E3D)', fontWeight: 600 }}>Plot UUID:</span> {plot.id}
                </div>
                <div>
                  <span style={{ color: 'var(--text-secondary, #6B4E3D)', fontWeight: 600 }}>Village ID:</span> {plot.villageId}
                </div>
                <div>
                  <span style={{ color: 'var(--text-secondary, #6B4E3D)', fontWeight: 600 }}>Version Token:</span> {plot.version}
                </div>
              </div>
            </motion.div>

            {/* Associated Cultivations Section */}
            <div style={{ marginTop: 8 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 14 }}>
                <div>
                  <h2 style={{ fontSize: 20, fontWeight: 700, color: 'var(--text-primary, #1A1408)', margin: 0 }}>
                    Cultivation Cycles on this Plot
                  </h2>
                  <p style={{ margin: '2px 0 0', fontSize: 13, color: 'var(--text-muted, #9A7562)' }}>
                    Alpha Business Rule: One primary crop per plot per cultivation cycle.
                  </p>
                </div>

                <Link
                  href={`/cultivations/new?plotId=${plot.id}`}
                  style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: 6,
                    padding: '8px 16px',
                    borderRadius: 10,
                    background: 'var(--canopy-700, #2F5233)',
                    color: '#FFFFFF',
                    fontSize: 13,
                    fontWeight: 600,
                    textDecoration: 'none',
                  }}
                >
                  <Plus size={15} />
                  <span>Register Cultivation</span>
                </Link>
              </div>

              {cultivations.length === 0 ? (
                <div
                  style={{
                    padding: '36px 20px',
                    borderRadius: 18,
                    background: 'rgba(255, 255, 255, 0.7)',
                    border: '1.5px dashed var(--border-soft, #D9E1D8)',
                    textAlign: 'center',
                  }}
                >
                  <Layers size={28} style={{ color: 'var(--canopy-600, #3D6B42)', margin: '0 auto 10px' }} />
                  <p style={{ margin: 0, fontWeight: 600, fontSize: 15, color: 'var(--text-primary, #1A1408)' }}>
                    No Cultivation Cycles Active or Recorded
                  </p>
                  <p style={{ margin: '4px 0 16px', fontSize: 13, color: 'var(--text-muted, #9A7562)' }}>
                    Attach a crop cycle to this parcel to track planting, harvest windows, and field advisories.
                  </p>
                  <Link
                    href={`/cultivations/new?plotId=${plot.id}`}
                    style={{
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: 6,
                      padding: '9px 16px',
                      borderRadius: 10,
                      background: 'var(--canopy-700, #2F5233)',
                      color: '#FFFFFF',
                      fontSize: 13,
                      fontWeight: 600,
                      textDecoration: 'none',
                    }}
                  >
                    <Plus size={15} />
                    <span>Register Crop Cycle</span>
                  </Link>
                </div>
              ) : (
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: 14 }}>
                  {cultivations.map((c) => (
                    <Link
                      key={c.id}
                      href={`/cultivations/${c.id}`}
                      style={{
                        padding: '18px 20px',
                        borderRadius: 16,
                        background: 'rgba(255, 255, 255, 0.88)',
                        border: '1px solid var(--border-soft, #D9E1D8)',
                        textDecoration: 'none',
                        color: 'inherit',
                        transition: 'transform 0.15s ease, box-shadow 0.15s ease',
                      }}
                    >
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 8 }}>
                        <span style={{ fontWeight: 700, fontSize: 16, color: 'var(--text-primary, #1A1408)' }}>
                          {c.cropName || 'Primary Crop'}
                        </span>
                        <span
                          style={{
                            padding: '3px 8px',
                            borderRadius: 12,
                            fontSize: 11,
                            fontWeight: 700,
                            background:
                              c.status === 'STARTED'
                                ? 'rgba(46, 125, 50, 0.15)'
                                : c.status === 'PLANNED'
                                  ? 'rgba(242, 200, 75, 0.2)'
                                  : 'rgba(122, 155, 92, 0.15)',
                            color:
                              c.status === 'STARTED'
                                ? '#2E7D32'
                                : c.status === 'PLANNED'
                                  ? '#8D6E14'
                                  : 'var(--canopy-700, #2F5233)',
                          }}
                        >
                          {c.status}
                        </span>
                      </div>

                      <p style={{ margin: 0, fontSize: 13, color: 'var(--text-muted, #9A7562)' }}>
                        Season: {c.seasonName || 'Current'} {c.cultivationYear} · {c.cultivatedAreaAcres} acres
                      </p>
                    </Link>
                  ))}
                </div>
              )}
            </div>
          </>
        )}

        {/* Edit Plot Modal */}
        <AnimatePresence>
          {showEditModal && (
            <div
              style={{
                position: 'fixed',
                inset: 0,
                zIndex: 50,
                background: 'rgba(26, 20, 8, 0.55)',
                backdropFilter: 'blur(4px)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                padding: 16,
              }}
            >
              <motion.div
                initial={{ opacity: 0, scale: 0.95 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0, scale: 0.95 }}
                style={{
                  width: '100%',
                  maxWidth: 480,
                  borderRadius: 20,
                  background: '#FFFFFF',
                  padding: 28,
                  boxShadow: '0 20px 40px rgba(0,0,0,0.2)',
                }}
              >
                <h3 style={{ fontSize: 18, fontWeight: 700, color: 'var(--text-primary, #1A1408)', margin: '0 0 4px' }}>
                  Edit Plot Holding
                </h3>
                <p style={{ fontSize: 12.5, color: 'var(--text-muted, #9A7562)', margin: '0 0 18px' }}>
                  Update plot label and total registered acreage. Note: Village location is immutable.
                </p>

                {editError && (
                  <div style={{ padding: '10px 12px', borderRadius: 8, background: 'rgba(183,28,28,0.08)', color: '#B71C1C', fontSize: 12.5, marginBottom: 14 }}>
                    {editError}
                  </div>
                )}

                <form onSubmit={handleEditSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
                  <div>
                    <label style={{ display: 'block', fontSize: 12.5, fontWeight: 600, marginBottom: 5 }}>
                      Plot Label
                    </label>
                    <input
                      type="text"
                      value={editLabel}
                      onChange={(e) => setEditLabel(e.target.value)}
                      style={modalInputStyle}
                    />
                  </div>

                  <div>
                    <label style={{ display: 'block', fontSize: 12.5, fontWeight: 600, marginBottom: 5 }}>
                      Total Area (Acres) *
                    </label>
                    <input
                      type="number"
                      step="0.01"
                      min="0.01"
                      value={editArea}
                      onChange={(e) => setEditArea(e.target.value)}
                      required
                      style={modalInputStyle}
                    />
                  </div>

                  <div style={{ display: 'flex', gap: 10, marginTop: 10 }}>
                    <button
                      type="submit"
                      disabled={editLoading}
                      style={{
                        flex: 1,
                        padding: '11px 16px',
                        borderRadius: 10,
                        background: 'var(--canopy-700, #2F5233)',
                        color: '#FFFFFF',
                        border: 'none',
                        fontSize: 13.5,
                        fontWeight: 600,
                        cursor: editLoading ? 'not-allowed' : 'pointer',
                      }}
                    >
                      {editLoading ? 'Saving…' : 'Save Changes'}
                    </button>
                    <button
                      type="button"
                      onClick={() => setShowEditModal(false)}
                      style={{
                        padding: '11px 16px',
                        borderRadius: 10,
                        background: 'transparent',
                        border: '1.5px solid var(--border-soft, #D9E1D8)',
                        color: 'var(--text-muted, #9A7562)',
                        fontSize: 13.5,
                        cursor: 'pointer',
                      }}
                    >
                      Cancel
                    </button>
                  </div>
                </form>
              </motion.div>
            </div>
          )}
        </AnimatePresence>
      </div>
    </AppLayout>
  )
}

const modalInputStyle: React.CSSProperties = {
  width: '100%',
  boxSizing: 'border-box',
  padding: '10px 12px',
  borderRadius: 8,
  border: '1.5px solid var(--border-soft, #D9E1D8)',
  fontSize: 13.5,
  outline: 'none',
}
