package com.agriplatform.cultivation.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * API response representing a cultivation status change event.
 */
public record CultivationStatusEventDto(
    UUID id,
    UUID intentionId,
    String fromStatus,
    String toStatus,
    UUID changedBy,
    String actorType,
    OffsetDateTime changedAt,
    String note
) {}
