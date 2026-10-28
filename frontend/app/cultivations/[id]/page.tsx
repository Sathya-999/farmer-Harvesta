'use client'

import React, { useState, useEffect } from 'react'
import { useParams, useRouter } from 'next/navigation'
import Link from 'next/link'
import { motion, AnimatePresence } from 'framer-motion'
import {
  cultivationService,
  CultivationResponseDto,
  CultivationStatusEventDto,
  CultivationStatus,
} from '@/lib/cultivationService'
import { plotService, PlotResponseDto } from '@/lib/plotService'
import AppLayout from '@/components/ui/AppLayout'
import {
  Sprout,
  ArrowLeft,
  Calendar,
  Layers,
  MapPin,
  Clock,
  CheckCircle2,
  AlertTriangle,
  XCircle,
  PlayCircle,
  History,
  Edit2,
  Info,
  ChevronRight,
  TrendingUp,
} from 'lucide-react'

export default function CultivationDetailPage() {
  const params = useParams()
  const router = useRouter()
  const id = typeof params?.id === 'string' ? params.id : Array.isArray(params?.id) ? params.id[0] : ''

  const [cultivation, setCultivation] = useState<CultivationResponseDto | null>(null)
  const [plot, setPlot] = useState<PlotResponseDto | null>(null)
  const [events, setEvents] = useState<CultivationStatusEventDto[]>([])
  const [loading, setLoading] = useState(true)
  const [errorMsg, setErrorMsg] = useState<string | null>(null)

  // Status Transition Modal State
  const [transitionTarget, setTransitionTarget] = useState<CultivationStatus | null>(null)
  const [transitionDate, setTransitionDate] = useState('')
  const [transitionReason, setTransitionReason] = useState('')
  const [transitionLoading, setTransitionLoading] = useState(false)
  const [transitionError, setTransitionError] = useState<string | null>(null)

  // Edit Planned Cycle Modal State
  const [showEditModal, setShowEditModal] = useState(false)
  const [editArea, setEditArea] = useState('')
  const [editPlannedDate, setEditPlannedDate] = useState('')
  const [editExpectedDate, setEditExpectedDate] = useState('')
  const [editNotes, setEditNotes] = useState('')
  const [editLoading, setEditLoading] = useState(false)
  const [editError, setEditError] = useState<string | null>(null)

  async function loadData() {
    if (!id) return
    setLoading(true)
    setErrorMsg(null)
    try {
      const data = await cultivationService.getCultivation(id)
      setCultivation(data)

      // Fetch plot details in parallel with events
      const [plotData, eventsData] = await Promise.allSettled([
        plotService.getPlot(data.plotId),
        cultivationService.getEvents(id),
      ])

      if (plotData.status === 'fulfilled') setPlot(plotData.value)
      if (eventsData.status === 'fulfilled') setEvents(eventsData.value || [])
    } catch (err: any) {
      console.error('Failed to load cultivation details:', err)
      setErrorMsg(err.message || 'Failed to load cultivation cycle. It may not exist or access is restricted.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [id])

  const openStatusModal = (target: CultivationStatus) => {
    setTransitionTarget(target)
    setTransitionDate(new Date().toISOString().split('T')[0])
    setTransitionReason('')
    setTransitionError(null)
  }

  const handleStatusSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!cultivation || !transitionTarget) return
    setTransitionLoading(true)
    setTransitionError(null)

    try {
      await cultivationService.updateStatus(cultivation.id, {
        targetStatus: transitionTarget,
        actualDate: transitionDate || undefined,
        reason: transitionReason.trim() || undefined,
      })

      setTransitionTarget(null)
      await loadData()
    } catch (err: any) {
      console.error('Failed to transition cultivation status:', err)
      setTransitionError(err.message || 'Transition failed. Please verify rules and required fields.')
    } finally {
      setTransitionLoading(false)
    }
  }

  const openEditModal = () => {
    if (!cultivation) return
    setEditArea(String(cultivation.cultivatedAreaAcres))
    setEditPlannedDate(cultivation.plannedPlantingDate || '')
    setEditExpectedDate(cultivation.expectedHarvestDate || '')
    setEditNotes(cultivation.notes || '')
    setEditError(null)
    setShowEditModal(true)
  }

  const handleEditSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!cultivation) return
    setEditLoading(true)
    setEditError(null)

    const areaNum = parseFloat(editArea)
    if (isNaN(areaNum) || areaNum <= 0) {
      setEditError('Cultivated area must be greater than 0.')
      setEditLoading(false)
      return
    }

    if (plot && areaNum > plot.totalAreaAcres) {
      setEditError(`Area cannot exceed plot capacity (${plot.totalAreaAcres} acres).`)
      setEditLoading(false)
      return
    }

    try {
      await cultivationService.updateCultivation(cultivation.id, {
        cultivatedAreaAcres: areaNum,
        plannedPlantingDate: editPlannedDate || undefined,
        expectedHarvestDate: editExpectedDate || undefined,
        notes: editNotes.trim() || undefined,
        expectedVersion: cultivation.version,
      })

      setShowEditModal(false)
      await loadData()
    } catch (err: any) {
      console.error('Failed to update cultivation:', err)
      setEditError(err.message || 'Failed to update cultivation cycle. Concurrency version mismatch ممکن.')
    } finally {
      setEditLoading(false)
    }
  }

  const getStatusBadge = (status: CultivationStatus) => {
    switch (status) {
      case 'PLANNED':
        return {
          bg: 'rgba(242, 200, 75, 0.16)',
          text: '#9A6B08',
          border: 'rgba(242, 200, 75, 0.4)',
          icon: <Clock size={14} />,
          label: 'Planned Intention',
        }
      case 'STARTED':
        return {
          bg: 'rgba(59, 130, 246, 0.12)',
          text: '#1D4ED8',
          border: 'rgba(59, 130, 246, 0.3)',
          icon: <PlayCircle size={14} />,
          label: 'In Growth Cycle',
        }
      case 'COMPLETED':
        return {
          bg: 'rgba(34, 197, 94, 0.12)',
          text: '#15803D',
          border: 'rgba(34, 197, 94, 0.3)',
          icon: <CheckCircle2 size={14} />,
          label: 'Harvest Completed',
        }
      case 'CANCELLED':
        return {
          bg: 'rgba(107, 114, 128, 0.12)',
          text: '#4B5563',
          border: 'rgba(107, 114, 128, 0.3)',
          icon: <XCircle size={14} />,
          label: 'Cancelled Cycle',
        }
      case 'ABANDONED':
        return {
          bg: 'rgba(220, 38, 38, 0.12)',
          text: '#B91C1C',
          border: 'rgba(220, 38, 38, 0.3)',
          icon: <AlertTriangle size={14} />,
          label: 'Crop Abandoned',
        }
    }
  }

  return (
    <AppLayout>
      <div style={{ maxWidth: 900, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 24 }}>
        {/* Top Navigation */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 12 }}>
          <Link
            href="/cultivations"
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
            <span>Back to Cultivations</span>
          </Link>

          {cultivation && (
            <Link
              href={`/history?cultivationId=${cultivation.id}`}
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: 6,
                padding: '8px 14px',
                borderRadius: 10,
                background: 'rgba(47, 82, 51, 0.08)',
                color: 'var(--canopy-800, #224229)',
                fontSize: 12,
                fontWeight: 600,
                textDecoration: 'none',
              }}
            >
              <History size={15} />
              <span>Version Snapshot History</span>
            </Link>
          )}
        </div>

        {/* Error Alert */}
        {errorMsg && (
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 10,
              padding: '16px 20px',
              borderRadius: 16,
              background: 'rgba(220, 38, 38, 0.08)',
              border: '1px solid rgba(220, 38, 38, 0.25)',
              color: '#991B1B',
              fontSize: 14,
            }}
          >
            <AlertTriangle size={20} />
            <div>{errorMsg}</div>
          </div>
        )}

        {loading ? (
          <div style={{ textAlign: 'center', padding: '80px 0', color: 'var(--text-muted, #7A6552)' }}>
            Retrieving cultivation cycle intelligence...
          </div>
        ) : !cultivation ? (
          <div style={{ textAlign: 'center', padding: '60px 0' }}>Cultivation cycle record not found.</div>
        ) : (
          <>
            {/* Header Card */}
            <motion.div
              initial={{ opacity: 0, y: 14 }}
              animate={{ opacity: 1, y: 0 }}
              style={{
                borderRadius: 24,
                background: 'rgba(255, 255, 255, 0.94)',
                backdropFilter: 'blur(16px)',
                border: '1.5px solid var(--border-soft, #D9E1D8)',
                padding: '28px clamp(18px, 4vw, 36px)',
                boxShadow: '0 8px 30px rgba(26, 20, 8, 0.06)',
                display: 'flex',
                flexDirection: 'column',
                gap: 20,
              }}
            >
              <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 16, flexWrap: 'wrap' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
                  <div
                    style={{
                      width: 52,
                      height: 52,
                      borderRadius: 16,
                      background: 'rgba(47, 82, 51, 0.12)',
                      color: 'var(--canopy-700, #2F5233)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                    }}
                  >
                    <Sprout size={28} />
                  </div>
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
                      <h1
                        style={{
                          fontFamily: 'var(--font-serif)',
                          fontSize: 26,
                          fontWeight: 700,
                          color: 'var(--canopy-900, #163A24)',
                          margin: 0,
                        }}
                      >
                        {cultivation.cropName || 'Field Crop'}
                      </h1>
                      {cultivation.varietyName && (
                        <span
                          style={{
                            fontSize: 13,
                            fontWeight: 600,
                            padding: '3px 10px',
                            borderRadius: 8,
                            background: 'var(--dawn-200, #F3EBD9)',
                            color: 'var(--canopy-800, #224229)',
                          }}
                        >
                          {cultivation.varietyName}
                        </span>
                      )}
                    </div>
                    <p style={{ margin: '4px 0 0', fontSize: 13, color: 'var(--text-muted, #7A6552)' }}>
                      {cultivation.seasonName || 'Season'} {cultivation.cultivationYear} • Registered{' '}
                      {new Date(cultivation.createdAt).toLocaleDateString()}
                    </p>
                  </div>
                </div>

                {/* Status Badge & Actions */}
                <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
                  {(() => {
                    const badge = getStatusBadge(cultivation.status)
                    return (
                      <span
                        style={{
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: 6,
                          padding: '6px 14px',
                          borderRadius: 999,
                          fontSize: 12,
                          fontWeight: 700,
                          background: badge.bg,
                          color: badge.text,
                          border: `1px solid ${badge.border}`,
                        }}
                      >
                        {badge.icon}
                        <span>{badge.label}</span>
                      </span>
                    )
                  })()}

                  {cultivation.status === 'PLANNED' && (
                    <button
                      onClick={openEditModal}
                      style={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: 6,
                        padding: '8px 14px',
                        borderRadius: 10,
                        border: '1.5px solid var(--border-soft, #D9E1D8)',
                        background: '#fff',
                        color: 'var(--canopy-900, #163A24)',
                        fontSize: 12,
                        fontWeight: 600,
                        cursor: 'pointer',
                      }}
                    >
                      <Edit2 size={13} />
                      <span>Edit Plan</span>
                    </button>
                  )}
                </div>
              </div>

              {/* Lifecycle Stage Bar */}
              <div
                style={{
                  padding: '18px 22px',
                  borderRadius: 16,
                  background: 'rgba(247, 246, 240, 0.7)',
                  border: '1px solid var(--border-soft, #D9E1D8)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  gap: 12,
                  flexWrap: 'wrap',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                  <div
                    style={{
                      width: 28,
                      height: 28,
                      borderRadius: '50%',
                      background: 'var(--canopy-800, #224229)',
                      color: '#fff',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontSize: 12,
                      fontWeight: 700,
                    }}
                  >
                    1
                  </div>
                  <div>
                    <div style={{ fontSize: 13, fontWeight: 700, color: 'var(--canopy-900, #163A24)' }}>Planned</div>
                    <div style={{ fontSize: 11, color: 'var(--text-muted, #7A6552)' }}>
                      {cultivation.plannedPlantingDate
                        ? `Target: ${cultivation.plannedPlantingDate}`
                        : 'Sowing intention'}
                    </div>
                  </div>
                </div>

                <ChevronRight size={18} color="var(--text-muted, #9A7562)" />

                <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                  <div
                    style={{
                      width: 28,
                      height: 28,
                      borderRadius: '50%',
                      background:
                        cultivation.status === 'STARTED' || cultivation.status === 'COMPLETED'
                          ? 'var(--canopy-800, #224229)'
                          : 'var(--border-soft, #D9E1D8)',
                      color: cultivation.status === 'PLANNED' ? 'var(--text-muted, #7A6552)' : '#fff',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontSize: 12,
                      fontWeight: 700,
                    }}
                  >
                    2
                  </div>
                  <div>
                    <div style={{ fontSize: 13, fontWeight: 700, color: 'var(--canopy-900, #163A24)' }}>
                      In Growth / Started
                    </div>
                    <div style={{ fontSize: 11, color: 'var(--text-muted, #7A6552)' }}>
                      {cultivation.actualPlantingDate
                        ? `Sown on ${cultivation.actualPlantingDate}`
                        : 'Active vegetative cycle'}
                    </div>
                  </div>
                </div>

                <ChevronRight size={18} color="var(--text-muted, #9A7562)" />

                <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                  <div
                    style={{
                      width: 28,
                      height: 28,
                      borderRadius: '50%',
                      background:
                        cultivation.status === 'COMPLETED'
                          ? '#15803D'
                          : cultivation.status === 'CANCELLED' || cultivation.status === 'ABANDONED'
                          ? '#991B1B'
                          : 'var(--border-soft, #D9E1D8)',
                      color:
                        cultivation.status === 'COMPLETED' ||
                        cultivation.status === 'CANCELLED' ||
                        cultivation.status === 'ABANDONED'
                          ? '#fff'
                          : 'var(--text-muted, #7A6552)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontSize: 12,
                      fontWeight: 700,
                    }}
                  >
                    3
                  </div>
                  <div>
                    <div style={{ fontSize: 13, fontWeight: 700, color: 'var(--canopy-900, #163A24)' }}>
                      {cultivation.status === 'CANCELLED'
                        ? 'Cancelled'
                        : cultivation.status === 'ABANDONED'
                        ? 'Abandoned'
                        : 'Harvested'}
                    </div>
                    <div style={{ fontSize: 11, color: 'var(--text-muted, #7A6552)' }}>
                      {cultivation.actualHarvestDate
                        ? `Harvested on ${cultivation.actualHarvestDate}`
                        : 'Terminal cycle phase'}
                    </div>
                  </div>
                </div>
              </div>

              {/* Lifecycle Controller Transition Buttons */}
              {cultivation.status === 'PLANNED' && (
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    gap: 12,
                    padding: '16px 20px',
                    borderRadius: 14,
                    background: 'rgba(59, 130, 246, 0.05)',
                    border: '1px solid rgba(59, 130, 246, 0.2)',
                    flexWrap: 'wrap',
                  }}
                >
                  <div style={{ fontSize: 13, color: 'var(--canopy-900, #163A24)' }}>
                    <strong>Next Action:</strong> Has sowing commenced on this holding?
                  </div>
                  <div style={{ display: 'flex', gap: 10 }}>
                    <button
                      onClick={() => openStatusModal('CANCELLED')}
                      style={{
                        padding: '8px 16px',
                        borderRadius: 10,
                        border: '1px solid rgba(107, 114, 128, 0.3)',
                        background: '#fff',
                        color: '#4B5563',
                        fontSize: 13,
                        fontWeight: 600,
                        cursor: 'pointer',
                      }}
                    >
                      Cancel Intention
                    </button>
                    <button
                      onClick={() => openStatusModal('STARTED')}
                      style={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: 6,
                        padding: '8px 18px',
                        borderRadius: 10,
                        border: 'none',
                        background: 'var(--canopy-800, #224229)',
                        color: '#fff',
                        fontSize: 13,
                        fontWeight: 600,
                        cursor: 'pointer',
                      }}
                    >
                      <PlayCircle size={15} />
                      <span>Start Cultivation</span>
                    </button>
                  </div>
                </div>
              )}

              {cultivation.status === 'STARTED' && (
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    gap: 12,
                    padding: '16px 20px',
                    borderRadius: 14,
                    background: 'rgba(34, 197, 94, 0.05)',
                    border: '1px solid rgba(34, 197, 94, 0.2)',
                    flexWrap: 'wrap',
                  }}
                >
                  <div style={{ fontSize: 13, color: 'var(--canopy-900, #163A24)' }}>
                    <strong>Next Action:</strong> Record crop outcome when cycle concludes.
                  </div>
                  <div style={{ display: 'flex', gap: 10 }}>
                    <button
                      onClick={() => openStatusModal('ABANDONED')}
                      style={{
                        padding: '8px 16px',
                        borderRadius: 10,
                        border: '1px solid rgba(220, 38, 38, 0.3)',
                        background: '#fff',
                        color: '#DC2626',
                        fontSize: 13,
                        fontWeight: 600,
                        cursor: 'pointer',
                      }}
                    >
                      Report Crop Loss
                    </button>
                    <button
                      onClick={() => openStatusModal('COMPLETED')}
                      style={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: 6,
                        padding: '8px 18px',
                        borderRadius: 10,
                        border: 'none',
                        background: '#15803D',
                        color: '#fff',
                        fontSize: 13,
                        fontWeight: 600,
                        cursor: 'pointer',
                      }}
                    >
                      <CheckCircle2 size={15} />
                      <span>Mark Harvest Completed</span>
                    </button>
                  </div>
                </div>
              )}
            </motion.div>

            {/* Two-Column Grid: Details & Plot Context */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: 20 }}>
              {/* Holding & Agricultural Specifications */}
              <div
                style={{
                  borderRadius: 20,
                  background: 'rgba(255, 255, 255, 0.94)',
                  backdropFilter: 'blur(16px)',
                  border: '1.5px solid var(--border-soft, #D9E1D8)',
                  padding: 24,
                  display: 'flex',
                  flexDirection: 'column',
                  gap: 18,
                }}
              >
                <h3
                  style={{
                    fontFamily: 'var(--font-serif)',
                    fontSize: 17,
                    fontWeight: 700,
                    color: 'var(--canopy-900, #163A24)',
                    margin: 0,
                  }}
                >
                  Agricultural Cycle Details
                </h3>

                <div style={{ display: 'flex', flexDirection: 'column', gap: 12, fontSize: 13 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid #F0EFEA', paddingBottom: 8 }}>
                    <span style={{ color: 'var(--text-muted, #7A6552)' }}>Cultivated Area:</span>
                    <strong style={{ color: 'var(--canopy-900, #163A24)' }}>{cultivation.cultivatedAreaAcres} Acres</strong>
                  </div>

                  <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid #F0EFEA', paddingBottom: 8 }}>
                    <span style={{ color: 'var(--text-muted, #7A6552)' }}>Season & Year:</span>
                    <strong style={{ color: 'var(--canopy-900, #163A24)' }}>
                      {cultivation.seasonName || 'Season'} {cultivation.cultivationYear}
                    </strong>
                  </div>

                  <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid #F0EFEA', paddingBottom: 8 }}>
                    <span style={{ color: 'var(--text-muted, #7A6552)' }}>Planned Sowing Date:</span>
                    <strong style={{ color: 'var(--canopy-900, #163A24)' }}>
                      {cultivation.plannedPlantingDate || 'Not specified'}
                    </strong>
                  </div>

                  <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid #F0EFEA', paddingBottom: 8 }}>
                    <span style={{ color: 'var(--text-muted, #7A6552)' }}>Actual Sowing Date:</span>
                    <strong style={{ color: 'var(--canopy-900, #163A24)' }}>
                      {cultivation.actualPlantingDate || '—'}
                    </strong>
                  </div>

                  <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid #F0EFEA', paddingBottom: 8 }}>
                    <span style={{ color: 'var(--text-muted, #7A6552)' }}>Expected Harvest Date:</span>
                    <strong style={{ color: 'var(--canopy-900, #163A24)' }}>
                      {cultivation.expectedHarvestDate || 'Not specified'}
                    </strong>
                  </div>

                  <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid #F0EFEA', paddingBottom: 8 }}>
                    <span style={{ color: 'var(--text-muted, #7A6552)' }}>Actual Harvest Date:</span>
                    <strong style={{ color: 'var(--canopy-900, #163A24)' }}>
                      {cultivation.actualHarvestDate || '—'}
                    </strong>
                  </div>

                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <span style={{ color: 'var(--text-muted, #7A6552)' }}>Concurrency Version:</span>
                    <span style={{ fontFamily: 'monospace', color: 'var(--text-muted, #7A6552)' }}>v{cultivation.version}</span>
                  </div>
                </div>

                {cultivation.notes && (
                  <div
                    style={{
                      padding: 12,
                      borderRadius: 10,
                      background: 'rgba(247, 246, 240, 0.8)',
                      fontSize: 12,
                      color: 'var(--canopy-900, #163A24)',
                      lineHeight: 1.5,
                    }}
                  >
                    <strong>Notes:</strong> {cultivation.notes}
                  </div>
                )}
              </div>

              {/* Linked Plot Holding Card */}
              <div
                style={{
                  borderRadius: 20,
                  background: 'rgba(255, 255, 255, 0.94)',
                  backdropFilter: 'blur(16px)',
                  border: '1.5px solid var(--border-soft, #D9E1D8)',
                  padding: 24,
                  display: 'flex',
                  flexDirection: 'column',
                  gap: 16,
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <h3
                    style={{
                      fontFamily: 'var(--font-serif)',
                      fontSize: 17,
                      fontWeight: 700,
                      color: 'var(--canopy-900, #163A24)',
                      margin: 0,
                    }}
                  >
                    Target Plot Holding
                  </h3>
                  <Link
                    href={`/plots/${cultivation.plotId}`}
                    style={{
                      fontSize: 12,
                      fontWeight: 600,
                      color: 'var(--canopy-700, #2F5233)',
                      textDecoration: 'none',
                    }}
                  >
                    View Plot Holding →
                  </Link>
                </div>

                {plot ? (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 12, fontSize: 13 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                      <div
                        style={{
                          width: 36,
                          height: 36,
                          borderRadius: 10,
                          background: 'rgba(47, 82, 51, 0.1)',
                          color: 'var(--canopy-700, #2F5233)',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                        }}
                      >
                        <Layers size={18} />
                      </div>
                      <div>
                        <div style={{ fontWeight: 700, color: 'var(--canopy-900, #163A24)' }}>
                          {plot.label || `Plot ${plot.id.slice(0, 8)}`}
                        </div>
                        <div style={{ fontSize: 12, color: 'var(--text-muted, #7A6552)' }}>
                          Total Capacity: {plot.totalAreaAcres} Acres
                        </div>
                      </div>
                    </div>

                    <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: 'var(--text-muted, #7A6552)' }}>
                      <MapPin size={16} />
                      <span>
                        {plot.villageName || 'Village'}, {plot.mandalName || 'Mandal'}, {plot.districtName || 'District'}
                      </span>
                    </div>

                    <div
                      style={{
                        padding: 12,
                        borderRadius: 10,
                        background: 'rgba(47, 82, 51, 0.04)',
                        border: '1px solid rgba(47, 82, 51, 0.1)',
                        fontSize: 12,
                        color: 'var(--canopy-800, #224229)',
                      }}
                    >
                      This cultivation cycle occupies{' '}
                      <strong>
                        {((cultivation.cultivatedAreaAcres / plot.totalAreaAcres) * 100).toFixed(1)}%
                      </strong>{' '}
                      of the physical plot area.
                    </div>
                  </div>
                ) : (
                  <div style={{ fontSize: 13, color: 'var(--text-muted, #7A6552)' }}>
                    Plot ID: <code style={{ fontSize: 12 }}>{cultivation.plotId}</code>
                  </div>
                )}
              </div>
            </div>

            {/* Lifecycle Status Change Audit Trail */}
            <div
              style={{
                borderRadius: 20,
                background: 'rgba(255, 255, 255, 0.94)',
                backdropFilter: 'blur(16px)',
                border: '1.5px solid var(--border-soft, #D9E1D8)',
                padding: 24,
                display: 'flex',
                flexDirection: 'column',
                gap: 16,
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <Clock size={18} color="var(--canopy-700, #2F5233)" />
                <h3
                  style={{
                    fontFamily: 'var(--font-serif)',
                    fontSize: 17,
                    fontWeight: 700,
                    color: 'var(--canopy-900, #163A24)',
                    margin: 0,
                  }}
                >
                  Status Transition Audit History
                </h3>
              </div>

              {events.length === 0 ? (
                <div style={{ fontSize: 13, color: 'var(--text-muted, #7A6552)', padding: '10px 0' }}>
                  No historical status transitions recorded beyond creation.
                </div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                  {events.map((evt, idx) => (
                    <div
                      key={evt.id || idx}
                      style={{
                        display: 'flex',
                        alignItems: 'flex-start',
                        gap: 14,
                        padding: '12px 16px',
                        borderRadius: 12,
                        background: '#FAF8F5',
                        border: '1px solid var(--border-soft, #D9E1D8)',
                        fontSize: 13,
                      }}
                    >
                      <div
                        style={{
                          width: 8,
                          height: 8,
                          borderRadius: '50%',
                          background: 'var(--canopy-600, #406844)',
                          marginTop: 6,
                          flexShrink: 0,
                        }}
                      />
                      <div style={{ flex: 1 }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
                          <span style={{ fontWeight: 700, color: 'var(--canopy-900, #163A24)' }}>
                            {evt.fromStatus} → {evt.toStatus}
                          </span>
                          <span style={{ fontSize: 11, color: 'var(--text-muted, #7A6552)' }}>
                            {new Date(evt.changedAt).toLocaleString()}
                          </span>
                        </div>
                        {evt.notes && (
                          <div style={{ fontSize: 12, color: 'var(--text-muted, #7A6552)', marginTop: 4 }}>
                            {evt.notes}
                          </div>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </>
        )}

        {/* Transition Status Modal */}
        <AnimatePresence>
          {transitionTarget && (
            <div
              style={{
                position: 'fixed',
                inset: 0,
                background: 'rgba(22, 58, 36, 0.45)',
                backdropFilter: 'blur(4px)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                zIndex: 999,
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
                  background: '#fff',
                  border: '1.5px solid var(--border-soft, #D9E1D8)',
                  padding: 28,
                  boxShadow: '0 20px 40px rgba(0,0,0,0.18)',
                }}
              >
                <h3
                  style={{
                    fontFamily: 'var(--font-serif)',
                    fontSize: 20,
                    fontWeight: 700,
                    color: 'var(--canopy-900, #163A24)',
                    margin: '0 0 8px',
                  }}
                >
                  Transition Cycle to {transitionTarget}
                </h3>
                <p style={{ margin: '0 0 20px', fontSize: 13, color: 'var(--text-muted, #7A6552)' }}>
                  {transitionTarget === 'STARTED' && 'Confirm that field sowing has commenced on this holding.'}
                  {transitionTarget === 'COMPLETED' && 'Record final crop harvest conclusion.'}
                  {transitionTarget === 'CANCELLED' && 'Cancel this planned crop intention before sowing.'}
                  {transitionTarget === 'ABANDONED' && 'Record crop loss or early termination.'}
                </p>

                {transitionError && (
                  <div
                    style={{
                      padding: '10px 14px',
                      borderRadius: 10,
                      background: 'rgba(220, 38, 38, 0.08)',
                      color: '#991B1B',
                      fontSize: 13,
                      marginBottom: 16,
                    }}
                  >
                    {transitionError}
                  </div>
                )}

                <form onSubmit={handleStatusSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                  {(transitionTarget === 'STARTED' || transitionTarget === 'COMPLETED') && (
                    <div>
                      <label
                        style={{
                          display: 'block',
                          fontSize: 13,
                          fontWeight: 600,
                          color: 'var(--canopy-900, #163A24)',
                          marginBottom: 6,
                        }}
                      >
                        {transitionTarget === 'STARTED' ? 'Actual Sowing Date' : 'Actual Harvest Date'}
                      </label>
                      <input
                        type="date"
                        value={transitionDate}
                        onChange={(e) => setTransitionDate(e.target.value)}
                        required
                        style={{
                          width: '100%',
                          padding: '10px 14px',
                          borderRadius: 10,
                          border: '1.5px solid var(--border-soft, #D9E1D8)',
                          fontSize: 14,
                          boxSizing: 'border-box',
                        }}
                      />
                    </div>
                  )}

                  <div>
                    <label
                      style={{
                        display: 'block',
                        fontSize: 13,
                        fontWeight: 600,
                        color: 'var(--canopy-900, #163A24)',
                        marginBottom: 6,
                      }}
                    >
                      Reason / Notes (Optional)
                    </label>
                    <textarea
                      rows={3}
                      value={transitionReason}
                      onChange={(e) => setTransitionReason(e.target.value)}
                      placeholder={
                        transitionTarget === 'ABANDONED'
                          ? 'Specify reason (e.g., erratic rainfall, pest outbreak)...'
                          : 'Operational remarks...'
                      }
                      style={{
                        width: '100%',
                        padding: '10px 14px',
                        borderRadius: 10,
                        border: '1.5px solid var(--border-soft, #D9E1D8)',
                        fontSize: 14,
                        boxSizing: 'border-box',
                        resize: 'vertical',
                      }}
                    />
                  </div>

                  <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 8 }}>
                    <button
                      type="button"
                      onClick={() => setTransitionTarget(null)}
                      style={{
                        padding: '10px 18px',
                        borderRadius: 10,
                        border: '1px solid var(--border-soft, #D9E1D8)',
                        background: '#fff',
                        fontSize: 13,
                        fontWeight: 600,
                        color: 'var(--text-muted, #7A6552)',
                        cursor: 'pointer',
                      }}
                    >
                      Cancel
                    </button>
                    <button
                      type="submit"
                      disabled={transitionLoading}
                      style={{
                        padding: '10px 20px',
                        borderRadius: 10,
                        border: 'none',
                        background: 'var(--canopy-800, #224229)',
                        color: '#fff',
                        fontSize: 13,
                        fontWeight: 600,
                        cursor: transitionLoading ? 'not-allowed' : 'pointer',
                      }}
                    >
                      {transitionLoading ? 'Updating Status...' : 'Confirm Transition'}
                    </button>
                  </div>
                </form>
              </motion.div>
            </div>
          )}
        </AnimatePresence>

        {/* Edit Plan Modal */}
        <AnimatePresence>
          {showEditModal && cultivation && (
            <div
              style={{
                position: 'fixed',
                inset: 0,
                background: 'rgba(22, 58, 36, 0.45)',
                backdropFilter: 'blur(4px)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                zIndex: 999,
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
                  background: '#fff',
                  border: '1.5px solid var(--border-soft, #D9E1D8)',
                  padding: 28,
                  boxShadow: '0 20px 40px rgba(0,0,0,0.18)',
                }}
              >
                <h3
                  style={{
                    fontFamily: 'var(--font-serif)',
                    fontSize: 20,
                    fontWeight: 700,
                    color: 'var(--canopy-900, #163A24)',
                    margin: '0 0 8px',
                  }}
                >
                  Edit Planned Cultivation
                </h3>
                <p style={{ margin: '0 0 20px', fontSize: 13, color: 'var(--text-muted, #7A6552)' }}>
                  Update sowing area or target dates before the cycle begins.
                </p>

                {editError && (
                  <div
                    style={{
                      padding: '10px 14px',
                      borderRadius: 10,
                      background: 'rgba(220, 38, 38, 0.08)',
                      color: '#991B1B',
                      fontSize: 13,
                      marginBottom: 16,
                    }}
                  >
                    {editError}
                  </div>
                )}

                <form onSubmit={handleEditSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                  <div>
                    <label
                      style={{
                        display: 'block',
                        fontSize: 13,
                        fontWeight: 600,
                        color: 'var(--canopy-900, #163A24)',
                        marginBottom: 6,
                      }}
                    >
                      Cultivated Area (Acres)
                    </label>
                    <input
                      type="number"
                      step="0.01"
                      min="0.01"
                      value={editArea}
                      onChange={(e) => setEditArea(e.target.value)}
                      required
                      style={{
                        width: '100%',
                        padding: '10px 14px',
                        borderRadius: 10,
                        border: '1.5px solid var(--border-soft, #D9E1D8)',
                        fontSize: 14,
                        boxSizing: 'border-box',
                      }}
                    />
                  </div>

                  <div>
                    <label
                      style={{
                        display: 'block',
                        fontSize: 13,
                        fontWeight: 600,
                        color: 'var(--canopy-900, #163A24)',
                        marginBottom: 6,
                      }}
                    >
                      Planned Planting Date
                    </label>
                    <input
                      type="date"
                      value={editPlannedDate}
                      onChange={(e) => setEditPlannedDate(e.target.value)}
                      style={{
                        width: '100%',
                        padding: '10px 14px',
                        borderRadius: 10,
                        border: '1.5px solid var(--border-soft, #D9E1D8)',
                        fontSize: 14,
                        boxSizing: 'border-box',
                      }}
                    />
                  </div>

                  <div>
                    <label
                      style={{
                        display: 'block',
                        fontSize: 13,
                        fontWeight: 600,
                        color: 'var(--canopy-900, #163A24)',
                        marginBottom: 6,
                      }}
                    >
                      Expected Harvest Date
                    </label>
                    <input
                      type="date"
                      value={editExpectedDate}
                      onChange={(e) => setEditExpectedDate(e.target.value)}
                      style={{
                        width: '100%',
                        padding: '10px 14px',
                        borderRadius: 10,
                        border: '1.5px solid var(--border-soft, #D9E1D8)',
                        fontSize: 14,
                        boxSizing: 'border-box',
                      }}
                    />
                  </div>

                  <div>
                    <label
                      style={{
                        display: 'block',
                        fontSize: 13,
                        fontWeight: 600,
                        color: 'var(--canopy-900, #163A24)',
                        marginBottom: 6,
                      }}
                    >
                      Notes
                    </label>
                    <textarea
                      rows={2}
                      value={editNotes}
                      onChange={(e) => setEditNotes(e.target.value)}
                      style={{
                        width: '100%',
                        padding: '10px 14px',
                        borderRadius: 10,
                        border: '1.5px solid var(--border-soft, #D9E1D8)',
                        fontSize: 14,
                        boxSizing: 'border-box',
                        resize: 'vertical',
                      }}
                    />
                  </div>

                  <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 8 }}>
                    <button
                      type="button"
                      onClick={() => setShowEditModal(false)}
                      style={{
                        padding: '10px 18px',
                        borderRadius: 10,
                        border: '1px solid var(--border-soft, #D9E1D8)',
                        background: '#fff',
                        fontSize: 13,
                        fontWeight: 600,
                        color: 'var(--text-muted, #7A6552)',
                        cursor: 'pointer',
                      }}
                    >
                      Cancel
                    </button>
                    <button
                      type="submit"
                      disabled={editLoading}
                      style={{
                        padding: '10px 20px',
                        borderRadius: 10,
                        border: 'none',
                        background: 'var(--canopy-800, #224229)',
                        color: '#fff',
                        fontSize: 13,
                        fontWeight: 600,
                        cursor: editLoading ? 'not-allowed' : 'pointer',
                      }}
                    >
                      {editLoading ? 'Saving...' : 'Save Changes'}
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
