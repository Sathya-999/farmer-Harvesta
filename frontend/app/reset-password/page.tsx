'use client'

import React, { useEffect, useState, useCallback } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { motion, AnimatePresence } from 'framer-motion'
import { Eye, EyeOff, Lock, ArrowLeft, CheckCircle2 } from 'lucide-react'
import { resetPasswordWithToken } from '@/lib/authService'
import AgriculturalBrandLogo from '@/components/auth/AgriculturalBrandLogo'
import AgriculturalHeroPanel from '@/components/auth/AgriculturalHeroPanel'

export default function ResetPasswordPage() {
  const router = useRouter()

  const [recoveryToken, setRecoveryToken] = useState<string | null>(null)
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)

  const [loading, setLoading] = useState(false)
  const [errorMsg, setErrorMsg] = useState('')
  const [success, setSuccess] = useState(false)

  // Extract recovery access_token from hash or query on mount
  useEffect(() => {
    if (typeof window === 'undefined') return

    // 1. Check hash: #access_token=...&type=recovery
    const hash = window.location.hash.substring(1)
    const hashParams = new URLSearchParams(hash)
    const hashToken = hashParams.get('access_token')

    // 2. Check query params: ?token=... or ?code=...
    const urlParams = new URLSearchParams(window.location.search)
    const queryToken = urlParams.get('token') || urlParams.get('code')

    const token = hashToken || queryToken
    if (token) {
      setRecoveryToken(token)
    } else {
      setErrorMsg('No recovery session detected. If you arrived here directly, please request a password reset link from the login page.')
    }
  }, [])

  const handleSubmit = useCallback(
    async (e: React.FormEvent) => {
      e.preventDefault()
      if (loading) return

      setErrorMsg('')

      if (!recoveryToken) {
        setErrorMsg('Recovery token is missing or expired. Please request a new password reset email.')
        return
      }
      if (!newPassword) {
        setErrorMsg('Please enter your new password.')
        return
      }
      if (newPassword.length < 8) {
        setErrorMsg('New password must be at least 8 characters long.')
        return
      }
      if (newPassword !== confirmPassword) {
        setErrorMsg('Passwords do not match. Please ensure both fields are identical.')
        return
      }

      setLoading(true)

      try {
        const result = await resetPasswordWithToken(newPassword, recoveryToken)
        if (result.ok) {
          setSuccess(true)
        } else {
          setErrorMsg(result.message)
        }
      } catch {
        setErrorMsg('An unexpected error occurred while updating your password. Please try again.')
      } finally {
        setLoading(false)
      }
    },
    [recoveryToken, newPassword, confirmPassword, loading],
  )

  return (
    <div
      style={{
        display: 'flex',
        width: '100vw',
        height: '100dvh',
        overflow: 'hidden',
        background: '#FCFCF8',
        margin: 0,
        padding: 0,
      }}
    >
      {/* ── Left Side: Reset Password Panel (50% desktop, 100% mobile) ── */}
      <div
        className="agricultural-auth-side"
        style={{
          flex: '0 0 50%',
          width: '50%',
          height: '100%',
          display: 'flex',
          flexDirection: 'column',
          overflowY: 'auto',
          overflowX: 'hidden',
          position: 'relative',
          background: '#FCFCF8',
        }}
      >
        {/* Top bar with back-to-login link */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '24px 36px 0',
          }}
        >
          <Link
            href="/login"
            id="reset-back-to-login"
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: 7,
              color: '#536157',
              fontFamily: 'Inter, system-ui, sans-serif',
              fontSize: '13.5px',
              fontWeight: 500,
              textDecoration: 'none',
              transition: 'color 0.15s ease',
            }}
          >
            <ArrowLeft size={16} />
            <span>Back to Login</span>
          </Link>

          <span
            style={{
              fontSize: '12px',
              fontWeight: 600,
              color: '#2E7D32',
              background: '#EEF4EA',
              padding: '4px 10px',
              borderRadius: 6,
              fontFamily: 'Inter, system-ui, sans-serif',
            }}
          >
            Security Recovery
          </span>
        </div>

        {/* Center Container */}
        <div
          className="agricultural-auth-container"
          style={{
            flex: 1,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '28px 36px 40px',
            boxSizing: 'border-box',
          }}
        >
          <motion.div
            initial={{ opacity: 0, y: 14 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.45 }}
            style={{
              width: '100%',
              maxWidth: 420,
              boxSizing: 'border-box',
            }}
          >
            {/* Logo */}
            <div style={{ marginBottom: 32 }}>
              <AgriculturalBrandLogo size="normal" showLink={true} />
            </div>

            {/* Heading */}
            <div style={{ marginBottom: 26 }}>
              <h1
                style={{
                  fontFamily: 'var(--font-display, "Plus Jakarta Sans", sans-serif)',
                  fontSize: 'clamp(26px, 2.3vw, 32px)',
                  fontWeight: 800,
                  color: '#163A24',
                  margin: '0 0 8px',
                  letterSpacing: '-0.025em',
                  lineHeight: 1.15,
                }}
              >
                Reset Your Password
              </h1>
              <p
                style={{
                  fontFamily: 'Inter, system-ui, sans-serif',
                  fontSize: '14.5px',
                  color: '#536157',
                  margin: 0,
                  lineHeight: 1.5,
                }}
              >
                Choose a strong new password for your Smart Farmer platform account.
              </p>
            </div>

            {/* Success State */}
            {success ? (
              <motion.div
                id="reset-success-card"
                initial={{ opacity: 0, scale: 0.95 }}
                animate={{ opacity: 1, scale: 1 }}
                style={{
                  background: '#F0FDF4',
                  border: '1.5px solid #86efac',
                  borderRadius: 12,
                  padding: '28px 20px',
                  textAlign: 'center',
                }}
              >
                <div
                  style={{
                    width: 48,
                    height: 48,
                    borderRadius: '50%',
                    background: '#DCFCE7',
                    color: '#15803D',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    margin: '0 auto 16px',
                  }}
                >
                  <CheckCircle2 size={28} />
                </div>
                <h3
                  style={{
                    fontFamily: 'var(--font-display, "Plus Jakarta Sans", sans-serif)',
                    fontSize: '19px',
                    fontWeight: 700,
                    color: '#14532D',
                    margin: '0 0 8px',
                  }}
                >
                  Password Updated!
                </h3>
                <p
                  style={{
                    fontFamily: 'Inter, system-ui, sans-serif',
                    fontSize: '14px',
                    color: '#166534',
                    lineHeight: 1.55,
                    margin: '0 0 24px',
                  }}
                >
                  Your account password has been successfully updated. You can now sign in with your new credentials.
                </p>
                <Link
                  href="/login"
                  id="reset-success-login-btn"
                  style={{
                    display: 'inline-block',
                    width: '100%',
                    padding: '13px',
                    background: '#15803D',
                    color: '#FFFFFF',
                    borderRadius: 10,
                    fontWeight: 600,
                    fontFamily: 'var(--font-display, "Plus Jakarta Sans", sans-serif)',
                    fontSize: '15.5px',
                    textDecoration: 'none',
                    boxSizing: 'border-box',
                  }}
                >
                  Sign In with New Password
                </Link>
              </motion.div>
            ) : (
              <>
                {/* Error Banner */}
                <AnimatePresence>
                  {errorMsg && (
                    <motion.div
                      id="reset-error-banner"
                      role="alert"
                      aria-live="polite"
                      initial={{ opacity: 0, height: 0, marginBottom: 0 }}
                      animate={{ opacity: 1, height: 'auto', marginBottom: 20 }}
                      exit={{ opacity: 0, height: 0, marginBottom: 0 }}
                      style={{
                        fontFamily: 'Inter, system-ui, sans-serif',
                        fontSize: '14px',
                        color: '#B71C1C',
                        padding: '12px 16px',
                        background: 'rgba(183, 28, 28, 0.08)',
                        borderRadius: 8,
                        borderLeft: '4px solid #B71C1C',
                        lineHeight: 1.45,
                      }}
                    >
                      {errorMsg}
                    </motion.div>
                  )}
                </AnimatePresence>

                {/* Form */}
                <form onSubmit={handleSubmit} noValidate>
                  {/* New Password */}
                  <div style={{ marginBottom: 18 }}>
                    <label
                      htmlFor="sf-reset-password"
                      style={{
                        display: 'block',
                        fontFamily: 'Inter, system-ui, sans-serif',
                        fontSize: '13.5px',
                        fontWeight: 600,
                        color: '#163A24',
                        marginBottom: 7,
                      }}
                    >
                      New Password * <span style={{ color: '#8A988D', fontWeight: 400 }}>(Minimum 8 characters)</span>
                    </label>
                    <div
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: 12,
                        padding: '0 16px',
                        height: 50,
                        border: '1px solid #D9E1D8',
                        borderRadius: 10,
                        background: '#FFFFFF',
                      }}
                    >
                      <Lock size={18} color="#8A988D" aria-hidden="true" />
                      <input
                        id="sf-reset-password"
                        name="newPassword"
                        type={showPassword ? 'text' : 'password'}
                        autoComplete="new-password"
                        required
                        placeholder="Enter your new password"
                        value={newPassword}
                        onChange={(e) => setNewPassword(e.target.value)}
                        disabled={loading}
                        style={{
                          flex: 1,
                          height: '100%',
                          border: 'none',
                          background: 'transparent',
                          fontFamily: 'Inter, system-ui, sans-serif',
                          fontSize: '15px',
                          color: '#163A24',
                          outline: 'none',
                          minWidth: 0,
                        }}
                      />
                      <button
                        type="button"
                        onClick={() => setShowPassword((prev) => !prev)}
                        aria-label={showPassword ? 'Hide password' : 'Show password'}
                        style={{
                          background: 'none',
                          border: 'none',
                          padding: 4,
                          cursor: 'pointer',
                          color: '#8A988D',
                          display: 'flex',
                          alignItems: 'center',
                        }}
                      >
                        {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                      </button>
                    </div>
                  </div>

                  {/* Confirm New Password */}
                  <div style={{ marginBottom: 26 }}>
                    <label
                      htmlFor="sf-reset-confirm-password"
                      style={{
                        display: 'block',
                        fontFamily: 'Inter, system-ui, sans-serif',
                        fontSize: '13.5px',
                        fontWeight: 600,
                        color: '#163A24',
                        marginBottom: 7,
                      }}
                    >
                      Confirm New Password *
                    </label>
                    <div
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: 12,
                        padding: '0 16px',
                        height: 50,
                        border: '1px solid #D9E1D8',
                        borderRadius: 10,
                        background: '#FFFFFF',
                      }}
                    >
                      <Lock size={18} color="#8A988D" aria-hidden="true" />
                      <input
                        id="sf-reset-confirm-password"
                        name="confirmNewPassword"
                        type={showConfirmPassword ? 'text' : 'password'}
                        autoComplete="new-password"
                        required
                        placeholder="Re-enter your new password"
                        value={confirmPassword}
                        onChange={(e) => setConfirmPassword(e.target.value)}
                        disabled={loading}
                        style={{
                          flex: 1,
                          height: '100%',
                          border: 'none',
                          background: 'transparent',
                          fontFamily: 'Inter, system-ui, sans-serif',
                          fontSize: '15px',
                          color: '#163A24',
                          outline: 'none',
                          minWidth: 0,
                        }}
                      />
                      <button
                        type="button"
                        onClick={() => setShowConfirmPassword((prev) => !prev)}
                        aria-label={showConfirmPassword ? 'Hide confirm password' : 'Show confirm password'}
                        style={{
                          background: 'none',
                          border: 'none',
                          padding: 4,
                          cursor: 'pointer',
                          color: '#8A988D',
                          display: 'flex',
                          alignItems: 'center',
                        }}
                      >
                        {showConfirmPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                      </button>
                    </div>
                  </div>

                  {/* Submit Button */}
                  <button
                    type="submit"
                    id="sf-reset-submit-btn"
                    disabled={loading || !recoveryToken}
                    aria-busy={loading}
                    style={{
                      width: '100%',
                      height: 52,
                      background: loading || !recoveryToken ? '#487A52' : '#1E5B2A',
                      color: '#FFFFFF',
                      border: 'none',
                      borderRadius: 10,
                      fontFamily: 'var(--font-display, "Plus Jakarta Sans", sans-serif)',
                      fontSize: '16px',
                      fontWeight: 600,
                      cursor: loading || !recoveryToken ? 'not-allowed' : 'pointer',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      gap: 10,
                      boxShadow: '0 4px 14px rgba(30, 91, 42, 0.28)',
                      transition: 'background 0.18s ease, transform 0.1s ease',
                    }}
                  >
                    {loading ? (
                      <>
                        <span
                          style={{
                            width: 18,
                            height: 18,
                            borderRadius: '50%',
                            border: '2px solid rgba(255,255,255,0.3)',
                            borderTopColor: '#FFFFFF',
                            animation: 'spin 0.8s linear infinite',
                            display: 'inline-block',
                          }}
                        />
                        <span>Updating Password...</span>
                      </>
                    ) : (
                      <span>Update Password</span>
                    )}
                  </button>
                </form>
              </>
            )}
          </motion.div>
        </div>
      </div>

      {/* ── Right Side: Agricultural Hero Panel ── */}
      <AgriculturalHeroPanel
        headline="Security &amp;&#10;Identity Protection."
        subheadline="Your farmer account protects sensitive plot boundaries, crop valuation records, and direct agricultural subsidy telemetry."
        tagline="ACCOUNT SECURITY"
      />
    </div>
  )
}
