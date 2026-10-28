package com.agriplatform.analytics.repository;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Spring Data JPA projection for aggregate geographic summary metrics.
 */
public interface GeographicSummaryProjection {
    BigDecimal getActiveArea();
    Long getActiveCultivations();
    Long getActiveFarmers();
    Long getActivePlots();
    BigDecimal getCompletedArea();
    Long getCompletedCultivations();
    Long getCancelledCultivations();
    Long getAbandonedCultivations();
}
