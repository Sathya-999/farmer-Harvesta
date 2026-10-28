package com.agriplatform.notification.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Task 11 ? Immutable response DTO for a single notification.
 *
 * <p>Never exposes internal database fields or cross-farmer data.
 * {@code read} is a derived boolean from {@code readAt}. Both are
 * included so clients can display "read at" timestamps if needed.
 */
public record NotificationDto(
        UUID id,
        String title,
        String body,
        String type,
        boolean read,
        OffsetDateTime readAt,
        OffsetDateTime createdAt
) {}