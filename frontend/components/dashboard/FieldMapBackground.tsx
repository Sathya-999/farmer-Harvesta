'use client'

import React from 'react'
import Image from 'next/image'

export default function FieldMapBackground() {
  return (
    <div
      style={{
        position: 'absolute',
        inset: 0,
        zIndex: 0,
        overflow: 'hidden',
        pointerEvents: 'none',
      }}
    >
      {/* ── Aerial Farmland Photo (Rich Sunny Green Fields) ── */}
      <Image
        src="/aerial_farmland.jpg"
        alt="Aerial Farmland Canvas"
        fill
        style={{
          objectFit: 'cover',
          objectPosition: 'center 45%',
          filter: 'brightness(0.96) contrast(1.08) saturate(1.15)',
        }}
        priority
      />

      {/* ── Subtle Ambient Light Overlay ── */}
      <div
        style={{
          position: 'absolute',
          inset: 0,
          background: 'radial-gradient(circle at 60% 40%, rgba(20, 50, 25, 0.1) 0%, rgba(15, 35, 20, 0.4) 100%)',
        }}
      />

      {/* ── Topographic Contour Scan Lines with Center Target Dots matching the 3 fields ── */}
      <svg
        width="100%"
        height="100%"
        style={{ position: 'absolute', inset: 0, pointerEvents: 'none' }}
        viewBox="0 0 1200 800"
        preserveAspectRatio="xMidYMid slice"
      >
        {/* Field Plot 1 (Top Center-Right): Concentric rounded contour rings + center dot */}
        <g transform="translate(640, 220)">
          <circle cx="0" cy="0" r="14" fill="none" stroke="rgba(255, 255, 255, 0.55)" strokeWidth="1.2" />
          <circle cx="0" cy="0" r="28" fill="none" stroke="rgba(255, 255, 255, 0.45)" strokeWidth="1.2" />
          <circle cx="0" cy="0" r="44" fill="none" stroke="rgba(255, 255, 255, 0.38)" strokeWidth="1.2" />
          <circle cx="0" cy="0" r="62" fill="none" stroke="rgba(255, 255, 255, 0.32)" strokeWidth="1.2" />
          <circle cx="0" cy="0" r="82" fill="none" stroke="rgba(255, 255, 255, 0.25)" strokeWidth="1.2" />
          <circle cx="0" cy="0" r="105" fill="none" stroke="rgba(255, 255, 255, 0.18)" strokeWidth="1.2" />
          {/* Target Center Dot */}
          <circle cx="0" cy="0" r="4" fill="#FFFFFF" />
          <circle cx="0" cy="0" r="8" fill="none" stroke="#FFFFFF" strokeWidth="1.5" />
        </g>

        {/* Field Plot 2 (Center Diamond/Field): Angular radiating contour lines + center dot */}
        <g transform="translate(560, 470)">
          <path d="M 0 -15 L 18 0 L 0 18 L -18 0 Z" fill="none" stroke="rgba(255, 255, 255, 0.55)" strokeWidth="1.2" />
          <path d="M 0 -32 L 38 0 L 0 38 L -38 0 Z" fill="none" stroke="rgba(255, 255, 255, 0.45)" strokeWidth="1.2" />
          <path d="M 0 -52 L 62 0 L 0 62 L -62 0 Z" fill="none" stroke="rgba(255, 255, 255, 0.38)" strokeWidth="1.2" />
          <path d="M 0 -76 L 90 0 L 0 90 L -90 0 Z" fill="none" stroke="rgba(255, 255, 255, 0.3)" strokeWidth="1.2" />
          <path d="M 0 -105 L 125 0 L 0 125 L -125 0 Z" fill="none" stroke="rgba(255, 255, 255, 0.22)" strokeWidth="1.2" />
          {/* Target Center Dot */}
          <circle cx="0" cy="0" r="4" fill="#FFFFFF" />
          <circle cx="0" cy="0" r="8" fill="none" stroke="#FFFFFF" strokeWidth="1.5" />
        </g>

        {/* Field Plot 3 (Lower-Left Field): Wavy rounded contour lines + center dot */}
        <g transform="translate(310, 520)">
          <ellipse cx="0" cy="0" rx="18" ry="12" fill="none" stroke="rgba(255, 255, 255, 0.5)" strokeWidth="1.2" />
          <ellipse cx="0" cy="0" rx="36" ry="24" fill="none" stroke="rgba(255, 255, 255, 0.4)" strokeWidth="1.2" />
          <ellipse cx="0" cy="0" rx="58" ry="40" fill="none" stroke="rgba(255, 255, 255, 0.32)" strokeWidth="1.2" />
          <ellipse cx="0" cy="0" rx="84" ry="58" fill="none" stroke="rgba(255, 255, 255, 0.25)" strokeWidth="1.2" />
          <ellipse cx="0" cy="0" rx="114" ry="78" fill="none" stroke="rgba(255, 255, 255, 0.18)" strokeWidth="1.2" />
          {/* Target Center Dot */}
          <circle cx="0" cy="0" r="4" fill="#FFFFFF" />
          <circle cx="0" cy="0" r="8" fill="none" stroke="#FFFFFF" strokeWidth="1.5" />
        </g>
      </svg>
    </div>
  )
}
