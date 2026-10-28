'use client'

import React, { useRef, useCallback, useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { Eye, EyeOff } from 'lucide-react'
import { useSproutStore } from '@/store/sproutStore'

export interface AuthSubmitData {
  mode: 'login' | 'signup'
  identifier: string
  password: string
  firstName?: string
  lastName?: string
}

interface Props {
  onSubmit: (data: AuthSubmitData) => Promise<'ok' | 'error' | string>
  loading?: boolean
}

/* ── Coordinated White-Side Color System ───────────────────── */
const COLOR = {
  heading:      '#163A24',
  body:         '#536157',
  inputBg:      '#FFFFFF',
  inputBorder:  '#D9E1D8',
  inputFocus:   '#6B9E68',
  focusRing:    'rgba(107, 158, 104, 0.22)',
  btnPrimary:   '#1E5B2A',
  btnHover:     '#174A22',
  link:         '#2F7A36',
  softGreen:    '#EEF4EA',
  accentGold:   '#D9A441',
  error:        '#B71C1C',
  errorBg:      'rgba(183, 28, 28, 0.08)',
}

export default function AuthFormPanel({ onSubmit, loading = false }: Props) {
  const { react } = useSproutStore()

  const [email, setEmail]               = useState('')
  const [password, setPassword]         = useState('')
  const [errorMsg, setErrorMsg]         = useState('')
  const [mode, setMode]                 = useState<'login' | 'signup'>('login')
  const [shake, setShake]               = useState(false)
  const [showPassword, setShowPassword] = useState(false)
  const [rememberMe, setRememberMe]     = useState(false)
  const [firstName, setFirstName]       = useState('')
  const [lastName, setLastName]         = useState('')

  const flutterRef = useRef(false)

  const handleEmailChange = useCallback((v: string) => {
    setEmail(v)
    if (!flutterRef.current) {
      react('typing-username')
      flutterRef.current = true
      setTimeout(() => { flutterRef.current = false }, 400)
    }
  }, [react])

  const handleSubmit = useCallback(async (e: React.FormEvent) => {
    e.preventDefault()
    setErrorMsg('')

    if (mode === 'signup') {
      if (!firstName.trim()) {
        setErrorMsg('Please enter your first name.')
        return
      }
      if (!lastName.trim()) {
        setErrorMsg('Please enter your last name.')
        return
      }
    }

    if (!email.trim()) {
      setErrorMsg('Please enter your mobile number or email.')
      return
    }

    if (!password || password.length < 6) {
      setErrorMsg('Password must be at least 6 characters.')
      return
    }

    react('thinking')
    const result = await onSubmit({
      mode,
      identifier: email.trim(),
      password,
      firstName: firstName.trim(),
      lastName: lastName.trim(),
    })

    if (result === 'ok') {
      react('correct')
    } else {
      react('incorrect')
      setShake(true)
      setErrorMsg(result === 'error'
        ? (mode === 'login' ? 'The mobile number or email and password do not match.' : 'Registration failed. Please check your details.')
        : result,
      )
      setTimeout(() => setShake(false), 500)
    }
  }, [mode, email, password, firstName, lastName, onSubmit, react])

  return (
    <div style={{ width: '100%' }}>

      {/* ── Heading Block ── */}
      <div style={{ marginBottom: 30 }}>
        <h1
          style={{
            fontFamily: 'var(--font-display)',
            fontSize: 'clamp(30px, 2.5vw, 36px)',
            fontWeight: 800,
            color: COLOR.heading,
            margin: '0 0 8px',
            letterSpacing: '-0.025em',
            lineHeight: 1.15,
          }}
        >
          {mode === 'login' ? 'Welcome back!' : 'Create account'}
        </h1>
        <p
          style={{
            fontFamily: 'Inter, system-ui, sans-serif',
            fontSize: '15px',
            color: COLOR.body,
            margin: 0,
            lineHeight: 1.5,
          }}
        >
          {mode === 'login'
            ? 'Sign in to continue to your account'
            : 'Start your smart farming journey today'}
        </p>
      </div>

      {/* ── Form ── */}
      <motion.form
        onSubmit={handleSubmit}
        animate={shake ? { x: [-6, 6, -5, 5, -2, 0] } : { x: 0 }}
        transition={{ duration: 0.35 }}
        noValidate
      >

        {/* Signup: Balanced 2-Column Grid for First name & Last name */}
        <AnimatePresence initial={false}>
          {mode === 'signup' && (
            <motion.div
              key="names"
              initial={{ height: 0, opacity: 0 }}
              animate={{ height: 'auto', opacity: 1 }}
              exit={{ height: 0, opacity: 0 }}
              transition={{ duration: 0.25 }}
              style={{ overflow: 'hidden' }}
            >
              <div
                style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(2, minmax(0, 1fr))',
                  gap: 14,
                  marginBottom: 16,
                  width: '100%',
                  boxSizing: 'border-box',
                }}
              >
                <FieldGroup id="sf-fname" label="First name">
                  <FieldIcon><UserIcon /></FieldIcon>
                  <input
                    id="sf-fname"
                    style={inputEl}
                    type="text"
                    placeholder="Rajasekhar"
                    value={firstName}
                    onChange={e => setFirstName(e.target.value)}
                    autoComplete="given-name"
                    required={mode === 'signup'}
                  />
                </FieldGroup>
                <FieldGroup id="sf-lname" label="Last name">
                  <FieldIcon><UserIcon /></FieldIcon>
                  <input
                    id="sf-lname"
                    style={inputEl}
                    type="text"
                    placeholder="G"
                    value={lastName}
                    onChange={e => setLastName(e.target.value)}
                    autoComplete="family-name"
                    required={mode === 'signup'}
                  />
                </FieldGroup>
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Mobile / Email */}
        <FieldGroup id="sf-identifier" label="Mobile Number or Email" style={{ marginBottom: 16 }}>
          <FieldIcon><UserIcon /></FieldIcon>
          <input
            id="sf-identifier"
            style={inputEl}
            type="text"
            inputMode="email"
            placeholder="Mobile Number or Email"
            value={email}
            onChange={e => handleEmailChange(e.target.value)}
            onFocus={() => react('typing-username')}
            onBlur={() => react('idle')}
            required
            autoComplete="username"
            aria-describedby={errorMsg ? 'sf-error' : undefined}
          />
        </FieldGroup>

        {/* Password */}
        <FieldGroup id="sf-password" label="Password" style={{ marginBottom: 16 }}>
          <FieldIcon><LockIcon /></FieldIcon>
          <input
            id="sf-password"
            style={{ ...inputEl, paddingRight: 48 }}
            type={showPassword ? 'text' : 'password'}
            placeholder="Password (minimum 6 characters)"
            value={password}
            onChange={e => setPassword(e.target.value)}
            onFocus={() => react('typing-password')}
            onBlur={() => react('idle')}
            required
            autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
            aria-describedby={errorMsg ? 'sf-error' : undefined}
          />
          <button
            type="button"
            onClick={() => setShowPassword(v => !v)}
            aria-label={showPassword ? 'Hide password' : 'Show password'}
            style={{
              position: 'absolute',
              right: 14,
              top: '50%',
              transform: 'translateY(-50%)',
              background: 'none',
              border: 'none',
              cursor: 'pointer',
              color: '#8A988D',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              padding: 6,
              borderRadius: 6,
              transition: 'color 0.15s ease',
            }}
          >
            {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
          </button>
        </FieldGroup>

        {/* Remember me + Forgot password (Login mode only) */}
        {mode === 'login' && (
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 24 }}>
            <label
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 9,
                cursor: 'pointer',
                fontFamily: 'Inter, system-ui, sans-serif',
                fontSize: '14px',
                color: COLOR.body,
                userSelect: 'none',
              }}
            >
              <input
                type="checkbox"
                checked={rememberMe}
                onChange={e => setRememberMe(e.target.checked)}
                style={{
                  width: 17,
                  height: 17,
                  accentColor: COLOR.btnPrimary,
                  cursor: 'pointer',
                  borderRadius: 4,
                }}
              />
              Remember me
            </label>
            <a
              href="#"
              style={{
                fontFamily: 'Inter, system-ui, sans-serif',
                fontSize: '14px',
                color: COLOR.link,
                textDecoration: 'none',
                fontWeight: 600,
              }}
            >
              Forgot password?
            </a>
          </div>
        )}

        {/* Error Notification */}
        <AnimatePresence>
          {errorMsg && (
            <motion.p
              id="sf-error"
              role="alert"
              aria-live="polite"
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: 'auto', marginBottom: 18 }}
              exit={{ opacity: 0, height: 0, marginBottom: 0 }}
              style={{
                fontFamily: 'Inter, system-ui, sans-serif',
                fontSize: '14px',
                color: COLOR.error,
                padding: '12px 16px',
                background: COLOR.errorBg,
                borderRadius: 8,
                borderLeft: `3.5px solid ${COLOR.error}`,
                margin: 0,
                lineHeight: 1.45,
              }}
            >
              {errorMsg}
            </motion.p>
          )}
        </AnimatePresence>

        {/* Action Button */}
        <SubmitButton loading={loading} mode={mode} />
      </motion.form>

      {/* Account Mode Switch Link */}
      <p
        style={{
          textAlign: 'center',
          fontFamily: 'Inter, system-ui, sans-serif',
          fontSize: '14.5px',
          color: COLOR.body,
          marginTop: 26,
          marginBottom: 0,
        }}
      >
        {mode === 'login' ? (
          <>
            Don&apos;t have an account?{' '}
            <button
              type="button"
              onClick={() => { setMode('signup'); setErrorMsg('') }}
              style={textLinkStyle}
            >
              Create Account
            </button>
          </>
        ) : (
          <>
            Already have an account?{' '}
            <button
              type="button"
              onClick={() => { setMode('login'); setErrorMsg('') }}
              style={textLinkStyle}
            >
              Sign In
            </button>
          </>
        )}
      </p>
    </div>
  )
}

/* ── Coordinated Sub-Components ────────────────────────────── */

function FieldGroup({
  id, label, children, style = {},
}: {
  id: string
  label: string
  children: React.ReactNode
  style?: React.CSSProperties
}) {
  return (
    <div style={{ width: '100%', minWidth: 0, boxSizing: 'border-box', ...style }}>
      <label
        htmlFor={id}
        style={{
          display: 'block',
          fontFamily: 'Inter, system-ui, sans-serif',
          fontSize: '13.5px',
          fontWeight: 500,
          color: COLOR.heading,
          marginBottom: 7,
        }}
      >
        {label}
      </label>
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 12,
          padding: '0 16px',
          height: 52,
          border: `1px solid ${COLOR.inputBorder}`,
          borderRadius: 10,
          background: COLOR.inputBg,
          position: 'relative',
          transition: 'border-color 0.18s ease, box-shadow 0.18s ease',
          boxSizing: 'border-box',
          width: '100%',
        }}
        onFocusCapture={e => {
          (e.currentTarget as HTMLDivElement).style.borderColor = COLOR.inputFocus
          ;(e.currentTarget as HTMLDivElement).style.boxShadow = `0 0 0 3px ${COLOR.focusRing}`
        }}
        onBlurCapture={e => {
          (e.currentTarget as HTMLDivElement).style.borderColor = COLOR.inputBorder
          ;(e.currentTarget as HTMLDivElement).style.boxShadow = 'none'
        }}
      >
        {children}
      </div>
    </div>
  )
}

function FieldIcon({ children }: { children: React.ReactNode }) {
  return (
    <span style={{ display: 'flex', alignItems: 'center', flexShrink: 0, color: '#8A988D' }}>
      {children}
    </span>
  )
}

function SubmitButton({ loading, mode }: { loading: boolean; mode: 'login' | 'signup' }) {
  const [hovered, setHovered] = useState(false)
  const [pressed, setPressed] = useState(false)

  return (
    <button
      type="submit"
      disabled={loading}
      aria-busy={loading}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => { setHovered(false); setPressed(false) }}
      onMouseDown={() => setPressed(true)}
      onMouseUp={() => setPressed(false)}
      style={{
        width: '100%',
        height: 52,
        background: loading ? '#487A52' : hovered ? COLOR.btnHover : COLOR.btnPrimary,
        color: '#FFFFFF',
        border: 'none',
        borderRadius: 10,
        fontFamily: 'var(--font-display)',
        fontSize: '16px',
        fontWeight: 600,
        letterSpacing: '0.01em',
        cursor: loading ? 'not-allowed' : 'pointer',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 9,
        transition: 'background 0.18s ease, transform 0.1s ease',
        transform: pressed && !loading ? 'scale(0.99)' : hovered && !loading ? 'translateY(-1px)' : 'none',
        boxShadow: hovered && !loading ? '0 4px 14px rgba(30, 91, 42, 0.28)' : '0 2px 6px rgba(30, 91, 42, 0.16)',
      }}
    >
      {loading ? (
        <><DotsLoader />{mode === 'login' ? 'Signing in…' : 'Creating account…'}</>
      ) : (
        <>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4" />
            <polyline points="10 17 15 12 10 7" />
            <line x1="15" y1="12" x2="3" y2="12" />
          </svg>
          {mode === 'login' ? 'Sign In' : 'Create Account'}
        </>
      )}
    </button>
  )
}

function DotsLoader() {
  return (
    <span aria-hidden="true" style={{ display: 'inline-flex', gap: 4, alignItems: 'center' }}>
      {[0, 1, 2].map(i => (
        <motion.span
          key={i}
          style={{ width: 4.5, height: 4.5, borderRadius: '50%', background: 'currentColor', display: 'inline-block' }}
          animate={{ opacity: [0.3, 1, 0.3], y: [0, -3, 0] }}
          transition={{ duration: 0.6, delay: i * 0.15, repeat: Infinity }}
        />
      ))}
    </span>
  )
}

function UserIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" /><circle cx="12" cy="7" r="4" />
    </svg>
  )
}

function LockIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <rect x="3" y="11" width="18" height="11" rx="2" ry="2" /><path d="M7 11V7a5 5 0 0 1 10 0v4" />
    </svg>
  )
}

/* ── Inline style constants ─────────────────────────────────── */

const inputEl: React.CSSProperties = {
  flex: 1,
  height: '100%',
  border: 'none',
  background: 'transparent',
  fontFamily: 'Inter, system-ui, sans-serif',
  fontSize: '15px',
  color: COLOR.heading,
  outline: 'none',
  minWidth: 0,
}

const textLinkStyle: React.CSSProperties = {
  background: 'none',
  border: 'none',
  padding: 0,
  color: COLOR.link,
  fontWeight: 600,
  fontSize: '14.5px',
  cursor: 'pointer',
  fontFamily: 'Inter, system-ui, sans-serif',
  textDecoration: 'none',
}
