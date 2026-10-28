import React from 'react'
import Link from 'next/link'
import { ArrowUpRight } from 'lucide-react'
import { CultivationResponseDto } from '@/lib/cultivationService'

interface Props {
  cultivations?: CultivationResponseDto[]
}

export default function LeafAreaCard({ cultivations = [] }: Props) {
  const plannedCount = cultivations.filter((c) => c.status === 'PLANNED').length
  const activeCount = cultivations.filter((c) => c.status === 'STARTED').length
  const finishedCount = cultivations.filter((c) => c.status === 'COMPLETED').length

  return (
    <div
      style={{
        background: '#4E5840',
        color: '#FFFFFF',
        borderRadius: 22,
        padding: '16px 20px',
        boxShadow: '0 6px 24px rgba(0, 0, 0, 0.12)',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between',
        height: '100%',
        boxSizing: 'border-box',
      }}
    >
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 6 }}>
        <span style={{ fontSize: 15, fontWeight: 600, color: '#FFFFFF' }}>
          Cultivation Cycles
        </span>

        {/* Lime-yellow circle with link to /cultivations */}
        <Link
          href="/cultivations"
          title="View Cultivations"
          style={{
            width: 26,
            height: 26,
            borderRadius: '50%',
            background: '#D4FD33',
            color: '#1A2215',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            textDecoration: 'none',
            cursor: 'pointer',
          }}
        >
          <ArrowUpRight size={16} strokeWidth={2.4} />
        </Link>
      </div>

      {/* 3 Overlapping Textured Circles Visualization with Real Counts */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', width: '100%', margin: '4px 0' }}>
        <svg width="220" height="96" viewBox="0 0 220 96">
          <defs>
            {/* Pattern 1: Diagonal Stripes for Planned */}
            <pattern id="diagStripes" width="6" height="6" patternTransform="rotate(45 0 0)" patternUnits="userSpaceOnUse">
              <line x1="0" y1="0" x2="0" y2="6" stroke="#D4A359" strokeWidth="1.8" />
            </pattern>

            {/* Pattern 2: Dotted for Active */}
            <pattern id="stippleDots" width="5" height="5" patternUnits="userSpaceOnUse">
              <circle cx="2.5" cy="2.5" r="1.2" fill="#D4FD33" />
            </pattern>

            {/* Pattern 3: Contours for Finished */}
            <pattern id="wavyContours" width="12" height="12" patternUnits="userSpaceOnUse">
              <path d="M 0 6 Q 3 0, 6 6 T 12 6" fill="none" stroke="#D8D6B8" strokeWidth="1.2" />
            </pattern>
          </defs>

          {/* Circle 1 (Left - Planned): r=30, cx=58, cy=42 */}
          <g>
            <circle cx="58" cy="42" r="30" fill="url(#diagStripes)" opacity="0.85" />
            <circle cx="58" cy="42" r="30" fill="none" stroke="#D4A359" strokeWidth="1.5" />
            <text x="58" y="47" textAnchor="middle" fill="#FFFFFF" fontSize="14" fontWeight="700" fontFamily="sans-serif">
              {plannedCount}
            </text>
            <text x="58" y="86" textAnchor="middle" fill="rgba(255,255,255,0.7)" fontSize="10" fontWeight="500" fontFamily="sans-serif">
              Planned
            </text>
          </g>

          {/* Circle 2 (Center - Active): r=34, cx=110, cy=40 */}
          <g>
            <circle cx="110" cy="40" r="34" fill="#5A664B" />
            <circle cx="110" cy="40" r="34" fill="url(#stippleDots)" opacity="0.9" />
            <circle cx="110" cy="40" r="34" fill="none" stroke="#D4FD33" strokeWidth="1.8" />
            <text x="110" y="46" textAnchor="middle" fill="#FFFFFF" fontSize="16" fontWeight="800" fontFamily="sans-serif">
              {activeCount}
            </text>
            <text x="110" y="86" textAnchor="middle" fill="#FFFFFF" fontSize="10" fontWeight="600" fontFamily="sans-serif">
              Active
            </text>
          </g>

          {/* Circle 3 (Right - Finished): r=30, cx=162, cy=42 */}
          <g>
            <circle cx="162" cy="42" r="30" fill="url(#wavyContours)" opacity="0.85" />
            <circle cx="162" cy="42" r="30" fill="none" stroke="#D8D6B8" strokeWidth="1.5" />
            <text x="162" y="47" textAnchor="middle" fill="#FFFFFF" fontSize="14" fontWeight="700" fontFamily="sans-serif">
              {finishedCount}
            </text>
            <text x="162" y="86" textAnchor="middle" fill="rgba(255,255,255,0.7)" fontSize="10" fontWeight="500" fontFamily="sans-serif">
              Finished
            </text>
          </g>
        </svg>
      </div>
    </div>
  )
}
