'use client'

import React, { useState } from 'react'
import { Send, Mic, Sparkles, X } from 'lucide-react'
import { motion, AnimatePresence } from 'framer-motion'

interface Props {
  fieldName?: string
}

export default function AssistantBar({ fieldName = 'Field 01' }: Props) {
  const [query, setQuery] = useState('')
  const [responseMsg, setResponseMsg] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!query.trim()) return

    setIsSubmitting(true)
    setTimeout(() => {
      setResponseMsg(
        `Field Assistant: For ${fieldName}, soil moisture is tracking at 32.4% (adequate). The current conditions are ideal for planting leafy crops. Next irrigation recommended at 06:00 AM.`
      )
      setIsSubmitting(false)
      setQuery('')
    }, 400)
  }

  return (
    <div style={{ width: '100%', maxWidth: 360, position: 'relative' }}>
      <form
        onSubmit={handleSubmit}
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 8,
          background: 'rgba(246, 244, 235, 0.94)',
          backdropFilter: 'blur(14px)',
          borderRadius: 999,
          padding: '4px 6px 4px 14px',
          boxShadow: '0 4px 18px rgba(0, 0, 0, 0.12)',
        }}
      >
        {/* Sparkle Icon */}
        <div style={{ color: '#7E8376', display: 'flex', alignItems: 'center' }}>
          <Sparkles size={16} />
        </div>

        {/* Input Field */}
        <input
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="How can I help you?"
          style={{
            flex: 1,
            minWidth: 0,
            border: 'none',
            background: 'transparent',
            outline: 'none',
            fontSize: 12.5,
            color: '#22281D',
            fontWeight: 500,
          }}
        />

        {/* Lime-yellow Send Plane Button */}
        <button
          type="submit"
          disabled={isSubmitting}
          title="Send"
          style={{
            width: 30,
            height: 30,
            borderRadius: '50%',
            background: '#D4FD33',
            color: '#1A2215',
            border: 'none',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            cursor: 'pointer',
            boxShadow: '0 2px 6px rgba(212, 253, 51, 0.4)',
            flexShrink: 0,
          }}
        >
          <Send size={13} strokeWidth={2.4} style={{ marginLeft: -1 }} />
        </button>

        {/* Microphone Button */}
        <button
          type="button"
          title="Voice query"
          onClick={() => setQuery('What is the best crop for this week?')}
          style={{
            width: 30,
            height: 30,
            borderRadius: '50%',
            background: 'transparent',
            border: 'none',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#555C4D',
            cursor: 'pointer',
            flexShrink: 0,
          }}
        >
          <Mic size={16} strokeWidth={2} />
        </button>
      </form>

      {/* Interactive Response Banner */}
      <AnimatePresence>
        {responseMsg && (
          <motion.div
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 8 }}
            style={{
              position: 'absolute',
              bottom: 46,
              left: 0,
              right: 0,
              background: '#24442A',
              color: '#FBF4E4',
              borderRadius: 16,
              padding: '12px 16px',
              fontSize: 12,
              lineHeight: 1.45,
              boxShadow: '0 10px 30px rgba(0,0,0,0.25)',
              border: '1px solid #385C30',
              zIndex: 30,
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 6, marginBottom: 4 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 6, color: '#D4FD33', fontWeight: 700, fontSize: 11 }}>
                <Sparkles size={13} />
                <span>FIELD ASSISTANT</span>
              </div>
              <button
                type="button"
                onClick={() => setResponseMsg(null)}
                style={{ background: 'none', border: 'none', color: '#A0B298', cursor: 'pointer', padding: 2 }}
              >
                <X size={14} />
              </button>
            </div>
            {responseMsg}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
}
