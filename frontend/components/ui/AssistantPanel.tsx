'use client'

import React from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { X, Send, Sprout } from 'lucide-react'
import { useSproutStore } from '@/store/sproutStore'
import GuardianSprout from '@/components/sprout/GuardianSprout'

interface Message {
  id: string
  role: 'user' | 'assistant'
  text: string
  timestamp: Date
}

const SAMPLE_REPLIES: Record<string, string> = {
  default: "Your wheat on Plot 3 is looking strong — 12 days until ideal harvest window. The MSP this season is ₹2,275/quintal, but nearby mandis are showing ₹2,340. I'd hold for 3–5 more days.",
  pest: "I'm seeing elevated armyworm risk in your district. Check the lower leaves on your maize crop. I'd recommend neem-based spray within 48 hours. Want me to find the nearest agricultural input dealer?",
  weather: "Rain expected in 4 days — about 18mm. Good news: it aligns with your wheat's tillering stage. No irrigation needed this week, save ₹240 in pump costs.",
  scheme: "You may be eligible for PM-KISAN's ₹6,000 annual transfer. Your land records are already verified. Want me to walk you through the application? It takes about 8 minutes.",
}

function getReply(input: string): string {
  const lower = input.toLowerCase()
  if (lower.includes('pest') || lower.includes('disease') || lower.includes('bug')) return SAMPLE_REPLIES.pest
  if (lower.includes('rain') || lower.includes('weather') || lower.includes('irrigat')) return SAMPLE_REPLIES.weather
  if (lower.includes('scheme') || lower.includes('kisan') || lower.includes('government')) return SAMPLE_REPLIES.scheme
  return SAMPLE_REPLIES.default
}

interface Props {
  open: boolean
  onClose: () => void
}

export default function AssistantPanel({ open, onClose }: Props) {
  const { react } = useSproutStore()
  const [messages, setMessages] = React.useState<Message[]>([
    {
      id: '0',
      role: 'assistant',
      text: "Good morning, Ramesh. Your fields look healthy today. What's on your mind?",
      timestamp: new Date(),
    }
  ])
  const [input, setInput] = React.useState('')
  const [typing, setTyping]  = React.useState(false)
  const endRef = React.useRef<HTMLDivElement>(null)

  const send = async () => {
    if (!input.trim()) return
    const userMsg: Message = { id: Date.now().toString(), role: 'user', text: input, timestamp: new Date() }
    setMessages(prev => [...prev, userMsg])
    setInput('')
    setTyping(true)
    react('thinking')

    await new Promise(r => setTimeout(r, 1200 + Math.random() * 600))
    const reply = getReply(input)
    setMessages(prev => [...prev, { id: (Date.now() + 1).toString(), role: 'assistant', text: reply, timestamp: new Date() }])
    setTyping(false)
    react('correct')
    setTimeout(() => endRef.current?.scrollIntoView({ behavior: 'smooth' }), 50)
  }

  React.useEffect(() => {
    if (open) {
      setTimeout(() => endRef.current?.scrollIntoView({ behavior: 'smooth' }), 300)
    }
  }, [open, messages.length])

  return (
    <AnimatePresence>
      {open && (
        <div className="mist-panel">
          {/* Overlay */}
          <motion.div
            className="mist-overlay"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
          />

          {/* Drawer — rises from the horizon */}
          <motion.div
            className="mist-drawer"
            initial={{ y: '100%', filter: 'blur(12px)' }}
            animate={{ y: 0, filter: 'blur(0px)' }}
            exit={{ y: '100%', filter: 'blur(8px)' }}
            transition={{ duration: 0.35, ease: [0.22, 1, 0.36, 1] }}
            style={{ maxHeight: '80vh', display: 'flex', flexDirection: 'column' }}
          >
            {/* Header */}
            <div style={{ display: 'flex', alignItems: 'center', gap: 16, marginBottom: 20, flexShrink: 0 }}>
              <div style={{ width: 56, height: 56, flexShrink: 0 }}>
                <GuardianSprout size={56} showParticles={false} />
              </div>
              <div style={{ flex: 1 }}>
                <h2 style={{ fontFamily: 'var(--font-display)', fontSize: 18, fontWeight: 600, margin: 0, color: 'var(--text-primary)' }}>
                  Your field guide
                </h2>
                <p style={{ fontSize: 12, color: 'var(--text-muted)', margin: '2px 0 0', fontFamily: 'var(--font-mono)' }}>
                  {typing ? 'thinking…' : 'ready to help'}
                </p>
              </div>
              <motion.button
                onClick={onClose}
                whileHover={{ scale: 1.1 }}
                whileTap={{ scale: 0.9 }}
                style={{ background: 'none', border: 'none', cursor: 'pointer', padding: 8, color: 'var(--text-muted)', borderRadius: 8 }}
                aria-label="Close assistant"
              >
                <X size={20} />
              </motion.button>
            </div>

            {/* Messages */}
            <div style={{ flex: 1, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: 12, paddingBottom: 8 }}>
              {messages.map((msg, i) => (
                <motion.div
                  key={msg.id}
                  initial={{ opacity: 0, y: 12 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: i === messages.length - 1 ? 0 : 0, duration: 0.3 }}
                  style={{
                    display: 'flex',
                    justifyContent: msg.role === 'user' ? 'flex-end' : 'flex-start',
                  }}
                >
                  <div style={{
                    maxWidth: '82%',
                    padding: '12px 16px',
                    borderRadius: msg.role === 'user' ? '18px 18px 4px 18px' : '18px 18px 18px 4px',
                    background: msg.role === 'user'
                      ? 'var(--canopy-700)'
                      : 'rgba(122, 155, 92, 0.12)',
                    color: msg.role === 'user' ? 'var(--dawn-50)' : 'var(--text-primary)',
                    fontSize: 14,
                    lineHeight: 1.55,
                    fontFamily: 'var(--font-body)',
                    border: msg.role === 'assistant' ? '1px solid var(--border-soft)' : 'none',
                  }}>
                    {msg.text}
                  </div>
                </motion.div>
              ))}

              {/* Typing indicator */}
              <AnimatePresence>
                {typing && (
                  <motion.div
                    initial={{ opacity: 0, y: 8 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0 }}
                    style={{ display: 'flex', alignItems: 'center', gap: 4, padding: '10px 14px', background: 'rgba(122, 155, 92, 0.08)', borderRadius: '18px 18px 18px 4px', width: 'fit-content' }}
                  >
                    {[0, 1, 2].map(i => (
                      <motion.span
                        key={i}
                        style={{ width: 6, height: 6, borderRadius: '50%', background: 'var(--moss-400)', display: 'inline-block' }}
                        animate={{ y: [0, -4, 0] }}
                        transition={{ duration: 0.6, delay: i * 0.15, repeat: Infinity }}
                      />
                    ))}
                  </motion.div>
                )}
              </AnimatePresence>
              <div ref={endRef} />
            </div>

            {/* Input */}
            <div style={{ display: 'flex', gap: 10, marginTop: 16, flexShrink: 0 }}>
              <input
                className="field-input"
                placeholder="Ask about crops, prices, weather, schemes…"
                value={input}
                onChange={e => setInput(e.target.value)}
                onKeyDown={e => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); send() } }}
                style={{ flex: 1, borderRadius: 20 }}
                aria-label="Message to AI assistant"
              />
              <motion.button
                onClick={send}
                disabled={!input.trim() || typing}
                whileHover={{ scale: 1.08 }}
                whileTap={{ scale: 0.92 }}
                style={{
                  width: 46, height: 46, borderRadius: '50%', border: 'none', cursor: 'pointer',
                  background: 'linear-gradient(135deg, var(--canopy-700), var(--canopy-500))',
                  color: 'var(--dawn-50)', display: 'flex', alignItems: 'center', justifyContent: 'center',
                  flexShrink: 0, opacity: !input.trim() || typing ? 0.5 : 1,
                }}
                aria-label="Send message"
              >
                <Send size={18} />
              </motion.button>
            </div>

            {/* Quick prompts */}
            <div style={{ display: 'flex', gap: 8, marginTop: 12, flexWrap: 'wrap', flexShrink: 0 }}>
              {['Pest risk today?', 'Rain forecast?', 'Best time to sell?', 'Scheme eligibility'].map(q => (
                <motion.button
                  key={q}
                  onClick={() => { setInput(q); }}
                  whileHover={{ y: -2 }}
                  style={{
                    padding: '6px 12px', borderRadius: 20, border: '1px solid var(--border-soft)',
                    background: 'transparent', fontFamily: 'var(--font-body)', fontSize: 12,
                    color: 'var(--text-secondary)', cursor: 'pointer',
                  }}
                >
                  {q}
                </motion.button>
              ))}
            </div>
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  )
}
