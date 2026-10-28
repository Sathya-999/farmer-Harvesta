package com.agriplatform.cultivation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * API response representing an immutable historical snapshot of a cultivation intention.
 */
public record CultivationHistoryDto(
    UUID historyId,
    UUID intentionId,
    Integer version,
    OffsetDateTime snapshotAt,
    UUID plotId,
    UUID farmerId,
    UUID cropId,
    UUID cropVarietyId,
    UUID seasonId,
    Short cultivationYear,
    BigDecimal cultivatedAreaAcres,
    LocalDate plannedPlantingDate,
    LocalDate actualPlantingDate,
    BigDecimal actualAreaAcres,
    LocalDate harvestDate,
    String status,
    String statusReason,
    String source,
    UUID actorId,
    String actorType,
    String changeType,
    List<String> changedFields,
    String changeReason
) {}
