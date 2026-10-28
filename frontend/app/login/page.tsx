'use client'

import React, { useCallback, useEffect, useState } from 'react'
import Link from 'next/link'
import { useRouter, useSearchParams } from 'next/navigation'
import { motion, AnimatePresence } from 'framer-motion'
import { Mail, CheckCircle2 } from 'lucide-react'
import { login, isAuthenticated, requestPasswordReset, getSafeRedirectUrl } from '@/lib/authService'
import AuthSplitLayout from '@/components/auth/AuthSplitLayout'
import AgriculturalBrandLogo from '@/components/auth/AgriculturalBrandLogo'
import AuthFormField from '@/components/auth/AuthFormField'
import PasswordInput from '@/components/auth/PasswordInput'
import AuthSubmitButton from '@/components/auth/AuthSubmitButton'
import AuthFeedback from '@/components/auth/AuthFeedback'

function LoginForm() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const isRegistered = searchParams?.get('registered') === 'true'
  const isConfirm = searchParams?.get('confirm') === 'true'
  const redirectParam = searchParams?.get('redirect')

  const [identifier, setIdentifier] = useState('')
  const [password, setPassword] = useState('')
  const [rememberMe, setRememberMe] = useState(false)
  const [loading, setLoading] = useState(false)
  const [errorMsg, setErrorMsg] = useState('')

  // Forgot password modal state
  const [showForgotModal, setShowForgotModal] = useState(false)
  const [resetEmail, setResetEmail] = useState('')
  const [resetLoading, setResetLoading] = useState(false)
  const [resetError, setResetError] = useState('')
  const [resetSuccessMsg, setResetSuccessMsg] = useState('')

  // Redirect if already authenticated
  useEffect(() => {
    if (isAuthenticated()) {
      router.replace('/dashboard')
    }
  }, [router])

  const handleSubmit = useCallback(
    async (e: React.FormEvent) => {
      e.preventDefault()
      if (loading) return

      setErrorMsg('')

      const idTrimmed = identifier.trim()
      if (!idTrimmed) {
        setErrorMsg('Please enter your email address or mobile number.')
        return
      }
      if (!password) {
        setErrorMsg('Please enter your account password.')
        return
      }

      setLoading(true)

      try {
        const result = await login({
          identifier: idTrimmed,
          password,
          rememberMe,
        })

        if (result.ok) {
          // Validate redirect URL to prevent open-redirect vulnerabilities
          const targetUrl = getSafeRedirectUrl(redirectParam, '/dashboard')
          router.push(targetUrl)
        } else {
          setErrorMsg(result.message)
        }
      } catch (err) {
        setErrorMsg('Unable to connect to the authentication service. Please check your network and try again.')
      } finally {
        setLoading(false)
      }
    },
    [identifier, password, rememberMe, loading, router, redirectParam],
  )

  const handleForgotPasswordSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (resetLoading) return
    setResetError('')
    setResetSuccessMsg('')

    const em = resetEmail.trim()
    if (!em) {
      setResetError('Please enter your email address.')
      return
    }

    setResetLoading(true)
    try {
      const res = await requestPasswordReset(em)
      if (res.ok) {
        setResetSuccessMsg(res.message)
      } else {
        setResetError(res.message)
      }
    } catch {
      setResetError('Failed to send reset instructions. Please check your connection.')
    } finally {
      setResetLoading(false)
    }
  }

  return (
    <AuthSplitLayout
      headline={'Your farm.\nBetter informed.'}
      subheadline="Access localized plot telemetry, NDVIs, and official mandi price indices in one integrated agricultural platform."
      tagline="HARVEST(A) AGRI INTELLIGENCE"
      badgeText="Farmer Sign In"
    >
      <div className="auth-form-wrapper" style={{ animation: 'fadeIn 0.3s ease-out' }}>
        {/* Brand Logo Header */}
        <div style={{ marginBottom: 28 }}>
          <AgriculturalBrandLogo size="normal" showLink={true} />
        </div>

        {/* Page Headings */}
        <div style={{ marginBottom: 24 }}>
          <h1
            style={{
              fontFamily: 'var(--font-serif, "Fraunces", Georgia, serif)',
              fontSize: 'clamp(28px, 2.5vw, 34px)',
              fontWeight: 800,
              color: '#272A22',
              margin: '0 0 8px',
              letterSpacing: '-0.025em',
              lineHeight: 1.15,
            }}
          >
            Welcome Back
          </h1>
          <p
            style={{
              fontFamily: 'var(--font-body, "Inter", sans-serif)',
              fontSize: '14.5px',
              color: '#74786C',
              margin: 0,
              lineHeight: 1.5,
            }}
          >
            Sign in to access your agricultural intelligence dashboard and field records.
          </p>
        </div>

        {/* Registration Success Notification Banner */}
        {isRegistered && (
          <AuthFeedback
            type="success"
            title="Account Created"
            message={
              isConfirm
                ? 'Your farmer account has been created. If email verification was sent, please verify your inbox before logging in.'
                : 'Your farmer account is ready! Please enter your credentials below to access your dashboard.'
            }
          />
        )}

        {/* Error Notification Banner */}
        <AnimatePresence>
          {errorMsg && (
            <AuthFeedback
              type="error"
              message={errorMsg}
              onDismiss={() => setErrorMsg('')}
            />
          )}
        </AnimatePresence>

        {/* Login Form */}
        <form onSubmit={handleSubmit} noValidate>
          {/* Email Address or Mobile Field */}
          <AuthFormField
            id="sf-login-identifier"
            name="identifier"
            type="text"
            label="Email Address or Mobile Number"
            required
            autoComplete="username"
            placeholder="farmer@example.com or 10-digit mobile"
            icon={Mail}
            value={identifier}
            onChange={(e) => setIdentifier(e.target.value)}
            disabled={loading}
          />

          {/* Password Field with Visibility Toggle & Forgot Password Link */}
          <PasswordInput
            id="sf-login-password"
            name="password"
            label="Password"
            required
            autoComplete="current-password"
            placeholder="Enter your account password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            disabled={loading}
            rightAction={
              <button
                type="button"
                id="sf-forgot-password-link"
                onClick={() => {
                  setResetEmail(identifier.includes('@') ? identifier : '')
                  setResetError('')
                  setResetSuccessMsg('')
                  setShowForgotModal(true)
                }}
                style={{
                  background: 'none',
                  border: 'none',
                  padding: 0,
                  fontFamily: 'var(--font-body, "Inter", sans-serif)',
                  fontSize: '13px',
                  color: '#24442A',
                  fontWeight: 600,
                  cursor: 'pointer',
                  textDecoration: 'underline',
                  textUnderlineOffset: 3,
                }}
              >
                Forgot password?
              </button>
            }
          />

          {/* Remember Me Checkbox */}
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              marginBottom: 24,
            }}
          >
            <label
              htmlFor="sf-login-remember"
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 8,
                cursor: 'pointer',
                userSelect: 'none',
                fontFamily: 'var(--font-body, "Inter", sans-serif)',
                fontSize: '13.5px',
                color: '#55594C',
              }}
            >
              <input
                id="sf-login-remember"
                name="rememberMe"
                type="checkbox"
                checked={rememberMe}
                onChange={(e) => setRememberMe(e.target.checked)}
                style={{
                  accentColor: '#24442A',
                  width: 16,
                  height: 16,
                  cursor: 'pointer',
                }}
              />
              Remember my session
            </label>
          </div>

          {/* Primary Submit Button */}
          <AuthSubmitButton
            id="sf-login-submit-btn"
            loading={loading}
            loadingText="Signing In..."
          >
            Sign In to Dashboard
          </AuthSubmitButton>
        </form>

        {/* Navigation to Registration */}
        <p
          style={{
            textAlign: 'center',
            fontFamily: 'var(--font-body, "Inter", sans-serif)',
            fontSize: '14px',
            color: '#74786C',
            marginTop: 28,
            marginBottom: 0,
          }}
        >
          Don&apos;t have an account?{' '}
          <Link
            href="/register?new=true"
            id="login-to-register-link"
            style={{
              color: '#24442A',
              fontWeight: 700,
              textDecoration: 'none',
              borderBottom: '1.5px solid #24442A',
              paddingBottom: 1,
            }}
          >
            Create Farmer Account
          </Link>
        </p>
      </div>

      {/* Forgot Password Modal (Active Email Recovery Flow) */}
      {showForgotModal && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="forgot-modal-title"
          style={{
            position: 'fixed',
            inset: 0,
            zIndex: 1000,
            background: 'rgba(22, 58, 36, 0.65)',
            backdropFilter: 'blur(6px)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: 20,
          }}
          onClick={() => setShowForgotModal(false)}
        >
          <div
            style={{
              background: '#FAF8F0',
              borderRadius: 20,
              padding: '30px 26px',
              maxWidth: 440,
              width: '100%',
              boxShadow: '0 24px 50px rgba(0, 0, 0, 0.3)',
              border: '1.5px solid #D5D5BE',
              boxSizing: 'border-box',
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <h3
              id="forgot-modal-title"
              style={{
                fontFamily: 'var(--font-serif, "Fraunces", Georgia, serif)',
                fontSize: '20px',
                fontWeight: 800,
                color: '#272A22',
                margin: '0 0 8px',
              }}
            >
              Reset Account Password
            </h3>
            <p
              style={{
                fontFamily: 'var(--font-body, "Inter", sans-serif)',
                fontSize: '13.5px',
                lineHeight: 1.5,
                color: '#74786C',
                margin: '0 0 20px',
              }}
            >
              Enter your registered farmer email address and we will send you a secure link to reset your credentials.
            </p>

            {resetSuccessMsg ? (
              <div
                style={{
                  background: 'rgba(46, 125, 50, 0.1)',
                  border: '1.5px solid #2E7D32',
                  borderRadius: 12,
                  padding: '16px',
                  marginBottom: 20,
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: '#1B5E20', fontWeight: 700, marginBottom: 4 }}>
                  <CheckCircle2 size={18} />
                  <span>Instructions Sent</span>
                </div>
                <p
                  style={{
                    fontFamily: 'var(--font-body, "Inter", sans-serif)',
                    fontSize: '13px',
                    color: '#1B5E20',
                    margin: 0,
                    lineHeight: 1.5,
                  }}
                >
                  {resetSuccessMsg}
                </p>
              </div>
            ) : (
              <form onSubmit={handleForgotPasswordSubmit}>
                {resetError && (
                  <AuthFeedback type="error" message={resetError} />
                )}

                <AuthFormField
                  id="sf-recovery-email"
                  type="email"
                  label="Registered Email Address"
                  required
                  placeholder="farmer@example.com"
                  icon={Mail}
                  value={resetEmail}
                  onChange={(e) => setResetEmail(e.target.value)}
                  disabled={resetLoading}
                />

                <div style={{ marginTop: 20, display: 'flex', flexDirection: 'column', gap: 10 }}>
                  <AuthSubmitButton
                    id="sf-recovery-submit-btn"
                    loading={resetLoading}
                    loadingText="Sending Recovery Link..."
                  >
                    Send Reset Link
                  </AuthSubmitButton>

                  <button
                    type="button"
                    onClick={() => setShowForgotModal(false)}
                    style={{
                      width: '100%',
                      padding: '12px',
                      borderRadius: 12,
                      background: 'transparent',
                      color: '#74786C',
                      border: '1px solid #D5D5BE',
                      fontFamily: 'var(--font-body, "Inter", sans-serif)',
                      fontSize: '14px',
                      fontWeight: 600,
                      cursor: 'pointer',
                    }}
                  >
                    Cancel
                  </button>
                </div>
              </form>
            )}

            {resetSuccessMsg && (
              <button
                type="button"
                onClick={() => setShowForgotModal(false)}
                style={{
                  width: '100%',
                  padding: '12px',
                  borderRadius: 12,
                  background: '#24442A',
                  color: '#FFFFFF',
                  border: 'none',
                  fontFamily: 'var(--font-body, "Inter", sans-serif)',
                  fontSize: '14px',
                  fontWeight: 600,
                  cursor: 'pointer',
                }}
              >
                Close
              </button>
            )}
          </div>
        </div>
      )}
    </AuthSplitLayout>
  )
}

export default function LoginPage() {
  return (
    <React.Suspense fallback={<div style={{ textAlign: 'center', padding: '60px 0', color: '#24442A' }}>Loading Harvest(a) sign in...</div>}>
      <LoginForm />
    </React.Suspense>
  )
}
