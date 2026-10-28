package com.agriplatform.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Task 11 ? Internal request record for programmatic notification creation.
 *
 * <p>This DTO is used by platform services (e.g., cultivation event hooks)
 * to create notifications. It is NOT a public API body ? farmers cannot
 * directly invoke the creation endpoint. The farmer_id is always resolved
 * from the service context, never from client input.
 *
 * <p>Validation mirrors the live DB CHECK constraints:
 * <ul>
 *   <li>{@code title}: non-blank, max 255 chars</li>
 *   <li>{@code body}: non-blank, max 2000 chars</li>
 *   <li>{@code type}: optional; defaults to GENERAL if blank</li>
 * </ul>
 */
public record CreateNotificationRequest(
        @NotBlank(message = "Notification title must not be blank")
        @Size(max = 255, message = "Notification title must not exceed 255 characters")
        String title,

        @NotBlank(message = "Notification body must not be blank")
        @Size(max = 2000, message = "Notification body must not exceed 2000 characters")
        String body,

        String type
) {}