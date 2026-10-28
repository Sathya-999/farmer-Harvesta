'use client'

import React, { useState } from 'react'
import { motion } from 'framer-motion'
import AppLayout from '@/components/ui/AppLayout'
import {
  Settings,
  Globe,
  Bell,
  Shield,
  Server,
  Database,
  Info,
  Check,
  RefreshCw,
} from 'lucide-react'
import { getToken, getCurrentUser } from '@/lib/authService'

export default function SettingsPage() {
  const [language, setLanguage] = useState<'en' | 'te' | 'hi'>('en')
  const [weatherAlerts, setWeatherAlerts] = useState(true)
  const [marketAlerts, setMarketAlerts] = useState(true)
  const [schemeAlerts, setSchemeAlerts] = useState(true)
  const [cacheCleared, setCacheCleared] = useState(false)

  const token = typeof window !== 'undefined' ? getToken() : null
  const user = getCurrentUser()

  const handleClearCache = () => {
    if (typeof window !== 'undefined') {
      try {
        localStorage.removeItem('farmer_profile')
        setCacheCleared(true)
        setTimeout(() => setCacheCleared(false), 3000)
      } catch (err) {
        console.error('Failed to clear cache:', err)
      }
    }
  }

  return (
    <AppLayout>
      <div style={{ maxWidth: 760, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 24 }}>
        {/* Header */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
          <div
            style={{
              width: 48,
              height: 48,
              borderRadius: 16,
              background: 'rgba(47, 82, 51, 0.12)',
              color: 'var(--canopy-700, #2F5233)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <Settings size={24} />
          </div>
          <div>
            <h1
              style={{
                fontFamily: 'var(--font-serif)',
                fontSize: 26,
                fontWeight: 700,
                color: 'var(--canopy-900, #163A24)',
                margin: 0,
              }}
            >
              Platform Settings & Preferences
            </h1>
            <p style={{ margin: '2px 0 0', fontSize: 13, color: 'var(--text-muted, #7A6552)' }}>
              Configure regional preferences, notification subscriptions, and system connectivity.
            </p>
          </div>
        </div>

        {/* Settings Sections */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
          {/* 1. Language Preferences */}
          <div
            style={{
              borderRadius: 20,
              background: 'rgba(255, 255, 255, 0.94)',
              backdropFilter: 'blur(16px)',
              border: '1.5px solid var(--border-soft, #D9E1D8)',
              padding: 24,
              boxShadow: '0 6px 20px rgba(26, 20, 8, 0.04)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 14 }}>
              <Globe size={18} color="var(--canopy-700, #2F5233)" />
              <h3 style={{ margin: 0, fontSize: 16, fontWeight: 700, color: 'var(--canopy-900, #163A24)' }}>
                Preferred Language & Dialect
              </h3>
            </div>
            <p style={{ margin: '0 0 16px', fontSize: 13, color: 'var(--text-muted, #7A6552)' }}>
              Select the language for on-screen advisories and speech prompts.
            </p>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 12 }}>
              {[
                { code: 'en', label: 'English', subtitle: 'Standard' },
                { code: 'te', label: 'తెలుగు (Telugu)', subtitle: 'ఆంధ్రప్రదేశ్ & తెలంగాణ' },
                { code: 'hi', label: 'हिन्दी (Hindi)', subtitle: 'राष्ट्रीय भाषा' },
              ].map((item) => (
                <button
                  key={item.code}
                  type="button"
                  onClick={() => setLanguage(item.code as any)}
                  style={{
                    padding: '14px 16px',
                    borderRadius: 14,
                    border: `2px solid ${language === item.code ? 'var(--canopy-800, #224229)' : 'var(--border-soft, #D9E1D8)'}`,
                    background: language === item.code ? 'rgba(47, 82, 51, 0.06)' : '#fff',
                    textAlign: 'left',
                    cursor: 'pointer',
                    transition: 'all 0.15s',
                  }}
                >
                  <div style={{ fontWeight: 700, fontSize: 14, color: 'var(--canopy-900, #163A24)' }}>
                    {item.label}
                  </div>
                  <div style={{ fontSize: 11, color: 'var(--text-muted, #7A6552)', marginTop: 2 }}>
                    {item.subtitle}
                  </div>
                </button>
              ))}
            </div>
          </div>

          {/* 2. Advisory Notification Subscriptions */}
          <div
            style={{
              borderRadius: 20,
              background: 'rgba(255, 255, 255, 0.94)',
              backdropFilter: 'blur(16px)',
              border: '1.5px solid var(--border-soft, #D9E1D8)',
              padding: 24,
              boxShadow: '0 6px 20px rgba(26, 20, 8, 0.04)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 14 }}>
              <Bell size={18} color="var(--canopy-700, #2F5233)" />
              <h3 style={{ margin: 0, fontSize: 16, fontWeight: 700, color: 'var(--canopy-900, #163A24)' }}>
                Advisory & Broadcast Subscriptions
              </h3>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
              <label style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', cursor: 'pointer' }}>
                <div>
                  <div style={{ fontSize: 14, fontWeight: 600, color: 'var(--canopy-900, #163A24)' }}>
                    Weather & Extreme Hazard Alerts
                  </div>
                  <div style={{ fontSize: 12, color: 'var(--text-muted, #7A6552)' }}>
                    Rainfall predictions, heatwave notices, and pest infection warnings
                  </div>
                </div>
                <input
                  type="checkbox"
                  checked={weatherAlerts}
                  onChange={(e) => setWeatherAlerts(e.target.checked)}
                  style={{ width: 18, height: 18, accentColor: 'var(--canopy-800, #224229)' }}
                />
              </label>

              <label style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', cursor: 'pointer' }}>
                <div>
                  <div style={{ fontSize: 14, fontWeight: 600, color: 'var(--canopy-900, #163A24)' }}>
                    Market Mandi Price Trends
                  </div>
                  <div style={{ fontSize: 12, color: 'var(--text-muted, #7A6552)' }}>
                    Weekly minimum support price (MSP) & nearest APMC market rates
                  </div>
                </div>
                <input
                  type="checkbox"
                  checked={marketAlerts}
                  onChange={(e) => setMarketAlerts(e.target.checked)}
                  style={{ width: 18, height: 18, accentColor: 'var(--canopy-800, #224229)' }}
                />
              </label>

              <label style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', cursor: 'pointer' }}>
                <div>
                  <div style={{ fontSize: 14, fontWeight: 600, color: 'var(--canopy-900, #163A24)' }}>
                    Government Agricultural Schemes
                  </div>
                  <div style={{ fontSize: 12, color: 'var(--text-muted, #7A6552)' }}>
                    PM-KISAN, PMFBY crop insurance enrollment windows & subsidy notices
                  </div>
                </div>
                <input
                  type="checkbox"
                  checked={schemeAlerts}
                  onChange={(e) => setSchemeAlerts(e.target.checked)}
                  style={{ width: 18, height: 18, accentColor: 'var(--canopy-800, #224229)' }}
                />
              </label>
            </div>
          </div>

          {/* 3. System Architecture & Diagnostics */}
          <div
            style={{
              borderRadius: 20,
              background: 'rgba(255, 255, 255, 0.94)',
              backdropFilter: 'blur(16px)',
              border: '1.5px solid var(--border-soft, #D9E1D8)',
              padding: 24,
              boxShadow: '0 6px 20px rgba(26, 20, 8, 0.04)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 14 }}>
              <Server size={18} color="var(--canopy-700, #2F5233)" />
              <h3 style={{ margin: 0, fontSize: 16, fontWeight: 700, color: 'var(--canopy-900, #163A24)' }}>
                System Architecture & Connectivity
              </h3>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: 12, fontSize: 13 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid #F0EFEA', paddingBottom: 8 }}>
                <span style={{ color: 'var(--text-muted, #7A6552)' }}>Backend API Service:</span>
                <strong style={{ color: 'var(--canopy-900, #163A24)', fontFamily: 'monospace' }}>
                  {process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'}
                </strong>
              </div>

              <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid #F0EFEA', paddingBottom: 8 }}>
                <span style={{ color: 'var(--text-muted, #7A6552)' }}>Authentication Mechanism:</span>
                <span style={{ color: 'var(--canopy-900, #163A24)' }}>
                  Stateless OAuth2 Bearer Token (JWT Resource Server)
                </span>
              </div>

              <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid #F0EFEA', paddingBottom: 8 }}>
                <span style={{ color: 'var(--text-muted, #7A6552)' }}>Session Token:</span>
                <span style={{ color: 'var(--text-muted, #7A6552)', fontFamily: 'monospace', fontSize: 12 }}>
                  {token ? `${token.slice(0, 16)}...` : 'No active token'}
                </span>
              </div>

              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingTop: 4 }}>
                <span style={{ color: 'var(--text-muted, #7A6552)' }}>Local Storage Cache:</span>
                <button
                  type="button"
                  onClick={handleClearCache}
                  style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: 6,
                    padding: '6px 12px',
                    borderRadius: 8,
                    border: '1px solid var(--border-soft, #D9E1D8)',
                    background: '#FAF8F5',
                    fontSize: 12,
                    fontWeight: 600,
                    color: 'var(--canopy-800, #224229)',
                    cursor: 'pointer',
                  }}
                >
                  {cacheCleared ? <Check size={14} color="#15803D" /> : <RefreshCw size={14} />}
                  <span>{cacheCleared ? 'Cache Purged' : 'Clear Client Cache'}</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </AppLayout>
  )
}
