package com.agriplatform.analytics.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Temporal analytics data point representing cultivation metrics for a specific year and season.
 */
public record TemporalTrendDto(
        Short cultivationYear,
        UUID seasonId,
        String seasonName,
        BigDecimal activeAreaAcres,
        BigDecimal completedAreaAcres,
        Long totalCultivationsCount,
        Long totalFarmersCount,
        BigDecimal yearOverYearAreaChangePercentage
) {}
