'use client'

import React, { useState, useEffect } from 'react'
import Link from 'next/link'
import { usePathname, useRouter } from 'next/navigation'
import { motion, AnimatePresence } from 'framer-motion'
import {
  Home,
  MapPin,
  Layers,
  History,
  BarChart3,
  Bell,
  Settings,
  User,
  LogOut,
  Menu,
  X,
  Sprout,
  PlusCircle,
} from 'lucide-react'
import { logout, getCurrentUser } from '@/lib/authService'
import { notificationService } from '@/lib/notificationService'
import HorizonScene from '@/components/ecosystem/HorizonScene'
import AuthGuard from '@/components/auth/AuthGuard'

interface NavItem {
  href: string
  icon: React.ComponentType<{ size: number; strokeWidth?: number }>
  label: string
  badge?: number
}

interface Props {
  children: React.ReactNode
}

export default function AppLayout({ children }: Props) {
  const pathname = usePathname()
  const router = useRouter()
  const [mobileOpen, setMobileOpen] = useState(false)
  const [unreadCount, setUnreadCount] = useState<number>(0)
  const currentUser = getCurrentUser()

  useEffect(() => {
    // Poll unread notification count
    notificationService
      .getUnreadCount()
      .then((c) => setUnreadCount(c))
      .catch(() => undefined)
  }, [pathname])

  const navItems: NavItem[] = [
    { href: '/dashboard', icon: Home, label: 'Dashboard' },
    { href: '/plots', icon: MapPin, label: 'My Plots' },
    { href: '/cultivations', icon: Layers, label: 'Cultivations' },
    { href: '/history', icon: History, label: 'Crop History' },
    { href: '/analytics', icon: BarChart3, label: 'Analytics' },
    { href: '/notifications', icon: Bell, label: 'Alerts', badge: unreadCount },
    { href: '/profile', icon: User, label: 'Profile' },
    { href: '/settings', icon: Settings, label: 'Settings' },
  ]

  const handleLogout = async () => {
    await logout()
    router.replace('/login')
  }

  return (
    <AuthGuard>
      <div style={{ position: 'relative', minHeight: '100svh', background: '#FEFCF6' }}>
        <HorizonScene />

        {/* ── Desktop Sidebar (Permanent Cream / Off-White Panel) ── */}
        <aside
          className="desktop-sidebar hidden md:flex"
          style={{
            position: 'fixed',
            top: 0,
            left: 0,
            bottom: 0,
            width: 250,
            zIndex: 40,
            flexDirection: 'column',
            background: 'rgba(251, 244, 228, 0.94)',
            backdropFilter: 'blur(16px)',
            borderRight: '1px solid var(--border-soft, #D9E1D8)',
            padding: '24px 16px',
            boxSizing: 'border-box',
          }}
        >
          {/* Logo Mark */}
          <Link
            href="/dashboard"
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 10,
              textDecoration: 'none',
              marginBottom: 28,
              paddingLeft: 6,
            }}
          >
            <div
              style={{
                width: 36,
                height: 36,
                borderRadius: 10,
                background: 'linear-gradient(135deg, #163A24, #2E7D32)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#86efac',
              }}
            >
              <Sprout size={20} />
            </div>
            <div>
              <p
                style={{
                  fontFamily: 'var(--font-display, "Plus Jakarta Sans", sans-serif)',
                  fontSize: 16,
                  fontWeight: 800,
                  color: '#163A24',
                  margin: 0,
                  lineHeight: 1.1,
                }}
              >
                Smart Farmer
              </p>
              <p
                style={{
                  fontFamily: 'var(--font-body, "Inter", sans-serif)',
                  fontSize: 11,
                  color: '#3D6B42',
                  margin: 0,
                  fontWeight: 600,
                }}
              >
                Intelligence Platform
              </p>
            </div>
          </Link>

          {/* Quick Action Shortcut */}
          <div style={{ marginBottom: 20 }}>
            <Link
              href="/plots/new"
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: 8,
                padding: '9px 12px',
                borderRadius: 10,
                background: 'linear-gradient(140deg, #2E7D32 0%, #1E5B2A 100%)',
                color: '#FFFFFF',
                fontSize: 13,
                fontWeight: 600,
                textDecoration: 'none',
                boxShadow: '0 2px 8px rgba(46, 125, 50, 0.25)',
              }}
            >
              <PlusCircle size={15} />
              <span>Register New Plot</span>
            </Link>
          </div>

          {/* Navigation Links */}
          <nav style={{ display: 'flex', flexDirection: 'column', gap: 4, flex: 1 }}>
            {navItems.map((item) => {
              const active = pathname === item.href || pathname?.startsWith(`${item.href}/`)
              const Icon = item.icon
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    padding: '10px 14px',
                    borderRadius: 10,
                    textDecoration: 'none',
                    fontFamily: 'var(--font-body, "Inter", sans-serif)',
                    fontSize: 13.5,
                    fontWeight: active ? 700 : 500,
                    color: active ? '#163A24' : '#6B4E3D',
                    background: active ? 'rgba(122, 155, 92, 0.18)' : 'transparent',
                    border: active ? '1px solid rgba(122, 155, 92, 0.35)' : '1px solid transparent',
                    transition: 'all 0.15s ease',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                    <Icon size={18} strokeWidth={active ? 2.4 : 1.8} />
                    <span>{item.label}</span>
                  </div>

                  {Boolean(item.badge && item.badge > 0) && (
                    <span
                      style={{
                        padding: '2px 7px',
                        borderRadius: 12,
                        background: '#D9A441',
                        color: '#1A1408',
                        fontSize: 11,
                        fontWeight: 700,
                      }}
                    >
                      {item.badge}
                    </span>
                  )}
                </Link>
              )
            })}
          </nav>

          {/* User Profile Info & Logout */}
          <div
            style={{
              borderTop: '1px solid var(--border-soft, #D9E1D8)',
              paddingTop: 16,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, minWidth: 0 }}>
              <div
                style={{
                  width: 32,
                  height: 32,
                  borderRadius: '50%',
                  background: 'var(--canopy-700, #2F5233)',
                  color: '#FFFFFF',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: 13,
                  fontWeight: 700,
                }}
              >
                {currentUser?.firstName?.[0] || 'F'}
              </div>
              <div style={{ minWidth: 0 }}>
                <p
                  style={{
                    margin: 0,
                    fontSize: 13,
                    fontWeight: 600,
                    color: '#163A24',
                    whiteSpace: 'nowrap',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                  }}
                >
                  {currentUser?.firstName || 'Farmer'} {currentUser?.lastName || ''}
                </p>
                <p style={{ margin: 0, fontSize: 11, color: '#9A7562' }}>Active Session</p>
              </div>
            </div>

            <button
              onClick={handleLogout}
              title="Logout"
              style={{
                background: 'transparent',
                border: 'none',
                cursor: 'pointer',
                color: '#9A7562',
                padding: 6,
                borderRadius: 8,
              }}
            >
              <LogOut size={16} />
            </button>
          </div>
        </aside>

        {/* ── Mobile Header & Navigation Drawer ── */}
        <header
          className="mobile-header md:hidden"
          style={{
            position: 'sticky',
            top: 0,
            left: 0,
            right: 0,
            zIndex: 40,
            background: 'rgba(251, 244, 228, 0.96)',
            backdropFilter: 'blur(12px)',
            borderBottom: '1px solid var(--border-soft, #D9E1D8)',
            padding: '12px 18px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <Sprout size={20} style={{ color: '#2E7D32' }} />
            <span style={{ fontWeight: 800, fontSize: 16, color: '#163A24' }}>Smart Farmer</span>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <Link
              href="/notifications"
              style={{ position: 'relative', color: '#6B4E3D', textDecoration: 'none' }}
            >
              <Bell size={20} />
              {unreadCount > 0 && (
                <span
                  style={{
                    position: 'absolute',
                    top: -4,
                    right: -4,
                    width: 8,
                    height: 8,
                    borderRadius: '50%',
                    background: '#D9A441',
                  }}
                />
              )}
            </Link>

            <button
              onClick={() => setMobileOpen(!mobileOpen)}
              style={{ background: 'transparent', border: 'none', cursor: 'pointer', color: '#163A24' }}
              aria-label="Toggle Navigation Menu"
            >
              {mobileOpen ? <X size={22} /> : <Menu size={22} />}
            </button>
          </div>
        </header>

        {/* Mobile Slide-down Drawer */}
        <AnimatePresence>
          {mobileOpen && (
            <motion.div
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: 'auto' }}
              exit={{ opacity: 0, height: 0 }}
              className="md:hidden"
              style={{
                background: 'rgba(251, 244, 228, 0.98)',
                borderBottom: '1.5px solid var(--border-soft, #D9E1D8)',
                padding: '16px 20px',
                zIndex: 39,
                position: 'relative',
              }}
            >
              <nav style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                {navItems.map((item) => (
                  <Link
                    key={item.href}
                    href={item.href}
                    onClick={() => setMobileOpen(false)}
                    style={{
                      padding: '10px 14px',
                      borderRadius: 10,
                      textDecoration: 'none',
                      fontSize: 14,
                      fontWeight: pathname === item.href ? 700 : 500,
                      color: pathname === item.href ? '#163A24' : '#6B4E3D',
                      background: pathname === item.href ? 'rgba(122, 155, 92, 0.2)' : 'transparent',
                    }}
                  >
                    {item.label}
                  </Link>
                ))}
                <button
                  onClick={handleLogout}
                  style={{
                    marginTop: 10,
                    padding: '10px 14px',
                    borderRadius: 10,
                    background: 'rgba(183, 28, 28, 0.08)',
                    color: '#B71C1C',
                    border: 'none',
                    textAlign: 'left',
                    fontWeight: 600,
                    fontSize: 14,
                  }}
                >
                  Sign Out
                </button>
              </nav>
            </motion.div>
          )}
        </AnimatePresence>

        {/* ── Main Workspace Body (Responsive Offset) ── */}
        <main
          className="app-main-content"
          style={{
            marginLeft: 0,
            padding: '24px 16px 80px',
            position: 'relative',
            zIndex: 5,
            boxSizing: 'border-box',
          }}
        >
          <div style={{ maxWidth: 1200, margin: '0 auto', width: '100%' }}>
            {children}
          </div>
        </main>
      </div>
    </AuthGuard>
  )
}
