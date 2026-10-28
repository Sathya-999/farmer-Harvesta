'use client'

import React from 'react'
import Link from 'next/link'
import { Sprout } from 'lucide-react'

interface Props {
  size?: 'normal' | 'large'
  showLink?: boolean
  subtitle?: string
}

export default function AgriculturalBrandLogo({
  size = 'normal',
  showLink = false,
  subtitle = 'Agri Intelligence OS',
}: Props) {
  const isLarge = size === 'large'

  const content = (
    <div
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: isLarge ? 14 : 12,
        userSelect: 'none',
        textDecoration: 'none',
      }}
    >
      {/* Brand Icon Badge with Sprout */}
      <div
        style={{
          width: isLarge ? 48 : 40,
          height: isLarge ? 48 : 40,
          borderRadius: isLarge ? 13 : 10,
          background: '#24442A',
          color: '#DFFF28',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          flexShrink: 0,
          boxShadow: '0 4px 14px rgba(36, 68, 42, 0.25)',
          border: '1px solid rgba(223, 255, 40, 0.25)',
        }}
      >
        <Sprout size={isLarge ? 26 : 22} />
      </div>

      {/* Brand Wordmark & Subtitle */}
      <div>
        <p
          style={{
            fontFamily: 'var(--font-serif, "Fraunces", Georgia, serif)',
            fontSize: isLarge ? '24px' : '20px',
            fontWeight: 800,
            color: '#272A22',
            margin: 0,
            lineHeight: 1.1,
            letterSpacing: '-0.02em',
          }}
        >
          Harvest<span style={{ color: '#5B7A38', fontWeight: 600 }}>(a)</span>
        </p>
        <p
          style={{
            fontFamily: 'var(--font-body, "Inter", system-ui, sans-serif)',
            fontSize: isLarge ? '11px' : '10px',
            color: '#71844A',
            margin: '2px 0 0',
            fontWeight: 600,
            letterSpacing: '0.04em',
            textTransform: 'uppercase',
          }}
        >
          {subtitle}
        </p>
      </div>
    </div>
  )

  if (showLink) {
    return (
      <Link href="/" style={{ textDecoration: 'none' }} title="Return to Harvest(a) Home">
        {content}
      </Link>
    )
  }

  return content
}
