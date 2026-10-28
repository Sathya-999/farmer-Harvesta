'use client'

import React from 'react'
import { motion } from 'framer-motion'
import Link from 'next/link'
import { Home, BarChart2, Leaf, Users, Settings, MessageCircle } from 'lucide-react'
import { useSproutStore } from '@/store/sproutStore'

const NAV_ITEMS = [
  { href: '/dashboard', icon: Home, label: 'Home' },
  { href: '/field', icon: Leaf, label: 'Field' },
  { href: '/market', icon: BarChart2, label: 'Market' },
  { href: '/community', icon: Users, label: 'Network' },
  { href: '/settings', icon: Settings, label: 'Settings' },
]

interface Props {
  active: string
  onAssistant?: () => void
}

export default function NavRail({ active, onAssistant }: Props) {
  const { react } = useSproutStore()

  return (
    <nav className="nav-rail" aria-label="Main navigation">
      {/* Logo mark */}
      <div style={{ padding: '4px 0 8px', textAlign: 'center', borderBottom: '1px solid var(--border-soft)', marginBottom: 8 }}>
        <span style={{ fontFamily: 'var(--font-display)', fontSize: 18, fontWeight: 700, color: 'var(--canopy-700)', lineHeight: 1 }}>
          🌱
        </span>
      </div>

      {NAV_ITEMS.map(({ href, icon: Icon, label }) => {
        const isActive = active === href
        return (
          <Link key={href} href={href} className={`nav-item ${isActive ? 'active' : ''}`}>
            <Icon size={20} strokeWidth={isActive ? 2.5 : 1.8} />
            <span>{label}</span>
          </Link>
        )
      })}

      {/* AI Assistant button */}
      <div style={{ marginTop: 8, borderTop: '1px solid var(--border-soft)', paddingTop: 8 }}>
        <motion.button
          className="nav-item"
          onClick={() => {
            react('thinking')
            onAssistant?.()
          }}
          whileHover={{ scale: 1.05 }}
          whileTap={{ scale: 0.95 }}
          style={{
            width: '100%',
            background: 'linear-gradient(135deg, var(--canopy-700), var(--moss-400))',
            color: 'var(--dawn-50)',
            border: 'none',
            cursor: 'pointer',
          }}
          aria-label="Open AI assistant"
        >
          <MessageCircle size={20} strokeWidth={2} />
          <span>Ask AI</span>
        </motion.button>
      </div>
    </nav>
  )
}
