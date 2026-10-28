'use client'

import React, { useState, useEffect } from 'react'
import Link from 'next/link'
import { motion } from 'framer-motion'
import { AlertCircle, ArrowRight, Sprout, BarChart2 } from 'lucide-react'
import NavRail from '@/components/ui/NavRail'
import AssistantPanel from '@/components/ui/AssistantPanel'
import HorizonScene from '@/components/ecosystem/HorizonScene'
import { cultivationService, CultivationResponseDto } from '@/lib/cultivationService'

export default function MarketPage() {
  const [assistantOpen, setAssistantOpen] = useState(false)
  const [cultivations, setCultivations] = useState<CultivationResponseDto[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    async function loadCultivations() {
      try {
        const res = await cultivationService.listCultivations({ page: 0, size: 50 })
        setCultivations(res.content || [])
      } catch (err) {
        console.error('Failed to load farmer cultivations for market page:', err)
      } finally {
        setLoading(false)
      }
    }
    loadCultivations()
  }, [])

  return (
    <div style={{ position: 'relative', minHeight: '100svh' }}>
      <HorizonScene />
      <NavRail active="/market" onAssistant={() => setAssistantOpen(true)} />

      <main style={{ marginLeft: 72, padding: '32px 28px 80px', position: 'relative', zIndex: 5, maxWidth: 1080 }}>
        {/* Header */}
        <motion.div
          initial={{ opacity: 0, y: -16 }}
          animate={{ opacity: 1, y: 0 }}
          style={{ marginBottom: 28 }}
        >
          <p style={{ fontFamily: 'var(--font-mono)', fontSize: 11, color: 'var(--text-muted)', letterSpacing: '0.1em', textTransform: 'uppercase', margin: 0 }}>
            Market Intelligence · Integration Gateway
          </p>
          <h1 style={{ fontFamily: 'var(--font-display)', fontSize: 30, fontWeight: 600, color: 'var(--text-primary)', margin: '4px 0 0' }}>
            APMC Mandi Price Gateway
          </h1>
        </motion.div>

        {/* Roadmap / Integration Status Banner */}
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
                background: 'rgba(245, 127, 23, 0.12)',
                color: '#D97706',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                flexShrink: 0,
              }}
            >
              <AlertCircle size={22} />
            </div>
            <div>
              <h2 style={{ fontSize: 17, fontWeight: 700, margin: 0, color: 'var(--text-primary, #272A22)' }}>
                e-NAM / Agmarknet Live Integration (Scheduled for Beta V2)
              </h2>
              <span style={{ fontSize: 12, color: 'var(--text-muted, #7E8376)' }}>
                Official National Agricultural Market API connector status
              </span>
            </div>
          </div>

          <p style={{ fontSize: 13.5, color: '#3A3F31', lineHeight: 1.6, margin: 0 }}>
            In accordance with platform verification guidelines, live mandi prices, modal rates, and MSP spreads are not simulated or fabricated. Connection to state APMC and central Agmarknet price feeds is queued for Release Beta V2 pending regulatory gateway approval.
          </p>

          <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', paddingTop: 4 }}>
            <Link
              href="/analytics"
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
              <BarChart2 size={16} />
              <span>View Verified Regional Analytics</span>
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
              <span>Review Registered Cultivations</span>
            </Link>
          </div>
        </motion.div>

        {/* Real Cultivations Approaching Market */}
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
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 18 }}>
            <div>
              <h2 style={{ fontSize: 18, fontWeight: 700, margin: 0, color: 'var(--text-primary)' }}>
                Your Registered Crops for Market
              </h2>
              <p style={{ fontSize: 12, color: 'var(--text-muted)', margin: '2px 0 0' }}>
                Cultivations registered under your verified farmer account
              </p>
            </div>
            <Link
              href="/cultivations/new"
              style={{
                fontSize: 12.5,
                fontWeight: 700,
                color: 'var(--canopy-700)',
                textDecoration: 'none',
                display: 'flex',
                alignItems: 'center',
                gap: 4,
              }}
            >
              <span>+ Add Cycle</span>
              <ArrowRight size={14} />
            </Link>
          </div>

          {loading ? (
            <div style={{ padding: '32px 0', textAlign: 'center', color: 'var(--text-muted)', fontSize: 13 }}>
              Loading registered cultivations...
            </div>
          ) : cultivations.length === 0 ? (
            <div style={{ padding: '36px 0', textAlign: 'center' }}>
              <p style={{ margin: '0 0 12px', fontSize: 14, color: 'var(--text-muted)' }}>
                No active cultivation cycles currently registered.
              </p>
              <Link
                href="/cultivations/new"
                style={{
                  display: 'inline-block',
                  padding: '8px 18px',
                  borderRadius: 10,
                  background: 'var(--canopy-700)',
                  color: '#fff',
                  fontSize: 13,
                  fontWeight: 600,
                  textDecoration: 'none',
                }}
              >
                Register First Cultivation
              </Link>
            </div>
          ) : (
            <div style={{ overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: 13 }}>
                <thead>
                  <tr style={{ borderBottom: '1.5px solid var(--border-soft)' }}>
                    <th style={{ padding: '10px 12px', color: 'var(--text-muted)', fontWeight: 600 }}>Crop</th>
                    <th style={{ padding: '10px 12px', color: 'var(--text-muted)', fontWeight: 600 }}>Plot Holding</th>
                    <th style={{ padding: '10px 12px', color: 'var(--text-muted)', fontWeight: 600 }}>Cultivated Area</th>
                    <th style={{ padding: '10px 12px', color: 'var(--text-muted)', fontWeight: 600 }}>Season</th>
                    <th style={{ padding: '10px 12px', color: 'var(--text-muted)', fontWeight: 600 }}>Lifecycle Status</th>
                    <th style={{ padding: '10px 12px', color: 'var(--text-muted)', fontWeight: 600, textAlign: 'right' }}>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {cultivations.map((c) => (
                    <tr key={c.id} style={{ borderBottom: '1px solid rgba(201,176,158,0.2)' }}>
                      <td style={{ padding: '12px', fontWeight: 700, color: 'var(--text-primary)' }}>{c.cropName}</td>
                      <td style={{ padding: '12px', color: 'var(--text-secondary)' }}>{c.plotLabel || 'Plot'}</td>
                      <td style={{ padding: '12px', fontFamily: 'var(--font-mono)' }}>{c.cultivatedAreaAcres} ac</td>
                      <td style={{ padding: '12px', color: 'var(--text-muted)' }}>{c.seasonName || 'Current'}</td>
                      <td style={{ padding: '12px' }}>
                        <span
                          style={{
                            padding: '3px 8px',
                            borderRadius: 6,
                            fontSize: 11,
                            fontWeight: 700,
                            background: c.status === 'STARTED' ? '#D4FD33' : '#EAE7DB',
                            color: '#1A2215',
                          }}
                        >
                          {c.status}
                        </span>
                      </td>
                      <td style={{ padding: '12px', textAlign: 'right' }}>
                        <Link
                          href={`/cultivations/${c.id}`}
                          style={{
                            fontSize: 12,
                            fontWeight: 600,
                            color: 'var(--canopy-700)',
                            textDecoration: 'none',
                          }}
                        >
                          Details →
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </motion.div>
      </main>

      <AssistantPanel open={assistantOpen} onClose={() => setAssistantOpen(false)} />
    </div>
  )
}
