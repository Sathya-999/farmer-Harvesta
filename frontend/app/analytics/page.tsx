'use client'

import React, { useState, useEffect } from 'react'
import Link from 'next/link'
import { motion } from 'framer-motion'
import {
  analyticsService,
  FarmerAnalyticsSummaryDto,
  GeographicSummaryDto,
  CropDistributionItemDto,
  DecisionSignalDto,
} from '@/lib/analyticsService'
import { farmerService, FarmerProfileDto } from '@/lib/farmerService'
import { cultivationService, SeasonDto } from '@/lib/cultivationService'
import AppLayout from '@/components/ui/AppLayout'
import {
  TrendingUp,
  PieChart,
  Lightbulb,
  MapPin,
  Layers,
  Sprout,
  Compass,
  AlertTriangle,
  CheckCircle2,
  Percent,
  Calendar,
} from 'lucide-react'

export default function AnalyticsPage() {
  const [profile, setProfile] = useState<FarmerProfileDto | null>(null)
  const [seasons, setSeasons] = useState<SeasonDto[]>([])
  const [selectedSeasonId, setSelectedSeasonId] = useState('')
  const [selectedYear, setSelectedYear] = useState<number>(new Date().getFullYear())

  const [farmerSummary, setFarmerSummary] = useState<FarmerAnalyticsSummaryDto | null>(null)
  const [geoSummary, setGeoSummary] = useState<GeographicSummaryDto | null>(null)
  const [cropDist, setCropDist] = useState<CropDistributionItemDto[]>([])
  const [signals, setSignals] = useState<DecisionSignalDto[]>([])

  const [loading, setLoading] = useState(true)
  const [errorMsg, setErrorMsg] = useState<string | null>(null)

  // 1. Initial Profile & Reference Load
  useEffect(() => {
    async function init() {
      try {
        const [prof, seasonsList] = await Promise.all([
          farmerService.getProfile(),
          cultivationService.getSeasons(),
        ])
        setProfile(prof)
        setSeasons(seasonsList || [])
      } catch (err: any) {
        console.error('Failed to load farmer profile for analytics:', err)
        setErrorMsg('Unable to initialize regional profile analytics.')
      }
    }
    init()
  }, [])

  // 2. Load Analytics Data
  useEffect(() => {
    async function loadAnalytics() {
      setLoading(true)
      setErrorMsg(null)
      try {
        // Farmer personal summary
        const fSum = await analyticsService.getFarmerSummary(
          selectedSeasonId || undefined,
          selectedYear || undefined
        )
        setFarmerSummary(fSum)

        // Regional analytics if location is present
        const targetLevel = profile?.mandalId ? 'MANDAL' : profile?.districtId ? 'DISTRICT' : null
        const targetId = profile?.mandalId || profile?.districtId

        if (targetLevel && targetId) {
          const [geoSumRes, cropDistRes, signalsRes] = await Promise.allSettled([
            analyticsService.getGeographicSummary(
              targetLevel,
              targetId,
              selectedSeasonId || undefined,
              selectedYear || undefined
            ),
            analyticsService.getCropDistribution(
              targetLevel,
              targetId,
              0,
              10,
              selectedSeasonId || undefined,
              selectedYear || undefined
            ),
            analyticsService.getSignalsAndInsights(
              targetLevel,
              targetId,
              selectedSeasonId || undefined,
              selectedYear || undefined
            ),
          ])

          if (geoSumRes.status === 'fulfilled') setGeoSummary(geoSumRes.value)
          if (cropDistRes.status === 'fulfilled') setCropDist(cropDistRes.value.items || [])
          if (signalsRes.status === 'fulfilled') setSignals(signalsRes.value.signals || [])
        }
      } catch (err: any) {
        console.error('Failed to fetch analytics metrics:', err)
        setErrorMsg('Some analytics services are currently updating. Displaying available insights.')
      } finally {
        setLoading(false)
      }
    }

    loadAnalytics()
  }, [profile, selectedSeasonId, selectedYear])

  return (
    <AppLayout>
      <div style={{ maxWidth: 1040, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 28 }}>
        {/* Header */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 16 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
            <div
              style={{
                width: 48,
                height: 48,
                borderRadius: 14,
                background: 'rgba(47, 82, 51, 0.12)',
                color: 'var(--canopy-700, #2F5233)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <TrendingUp size={24} />
            </div>
            <div>
              <h1
                style={{
                  fontFamily: 'var(--font-serif)',
                  fontSize: 28,
                  fontWeight: 700,
                  color: 'var(--canopy-900, #163A24)',
                  margin: 0,
                }}
              >
                Regional Agricultural Intelligence
              </h1>
              <p style={{ margin: '3px 0 0', fontSize: 13, color: 'var(--text-muted, #7A6552)' }}>
                {profile?.mandalName
                  ? `${profile.mandalName} Mandal, ${profile.districtName || 'District'} • Verified Land Intelligence`
                  : 'Aggregate holding utilization and regional crop distribution'}
              </p>
            </div>
          </div>

          {/* Season & Year Controls */}
          <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
            <select
              value={selectedSeasonId}
              onChange={(e) => setSelectedSeasonId(e.target.value)}
              style={{
                padding: '8px 12px',
                borderRadius: 10,
                border: '1.5px solid var(--border-soft, #D9E1D8)',
                background: '#fff',
                fontSize: 13,
                color: 'var(--canopy-900, #163A24)',
                outline: 'none',
              }}
            >
              <option value="">All Seasons</option>
              {seasons.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.name}
                </option>
              ))}
            </select>

            <select
              value={selectedYear}
              onChange={(e) => setSelectedYear(Number(e.target.value))}
              style={{
                padding: '8px 12px',
                borderRadius: 10,
                border: '1.5px solid var(--border-soft, #D9E1D8)',
                background: '#fff',
                fontSize: 13,
                color: 'var(--canopy-900, #163A24)',
                outline: 'none',
              }}
            >
              {[2026, 2025, 2024].map((yr) => (
                <option key={yr} value={yr}>
                  {yr}
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* Farmer Land Capacity Summary Card */}
        {farmerSummary && (
          <motion.div
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            style={{
              borderRadius: 22,
              background: 'linear-gradient(135deg, rgba(22, 58, 36, 0.94), rgba(47, 82, 51, 0.96))',
              color: '#fff',
              padding: '28px 32px',
              boxShadow: '0 12px 36px rgba(22, 58, 36, 0.22)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 20, flexWrap: 'wrap', gap: 12 }}>
              <div>
                <span
                  style={{
                    fontSize: 11,
                    textTransform: 'uppercase',
                    letterSpacing: '0.08em',
                    fontWeight: 700,
                    color: 'var(--sun-400, #F2C84B)',
                  }}
                >
                  Personal Land Capacity
                </span>
                <h2 style={{ fontFamily: 'var(--font-serif)', fontSize: 22, fontWeight: 700, margin: '4px 0 0' }}>
                  Holding Utilization Overview
                </h2>
              </div>
              {farmerSummary.primaryCrop && (
                <span
                  style={{
                    padding: '6px 14px',
                    borderRadius: 999,
                    background: 'rgba(255, 255, 255, 0.15)',
                    backdropFilter: 'blur(8px)',
                    fontSize: 12,
                    fontWeight: 600,
                  }}
                >
                  Primary Crop: {farmerSummary.primaryCrop}
                </span>
              )}
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 20 }}>
              <div>
                <div style={{ fontSize: 12, opacity: 0.8 }}>Registered Land</div>
                <div style={{ fontSize: 28, fontWeight: 700, marginTop: 4 }}>
                  {farmerSummary.totalLandAreaAcres} <span style={{ fontSize: 16 }}>Acres</span>
                </div>
                <div style={{ fontSize: 11, opacity: 0.7, marginTop: 2 }}>
                  across {farmerSummary.registeredPlotsCount} plot holdings
                </div>
              </div>

              <div>
                <div style={{ fontSize: 12, opacity: 0.8 }}>Currently Sown</div>
                <div style={{ fontSize: 28, fontWeight: 700, marginTop: 4 }}>
                  {farmerSummary.currentlyCultivatedAcres} <span style={{ fontSize: 16 }}>Acres</span>
                </div>
                <div style={{ fontSize: 11, opacity: 0.7, marginTop: 2 }}>
                  in {farmerSummary.activeCultivationsCount} active cycle(s)
                </div>
              </div>

              <div>
                <div style={{ fontSize: 12, opacity: 0.8 }}>Land Utilization</div>
                <div style={{ fontSize: 28, fontWeight: 700, marginTop: 4, color: 'var(--sun-400, #F2C84B)' }}>
                  {(farmerSummary.utilizationPercentage ?? 0).toFixed(1)}%
                </div>
                {/* Progress bar */}
                <div
                  style={{
                    width: '100%',
                    height: 6,
                    borderRadius: 3,
                    background: 'rgba(255, 255, 255, 0.2)',
                    marginTop: 8,
                    overflow: 'hidden',
                  }}
                >
                  <div
                    style={{
                      width: `${Math.min(100, farmerSummary.utilizationPercentage ?? 0)}%`,
                      height: '100%',
                      background: 'var(--sun-400, #F2C84B)',
                      borderRadius: 3,
                    }}
                  />
                </div>
              </div>
            </div>
          </motion.div>
        )}

        {/* Two-Column: Regional Crop Distribution & Decision Signals */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))', gap: 24 }}>
          {/* Regional Crop Distribution */}
          <div
            style={{
              padding: 24,
              borderRadius: 22,
              background: 'rgba(255, 255, 255, 0.94)',
              backdropFilter: 'blur(16px)',
              border: '1.5px solid var(--border-soft, #D9E1D8)',
              boxShadow: '0 8px 24px rgba(26, 20, 8, 0.04)',
              display: 'flex',
              flexDirection: 'column',
              gap: 16,
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <PieChart size={20} color="var(--canopy-700, #2F5233)" />
              <h3
                style={{
                  fontFamily: 'var(--font-serif)',
                  fontSize: 18,
                  fontWeight: 700,
                  color: 'var(--canopy-900, #163A24)',
                  margin: 0,
                }}
              >
                Regional Crop Distribution
              </h3>
            </div>
            <p style={{ margin: 0, fontSize: 13, color: 'var(--text-muted, #7A6552)' }}>
              Acreage allocation by crop in your administrative cluster.
            </p>

            {loading ? (
              <div style={{ textAlign: 'center', padding: '36px 0', color: 'var(--text-muted, #7A6552)' }}>
                Compiling regional distribution...
              </div>
            ) : cropDist.length === 0 ? (
              <div style={{ padding: '30px 0', textAlign: 'center', color: 'var(--text-muted, #7A6552)', fontSize: 13 }}>
                No regional crop distribution data recorded for this season.
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
                {cropDist.map((item) => (
                  <div key={item.cropId}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13, marginBottom: 4 }}>
                      <span style={{ fontWeight: 700, color: 'var(--canopy-900, #163A24)' }}>{item.cropName}</span>
                      <span style={{ color: 'var(--text-muted, #7A6552)' }}>
                        <strong>{item.cultivatedAreaAcres} Ac</strong> ({item.percentageOfTotal.toFixed(1)}%)
                      </span>
                    </div>
                    <div
                      style={{
                        width: '100%',
                        height: 8,
                        borderRadius: 4,
                        background: '#F0EFEA',
                        overflow: 'hidden',
                      }}
                    >
                      <div
                        style={{
                          width: `${Math.min(100, item.percentageOfTotal)}%`,
                          height: '100%',
                          borderRadius: 4,
                          background: 'linear-gradient(90deg, var(--canopy-600, #406844), var(--canopy-800, #224229))',
                        }}
                      />
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Decision Support Signals */}
          <div
            style={{
              padding: 24,
              borderRadius: 22,
              background: 'rgba(255, 255, 255, 0.94)',
              backdropFilter: 'blur(16px)',
              border: '1.5px solid var(--border-soft, #D9E1D8)',
              boxShadow: '0 8px 24px rgba(26, 20, 8, 0.04)',
              display: 'flex',
              flexDirection: 'column',
              gap: 16,
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <Lightbulb size={20} color="var(--sun-600, #D99B16)" />
              <h3
                style={{
                  fontFamily: 'var(--font-serif)',
                  fontSize: 18,
                  fontWeight: 700,
                  color: 'var(--canopy-900, #163A24)',
                  margin: 0,
                }}
              >
                Decision Support Signals
              </h3>
            </div>
            <p style={{ margin: 0, fontSize: 13, color: 'var(--text-muted, #7A6552)' }}>
              Algorithmic insights derived from regional planting density and price trends.
            </p>

            {loading ? (
              <div style={{ textAlign: 'center', padding: '36px 0', color: 'var(--text-muted, #7A6552)' }}>
                Analyzing regional signals...
              </div>
            ) : signals.length === 0 ? (
              <div style={{ padding: '30px 0', textAlign: 'center', color: 'var(--text-muted, #7A6552)', fontSize: 13 }}>
                No active decision signals generated for this regional cycle.
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                {signals.map((sig, idx) => (
                  <div
                    key={idx}
                    style={{
                      padding: '14px 16px',
                      borderRadius: 14,
                      background:
                        sig.severity === 'OPPORTUNITY'
                          ? 'rgba(34, 197, 94, 0.06)'
                          : sig.severity === 'WARNING'
                          ? 'rgba(220, 38, 38, 0.06)'
                          : '#FAF8F5',
                      border: `1px solid ${
                        sig.severity === 'OPPORTUNITY'
                          ? 'rgba(34, 197, 94, 0.25)'
                          : sig.severity === 'WARNING'
                          ? 'rgba(220, 38, 38, 0.25)'
                          : 'var(--border-soft, #D9E1D8)'
                      }`,
                      fontSize: 13,
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8 }}>
                      <span style={{ fontWeight: 700, color: 'var(--canopy-900, #163A24)' }}>{sig.title}</span>
                      <span
                        style={{
                          fontSize: 10,
                          fontWeight: 700,
                          padding: '2px 6px',
                          borderRadius: 4,
                          background:
                            sig.severity === 'OPPORTUNITY'
                              ? '#15803D'
                              : sig.severity === 'WARNING'
                              ? '#DC2626'
                              : 'var(--canopy-800, #224229)',
                          color: '#fff',
                        }}
                      >
                        {sig.severity}
                      </span>
                    </div>
                    <p style={{ margin: '4px 0 0', fontSize: 12, color: 'var(--text-muted, #7A6552)', lineHeight: 1.4 }}>
                      {sig.description}
                    </p>
                    {sig.confidenceScore && (
                      <div style={{ marginTop: 6, fontSize: 11, color: 'var(--canopy-700, #2F5233)', fontWeight: 600 }}>
                        Confidence Index: {(sig.confidenceScore * 100).toFixed(0)}%
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </AppLayout>
  )
}
