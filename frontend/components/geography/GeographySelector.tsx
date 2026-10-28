'use client'

import React, { useEffect, useState, useCallback, useRef } from 'react'
import {
  geographyService,
  StateDto,
  DistrictDto,
  MandalDto,
  VillageDto,
  LocationResolutionDto,
} from '@/lib/geographyService'
import { MapPin, Navigation, CheckCircle2, AlertCircle, Loader2 } from 'lucide-react'

export interface SelectedGeography {
  stateId: string
  stateName?: string
  districtId: string
  districtName?: string
  mandalId: string
  mandalName?: string
  villageId: string
  villageName?: string
}

interface Props {
  value?: Partial<SelectedGeography>
  onChange: (geo: SelectedGeography) => void
  disabled?: boolean
  required?: boolean
  showGpsButton?: boolean
}

export default function GeographySelector({
  value,
  onChange,
  disabled = false,
  required = true,
  showGpsButton = true,
}: Props) {
  // Lists
  const [states, setStates] = useState<StateDto[]>([])
  const [districts, setDistricts] = useState<DistrictDto[]>([])
  const [mandals, setMandals] = useState<MandalDto[]>([])
  const [villages, setVillages] = useState<VillageDto[]>([])

  // Selections
  const [selectedStateId, setSelectedStateId] = useState(value?.stateId || '')
  const [selectedDistrictId, setSelectedDistrictId] = useState(value?.districtId || '')
  const [selectedMandalId, setSelectedMandalId] = useState(value?.mandalId || '')
  const [selectedVillageId, setSelectedVillageId] = useState(value?.villageId || '')

  // Loading states
  const [loadingStates, setLoadingStates] = useState(false)
  const [loadingDistricts, setLoadingDistricts] = useState(false)
  const [loadingMandals, setLoadingMandals] = useState(false)
  const [loadingVillages, setLoadingVillages] = useState(false)
  const [gpsLoading, setGpsLoading] = useState(false)
  const [gpsError, setGpsError] = useState<string | null>(null)
  const [suggestion, setSuggestion] = useState<LocationResolutionDto | null>(null)

  // Ref tracking pending candidate cascade across async hierarchy loads
  const pendingCandidateRef = useRef<LocationResolutionDto | null>(null)

  // 1. Load active states on mount
  useEffect(() => {
    let isCurrent = true
    setLoadingStates(true)
    geographyService
      .getStates()
      .then((data) => {
        if (!isCurrent) return
        setStates(data || [])
      })
      .catch((err) => {
        if (!isCurrent) return
        console.error('Failed to load states from geography API:', err)
        setStates([])
      })
      .finally(() => {
        if (isCurrent) setLoadingStates(false)
      })

    return () => {
      isCurrent = false
    }
  }, [])

  // 2. Load districts when state changes
  useEffect(() => {
    let isCurrent = true
    if (!selectedStateId) {
      setDistricts([])
      setSelectedDistrictId('')
      setMandals([])
      setSelectedMandalId('')
      setVillages([])
      setSelectedVillageId('')
      return
    }

    setLoadingDistricts(true)
    geographyService
      .getDistricts(selectedStateId)
      .then((data) => {
        if (!isCurrent) return
        const list = data || []
        setDistricts(list)

        const candidate = pendingCandidateRef.current
        if (candidate && (candidate.candidateDistrictCode || candidate.candidateDistrictName)) {
          const matchedDistrict = list.find(
            (d) =>
              (candidate.candidateDistrictCode && d.code === candidate.candidateDistrictCode) ||
              (candidate.candidateDistrictName && d.name.toLowerCase() === candidate.candidateDistrictName.toLowerCase())
          )
          if (matchedDistrict) {
            setSelectedDistrictId(matchedDistrict.id)
          } else {
            setSelectedDistrictId('')
            pendingCandidateRef.current = null
          }
        } else {
          setSelectedDistrictId('')
          if (candidate && !candidate.candidateDistrictCode && !candidate.candidateDistrictName) {
            pendingCandidateRef.current = null
          }
        }
      })
      .catch(() => {
        if (!isCurrent) return
        setDistricts([])
        setSelectedDistrictId('')
        pendingCandidateRef.current = null
      })
      .finally(() => {
        if (isCurrent) setLoadingDistricts(false)
      })

    return () => {
      isCurrent = false
    }
  }, [selectedStateId])

  // 3. Load mandals when district changes
  useEffect(() => {
    let isCurrent = true
    if (!selectedDistrictId) {
      setMandals([])
      setSelectedMandalId('')
      setVillages([])
      setSelectedVillageId('')
      return
    }

    setLoadingMandals(true)
    geographyService
      .getMandals(selectedDistrictId)
      .then((data) => {
        if (!isCurrent) return
        const list = data || []
        setMandals(list)

        const candidate = pendingCandidateRef.current
        if (candidate && (candidate.candidateMandalCode || candidate.candidateMandalName)) {
          const matchedMandal = list.find(
            (m) =>
              (candidate.candidateMandalCode && m.code === candidate.candidateMandalCode) ||
              (candidate.candidateMandalName && m.name.toLowerCase() === candidate.candidateMandalName.toLowerCase())
          )
          if (matchedMandal) {
            setSelectedMandalId(matchedMandal.id)
          } else {
            setSelectedMandalId('')
            pendingCandidateRef.current = null
          }
        } else {
          setSelectedMandalId('')
          if (candidate && !candidate.candidateMandalCode && !candidate.candidateMandalName) {
            pendingCandidateRef.current = null
          }
        }
      })
      .catch(() => {
        if (!isCurrent) return
        setMandals([])
        setSelectedMandalId('')
        pendingCandidateRef.current = null
      })
      .finally(() => {
        if (isCurrent) setLoadingMandals(false)
      })

    return () => {
      isCurrent = false
    }
  }, [selectedDistrictId])

  // 4. Load villages when mandal changes
  useEffect(() => {
    let isCurrent = true
    if (!selectedMandalId) {
      setVillages([])
      setSelectedVillageId('')
      return
    }

    setLoadingVillages(true)
    geographyService
      .getVillages(selectedMandalId)
      .then((data) => {
        if (!isCurrent) return
        const list = data || []
        setVillages(list)

        const candidate = pendingCandidateRef.current
        if (candidate && (candidate.candidateVillageCode || candidate.candidateVillageName)) {
          const matchedVillage = list.find(
            (v) =>
              (candidate.candidateVillageCode && v.code === candidate.candidateVillageCode) ||
              (candidate.candidateVillageName && v.name.toLowerCase() === candidate.candidateVillageName.toLowerCase())
          )
          if (matchedVillage) {
            setSelectedVillageId(matchedVillage.id)
          } else {
            setSelectedVillageId('')
          }
        } else {
          setSelectedVillageId('')
        }
        pendingCandidateRef.current = null
      })
      .catch(() => {
        if (!isCurrent) return
        setVillages([])
        setSelectedVillageId('')
        pendingCandidateRef.current = null
      })
      .finally(() => {
        if (isCurrent) setLoadingVillages(false)
      })

    return () => {
      isCurrent = false
    }
  }, [selectedMandalId])

  // 5. Emit full selection change when village is chosen
  useEffect(() => {
    if (selectedVillageId && selectedStateId && selectedDistrictId && selectedMandalId) {
      const stateObj = states.find((s) => s.id === selectedStateId)
      const districtObj = districts.find((d) => d.id === selectedDistrictId)
      const mandalObj = mandals.find((m) => m.id === selectedMandalId)
      const villageObj = villages.find((v) => v.id === selectedVillageId)

      onChange({
        stateId: selectedStateId,
        stateName: stateObj?.name,
        districtId: selectedDistrictId,
        districtName: districtObj?.name,
        mandalId: selectedMandalId,
        mandalName: mandalObj?.name,
        villageId: selectedVillageId,
        villageName: villageObj?.name,
      })
    }
  }, [selectedVillageId, selectedStateId, selectedDistrictId, selectedMandalId, states, districts, mandals, villages, onChange])

  // GPS Resolution Flow
  const handleDetectLocation = useCallback(() => {
    if (typeof window === 'undefined' || !navigator.geolocation) {
      setGpsError('Geolocation is not supported by your browser.')
      return
    }

    setGpsLoading(true)
    setGpsError(null)
    setSuggestion(null)

    navigator.geolocation.getCurrentPosition(
      async (pos) => {
        try {
          const lat = Number(pos.coords.latitude.toFixed(6))
          const lon = Number(pos.coords.longitude.toFixed(6))
          const res = await geographyService.resolveLocation(lat, lon)
          setSuggestion(res)
        } catch {
          setGpsError('Could not resolve location with agricultural geography engine. Please select manually below.')
        } finally {
          setGpsLoading(false)
        }
      },
      (err) => {
        setGpsLoading(false)
        if (err.code === 1) {
          setGpsError('Location access was denied. Please select your administrative location manually below.')
        } else if (err.code === 3) {
          setGpsError('Location request timed out. Please select your location manually.')
        } else {
          setGpsError('Unable to retrieve device GPS coordinates. Please select manually.')
        }
      },
      { timeout: 10000, enableHighAccuracy: true }
    )
  }, [])

  // Confirm GPS Candidate
  const handleApplySuggestion = useCallback(() => {
    if (!suggestion) return

    pendingCandidateRef.current = suggestion

    // Match candidate state in loaded states list
    const matchedState = states.find(
      (s) =>
        s.code === suggestion.candidateStateCode ||
        s.name.toLowerCase() === suggestion.candidateStateName?.toLowerCase()
    )

    if (matchedState) {
      if (selectedStateId === matchedState.id) {
        if (suggestion.candidateDistrictCode || suggestion.candidateDistrictName) {
          const matchedDistrict = districts.find(
            (d) =>
              (suggestion.candidateDistrictCode && d.code === suggestion.candidateDistrictCode) ||
              (suggestion.candidateDistrictName && d.name.toLowerCase() === suggestion.candidateDistrictName.toLowerCase())
          )
          if (matchedDistrict) {
            setSelectedDistrictId(matchedDistrict.id)
          } else {
            pendingCandidateRef.current = null
          }
        } else {
          pendingCandidateRef.current = null
        }
      } else {
        setSelectedStateId(matchedState.id)
      }
    } else {
      pendingCandidateRef.current = null
    }

    setSuggestion(null)
  }, [suggestion, states, selectedStateId, districts])

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
      {/* GPS Trigger Header */}
      {showGpsButton && (
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '10px 14px',
            borderRadius: 12,
            background: 'rgba(255, 255, 255, 0.65)',
            border: '1px solid var(--border-soft, #D9E1D8)',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <MapPin size={18} style={{ color: 'var(--canopy-700, #2F5233)' }} />
            <span style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-primary, #1A1408)' }}>
              Administrative Geography
            </span>
          </div>

          <button
            type="button"
            onClick={handleDetectLocation}
            disabled={disabled || gpsLoading}
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: 6,
              padding: '6px 12px',
              borderRadius: 8,
              border: '1px solid rgba(47, 82, 51, 0.25)',
              background: 'rgba(122, 155, 92, 0.12)',
              color: 'var(--canopy-700, #2F5233)',
              fontSize: 12,
              fontWeight: 600,
              cursor: disabled || gpsLoading ? 'not-allowed' : 'pointer',
              fontFamily: 'var(--font-body, "Inter", sans-serif)',
            }}
          >
            {gpsLoading ? (
              <>
                <Loader2 size={13} className="animate-spin" />
                <span>Locating…</span>
              </>
            ) : (
              <>
                <Navigation size={13} />
                <span>Auto-detect via GPS</span>
              </>
            )}
          </button>
        </div>
      )}

      {/* GPS Notice / Error Banner */}
      {gpsError && (
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 8,
            padding: '8px 12px',
            borderRadius: 8,
            background: 'rgba(183, 28, 28, 0.08)',
            border: '1px solid rgba(183, 28, 28, 0.2)',
            color: '#B71C1C',
            fontSize: 12,
          }}
        >
          <AlertCircle size={15} style={{ flexShrink: 0 }} />
          <span>{gpsError}</span>
        </div>
      )}

      {/* GPS Suggested Location Candidate Banner */}
      {suggestion && (
        <div
          style={{
            padding: '12px 14px',
            borderRadius: 12,
            background: 'rgba(242, 200, 75, 0.15)',
            border: '1.5px solid var(--sun-300, #F5D87A)',
            display: 'flex',
            flexDirection: 'column',
            gap: 8,
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <CheckCircle2 size={16} style={{ color: 'var(--canopy-700, #2F5233)' }} />
            <span style={{ fontSize: 13, fontWeight: 700, color: 'var(--text-primary, #1A1408)' }}>
              Suggested Candidate Location
            </span>
          </div>

          <p style={{ margin: 0, fontSize: 12.5, color: 'var(--text-secondary, #6B4E3D)', lineHeight: 1.5 }}>
            {[
              suggestion.candidateVillageName,
              suggestion.candidateMandalName,
              suggestion.candidateDistrictName,
              suggestion.candidateStateName,
            ]
              .filter(Boolean)
              .join(', ')}
          </p>

          <p style={{ margin: 0, fontSize: 11, color: 'var(--text-muted, #9A7562)', fontStyle: 'italic' }}>
            {suggestion.advisoryMessage || 'Advisory GPS resolution. Please confirm your administrative hierarchy.'}
          </p>

          <div style={{ display: 'flex', gap: 8, marginTop: 4 }}>
            <button
              type="button"
              onClick={handleApplySuggestion}
              style={{
                padding: '6px 14px',
                borderRadius: 8,
                background: 'var(--canopy-700, #2F5233)',
                color: '#FFFFFF',
                border: 'none',
                fontSize: 12,
                fontWeight: 600,
                cursor: 'pointer',
              }}
            >
              Confirm and Apply
            </button>
            <button
              type="button"
              onClick={() => setSuggestion(null)}
              style={{
                padding: '6px 12px',
                borderRadius: 8,
                background: 'transparent',
                border: '1px solid var(--border-soft, #D9E1D8)',
                color: 'var(--text-muted, #9A7562)',
                fontSize: 12,
                cursor: 'pointer',
              }}
            >
              Choose Manually
            </button>
          </div>
        </div>
      )}

      {/* Cascading 4-Tier Selectors */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
        {/* 1. State / UT */}
        <div>
          <label style={{ display: 'block', fontSize: 12, fontWeight: 600, color: 'var(--text-secondary, #6B4E3D)', marginBottom: 5 }}>
            State / Union Territory {required && <span style={{ color: '#B71C1C' }}>*</span>}
          </label>
          <select
            value={selectedStateId}
            onChange={(e) => {
              pendingCandidateRef.current = null
              setSelectedStateId(e.target.value)
            }}
            disabled={disabled || loadingStates}
            required={required}
            style={selectStyle}
          >
            <option value="">{loadingStates ? 'Loading states…' : 'Select State / UT'}</option>
            {states.map((s) => (
              <option key={s.id} value={s.id}>
                {s.name}
              </option>
            ))}
          </select>
        </div>

        {/* 2. District */}
        <div>
          <label style={{ display: 'block', fontSize: 12, fontWeight: 600, color: 'var(--text-secondary, #6B4E3D)', marginBottom: 5 }}>
            District {required && <span style={{ color: '#B71C1C' }}>*</span>}
          </label>
          <select
            value={selectedDistrictId}
            onChange={(e) => {
              pendingCandidateRef.current = null
              setSelectedDistrictId(e.target.value)
            }}
            disabled={disabled || !selectedStateId || loadingDistricts}
            required={required}
            style={selectStyle}
          >
            <option value="">
              {!selectedStateId ? 'Select state first' : loadingDistricts ? 'Loading districts…' : 'Select District'}
            </option>
            {districts.map((d) => (
              <option key={d.id} value={d.id}>
                {d.name}
              </option>
            ))}
          </select>
        </div>

        {/* 3. Mandal / Taluk */}
        <div>
          <label style={{ display: 'block', fontSize: 12, fontWeight: 600, color: 'var(--text-secondary, #6B4E3D)', marginBottom: 5 }}>
            Mandal / Taluk {required && <span style={{ color: '#B71C1C' }}>*</span>}
          </label>
          <select
            value={selectedMandalId}
            onChange={(e) => {
              pendingCandidateRef.current = null
              setSelectedMandalId(e.target.value)
            }}
            disabled={disabled || !selectedDistrictId || loadingMandals}
            required={required}
            style={selectStyle}
          >
            <option value="">
              {!selectedDistrictId ? 'Select district first' : loadingMandals ? 'Loading mandals…' : 'Select Mandal'}
            </option>
            {mandals.map((m) => (
              <option key={m.id} value={m.id}>
                {m.name}
              </option>
            ))}
          </select>
        </div>

        {/* 4. Village */}
        <div>
          <label style={{ display: 'block', fontSize: 12, fontWeight: 600, color: 'var(--text-secondary, #6B4E3D)', marginBottom: 5 }}>
            Village {required && <span style={{ color: '#B71C1C' }}>*</span>}
          </label>
          <select
            value={selectedVillageId}
            onChange={(e) => {
              pendingCandidateRef.current = null
              setSelectedVillageId(e.target.value)
            }}
            disabled={disabled || !selectedMandalId || loadingVillages}
            required={required}
            style={selectStyle}
          >
            <option value="">
              {!selectedMandalId ? 'Select mandal first' : loadingVillages ? 'Loading villages…' : 'Select Village'}
            </option>
            {villages.map((v) => (
              <option key={v.id} value={v.id}>
                {v.name}
              </option>
            ))}
          </select>
        </div>
      </div>
    </div>
  )
}

const selectStyle: React.CSSProperties = {
  width: '100%',
  padding: '10px 12px',
  borderRadius: 10,
  border: '1.5px solid var(--border-soft, #D9E1D8)',
  background: '#FFFFFF',
  fontFamily: 'var(--font-body, "Inter", sans-serif)',
  fontSize: 13,
  color: 'var(--text-primary, #1A1408)',
  outline: 'none',
  transition: 'border-color 0.2s ease',
}
