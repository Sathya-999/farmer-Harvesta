'use client'

import React from 'react'
import { CheckCircle2, AlertCircle, Info, X } from 'lucide-react'

interface Props {
  type: 'success' | 'error' | 'info'
  message: string
  title?: string
  onDismiss?: () => void
}

export default function AuthFeedback({
  type,
  message,
  title,
  onDismiss,
}: Props) {
  if (!message) return null

  const isSuccess = type === 'success'
  const isError = type === 'error'

  const bgColor = isSuccess
    ? 'rgba(46, 125, 50, 0.1)'
    : isError
    ? 'rgba(211, 47, 47, 0.08)'
    : 'rgba(91, 122, 56, 0.1)'

  const borderColor = isSuccess
    ? '#2E7D32'
    : isError
    ? '#D32F2F'
    : '#71844A'

  const textColor = isSuccess
    ? '#1B5E20'
    : isError
    ? '#B71C1C'
    : '#272A22'

  const Icon = isSuccess ? CheckCircle2 : isError ? AlertCircle : Info

  return (
    <div
      role={isError ? 'alert' : 'status'}
      aria-live="polite"
      style={{
        display: 'flex',
        alignItems: 'flex-start',
        gap: 12,
        padding: '14px 16px',
        borderRadius: 12,
        background: bgColor,
        border: `1.5px solid ${borderColor}`,
        marginBottom: 20,
        boxSizing: 'border-box',
        width: '100%',
      }}
    >
      <Icon size={20} color={borderColor} style={{ flexShrink: 0, marginTop: 1 }} />
      <div style={{ flex: 1, minWidth: 0 }}>
        {title && (
          <div
            style={{
              fontFamily: 'var(--font-display, "Plus Jakarta Sans", sans-serif)',
              fontSize: '14px',
              fontWeight: 700,
              color: textColor,
              marginBottom: 4,
            }}
          >
            {title}
          </div>
        )}
        <p
          style={{
            margin: 0,
            fontFamily: 'var(--font-body, "Inter", sans-serif)',
            fontSize: '13.5px',
            color: textColor,
            lineHeight: 1.45,
          }}
        >
          {message}
        </p>
      </div>
      {onDismiss && (
        <button
          type="button"
          onClick={onDismiss}
          aria-label="Dismiss message"
          style={{
            background: 'none',
            border: 'none',
            padding: 4,
            cursor: 'pointer',
            color: textColor,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            opacity: 0.7,
            borderRadius: 4,
          }}
        >
          <X size={16} />
        </button>
      )}
    </div>
  )
}
