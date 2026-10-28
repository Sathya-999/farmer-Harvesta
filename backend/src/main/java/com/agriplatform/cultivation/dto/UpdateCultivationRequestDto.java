package com.agriplatform.cultivation.dto;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request payload for updating mutable fields of a cultivation intention in {@code PLANNED} status.
 *
 * <p>Lineage identity fields ({@code plotId}, {@code seasonId}, {@code cultivationYear})
 * are strictly immutable.
 */
public record UpdateCultivationRequestDto(

    UUID cropId,

    UUID cropVarietyId,

    @DecimalMin(value = "0.01", inclusive = true, message = "cultivatedAreaAcres must be greater than 0")
    BigDecimal cultivatedAreaAcres,

    LocalDate plannedPlantingDate,

    Integer expectedVersion
) {}
