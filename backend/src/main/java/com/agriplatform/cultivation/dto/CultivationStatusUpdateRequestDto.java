package com.agriplatform.cultivation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request payload for lifecycle state transitions on a cultivation cycle.
 *
 * <h2>Allowed Transitions</h2>
 * <ul>
 *   <li>{@code PLANNED} → {@code STARTED} (requires {@code actualPlantingDate}, optional {@code actualAreaAcres})</li>
 *   <li>{@code PLANNED} → {@code CANCELLED} (optional {@code statusReason})</li>
 *   <li>{@code STARTED} → {@code COMPLETED} (requires {@code harvestDate} and {@code actualPlantingDate})</li>
 *   <li>{@code STARTED} → {@code ABANDONED} (optional {@code statusReason})</li>
 * </ul>
 */
public record CultivationStatusUpdateRequestDto(

    @NotBlank(message = "targetStatus is required")
    String targetStatus,

    String statusReason,

    LocalDate actualPlantingDate,

    @DecimalMin(value = "0.01", inclusive = true, message = "actualAreaAcres must be greater than 0")
    BigDecimal actualAreaAcres,

    LocalDate harvestDate,

    Integer expectedVersion
) {}
