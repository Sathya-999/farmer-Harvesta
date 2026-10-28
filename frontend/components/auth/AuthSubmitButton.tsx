'use client'

import React from 'react'
import { ArrowRight } from 'lucide-react'

interface Props extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  loading?: boolean
  loadingText?: string
  children: React.ReactNode
}

export default function AuthSubmitButton({
  loading = false,
  loadingText = 'Processing...',
  children,
  disabled,
  style,
  ...props
}: Props) {
  const isDisabled = disabled || loading

  return (
    <button
      type="submit"
      disabled={isDisabled}
      aria-busy={loading}
      style={{
        width: '100%',
        height: 52,
        background: isDisabled ? '#4A624E' : '#24442A',
        color: '#FFFFFF',
        border: 'none',
        borderRadius: 12,
        fontFamily: 'var(--font-display, "Plus Jakarta Sans", sans-serif)',
        fontSize: '15.5px',
        fontWeight: 700,
        cursor: isDisabled ? 'not-allowed' : 'pointer',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 10,
        boxShadow: isDisabled
          ? 'none'
          : '0 4px 16px rgba(36, 68, 42, 0.3)',
        transition: 'background 0.15s ease, transform 0.1s ease, box-shadow 0.15s ease',
        boxSizing: 'border-box',
        ...style,
      }}
      {...props}
    >
      {loading ? (
        <>
          <span
            style={{
              width: 18,
              height: 18,
              borderRadius: '50%',
              border: '2px solid rgba(255, 255, 255, 0.35)',
              borderTopColor: '#DFFF28',
              animation: 'spin 0.8s linear infinite',
              display: 'inline-block',
            }}
          />
          <span style={{ color: '#FBF4E4' }}>{loadingText}</span>
        </>
      ) : (
        <>
          <span>{children}</span>
          <ArrowRight size={18} color="#DFFF28" strokeWidth={2.5} />
        </>
      )}
    </button>
  )
}
