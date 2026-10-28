'use client'

import React, { useCallback, useEffect, useState } from 'react'
import Link from 'next/link'
import { useRouter, useSearchParams } from 'next/navigation'
import { motion, AnimatePresence } from 'framer-motion'
import { Mail, User, Phone, CheckCircle2, ShieldCheck, X } from 'lucide-react'
import { register, isAuthenticated, isOnboardingCompleted, markOnboardingCompleted } from '@/lib/authService'
import AuthSplitLayout from '@/components/auth/AuthSplitLayout'
import AgriculturalBrandLogo from '@/components/auth/AgriculturalBrandLogo'
import AuthFormField from '@/components/auth/AuthFormField'
import PasswordInput from '@/components/auth/PasswordInput'
import AuthSubmitButton from '@/components/auth/AuthSubmitButton'
import AuthFeedback from '@/components/auth/AuthFeedback'

function RegisterForm() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const isDirectNew = searchParams?.get('new') === 'true'

  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')
  const [email, setEmail] = useState('')
  const [phone, setPhone] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [termsAccepted, setTermsAccepted] = useState(false)

  const [loading, setLoading] = useState(false)
  const [errorMsg, setErrorMsg] = useState('')
  const [showTermsModal, setShowTermsModal] = useState(false)

  // Redirect if already authenticated or returning registered user
  useEffect(() => {
    if (isAuthenticated()) {
      router.replace('/dashboard')
      return
    }
    // Returning registered user who is not logged in should open login directly
    if (isOnboardingCompleted() && !isDirectNew) {
      router.replace('/login')
    }
  }, [router, isDirectNew])

  const handleSubmit = useCallback(
    async (e: React.FormEvent) => {
      e.preventDefault()
      if (loading) return

      setErrorMsg('')

      const fName = firstName.trim()
      const lName = lastName.trim()
      const em = email.trim()

      if (!fName) {
        setErrorMsg('Please enter your first name.')
        return
      }
      if (!lName) {
        setErrorMsg('Please enter your last name.')
        return
      }
      if (!em) {
        setErrorMsg('Please enter your email address.')
        return
      }
      if (!password) {
        setErrorMsg('Please enter an account password.')
        return
      }
      if (password.length < 8) {
        setErrorMsg('Password must be at least 8 characters long.')
        return
      }
      if (password !== confirmPassword) {
        setErrorMsg('Passwords do not match. Please ensure both passwords are identical.')
        return
      }
      if (!termsAccepted) {
        setErrorMsg('You must agree to the Agricultural Platform Terms and Privacy Policy to create an account.')
        return
      }

      setLoading(true)

      try {
        const result = await register({
          firstName: fName,
          lastName: lName,
          email: em,
          password,
          phone: phone.trim() || undefined,
          termsAccepted: true,
        })

        if (result.ok) {
          // Persist the onboarding-completed marker only after confirmed successful registration
          markOnboardingCompleted()

          if (result.requiresEmailConfirmation) {
            router.push('/login?registered=true&confirm=true')
          } else {
            router.push('/login?registered=true')
          }
        } else {
          setErrorMsg(result.message)
        }
      } catch (err) {
        setErrorMsg('An unexpected error occurred during registration. Please verify your internet connection.')
      } finally {
        setLoading(false)
      }
    },
    [firstName, lastName, email, phone, password, confirmPassword, termsAccepted, loading, router],
  )

  return (
    <AuthSplitLayout
      headline={'Sow intelligence.\nReap certainty.'}
      subheadline="Create your verified farmer account to map farm plots, track real-time soil hydration, and coordinate mandi sales across Indian APMC markets."
      tagline="HARVEST(A) AGRI INTELLIGENCE"
      badgeText="New Farmer Account"
    >
      <div className="auth-form-wrapper" style={{ animation: 'fadeIn 0.3s ease-out' }}>
        {/* Brand Logo Header */}
        <div style={{ marginBottom: 24 }}>
          <AgriculturalBrandLogo size="normal" showLink={true} />
        </div>

        {/* Page Headings */}
        <div style={{ marginBottom: 22 }}>
          <h1
            style={{
              fontFamily: 'var(--font-serif, "Fraunces", Georgia, serif)',
              fontSize: 'clamp(26px, 2.3vw, 32px)',
              fontWeight: 800,
              color: '#272A22',
              margin: '0 0 8px',
              letterSpacing: '-0.025em',
              lineHeight: 1.15,
            }}
          >
            Create Your Farmer Account
          </h1>
          <p
            style={{
              fontFamily: 'var(--font-body, "Inter", sans-serif)',
              fontSize: '14px',
              color: '#74786C',
              margin: 0,
              lineHeight: 1.5,
            }}
          >
            Join farmers across India growing smarter with data-driven agricultural intelligence.
          </p>
        </div>

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

        {/* Farmer Registration Form */}
        <form onSubmit={handleSubmit} noValidate>
          {/* Name Row: First Name & Last Name */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <AuthFormField
              id="sf-reg-firstname"
              name="firstName"
              type="text"
              label="First Name"
              required
              placeholder="e.g. Ramesh"
              icon={User}
              value={firstName}
              onChange={(e) => setFirstName(e.target.value)}
              disabled={loading}
            />
            <AuthFormField
              id="sf-reg-lastname"
              name="lastName"
              type="text"
              label="Last Name"
              required
              placeholder="e.g. Patel"
              value={lastName}
              onChange={(e) => setLastName(e.target.value)}
              disabled={loading}
            />
          </div>

          {/* Email Address */}
          <AuthFormField
            id="sf-reg-email"
            name="email"
            type="email"
            label="Email Address"
            required
            autoComplete="email"
            placeholder="farmer@example.com"
            icon={Mail}
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            disabled={loading}
          />

          {/* Mobile Number (Optional, for advisory alerts) */}
          <AuthFormField
            id="sf-reg-phone"
            name="phone"
            type="tel"
            label="Mobile Number"
            helperText="Optional — for account contact and advisory updates"
            autoComplete="tel"
            placeholder="+91 98765 43210"
            icon={Phone}
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            disabled={loading}
          />

          {/* Password with Strength Meter */}
          <PasswordInput
            id="sf-reg-password"
            name="password"
            label="Account Password"
            required
            showStrength={true}
            autoComplete="new-password"
            placeholder="Create a strong password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            disabled={loading}
          />

          {/* Confirm Password */}
          <PasswordInput
            id="sf-reg-confirm-password"
            name="confirmPassword"
            label="Confirm Password"
            required
            autoComplete="new-password"
            placeholder="Re-enter your password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            disabled={loading}
          />

          {/* Terms & Conditions Agreement */}
          <div style={{ marginBottom: 24, marginTop: 4 }}>
            <label
              htmlFor="sf-reg-terms"
              style={{
                display: 'flex',
                alignItems: 'flex-start',
                gap: 10,
                cursor: 'pointer',
                userSelect: 'none',
                fontFamily: 'var(--font-body, "Inter", sans-serif)',
                fontSize: '13px',
                color: '#55594C',
                lineHeight: 1.45,
              }}
            >
              <input
                id="sf-reg-terms"
                name="terms"
                type="checkbox"
                checked={termsAccepted}
                onChange={(e) => setTermsAccepted(e.target.checked)}
                style={{
                  accentColor: '#24442A',
                  width: 17,
                  height: 17,
                  marginTop: 2,
                  cursor: 'pointer',
                  flexShrink: 0,
                }}
              />
              <span>
                I agree to the{' '}
                <button
                  type="button"
                  onClick={(e) => {
                    e.preventDefault()
                    setShowTermsModal(true)
                  }}
                  style={{
                    background: 'none',
                    border: 'none',
                    padding: 0,
                    color: '#24442A',
                    fontWeight: 700,
                    textDecoration: 'underline',
                    cursor: 'pointer',
                    fontFamily: 'inherit',
                    fontSize: 'inherit',
                  }}
                >
                  Agricultural Platform Terms
                </button>{' '}
                and{' '}
                <button
                  type="button"
                  onClick={(e) => {
                    e.preventDefault()
                    setShowTermsModal(true)
                  }}
                  style={{
                    background: 'none',
                    border: 'none',
                    padding: 0,
                    color: '#24442A',
                    fontWeight: 700,
                    textDecoration: 'underline',
                    cursor: 'pointer',
                    fontFamily: 'inherit',
                    fontSize: 'inherit',
                  }}
                >
                  Privacy Policy
                </button>
                .
              </span>
            </label>
          </div>

          {/* Primary Submit Button */}
          <AuthSubmitButton
            id="sf-register-submit-btn"
            loading={loading}
            loadingText="Creating Farmer Account..."
          >
            Create Farmer Account
          </AuthSubmitButton>
        </form>

        {/* Link to Login */}
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
          Already have an account?{' '}
          <Link
            href="/login"
            id="register-to-login-link"
            style={{
              color: '#24442A',
              fontWeight: 700,
              textDecoration: 'none',
              borderBottom: '1.5px solid #24442A',
              paddingBottom: 1,
            }}
          >
            Sign In Here
          </Link>
        </p>
      </div>

      {/* Terms of Service & Privacy Policy Modal */}
      {showTermsModal && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="terms-modal-title"
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
          onClick={() => setShowTermsModal(false)}
        >
          <div
            style={{
              background: '#FAF8F0',
              borderRadius: 20,
              padding: '28px 24px',
              maxWidth: 520,
              width: '100%',
              maxHeight: '80vh',
              overflowY: 'auto',
              boxShadow: '0 24px 50px rgba(0, 0, 0, 0.3)',
              border: '1.5px solid #D5D5BE',
              boxSizing: 'border-box',
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                marginBottom: 16,
              }}
            >
              <h3
                id="terms-modal-title"
                style={{
                  fontFamily: 'var(--font-serif, "Fraunces", Georgia, serif)',
                  fontSize: '20px',
                  fontWeight: 800,
                  color: '#272A22',
                  margin: 0,
                }}
              >
                Platform Terms & Privacy Policy
              </h3>
              <button
                type="button"
                onClick={() => setShowTermsModal(false)}
                style={{
                  background: 'none',
                  border: 'none',
                  padding: 4,
                  cursor: 'pointer',
                  color: '#74786C',
                  display: 'flex',
                  alignItems: 'center',
                }}
              >
                <X size={20} />
              </button>
            </div>

            <div
              style={{
                fontSize: '13.5px',
                color: '#55594C',
                lineHeight: 1.6,
                fontFamily: 'var(--font-body, "Inter", sans-serif)',
                display: 'flex',
                flexDirection: 'column',
                gap: 12,
              }}
            >
              <p style={{ margin: 0 }}>
                <strong>1. Agricultural Intelligence Services:</strong> Harvest(a) provides digital tools for farm plot mapping,
                NDVI vegetation monitoring, irrigation scheduling, and APMC mandi market reference data.
                Advisories are based on mathematical and regional agronomic estimates and do not constitute agricultural insurance or financial guarantees.
              </p>
              <p style={{ margin: 0 }}>
                <strong>2. Data Privacy:</strong> Your farm coordinates, soil measurements, and
                crop yield entries are used to generate your personalized platform advisories and are protected according to industry security standards.
              </p>
              <p style={{ margin: 0 }}>
                <strong>3. Account Security:</strong> You are responsible for keeping your credentials confidential.
                Never share your password with unauthorized personnel.
              </p>
            </div>

            <button
              type="button"
              onClick={() => {
                setTermsAccepted(true)
                setShowTermsModal(false)
              }}
              style={{
                marginTop: 20,
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
              Accept Terms & Close
            </button>
          </div>
        </div>
      )}
    </AuthSplitLayout>
  )
}

export default function RegisterPage() {
  return (
    <React.Suspense fallback={<div style={{ textAlign: 'center', padding: '60px 0', color: '#24442A' }}>Loading Harvest(a) registration...</div>}>
      <RegisterForm />
    </React.Suspense>
  )
}
