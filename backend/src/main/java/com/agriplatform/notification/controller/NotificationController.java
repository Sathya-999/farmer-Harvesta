package com.agriplatform.notification.controller;

import com.agriplatform.common.response.ApiResponse;
import com.agriplatform.notification.dto.NotificationDto;
import com.agriplatform.notification.dto.UnreadCountDto;
import com.agriplatform.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Task 11 ? Notification REST Controller.
 *
 * <h2>Endpoints</h2>
 * <pre>
 * GET  /api/v1/notifications              ? Paginated all notifications (newest first)
 * GET  /api/v1/notifications/unread       ? Paginated unread notifications (newest first)
 * GET  /api/v1/notifications/unread/count ? Unread notification count
 * PATCH /api/v1/notifications/{id}/read  ? Mark one notification as read (idempotent)
 * POST  /api/v1/notifications/read-all   ? Mark all unread as read
 * </pre>
 *
 * <h2>Security</h2>
 * <p>All endpoints require a valid Supabase JWT (enforced by global security config).
 * Farmer identity is always resolved from the JWT sub claim inside the service layer.
 * Farmers cannot supply or override their own farmer_id.
 *
 * <h2>IDOR</h2>
 * <p>Notification ownership is enforced at the repository layer:
 * every query and mutation includes the authenticated farmerId as a filter.
 * Accessing another farmer's notification returns 404 (not 403), preventing
 * cross-farmer enumeration.
 *
 * <h2>Pagination</h2>
 * <p>Page size is bounded at {@value #MAX_PAGE_SIZE}. Default is
 * {@value #DEFAULT_PAGE_SIZE}. Ordering is always {@code created_at DESC}.
 *
 * <h2>Farmer Write Access</h2>
 * <p>Farmers cannot create or delete notifications via this API. Creation is
 * performed internally by platform services. No DELETE endpoint is exposed.
 */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);
    private static final int MAX_PAGE_SIZE     = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Returns a paginated list of all notifications for the authenticated farmer,
     * ordered by {@code created_at DESC} (newest first).
     *
     * @param page zero-based page index (default 0)
     * @param size page size, clamped to [{@value #DEFAULT_PAGE_SIZE}..{@value #MAX_PAGE_SIZE}]
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationDto>>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        int bounded = boundSize(size);
        PageRequest pageable = PageRequest.of(page, bounded, Sort.by(Sort.Direction.DESC, "createdAt"));
        log.debug("GET /notifications page={} size={}", page, bounded);
        return ResponseEntity.ok(ApiResponse.success(notificationService.getNotifications(pageable)));
    }

    /**
     * Returns a paginated list of UNREAD notifications for the authenticated farmer.
     */
    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<Page<NotificationDto>>> getUnreadNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        int bounded = boundSize(size);
        PageRequest pageable = PageRequest.of(page, bounded, Sort.by(Sort.Direction.DESC, "createdAt"));
        log.debug("GET /notifications/unread page={} size={}", page, bounded);
        return ResponseEntity.ok(ApiResponse.success(notificationService.getUnreadNotifications(pageable)));
    }

    /**
     * Returns the total count of unread notifications for the authenticated farmer.
     */
    @GetMapping("/unread/count")
    public ResponseEntity<ApiResponse<UnreadCountDto>> getUnreadCount() {
        log.debug("GET /notifications/unread/count");
        return ResponseEntity.ok(ApiResponse.success(notificationService.getUnreadCount()));
    }

    /**
     * Marks a specific notification as read. Idempotent.
     * Returns 404 if the notification does not exist or belongs to another farmer.
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationDto>> markAsRead(@PathVariable UUID id) {
        log.debug("PATCH /notifications/{}/read", id);
        return ResponseEntity.ok(ApiResponse.success(notificationService.markAsRead(id)));
    }

    /**
     * Marks ALL unread notifications for the authenticated farmer as read.
     * Single-query DB update ? no N+1, no Java-side iteration.
     * Returns a count of how many were updated.
     */
    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllAsRead() {
        log.debug("POST /notifications/read-all");
        int updated = notificationService.markAllAsRead();
        return ResponseEntity.ok(ApiResponse.success(Map.of("markedRead", updated)));
    }

    // -------------------------------------------------------------------------

    private int boundSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }
}