/**
 * FieldMapVisual.tsx — Smart Farmer
 *
 * The ONE memorable design element for the login experience.
 *
 * An SVG land-parcel composition based on real Indian agricultural
 * field geometry — irregular parcels, crop-row lines, survey boundaries.
 * Not a cartoon. Not stock photography. Not an icon set.
 *
 * Design principles followed:
 * - Earth tones only (canopy, soil, moss, bark, sun-accent)
 * - Sun-gold accent on ONE parcel only
 * - Crop-row geometry within parcels (authentic to Indian fieldwork)
 * - One subtle pulse animation on the highlighted parcel
 * - Fully static otherwise — no automatic motion
 * - aria-hidden — purely decorative, not conveying information
 */

'use client'

import React from 'react'
import { motion } from 'framer-motion'

interface Props {
  className?: string
}

export default function FieldMapVisual({ className }: Props) {
  return (
    <div
      className={className}
      style={{ width: '100%', height: '100%', position: 'relative' }}
      aria-hidden="true"
    >
      <svg
        viewBox="0 0 480 520"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        style={{ width: '100%', height: '100%' }}
        role="img"
        aria-label="Agricultural field map"
      >
        <defs>
          {/* Crop row pattern for large parcels */}
          <pattern id="cropRows" x="0" y="0" width="10" height="10" patternUnits="userSpaceOnUse" patternTransform="rotate(18)">
            <line x1="0" y1="5" x2="10" y2="5" stroke="rgba(47,82,51,0.18)" strokeWidth="0.8" />
          </pattern>

          {/* Crop row pattern for highlight parcel — gold rows */}
          <pattern id="cropRowsGold" x="0" y="0" width="10" height="10" patternUnits="userSpaceOnUse" patternTransform="rotate(12)">
            <line x1="0" y1="5" x2="10" y2="5" stroke="rgba(242,200,75,0.35)" strokeWidth="1" />
          </pattern>

          {/* Tight rows for a small parcel */}
          <pattern id="cropRowsTight" x="0" y="0" width="7" height="7" patternUnits="userSpaceOnUse" patternTransform="rotate(-8)">
            <line x1="0" y1="3.5" x2="7" y2="3.5" stroke="rgba(47,82,51,0.14)" strokeWidth="0.7" />
          </pattern>

          {/* Fallow / bare field texture */}
          <pattern id="fallowTexture" x="0" y="0" width="16" height="16" patternUnits="userSpaceOnUse">
            <circle cx="4" cy="4" r="0.8" fill="rgba(107,78,61,0.12)" />
            <circle cx="12" cy="12" r="0.8" fill="rgba(107,78,61,0.09)" />
          </pattern>

          {/* Soft glow for highlighted parcel */}
          <filter id="parcelGlow" x="-20%" y="-20%" width="140%" height="140%">
            <feGaussianBlur stdDeviation="6" result="blur" />
            <feMerge>
              <feMergeNode in="blur" />
              <feMergeNode in="SourceGraphic" />
            </feMerge>
          </filter>

          {/* Subtle drop shadow for parcel boundaries */}
          <filter id="boundaryShadow" x="-5%" y="-5%" width="110%" height="110%">
            <feDropShadow dx="1" dy="1" stdDeviation="2" floodColor="rgba(26,20,8,0.12)" />
          </filter>
        </defs>

        {/* ── Background — muted field tone ────────────────────────────── */}
        <rect width="480" height="520" fill="rgba(247,244,234,0.4)" rx="2" />

        {/* ── Survey grid lines (very faint) ─────────────────────────── */}
        {/* Horizontal grid */}
        {[80, 160, 240, 320, 400, 480].map((y) => (
          <line key={`h${y}`} x1="0" y1={y} x2="480" y2={y} stroke="rgba(107,78,61,0.07)" strokeWidth="0.5" strokeDasharray="4 8" />
        ))}
        {/* Vertical grid */}
        {[80, 160, 240, 320, 400].map((x) => (
          <line key={`v${x}`} x1={x} y1="0" x2={x} y2="520" stroke="rgba(107,78,61,0.07)" strokeWidth="0.5" strokeDasharray="4 8" />
        ))}

        {/* ════════════════════════════════════════════════════════════
            PARCEL 1 — Large wheat field (highlighted in sun-gold)
            North-west quadrant, irregular shape
        ════════════════════════════════════════════════════════════ */}
        <motion.g
          animate={{ opacity: [0.92, 1, 0.92] }}
          transition={{ duration: 4, repeat: Infinity, ease: 'easeInOut' }}
        >
          {/* Fill — warm gold base */}
          <path
            d="M 42 44 L 198 38 L 214 142 L 172 158 L 88 164 L 44 148 Z"
            fill="rgba(242,200,75,0.22)"
            stroke="none"
          />
          {/* Crop row texture */}
          <path
            d="M 42 44 L 198 38 L 214 142 L 172 158 L 88 164 L 44 148 Z"
            fill="url(#cropRowsGold)"
          />
          {/* Highlight border — sun-gold */}
          <path
            d="M 42 44 L 198 38 L 214 142 L 172 158 L 88 164 L 44 148 Z"
            stroke="#D9A441"
            strokeWidth="1.8"
            strokeLinejoin="round"
            filter="url(#boundaryShadow)"
          />
          {/* Parcel label — crop type */}
          <text x="122" y="105" textAnchor="middle" fontFamily="Inter, sans-serif" fontSize="9" fill="rgba(90,58,22,0.75)" fontWeight="500">Wheat</text>
          <text x="122" y="118" textAnchor="middle" fontFamily="Inter, sans-serif" fontSize="7.5" fill="rgba(90,58,22,0.5)">Plot 1 · 2.4 ac</text>
        </motion.g>

        {/* ════════════════════════════════════════════════════════════
            PARCEL 2 — Rice paddy, north-east
        ════════════════════════════════════════════════════════════ */}
        <g>
          <path
            d="M 218 36 L 384 42 L 398 62 L 410 148 L 370 162 L 230 158 L 214 142 Z"
            fill="rgba(77,131,82,0.18)"
          />
          <path
            d="M 218 36 L 384 42 L 398 62 L 410 148 L 370 162 L 230 158 L 214 142 Z"
            fill="url(#cropRows)"
          />
          <path
            d="M 218 36 L 384 42 L 398 62 L 410 148 L 370 162 L 230 158 L 214 142 Z"
            stroke="rgba(47,82,51,0.45)"
            strokeWidth="1.4"
            strokeLinejoin="round"
          />
          <text x="310" y="102" textAnchor="middle" fontFamily="Inter, sans-serif" fontSize="9" fill="rgba(30,60,35,0.75)" fontWeight="500">Rice</text>
          <text x="310" y="115" textAnchor="middle" fontFamily="Inter, sans-serif" fontSize="7.5" fill="rgba(30,60,35,0.5)">Plot 2 · 1.8 ac</text>
        </g>

        {/* ════════════════════════════════════════════════════════════
            PARCEL 3 — Small maize plot, far north-east
        ════════════════════════════════════════════════════════════ */}
        <g>
          <path
            d="M 388 42 L 452 48 L 458 130 L 412 148 L 398 62 Z"
            fill="rgba(122,155,92,0.15)"
          />
          <path
            d="M 388 42 L 452 48 L 458 130 L 412 148 L 398 62 Z"
            fill="url(#cropRowsTight)"
          />
          <path
            d="M 388 42 L 452 48 L 458 130 L 412 148 L 398 62 Z"
            stroke="rgba(77,131,82,0.35)"
            strokeWidth="1.2"
            strokeLinejoin="round"
          />
          <text x="425" y="96" textAnchor="middle" fontFamily="Inter, sans-serif" fontSize="8.5" fill="rgba(30,60,35,0.65)" fontWeight="500">Maize</text>
          <text x="425" y="108" textAnchor="middle" fontFamily="Inter, sans-serif" fontSize="7" fill="rgba(30,60,35,0.45)">0.6 ac</text>
        </g>

        {/* ════════════════════════════════════════════════════════════
            PARCEL 4 — Fallow / unplanted, west-centre
        ════════════════════════════════════════════════════════════ */}
        <g>
          <path
            d="M 32 164 L 86 166 L 172 160 L 180 272 L 148 296 L 64 298 L 28 280 Z"
            fill="rgba(154,117,98,0.14)"
          />
          <path
            d="M 32 164 L 86 166 L 172 160 L 180 272 L 148 296 L 64 298 L 28 280 Z"
            fill="url(#fallowTexture)"
          />
          <path
            d="M 32 164 L 86 166 L 172 160 L 180 272 L 148 296 L 64 298 L 28 280 Z"
            stroke="rgba(107,78,61,0.35)"
            strokeWidth="1.2"
            strokeLinejoin="round"
            strokeDasharray="5 3"
          />
          <text x="102" y="232" textAnchor="middle" fontFamily="Inter, sans-serif" fontSize="9" fill="rgba(107,78,61,0.65)" fontWeight="500">Fallow</text>
          <text x="102" y="245" textAnchor="middle" fontFamily="Inter, sans-serif" fontSize="7.5" fill="rgba(107,78,61,0.45)">Plot 4 · resting</text>
        </g>

        {/* ════════════════════════════════════════════════════════════
            PARCEL 5 — Centre block, large mixed field
        ════════════════════════════════════════════════════════════ */}
        <g>
          <path
            d="M 172 160 L 370 164 L 382 172 L 390 290 L 342 318 L 210 322 L 182 306 L 180 272 Z"
            fill="rgba(61,107,66,0.16)"
          />
          <path
            d="M 172 160 L 370 164 L 382 172 L 390 290 L 342 318 L 210 322 L 182 306 L 180 272 Z"
            fill="url(#cropRows)"
          />
          <path
            d="M 172 160 L 370 164 L 382 172 L 390 290 L 342 318 L 210 322 L 182 306 L 180 272 Z"
            stroke="rgba(47,82,51,0.4)"
            strokeWidth="1.5"
            strokeLinejoin="round"
          />
          <text x="285" y="244" textAnchor="middle" fontFamily="Inter, sans-serif" fontSize="9" fill="rgba(30,60,35,0.72)" fontWeight="500">Sorghum</text>
          <text x="285" y="257" textAnchor="middle" fontFamily="Inter, sans-serif" fontSize="7.5" fill="rgba(30,60,35,0.48)">Plot 5 · 3.1 ac</text>
        </g>

        {/* ════════════════════════════════════════════════════════════
            PARCEL 6 — East strip, long narrow field
        ════════════════════════════════════════════════════════════ */}
        <g>
          <path
            d="M 374 162 L 454 136 L 462 310 L 416 322 L 392 292 L 390 172 Z"
            fill="rgba(122,155,92,0.18)"
          />
          <pattern id="cropRowsNarrow" x="0" y="0" width="8" height="8" patternUnits="userSpaceOnUse" patternTransform="rotate(75)">
            <line x1="0" y1="4" x2="8" y2="4" stroke="rgba(47,82,51,0.16)" strokeWidth="0.8" />
          </pattern>
          <path
            d="M 374 162 L 454 136 L 462 310 L 416 322 L 392 292 L 390 172 Z"
            fill="url(#cropRowsNarrow)"
          />
          <path
            d="M 374 162 L 454 136 L 462 310 L 416 322 L 392 292 L 390 172 Z"
            stroke="rgba(77,131,82,0.35)"
            strokeWidth="1.2"
            strokeLinejoin="round"
          />
          <text x="425" y="225" textAnchor="middle" fontFamily="Inter, sans-serif" fontSize="8.5" fill="rgba(30,60,35,0.65)" fontWeight="500">Sugarcane</text>
          <text x="425" y="238" textAnchor="middle" fontFamily="Inter, sans-serif" fontSize="7" fill="rgba(30,60,35,0.45)">1.2 ac</text>
        </g>

        {/* ════════════════════════════════════════════════════════════
            PARCEL 7 — South-west, pulses plot
        ════════════════════════════════════════════════════════════ */}
        <g>
          <path
            d="M 26 284 L 66 300 L 148 298 L 180 308 L 176 400 L 130 428 L 52 420 L 22 402 Z"
            fill="rgba(154,117,98,0.16)"
          />
          <path
            d="M 26 284 L 66 300 L 148 298 L 180 308 L 176 400 L 130 428 L 52 420 L 22 402 Z"
            fill="url(#cropRowsTight)"
          />
          <path
            d="M 26 284 L 66 300 L 148 298 L 180 308 L 176 400 L 130 428 L 52 420 L 22 402 Z"
            stroke="rgba(107,78,61,0.35)"
            strokeWidth="1.3"
            strokeLinejoin="round"
          />
          <text x="102" y="362" textAnchor="middle" fontFamily="Inter, sans-serif" fontSize="9" fill="rgba(90,55,20,0.68)" fontWeight="500">Pulses</text>
          <text x="102" y="375" textAnchor="middle" fontFamily="Inter, sans-serif" fontSize="7.5" fill="rgba(90,55,20,0.45)">Plot 7 · 2.0 ac</text>
        </g>

        {/* ════════════════════════════════════════════════════════════
            PARCEL 8 — South-centre, vegetable beds
        ════════════════════════════════════════════════════════════ */}
        <g>
          <path
            d="M 182 310 L 210 326 L 342 320 L 370 332 L 364 430 L 284 448 L 200 440 L 178 418 Z"
            fill="rgba(77,131,82,0.14)"
          />
          <path
            d="M 182 310 L 210 326 L 342 320 L 370 332 L 364 430 L 284 448 L 200 440 L 178 418 Z"
            fill="url(#cropRows)"
          />
          <path
            d="M 182 310 L 210 326 L 342 320 L 370 332 L 364 430 L 284 448 L 200 440 L 178 418 Z"
            stroke="rgba(47,82,51,0.38)"
            strokeWidth="1.4"
            strokeLinejoin="round"
          />
          <text x="274" y="382" textAnchor="middle" fontFamily="Inter, sans-serif" fontSize="9" fill="rgba(30,60,35,0.7)" fontWeight="500">Vegetables</text>
          <text x="274" y="395" textAnchor="middle" fontFamily="Inter, sans-serif" fontSize="7.5" fill="rgba(30,60,35,0.48)">Plot 8 · 1.6 ac</text>
        </g>

        {/* ════════════════════════════════════════════════════════════
            PARCEL 9 — South-east, orchard / mango grove
        ════════════════════════════════════════════════════════════ */}
        <g>
          <path
            d="M 374 332 L 418 324 L 464 314 L 468 430 L 430 452 L 368 440 L 362 432 Z"
            fill="rgba(47,82,51,0.13)"
          />
          {/* Orchard dot pattern — trees, not rows */}
          <pattern id="orchardDots" x="0" y="0" width="18" height="18" patternUnits="userSpaceOnUse">
            <circle cx="9" cy="9" r="2.5" fill="rgba(47,82,51,0.22)" />
          </pattern>
          <path
            d="M 374 332 L 418 324 L 464 314 L 468 430 L 430 452 L 368 440 L 362 432 Z"
            fill="url(#orchardDots)"
          />
          <path
            d="M 374 332 L 418 324 L 464 314 L 468 430 L 430 452 L 368 440 L 362 432 Z"
            stroke="rgba(47,82,51,0.35)"
            strokeWidth="1.3"
            strokeLinejoin="round"
          />
          <text x="416" y="382" textAnchor="middle" fontFamily="Inter, sans-serif" fontSize="9" fill="rgba(30,60,35,0.65)" fontWeight="500">Mango</text>
          <text x="416" y="395" textAnchor="middle" fontFamily="Inter, sans-serif" fontSize="7.5" fill="rgba(30,60,35,0.45)">Orchard</text>
        </g>

        {/* ════════════════════════════════════════════════════════════
            IRRIGATION CHANNEL — runs through the map
        ════════════════════════════════════════════════════════════ */}
        <path
          d="M 0 150 Q 30 152 44 148"
          stroke="rgba(91,180,213,0.5)"
          strokeWidth="3"
          strokeLinecap="round"
          fill="none"
        />
        <path
          d="M 214 142 Q 282 144 370 162"
          stroke="rgba(91,180,213,0.4)"
          strokeWidth="2.5"
          strokeLinecap="round"
          fill="none"
        />
        <path
          d="M 180 272 Q 184 290 182 310"
          stroke="rgba(91,180,213,0.4)"
          strokeWidth="2"
          strokeLinecap="round"
          fill="none"
        />

        {/* ════════════════════════════════════════════════════════════
            FIELD ROAD — main access track
        ════════════════════════════════════════════════════════════ */}
        <path
          d="M 0 410 L 24 404 L 52 420 L 132 428 L 180 418 L 200 440 L 284 448 L 368 440 L 430 452 L 480 448"
          stroke="rgba(154,117,98,0.45)"
          strokeWidth="4"
          strokeLinecap="round"
          strokeLinejoin="round"
          fill="none"
          strokeDasharray="none"
        />
        {/* Road centre line */}
        <path
          d="M 0 410 L 24 404 L 52 420 L 132 428 L 180 418 L 200 440 L 284 448 L 368 440 L 430 452 L 480 448"
          stroke="rgba(247,244,234,0.6)"
          strokeWidth="1"
          strokeLinecap="round"
          strokeLinejoin="round"
          fill="none"
          strokeDasharray="8 6"
        />

        {/* ════════════════════════════════════════════════════════════
            COMPASS ROSE — subtle, top-right corner
        ════════════════════════════════════════════════════════════ */}
        <g transform="translate(448, 24)" opacity="0.35">
          <circle cx="0" cy="0" r="12" fill="none" stroke="rgba(107,78,61,0.4)" strokeWidth="0.8" />
          {/* N */}
          <path d="M 0 -10 L 0 -4" stroke="rgba(47,82,51,0.7)" strokeWidth="1.2" strokeLinecap="round" />
          <path d="M 0 10 L 0 4" stroke="rgba(107,78,61,0.4)" strokeWidth="0.8" strokeLinecap="round" />
          <path d="M -10 0 L -4 0" stroke="rgba(107,78,61,0.4)" strokeWidth="0.8" strokeLinecap="round" />
          <path d="M 10 0 L 4 0" stroke="rgba(107,78,61,0.4)" strokeWidth="0.8" strokeLinecap="round" />
          <text x="0" y="-13" textAnchor="middle" fontFamily="Inter, sans-serif" fontSize="7" fill="rgba(47,82,51,0.7)" fontWeight="600">N</text>
        </g>

        {/* ════════════════════════════════════════════════════════════
            SCALE BAR — bottom-left
        ════════════════════════════════════════════════════════════ */}
        <g transform="translate(24, 490)" opacity="0.5">
          <line x1="0" y1="0" x2="48" y2="0" stroke="rgba(107,78,61,0.6)" strokeWidth="1.2" />
          <line x1="0" y1="-4" x2="0" y2="4" stroke="rgba(107,78,61,0.6)" strokeWidth="1.2" />
          <line x1="48" y1="-4" x2="48" y2="4" stroke="rgba(107,78,61,0.6)" strokeWidth="1.2" />
          <text x="24" y="-7" textAnchor="middle" fontFamily="Inter, sans-serif" fontSize="7" fill="rgba(107,78,61,0.7)">200 m</text>
        </g>

        {/* ════════════════════════════════════════════════════════════
            HIGHLIGHTED PARCEL GLOW — layered on top of Parcel 1
        ════════════════════════════════════════════════════════════ */}
        <motion.path
          d="M 42 44 L 198 38 L 214 142 L 172 158 L 88 164 L 44 148 Z"
          fill="none"
          stroke="#D9A441"
          strokeWidth="2.5"
          strokeLinejoin="round"
          animate={{ opacity: [0.6, 1, 0.6] }}
          transition={{ duration: 3.5, repeat: Infinity, ease: 'easeInOut' }}
        />

        {/* Harvest ready indicator on Parcel 1 */}
        <motion.g
          animate={{ opacity: [0.7, 1, 0.7] }}
          transition={{ duration: 2.8, repeat: Infinity, ease: 'easeInOut', delay: 0.4 }}
        >
          <circle cx="128" cy="86" r="6" fill="rgba(242,200,75,0.9)" />
          <path d="M 125 86 L 128 83 L 131 86" fill="rgba(90,58,22,0.7)" />
          <text x="128" y="90" textAnchor="middle" fontFamily="Inter, sans-serif" fontSize="5.5" fill="rgba(90,58,22,0.85)" fontWeight="700">12d</text>
        </motion.g>
      </svg>

      {/* Legend strip at the bottom */}
      <div
        style={{
          position: 'absolute',
          bottom: 16,
          right: 16,
          display: 'flex',
          flexDirection: 'column',
          gap: 5,
        }}
      >
        {[
          { color: 'rgba(242,200,75,0.7)', label: 'Wheat · harvest soon' },
          { color: 'rgba(61,107,66,0.55)', label: 'Kharif crops' },
          { color: 'rgba(154,117,98,0.5)', label: 'Fallow / resting' },
          { color: 'rgba(91,180,213,0.55)', label: 'Irrigation channel' },
        ].map(({ color, label }) => (
          <div key={label} style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <div style={{ width: 10, height: 10, borderRadius: 2, background: color, flexShrink: 0 }} />
            <span style={{ fontFamily: 'Inter, sans-serif', fontSize: 9, color: 'rgba(107,78,61,0.7)', whiteSpace: 'nowrap' }}>
              {label}
            </span>
          </div>
        ))}
      </div>
    </div>
  )
}
