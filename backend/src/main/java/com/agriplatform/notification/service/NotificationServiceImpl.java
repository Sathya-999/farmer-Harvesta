package com.agriplatform.notification.service;

import com.agriplatform.common.exception.ResourceNotFoundException;
import com.agriplatform.common.util.SecurityUtils;
import com.agriplatform.farmer.entity.FarmerProfile;
import com.agriplatform.farmer.repository.FarmerProfileRepository;
import com.agriplatform.notification.dto.CreateNotificationRequest;
import com.agriplatform.notification.dto.NotificationDto;
import com.agriplatform.notification.dto.UnreadCountDto;
import com.agriplatform.notification.entity.Notification;
import com.agriplatform.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Task 11 ? Notification Service implementation.
 *
 * <h2>Identity and IDOR</h2>
 * <p>All authenticated-farmer operations derive farmer identity exclusively from
 * the JWT sub claim via {@link SecurityUtils#getAuthenticatedUserId()}.
 * Notification IDs are always cross-referenced with farmer_id before returning
 * data. A notification belonging to another farmer returns 404 (not 403) to
 * prevent enumeration.
 *
 * <h2>Read State</h2>
 * <p>Read state is tracked by the {@code read_at} column (null = unread).
 * Mark-as-read is idempotent: conditional UPDATE WHERE read_at IS NULL prevents
 * overwriting the original read timestamp on duplicate calls.
 *
 * <h2>Farmer Status</h2>
 * <p>DELETED farmers cannot access notifications (same as History).
 * SUSPENDED farmers retain read access (read-only operations).
 * Only ACTIVE farmers may receive new notifications via createForFarmer.
 *
 * <h2>Performance</h2>
 * <p>All queries filter by farmer_id at the DB level. The partial index
 * ix_notifications_farmer_unread (farmer_id, created_at DESC) WHERE read_at IS NULL
 * is used by unread queries. Mark-all-as-read is a single bounded DB UPDATE.
 * No Java-side filtering or sorting is performed.
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;
    private final FarmerProfileRepository farmerProfileRepository;
    private final SecurityUtils securityUtils;

    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            FarmerProfileRepository farmerProfileRepository,
            SecurityUtils securityUtils) {
        this.notificationRepository = notificationRepository;
        this.farmerProfileRepository = farmerProfileRepository;
        this.securityUtils = securityUtils;
    }

    // =========================================================================
    // READ OPERATIONS (authenticated farmer)
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDto> getNotifications(Pageable pageable) {
        UUID farmerId = resolveAndValidateFarmer();
        log.debug("getNotifications: farmerId={}", farmerId);
        return notificationRepository
                .findByFarmerIdOrderByCreatedAtDesc(farmerId, pageable)
                .map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDto> getUnreadNotifications(Pageable pageable) {
        UUID farmerId = resolveAndValidateFarmer();
        log.debug("getUnreadNotifications: farmerId={}", farmerId);
        return notificationRepository
                .findByFarmerIdAndReadAtIsNullOrderByCreatedAtDesc(farmerId, pageable)
                .map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadCountDto getUnreadCount() {
        UUID farmerId = resolveAndValidateFarmer();
        long count = notificationRepository.countByFarmerIdAndReadAtIsNull(farmerId);
        log.debug("getUnreadCount: farmerId={}, count={}", farmerId, count);
        return new UnreadCountDto(count);
    }

    // =========================================================================
    // MUTATION OPERATIONS (authenticated farmer)
    // =========================================================================

    @Override
    @Transactional
    public NotificationDto markAsRead(UUID notificationId) {
        UUID farmerId = resolveAndValidateFarmer();

        // Verify ownership first (IDOR: 404 not 403)
        Notification notification = notificationRepository
                .findByIdAndFarmerId(notificationId, farmerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification not found with id: " + notificationId));

        if (!notification.isRead()) {
            int updated = notificationRepository.markAsRead(
                    notificationId, farmerId, OffsetDateTime.now());
            log.debug("markAsRead: notificationId={}, farmerId={}, rowsUpdated={}", notificationId, farmerId, updated);
            // Re-fetch to get the persisted read_at timestamp
            notification = notificationRepository
                    .findByIdAndFarmerId(notificationId, farmerId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Notification not found after mark-read: " + notificationId));
        } else {
            log.debug("markAsRead (already read): notificationId={}, farmerId={}", notificationId, farmerId);
        }

        return toDto(notification);
    }

    @Override
    @Transactional
    public int markAllAsRead() {
        UUID farmerId = resolveAndValidateFarmer();
        int updated = notificationRepository.markAllAsRead(farmerId, OffsetDateTime.now());
        log.debug("markAllAsRead: farmerId={}, rowsUpdated={}", farmerId, updated);
        return updated;
    }

    // =========================================================================
    // INTERNAL CREATION (platform services, not public API)
    // =========================================================================

    @Override
    @Transactional
    public NotificationDto createForFarmer(UUID farmerId, CreateNotificationRequest request) {
        // Only ACTIVE farmers receive new notifications
        FarmerProfile farmer = farmerProfileRepository.findByIdWithGeography(farmerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Farmer profile not found: " + farmerId));
        if (!FarmerProfile.STATUS_ACTIVE.equals(farmer.getProfileStatus())) {
            throw new ResourceNotFoundException("Farmer profile not found: " + farmerId);
        }

        Notification notification = new Notification(
                farmerId,
                request.title().strip(),
                request.body().strip(),
                request.type()
        );
        Notification saved = notificationRepository.save(notification);
        log.debug("createForFarmer: farmerId={}, notificationId={}, type={}", farmerId, saved.getId(), saved.getType());
        return toDto(saved);
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * Resolves farmer identity from JWT and validates:
     * - Farmer profile must exist
     * - Farmer must not be DELETED
     * SUSPENDED farmers retain read access.
     */
    private UUID resolveAndValidateFarmer() {
        UUID farmerId = securityUtils.getAuthenticatedUserId();
        FarmerProfile farmer = farmerProfileRepository.findByIdWithGeography(farmerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Farmer profile not found for user: " + farmerId));
        if (FarmerProfile.STATUS_DELETED.equals(farmer.getProfileStatus())) {
            throw new ResourceNotFoundException("Farmer profile not found for user: " + farmerId);
        }
        return farmerId;
    }

    private NotificationDto toDto(Notification n) {
        return new NotificationDto(
                n.getId(),
                n.getTitle(),
                n.getBody(),
                n.getType(),
                n.isRead(),
                n.getReadAt(),
                n.getCreatedAt()
        );
    }
}