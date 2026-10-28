package com.agriplatform.analytics.repository;

import java.math.BigDecimal;

/**
 * Spring Data JPA projection for farmer personal cultivation area and counts summary.
 */
public interface FarmerCultivationSummaryProjection {
    BigDecimal getActiveArea();
    BigDecimal getCompletedArea();
    Long getActiveCount();
    Long getCompletedCount();
    Long getCancelledCount();
    Long getAbandonedCount();
}
