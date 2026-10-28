package com.agriplatform.analytics.repository;

import java.math.BigDecimal;

/**
 * Spring Data JPA projection for farmer land capacity summary.
 */
public interface FarmerPlotSummaryProjection {
    Long getTotalPlots();
    BigDecimal getTotalLandCapacity();
}
