'use client'

import React from 'react'
import Link from 'next/link'
import { ArrowLeft } from 'lucide-react'
import AgriculturalHeroPanel from './AgriculturalHeroPanel'

interface Props {
  children: React.ReactNode
  headline?: string
  subheadline?: string
  tagline?: string
  badgeText?: string
}

export default function AuthSplitLayout({
  children,
  headline = 'Your farm.\nBetter informed.',
  subheadline = 'Streamline field telemetry, crop cultivation schedules, and official APMC mandi price indices in one integrated agricultural intelligence operating system.',
  tagline = 'HARVEST(A) AGRI INTELLIGENCE',
  badgeText = 'Farmer Portal',
}: Props) {
  return (
    <div
      style={{
        display: 'flex',
        width: '100vw',
        height: '100dvh',
        overflow: 'hidden',
        background: '#FAF8F0',
        margin: 0,
        padding: 0,
      }}
    >
      {/* ── Left Side on Desktop: Visual Agricultural Brand Panel ── */}
      <AgriculturalHeroPanel
        headline={headline}
        subheadline={subheadline}
        tagline={tagline}
      />

      {/* ── Right Side: Form Panel (100% on mobile, 50% on desktop) ── */}
      <div
        className="agricultural-auth-side"
        style={{
          flex: '0 0 50%',
          width: '50%',
          height: '100%',
          display: 'flex',
          flexDirection: 'column',
          overflowY: 'auto',
          overflowX: 'hidden',
          position: 'relative',
          background: '#FAF8F0',
        }}
      >
        {/* Navigation Bar at Top */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '24px clamp(20px, 3.5vw, 40px) 0',
          }}
        >
          <Link
            href="/"
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: 8,
              color: '#55594C',
              fontFamily: 'var(--font-body, "Inter", sans-serif)',
              fontSize: '13.5px',
              fontWeight: 600,
              textDecoration: 'none',
              transition: 'color 0.15s ease',
            }}
          >
            <ArrowLeft size={16} />
            <span>Back to Home</span>
          </Link>

          <span
            style={{
              fontSize: '12px',
              fontWeight: 700,
              color: '#24442A',
              background: 'rgba(36, 68, 42, 0.08)',
              border: '1px solid rgba(36, 68, 42, 0.15)',
              padding: '4px 12px',
              borderRadius: 999,
              fontFamily: 'var(--font-body, "Inter", sans-serif)',
              letterSpacing: '0.02em',
            }}
          >
            {badgeText}
          </span>
        </div>

        {/* Scrollable Form Body Container */}
        <div
          className="agricultural-auth-container"
          style={{
            flex: 1,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '28px clamp(20px, 3.5vw, 40px) 40px',
            boxSizing: 'border-box',
          }}
        >
          <div
            style={{
              width: '100%',
              maxWidth: 440,
              boxSizing: 'border-box',
            }}
          >
            {children}
          </div>
        </div>
      </div>
    </div>
  )
}
