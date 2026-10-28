/**
 * notificationService.ts — Notification Service (Task 11)
 *
 * Interacts with Spring Boot NotificationController:
 * - GET   /api/v1/notifications -> Paginated all notifications
 * - GET   /api/v1/notifications/unread -> Paginated unread notifications
 * - GET   /api/v1/notifications/unread/count -> Unread count badge
 * - PATCH /api/v1/notifications/{id}/read -> Mark one as read
 * - POST  /api/v1/notifications/read-all -> Mark all as read
 */

import { api } from './apiClient'
import { PageResponse } from './plotService'

export interface NotificationDto {
  id: string
  farmerId: string
  title: string
  message: string
  category: 'CULTIVATION' | 'WEATHER' | 'MARKET' | 'SCHEME' | 'SYSTEM'
  severity: 'INFO' | 'WARNING' | 'ALERT' | 'SUCCESS'
  read: boolean
  createdAt: string
  actionUrl?: string
}

export interface UnreadCountDto {
  unreadCount: number
}

export const notificationService = {
  /**
   * Retrieves paginated notifications (newest first).
   */
  async getNotifications(page = 0, size = 20): Promise<PageResponse<NotificationDto>> {
    return await api.get<PageResponse<NotificationDto>>(
      `/api/v1/notifications?page=${page}&size=${size}`
    )
  },

  /**
   * Retrieves paginated unread notifications.
   */
  async getUnread(page = 0, size = 20): Promise<PageResponse<NotificationDto>> {
    return await api.get<PageResponse<NotificationDto>>(
      `/api/v1/notifications/unread?page=${page}&size=${size}`
    )
  },

  /**
   * Retrieves total unread notification count.
   */
  async getUnreadCount(): Promise<number> {
    const res = await api.get<UnreadCountDto>('/api/v1/notifications/unread/count')
    return res?.unreadCount ?? 0
  },

  /**
   * Marks a single notification as read (idempotent).
   */
  async markAsRead(id: string): Promise<NotificationDto> {
    return await api.patch<NotificationDto>(`/api/v1/notifications/${id}/read`)
  },

  /**
   * Marks all notifications as read.
   */
  async markAllAsRead(): Promise<void> {
    await api.post('/api/v1/notifications/read-all')
  },
}
