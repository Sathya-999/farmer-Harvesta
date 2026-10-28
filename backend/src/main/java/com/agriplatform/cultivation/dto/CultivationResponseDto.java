package com.agriplatform.cultivation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * API response representing a cultivation intention / cycle.
 */
public record CultivationResponseDto(
    UUID id,
    UUID plotId,
    String plotLabel,
    UUID farmerId,
    UUID cropId,
    String cropName,
    String cropCategory,
    UUID cropVarietyId,
    String cropVarietyName,
    UUID seasonId,
    String seasonName,
    Short cultivationYear,
    BigDecimal cultivatedAreaAcres,
    LocalDate plannedPlantingDate,
    LocalDate actualPlantingDate,
    BigDecimal actualAreaAcres,
    LocalDate harvestDate,
    String status,
    String statusReason,
    Integer version,
    String source,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
