'use client'

import React, { useState, useEffect } from 'react'
import Link from 'next/link'
import { motion } from 'framer-motion'
import { farmerService, FarmerProfileDto } from '@/lib/farmerService'
import AppLayout from '@/components/ui/AppLayout'
import GeographySelector, { SelectedGeography } from '@/components/geography/GeographySelector'
import {
  User,
  MapPin,
  ShieldCheck,
  Edit2,
  Check,
  AlertCircle,
  Clock,
  Phone,
  FileCheck2,
} from 'lucide-react'
import { getCurrentUser } from '@/lib/authService'

export default function ProfilePage() {
  const [profile, setProfile] = useState<FarmerProfileDto | null>(null)
  const [loading, setLoading] = useState(true)
  const [isEditing, setIsEditing] = useState(false)

  // Edit form state
  const [editName, setEditName] = useState('')
  const [editGeo, setEditGeo] = useState<Partial<SelectedGeography> | null>(null)
  const [saving, setSaving] = useState(false)
  const [errorMsg, setErrorMsg] = useState<string | null>(null)
  const [successMsg, setSuccessMsg] = useState<string | null>(null)

  const currentUser = getCurrentUser()

  const loadProfile = async () => {
    setLoading(true)
    setErrorMsg(null)
    try {
      const data = await farmerService.getProfile()
      setProfile(data)
      if (data) {
        setEditName(data.fullName)
        setEditGeo({
          villageId: data.homeVillageId,
          villageName: data.villageName || data.homeVillageName,
          mandalId: data.mandalId,
          mandalName: data.mandalName,
          districtId: data.districtId,
          districtName: data.districtName,
          stateId: data.stateId,
          stateName: data.stateName,
        })
      }
    } catch (err: any) {
      console.error('Failed to load farmer profile:', err)
      setErrorMsg(err.message || 'Failed to retrieve profile record.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadProfile()
  }, [])

  const handleUpdate = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!profile) return
    setErrorMsg(null)
    setSuccessMsg(null)

    if (!editName.trim()) {
      setErrorMsg('Full name cannot be empty.')
      return
    }

    if (!editGeo?.villageId) {
      setErrorMsg('Please select a verified administrative home village.')
      return
    }

    setSaving(true)
    try {
      const updated = await farmerService.updateProfile({
        fullName: editName.trim(),
        homeVillageId: editGeo.villageId,
        expectedVersion: profile.version,
      })

      setProfile(updated)
      setIsEditing(false)
      setSuccessMsg('Farmer profile successfully updated.')
    } catch (err: any) {
      console.error('Failed to update profile:', err)
      setErrorMsg(
        err.message ||
          'Failed to update profile. A concurrent modification may have occurred; please reload.'
      )
    } finally {
      setSaving(false)
    }
  }

  return (
    <AppLayout>
      <div style={{ maxWidth: 760, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 24 }}>
        {/* Header */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 16 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
            <div
              style={{
                width: 48,
                height: 48,
                borderRadius: 16,
                background: 'rgba(47, 82, 51, 0.12)',
                color: 'var(--canopy-700, #2F5233)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <User size={24} />
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
                Farmer Profile & Identity
              </h1>
              <p style={{ margin: '2px 0 0', fontSize: 13, color: 'var(--text-muted, #7A6552)' }}>
                Verified agricultural identity and administrative residential registration.
              </p>
            </div>
          </div>

          {!loading && profile && !isEditing && (
            <button
              onClick={() => setIsEditing(true)}
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: 6,
                padding: '9px 16px',
                borderRadius: 10,
                border: '1.5px solid var(--border-soft, #D9E1D8)',
                background: '#fff',
                color: 'var(--canopy-900, #163A24)',
                fontSize: 13,
                fontWeight: 600,
                cursor: 'pointer',
              }}
            >
              <Edit2 size={14} />
              <span>Edit Profile</span>
            </button>
          )}
        </div>

        {/* Notifications */}
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

        {successMsg && (
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 10,
              padding: '12px 16px',
              borderRadius: 12,
              background: 'rgba(34, 197, 94, 0.08)',
              border: '1px solid rgba(34, 197, 94, 0.25)',
              color: '#15803D',
              fontSize: 13,
            }}
          >
            <Check size={18} />
            <div>{successMsg}</div>
          </div>
        )}

        {loading ? (
          <div style={{ textAlign: 'center', padding: '60px 0', color: 'var(--text-muted, #7A6552)' }}>
            Retrieving authenticated farmer identity...
          </div>
        ) : !profile ? (
          <div
            style={{
              borderRadius: 20,
              background: '#fff',
              border: '1.5px solid var(--border-soft, #D9E1D8)',
              padding: 40,
              textAlign: 'center',
            }}
          >
            <User size={36} color="var(--text-muted, #9A7562)" style={{ margin: '0 auto 12px' }} />
            <h3 style={{ margin: '0 0 8px', color: 'var(--canopy-900, #163A24)' }}>Profile Not Initialized</h3>
            <p style={{ margin: '0 0 16px', fontSize: 13, color: 'var(--text-muted, #7A6552)' }}>
              Complete the first-time farmer onboarding process to activate your profile.
            </p>
            <Link
              href="/onboarding"
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
              Start Onboarding
            </Link>
          </div>
        ) : isEditing ? (
          /* Profile Edit Mode */
          <motion.form
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            onSubmit={handleUpdate}
            style={{
              borderRadius: 22,
              background: 'rgba(255, 255, 255, 0.94)',
              backdropFilter: 'blur(16px)',
              border: '1.5px solid var(--border-soft, #D9E1D8)',
              padding: 32,
              display: 'flex',
              flexDirection: 'column',
              gap: 20,
              boxShadow: '0 8px 30px rgba(26, 20, 8, 0.05)',
            }}
          >
            <h3 style={{ margin: 0, fontFamily: 'var(--font-serif)', fontSize: 20, color: 'var(--canopy-900, #163A24)' }}>
              Edit Farmer Information
            </h3>

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
                Farmer Full Name <span style={{ color: '#DC2626' }}>*</span>
              </label>
              <input
                type="text"
                value={editName}
                onChange={(e) => setEditName(e.target.value)}
                required
                style={{
                  width: '100%',
                  padding: '11px 14px',
                  borderRadius: 12,
                  border: '1.5px solid var(--border-soft, #D9E1D8)',
                  background: '#fff',
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
                  marginBottom: 10,
                }}
              >
                Home Village Residence
              </label>
              <GeographySelector
                value={editGeo || undefined}
                onChange={(geo) => setEditGeo(geo)}
              />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 12, marginTop: 10 }}>
              <button
                type="button"
                onClick={() => {
                  setIsEditing(false)
                  setErrorMsg(null)
                }}
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
                disabled={saving}
                style={{
                  padding: '10px 22px',
                  borderRadius: 10,
                  border: 'none',
                  background: 'var(--canopy-800, #224229)',
                  color: '#fff',
                  fontSize: 13,
                  fontWeight: 600,
                  cursor: saving ? 'not-allowed' : 'pointer',
                }}
              >
                {saving ? 'Saving...' : 'Save Profile Changes'}
              </button>
            </div>
          </motion.form>
        ) : (
          /* Profile View Mode */
          <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            style={{
              borderRadius: 22,
              background: 'rgba(255, 255, 255, 0.94)',
              backdropFilter: 'blur(16px)',
              border: '1.5px solid var(--border-soft, #D9E1D8)',
              padding: 32,
              display: 'flex',
              flexDirection: 'column',
              gap: 24,
              boxShadow: '0 8px 30px rgba(26, 20, 8, 0.05)',
            }}
          >
            {/* Identity Status Pill */}
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 12 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <span
                  style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: 6,
                    padding: '6px 12px',
                    borderRadius: 999,
                    background: 'rgba(34, 197, 94, 0.1)',
                    color: '#15803D',
                    fontSize: 12,
                    fontWeight: 700,
                    border: '1px solid rgba(34, 197, 94, 0.3)',
                  }}
                >
                  <ShieldCheck size={14} />
                  <span>Profile Status: {profile.profileStatus}</span>
                </span>

                {profile.onboardingCompleted && (
                  <span
                    style={{
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: 4,
                      padding: '6px 12px',
                      borderRadius: 999,
                      background: 'rgba(47, 82, 51, 0.08)',
                      color: 'var(--canopy-800, #224229)',
                      fontSize: 12,
                      fontWeight: 600,
                    }}
                  >
                    <FileCheck2 size={14} />
                    <span>Onboarded</span>
                  </span>
                )}
              </div>

              <div style={{ fontSize: 12, color: 'var(--text-muted, #7A6552)', fontFamily: 'monospace' }}>
                Version: v{profile.version}
              </div>
            </div>

            {/* Profile Fields List */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: 20 }}>
              <div style={{ padding: 16, borderRadius: 14, background: '#FAF8F5', border: '1px solid var(--border-soft, #D9E1D8)' }}>
                <span style={{ fontSize: 12, color: 'var(--text-muted, #7A6552)', display: 'block', marginBottom: 4 }}>
                  Full Legal Name
                </span>
                <span style={{ fontSize: 17, fontWeight: 700, color: 'var(--canopy-900, #163A24)' }}>
                  {profile.fullName}
                </span>
              </div>

              <div style={{ padding: 16, borderRadius: 14, background: '#FAF8F5', border: '1px solid var(--border-soft, #D9E1D8)' }}>
                <span style={{ fontSize: 12, color: 'var(--text-muted, #7A6552)', display: 'block', marginBottom: 4 }}>
                  Authenticated Mobile / Identity
                </span>
                <span style={{ fontSize: 15, fontWeight: 600, color: 'var(--canopy-900, #163A24)' }}>
                  {currentUser?.phone || currentUser?.email || 'Authenticated Session'}
                </span>
              </div>
            </div>

            {/* Administrative Residence Card */}
            <div
              style={{
                padding: 20,
                borderRadius: 16,
                background: 'rgba(47, 82, 51, 0.04)',
                border: '1px solid rgba(47, 82, 51, 0.12)',
                display: 'flex',
                flexDirection: 'column',
                gap: 12,
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <MapPin size={18} color="var(--canopy-700, #2F5233)" />
                <h4 style={{ margin: 0, fontSize: 15, fontWeight: 700, color: 'var(--canopy-900, #163A24)' }}>
                  Administrative Residential Jurisdiction
                </h4>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))', gap: 12, fontSize: 13 }}>
                <div>
                  <span style={{ color: 'var(--text-muted, #7A6552)', display: 'block', fontSize: 11 }}>Village</span>
                  <strong style={{ color: 'var(--canopy-900, #163A24)' }}>
                    {profile.villageName || profile.homeVillageName || '—'}
                  </strong>
                </div>

                <div>
                  <span style={{ color: 'var(--text-muted, #7A6552)', display: 'block', fontSize: 11 }}>Mandal / Tehsil</span>
                  <strong style={{ color: 'var(--canopy-900, #163A24)' }}>{profile.mandalName || '—'}</strong>
                </div>

                <div>
                  <span style={{ color: 'var(--text-muted, #7A6552)', display: 'block', fontSize: 11 }}>District</span>
                  <strong style={{ color: 'var(--canopy-900, #163A24)' }}>{profile.districtName || '—'}</strong>
                </div>

                <div>
                  <span style={{ color: 'var(--text-muted, #7A6552)', display: 'block', fontSize: 11 }}>State</span>
                  <strong style={{ color: 'var(--canopy-900, #163A24)' }}>{profile.stateName || '—'}</strong>
                </div>
              </div>
            </div>
          </motion.div>
        )}
      </div>
    </AppLayout>
  )
}
