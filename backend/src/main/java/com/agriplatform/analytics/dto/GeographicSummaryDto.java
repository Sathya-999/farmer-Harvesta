package com.agriplatform.analytics.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * High-level aggregate intelligence summary for a specific geographic boundary.
 *
 * <h2>Privacy Guarantee</h2>
 * <p>Contains strictly aggregated numerical counts, areas, and statistical indicators.
 * Exposes ZERO personally identifiable information (no farmer names, phones, user IDs, or plot labels).
 *
 * <h2>Area Semantics</h2>
 * <p>Canonical unit is <strong>ACRES</strong>.
 * <ul>
 *   <li>{@code activeCultivatedAreaAcres}: Sum of {@code cultivated_area_acres} for current active
 *       cultivations ({@code PLANNED} and {@code STARTED}).</li>
 *   <li>{@code completedCultivatedAreaAcres}: Sum of verified harvested/cultivated area for
 *       historical {@code COMPLETED} cultivations.</li>
 * </ul>
 */
public record GeographicSummaryDto(
        GeographicLevel level,
        UUID id,
        String name,
        String code,
        String parentName,
        UUID seasonId,
        String seasonName,
        Short cultivationYear,
        BigDecimal activeCultivatedAreaAcres,
        Long activeCultivationCount,
        Long activeFarmerCount,
        Long activePlotCount,
        BigDecimal averageAreaPerFarmerAcres,
        BigDecimal averageAreaPerCultivationAcres,
        BigDecimal completedCultivatedAreaAcres,
        Long completedCultivationCount,
        Long cancelledCultivationCount,
        Long abandonedCultivationCount
) {}
