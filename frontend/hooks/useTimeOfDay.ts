'use client'

import { useEffect, useCallback } from 'react'
import { useEcosystemStore } from '@/store/ecosystemStore'

/**
 * useTimeOfDay
 *
 * Syncs the EcosystemStore time-of-day with real clock every 5 minutes.
 * Applies the lighting rig as CSS custom properties on :root so every
 * CSS rule in globals.css can react to it without re-renders.
 */
export function useTimeOfDay() {
  const { lighting, timeOfDay, syncFromClock } = useEcosystemStore()

  const applyLighting = useCallback(() => {
    if (typeof document === 'undefined') return
    const root = document.documentElement
    root.style.setProperty('--horizon-top',        lighting.horizonTop)
    root.style.setProperty('--horizon-bot',        lighting.horizonBot)
    root.style.setProperty('--ambient-brightness', String(lighting.ambientBrightness))
    root.style.setProperty('--sun-glow',           lighting.sunGlow)
    root.style.setProperty('--sky-overlay',        lighting.skyOverlay)

    // Night mode class toggle
    root.dataset.theme = timeOfDay === 'night' ? 'night' : 'day'
  }, [lighting, timeOfDay])

  useEffect(() => {
    applyLighting()
  }, [applyLighting])

  useEffect(() => {
    const id = setInterval(() => {
      syncFromClock()
    }, 5 * 60 * 1000) // every 5 minutes
    return () => clearInterval(id)
  }, [syncFromClock])

  return { timeOfDay, lighting }
}
