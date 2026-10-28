package com.agriplatform.plot.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request DTO for updating an existing Plot via {@code PUT /api/v1/plots/{id}}.
 *
 * <p>Mutable fields: {@code label} and {@code totalAreaAcres}.
 * Immutable fields ({@code village_id}, {@code farmer_id}) cannot be supplied or changed.
 *
 * <p>{@code expectedVersion} must be the opaque ISO-8601 microsecond string
 * returned by the server in {@code PlotResponseDto.version}. Clients must echo
 * it verbatim without modification.
 *
 * @param label           New label (null = leave unchanged; empty string not accepted)
 * @param totalAreaAcres  New area in acres (null = leave unchanged; > 0 required if supplied)
 * @param expectedVersion Opaque version token from prior GET/POST response (required)
 */
public record UpdatePlotRequestDto(

        @jakarta.validation.constraints.Pattern(regexp = ".*\\S.*", message = "label must not be blank")
        @Size(max = 500, message = "label must not exceed 500 characters")
        String label,

        @Positive(message = "totalAreaAcres must be greater than 0")
        @Digits(integer = 10, fraction = 4, message = "totalAreaAcres must have at most 10 integer and 4 fractional digits")
        BigDecimal totalAreaAcres,

        @NotNull(message = "expectedVersion is required for optimistic concurrency control")
        String expectedVersion
) {}
