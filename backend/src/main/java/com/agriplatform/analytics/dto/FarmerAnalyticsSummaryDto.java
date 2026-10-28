package com.agriplatform.analytics.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Private, IDOR-protected cultivation and land capacity analytics summary for an individual farmer.
 *
 * <h2>Identity Scoping</h2>
 * <p>Always derived directly from the authenticated JWT {@code sub} claim.
 * Cannot be accessed or enumerated by other farmers.
 *
 * <h2>Area Semantics</h2>
 * <ul>
 *   <li>{@code totalPlotLandCapacityAcres}: Total physical land capacity across active plots.</li>
 *   <li>{@code activeCultivatedAreaAcres}: Allocated area for active cycles ({@code PLANNED} and {@code STARTED}).</li>
 *   <li>{@code landCapacityUtilizationPercentage}: (Active area / Total land capacity) * 100, where land capacity &gt; 0.</li>
 * </ul>
 */
public record FarmerAnalyticsSummaryDto(
        UUID farmerId,
        UUID seasonId,
        String seasonName,
        Short cultivationYear,
        Long totalPlotsCount,
        BigDecimal totalPlotLandCapacityAcres,
        BigDecimal activeCultivatedAreaAcres,
        BigDecimal completedCultivatedAreaAcres,
        Long activeCultivationsCount,
        Long completedCultivationsCount,
        Long cancelledCultivationsCount,
        Long abandonedCultivationsCount,
        BigDecimal landCapacityUtilizationPercentage,
        List<FarmerCropBreakdownDto> cropBreakdown
) {}
