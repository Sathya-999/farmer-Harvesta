'use client'

import React, { useState } from 'react'
import Image from 'next/image'
import { motion, AnimatePresence } from 'framer-motion'
import { Sparkles, X } from 'lucide-react'

interface Props {
  onSeeDetails?: () => void
}

export default function RecommendationPanel({ onSeeDetails }: Props) {
  const [modalOpen, setModalOpen] = useState(false)

  return (
    <>
      {/* ── Advisor Section (Seamless Farmer Cutout + Floating Text + See details Button) ── */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 16,
          position: 'relative',
        }}
      >
        {/* Floating Text & See details button */}
        <div
          style={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'flex-start',
            gap: 12,
            maxWidth: 210,
          }}
        >
          <p
            style={{
              margin: 0,
              fontSize: 15.5,
              fontWeight: 500,
              color: '#FFFFFF',
              lineHeight: 1.35,
              textShadow: '0 2px 6px rgba(0, 0, 0, 0.65)',
              fontFamily: 'var(--font-body, system-ui, sans-serif)',
            }}
          >
            The conditions are ideal for planting leafy crops such as lettuce.
          </p>

          <button
            onClick={() => {
              setModalOpen(true)
              if (onSeeDetails) onSeeDetails()
            }}
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              padding: '8px 22px',
              borderRadius: 999,
              border: 'none',
              background: '#D4FD33',
              color: '#1A2215',
              fontSize: 12.5,
              fontWeight: 700,
              cursor: 'pointer',
              boxShadow: '0 3px 12px rgba(212, 253, 51, 0.45)',
              transition: 'transform 0.15s, box-shadow 0.15s',
            }}
          >
            See details
          </button>
        </div>

        {/* Farmer Cutout Holding Cabbage */}
        <div
          style={{
            position: 'relative',
            width: 140,
            height: 170,
            flexShrink: 0,
            overflow: 'hidden',
            borderRadius: '50% 50% 30% 30% / 60% 60% 40% 40%',
            filter: 'drop-shadow(0 8px 16px rgba(0,0,0,0.35))',
          }}
        >
          <Image
            src="/farmer_cabbage.jpg"
            alt="Farmer Advisor Holding Cabbage"
            fill
            style={{
              objectFit: 'cover',
              objectPosition: 'center 15%',
            }}
            priority
          />
        </div>
      </div>

      {/* Advisory Modal */}
      <AnimatePresence>
        {modalOpen && (
          <div
            style={{
              position: 'fixed',
              inset: 0,
              background: 'rgba(22, 58, 36, 0.45)',
              backdropFilter: 'blur(4px)',
              zIndex: 999,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              padding: 16,
            }}
          >
            <motion.div
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.95 }}
              style={{
                width: '100%',
                maxWidth: 480,
                borderRadius: 20,
                background: '#FAF8F0',
                border: '1.5px solid #D5D5BE',
                padding: 24,
                boxShadow: '0 20px 40px rgba(0,0,0,0.2)',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: 12 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                  <div
                    style={{
                      width: 36,
                      height: 36,
                      borderRadius: 10,
                      background: '#24442A',
                      color: '#D4FD33',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                    }}
                  >
                    <Sparkles size={18} />
                  </div>
                  <div>
                    <h3 style={{ margin: 0, fontSize: 18, fontWeight: 700, color: '#272A22' }}>
                      Leafy Crop Advisory (Lettuce & Brassicas)
                    </h3>
                    <span style={{ fontSize: 11, color: '#777A6D' }}>Field 01 Microclimate Recommendation</span>
                  </div>
                </div>
                <button
                  onClick={() => setModalOpen(false)}
                  style={{ background: 'none', border: 'none', padding: 4, cursor: 'pointer', color: '#777A6D' }}
                >
                  <X size={20} />
                </button>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: 12, fontSize: 13, color: '#3A3F31', lineHeight: 1.5 }}>
                <p style={{ margin: 0 }}>
                  With current soil moisture at 32.4% and optimal vegetative NDVI signals, Field 01 offers prime conditions for fast-turnaround leafy greens like Romaine and Butterhead lettuce.
                </p>
                <div style={{ padding: 12, borderRadius: 10, background: '#E8E8D7', border: '1px solid #D5D5BE' }}>
                  <strong>Agronomist Note:</strong> Bed preparation with light sprinkler irrigation recommended prior to transplanting. Estimated cycle to harvest: 45–55 days.
                </div>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 20 }}>
                <button
                  onClick={() => setModalOpen(false)}
                  style={{
                    padding: '9px 18px',
                    borderRadius: 10,
                    border: 'none',
                    background: '#24442A',
                    color: '#fff',
                    fontSize: 13,
                    fontWeight: 600,
                    cursor: 'pointer',
                  }}
                >
                  Close Advisory
                </button>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </>
  )
}
