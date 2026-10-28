'use client'

import React, { useState, useEffect } from 'react'
import Link from 'next/link'
import { motion } from 'framer-motion'
import { notificationService, NotificationDto } from '@/lib/notificationService'
import AppLayout from '@/components/ui/AppLayout'
import {
  Bell,
  CheckCircle,
  AlertTriangle,
  Info,
  ShieldAlert,
  Check,
  CheckCheck,
  ArrowLeft,
  Calendar,
  CloudSun,
  TrendingUp,
  Landmark,
  Radio,
} from 'lucide-react'

export default function NotificationsPage() {
  const [notifications, setNotifications] = useState<NotificationDto[]>([])
  const [unreadCount, setUnreadCount] = useState<number>(0)
  const [filter, setFilter] = useState<'ALL' | 'UNREAD'>('ALL')
  const [loading, setLoading] = useState(true)
  const [actionLoading, setActionLoading] = useState(false)
  const [errorMsg, setErrorMsg] = useState<string | null>(null)

  const loadData = async () => {
    setLoading(true)
    setErrorMsg(null)
    try {
      const [notifsRes, countRes] = await Promise.all([
        filter === 'UNREAD' ? notificationService.getUnread(0, 50) : notificationService.getNotifications(0, 50),
        notificationService.getUnreadCount(),
      ])

      setNotifications(notifsRes.content || [])
      setUnreadCount(countRes || 0)
    } catch (err: any) {
      console.error('Failed to load notifications:', err)
      setErrorMsg('Failed to fetch notifications. Please check your network.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [filter])

  const handleMarkOne = async (id: string) => {
    try {
      await notificationService.markAsRead(id)
      setNotifications((prev) =>
        prev.map((n) => (n.id === id ? { ...n, read: true } : n))
      )
      setUnreadCount((c) => Math.max(0, c - 1))
    } catch (err) {
      console.error('Failed to mark notification as read:', err)
    }
  }

  const handleMarkAll = async () => {
    setActionLoading(true)
    try {
      await notificationService.markAllAsRead()
      setNotifications((prev) => prev.map((n) => ({ ...n, read: true })))
      setUnreadCount(0)
    } catch (err) {
      console.error('Failed to mark all as read:', err)
    } finally {
      setActionLoading(false)
    }
  }

  const getCategoryIcon = (category: string) => {
    switch (category) {
      case 'WEATHER':
        return <CloudSun size={18} />
      case 'MARKET':
        return <TrendingUp size={18} />
      case 'SCHEME':
        return <Landmark size={18} />
      case 'CULTIVATION':
        return <CheckCircle size={18} />
      default:
        return <Radio size={18} />
    }
  }

  const getSeverityStyle = (severity: string) => {
    switch (severity) {
      case 'ALERT':
        return {
          bg: 'rgba(220, 38, 38, 0.08)',
          border: 'rgba(220, 38, 38, 0.3)',
          badgeBg: '#DC2626',
          badgeText: '#fff',
        }
      case 'WARNING':
        return {
          bg: 'rgba(242, 200, 75, 0.12)',
          border: 'rgba(242, 200, 75, 0.35)',
          badgeBg: 'var(--sun-600, #D99B16)',
          badgeText: '#fff',
        }
      case 'SUCCESS':
        return {
          bg: 'rgba(34, 197, 94, 0.08)',
          border: 'rgba(34, 197, 94, 0.3)',
          badgeBg: '#15803D',
          badgeText: '#fff',
        }
      default:
        return {
          bg: 'rgba(47, 82, 51, 0.04)',
          border: 'rgba(47, 82, 51, 0.15)',
          badgeBg: 'var(--canopy-700, #2F5233)',
          badgeText: '#fff',
        }
    }
  }

  return (
    <AppLayout>
      <div style={{ maxWidth: 880, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 24 }}>
        {/* Header */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 16 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <div
              style={{
                width: 44,
                height: 44,
                borderRadius: 14,
                background: 'rgba(47, 82, 51, 0.12)',
                color: 'var(--canopy-700, #2F5233)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <Bell size={22} />
            </div>
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <h1
                  style={{
                    fontFamily: 'var(--font-serif)',
                    fontSize: 26,
                    fontWeight: 700,
                    color: 'var(--canopy-900, #163A24)',
                    margin: 0,
                  }}
                >
                  Advisories & Notifications
                </h1>
                {unreadCount > 0 && (
                  <span
                    style={{
                      padding: '2px 8px',
                      borderRadius: 999,
                      background: 'var(--sun-500, #F2C84B)',
                      color: 'var(--canopy-900, #163A24)',
                      fontSize: 12,
                      fontWeight: 700,
                    }}
                  >
                    {unreadCount} unread
                  </span>
                )}
              </div>
              <p style={{ margin: '2px 0 0', fontSize: 13, color: 'var(--text-muted, #7A6552)' }}>
                Government scheme updates, regional weather alerts, and crop advisories.
              </p>
            </div>
          </div>

          {/* Mark all as read button */}
          {unreadCount > 0 && (
            <button
              onClick={handleMarkAll}
              disabled={actionLoading}
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: 6,
                padding: '9px 16px',
                borderRadius: 10,
                border: '1.5px solid var(--border-soft, #D9E1D8)',
                background: '#fff',
                color: 'var(--canopy-900, #163A24)',
                fontSize: 13,
                fontWeight: 600,
                cursor: actionLoading ? 'not-allowed' : 'pointer',
              }}
            >
              <CheckCheck size={16} />
              <span>{actionLoading ? 'Marking...' : 'Mark All as Read'}</span>
            </button>
          )}
        </div>

        {/* Filter Tabs */}
        <div style={{ display: 'flex', gap: 10 }}>
          <button
            onClick={() => setFilter('ALL')}
            style={{
              padding: '8px 18px',
              borderRadius: 10,
              border: 'none',
              background: filter === 'ALL' ? 'var(--canopy-800, #224229)' : 'rgba(255, 255, 255, 0.8)',
              color: filter === 'ALL' ? '#fff' : 'var(--text-muted, #7A6552)',
              fontSize: 13,
              fontWeight: 600,
              cursor: 'pointer',
            }}
          >
            All Notices
          </button>
          <button
            onClick={() => setFilter('UNREAD')}
            style={{
              padding: '8px 18px',
              borderRadius: 10,
              border: 'none',
              background: filter === 'UNREAD' ? 'var(--canopy-800, #224229)' : 'rgba(255, 255, 255, 0.8)',
              color: filter === 'UNREAD' ? '#fff' : 'var(--text-muted, #7A6552)',
              fontSize: 13,
              fontWeight: 600,
              cursor: 'pointer',
            }}
          >
            Unread Only ({unreadCount})
          </button>
        </div>

        {/* Notification List */}
        {loading ? (
          <div style={{ textAlign: 'center', padding: '60px 0', color: 'var(--text-muted, #7A6552)' }}>
            Retrieving notification broadcast...
          </div>
        ) : notifications.length === 0 ? (
          <div
            style={{
              borderRadius: 20,
              background: 'rgba(255, 255, 255, 0.94)',
              border: '1.5px solid var(--border-soft, #D9E1D8)',
              padding: '60px 24px',
              textAlign: 'center',
            }}
          >
            <Bell size={40} color="var(--text-muted, #9A7562)" style={{ margin: '0 auto 12px' }} />
            <h3 style={{ margin: '0 0 6px', color: 'var(--canopy-900, #163A24)' }}>No Notifications</h3>
            <p style={{ margin: 0, fontSize: 13, color: 'var(--text-muted, #7A6552)' }}>
              {filter === 'UNREAD' ? 'You have reviewed all active notifications.' : 'Your notification feed is empty.'}
            </p>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            {notifications.map((item) => {
              const style = getSeverityStyle(item.severity)
              return (
                <motion.div
                  key={item.id}
                  initial={{ opacity: 0, y: 8 }}
                  animate={{ opacity: 1, y: 0 }}
                  style={{
                    borderRadius: 16,
                    background: item.read ? 'rgba(255, 255, 255, 0.85)' : '#fff',
                    border: `1.5px solid ${item.read ? 'var(--border-soft, #D9E1D8)' : style.border}`,
                    padding: '18px 22px',
                    boxShadow: item.read ? 'none' : '0 4px 16px rgba(26, 20, 8, 0.05)',
                    display: 'flex',
                    alignItems: 'flex-start',
                    justifyContent: 'space-between',
                    gap: 16,
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'flex-start', gap: 14 }}>
                    <div
                      style={{
                        width: 38,
                        height: 38,
                        borderRadius: 10,
                        background: style.bg,
                        color: 'var(--canopy-800, #224229)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        flexShrink: 0,
                        marginTop: 2,
                      }}
                    >
                      {getCategoryIcon(item.category)}
                    </div>
                    <div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
                        <span style={{ fontWeight: 700, fontSize: 15, color: 'var(--canopy-900, #163A24)' }}>
                          {item.title}
                        </span>
                        <span
                          style={{
                            fontSize: 10,
                            fontWeight: 700,
                            padding: '2px 6px',
                            borderRadius: 4,
                            background: style.badgeBg,
                            color: style.badgeText,
                          }}
                        >
                          {item.severity}
                        </span>
                        <span
                          style={{
                            fontSize: 11,
                            fontWeight: 600,
                            padding: '2px 6px',
                            borderRadius: 4,
                            background: 'rgba(47, 82, 51, 0.08)',
                            color: 'var(--canopy-800, #224229)',
                          }}
                        >
                          {item.category}
                        </span>
                      </div>

                      <p
                        style={{
                          margin: '6px 0 0',
                          fontSize: 13,
                          color: 'var(--text-primary, #1A1408)',
                          lineHeight: 1.5,
                        }}
                      >
                        {item.message}
                      </p>

                      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginTop: 8, fontSize: 11, color: 'var(--text-muted, #7A6552)' }}>
                        <span>{new Date(item.createdAt).toLocaleString()}</span>
                        {item.actionUrl && (
                          <Link
                            href={item.actionUrl}
                            style={{
                              fontWeight: 600,
                              color: 'var(--canopy-700, #2F5233)',
                              textDecoration: 'underline',
                            }}
                          >
                            Open Link →
                          </Link>
                        )}
                      </div>
                    </div>
                  </div>

                  {!item.read && (
                    <button
                      onClick={() => handleMarkOne(item.id)}
                      title="Mark as read"
                      style={{
                        padding: '6px 10px',
                        borderRadius: 8,
                        border: '1px solid var(--border-soft, #D9E1D8)',
                        background: '#FAF8F5',
                        color: 'var(--canopy-800, #224229)',
                        fontSize: 12,
                        fontWeight: 600,
                        cursor: 'pointer',
                        display: 'flex',
                        alignItems: 'center',
                        gap: 4,
                        whiteSpace: 'nowrap',
                      }}
                    >
                      <Check size={14} />
                      <span>Mark Read</span>
                    </button>
                  )}
                </motion.div>
              )
            })}
          </div>
        )}
      </div>
    </AppLayout>
  )
}
