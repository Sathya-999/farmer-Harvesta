'use client'

import React, { useState } from 'react'
import Link from 'next/link'
import { motion } from 'framer-motion'
import { Users, AlertCircle, BarChart2, ShieldCheck, ArrowRight } from 'lucide-react'
import NavRail from '@/components/ui/NavRail'
import AssistantPanel from '@/components/ui/AssistantPanel'
import HorizonScene from '@/components/ecosystem/HorizonScene'

export default function CommunityPage() {
  const [assistantOpen, setAssistantOpen] = useState(false)

  return (
    <div style={{ position: 'relative', minHeight: '100svh' }}>
      <HorizonScene />
      <NavRail active="/community" onAssistant={() => setAssistantOpen(true)} />

      <main style={{ marginLeft: 72, padding: '32px 28px 80px', position: 'relative', zIndex: 5, maxWidth: 1080 }}>
        {/* Header */}
        <motion.div
          initial={{ opacity: 0, y: -16 }}
          animate={{ opacity: 1, y: 0 }}
          style={{ marginBottom: 28 }}
        >
          <p style={{ fontFamily: 'var(--font-mono)', fontSize: 11, color: 'var(--text-muted)', letterSpacing: '0.1em', textTransform: 'uppercase', margin: 0 }}>
            Community · Peer Farmer Network
          </p>
          <h1 style={{ fontFamily: 'var(--font-display)', fontSize: 30, fontWeight: 600, color: 'var(--text-primary)', margin: '4px 0 0' }}>
            Farmer Exchange & Knowledge Sharing
          </h1>
        </motion.div>

        {/* Roadmap & Privacy Notice */}
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          style={{
            background: 'rgba(251, 244, 228, 0.95)',
            backdropFilter: 'blur(14px)',
            borderRadius: 20,
            border: '1.5px solid var(--border-soft, #D9E1D8)',
            padding: 28,
            boxShadow: '0 8px 28px rgba(26, 20, 8, 0.06)',
            display: 'flex',
            flexDirection: 'column',
            gap: 16,
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <div
              style={{
                width: 44,
                height: 44,
                borderRadius: 14,
                background: 'rgba(47, 82, 51, 0.12)',
                color: 'var(--canopy-700, #2F5233)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                flexShrink: 0,
              }}
            >
              <Users size={24} />
            </div>
            <div>
              <h2 style={{ fontSize: 18, fontWeight: 700, margin: 0, color: 'var(--text-primary)' }}>
                Peer Farmer Network (Scheduled for Beta V2)
              </h2>
              <span style={{ fontSize: 12, color: 'var(--text-muted)' }}>
                Private, consent-driven community knowledge exchange
              </span>
            </div>
          </div>

          <p style={{ fontSize: 13.5, color: '#3A3F31', lineHeight: 1.6, margin: 0 }}>
            Under platform privacy, anti-IDOR, and data protection rules, individual farmer identities and private holding yields are not shared publicly or fabricated. Community exchange and cooperative buying groups are slated for Release Beta V2 with farmer opt-in verification.
          </p>

          <p style={{ fontSize: 13, color: '#555C4D', margin: 0, lineHeight: 1.5 }}>
            To view verified aggregate agricultural patterns for your district without compromising individual privacy, consult Regional Agricultural Intelligence.
          </p>

          <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', paddingTop: 6 }}>
            <Link
              href="/analytics"
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: 8,
                padding: '10px 20px',
                borderRadius: 12,
                background: 'var(--canopy-700, #2F5233)',
                color: '#FFFFFF',
                fontSize: 13,
                fontWeight: 600,
                textDecoration: 'none',
              }}
            >
              <BarChart2 size={16} />
              <span>Explore Regional Agricultural Intelligence</span>
            </Link>
            <Link
              href="/dashboard"
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: 8,
                padding: '10px 20px',
                borderRadius: 12,
                background: '#FFFFFF',
                border: '1.5px solid var(--border-soft, #D9E1D8)',
                color: 'var(--canopy-700, #2F5233)',
                fontSize: 13,
                fontWeight: 600,
                textDecoration: 'none',
              }}
            >
              <span>Return to Farmland Dashboard</span>
              <ArrowRight size={14} />
            </Link>
          </div>
        </motion.div>
      </main>

      <AssistantPanel open={assistantOpen} onClose={() => setAssistantOpen(false)} />
    </div>
  )
}
