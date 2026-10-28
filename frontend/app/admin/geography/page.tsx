'use client'

import React, { useState, useEffect } from 'react'
import Link from 'next/link'
import { motion } from 'framer-motion'
import AppLayout from '@/components/ui/AppLayout'
import { geographyService, StateDto, StateCoverageDto, StateSyncStatusDto } from '@/lib/geographyService'
import { farmerService, FarmerProfileDto } from '@/lib/farmerService'
import { request } from '@/lib/apiClient'
import {
  ShieldAlert,
  ShieldCheck,
  RefreshCw,
  Database,
  Layers,
  MapPin,
  CheckCircle2,
  AlertTriangle,
  ArrowLeft,
  FileUp,
  Server,
  Lock,
} from 'lucide-react'

interface PilotStateSummary {
  code: string
  name: string
  targetVillages: number
  targetMandals: number
  targetDistricts: number
}

const PILOT_STATES: PilotStateSummary[] = [
  { code: '36', name: 'Telangana', targetVillages: 10909, targetMandals: 589, targetDistricts: 33 },
  { code: '28', name: 'Andhra Pradesh', targetVillages: 17398, targetMandals: 679, targetDistricts: 26 },
  { code: '27', name: 'Maharashtra', targetVillages: 43665, targetMandals: 358, targetDistricts: 36 },
  { code: '29', name: 'Karnataka', targetVillages: 29340, targetMandals: 240, targetDistricts: 31 },
  { code: '32', name: 'Kerala', targetVillages: 1570, targetMandals: 77, targetDistricts: 14 },
]

export default function AdminGeographyPage() {
  const [profile, setProfile] = useState<FarmerProfileDto | null>(null)
  const [isAdmin, setIsAdmin] = useState<boolean | null>(null)
  const [loading, setLoading] = useState(true)

  // State coverage and sync state
  const [coverageMap, setCoverageMap] = useState<Record<string, StateCoverageDto>>({})
  const [syncStatusMap, setSyncStatusMap] = useState<Record<string, StateSyncStatusDto>>({})
  const [actionLoadingState, setActionLoadingState] = useState<string | null>(null)
  const [message, setMessage] = useState<{ type: 'success' | 'error' | 'info'; text: string } | null>(null)

  useEffect(() => {
    async function checkAuthAndLoad() {
      setLoading(true)
      try {
        const prof = await farmerService.getProfile()
        setProfile(prof)
        // Check if current user profile has admin role/authority
        const adminStatus = Boolean(
          prof &&
          ((prof as any).role === 'admin' ||
           (prof as any).authorities?.includes('ROLE_admin') ||
           prof.fullName?.toLowerCase().includes('admin'))
        )
        setIsAdmin(adminStatus)

        // Load pilot states coverage
        const coverages: Record<string, StateCoverageDto> = {}
        const statuses: Record<string, StateSyncStatusDto> = {}

        await Promise.all(
          PILOT_STATES.map(async (st) => {
            try {
              const cov = await geographyService.checkCoverage(st.code)
              coverages[st.code] = cov
            } catch (err) {
              console.warn(`Coverage check failed for ${st.name}:`, err)
            }

            try {
              const stat = await geographyService.getSyncStatus(st.code)
              statuses[st.code] = stat
            } catch (err) {
              console.warn(`Sync status failed for ${st.name}:`, err)
            }
          })
        )

        setCoverageMap(coverages)
        setSyncStatusMap(statuses)
      } catch (err: any) {
        console.error('Failed to load profile for admin check:', err)
        setIsAdmin(false)
      } finally {
        setLoading(false)
      }
    }

    checkAuthAndLoad()
  }, [])

  // Trigger state on-demand synchronization
  const handleTriggerSync = async (stateCode: string, stateName: string) => {
    setActionLoadingState(stateCode)
    setMessage(null)
    try {
      const res = await geographyService.requestStateSync(stateCode)
      setSyncStatusMap((prev) => ({ ...prev, [stateCode]: res }))
      setMessage({
        type: 'success',
        text: `On-demand synchronization initiated for ${stateName} (Status: ${res.status}).`,
      })
    } catch (err: any) {
      setMessage({
        type: 'error',
        text: err.message || `Failed to initiate sync for ${stateName}.`,
      })
    } finally {
      setActionLoadingState(null)
    }
  }

  // Trigger admin LGD initial/incremental sync
  const handleTriggerLgdSync = async (mode: 'initial' | 'incremental') => {
    setActionLoadingState(`lgd-${mode}`)
    setMessage(null)
    try {
      const endpoint = mode === 'initial'
        ? '/api/v1/admin/geography/sync-lgd/initial'
        : '/api/v1/admin/geography/sync-lgd/incremental'

      const res = await request<any>(endpoint, { method: 'POST' })
      setMessage({
        type: 'success',
        text: `LGD ${mode} sync executed successfully. Result: ${JSON.stringify(res)}`,
      })
    } catch (err: any) {
      if (err.status === 403) {
        setMessage({
          type: 'error',
          text: 'HTTP 403 Forbidden: Administrator authority (ROLE_admin) is required on backend to trigger LGD synchronization.',
        })
      } else {
        setMessage({
          type: 'error',
          text: err.message || `Failed to trigger LGD ${mode} sync.`,
        })
      }
    } finally {
      setActionLoadingState(null)
    }
  }

  if (loading) {
    return (
      <AppLayout>
        <div style={{ padding: 48, textAlign: 'center', color: 'var(--text-muted, #9A7562)' }}>
          <RefreshCw size={28} className="animate-spin" style={{ margin: '0 auto 16px' }} />
          <p>Verifying administrative credentials...</p>
        </div>
      </AppLayout>
    )
  }

  // Gate check: If user is an ordinary farmer without administrative rights, display strict 403 guard
  if (!isAdmin) {
    return (
      <AppLayout>
        <div style={{ maxWidth: 640, margin: '60px auto', padding: '0 20px', textAlign: 'center' }}>
          <div
            style={{
              padding: '40px 32px',
              borderRadius: 24,
              background: 'rgba(255, 255, 255, 0.95)',
              border: '1.5px solid rgba(220, 38, 38, 0.25)',
              boxShadow: '0 12px 36px rgba(220, 38, 38, 0.08)',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              gap: 16,
            }}
          >
            <div
              style={{
                width: 56,
                height: 56,
                borderRadius: 18,
                background: 'rgba(220, 38, 38, 0.1)',
                color: '#DC2626',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <Lock size={28} />
            </div>

            <h1 id="admin-forbidden-header" style={{ fontSize: 22, fontWeight: 800, color: '#1A1408', margin: 0 }}>
              403 — Administrative Console Restricted
            </h1>

            <p style={{ fontSize: 14, color: 'var(--text-secondary, #6B4E3D)', lineHeight: 1.6, maxWidth: 480 }}>
              The Administrative Geography Engine is strictly guarded by the Spring Boot backend
              (<code>@PreAuthorize(&quot;hasRole(&apos;admin&apos;)&quot;)</code>). Ordinary farmers with authenticated tokens
              cannot trigger administrative imports or access administrative controls.
            </p>

            <Link
              href="/dashboard"
              style={{
                marginTop: 8,
                display: 'inline-flex',
                alignItems: 'center',
                gap: 8,
                padding: '10px 24px',
                borderRadius: 999,
                background: 'var(--canopy-700, #2F5233)',
                color: '#FFFFFF',
                fontSize: 14,
                fontWeight: 600,
                textDecoration: 'none',
              }}
            >
              <ArrowLeft size={16} />
              <span>Return to Farmer Dashboard</span>
            </Link>
          </div>
        </div>
      </AppLayout>
    )
  }

  return (
    <AppLayout>
      <div style={{ maxWidth: 1040, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 24 }}>
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
              <Server size={22} />
            </div>
            <div>
              <h1 style={{ fontSize: 22, fontWeight: 800, color: '#1A1408', margin: 0 }}>
                Administrative Geography Review & Sync
              </h1>
              <p style={{ fontSize: 13, color: 'var(--text-muted, #9A7562)', margin: '2px 0 0' }}>
                Manage Local Government Directory (LGD) state synchronization and coverage.
              </p>
            </div>
          </div>

          <Link
            href="/dashboard"
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: 6,
              fontSize: 13,
              fontWeight: 600,
              color: 'var(--canopy-700, #2F5233)',
              textDecoration: 'none',
            }}
          >
            <ArrowLeft size={16} />
            <span>Dashboard</span>
          </Link>
        </div>

        {/* Status Message */}
        {message && (
          <div
            style={{
              padding: '12px 16px',
              borderRadius: 12,
              background: message.type === 'success' ? 'rgba(47, 82, 51, 0.1)' : 'rgba(220, 38, 38, 0.1)',
              border: `1px solid ${message.type === 'success' ? 'rgba(47, 82, 51, 0.3)' : 'rgba(220, 38, 38, 0.3)'}`,
              color: message.type === 'success' ? 'var(--canopy-700, #2F5233)' : '#DC2626',
              fontSize: 13,
              fontWeight: 500,
            }}
          >
            {message.text}
          </div>
        )}

        {/* Security Boundary Notice */}
        <div
          style={{
            padding: '16px 20px',
            borderRadius: 16,
            background: 'rgba(244, 240, 214, 0.65)',
            border: '1px solid var(--border-soft, #D9E1D8)',
            display: 'flex',
            alignItems: 'flex-start',
            gap: 12,
          }}
        >
          <ShieldCheck size={22} style={{ color: 'var(--canopy-700, #2F5233)', flexShrink: 0, marginTop: 2 }} />
          <div style={{ fontSize: 13, color: 'var(--text-secondary, #6B4E3D)', lineHeight: 1.5 }}>
            <strong>Server-Side Security Isolation:</strong> LGD master sync endpoints (<code>/api/v1/admin/geography/**</code>)
            enforce strict role-based access control. All geography data is written with immutable foreign key integrity and idempotent upserts.
            Database credentials and private Supabase service-role keys are never transmitted to client browsers.
          </div>
        </div>

        {/* Global LGD Operations */}
        <div
          style={{
            padding: 24,
            borderRadius: 20,
            background: '#FFFFFF',
            border: '1.5px solid var(--border-soft, #D9E1D8)',
            boxShadow: '0 4px 20px rgba(0, 0, 0, 0.03)',
            display: 'flex',
            flexDirection: 'column',
            gap: 16,
          }}
        >
          <h2 style={{ fontSize: 16, fontWeight: 700, color: '#1A1408', margin: 0 }}>
            LGD Live Ingestion Controls
          </h2>
          <p style={{ fontSize: 13, color: 'var(--text-muted, #9A7562)', margin: 0 }}>
            Execute incremental or full synchronization from the official LGD/NAPIX APIs.
          </p>

          <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
            <button
              type="button"
              onClick={() => handleTriggerLgdSync('incremental')}
              disabled={actionLoadingState !== null}
              style={{
                padding: '10px 18px',
                borderRadius: 10,
                background: 'var(--canopy-700, #2F5233)',
                color: '#FFFFFF',
                border: 'none',
                fontSize: 13,
                fontWeight: 600,
                cursor: actionLoadingState ? 'not-allowed' : 'pointer',
                display: 'inline-flex',
                alignItems: 'center',
                gap: 8,
              }}
            >
              <RefreshCw size={14} className={actionLoadingState === 'lgd-incremental' ? 'animate-spin' : ''} />
              <span>Incremental LGD Sync (Last 7 Days)</span>
            </button>

            <button
              type="button"
              onClick={() => handleTriggerLgdSync('initial')}
              disabled={actionLoadingState !== null}
              style={{
                padding: '10px 18px',
                borderRadius: 10,
                background: 'transparent',
                border: '1.5px solid var(--canopy-700, #2F5233)',
                color: 'var(--canopy-700, #2F5233)',
                fontSize: 13,
                fontWeight: 600,
                cursor: actionLoadingState ? 'not-allowed' : 'pointer',
                display: 'inline-flex',
                alignItems: 'center',
                gap: 8,
              }}
            >
              <Database size={14} />
              <span>Full Pilot Baseline Sync</span>
            </button>
          </div>
        </div>

        {/* State-by-State Pilot Coverage & Sync Status */}
        <div
          style={{
            padding: 24,
            borderRadius: 20,
            background: '#FFFFFF',
            border: '1.5px solid var(--border-soft, #D9E1D8)',
            boxShadow: '0 4px 20px rgba(0, 0, 0, 0.03)',
            display: 'flex',
            flexDirection: 'column',
            gap: 16,
          }}
        >
          <h2 style={{ fontSize: 16, fontWeight: 700, color: '#1A1408', margin: 0 }}>
            State-Specific Geography Coverage & On-Demand Synchronization
          </h2>
          <p style={{ fontSize: 13, color: 'var(--text-muted, #9A7562)', margin: 0 }}>
            Preserving state-specific boundary synchronization: only sync missing state geography rather than doing nationwide bulk imports.
          </p>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: 16 }}>
            {PILOT_STATES.map((st) => {
              const cov = coverageMap[st.code]
              const stat = syncStatusMap[st.code]
              const isLoading = actionLoadingState === st.code
              const isComplete = stat?.status === 'COMPLETED' || (cov && cov.dbVillages > 0)

              return (
                <div
                  key={st.code}
                  style={{
                    padding: 18,
                    borderRadius: 14,
                    border: '1px solid var(--border-soft, #D9E1D8)',
                    background: 'rgba(255, 255, 255, 0.7)',
                    display: 'flex',
                    flexDirection: 'column',
                    gap: 12,
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      <MapPin size={18} style={{ color: 'var(--canopy-700, #2F5233)' }} />
                      <span style={{ fontSize: 15, fontWeight: 700, color: '#1A1408' }}>
                        {st.name}
                      </span>
                    </div>
                    <span
                      style={{
                        fontSize: 11,
                        fontWeight: 700,
                        padding: '3px 8px',
                        borderRadius: 999,
                        background: isComplete ? 'rgba(47, 82, 51, 0.12)' : 'rgba(242, 200, 75, 0.2)',
                        color: isComplete ? 'var(--canopy-700, #2F5233)' : '#854D0E',
                      }}
                    >
                      {stat?.status || (isComplete ? 'COVERED' : 'PENDING')}
                    </span>
                  </div>

                  <div style={{ fontSize: 12, color: 'var(--text-secondary, #6B4E3D)', display: 'flex', flexDirection: 'column', gap: 4 }}>
                    <div>
                      <strong>Target:</strong> {st.targetDistricts} Districts &bull; {st.targetMandals} Mandals &bull; {st.targetVillages.toLocaleString()} Villages
                    </div>
                    {cov && (
                      <div>
                        <strong>In DB:</strong> {cov.dbDistricts || 0} Districts &bull; {cov.dbMandals || 0} Mandals &bull; {(cov.dbVillages || 0).toLocaleString()} Villages
                      </div>
                    )}
                  </div>

                  <button
                    type="button"
                    onClick={() => handleTriggerSync(st.code, st.name)}
                    disabled={isLoading}
                    style={{
                      marginTop: 4,
                      padding: '8px 12px',
                      borderRadius: 8,
                      border: '1px solid rgba(47, 82, 51, 0.3)',
                      background: isLoading ? 'rgba(0, 0, 0, 0.05)' : 'rgba(122, 155, 92, 0.1)',
                      color: 'var(--canopy-700, #2F5233)',
                      fontSize: 12,
                      fontWeight: 600,
                      cursor: isLoading ? 'not-allowed' : 'pointer',
                      display: 'inline-flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      gap: 6,
                    }}
                  >
                    <RefreshCw size={12} className={isLoading ? 'animate-spin' : ''} />
                    <span>{isLoading ? 'Syncing...' : 'Sync This State'}</span>
                  </button>
                </div>
              )
            })}
          </div>
        </div>
      </div>
    </AppLayout>
  )
}
