package com.agriplatform.notification.repository;

import com.agriplatform.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Task 11 ? Repository for {@link Notification} entities.
 *
 * <p>All queries are scoped by {@code farmerId} to prevent cross-farmer data leakage.
 * The partial index {@code ix_notifications_farmer_unread} on
 * {@code (farmer_id, created_at DESC) WHERE read_at IS NULL} is used by
 * {@link #findUnreadByFarmerIdOrderByCreatedAtDesc} and
 * {@link #countUnreadByFarmerId} for efficient unread retrieval.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * Returns all notifications for a farmer ordered by creation time (newest first).
     * Used for the main GET notifications feed.
     */
    Page<Notification> findByFarmerIdOrderByCreatedAtDesc(UUID farmerId, Pageable pageable);

    /**
     * Returns unread notifications for a farmer (read_at IS NULL), newest first.
     * Backed by the partial index ix_notifications_farmer_unread.
     */
    Page<Notification> findByFarmerIdAndReadAtIsNullOrderByCreatedAtDesc(UUID farmerId, Pageable pageable);

    /**
     * Returns count of unread notifications for a farmer.
     * Backed by the partial index ix_notifications_farmer_unread.
     */
    long countByFarmerIdAndReadAtIsNull(UUID farmerId);

    /**
     * Finds a single notification by ID, scoped to the farmer (IDOR protection).
     */
    Optional<Notification> findByIdAndFarmerId(UUID id, UUID farmerId);

    /**
     * Marks a single notification as read atomically.
     * Conditional on read_at IS NULL to prevent overwriting the original read timestamp.
     * Returns 1 if updated (was unread), 0 if already read (idempotent).
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.readAt = :readAt " +
           "WHERE n.id = :id AND n.farmerId = :farmerId AND n.readAt IS NULL")
    int markAsRead(@Param("id") UUID id,
                   @Param("farmerId") UUID farmerId,
                   @Param("readAt") OffsetDateTime readAt);

    /**
     * Marks ALL unread notifications for a farmer as read in a single bounded DB UPDATE.
     * Safe under concurrent requests: conditional on read_at IS NULL.
     * Returns the count of rows updated.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.readAt = :readAt " +
           "WHERE n.farmerId = :farmerId AND n.readAt IS NULL")
    int markAllAsRead(@Param("farmerId") UUID farmerId,
                      @Param("readAt") OffsetDateTime readAt);
}