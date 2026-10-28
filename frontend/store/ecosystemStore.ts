/**
 * Zustand store: EcosystemProvider
 * Holds ambient state — time-of-day, weather, lighting, reduced-motion.
 * Completely independent of business data — designers can iterate
 * on mood without touching app logic.
 */
import { create } from 'zustand'

export type TimeOfDay = 'dawn' | 'morning' | 'midday' | 'afternoon' | 'dusk' | 'night'
export type WeatherState = 'clear' | 'cloudy' | 'overcast' | 'rainy' | 'foggy'

interface LightingRig {
  horizonTop:        string
  horizonBot:        string
  ambientBrightness: number
  sunGlow:           string
  skyOverlay:        string
}

const LIGHTING: Record<TimeOfDay, LightingRig> = {
  dawn: {
    horizonTop:        '#F9B57A',
    horizonBot:        '#FBF4E4',
    ambientBrightness: 0.85,
    sunGlow:           'rgba(242, 181, 122, 0.35)',
    skyOverlay:        'rgba(249, 181, 122, 0.12)',
  },
  morning: {
    horizonTop:        '#87CEEB',
    horizonBot:        '#FBF4E4',
    ambientBrightness: 1.0,
    sunGlow:           'rgba(242, 200, 75, 0.25)',
    skyOverlay:        'rgba(135, 206, 235, 0.06)',
  },
  midday: {
    horizonTop:        '#5BB4D5',
    horizonBot:        '#F0EAD8',
    ambientBrightness: 1.1,
    sunGlow:           'rgba(242, 200, 75, 0.15)',
    skyOverlay:        'rgba(91, 180, 213, 0.08)',
  },
  afternoon: {
    horizonTop:        '#81B8D4',
    horizonBot:        '#F5EDD5',
    ambientBrightness: 0.95,
    sunGlow:           'rgba(242, 200, 75, 0.3)',
    skyOverlay:        'rgba(129, 184, 212, 0.08)',
  },
  dusk: {
    horizonTop:        '#E8716A',
    horizonBot:        '#FBF4E4',
    ambientBrightness: 0.8,
    sunGlow:           'rgba(232, 113, 106, 0.4)',
    skyOverlay:        'rgba(232, 113, 106, 0.15)',
  },
  night: {
    horizonTop:        '#1A1A2E',
    horizonBot:        '#241C0B',
    ambientBrightness: 0.6,
    sunGlow:           'rgba(122, 155, 92, 0.1)',
    skyOverlay:        'rgba(26, 26, 46, 0.4)',
  },
}

interface EcosystemState {
  timeOfDay:      TimeOfDay
  weather:        WeatherState
  lighting:       LightingRig
  reducedMotion:  boolean
  isDarkMode:     boolean

  setTimeOfDay:   (t: TimeOfDay) => void
  setWeather:     (w: WeatherState) => void
  setReducedMotion: (v: boolean) => void
  syncFromClock:  () => void
}

function getTimeOfDay(): TimeOfDay {
  const h = new Date().getHours()
  if (h >= 5  && h < 7)  return 'dawn'
  if (h >= 7  && h < 11) return 'morning'
  if (h >= 11 && h < 14) return 'midday'
  if (h >= 14 && h < 17) return 'afternoon'
  if (h >= 17 && h < 20) return 'dusk'
  return 'night'
}

export const useEcosystemStore = create<EcosystemState>((set) => ({
  timeOfDay:     getTimeOfDay(),
  weather:       'clear',
  lighting:      LIGHTING[getTimeOfDay()],
  reducedMotion: false,
  isDarkMode:    getTimeOfDay() === 'night',

  setTimeOfDay: (t) => set({
    timeOfDay:  t,
    lighting:   LIGHTING[t],
    isDarkMode: t === 'night',
  }),

  setWeather: (w) => set({ weather: w }),
  setReducedMotion: (v) => set({ reducedMotion: v }),

  syncFromClock: () => {
    const t = getTimeOfDay()
    set({ timeOfDay: t, lighting: LIGHTING[t], isDarkMode: t === 'night' })
  },
}))
