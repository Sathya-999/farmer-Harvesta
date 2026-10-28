'use client'

import React from 'react'
import { LucideIcon } from 'lucide-react'

interface Props extends React.InputHTMLAttributes<HTMLInputElement> {
  id: string
  label: string
  icon?: LucideIcon
  error?: string
  helperText?: string
  required?: boolean
}

export default function AuthFormField({
  id,
  label,
  icon: Icon,
  error,
  helperText,
  required,
  disabled,
  style,
  ...inputProps
}: Props) {
  return (
    <div style={{ marginBottom: 18, width: '100%', boxSizing: 'border-box' }}>
      <label
        htmlFor={id}
        style={{
          display: 'block',
          fontFamily: 'var(--font-body, "Inter", sans-serif)',
          fontSize: '13.5px',
          fontWeight: 600,
          color: '#272A22',
          marginBottom: 7,
          lineHeight: 1.2,
        }}
      >
        {label}
        {required && <span style={{ color: '#C62828', marginLeft: 4 }}>*</span>}
      </label>

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
        {Icon && (
          <Icon
            size={18}
            color={error ? '#C62828' : '#74786C'}
            aria-hidden="true"
            style={{ flexShrink: 0 }}
          />
        )}
        <input
          id={id}
          disabled={disabled}
          aria-invalid={Boolean(error)}
          aria-describedby={error ? `${id}-error` : helperText ? `${id}-helper` : undefined}
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
      </div>

      {error ? (
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
      ) : helperText ? (
        <p
          id={`${id}-helper`}
          style={{
            margin: '6px 0 0',
            fontSize: '12px',
            color: '#74786C',
            fontFamily: 'var(--font-body, "Inter", sans-serif)',
          }}
        >
          {helperText}
        </p>
      ) : null}
    </div>
  )
}
