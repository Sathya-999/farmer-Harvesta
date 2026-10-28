'use client'

import React, { useEffect, useRef, useState } from 'react'
import { useRouter } from 'next/navigation'
import Link from 'next/link'
import { isAuthenticated, isOnboardingCompleted } from '@/lib/authService'

// 5-second presentation display duration (5,000 milliseconds) matching Phase 3.A
export const PRESENTATION_DURATION_MS = 5000

export default function AgriculturalSplashScreen() {
  const router = useRouter()
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const hasNavigatedRef = useRef(false)
  const [isRedirecting, setIsRedirecting] = useState(false)
  const [targetRoute, setTargetRoute] = useState('/register')
  const [countdown, setCountdown] = useState(5)

  useEffect(() => {
    // 1. If already authenticated: skip splash, registration, and login -> Dashboard directly
    if (isAuthenticated()) {
      hasNavigatedRef.current = true
      setIsRedirecting(true)
      router.replace('/dashboard')
      return
    }

    // 2. If returning registered user (onboarding completed): skip splash & registration -> Login directly
    if (isOnboardingCompleted()) {
      hasNavigatedRef.current = true
      setIsRedirecting(true)
      router.replace('/login')
      return
    }

    // 3. First-time visitor: Target route is /register
    setTargetRoute('/register')

    // Countdown interval for visual feedback
    const interval = setInterval(() => {
      setCountdown((prev) => (prev > 1 ? prev - 1 : 1))
    }, 1000)

    // Exactly 5,000 milliseconds display duration for first-time onboarding
    timerRef.current = setTimeout(() => {
      if (!hasNavigatedRef.current) {
        hasNavigatedRef.current = true
        router.push('/register')
      }
    }, PRESENTATION_DURATION_MS)

    // Cleanup timer on unmount (prevents leaks & handles React Strict Mode)
    return () => {
      clearInterval(interval)
      if (timerRef.current) {
        clearTimeout(timerRef.current)
        timerRef.current = null
      }
    }
  }, [router])

  const handleManualSkip = (e?: React.MouseEvent) => {
    if (e) e.preventDefault()
    if (timerRef.current) {
      clearTimeout(timerRef.current)
      timerRef.current = null
    }
    if (!hasNavigatedRef.current) {
      hasNavigatedRef.current = true
      if (isAuthenticated()) {
        router.push('/dashboard')
      } else if (isOnboardingCompleted()) {
        router.push('/login')
      } else {
        router.push('/register')
      }
    }
  }

  if (isRedirecting) {
    return (
      <main
        id="splash-screen"
        className="splash-viewport"
        style={{
          position: 'fixed',
          inset: 0,
          zIndex: 99999,
          width: '100vw',
          height: '100vh',
          backgroundColor: '#031A14',
        }}
      />
    )
  }

  return (
    <main
      id="splash-screen"
      role="main"
      aria-label="Harvest(a) Agricultural Intelligence Platform Opening Experience"
      className="splash-viewport"
      style={{
        position: 'fixed',
        inset: 0,
        zIndex: 99999,
        width: '100vw',
        height: '100vh',
        minHeight: '100dvh',
        overflow: 'hidden',
        backgroundColor: '#031A14',
        color: '#F4F0D6',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between',
        alignItems: 'center',
        padding: 'clamp(28px, 4.5vh, 48px) clamp(20px, 4.5vw, 64px)',
        boxSizing: 'border-box',
        userSelect: 'none',
      }}
    >
      {/* ─────────────────────────────────────────────────────────────
          1. BACKGROUND ARTWORK — SUBTLE TOPOGRAPHIC CONTOURS & GLOW
          Matching Reference 1: deep forest green with edge contours
      ───────────────────────────────────────────────────────────── */}
      <div
        className="splash-background-layer"
        aria-hidden="true"
        style={{
          position: 'absolute',
          inset: 0,
          pointerEvents: 'none',
          overflow: 'hidden',
          zIndex: 0,
        }}
      >
        {/* Deep Forest-Green Primary Gradient Background */}
        <div
          style={{
            position: 'absolute',
            inset: 0,
            background:
              'radial-gradient(ellipse 85% 70% at 50% 48%, #05291F 0%, #031A14 68%, #02120E 100%)',
          }}
        />

        {/* Soft Center Illumination Glow behind the Hero */}
        <div
          className="splash-ambient-glow"
          style={{
            position: 'absolute',
            top: '48%',
            left: '50%',
            transform: 'translate(-50%, -50%)',
            width: 'clamp(420px, 68vw, 980px)',
            height: 'clamp(320px, 52vh, 600px)',
            borderRadius: '50%',
            background:
              'radial-gradient(circle, rgba(184, 243, 74, 0.08) 0%, rgba(13, 71, 52, 0.35) 44%, transparent 74%)',
            filter: 'blur(50px)',
            pointerEvents: 'none',
          }}
        />

        {/* Topographic Contour Lines Framing Viewport Edges */}
        <svg
          className="splash-contours-svg"
          viewBox="0 0 1440 900"
          preserveAspectRatio="xMidYMid slice"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
          style={{
            position: 'absolute',
            inset: 0,
            width: '100%',
            height: '100%',
            opacity: 0.72,
          }}
        >
          <defs>
            {/* Fine Geospatial Coordinate Grid */}
            <pattern id="geoGridRef" width="100" height="100" patternUnits="userSpaceOnUse">
              <path
                d="M 100 0 L 0 0 0 100"
                fill="none"
                stroke="rgba(157, 173, 159, 0.04)"
                strokeWidth="0.75"
              />
              <circle cx="0" cy="0" r="1.2" fill="rgba(184, 243, 74, 0.16)" />
            </pattern>

            <linearGradient id="edgeContourGrad" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stopColor="#B8F34A" stopOpacity="0.28" />
              <stop offset="50%" stopColor="#9DAD9F" stopOpacity="0.12" />
              <stop offset="100%" stopColor="#05291F" stopOpacity="0.04" />
            </linearGradient>
          </defs>

          {/* Coordinate Grid */}
          <rect width="100%" height="100%" fill="url(#geoGridRef)" />

          {/* Topographic Elevation Contours Framing the Edges */}
          {/* Upper Left Contours */}
          <path
            d="M -120 180 C 140 120, 320 240, 520 160 C 680 95, 780 140, 940 80"
            stroke="url(#edgeContourGrad)"
            strokeWidth="1.2"
          />
          <path
            d="M -80 320 C 180 260, 340 380, 560 300 C 720 240, 860 310, 1020 220"
            stroke="rgba(157, 173, 159, 0.12)"
            strokeWidth="1"
          />

          {/* Upper Right Contours */}
          <path
            d="M 980 90 C 1140 180, 1280 110, 1540 190"
            stroke="rgba(184, 243, 74, 0.18)"
            strokeWidth="1.2"
          />
          <path
            d="M 920 240 C 1120 310, 1310 220, 1560 280"
            stroke="rgba(157, 173, 159, 0.1)"
            strokeWidth="1"
          />

          {/* Lower Contours framing bottom */}
          <path
            d="M -100 680 C 220 590, 480 740, 780 650 C 1080 560, 1320 710, 1580 620"
            stroke="rgba(157, 173, 159, 0.14)"
            strokeWidth="1.1"
          />
          <path
            d="M -80 820 C 260 740, 540 880, 890 790 C 1180 720, 1380 840, 1560 770"
            stroke="rgba(184, 243, 74, 0.16)"
            strokeWidth="1.3"
          />

          {/* Cartographic Corner Annotation Markings */}
          <g opacity="0.4" fill="#9DAD9F" fontFamily="var(--font-mono, monospace)" fontSize="8.5" letterSpacing="0.1em">
            <text x="64" y="90">HARVEST(A) // GEOSPATIAL</text>
            <text x="64" y="104">21°08&apos;44&quot;N  79°05&apos;18&quot;E</text>
            <text x="1260" y="90">TERRACE ELEV: 312M</text>
            <text x="1260" y="104">NDVI OPTIMAL: 0.76</text>
          </g>
        </svg>

        {/* Ambient Subtle Light Sweep */}
        <div className="splash-light-sweep" aria-hidden="true" />
      </div>

      {/* ─────────────────────────────────────────────────────────────
          2. BRAND SECTION — CENTERED LOGO, WORDMARK & DESCRIPTOR
          Matching Reference 1: Centered at top, compact and elegant
      ───────────────────────────────────────────────────────────── */}
      <header
        className="splash-header"
        style={{
          position: 'relative',
          zIndex: 2,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          textAlign: 'center',
          width: '100%',
          maxWidth: '800px',
          margin: '0 auto',
        }}
      >
        <div
          id="splash-brand-mark"
          style={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            gap: '8px',
          }}
        >
          {/* Logo Mark: Stylized Agricultural Seedling Badge */}
          <div
            className="brand-icon-frame"
            style={{
              width: '46px',
              height: '46px',
              borderRadius: '13px',
              background: 'linear-gradient(145deg, #05291F 0%, #031A14 100%)',
              border: '1.5px solid rgba(184, 243, 74, 0.45)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: '0 6px 20px rgba(2, 18, 14, 0.6), inset 0 1px 1px rgba(184, 243, 74, 0.35)',
              marginBottom: '2px',
            }}
          >
            <svg
              width="24"
              height="24"
              viewBox="0 0 24 24"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
              aria-label="Harvest(a) Logo Icon"
            >
              {/* Primary shoot */}
              <path
                d="M12 21V11C12 6.5 15.5 3.5 20 3.5"
                stroke="#B8F34A"
                strokeWidth="2.2"
                strokeLinecap="round"
              />
              {/* Right Leaf */}
              <path
                d="M12 11C12 7 16 4.5 20 3.5C20 8 17 11 12 11Z"
                fill="#B8F34A"
                fillOpacity="0.3"
                stroke="#B8F34A"
                strokeWidth="1.8"
                strokeLinejoin="round"
              />
              {/* Left Leaf */}
              <path
                d="M12 14C8 14 5 11 4 7C8 7 11 10 12 14Z"
                fill="#9DAD9F"
                fillOpacity="0.35"
                stroke="#9DAD9F"
                strokeWidth="1.6"
                strokeLinejoin="round"
              />
            </svg>
          </div>

          {/* Brand Name */}
          <span
            id="splash-brand-title"
            style={{
              fontFamily: 'var(--font-display, "Plus Jakarta Sans", system-ui, sans-serif)',
              fontSize: 'clamp(24px, 2.5vw, 28px)',
              fontWeight: 800,
              color: '#F4F0D6',
              letterSpacing: '-0.025em',
              lineHeight: 1.1,
            }}
          >
            Harvest
            <span
              style={{
                color: '#B8F34A',
                fontWeight: 700,
                marginLeft: '1px',
              }}
            >
              (a)
            </span>
          </span>

          {/* Brand Descriptor */}
          <span
            id="splash-brand-descriptor"
            style={{
              fontFamily: 'var(--font-body, "Inter", sans-serif)',
              fontSize: '11px',
              fontWeight: 700,
              color: '#9DAD9F',
              letterSpacing: '0.16em',
              textTransform: 'uppercase',
              marginTop: '1px',
            }}
          >
            AGRICULTURAL INTELLIGENCE PLATFORM
          </span>
        </div>
      </header>

      {/* ─────────────────────────────────────────────────────────────
          3. HERO CENTERPIECE — TAGLINE, LARGE HEADLINE, DESCRIPTION
          Matching Reference 1: Large single-line headline with lime glow
      ───────────────────────────────────────────────────────────── */}
      <section
        className="splash-centerpiece"
        style={{
          position: 'relative',
          zIndex: 2,
          maxWidth: '1080px',
          width: '100%',
          margin: '0 auto',
          textAlign: 'center',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          padding: 'clamp(10px, 2.5vh, 24px) 0',
        }}
      >
        {/* Introductory Tagline (Noticeably larger, warm cream, Reference 1 style) */}
        <p
          id="splash-supporting-line"
          className="splash-anim-supporting"
          style={{
            margin: '0 0 clamp(14px, 2vh, 22px)',
            fontFamily: 'var(--font-body, "Inter", system-ui, sans-serif)',
            fontSize: 'clamp(16px, 1.45vw, 20px)',
            fontWeight: 500,
            letterSpacing: '0.02em',
            color: '#F4F0D6',
            lineHeight: 1.4,
          }}
        >
          Rooted in the land. Powered by intelligence.
        </p>

        {/* Main Headline (Targeting ~48–58px desktop single-line, bold, cream + lime glow) */}
        <h1
          id="splash-headline"
          className="splash-anim-headline"
          style={{
            fontFamily: 'var(--font-display, "Plus Jakarta Sans", system-ui, sans-serif)',
            fontSize: 'clamp(34px, 4.4vw, 58px)',
            fontWeight: 800,
            lineHeight: 1.15,
            letterSpacing: '-0.03em',
            color: '#F4F0D6',
            margin: '0 0 clamp(16px, 2.4vh, 24px)',
            maxWidth: '1020px',
            textWrap: 'balance',
            whiteSpace: 'normal',
          }}
        >
          Cultivate Today.{' '}
          <span
            className="headline-lime-accent"
            style={{
              color: '#B8F34A',
              textShadow:
                '0 0 28px rgba(184, 243, 74, 0.42), 0 0 54px rgba(184, 243, 74, 0.22)',
              display: 'inline-block',
            }}
          >
            Shape Tomorrow.
          </span>
        </h1>

        {/* Supporting Description (Exact updated copy, 2 balanced lines, readable ~880px max-width) */}
        <p
          id="splash-description"
          className="splash-anim-description"
          style={{
            fontFamily: 'var(--font-body, "Inter", system-ui, sans-serif)',
            fontSize: 'clamp(16px, 1.38vw, 20px)',
            fontWeight: 400,
            lineHeight: 1.55,
            color: 'rgba(244, 240, 214, 0.85)',
            maxWidth: '890px',
            margin: '0 auto clamp(22px, 3vh, 32px)',
            textWrap: 'pretty',
          }}
        >
          Bringing farmers, cultivation insights, and connected agricultural data together
          to build a smarter, more informed, and resilient farming future.
        </p>

        {/* Enter Now Interaction & Presentation Readiness Cue */}
        <div
          className="splash-anim-enter-area"
          style={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            gap: '12px',
            marginTop: '4px',
          }}
        >
          {/* Subtle Decorative Accent Divider Line */}
          <div
            style={{
              width: '84px',
              height: '2px',
              borderRadius: '999px',
              background: 'linear-gradient(90deg, transparent 0%, #B8F34A 50%, transparent 100%)',
              opacity: 0.8,
            }}
          />

          {/* Calm Readiness Cue (Honest display note: workspace ready) */}
          <span
            style={{
              fontFamily: 'var(--font-mono, "Fragment Mono", monospace)',
              fontSize: '11px',
              fontWeight: 500,
              color: '#9DAD9F',
              letterSpacing: '0.14em',
              textTransform: 'uppercase',
            }}
          >
            YOUR AGRICULTURAL WORKSPACE IS READY
          </span>

          {/* Automatic 5s Countdown Progress Indicator */}
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              fontSize: '11px',
              fontFamily: 'var(--font-mono, monospace)',
              color: '#B8F34A',
              letterSpacing: '0.06em',
            }}
          >
            <span>Opening in {countdown}s</span>
            <div
              style={{
                width: '60px',
                height: '3px',
                borderRadius: '999px',
                background: 'rgba(184, 243, 74, 0.2)',
                overflow: 'hidden',
              }}
            >
              <div
                style={{
                  height: '100%',
                  width: `${((5 - countdown) / 5) * 100}%`,
                  background: '#B8F34A',
                  transition: 'width 1s linear',
                }}
              />
            </div>
          </div>

          {/* Action Links: Enter Now & Obvious Sign In / Login */}
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '12px',
              flexWrap: 'wrap',
              justifyContent: 'center',
            }}
          >
            {/* Interactive Enter Now Button / Link */}
            <Link
              href={targetRoute}
              id="splash-skip-link"
              onClick={handleManualSkip}
              className="splash-enter-button"
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '8px',
                padding: '10px 24px',
                borderRadius: '999px',
                backgroundColor: 'rgba(184, 243, 74, 0.15)',
                border: '1.5px solid rgba(184, 243, 74, 0.55)',
                color: '#B8F34A',
                fontFamily: 'var(--font-body, "Inter", system-ui, sans-serif)',
                fontSize: '15px',
                fontWeight: 700,
                letterSpacing: '0.04em',
                textDecoration: 'none',
                cursor: 'pointer',
                transition: 'all 0.22s ease',
                boxShadow: '0 4px 18px rgba(184, 243, 74, 0.2)',
              }}
            >
              <span>Enter Now</span>
              <span style={{ fontSize: '17px', transform: 'translateX(1px)' }}>&rarr;</span>
            </Link>

            {/* Obvious Login Option for Returning Farmers */}
            <Link
              href="/login"
              id="splash-login-link"
              onClick={(e) => {
                if (timerRef.current) {
                  clearTimeout(timerRef.current)
                  timerRef.current = null
                }
                hasNavigatedRef.current = true
              }}
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '6px',
                padding: '10px 20px',
                borderRadius: '999px',
                backgroundColor: 'rgba(255, 255, 255, 0.05)',
                border: '1.5px solid rgba(157, 173, 159, 0.35)',
                color: '#F4F0D6',
                fontFamily: 'var(--font-body, "Inter", system-ui, sans-serif)',
                fontSize: '14px',
                fontWeight: 600,
                letterSpacing: '0.02em',
                textDecoration: 'none',
                cursor: 'pointer',
                transition: 'all 0.22s ease',
              }}
            >
              <span>Already registered? Sign In</span>
            </Link>
          </div>
        </div>
      </section>

      {/* ─────────────────────────────────────────────────────────────
          4. FOOTER SECTION — CREATOR ATTRIBUTION & MICRO-DETAIL
          Matching Reference 1: Centered at lower section
      ───────────────────────────────────────────────────────────── */}
      <footer
        className="splash-footer"
        style={{
          position: 'relative',
          zIndex: 2,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          gap: '8px',
          width: '100%',
          maxWidth: '800px',
          margin: '0 auto',
          textAlign: 'center',
          paddingBottom: 'max(8px, env(safe-area-inset-bottom))',
        }}
      >
        {/* Creator Attribution */}
        <div
          id="splash-creator-attribution"
          className="splash-anim-attribution"
          style={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            gap: '3px',
          }}
        >
          <span
            style={{
              fontFamily: 'var(--font-body, "Inter", sans-serif)',
              fontSize: '10px',
              fontWeight: 600,
              letterSpacing: '0.2em',
              textTransform: 'uppercase',
              color: '#9DAD9F',
            }}
          >
            CRAFTED WITH PURPOSE BY
          </span>

          <span
            id="splash-creator-name"
            style={{
              fontFamily: 'var(--font-display, "Plus Jakarta Sans", system-ui, sans-serif)',
              fontSize: '15px',
              fontWeight: 700,
              color: '#F4F0D6',
              letterSpacing: '0.01em',
            }}
          >
            Malle Govardhan
          </span>
        </div>

        {/* Fine Separator Line */}
        <div
          style={{
            width: '36px',
            height: '1px',
            backgroundColor: 'rgba(157, 173, 159, 0.25)',
            margin: '2px 0',
          }}
        />

        {/* Bottom Micro-Detail */}
        <div
          id="splash-bottom-micro-detail"
          className="splash-anim-micro"
          style={{
            fontFamily: 'var(--font-body, "Inter", sans-serif)',
            fontSize: '10px',
            fontWeight: 600,
            letterSpacing: '0.24em',
            color: 'rgba(157, 173, 159, 0.75)',
            textTransform: 'uppercase',
          }}
        >
          AGRICULTURE &bull; INTELLIGENCE &bull; PROGRESS
        </div>
      </footer>

      {/* ─────────────────────────────────────────────────────────────
          5. EMBEDDED STYLES & ACCESSIBILITY OVERRIDES
      ───────────────────────────────────────────────────────────── */}
      <style jsx global>{`
        /* Staggered entrance animations */
        .splash-header {
          animation: splashFadeInDown 0.35s cubic-bezier(0.16, 1, 0.3, 1) forwards;
        }

        .splash-anim-supporting {
          animation: splashFadeInUp 0.35s cubic-bezier(0.16, 1, 0.3, 1) 0.04s both;
        }

        .splash-anim-headline {
          animation: splashFadeInUp 0.38s cubic-bezier(0.16, 1, 0.3, 1) 0.08s both;
        }

        .splash-anim-description {
          animation: splashFadeInUp 0.38s cubic-bezier(0.16, 1, 0.3, 1) 0.12s both;
        }

        .splash-anim-enter-area {
          animation: splashFadeIn 0.35s ease-out 0.16s both;
        }

        .splash-anim-attribution {
          animation: splashFadeInUp 0.35s cubic-bezier(0.16, 1, 0.3, 1) 0.18s both;
        }

        .splash-anim-micro {
          animation: splashFadeIn 0.35s ease-out 0.22s both;
        }

        /* Hover & Focus state for Enter Now */
        .splash-enter-button:hover {
          background-color: rgba(184, 243, 74, 0.22) !important;
          border-color: #B8F34A !important;
          box-shadow: 0 6px 24px rgba(184, 243, 74, 0.32) !important;
          transform: translateY(-1px);
        }

        .splash-enter-button:focus-visible {
          outline: 2px solid #B8F34A !important;
          outline-offset: 3px !important;
        }

        /* Ambient subtle light sweep */
        .splash-light-sweep {
          position: absolute;
          inset: -40%;
          background: radial-gradient(
            circle at 50% 50%,
            rgba(184, 243, 74, 0.05) 0%,
            rgba(5, 41, 31, 0.06) 40%,
            transparent 65%
          );
          animation: ambientLightSweep 18s ease-in-out infinite alternate;
          pointer-events: none;
        }

        @keyframes splashFadeInDown {
          from {
            opacity: 0;
            transform: translateY(-12px);
          }
          to {
            opacity: 1;
            transform: translateY(0);
          }
        }

        @keyframes splashFadeInUp {
          from {
            opacity: 0;
            transform: translateY(14px);
          }
          to {
            opacity: 1;
            transform: translateY(0);
          }
        }

        @keyframes splashFadeIn {
          from {
            opacity: 0;
          }
          to {
            opacity: 1;
          }
        }

        @keyframes ambientLightSweep {
          0% {
            transform: translate(-6%, -5%) scale(0.96);
          }
          50% {
            transform: translate(5%, 6%) scale(1.04);
          }
          100% {
            transform: translate(-3%, 3%) scale(1);
          }
        }

        /* Responsive Overrides */
        @media (max-width: 640px) {
          .splash-viewport {
            padding: 24px 18px 16px !important;
          }
        }

        /* Reduced-Motion Accessibility Override */
        @media (prefers-reduced-motion: reduce) {
          .splash-header,
          .splash-anim-supporting,
          .splash-anim-headline,
          .splash-anim-description,
          .splash-anim-enter-area,
          .splash-anim-attribution,
          .splash-anim-micro,
          .splash-light-sweep {
            animation: none !important;
            opacity: 1 !important;
            transform: none !important;
          }
        }
      `}</style>
    </main>
  )
}
