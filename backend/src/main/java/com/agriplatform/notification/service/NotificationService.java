package com.agriplatform.notification.service;

import com.agriplatform.notification.dto.CreateNotificationRequest;
import com.agriplatform.notification.dto.NotificationDto;
import com.agriplatform.notification.dto.UnreadCountDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Task 11 ? Notification service contract.
 *
 * <p>All read/mutation operations that use the authenticated farmer identity
 * resolve it internally from the JWT (via SecurityUtils). Callers do not
 * supply farmer_id; it is always derived from the security context.
 *
 * <p>Internal creation (createForFarmer) accepts an explicit farmerId so that
 * platform services and cultivation event hooks can create notifications on
 * behalf of a specific farmer without needing to be the authenticated principal.
 */
public interface NotificationService {

    /** Returns a paginated list of all notifications for the authenticated farmer. */
    Page<NotificationDto> getNotifications(Pageable pageable);

    /** Returns a paginated list of unread notifications for the authenticated farmer. */
    Page<NotificationDto> getUnreadNotifications(Pageable pageable);

    /** Returns the unread notification count for the authenticated farmer. */
    UnreadCountDto getUnreadCount();

    /**
     * Marks a specific notification as read.
     * Idempotent: marking an already-read notification is a no-op.
     * Returns 404 if the notification does not exist or belongs to another farmer.
     */
    NotificationDto markAsRead(UUID notificationId);

    /**
     * Marks all unread notifications for the authenticated farmer as read.
     * Efficient single-query DB UPDATE ? no Java-side iteration.
     * Returns the count of notifications that were actually updated.
     */
    int markAllAsRead();

    /**
     * Internal method: creates a notification for a specific farmer.
     * Called by platform services, not by the REST controller directly.
     * farmerId must be valid and non-deleted.
     */
    NotificationDto createForFarmer(UUID farmerId, CreateNotificationRequest request);
}