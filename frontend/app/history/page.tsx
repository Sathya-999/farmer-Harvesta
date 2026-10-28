'use client'

import React, { useState, useEffect } from 'react'
import { useSearchParams, useRouter } from 'next/navigation'
import Link from 'next/link'
import { motion } from 'framer-motion'
import { historyService, HistorySnapshotDto, HistoryStatusEventDto } from '@/lib/historyService'
import { cultivationService, CultivationResponseDto } from '@/lib/cultivationService'
import AppLayout from '@/components/ui/AppLayout'
import { History, Sprout, ArrowLeft, Clock, ShieldCheck, FileText, ChevronRight, AlertCircle } from 'lucide-react'

function HistoryContent() {
  const searchParams = useSearchParams()
  const router = useRouter()
  const initialCultivationId = searchParams?.get('cultivationId') || ''

  const [cultivations, setCultivations] = useState<CultivationResponseDto[]>([])
  const [selectedCultivationId, setSelectedCultivationId] = useState(initialCultivationId)
  const [snapshots, setSnapshots] = useState<HistorySnapshotDto[]>([])
  const [events, setEvents] = useState<HistoryStatusEventDto[]>([])

  const [loading, setLoading] = useState(true)
  const [historyLoading, setHistoryLoading] = useState(false)
  const [errorMsg, setErrorMsg] = useState<string | null>(null)

  // 1. Fetch cultivations for dropdown
  useEffect(() => {
    async function loadCultivations() {
      try {
        const res = await cultivationService.listCultivations({ page: 0, size: 50 })
        const items = res.content || []
        setCultivations(items)
        if (!selectedCultivationId && items.length > 0) {
          setSelectedCultivationId(items[0].id)
        }
      } catch (err: any) {
        console.error('Failed to load cultivations list:', err)
        setErrorMsg('Failed to load cultivations for audit inspection.')
      } finally {
        setLoading(false)
      }
    }
    loadCultivations()
  }, [])

  // 2. Fetch history for selected cultivation
  useEffect(() => {
    if (!selectedCultivationId) return

    async function loadHistory() {
      setHistoryLoading(true)
      setErrorMsg(null)
      try {
        const [snapRes, evtRes] = await Promise.allSettled([
          historyService.getSnapshots(selectedCultivationId, 0, 50),
          historyService.getEvents(selectedCultivationId),
        ])

        if (snapRes.status === 'fulfilled') {
          setSnapshots(snapRes.value.content || [])
        }
        if (evtRes.status === 'fulfilled') {
          setEvents(evtRes.value || [])
        }
      } catch (err: any) {
        console.error('Failed to load history data:', err)
        setErrorMsg('Failed to load history snapshots for this cultivation cycle.')
      } finally {
        setHistoryLoading(false)
      }
    }

    loadHistory()
  }, [selectedCultivationId])

  const selectedCultivation = cultivations.find((c) => c.id === selectedCultivationId)

  return (
    <AppLayout>
      <div style={{ maxWidth: 960, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 24 }}>
        {/* Header */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 12 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <div
              style={{
                width: 44,
                height: 44,
                borderRadius: 14,
                background: 'rgba(47, 82, 51, 0.12)',
                color: 'var(--canopy-700, #2F5233)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <History size={22} />
            </div>
            <div>
              <h1
                style={{
                  fontFamily: 'var(--font-serif)',
                  fontSize: 26,
                  fontWeight: 700,
                  color: 'var(--canopy-900, #163A24)',
                  margin: 0,
                }}
              >
                Immutable Cultivation Audit History
              </h1>
              <p style={{ margin: '2px 0 0', fontSize: 13, color: 'var(--text-muted, #7A6552)' }}>
                System-of-record version snapshots and lifecycle status audit trail.
              </p>
            </div>
          </div>

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
            <span>Cultivations</span>
          </Link>
        </div>

        {/* Cultivation Cycle Picker */}
        <div
          style={{
            borderRadius: 18,
            background: 'rgba(255, 255, 255, 0.94)',
            backdropFilter: 'blur(16px)',
            border: '1.5px solid var(--border-soft, #D9E1D8)',
            padding: '20px 24px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: 16,
            flexWrap: 'wrap',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, flex: 1, minWidth: 260 }}>
            <Sprout size={20} color="var(--canopy-700, #2F5233)" />
            <div style={{ flex: 1 }}>
              <label style={{ display: 'block', fontSize: 12, fontWeight: 700, color: 'var(--canopy-900, #163A24)', marginBottom: 4 }}>
                Select Cultivation Intention
              </label>
              <select
                value={selectedCultivationId}
                onChange={(e) => {
                  setSelectedCultivationId(e.target.value)
                  router.replace(`/history?cultivationId=${e.target.value}`)
                }}
                disabled={loading || cultivations.length === 0}
                style={{
                  width: '100%',
                  padding: '9px 12px',
                  borderRadius: 10,
                  border: '1.5px solid var(--border-soft, #D9E1D8)',
                  background: '#fff',
                  fontSize: 13,
                  color: 'var(--canopy-900, #163A24)',
                  outline: 'none',
                }}
              >
                {cultivations.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.cropName || 'Crop'} ({c.seasonName || 'Season'} {c.cultivationYear}) — {c.cultivatedAreaAcres} Acres [{c.status}]
                  </option>
                ))}
              </select>
            </div>
          </div>

          {selectedCultivation && (
            <Link
              href={`/cultivations/${selectedCultivation.id}`}
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: 6,
                padding: '9px 14px',
                borderRadius: 10,
                background: 'rgba(47, 82, 51, 0.08)',
                color: 'var(--canopy-800, #224229)',
                fontSize: 12,
                fontWeight: 600,
                textDecoration: 'none',
              }}
            >
              <span>View Cultivation Detail</span>
              <ChevronRight size={14} />
            </Link>
          )}
        </div>

        {errorMsg && (
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 10,
              padding: '12px 16px',
              borderRadius: 12,
              background: 'rgba(220, 38, 38, 0.08)',
              border: '1px solid rgba(220, 38, 38, 0.25)',
              color: '#991B1B',
              fontSize: 13,
            }}
          >
            <AlertCircle size={18} />
            <div>{errorMsg}</div>
          </div>
        )}

        {/* Audit Sections */}
        {historyLoading ? (
          <div style={{ textAlign: 'center', padding: '60px 0', color: 'var(--text-muted, #7A6552)' }}>
            Retrieving ledger snapshots...
          </div>
        ) : !selectedCultivationId ? (
          <div style={{ textAlign: 'center', padding: '60px 0', color: 'var(--text-muted, #7A6552)' }}>
            No cultivation cycles available to inspect.
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
            {/* 1. Version Snapshots Ledger */}
            <div
              style={{
                borderRadius: 20,
                background: 'rgba(255, 255, 255, 0.94)',
                backdropFilter: 'blur(16px)',
                border: '1.5px solid var(--border-soft, #D9E1D8)',
                padding: 24,
                boxShadow: '0 8px 30px rgba(26, 20, 8, 0.04)',
                display: 'flex',
                flexDirection: 'column',
                gap: 16,
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <ShieldCheck size={20} color="var(--canopy-700, #2F5233)" />
                <h3
                  style={{
                    fontFamily: 'var(--font-serif)',
                    fontSize: 18,
                    fontWeight: 700,
                    color: 'var(--canopy-900, #163A24)',
                    margin: 0,
                  }}
                >
                  Version Snapshots (Immutable Ledger)
                </h3>
              </div>
              <p style={{ margin: 0, fontSize: 13, color: 'var(--text-muted, #7A6552)' }}>
                Every mutation triggers an append-only snapshot recording state changes, version numbers, and audit metadata.
              </p>

              {snapshots.length === 0 ? (
                <div style={{ padding: '24px 0', textAlign: 'center', color: 'var(--text-muted, #7A6552)', fontSize: 13 }}>
                  No historical mutation snapshots found for this cycle.
                </div>
              ) : (
                <div style={{ overflowX: 'auto' }}>
                  <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13, textAlign: 'left' }}>
                    <thead>
                      <tr style={{ borderBottom: '2px solid var(--border-soft, #D9E1D8)', color: 'var(--canopy-900, #163A24)' }}>
                        <th style={{ padding: '10px 12px', fontWeight: 700 }}>Version</th>
                        <th style={{ padding: '10px 12px', fontWeight: 700 }}>Status</th>
                        <th style={{ padding: '10px 12px', fontWeight: 700 }}>Area</th>
                        <th style={{ padding: '10px 12px', fontWeight: 700 }}>Planned Sowing</th>
                        <th style={{ padding: '10px 12px', fontWeight: 700 }}>Recorded At</th>
                        <th style={{ padding: '10px 12px', fontWeight: 700 }}>Notes</th>
                      </tr>
                    </thead>
                    <tbody>
                      {snapshots.map((s) => (
                        <tr
                          key={s.id || s.version}
                          style={{
                            borderBottom: '1px solid #F0EFEA',
                            background: s.version === selectedCultivation?.version ? 'rgba(47, 82, 51, 0.04)' : 'transparent',
                          }}
                        >
                          <td style={{ padding: '12px', fontWeight: 700, fontFamily: 'monospace' }}>
                            v{s.version}
                            {s.version === selectedCultivation?.version && (
                              <span
                                style={{
                                  fontSize: 10,
                                  marginLeft: 6,
                                  padding: '2px 6px',
                                  borderRadius: 4,
                                  background: 'rgba(47, 82, 51, 0.12)',
                                  color: 'var(--canopy-800, #224229)',
                                }}
                              >
                                Current
                              </span>
                            )}
                          </td>
                          <td style={{ padding: '12px' }}>
                            <span
                              style={{
                                fontSize: 11,
                                fontWeight: 700,
                                padding: '3px 8px',
                                borderRadius: 999,
                                background:
                                  s.status === 'STARTED'
                                    ? 'rgba(59, 130, 246, 0.12)'
                                    : s.status === 'COMPLETED'
                                    ? 'rgba(34, 197, 94, 0.12)'
                                    : 'rgba(242, 200, 75, 0.16)',
                                color:
                                  s.status === 'STARTED'
                                    ? '#1D4ED8'
                                    : s.status === 'COMPLETED'
                                    ? '#15803D'
                                    : '#9A6B08',
                              }}
                            >
                              {s.status}
                            </span>
                          </td>
                          <td style={{ padding: '12px', fontWeight: 600 }}>{s.cultivatedAreaAcres} Ac</td>
                          <td style={{ padding: '12px', color: 'var(--text-muted, #7A6552)' }}>
                            {s.plannedPlantingDate || '—'}
                          </td>
                          <td style={{ padding: '12px', color: 'var(--text-muted, #7A6552)', fontSize: 12 }}>
                            {new Date(s.recordedAt).toLocaleString()}
                          </td>
                          <td style={{ padding: '12px', color: 'var(--text-muted, #7A6552)', maxWidth: 200 }}>
                            {s.notes || '—'}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>

            {/* 2. Status Change Events Audit Trail */}
            <div
              style={{
                borderRadius: 20,
                background: 'rgba(255, 255, 255, 0.94)',
                backdropFilter: 'blur(16px)',
                border: '1.5px solid var(--border-soft, #D9E1D8)',
                padding: 24,
                boxShadow: '0 8px 30px rgba(26, 20, 8, 0.04)',
                display: 'flex',
                flexDirection: 'column',
                gap: 16,
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <Clock size={20} color="var(--canopy-700, #2F5233)" />
                <h3
                  style={{
                    fontFamily: 'var(--font-serif)',
                    fontSize: 18,
                    fontWeight: 700,
                    color: 'var(--canopy-900, #163A24)',
                    margin: 0,
                  }}
                >
                  Status Transition Trail
                </h3>
              </div>

              {events.length === 0 ? (
                <div style={{ padding: '20px 0', textAlign: 'center', color: 'var(--text-muted, #7A6552)', fontSize: 13 }}>
                  No lifecycle transitions recorded for this intention.
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
                        padding: '14px 18px',
                        borderRadius: 14,
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
                        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 8 }}>
                          <span style={{ fontWeight: 700, color: 'var(--canopy-900, #163A24)' }}>
                            {evt.fromStatus} → {evt.toStatus}
                          </span>
                          <span style={{ fontSize: 11, color: 'var(--text-muted, #7A6552)' }}>
                            {new Date(evt.changedAt).toLocaleString()}
                          </span>
                        </div>
                        {evt.notes && (
                          <div style={{ fontSize: 12, color: 'var(--text-muted, #7A6552)', marginTop: 4 }}>
                            Reason / Remark: {evt.notes}
                          </div>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </AppLayout>
  )
}

export default function HistoryPage() {
  return (
    <React.Suspense fallback={<div style={{ textAlign: 'center', padding: '60px 0' }}>Loading ledger audit...</div>}>
      <HistoryContent />
    </React.Suspense>
  )
}
