'use client'

import React, { useState } from 'react'
import { useRouter } from 'next/navigation'
import Link from 'next/link'
import { motion } from 'framer-motion'
import { plotService } from '@/lib/plotService'
import AppLayout from '@/components/ui/AppLayout'
import GeographySelector, { SelectedGeography } from '@/components/geography/GeographySelector'
import { MapPin, ArrowLeft, Check, AlertCircle, Maximize2 } from 'lucide-react'

export default function NewPlotPage() {
  const router = useRouter()
  const [label, setLabel] = useState('')
  const [totalAreaAcres, setTotalAreaAcres] = useState('')
  const [geo, setGeo] = useState<SelectedGeography | null>(null)
  const [loading, setLoading] = useState(false)
  const [errorMsg, setErrorMsg] = useState<string | null>(null)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setErrorMsg(null)

    const areaNum = parseFloat(totalAreaAcres)
    if (isNaN(areaNum) || areaNum <= 0) {
      setErrorMsg('Total plot area must be a positive number greater than 0 acres.')
      return
    }

    if (!geo || !geo.villageId) {
      setErrorMsg('Please select the administrative village where this plot is physically located.')
      return
    }

    setLoading(true)

    try {
      await plotService.createPlot({
        label: label.trim() || undefined,
        totalAreaAcres: areaNum,
        villageId: geo.villageId,
      })

      router.push('/plots')
    } catch (err: any) {
      setErrorMsg(err.message || 'Failed to register plot holding. Please check field inputs.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <AppLayout>
      <div style={{ maxWidth: 680, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 20 }}>
        {/* Back Link */}
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
          <span>Back to My Plots</span>
        </Link>

        {/* Form Container */}
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
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 6 }}>
            <div
              style={{
                width: 38,
                height: 38,
                borderRadius: 12,
                background: 'rgba(47, 82, 51, 0.12)',
                color: 'var(--canopy-700, #2F5233)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <MapPin size={20} />
            </div>
            <div>
              <h1 style={{ fontFamily: 'var(--font-display, "Plus Jakarta Sans", sans-serif)', fontSize: 22, fontWeight: 700, color: 'var(--text-primary, #1A1408)', margin: 0 }}>
                Register New Agricultural Plot
              </h1>
              <p style={{ margin: '2px 0 0', fontSize: 13, color: 'var(--text-muted, #9A7562)' }}>
                Declare a land parcel for cultivation cycle planning and localized field intelligence.
              </p>
            </div>
          </div>

          {errorMsg && (
            <div
              id="plot-form-error"
              style={{
                marginTop: 18,
                padding: '12px 14px',
                borderRadius: 10,
                background: 'rgba(183, 28, 28, 0.08)',
                border: '1px solid rgba(183, 28, 28, 0.25)',
                color: '#B71C1C',
                fontSize: 13,
                display: 'flex',
                alignItems: 'center',
                gap: 8,
              }}
            >
              <AlertCircle size={16} style={{ flexShrink: 0 }} />
              <span>{errorMsg}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} noValidate style={{ marginTop: 24, display: 'flex', flexDirection: 'column', gap: 20 }}>
            {/* Plot Label */}
            <div>
              <label style={{ display: 'block', fontSize: 13, fontWeight: 600, color: 'var(--text-primary, #1A1408)', marginBottom: 6 }}>
                Plot Label / Local Name <span style={{ color: 'var(--text-muted, #9A7562)', fontWeight: 400 }}>(Optional)</span>
              </label>
              <input
                type="text"
                value={label}
                onChange={(e) => setLabel(e.target.value)}
                placeholder="e.g. North Riverbank Field, Plot 3, Tamarind Well Parcel"
                maxLength={100}
                style={inputStyle}
              />
              <p style={{ margin: '4px 0 0', fontSize: 11.5, color: 'var(--text-muted, #9A7562)' }}>
                Unique name to identify this parcel in your records and cultivation plans.
              </p>
            </div>

            {/* Total Area */}
            <div>
              <label style={{ display: 'block', fontSize: 13, fontWeight: 600, color: 'var(--text-primary, #1A1408)', marginBottom: 6 }}>
                Total Plot Area (Acres) <span style={{ color: '#B71C1C' }}>*</span>
              </label>
              <div style={{ position: 'relative' }}>
                <Maximize2
                  size={16}
                  style={{
                    position: 'absolute',
                    left: 14,
                    top: '50%',
                    transform: 'translateY(-50%)',
                    color: 'var(--text-muted, #9A7562)',
                  }}
                />
                <input
                  type="number"
                  step="0.01"
                  min="0.01"
                  max="10000"
                  value={totalAreaAcres}
                  onChange={(e) => setTotalAreaAcres(e.target.value)}
                  placeholder="e.g. 4.50"
                  required
                  style={{ ...inputStyle, paddingLeft: 42 }}
                />
              </div>
              <p style={{ margin: '4px 0 0', fontSize: 11.5, color: 'var(--text-muted, #9A7562)' }}>
                Standard operational measurement in acres (1 acre = 4,047 sq meters ≈ 0.404 hectare).
              </p>
            </div>

            {/* Plot Location Selector */}
            <div style={{ borderTop: '1px solid var(--border-soft, #D9E1D8)', paddingTop: 18 }}>
              <p style={{ fontSize: 13, fontWeight: 700, color: 'var(--text-primary, #1A1408)', margin: '0 0 10px' }}>
                Plot Geographic Location <span style={{ color: '#B71C1C' }}>*</span>
              </p>
              <GeographySelector onChange={setGeo} required />
              <p style={{ margin: '8px 0 0', fontSize: 11.5, color: 'var(--text-muted, #9A7562)' }}>
                Plots may physically reside in any village, regardless of whether it matches your home residential village.
              </p>
            </div>

            {/* Submit Action */}
            <div style={{ display: 'flex', gap: 12, marginTop: 12 }}>
              <button
                type="submit"
                disabled={loading}
                style={{
                  flex: 1,
                  display: 'inline-flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: 8,
                  padding: '13px 20px',
                  borderRadius: 12,
                  background: 'linear-gradient(140deg, #163A24 0%, #2E7D32 100%)',
                  color: '#FFFFFF',
                  fontSize: 14.5,
                  fontWeight: 700,
                  border: 'none',
                  cursor: loading ? 'not-allowed' : 'pointer',
                  boxShadow: '0 4px 14px rgba(46, 125, 50, 0.3)',
                }}
              >
                {loading ? 'Creating Plot Record…' : 'Register Plot Holding'}
              </button>

              <Link
                href="/plots"
                style={{
                  padding: '13px 18px',
                  borderRadius: 12,
                  border: '1.5px solid var(--border-soft, #D9E1D8)',
                  background: 'transparent',
                  color: 'var(--text-muted, #9A7562)',
                  fontSize: 14,
                  fontWeight: 600,
                  textDecoration: 'none',
                  display: 'inline-flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              >
                Cancel
              </Link>
            </div>
          </form>
        </motion.div>
      </div>
    </AppLayout>
  )
}

const inputStyle: React.CSSProperties = {
  width: '100%',
  boxSizing: 'border-box',
  padding: '11px 14px',
  borderRadius: 10,
  border: '1.5px solid var(--border-soft, #D9E1D8)',
  background: '#FFFFFF',
  fontSize: 14,
  color: 'var(--text-primary, #1A1408)',
  outline: 'none',
}
