'use client'

import React from 'react'
import { motion } from 'framer-motion'
import { MapPin, Sprout, Activity, ShieldCheck, Sun } from 'lucide-react'

interface Props {
  headline?: string
  subheadline?: string
  tagline?: string
  coordinates?: string
}

export default function AgriculturalHeroPanel({
  headline = 'Your farm.\nBetter informed.',
  subheadline = 'Streamline field telemetry, crop cultivation schedules, and official APMC mandi price indices in one integrated agricultural intelligence operating system.',
  tagline = 'HARVEST(A) AGRI INTELLIGENCE',
  coordinates = '17.3850° N, 78.4867° E',
}: Props) {
  return (
    <div
      className="agricultural-hero-container"
      style={{
        flex: '0 0 50%',
        width: '50%',
        height: '100%',
        position: 'relative',
        overflow: 'hidden',
        background: '#163A24',
      }}
    >
      {/* High-Resolution Aerial Farmland Photograph */}
      <img
        src="/aerial_farmland.jpg"
        alt="Aerial drone photography of Indian agricultural farm plots with crop field boundaries"
        style={{
          display: 'block',
          width: '100%',
          height: '100%',
          objectFit: 'cover',
          objectPosition: 'center center',
          filter: 'brightness(0.88) contrast(1.08)',
        }}
      />

      {/* Multi-Stage Dark-Forest & Golden-Hour Vignette Gradient */}
      <div
        style={{
          position: 'absolute',
          inset: 0,
          background:
            'linear-gradient(180deg, rgba(22, 58, 36, 0.4) 0%, rgba(36, 68, 42, 0.35) 40%, rgba(26, 20, 8, 0.9) 100%)',
        }}
        aria-hidden="true"
      />

      {/* Atmospheric Telemetry Badge at Top-Right */}
      <div
        style={{
          position: 'absolute',
          top: 28,
          right: 32,
          zIndex: 2,
          display: 'flex',
          alignItems: 'center',
          gap: 8,
          background: 'rgba(22, 58, 36, 0.85)',
          backdropFilter: 'blur(12px)',
          padding: '6px 14px',
          borderRadius: 999,
          border: '1px solid rgba(223, 255, 40, 0.25)',
          boxShadow: '0 4px 16px rgba(0, 0, 0, 0.25)',
        }}
      >
        <span
          style={{
            width: 7,
            height: 7,
            borderRadius: '50%',
            background: '#DFFF28',
            boxShadow: '0 0 10px #DFFF28',
            display: 'inline-block',
          }}
        />
        <span
          style={{
            fontFamily: 'var(--font-body, "Inter", sans-serif)',
            fontSize: '11px',
            color: '#FBF4E4',
            fontWeight: 700,
            letterSpacing: '0.06em',
            textTransform: 'uppercase',
          }}
        >
          {tagline}
        </span>
      </div>

      {/* Coordinates / Field Survey Details at Top-Left */}
      <div
        style={{
          position: 'absolute',
          top: 28,
          left: 32,
          zIndex: 2,
          display: 'flex',
          alignItems: 'center',
          gap: 6,
          background: 'rgba(26, 20, 8, 0.65)',
          backdropFilter: 'blur(10px)',
          padding: '5px 12px',
          borderRadius: 999,
          border: '1px solid rgba(255, 255, 255, 0.15)',
          color: 'rgba(251, 244, 228, 0.85)',
          fontFamily: 'var(--font-mono, monospace)',
          fontSize: '11px',
        }}
      >
        <MapPin size={12} color="#DFFF28" />
        <span>Demo Coordinates: {coordinates}</span>
      </div>

      {/* Floating Field Boundary Accent Overlay */}
      <div
        style={{
          position: 'absolute',
          top: '35%',
          left: '12%',
          zIndex: 2,
          background: 'rgba(22, 58, 36, 0.75)',
          backdropFilter: 'blur(10px)',
          border: '1px solid rgba(223, 255, 40, 0.3)',
          borderRadius: 14,
          padding: '10px 14px',
          display: 'flex',
          alignItems: 'center',
          gap: 10,
          boxShadow: '0 8px 24px rgba(0, 0, 0, 0.25)',
        }}
      >
        <div
          style={{
            width: 28,
            height: 28,
            borderRadius: 8,
            background: '#24442A',
            color: '#DFFF28',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          <Activity size={15} />
        </div>
        <div>
          <div style={{ fontSize: '11px', color: '#DFFF28', fontWeight: 700 }}>
            Demo Telemetry Sample
          </div>
          <div style={{ fontSize: '13px', color: '#FFFFFF', fontWeight: 800 }}>
            NDVI 0.76 (Representative)
          </div>
        </div>
      </div>

      {/* Bottom Content Narrative */}
      <motion.div
        initial={{ opacity: 0, y: 18 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.55, delay: 0.1, ease: [0.22, 1, 0.36, 1] }}
        style={{
          position: 'absolute',
          bottom: 0,
          left: 0,
          right: 0,
          padding: '0 48px 44px',
          zIndex: 2,
        }}
      >
        {/* Metric Badges */}
        <div
          style={{
            display: 'flex',
            gap: 10,
            marginBottom: 18,
            flexWrap: 'wrap',
          }}
        >
          <div
            style={{
              padding: '5px 12px',
              borderRadius: 8,
              background: 'rgba(255, 255, 255, 0.12)',
              backdropFilter: 'blur(8px)',
              border: '1px solid rgba(255, 255, 255, 0.2)',
              color: '#FFFFFF',
              fontFamily: 'var(--font-body, "Inter", sans-serif)',
              fontSize: '12px',
              fontWeight: 600,
              display: 'flex',
              alignItems: 'center',
              gap: 6,
            }}
          >
            <ShieldCheck size={14} color="#DFFF28" />
            <span>Platform: Plot Management</span>
          </div>
          <div
            style={{
              padding: '5px 12px',
              borderRadius: 8,
              background: 'rgba(255, 255, 255, 0.12)',
              backdropFilter: 'blur(8px)',
              border: '1px solid rgba(255, 255, 255, 0.2)',
              color: '#FFFFFF',
              fontFamily: 'var(--font-body, "Inter", sans-serif)',
              fontSize: '12px',
              fontWeight: 600,
              display: 'flex',
              alignItems: 'center',
              gap: 6,
            }}
          >
            <Sun size={14} color="#DFFF28" />
            <span>Platform: Irrigation Advisories</span>
          </div>
        </div>

        {/* Headline */}
        <h2
          style={{
            fontFamily: 'var(--font-serif, "Fraunces", Georgia, serif)',
            fontSize: 'clamp(30px, 3.2vw, 44px)',
            fontWeight: 800,
            color: '#FFFFFF',
            lineHeight: 1.12,
            letterSpacing: '-0.025em',
            margin: '0 0 14px',
            whiteSpace: 'pre-line',
            textShadow: '0 2px 20px rgba(0, 0, 0, 0.45)',
          }}
        >
          {headline}
        </h2>

        {/* Brand Accent Bar */}
        <div
          style={{
            width: 48,
            height: 3.5,
            background: 'linear-gradient(90deg, #DFFF28, #71844A)',
            borderRadius: 2,
            marginBottom: 16,
          }}
        />

        {/* Subtitle / Description */}
        <p
          style={{
            fontFamily: 'var(--font-body, "Inter", sans-serif)',
            fontSize: '14.5px',
            color: 'rgba(251, 244, 228, 0.92)',
            lineHeight: 1.6,
            margin: 0,
            maxWidth: 460,
            textShadow: '0 1px 6px rgba(0, 0, 0, 0.35)',
          }}
        >
          {subheadline}
        </p>
      </motion.div>
    </div>
  )
}
