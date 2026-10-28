'use client'

import React, { useState, useRef, useEffect } from 'react'
import Link from 'next/link'
import Image from 'next/image'
import { usePathname, useRouter } from 'next/navigation'
import {
  LayoutGrid,
  Sprout,
  Droplets,
  CloudSun,
  Bug,
  Layers,
  FileText,
  HelpCircle,
  Bell,
  Settings,
  Menu,
  X,
  LogOut,
  BarChart2,
  User,
} from 'lucide-react'
import { logout } from '@/lib/authService'
import { FarmerProfileDto } from '@/lib/farmerService'
import AutoHidingScrollbar from '@/components/ui/AutoHidingScrollbar'

interface Props {
  profile?: FarmerProfileDto | null
  unreadCount?: number
  mobileOpen?: boolean
  onToggleMobile?: () => void
}

export default function SidebarNav({
  profile,
  unreadCount = 2,
  mobileOpen = false,
  onToggleMobile,
}: Props) {
  const pathname = usePathname()
  const router = useRouter()
  const navRef = useRef<HTMLElement>(null)

  // ── Hover-Expansion & Focus Management ──
  const [isHovered, setIsHovered] = useState(false)
  const [isFocused, setIsFocused] = useState(false)
  const [hoveredTooltip, setHoveredTooltip] = useState<string | null>(null)
  const collapseTimerRef = useRef<NodeJS.Timeout | null>(null)

  const isExpanded = isHovered || isFocused

  const handleMouseEnter = () => {
    if (collapseTimerRef.current) {
      clearTimeout(collapseTimerRef.current)
      collapseTimerRef.current = null
    }
    setIsHovered(true)
  }

  const handleMouseLeave = () => {
    if (collapseTimerRef.current) {
      clearTimeout(collapseTimerRef.current)
    }
    // 200ms debounce prevents accidental collapse while moving pointer
    collapseTimerRef.current = setTimeout(() => {
      setIsHovered(false)
      setHoveredTooltip(null)
    }, 200)
  }

  const handleFocusCapture = () => {
    if (collapseTimerRef.current) {
      clearTimeout(collapseTimerRef.current)
      collapseTimerRef.current = null
    }
    setIsFocused(true)
  }

  const handleBlurCapture = (e: React.FocusEvent) => {
    // If focus moves completely outside the sidebar container, collapse smoothly
    if (!e.currentTarget.contains(e.relatedTarget as Node)) {
      if (collapseTimerRef.current) clearTimeout(collapseTimerRef.current)
      collapseTimerRef.current = setTimeout(() => {
        setIsFocused(false)
        setHoveredTooltip(null)
      }, 200)
    }
  }

  // Keyboard Escape listener
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setIsHovered(false)
        setIsFocused(false)
        setHoveredTooltip(null)
        if (mobileOpen && onToggleMobile) onToggleMobile()
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => {
      window.removeEventListener('keydown', handleKeyDown)
      if (collapseTimerRef.current) clearTimeout(collapseTimerRef.current)
    }
  }, [mobileOpen, onToggleMobile])

  const navItems = [
    { label: 'Dashboard', icon: LayoutGrid, href: '/dashboard' },
    { label: 'Fields', icon: Sprout, href: '/plots' },
    { label: 'Cultivations', icon: Layers, href: '/cultivations' },
    { label: 'History', icon: FileText, href: '/history' },
    { label: 'Analytics', icon: BarChart2, href: '/analytics' },
    { label: 'Alerts', icon: Bell, href: '/notifications' },
    { label: 'Profile', icon: User, href: '/profile' },
    { label: 'Settings', icon: Settings, href: '/settings' },
  ]

  const handleLogout = async () => {
    await logout()
    router.replace('/login')
  }

  const farmerName = profile?.fullName || 'Farmer'

  return (
    <>
      {/* ── Mobile Hamburger Header (Visible ONLY on <= 768px screens) ── */}
      <div
        className="mobile-top-header"
        style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          height: 56,
          background: 'rgba(237, 235, 227, 0.98)',
          backdropFilter: 'blur(12px)',
          borderBottom: '1px solid #D5D3C7',
          zIndex: 60,
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '0 16px',
          boxSizing: 'border-box',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          {/* Harvesta Concentric Arches Logo */}
          <svg width="24" height="24" viewBox="0 0 28 28" fill="none">
            <path d="M 4 22 A 10 10 0 0 1 24 22" stroke="#232B20" strokeWidth="2.6" strokeLinecap="round" />
            <path d="M 8 22 A 6 6 0 0 1 20 22" stroke="#232B20" strokeWidth="2.6" strokeLinecap="round" />
            <path d="M 12 22 A 2 2 0 0 1 16 22" stroke="#232B20" strokeWidth="2.6" strokeLinecap="round" />
          </svg>
          <span style={{ fontSize: 18, fontWeight: 700, color: '#1F2718', letterSpacing: '-0.01em' }}>
            Harvesta
          </span>
        </div>

        <button
          onClick={onToggleMobile}
          style={{
            background: 'none',
            border: 'none',
            color: '#1F2718',
            padding: 8,
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
          aria-label="Toggle Navigation Menu"
        >
          {mobileOpen ? <X size={24} /> : <Menu size={24} />}
        </button>
      </div>

      {/* ── Mobile Drawer Backdrop ── */}
      {mobileOpen && (
        <div
          onClick={onToggleMobile}
          className="mobile-drawer-backdrop"
          style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(22, 58, 36, 0.5)',
            backdropFilter: 'blur(4px)',
            zIndex: 75,
          }}
        />
      )}

      {/* ── Desktop Dynamic Hover-Expanding Sidebar & Mobile Drawer ── */}
      <aside
        className={`desktop-sidebar ${mobileOpen ? 'mobile-open' : ''} ${isExpanded ? 'sidebar-expanded' : 'sidebar-collapsed'}`}
        onMouseEnter={handleMouseEnter}
        onMouseLeave={handleMouseLeave}
        onFocusCapture={handleFocusCapture}
        onBlurCapture={handleBlurCapture}
        aria-label="Main Navigation"
        style={{
          position: 'fixed',
          top: 0,
          left: 0,
          bottom: 0,
          width: isExpanded ? 240 : 68,
          background: '#EDEBE2',
          borderRight: '1px solid #DEDBCF',
          zIndex: 80,
          display: 'flex',
          flexDirection: 'column',
          padding: isExpanded ? '24px 16px 18px' : '24px 10px 18px',
          boxSizing: 'border-box',
          boxShadow: isExpanded ? '6px 0 28px rgba(0, 0, 0, 0.18)' : '2px 0 10px rgba(0, 0, 0, 0.04)',
          transition: 'width 0.26s cubic-bezier(0.2, 0.8, 0.2, 1), box-shadow 0.26s ease, padding 0.26s ease',
          overflow: 'visible', // allows tooltips to pop out to the right
        }}
      >
        {/* ── Brand Logo & Wordmark ── */}
        <Link
          href="/dashboard"
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 12,
            textDecoration: 'none',
            marginBottom: 28,
            paddingLeft: isExpanded ? 6 : 8,
            height: 32,
            position: 'relative',
          }}
          onMouseEnter={() => !isExpanded && setHoveredTooltip('Harvesta')}
          onMouseLeave={() => setHoveredTooltip(null)}
        >
          {/* Concentric Rainbow Arches Icon */}
          <div
            style={{
              width: 28,
              height: 28,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexShrink: 0,
            }}
          >
            <svg width="26" height="26" viewBox="0 0 28 28" fill="none">
              <path d="M 4 22 A 10 10 0 0 1 24 22" stroke="#222B1E" strokeWidth="2.6" strokeLinecap="round" />
              <path d="M 8 22 A 6 6 0 0 1 20 22" stroke="#222B1E" strokeWidth="2.6" strokeLinecap="round" />
              <path d="M 12 22 A 2 2 0 0 1 16 22" stroke="#222B1E" strokeWidth="2.6" strokeLinecap="round" />
            </svg>
          </div>

          {/* Wordmark (Fades in/out smoothly) */}
          <span
            style={{
              fontSize: 20,
              fontWeight: 700,
              color: '#1F2718',
              letterSpacing: '-0.02em',
              whiteSpace: 'nowrap',
              opacity: isExpanded ? 1 : 0,
              transform: isExpanded ? 'translateX(0)' : 'translateX(-8px)',
              transition: 'opacity 0.2s ease 0.04s, transform 0.2s ease 0.04s',
              pointerEvents: isExpanded ? 'auto' : 'none',
            }}
          >
            Harvesta
          </span>

          {/* Tooltip on Collapsed Brand */}
          {!isExpanded && hoveredTooltip === 'Harvesta' && (
            <div className="sidebar-tooltip">Harvesta OS</div>
          )}
        </Link>

        {/* ── Navigation Item List ── */}
        <nav
          ref={navRef}
          className="no-native-scrollbar"
          style={{
            position: 'relative',
            display: 'flex',
            flexDirection: 'column',
            gap: 6,
            flex: 1,
            overflowY: 'auto',
            overflowX: 'visible',
          }}
        >
          {navItems.map((item) => {
            // "Fields" is the active item in the reference image
            const isFields = item.label === 'Fields'
            const Icon = item.icon
            const isItemHovered = hoveredTooltip === item.label

            return (
              <div
                key={item.label}
                style={{ position: 'relative', display: 'flex', width: '100%' }}
                onMouseEnter={() => !isExpanded && setHoveredTooltip(item.label)}
                onMouseLeave={() => setHoveredTooltip(null)}
              >
                <Link
                  href={item.href}
                  onClick={() => onToggleMobile && onToggleMobile()}
                  aria-label={item.label}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 12,
                    width: '100%',
                    height: 40,
                    padding: isExpanded ? '8px 14px' : '8px 0',
                    justifyContent: isExpanded ? 'flex-start' : 'center',
                    borderRadius: isFields ? 999 : 12,
                    textDecoration: 'none',
                    fontSize: 13.5,
                    fontWeight: isFields ? 600 : 500,
                    color: isFields ? '#FFFFFF' : '#6A6F62',
                    background: isFields ? '#58644A' : isItemHovered ? 'rgba(88, 100, 74, 0.08)' : 'transparent',
                    transition: 'background 0.15s ease, color 0.15s ease, padding 0.2s ease',
                    boxSizing: 'border-box',
                    outline: 'none',
                  }}
                  className="sidebar-nav-link"
                >
                  {/* Icon */}
                  <div
                    style={{
                      width: 24,
                      height: 24,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      flexShrink: 0,
                    }}
                  >
                    <Icon
                      size={18}
                      strokeWidth={isFields ? 2.3 : 1.8}
                      color={isFields ? '#FFFFFF' : '#707567'}
                    />
                  </div>

                  {/* Text Label (Smoothly hidden when collapsed) */}
                  <span
                    style={{
                      whiteSpace: 'nowrap',
                      opacity: isExpanded ? 1 : 0,
                      transform: isExpanded ? 'translateX(0)' : 'translateX(-8px)',
                      transition: 'opacity 0.2s ease 0.04s, transform 0.2s ease 0.04s',
                      display: isExpanded ? 'inline' : 'none',
                    }}
                  >
                    {item.label}
                  </span>
                </Link>

                {/* Floating Tooltip (Visible ONLY on icon hover while collapsed) */}
                {!isExpanded && isItemHovered && (
                  <div className="sidebar-tooltip">
                    {item.label}
                    {isFields ? ' (Active)' : ''}
                  </div>
                )}
              </div>
            )
          })}

          <AutoHidingScrollbar
            containerRef={navRef}
            width={4}
            rightOffset={1}
            thumbColor="rgba(88, 100, 74, 0.4)"
            thumbActiveColor="#58644A"
          />
        </nav>

        {/* ── Bottom Section: Notification, Setting, and User Profile ── */}
        <div
          style={{
            display: 'flex',
            flexDirection: 'column',
            gap: 8,
            paddingTop: 14,
            borderTop: '1px solid #DEDBCF',
          }}
        >
          {/* 1. Notification Item */}
          <div
            style={{ position: 'relative', width: '100%' }}
            onMouseEnter={() => !isExpanded && setHoveredTooltip('Notification')}
            onMouseLeave={() => setHoveredTooltip(null)}
          >
            <Link
              href="/notifications"
              aria-label={`Notifications, ${unreadCount} unread`}
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: isExpanded ? 'space-between' : 'center',
                padding: isExpanded ? '7px 12px' : '7px 0',
                width: '100%',
                height: 38,
                borderRadius: 12,
                textDecoration: 'none',
                fontSize: 13.5,
                fontWeight: 500,
                color: '#6A6F62',
                background: hoveredTooltip === 'Notification' ? 'rgba(88, 100, 74, 0.08)' : 'transparent',
                transition: 'background 0.15s ease',
                boxSizing: 'border-box',
                outline: 'none',
              }}
              className="sidebar-nav-link"
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: 12, position: 'relative' }}>
                <Bell size={18} strokeWidth={1.8} color="#707567" />
                {/* Collapsed Badge (Small yellow indicator) */}
                {!isExpanded && (
                  <span
                    style={{
                      position: 'absolute',
                      top: -4,
                      right: -6,
                      width: 14,
                      height: 14,
                      borderRadius: '50%',
                      background: '#D4FD33',
                      color: '#1A2215',
                      fontSize: 9,
                      fontWeight: 800,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                    }}
                  >
                    {unreadCount}
                  </span>
                )}
                <span
                  style={{
                    display: isExpanded ? 'inline' : 'none',
                    opacity: isExpanded ? 1 : 0,
                    transition: 'opacity 0.2s ease',
                  }}
                >
                  Notification
                </span>
              </div>

              {/* Expanded Pill Badge */}
              {isExpanded && (
                <span
                  style={{
                    fontSize: 11,
                    fontWeight: 800,
                    padding: '2px 7px',
                    borderRadius: 999,
                    background: '#D4FD33',
                    color: '#1A2215',
                  }}
                >
                  {unreadCount}
                </span>
              )}
            </Link>

            {!isExpanded && hoveredTooltip === 'Notification' && (
              <div className="sidebar-tooltip">Notifications ({unreadCount})</div>
            )}
          </div>

          {/* 2. Setting Item */}
          <div
            style={{ position: 'relative', width: '100%' }}
            onMouseEnter={() => !isExpanded && setHoveredTooltip('Setting')}
            onMouseLeave={() => setHoveredTooltip(null)}
          >
            <Link
              href="/settings"
              aria-label="Platform Settings"
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 12,
                justifyContent: isExpanded ? 'flex-start' : 'center',
                padding: isExpanded ? '7px 12px' : '7px 0',
                width: '100%',
                height: 38,
                borderRadius: 12,
                textDecoration: 'none',
                fontSize: 13.5,
                fontWeight: 500,
                color: '#6A6F62',
                background: hoveredTooltip === 'Setting' ? 'rgba(88, 100, 74, 0.08)' : 'transparent',
                transition: 'background 0.15s ease',
                boxSizing: 'border-box',
                outline: 'none',
              }}
              className="sidebar-nav-link"
            >
              <Settings size={18} strokeWidth={1.8} color="#707567" />
              <span
                style={{
                  display: isExpanded ? 'inline' : 'none',
                  opacity: isExpanded ? 1 : 0,
                  transition: 'opacity 0.2s ease',
                  whiteSpace: 'nowrap',
                }}
              >
                Setting
              </span>
            </Link>

            {!isExpanded && hoveredTooltip === 'Setting' && (
              <div className="sidebar-tooltip">Setting</div>
            )}
          </div>

          {/* 3. Alex Jackson Profile Item */}
          <div
            style={{ position: 'relative', width: '100%' }}
            onMouseEnter={() => !isExpanded && setHoveredTooltip('Profile')}
            onMouseLeave={() => setHoveredTooltip(null)}
          >
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: isExpanded ? 'space-between' : 'center',
                padding: isExpanded ? '8px 6px' : '8px 0',
                borderRadius: 12,
                width: '100%',
                boxSizing: 'border-box',
              }}
            >
              <Link
                href="/profile"
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 10,
                  textDecoration: 'none',
                  minWidth: 0,
                  outline: 'none',
                }}
              >
                <div
                  style={{
                    width: 32,
                    height: 32,
                    borderRadius: '50%',
                    overflow: 'hidden',
                    position: 'relative',
                    flexShrink: 0,
                    border: '1.5px solid #D5D3C7',
                  }}
                >
                  <Image
                    src="/alex_jackson.jpg"
                    alt={farmerName}
                    fill
                    style={{ objectFit: 'cover' }}
                  />
                </div>

                <div
                  style={{
                    display: isExpanded ? 'flex' : 'none',
                    flexDirection: 'column',
                    minWidth: 0,
                    opacity: isExpanded ? 1 : 0,
                    transition: 'opacity 0.2s ease',
                  }}
                >
                  <span
                    style={{
                      fontSize: 13,
                      fontWeight: 600,
                      color: '#22281D',
                      whiteSpace: 'nowrap',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                    }}
                  >
                    {farmerName}
                  </span>
                  <span style={{ fontSize: 10.5, color: '#888D80' }}>Field Owner</span>
                </div>
              </Link>

              {isExpanded && (
                <button
                  onClick={handleLogout}
                  title="Sign Out"
                  style={{
                    background: 'none',
                    border: 'none',
                    padding: 4,
                    cursor: 'pointer',
                    color: '#8A8E82',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    outline: 'none',
                  }}
                >
                  <LogOut size={14} />
                </button>
              )}
            </div>

            {!isExpanded && hoveredTooltip === 'Profile' && (
              <div className="sidebar-tooltip">{farmerName} (Field Owner)</div>
            )}
          </div>
        </div>
      </aside>

      {/* ── Strict Responsive CSS Rules & Tooltip Styles ── */}
      <style jsx global>{`
        /* Floating Tooltip */
        .sidebar-tooltip {
          position: absolute;
          left: 64px;
          top: 50%;
          transform: translateY(-50%);
          background: #20271B;
          color: #FFFFFF;
          font-size: 11.5px;
          font-weight: 600;
          padding: 5px 12px;
          border-radius: 8px;
          white-space: nowrap;
          pointer-events: none;
          box-shadow: 0 4px 14px rgba(0, 0, 0, 0.22);
          z-index: 100;
          animation: sidebarTooltipFadeIn 0.15s ease-out;
        }

        .sidebar-tooltip::before {
          content: '';
          position: absolute;
          left: -4px;
          top: 50%;
          transform: translateY(-50%);
          width: 0;
          height: 0;
          border-top: 4px solid transparent;
          border-bottom: 4px solid transparent;
          border-right: 4px solid #20271B;
        }

        @keyframes sidebarTooltipFadeIn {
          from {
            opacity: 0;
            transform: translateY(-50%) translateX(-4px);
          }
          to {
            opacity: 1;
            transform: translateY(-50%) translateX(0);
          }
        }

        /* Focus indicators for accessibility */
        .sidebar-nav-link:focus-visible {
          outline: 2px solid #58644A !important;
          outline-offset: 2px;
        }

        /* Desktop screens (> 768px) */
        @media (min-width: 769px) {
          .mobile-top-header {
            display: none !important;
          }
          .mobile-drawer-backdrop {
            display: none !important;
          }
          .desktop-sidebar {
            display: flex !important;
            transform: none !important;
            left: 0 !important;
          }
        }

        /* Mobile screens (<= 768px) */
        @media (max-width: 768px) {
          .mobile-top-header {
            display: flex !important;
          }
          .desktop-sidebar {
            display: flex !important;
            width: 250px !important;
            transform: translateX(-100%);
            padding: 24px 16px 18px !important;
          }
          .desktop-sidebar.mobile-open {
            transform: translateX(0) !important;
            box-shadow: 0 10px 40px rgba(0, 0, 0, 0.35);
          }
        }

        /* Respect prefers-reduced-motion */
        @media (prefers-reduced-motion: reduce) {
          .desktop-sidebar,
          .desktop-sidebar * {
            transition: none !important;
            animation: none !important;
          }
        }
      `}</style>
    </>
  )
}
