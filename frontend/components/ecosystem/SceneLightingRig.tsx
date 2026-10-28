'use client'

import { useEffect } from 'react'
import { useTimeOfDay } from '@/hooks/useTimeOfDay'

/**
 * SceneLightingRig
 *
 * Renders nothing visible — just wires the Zustand ecosystem store's
 * lighting values into CSS custom properties on :root every time they change.
 * Every single CSS rule in globals.css (horizon, sun-glow, sky-overlay) reacts
 * automatically. "Free" ambient lighting system.
 */
export default function SceneLightingRig() {
  useTimeOfDay()
  return null
}
