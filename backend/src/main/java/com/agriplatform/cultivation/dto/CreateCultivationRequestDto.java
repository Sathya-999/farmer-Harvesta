package com.agriplatform.cultivation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request payload for creating a new cultivation intention.
 *
 * <p>Only client-editable fields are accepted.
 * Farmer identity, initial status ('PLANNED'), version (1), and source ('FARMER_APP')
 * are set by the server.
 */
public record CreateCultivationRequestDto(

    @NotNull(message = "plotId is required")
    UUID plotId,

    @NotNull(message = "cropId is required")
    UUID cropId,

    UUID cropVarietyId,

    @NotNull(message = "seasonId is required")
    UUID seasonId,

    @NotNull(message = "cultivationYear is required")
    @Min(value = 2000, message = "cultivationYear must be between 2000 and 2100")
    @Max(value = 2100, message = "cultivationYear must be between 2000 and 2100")
    Short cultivationYear,

    @NotNull(message = "cultivatedAreaAcres is required")
    @DecimalMin(value = "0.01", inclusive = true, message = "cultivatedAreaAcres must be greater than 0")
    BigDecimal cultivatedAreaAcres,

    @NotNull(message = "plannedPlantingDate is required")
    LocalDate plannedPlantingDate
) {}
