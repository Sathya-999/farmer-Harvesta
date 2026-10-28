'use client'

import React, { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { isAuthenticated, getCurrentUser, AuthUser } from '@/lib/authService'
import { farmerService, FarmerProfileDto } from '@/lib/farmerService'

interface Props {
  children: React.ReactNode
  requireOnboarded?: boolean
}

export default function AuthGuard({ children, requireOnboarded = false }: Props) {
  const router = useRouter()
  const [checking, setChecking] = useState(true)
  const [authed, setAuthed] = useState(false)

  useEffect(() => {
    if (!isAuthenticated()) {
      const currentPath = typeof window !== 'undefined' ? window.location.pathname + window.location.search : ''
      const safeRedirect = currentPath && currentPath !== '/' && currentPath !== '/login'
        ? `/login?redirect=${encodeURIComponent(currentPath)}`
        : '/login'
      router.replace(safeRedirect)
      return
    }

    setAuthed(true)

    if (requireOnboarded) {
      farmerService.getProfile()
        .then((profile) => {
          if (!profile || !profile.onboardingCompleted) {
            router.replace('/onboarding')
          } else {
            setChecking(false)
          }
        })
        .catch(() => {
          // If backend profile check fails, allow access to avoid permanent lockout
          setChecking(false)
        })
    } else {
      setChecking(false)
    }
  }, [router, requireOnboarded])

  if (checking || !authed) {
    return (
      <div
        style={{
          minHeight: '100vh',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          background: 'var(--bg-base, #FBF4E4)',
          fontFamily: 'var(--font-body, "Inter", sans-serif)',
          color: 'var(--canopy-700, #2F5233)',
        }}
      >
        <div style={{ fontSize: 36, marginBottom: 16 }}>🌱</div>
        <p style={{ fontWeight: 600, fontSize: 16, margin: 0 }}>Validating farmer session…</p>
      </div>
    )
  }

  return <>{children}</>
}
