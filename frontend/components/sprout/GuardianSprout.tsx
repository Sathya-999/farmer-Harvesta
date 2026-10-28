'use client'

import React, { useRef, useEffect, useCallback } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { useSproutStore, SproutMood } from '@/store/sproutStore'

interface Props {
  size?: number
  showParticles?: boolean
  className?: string
}

/** Particle component for success burst */
function Particle({ x, py }: { x: number; py: number }) {
  const angle = Math.random() * Math.PI * 2
  const dist  = 40 + Math.random() * 40
  const px    = Math.cos(angle) * dist
  return (
    <motion.circle
      cx={x} cy={140}
      r={3}
      fill="var(--sun-400)"
      initial={{ x: 0, y: 0, opacity: 1, scale: 1 }}
      animate={{
        x: px, y: -Math.abs(py),
        opacity: 0, scale: 0,
        transition: { duration: 0.9, ease: [0.22, 1, 0.36, 1] }
      }}
    />
  )
}

/** Fallen leaf for sad/error state */
function FallenLeaf({ visible }: { visible: boolean }) {
  return (
    <AnimatePresence>
      {visible && (
        <motion.ellipse
          cx={120} cy={185}
          rx={10} ry={6}
          fill="var(--canopy-600)"
          opacity={0.7}
          initial={{ y: -40, x: 0, rotate: 0, opacity: 0.8 }}
          animate={{
            y: 0, x: -20, rotate: -120, opacity: 0,
            transition: { duration: 0.7, ease: 'easeIn' }
          }}
          exit={{ opacity: 0 }}
        />
      )}
    </AnimatePresence>
  )
}

/** Bloom flowers for happy/success state */
function BloomFlowers({ visible }: { visible: boolean }) {
  const positions = [
    { x: 60, y: 185 },
    { x: 95, y: 190 },
    { x: 130, y: 185 },
    { x: 75, y: 178 },
    { x: 115, y: 178 },
  ]
  return (
    <AnimatePresence>
      {visible && positions.map((p, i) => (
        <motion.g key={i}>
          {/* Petals */}
          {[0, 72, 144, 216, 288].map((deg, j) => (
            <motion.ellipse
              key={j}
              cx={p.x + Math.cos((deg * Math.PI) / 180) * 6}
              cy={p.y + Math.sin((deg * Math.PI) / 180) * 6}
              rx={4} ry={3}
              fill={i % 2 === 0 ? 'var(--sun-300)' : 'var(--moss-300)'}
              initial={{ scale: 0, opacity: 0 }}
              animate={{
                scale: 1, opacity: 1,
                transition: { delay: i * 0.06 + j * 0.02, duration: 0.4, type: 'spring', bounce: 0.5 }
              }}
              exit={{ scale: 0, opacity: 0 }}
            />
          ))}
          {/* Center */}
          <motion.circle
            cx={p.x} cy={p.y} r={3}
            fill="var(--sun-400)"
            initial={{ scale: 0 }}
            animate={{ scale: 1, transition: { delay: i * 0.06 + 0.1, type: 'spring' } }}
            exit={{ scale: 0 }}
          />
        </motion.g>
      ))}
    </AnimatePresence>
  )
}

/** The full Guardian Sprout SVG character */
export default function GuardianSprout({
  size = 220,
  showParticles = true,
  className = '',
}: Props) {
  const { mood, growthStage } = useSproutStore()
  const blinkerRef = useRef<ReturnType<typeof setInterval> | null>(null)
  const [blinkOpen, setBlinkOpen] = React.useState(true)
  const [particles, setParticles] = React.useState<number[]>([])

  // Randomized blink timing — not metronomic (avoids robotic feel)
  const scheduleBlink = useCallback(() => {
    const delay = 3000 + Math.random() * 4000
    blinkerRef.current = setTimeout(() => {
      setBlinkOpen(false)
      setTimeout(() => {
        setBlinkOpen(true)
        scheduleBlink()
      }, 120)
    }, delay)
  }, [])

  useEffect(() => {
    scheduleBlink()
    return () => {
      if (blinkerRef.current) clearTimeout(blinkerRef.current as unknown as number)
    }
  }, [scheduleBlink])

  // Burst particles on success
  useEffect(() => {
    if (mood === 'happy') {
      setParticles([1, 2, 3, 4, 5, 6])
      setTimeout(() => setParticles([]), 1200)
    }
  }, [mood])

  // Mood-driven transforms
  const stemAnim = {
    idle:        { rotate: 0,  y: 0 },
    interested:  { rotate: 4,  y: -2 },
    focused:     { rotate: -2, y: 0 },
    happy:       { rotate: 0,  y: -4 },
    sad:         { rotate: -6, y: 4 },
    thinking:    { rotate: 2,  y: -1 },
    celebrating: { rotate: 5,  y: -5 },
  }

  const leafLeftAnim = {
    idle:        { rotate: 0 },
    interested:  { rotate: -12 },
    focused:     { rotate: -18 },
    happy:       { rotate: -5 },
    sad:         { rotate: 10 },
    thinking:    { rotate: -8 },
    celebrating: { rotate: -25 },
  }

  const leafRightAnim = {
    idle:        { rotate: 0 },
    interested:  { rotate: 12 },
    focused:     { rotate: 18 },
    happy:       { rotate: 5 },
    sad:         { rotate: -10 },
    thinking:    { rotate: 8 },
    celebrating: { rotate: 25 },
  }

  const eyeScale = {
    idle:        1,
    interested:  1.2,
    focused:     0.7,
    happy:       1.1,
    sad:         0.8,
    thinking:    0.9,
    celebrating: 1.3,
  }

  const moodTransition = { type: 'spring', stiffness: 120, damping: 14 }

  return (
    <div className={`sprout-wrap ${className}`} style={{ width: size, height: size * 1.1 }}>
      <svg
        viewBox="0 0 200 220"
        width={size}
        height={size * 1.1}
        style={{ overflow: 'visible' }}
        aria-label={`Guardian Sprout — feeling ${mood}`}
        role="img"
      >
        <defs>
          {/* Leaf gradient */}
          <radialGradient id="leafGrad" cx="40%" cy="30%">
            <stop offset="0%" stopColor="var(--moss-300)" />
            <stop offset="100%" stopColor="var(--canopy-700)" />
          </radialGradient>
          {/* Stem gradient */}
          <linearGradient id="stemGrad" x1="0" y1="0" x2="1" y2="0">
            <stop offset="0%" stopColor="var(--canopy-600)" />
            <stop offset="100%" stopColor="var(--moss-400)" />
          </linearGradient>
          {/* Soil gradient */}
          <radialGradient id="soilGrad" cx="50%" cy="30%">
            <stop offset="0%" stopColor="var(--bark-600)" />
            <stop offset="100%" stopColor="var(--soil-800)" />
          </radialGradient>
          {/* Eye glow */}
          <radialGradient id="eyeGrad" cx="40%" cy="35%">
            <stop offset="0%" stopColor="white" />
            <stop offset="100%" stopColor="var(--dawn-100)" />
          </radialGradient>
          {/* Glow filter */}
          <filter id="glow">
            <feGaussianBlur in="SourceGraphic" stdDeviation="3" result="blur" />
            <feMerge>
              <feMergeNode in="blur" />
              <feMergeNode in="SourceGraphic" />
            </feMerge>
          </filter>
        </defs>

        {/* ── GROUND / SOIL ──────────────────────────────────── */}
        <ellipse cx="100" cy="193" rx="55" ry="12" fill="url(#soilGrad)" opacity="0.9" />
        <ellipse cx="100" cy="193" rx="45" ry="7"  fill="var(--bark-400)" opacity="0.3" />

        {/* Fallen leaf (sad state) */}
        <FallenLeaf visible={mood === 'sad'} />

        {/* Bloom flowers (happy state) */}
        <BloomFlowers visible={mood === 'happy' || mood === 'celebrating'} />

        {/* ── GRASS BLADES ────────────────────────────────────── */}
        {[
          { cx: 58, h: 22, delay: '0s' },
          { cx: 70, h: 16, delay: '-1.2s' },
          { cx: 48, h: 18, delay: '-2.4s' },
          { cx: 140, h: 20, delay: '-0.8s' },
          { cx: 150, h: 14, delay: '-1.8s' },
          { cx: 135, h: 24, delay: '-0.4s' },
        ].map((g, i) => (
          <motion.path
            key={i}
            d={`M ${g.cx} 190 Q ${g.cx - 4} ${190 - g.h / 2} ${g.cx + 2} ${190 - g.h}`}
            stroke="var(--moss-400)"
            strokeWidth="2.5"
            strokeLinecap="round"
            fill="none"
            className="grass-blade"
            style={{ animationDelay: g.delay }}
            opacity={0.7}
          />
        ))}

        {/* ── STEM ─────────────────────────────────────────────── */}
        <motion.g
          animate={stemAnim[mood]}
          transition={moodTransition}
          style={{ originX: '100px', originY: '190px' }}
        >
          <motion.path
            d="M 100 190 C 98 170, 102 150, 100 125"
            stroke="url(#stemGrad)"
            strokeWidth="6"
            strokeLinecap="round"
            fill="none"
            filter="url(#glow)"
          />

          {/* ── LEFT LEAF ──────────────────────────────────────── */}
          <motion.g
            animate={leafLeftAnim[mood]}
            transition={moodTransition}
            style={{ originX: '100px', originY: '155px' }}
          >
            <motion.ellipse
              cx="75" cy="150"
              rx="28" ry="14"
              fill="url(#leafGrad)"
              transform="rotate(-35 75 150)"
              opacity={0.95}
            />
            {/* Leaf vein */}
            <motion.path
              d="M 95 157 C 85 150 78 147 68 145"
              stroke="var(--canopy-600)"
              strokeWidth="1"
              fill="none"
              opacity="0.4"
            />
          </motion.g>

          {/* ── RIGHT LEAF ─────────────────────────────────────── */}
          <motion.g
            animate={leafRightAnim[mood]}
            transition={moodTransition}
            style={{ originX: '100px', originY: '155px' }}
          >
            <motion.ellipse
              cx="125" cy="150"
              rx="28" ry="14"
              fill="url(#leafGrad)"
              transform="rotate(35 125 150)"
              opacity={0.95}
            />
            <motion.path
              d="M 105 157 C 115 150 122 147 132 145"
              stroke="var(--canopy-600)"
              strokeWidth="1"
              fill="none"
              opacity="0.4"
            />
          </motion.g>

          {/* ── BROW LEAVES (smaller, above eyes) ─────────────── */}
          <motion.ellipse
            cx="86" cy="118"
            rx="12" ry="6"
            fill="var(--canopy-700)"
            transform="rotate(-20 86 118)"
            animate={{
              rotate: mood === 'sad' ? -35 : mood === 'focused' ? -15 : -20,
              y:      mood === 'interested' ? -3 : 0,
            }}
            transition={moodTransition}
          />
          <motion.ellipse
            cx="114" cy="118"
            rx="12" ry="6"
            fill="var(--canopy-700)"
            transform="rotate(20 114 118)"
            animate={{
              rotate: mood === 'sad' ? 35 : mood === 'focused' ? 15 : 20,
              y:      mood === 'interested' ? -3 : 0,
            }}
            transition={moodTransition}
          />

          {/* ── HEAD / FACE AREA ───────────────────────────────── */}
          <motion.ellipse
            cx="100" cy="128"
            rx="22" ry="20"
            fill="url(#leafGrad)"
            opacity={0.15}
          />

          {/* ── EYES ───────────────────────────────────────────── */}
          {/* Left eye */}
          <motion.g
            animate={{
              scaleX: eyeScale[mood],
              scaleY: blinkOpen
                ? eyeScale[mood]
                : 0.08,
              y: mood === 'sad' ? 2 : 0,
            }}
            transition={{ scaleY: { duration: 0.08 }, scaleX: moodTransition, y: moodTransition }}
            style={{ originX: '88px', originY: '128px' }}
          >
            <circle cx="88" cy="128" r="7" fill="url(#eyeGrad)" />
            <circle cx="88" cy="128" r="4.5" fill="var(--soil-950)" />
            <circle cx="86" cy="126" r="1.5" fill="white" opacity="0.9" />
            {/* Focused privacy veil */}
            {mood === 'focused' && (
              <motion.path
                d="M 81 128 Q 88 122 95 128"
                stroke="var(--canopy-700)"
                strokeWidth="2.5"
                strokeLinecap="round"
                fill="none"
                initial={{ pathLength: 0 }}
                animate={{ pathLength: 1 }}
                exit={{ pathLength: 0 }}
              />
            )}
          </motion.g>

          {/* Right eye */}
          <motion.g
            animate={{
              scaleX: eyeScale[mood],
              scaleY: blinkOpen
                ? eyeScale[mood]
                : 0.08,
              y: mood === 'sad' ? 2 : 0,
            }}
            transition={{ scaleY: { duration: 0.08 }, scaleX: moodTransition, y: moodTransition }}
            style={{ originX: '112px', originY: '128px' }}
          >
            <circle cx="112" cy="128" r="7" fill="url(#eyeGrad)" />
            <circle cx="112" cy="128" r="4.5" fill="var(--soil-950)" />
            <circle cx="110" cy="126" r="1.5" fill="white" opacity="0.9" />
            {mood === 'focused' && (
              <motion.path
                d="M 105 128 Q 112 122 119 128"
                stroke="var(--canopy-700)"
                strokeWidth="2.5"
                strokeLinecap="round"
                fill="none"
                initial={{ pathLength: 0 }}
                animate={{ pathLength: 1 }}
                exit={{ pathLength: 0 }}
              />
            )}
          </motion.g>

          {/* ── MOUTH ──────────────────────────────────────────── */}
          <motion.path
            animate={{
              d: mood === 'happy' || mood === 'celebrating'
                ? 'M 93 140 Q 100 146 107 140'
                : mood === 'sad'
                  ? 'M 93 144 Q 100 140 107 144'
                  : mood === 'thinking'
                    ? 'M 93 141 Q 100 141 107 141'
                    : 'M 94 141 Q 100 144 106 141',
            }}
            stroke="var(--canopy-700)"
            strokeWidth="2"
            strokeLinecap="round"
            fill="none"
            transition={moodTransition}
          />

          {/* Cheek blush (happy) */}
          <AnimatePresence>
            {(mood === 'happy' || mood === 'celebrating') && (
              <>
                <motion.ellipse
                  cx="80" cy="135" rx="6" ry="4"
                  fill="var(--sky-dawn)"
                  initial={{ opacity: 0, scale: 0.5 }}
                  animate={{ opacity: 0.5, scale: 1 }}
                  exit={{ opacity: 0 }}
                />
                <motion.ellipse
                  cx="120" cy="135" rx="6" ry="4"
                  fill="var(--sky-dawn)"
                  initial={{ opacity: 0, scale: 0.5 }}
                  animate={{ opacity: 0.5, scale: 1 }}
                  exit={{ opacity: 0 }}
                />
              </>
            )}
          </AnimatePresence>

          {/* ── TOP BUD (apex) ─────────────────────────────────── */}
          <motion.ellipse
            cx="100" cy="108"
            rx="8" ry="12"
            fill="var(--canopy-500)"
            animate={{
              scaleY: mood === 'happy' || mood === 'celebrating' ? 1.15 : 1,
              y: mood === 'celebrating' ? -4 : 0,
            }}
            transition={{ type: 'spring', stiffness: 80 }}
          />
          <motion.ellipse
            cx="100" cy="103"
            rx="5" ry="4"
            fill="var(--moss-300)"
            animate={{ y: mood === 'celebrating' ? -4 : 0 }}
            transition={moodTransition}
          />
        </motion.g>

        {/* ── POLLEN PARTICLES (success burst) ─────────────────── */}
        {showParticles && particles.map((_, i) => (
          <Particle
            key={i}
            x={100 + (Math.random() - 0.5) * 30}
            py={30 + Math.random() * 30}
          />
        ))}

        {/* ── THINKING DOTS ─────────────────────────────────────── */}
        <AnimatePresence>
          {mood === 'thinking' && (
            [0, 1, 2].map((i) => (
              <motion.circle
                key={i}
                cx={87 + i * 13} cy={98}
                r={3}
                fill="var(--moss-400)"
                initial={{ opacity: 0, y: 0 }}
                animate={{
                  opacity: [0, 1, 0],
                  y: [0, -6, 0],
                  transition: {
                    delay: i * 0.15,
                    duration: 0.8,
                    repeat: Infinity,
                    ease: 'easeInOut',
                  }
                }}
                exit={{ opacity: 0 }}
              />
            ))
          )}
        </AnimatePresence>

        {/* ── CELEBRATION STARS ─────────────────────────────────── */}
        <AnimatePresence>
          {mood === 'celebrating' && (
            [{ x: 55, y: 85 }, { x: 145, y: 90 }, { x: 60, y: 110 }, { x: 140, y: 105 }]
              .map((pos, i) => (
                <motion.text
                  key={i}
                  x={pos.x} y={pos.y}
                  fontSize="16"
                  initial={{ opacity: 0, scale: 0, rotate: -30 }}
                  animate={{
                    opacity: [0, 1, 0],
                    scale: [0, 1.2, 0],
                    y: [0, -15, -30],
                    transition: { delay: i * 0.1, duration: 0.9 }
                  }}
                  exit={{ opacity: 0 }}
                >
                  ✨
                </motion.text>
              ))
          )}
        </AnimatePresence>
      </svg>

      {/* Growth stage label */}
      {growthStage !== 'sprout' && (
        <div
          style={{
            position: 'absolute',
            bottom: -24,
            left: '50%',
            transform: 'translateX(-50%)',
            fontSize: '11px',
            color: 'var(--text-muted)',
            fontFamily: 'var(--font-mono)',
            whiteSpace: 'nowrap',
            textTransform: 'uppercase',
            letterSpacing: '0.08em',
          }}
        >
          {growthStage}
        </div>
      )}
    </div>
  )
}
