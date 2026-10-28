'use client'

import React from 'react'
import { motion } from 'framer-motion'

interface PlotCardProps {
  title: string
  value: string | number
  unit?: string
  subtitle?: string
  icon?: React.ReactNode
  trend?: 'up' | 'down' | 'flat'
  trendValue?: string
  variant?: 'default' | 'highlight' | 'warning' | 'growth'
  onClick?: () => void
  delay?: number
  children?: React.ReactNode
}

const VARIANT_STYLES = {
  default: {
    border: '1.5px solid var(--border-soft)',
    background: 'linear-gradient(145deg, var(--dawn-50), var(--dawn-100))',
  },
  highlight: {
    border: '1.5px solid var(--sun-300)',
    background: 'linear-gradient(145deg, var(--sun-200), var(--dawn-100))',
  },
  warning: {
    border: '1.5px solid rgba(198, 40, 40, 0.25)',
    background: 'linear-gradient(145deg, rgba(198,40,40,0.05), var(--dawn-100))',
  },
  growth: {
    border: '1.5px solid var(--moss-300)',
    background: 'linear-gradient(145deg, rgba(122, 155, 92, 0.12), var(--dawn-100))',
  },
}

const TREND_COLORS = {
  up:   { color: '#2E7D32', symbol: '↑' },
  down: { color: '#C62828', symbol: '↓' },
  flat: { color: 'var(--bark-400)', symbol: '→' },
}

export default function PlotCard({
  title,
  value,
  unit,
  subtitle,
  icon,
  trend,
  trendValue,
  variant = 'default',
  onClick,
  delay = 0,
  children,
}: PlotCardProps) {
  const style = VARIANT_STYLES[variant]

  return (
    <motion.div
      className="plot-card"
      style={{ ...style, cursor: onClick ? 'pointer' : 'default' }}
      initial={{ opacity: 0, y: 24 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5, delay, ease: [0.22, 1, 0.36, 1] }}
      whileHover={onClick ? { y: -3, scale: 1.01 } : {}}
      whileTap={onClick ? { scale: 0.98 } : {}}
      onClick={onClick}
      role={onClick ? 'button' : undefined}
      tabIndex={onClick ? 0 : undefined}
    >
      {/* Header row */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 12 }}>
        <span style={{
          fontSize: 11, fontWeight: 700, color: 'var(--text-muted)', letterSpacing: '0.09em',
          textTransform: 'uppercase', fontFamily: 'var(--font-body)',
        }}>
          {title}
        </span>
        {icon && (
          <span style={{ opacity: 0.6, display: 'flex', alignItems: 'center' }}>
            {icon}
          </span>
        )}
      </div>

      {/* Main value */}
      <div style={{ display: 'flex', alignItems: 'baseline', gap: 6, marginBottom: 6 }}>
        <span style={{
          fontFamily: 'var(--font-mono)',
          fontSize: typeof value === 'string' && value.length > 5 ? 22 : 28,
          fontWeight: 400,
          color: variant === 'highlight' ? 'var(--soil-950)' : 'var(--canopy-700)',
          lineHeight: 1,
        }}>
          {value}
        </span>
        {unit && (
          <span style={{ fontSize: 13, color: 'var(--text-muted)', fontFamily: 'var(--font-body)' }}>
            {unit}
          </span>
        )}
      </div>

      {/* Trend badge */}
      {trend && trendValue && (
        <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 8 }}>
          <span className={`trend-badge ${trend}`}>
            {TREND_COLORS[trend].symbol} {trendValue}
          </span>
        </div>
      )}

      {/* Subtitle */}
      {subtitle && (
        <p style={{ fontSize: 12, color: 'var(--text-muted)', margin: 0, lineHeight: 1.5 }}>
          {subtitle}
        </p>
      )}

      {/* Children (charts, etc.) */}
      {children}
    </motion.div>
  )
}
