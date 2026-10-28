/**
 * Zustand store: GuardianSproutProvider
 *
 * Exposes an imperative react(event) API — any component can trigger
 * a character reaction without owning animation logic.
 *
 * Growth stages: seed → sprout → sapling → tree
 * Persists growth in localStorage so the character evolves over weeks.
 */
import { create } from 'zustand'
import { persist } from 'zustand/middleware'

export type SproutMood =
  | 'idle'
  | 'interested'   // typing username
  | 'focused'      // typing password
  | 'happy'        // correct credentials
  | 'sad'          // wrong credentials
  | 'thinking'     // waiting for API
  | 'celebrating'  // achievement unlocked

export type GrowthStage = 'seed' | 'sprout' | 'sapling' | 'tree'

export type SproutEvent =
  | 'typing-username'
  | 'typing-password'
  | 'correct'
  | 'incorrect'
  | 'thinking'
  | 'idle'
  | 'achievement'
  | 'data-recorded'    // increments growth

const MOOD_MAP: Record<SproutEvent, SproutMood> = {
  'typing-username': 'interested',
  'typing-password': 'focused',
  'correct':         'happy',
  'incorrect':       'sad',
  'thinking':        'thinking',
  'idle':            'idle',
  'achievement':     'celebrating',
  'data-recorded':   'happy',
}

// Growth milestones (data events needed to level up)
const GROWTH_THRESHOLDS = { seed: 0, sprout: 5, sapling: 20, tree: 50 }

interface SproutState {
  mood:         SproutMood
  growthStage:  GrowthStage
  dataEvents:   number
  blinkCount:   number
  leafFlutter:  boolean

  react:        (event: SproutEvent) => void
  incrementGrowth: () => void
  resetMood:    () => void
}

export const useSproutStore = create<SproutState>()(
  persist(
    (set, get) => ({
      mood:        'idle',
      growthStage: 'sprout',
      dataEvents:  0,
      blinkCount:  0,
      leafFlutter: false,

      react: (event: SproutEvent) => {
        const mood = MOOD_MAP[event]
        set({ mood, leafFlutter: event === 'typing-username' })

        if (event === 'data-recorded') {
          get().incrementGrowth()
        }

        // Auto-return to idle after transient states
        if (['happy', 'sad', 'celebrating', 'thinking'].includes(mood)) {
          const capturedMood = mood
          const delay = event === 'incorrect' ? 1800 : event === 'correct' ? 2500 : 3000
          setTimeout(() => {
            if (get().mood === capturedMood) set({ mood: 'idle', leafFlutter: false })
          }, delay)
        }

        if (event === 'typing-username') {
          setTimeout(() => set({ leafFlutter: false }), 500)
        }
      },

      incrementGrowth: () => {
        const events = get().dataEvents + 1
        let stage: GrowthStage = 'seed'
        if (events >= GROWTH_THRESHOLDS.tree)    stage = 'tree'
        else if (events >= GROWTH_THRESHOLDS.sapling) stage = 'sapling'
        else if (events >= GROWTH_THRESHOLDS.sprout)  stage = 'sprout'
        set({ dataEvents: events, growthStage: stage })
      },

      resetMood: () => set({ mood: 'idle', leafFlutter: false }),
    }),
    {
      name: 'guardian-sprout',
      partialize: (s) => ({
        growthStage: s.growthStage,
        dataEvents:  s.dataEvents,
      }),
    }
  )
)
