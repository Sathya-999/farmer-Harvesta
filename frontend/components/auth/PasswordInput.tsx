'use client'

import React, { useState } from 'react'
import { Lock, Eye, EyeOff } from 'lucide-react'

interface Props extends React.InputHTMLAttributes<HTMLInputElement> {
  id: string
  label: string
  error?: string
  required?: boolean
  showStrength?: boolean
  rightAction?: React.ReactNode
}

export default function PasswordInput({
  id,
  label,
  error,
  required,
  showStrength = false,
  rightAction,
  disabled,
  value,
  style,
  ...inputProps
}: Props) {
  const [showPassword, setShowPassword] = useState(false)

  // Compute password strength for registration
  const strVal = String(value || '')
  let strengthScore = 0
  if (strVal.length >= 8) strengthScore += 1
  if (/[A-Z]/.test(strVal) && /[a-z]/.test(strVal)) strengthScore += 1
  if (/[0-9]/.test(strVal)) strengthScore += 1
  if (/[^A-Za-z0-9]/.test(strVal)) strengthScore += 1

  const strengthLabel =
    strVal.length === 0
      ? ''
      : strengthScore <= 1
      ? 'Weak'
      : strengthScore <= 3
      ? 'Medium'
      : 'Strong'

  const strengthColor =
    strengthScore <= 1
      ? '#D32F2F'
      : strengthScore <= 3
      ? '#E65100'
      : '#2E7D32'

  return (
    <div style={{ marginBottom: 18, width: '100%', boxSizing: 'border-box' }}>
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          marginBottom: 7,
        }}
      >
        <label
          htmlFor={id}
          style={{
            fontFamily: 'var(--font-body, "Inter", sans-serif)',
            fontSize: '13.5px',
            fontWeight: 600,
            color: '#272A22',
            lineHeight: 1.2,
          }}
        >
          {label}
          {required && <span style={{ color: '#C62828', marginLeft: 4 }}>*</span>}
        </label>
        {rightAction}
      </div>

      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 12,
          padding: '0 16px',
          height: 50,
          border: error ? '1.5px solid #C62828' : '1.5px solid #D5D5BE',
          borderRadius: 12,
          background: disabled ? '#F3F3E8' : '#FEFCF6',
          boxShadow: '0 1px 3px rgba(39, 42, 34, 0.04)',
          transition: 'border-color 0.15s ease, box-shadow 0.15s ease',
          boxSizing: 'border-box',
        }}
      >
        <Lock
          size={18}
          color={error ? '#C62828' : '#74786C'}
          aria-hidden="true"
          style={{ flexShrink: 0 }}
        />
        <input
          id={id}
          type={showPassword ? 'text' : 'password'}
          disabled={disabled}
          value={value}
          aria-invalid={Boolean(error)}
          aria-describedby={error ? `${id}-error` : undefined}
          required={required}
          style={{
            flex: 1,
            height: '100%',
            border: 'none',
            background: 'transparent',
            fontFamily: 'var(--font-body, "Inter", sans-serif)',
            fontSize: '15px',
            color: '#272A22',
            outline: 'none',
            minWidth: 0,
            ...style,
          }}
          {...inputProps}
        />
        <button
          type="button"
          onClick={() => setShowPassword((prev) => !prev)}
          aria-label={showPassword ? 'Hide password' : 'Show password'}
          tabIndex={0}
          style={{
            background: 'none',
            border: 'none',
            padding: 4,
            cursor: 'pointer',
            color: '#74786C',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            borderRadius: 6,
            transition: 'color 0.15s',
          }}
        >
          {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
        </button>
      </div>

      {showStrength && strVal.length > 0 && (
        <div style={{ marginTop: 8 }}>
          <div style={{ display: 'flex', gap: 4, height: 4, marginBottom: 4 }}>
            {[1, 2, 3, 4].map((step) => (
              <div
                key={step}
                style={{
                  flex: 1,
                  height: '100%',
                  borderRadius: 2,
                  background:
                    step <= strengthScore ? strengthColor : '#E8E8D7',
                  transition: 'background 0.2s ease',
                }}
              />
            ))}
          </div>
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              fontSize: '11px',
              fontFamily: 'var(--font-body, "Inter", sans-serif)',
              color: '#74786C',
            }}
          >
            <span>Password strength: <strong style={{ color: strengthColor }}>{strengthLabel}</strong></span>
            <span>At least 8 characters</span>
          </div>
        </div>
      )}

      {error && (
        <p
          id={`${id}-error`}
          role="alert"
          style={{
            margin: '6px 0 0',
            fontSize: '12px',
            color: '#C62828',
            fontFamily: 'var(--font-body, "Inter", sans-serif)',
            fontWeight: 500,
          }}
        >
          {error}
        </p>
      )}
    </div>
  )
}
