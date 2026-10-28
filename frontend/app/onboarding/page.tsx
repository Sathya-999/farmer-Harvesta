'use client'

import React, { useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { motion } from 'framer-motion'
import { farmerService } from '@/lib/farmerService'
import { getCurrentUser, isAuthenticated } from '@/lib/authService'
import GeographySelector, { SelectedGeography } from '@/components/geography/GeographySelector'
import AgriculturalBrandLogo from '@/components/auth/AgriculturalBrandLogo'
import { Sprout, Check, AlertCircle, ArrowRight, User } from 'lucide-react'

export default function OnboardingPage() {
  const router = useRouter()
  const [fullName, setFullName] = useState('')
  const [geo, setGeo] = useState<SelectedGeography | null>(null)
  const [loading, setLoading] = useState(false)
  const [errorMsg, setErrorMsg] = useState('')

  // Prefill name from authenticated user session
  useEffect(() => {
    if (!isAuthenticated()) {
      router.replace('/login')
      return
    }

    const u = getCurrentUser()
    if (u) {
      setFullName(`${u.firstName || ''} ${u.lastName || ''}`.trim())
    }

    // Check if farmer is already onboarded
    farmerService
      .getProfile()
      .then((p) => {
        if (p && p.onboardingCompleted) {
          router.replace('/dashboard')
        }
      })
      .catch(() => undefined)
  }, [router])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setErrorMsg('')

    if (!fullName.trim()) {
      setErrorMsg('Please enter your full name as recognized in land documents.')
      return
    }

    if (!geo || !geo.villageId) {
      setErrorMsg('Please select your residential home village from the geography selector.')
      return
    }

    setLoading(true)

    try {
      await farmerService.initProfile({
        fullName: fullName.trim(),
        homeVillageId: geo.villageId,
        mandalId: geo.mandalId,
        districtId: geo.districtId,
        stateId: geo.stateId,
      })

      router.push('/dashboard')
    } catch (err: any) {
      setErrorMsg(err.message || 'Failed to initialize farmer profile. Please verify your details.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div
      style={{
        minHeight: '100svh',
        background: 'linear-gradient(180deg, #FBF4E4 0%, #FEFCF6 100%)',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        padding: '40px 20px',
        fontFamily: 'var(--font-body, "Inter", sans-serif)',
      }}
    >
      {/* Brand Header */}
      <div style={{ marginBottom: 32 }}>
        <AgriculturalBrandLogo size="large" showLink={false} subtitle="Farmer Onboarding" />
      </div>

      <motion.div
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        style={{
          width: '100%',
          maxWidth: 620,
          background: 'rgba(255, 255, 255, 0.92)',
          backdropFilter: 'blur(14px)',
          borderRadius: 24,
          border: '1.5px solid var(--border-soft, #D9E1D8)',
          boxShadow: '0 12px 36px rgba(26, 20, 8, 0.08)',
          padding: '36px clamp(20px, 4vw, 40px)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 8 }}>
          <div
            style={{
              width: 36,
              height: 36,
              borderRadius: 10,
              background: 'rgba(122, 155, 92, 0.18)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: 'var(--canopy-700, #2F5233)',
            }}
          >
            <Sprout size={20} />
          </div>
          <h1
            style={{
              fontFamily: 'var(--font-display, "Plus Jakarta Sans", sans-serif)',
              fontSize: 22,
              fontWeight: 700,
              color: 'var(--text-primary, #1A1408)',
              margin: 0,
            }}
          >
            Complete Your Farmer Profile
          </h1>
        </div>

        <p style={{ fontSize: 13.5, color: 'var(--text-secondary, #6B4E3D)', margin: '0 0 24px', lineHeight: 1.5 }}>
          Set up your official farmer profile and primary home location to enable precision plot management and regional crop advisories.
        </p>

        {errorMsg && (
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 8,
              padding: '12px 14px',
              borderRadius: 10,
              background: 'rgba(183, 28, 28, 0.08)',
              border: '1px solid rgba(183, 28, 28, 0.25)',
              color: '#B71C1C',
              fontSize: 13,
              marginBottom: 20,
            }}
          >
            <AlertCircle size={16} style={{ flexShrink: 0 }} />
            <span>{errorMsg}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
          {/* Full Name */}
          <div>
            <label
              style={{
                display: 'block',
                fontSize: 13,
                fontWeight: 600,
                color: 'var(--text-primary, #1A1408)',
                marginBottom: 6,
              }}
            >
              Farmer Full Name <span style={{ color: '#B71C1C' }}>*</span>
            </label>
            <div style={{ position: 'relative' }}>
              <User
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
                type="text"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                placeholder="e.g. Ramesh Kumar Patel"
                required
                style={{
                  width: '100%',
                  boxSizing: 'border-box',
                  padding: '12px 14px 12px 42px',
                  borderRadius: 10,
                  border: '1.5px solid var(--border-soft, #D9E1D8)',
                  background: '#FFFFFF',
                  fontSize: 14,
                  color: 'var(--text-primary, #1A1408)',
                  outline: 'none',
                }}
              />
            </div>
            <p style={{ margin: '4px 0 0', fontSize: 11.5, color: 'var(--text-muted, #9A7562)' }}>
              Name as it appears on your Aadhaar card or PM-KISAN registration.
            </p>
          </div>

          {/* Home Village Hierarchy */}
          <div style={{ borderTop: '1px solid var(--border-soft, #D9E1D8)', paddingTop: 18 }}>
            <p style={{ fontSize: 13, fontWeight: 700, color: 'var(--text-primary, #1A1408)', margin: '0 0 10px' }}>
              Home Village & Residence
            </p>
            <GeographySelector onChange={setGeo} required />
          </div>

          {/* Submit CTA */}
          <div style={{ marginTop: 12 }}>
            <button
              type="submit"
              disabled={loading}
              style={{
                width: '100%',
                display: 'inline-flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: 8,
                padding: '14px 20px',
                borderRadius: 12,
                background: 'linear-gradient(140deg, #163A24 0%, #2E7D32 100%)',
                color: '#FFFFFF',
                fontSize: 15,
                fontWeight: 700,
                border: 'none',
                cursor: loading ? 'not-allowed' : 'pointer',
                boxShadow: '0 4px 14px rgba(46, 125, 50, 0.3)',
              }}
            >
              {loading ? (
                <span>Initializing Profile…</span>
              ) : (
                <>
                  <span>Complete Onboarding & Enter Portal</span>
                  <ArrowRight size={18} />
                </>
              )}
            </button>
          </div>
        </form>
      </motion.div>
    </div>
  )
}
