package com.agriplatform.plot.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for creating a new Plot.
 *
 * <p>The {@code farmerId} is deliberately absent — it is derived from the
 * authenticated JWT {@code sub} claim by the service layer.
 *
 * @param villageId      UUID of the village where the plot physically lies (required)
 * @param label          Optional human-readable label; trimmed, case-insensitive unique per farmer
 * @param totalAreaAcres Area in acres; must be &gt; 0 if supplied; null is allowed
 */
public record CreatePlotRequestDto(

        UUID villageId,

        @jakarta.validation.constraints.Pattern(regexp = ".*\\S.*", message = "label must not be blank")
        @Size(max = 500, message = "label must not exceed 500 characters")
        String label,

        @Positive(message = "totalAreaAcres must be greater than 0")
        @Digits(integer = 10, fraction = 4, message = "totalAreaAcres must have at most 10 integer and 4 fractional digits")
        BigDecimal totalAreaAcres
) {}
