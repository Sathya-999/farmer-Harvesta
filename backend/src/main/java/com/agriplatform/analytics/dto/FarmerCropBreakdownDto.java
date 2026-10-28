package com.agriplatform.analytics.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Breakdown of a specific crop within a farmer's personal cultivation portfolio.
 */
public record FarmerCropBreakdownDto(
        UUID cropId,
        String cropName,
        String cropCategory,
        BigDecimal activeCultivatedAreaAcres,
        Long cultivationCyclesCount,
        BigDecimal percentageOfFarmerActiveArea
) {}
