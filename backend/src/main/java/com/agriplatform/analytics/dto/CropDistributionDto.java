package com.agriplatform.analytics.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Aggregated crop allocation statistics within a geographic entity.
 */
public record CropDistributionDto(
        UUID cropId,
        String cropName,
        String cropCategory,
        BigDecimal activeCultivatedAreaAcres,
        Long cultivationCount,
        Long farmerCount,
        BigDecimal percentageShare
) {}
