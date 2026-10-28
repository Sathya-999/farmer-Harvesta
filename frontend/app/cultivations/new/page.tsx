'use client'

import React, { useState, useEffect } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import Link from 'next/link'
import { motion } from 'framer-motion'
import { cultivationService, CropDto, CropVarietyDto, SeasonDto } from '@/lib/cultivationService'
import { plotService, PlotResponseDto } from '@/lib/plotService'
import AppLayout from '@/components/ui/AppLayout'
import { Sprout, ArrowLeft, Check, AlertCircle, Calendar, Layers, MapPin, Info } from 'lucide-react'

function NewCultivationForm() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const preselectedPlotId = searchParams?.get('plotId') || ''

  const [plots, setPlots] = useState<PlotResponseDto[]>([])
  const [crops, setCrops] = useState<CropDto[]>([])
  const [varieties, setVarieties] = useState<CropVarietyDto[]>([])
  const [seasons, setSeasons] = useState<SeasonDto[]>([])

  const [selectedPlotId, setSelectedPlotId] = useState(preselectedPlotId)
  const [selectedCropId, setSelectedCropId] = useState('')
  const [selectedVarietyId, setSelectedVarietyId] = useState('')
  const [selectedSeasonId, setSelectedSeasonId] = useState('')
  const [cultivationYear, setCultivationYear] = useState(new Date().getFullYear())
  const [cultivatedAreaAcres, setCultivatedAreaAcres] = useState('')
  const [plannedPlantingDate, setPlannedPlantingDate] = useState('')
  const [expectedHarvestDate, setExpectedHarvestDate] = useState('')
  const [notes, setNotes] = useState('')

  const [initLoading, setInitLoading] = useState(true)
  const [loadingVarieties, setLoadingVarieties] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [errorMsg, setErrorMsg] = useState<string | null>(null)

  // Load initial reference data & plots
  useEffect(() => {
    async function initData() {
      try {
        const [plotsPage, cropsList, seasonsList] = await Promise.all([
          plotService.listPlots(0, 100, 'ACTIVE'),
          cultivationService.getCrops(),
          cultivationService.getSeasons(),
        ])
        setPlots(plotsPage.content || [])
        setCrops(cropsList || [])
        setSeasons(seasonsList || [])

        // If preselectedPlotId exists in fetched plots, select it
        if (preselectedPlotId) {
          setSelectedPlotId(preselectedPlotId)
        } else if (plotsPage.content && plotsPage.content.length > 0) {
          setSelectedPlotId(plotsPage.content[0].id)
        }

        if (seasonsList && seasonsList.length > 0) {
          setSelectedSeasonId(seasonsList[0].id)
        }
      } catch (err: any) {
        console.error('Failed to load cultivation form prerequisites:', err)
        setErrorMsg('Failed to load available plots or crop data. Please verify your connection.')
      } finally {
        setInitLoading(false)
      }
    }
    initData()
  }, [preselectedPlotId])

  // Load varieties when crop selection changes
  useEffect(() => {
    if (!selectedCropId) {
      setVarieties([])
      setSelectedVarietyId('')
      return
    }

    async function loadVarieties() {
      setLoadingVarieties(true)
      try {
        const vars = await cultivationService.getVarieties(selectedCropId)
        setVarieties(vars || [])
      } catch (err) {
        console.error('Failed to fetch varieties:', err)
        setVarieties([])
      } finally {
        setLoadingVarieties(false)
      }
    }
    loadVarieties()
  }, [selectedCropId])

  const selectedPlot = plots.find((p) => p.id === selectedPlotId)

  // Auto-fill area when plot changes if empty
  const handlePlotChange = (plotId: string) => {
    setSelectedPlotId(plotId)
    const p = plots.find((item) => item.id === plotId)
    if (p && !cultivatedAreaAcres) {
      setCultivatedAreaAcres(String(p.totalAreaAcres))
    }
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setErrorMsg(null)

    if (!selectedPlotId) {
      setErrorMsg('Please select a target agricultural plot.')
      return
    }

    if (!selectedCropId) {
      setErrorMsg('Please select a crop to cultivate.')
      return
    }

    if (!selectedSeasonId) {
      setErrorMsg('Please specify an agricultural season.')
      return
    }

    const areaNum = parseFloat(cultivatedAreaAcres)
    if (isNaN(areaNum) || areaNum <= 0) {
      setErrorMsg('Cultivated area must be a positive number greater than 0.')
      return
    }

    if (selectedPlot && areaNum > selectedPlot.totalAreaAcres) {
      setErrorMsg(
        `Cultivated area (${areaNum} acres) cannot exceed the total plot holding area (${selectedPlot.totalAreaAcres} acres).`
      )
      return
    }

    setSubmitting(true)

    try {
      await cultivationService.createCultivation({
        plotId: selectedPlotId,
        cropId: selectedCropId,
        varietyId: selectedVarietyId || undefined,
        seasonId: selectedSeasonId,
        cultivationYear: Number(cultivationYear),
        cultivatedAreaAcres: areaNum,
        plannedPlantingDate: plannedPlantingDate || new Date().toISOString().split('T')[0],
        expectedHarvestDate: expectedHarvestDate || undefined,
        notes: notes.trim() || undefined,
      })

      router.push('/cultivations')
    } catch (err: any) {
      console.error('Failed to register cultivation cycle:', err)
      setErrorMsg(
        err.message ||
          'Failed to record cultivation intention. Ensure no conflicting active cultivation exists on this plot.'
      )
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AppLayout>
      <div style={{ maxWidth: 720, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 20 }}>
        {/* Back Link */}
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

        {/* Card Form */}
        <motion.div
          initial={{ opacity: 0, y: 14 }}
          animate={{ opacity: 1, y: 0 }}
          style={{
            borderRadius: 24,
            background: 'rgba(255, 255, 255, 0.94)',
            backdropFilter: 'blur(16px)',
            border: '1.5px solid var(--border-soft, #D9E1D8)',
            padding: '32px clamp(18px, 4vw, 36px)',
            boxShadow: '0 8px 30px rgba(26, 20, 8, 0.06)',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 8 }}>
            <div
              style={{
                width: 42,
                height: 42,
                borderRadius: 12,
                background: 'rgba(47, 82, 51, 0.12)',
                color: 'var(--canopy-700, #2F5233)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <Sprout size={22} />
            </div>
            <div>
              <h1
                style={{
                  fontFamily: 'var(--font-serif)',
                  fontSize: 24,
                  fontWeight: 700,
                  color: 'var(--canopy-900, #163A24)',
                  margin: 0,
                }}
              >
                Register Cultivation Intention
              </h1>
              <p style={{ margin: 0, fontSize: 13, color: 'var(--text-muted, #7A6552)' }}>
                Record planned crop sowing cycle for your agricultural holding.
              </p>
            </div>
          </div>

          {/* Error Banner */}
          {errorMsg && (
            <div
              style={{
                display: 'flex',
                alignItems: 'flex-start',
                gap: 10,
                padding: '12px 16px',
                borderRadius: 12,
                background: 'rgba(220, 38, 38, 0.08)',
                border: '1px solid rgba(220, 38, 38, 0.25)',
                color: '#991B1B',
                fontSize: 13,
                margin: '20px 0',
              }}
            >
              <AlertCircle size={18} style={{ flexShrink: 0, marginTop: 1 }} />
              <div>{errorMsg}</div>
            </div>
          )}

          {initLoading ? (
            <div style={{ textAlign: 'center', padding: '48px 0', color: 'var(--text-muted, #7A6552)' }}>
              Loading plots and crop registry...
            </div>
          ) : plots.length === 0 ? (
            <div
              style={{
                textAlign: 'center',
                padding: '36px 20px',
                background: 'rgba(242, 200, 75, 0.1)',
                borderRadius: 16,
                border: '1px solid rgba(242, 200, 75, 0.3)',
                margin: '20px 0',
              }}
            >
              <Layers size={36} color="var(--sun-600, #D99B16)" style={{ margin: '0 auto 10px' }} />
              <h3 style={{ margin: '0 0 6px', color: 'var(--canopy-900, #163A24)' }}>No Active Plots Registered</h3>
              <p style={{ fontSize: 13, color: 'var(--text-muted, #7A6552)', margin: '0 0 16px' }}>
                You must register an agricultural plot holding before recording a cultivation cycle.
              </p>
              <Link
                href="/plots/new"
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: 8,
                  padding: '10px 18px',
                  borderRadius: 10,
                  background: 'var(--canopy-800, #224229)',
                  color: '#fff',
                  textDecoration: 'none',
                  fontWeight: 600,
                  fontSize: 13,
                }}
              >
                Register First Plot
              </Link>
            </div>
          ) : (
            <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 20, marginTop: 20 }}>
              {/* Plot Selection */}
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
                  Target Plot <span style={{ color: '#DC2626' }}>*</span>
                </label>
                <select
                  value={selectedPlotId}
                  onChange={(e) => handlePlotChange(e.target.value)}
                  required
                  style={{
                    width: '100%',
                    padding: '11px 14px',
                    borderRadius: 12,
                    border: '1.5px solid var(--border-soft, #D9E1D8)',
                    background: '#fff',
                    fontSize: 14,
                    color: 'var(--canopy-900, #163A24)',
                    outline: 'none',
                  }}
                >
                  <option value="" disabled>Select a plot holding...</option>
                  {plots.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.label || `Plot ${p.id.slice(0, 8)}`} — {p.totalAreaAcres} Acres ({p.villageName || 'Village'}, {p.mandalName || 'Mandal'})
                    </option>
                  ))}
                </select>
                {selectedPlot && (
                  <p style={{ fontSize: 12, color: 'var(--text-muted, #7A6552)', margin: '6px 0 0' }}>
                    Available holding capacity: <strong>{selectedPlot.totalAreaAcres} Acres</strong>
                  </p>
                )}
              </div>

              {/* Crop & Variety Grid */}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: 16 }}>
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
                    Crop <span style={{ color: '#DC2626' }}>*</span>
                  </label>
                  <select
                    value={selectedCropId}
                    onChange={(e) => setSelectedCropId(e.target.value)}
                    required
                    style={{
                      width: '100%',
                      padding: '11px 14px',
                      borderRadius: 12,
                      border: '1.5px solid var(--border-soft, #D9E1D8)',
                      background: '#fff',
                      fontSize: 14,
                      color: 'var(--canopy-900, #163A24)',
                      outline: 'none',
                    }}
                  >
                    <option value="">Select crop...</option>
                    {crops.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.name} {c.scientificName ? `(${c.scientificName})` : ''}
                      </option>
                    ))}
                  </select>
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
                    Variety (Optional)
                  </label>
                  <select
                    value={selectedVarietyId}
                    onChange={(e) => setSelectedVarietyId(e.target.value)}
                    disabled={!selectedCropId || loadingVarieties}
                    style={{
                      width: '100%',
                      padding: '11px 14px',
                      borderRadius: 12,
                      border: '1.5px solid var(--border-soft, #D9E1D8)',
                      background: !selectedCropId ? '#F5F5F0' : '#fff',
                      fontSize: 14,
                      color: 'var(--canopy-900, #163A24)',
                      outline: 'none',
                    }}
                  >
                    <option value="">
                      {loadingVarieties
                        ? 'Loading varieties...'
                        : !selectedCropId
                        ? 'Select a crop first'
                        : varieties.length === 0
                        ? 'Standard / General Variety'
                        : 'Select variety...'}
                    </option>
                    {varieties.map((v) => (
                      <option key={v.id} value={v.id}>
                        {v.name} {v.durationDays ? `(${v.durationDays} days)` : ''}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              {/* Season & Year & Cultivated Area Grid */}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 16 }}>
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
                    Season <span style={{ color: '#DC2626' }}>*</span>
                  </label>
                  <select
                    value={selectedSeasonId}
                    onChange={(e) => setSelectedSeasonId(e.target.value)}
                    required
                    style={{
                      width: '100%',
                      padding: '11px 14px',
                      borderRadius: 12,
                      border: '1.5px solid var(--border-soft, #D9E1D8)',
                      background: '#fff',
                      fontSize: 14,
                      color: 'var(--canopy-900, #163A24)',
                      outline: 'none',
                    }}
                  >
                    <option value="" disabled>Select season...</option>
                    {seasons.map((s) => (
                      <option key={s.id} value={s.id}>
                        {s.name} ({s.code})
                      </option>
                    ))}
                  </select>
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
                    Cultivation Year <span style={{ color: '#DC2626' }}>*</span>
                  </label>
                  <input
                    type="number"
                    min={2020}
                    max={2035}
                    value={cultivationYear}
                    onChange={(e) => setCultivationYear(Number(e.target.value))}
                    required
                    style={{
                      width: '100%',
                      padding: '11px 14px',
                      borderRadius: 12,
                      border: '1.5px solid var(--border-soft, #D9E1D8)',
                      background: '#fff',
                      fontSize: 14,
                      color: 'var(--canopy-900, #163A24)',
                      outline: 'none',
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
                    Cultivated Area (Acres) <span style={{ color: '#DC2626' }}>*</span>
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    min="0.01"
                    placeholder="e.g. 2.50"
                    value={cultivatedAreaAcres}
                    onChange={(e) => setCultivatedAreaAcres(e.target.value)}
                    required
                    style={{
                      width: '100%',
                      padding: '11px 14px',
                      borderRadius: 12,
                      border: '1.5px solid var(--border-soft, #D9E1D8)',
                      background: '#fff',
                      fontSize: 14,
                      color: 'var(--canopy-900, #163A24)',
                      outline: 'none',
                      boxSizing: 'border-box',
                    }}
                  />
                </div>
              </div>

              {/* Dates Grid */}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: 16 }}>
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
                    Planned Sowing / Planting Date
                  </label>
                  <input
                    type="date"
                    value={plannedPlantingDate}
                    onChange={(e) => setPlannedPlantingDate(e.target.value)}
                    style={{
                      width: '100%',
                      padding: '11px 14px',
                      borderRadius: 12,
                      border: '1.5px solid var(--border-soft, #D9E1D8)',
                      background: '#fff',
                      fontSize: 14,
                      color: 'var(--canopy-900, #163A24)',
                      outline: 'none',
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
                    value={expectedHarvestDate}
                    onChange={(e) => setExpectedHarvestDate(e.target.value)}
                    style={{
                      width: '100%',
                      padding: '11px 14px',
                      borderRadius: 12,
                      border: '1.5px solid var(--border-soft, #D9E1D8)',
                      background: '#fff',
                      fontSize: 14,
                      color: 'var(--canopy-900, #163A24)',
                      outline: 'none',
                      boxSizing: 'border-box',
                    }}
                  />
                </div>
              </div>

              {/* Notes */}
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
                  Cycle Notes & Crop Intentions (Optional)
                </label>
                <textarea
                  rows={3}
                  placeholder="Notes on seed treatment, target market, fertilizer plans..."
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '11px 14px',
                    borderRadius: 12,
                    border: '1.5px solid var(--border-soft, #D9E1D8)',
                    background: '#fff',
                    fontSize: 14,
                    color: 'var(--canopy-900, #163A24)',
                    outline: 'none',
                    resize: 'vertical',
                    boxSizing: 'border-box',
                  }}
                />
              </div>

              {/* Notice Box */}
              <div
                style={{
                  display: 'flex',
                  alignItems: 'flex-start',
                  gap: 10,
                  padding: '12px 16px',
                  borderRadius: 12,
                  background: 'rgba(47, 82, 51, 0.05)',
                  border: '1px solid rgba(47, 82, 51, 0.12)',
                  fontSize: 12,
                  color: 'var(--canopy-800, #224229)',
                }}
              >
                <Info size={16} style={{ flexShrink: 0, marginTop: 1 }} />
                <span>
                  <strong>Alpha Platform Guideline:</strong> Exactly one primary crop is registered per plot holding
                  per cultivation cycle. Intercropping capabilities will be enabled in upcoming releases.
                </span>
              </div>

              {/* Action Buttons */}
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 12, marginTop: 10 }}>
                <Link
                  href="/cultivations"
                  style={{
                    padding: '12px 22px',
                    borderRadius: 12,
                    border: '1.5px solid var(--border-soft, #D9E1D8)',
                    background: '#fff',
                    color: 'var(--text-muted, #7A6552)',
                    fontSize: 14,
                    fontWeight: 600,
                    textDecoration: 'none',
                    textAlign: 'center',
                  }}
                >
                  Cancel
                </Link>
                <button
                  type="submit"
                  disabled={submitting}
                  style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: 8,
                    padding: '12px 28px',
                    borderRadius: 12,
                    border: 'none',
                    background: submitting
                      ? 'var(--text-muted, #7A6552)'
                      : 'linear-gradient(135deg, var(--canopy-800, #224229), var(--canopy-900, #163A24))',
                    color: '#fff',
                    fontSize: 14,
                    fontWeight: 600,
                    cursor: submitting ? 'not-allowed' : 'pointer',
                    boxShadow: '0 4px 14px rgba(22, 58, 36, 0.25)',
                  }}
                >
                  {submitting ? (
                    'Recording Intention...'
                  ) : (
                    <>
                      <Check size={18} />
                      <span>Record Cultivation</span>
                    </>
                  )}
                </button>
              </div>
            </form>
          )}
        </motion.div>
      </div>
    </AppLayout>
  )
}

export default function NewCultivationPage() {
  return (
    <React.Suspense fallback={<div style={{ textAlign: 'center', padding: '60px 0' }}>Loading form...</div>}>
      <NewCultivationForm />
    </React.Suspense>
  )
}
