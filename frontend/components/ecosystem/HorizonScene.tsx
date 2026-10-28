'use client'

import React, { useRef } from 'react'
import { motion, useScroll, useTransform } from 'framer-motion'

interface Props {
  variant?: 'login' | 'dashboard' | 'full'
}

/** Parallax background layer — clouds */
function CloudLayer({ scrollY }: { scrollY: ReturnType<typeof useScroll>['scrollY'] }) {
  const y = useTransform(scrollY, [0, 500], [0, -30])
  return (
    <motion.div style={{ y }} className="absolute inset-0 pointer-events-none">
      <svg width="100%" height="100%" viewBox="0 0 800 300" preserveAspectRatio="xMidYMid slice">
        <defs>
          <filter id="cloudBlur">
            <feGaussianBlur stdDeviation="8" />
          </filter>
        </defs>
        {/* Soft cloud shapes */}
        {[
          { cx: 150, cy: 60, rx: 80, ry: 30, op: 0.4 },
          { cx: 600, cy: 40, rx: 120, ry: 35, op: 0.3 },
          { cx: 400, cy: 80, rx: 90, ry: 25, op: 0.2 },
          { cx: 720, cy: 90, rx: 60, ry: 20, op: 0.25 },
          { cx: 80,  cy: 100, rx: 50, ry: 18, op: 0.2 },
        ].map((c, i) => (
          <ellipse
            key={i}
            cx={c.cx} cy={c.cy} rx={c.rx} ry={c.ry}
            fill="white"
            opacity={c.op}
            filter="url(#cloudBlur)"
          />
        ))}
      </svg>
    </motion.div>
  )
}

/** Parallax foreground — rolling hills / field boundary */
function FieldLayer({ scrollY }: { scrollY: ReturnType<typeof useScroll>['scrollY'] }) {
  const y = useTransform(scrollY, [0, 500], [0, 15])
  return (
    <motion.div
      style={{ y, position: 'absolute', bottom: 0, left: 0, right: 0, height: '45%' }}
      className="pointer-events-none"
    >
      <svg width="100%" height="100%" viewBox="0 0 800 220" preserveAspectRatio="xMidYMax slice">
        {/* Far field — pale green */}
        <path
          d="M 0 100 Q 200 60 400 85 Q 600 110 800 70 L 800 220 L 0 220 Z"
          fill="var(--moss-300)"
          opacity="0.25"
        />
        {/* Mid field — moss */}
        <path
          d="M 0 130 Q 150 105 350 120 Q 550 135 800 105 L 800 220 L 0 220 Z"
          fill="var(--moss-400)"
          opacity="0.35"
        />
        {/* Foreground — canopy */}
        <path
          d="M 0 165 Q 100 148 250 160 Q 450 172 650 155 Q 750 148 800 160 L 800 220 L 0 220 Z"
          fill="var(--canopy-700)"
          opacity="0.5"
        />
        {/* Soil line */}
        <rect x="0" y="205" width="800" height="15" fill="var(--soil-800)" opacity="0.4" />

        {/* Scattered tall grass blades on horizon */}
        {Array.from({ length: 24 }).map((_, i) => {
          const x  = (i / 24) * 800 + Math.sin(i * 2.3) * 10
          const h  = 12 + Math.abs(Math.sin(i * 1.7)) * 18
          const sw = 1.5 + Math.random() * 1
          return (
            <path
              key={i}
              d={`M ${x} 205 Q ${x + (i % 2 === 0 ? 3 : -3)} ${205 - h / 2} ${x + (i % 3 - 1) * 4} ${205 - h}`}
              stroke="var(--moss-300)"
              strokeWidth={sw}
              strokeLinecap="round"
              fill="none"
              opacity={0.6 + Math.random() * 0.4}
              className="grass-blade"
              style={{ animationDelay: `${-i * 0.15}s`, animationDuration: `${2.5 + Math.random() * 2}s` }}
            />
          )
        })}
      </svg>
    </motion.div>
  )
}

/** The ambient horizon scene that underlies every screen */
export default function HorizonScene({ variant = 'full' }: Props) {
  const ref = useRef<HTMLDivElement>(null)
  const { scrollY } = useScroll({ container: ref })

  return (
    <div
      ref={ref}
      className="horizon-shell"
      style={{ position: 'fixed', inset: 0, zIndex: 0, overflow: 'hidden' }}
      aria-hidden="true"
    >
      {/* Sky gradient — driven by CSS vars from SceneLightingRig */}
      <div
        style={{
          position: 'absolute', inset: 0,
          background: 'linear-gradient(to bottom, var(--horizon-top) 0%, var(--bg-base) 45%)',
          transition: 'background 4s ease',
        }}
      />

      {/* Horizon glow line */}
      <div className="horizon-line" />

      {/* Sun/moon disc */}
      <motion.div
        style={{
          position: 'absolute',
          bottom: '38%',
          left: '65%',
          width: 48,
          height: 48,
          borderRadius: '50%',
          background: 'radial-gradient(circle, var(--sun-300) 0%, var(--sun-400) 60%, transparent 100%)',
          filter: 'blur(6px)',
          opacity: 0.7,
        }}
        animate={{ y: [0, -4, 0], opacity: [0.7, 0.8, 0.7] }}
        transition={{ duration: 6, repeat: Infinity, ease: 'easeInOut' }}
      />

      {/* Ambient pollen particles */}
      {Array.from({ length: 8 }).map((_, i) => (
        <motion.div
          key={i}
          style={{
            position: 'absolute',
            width: 4,
            height: 4,
            borderRadius: '50%',
            background: 'var(--sun-300)',
            left: `${10 + i * 11}%`,
            bottom: `38%`,
            opacity: 0,
          }}
          animate={{
            y: [-20, -80, -120],
            x: [0, Math.sin(i) * 20, Math.cos(i) * 15],
            opacity: [0, 0.5, 0],
          }}
          transition={{
            duration: 4 + i * 0.8,
            delay: i * 0.6,
            repeat: Infinity,
            ease: 'easeOut',
          }}
        />
      ))}

      <CloudLayer scrollY={scrollY} />
      <FieldLayer scrollY={scrollY} />
    </div>
  )
}
