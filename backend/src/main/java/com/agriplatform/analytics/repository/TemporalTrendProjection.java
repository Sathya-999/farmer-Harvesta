package com.agriplatform.analytics.repository;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Spring Data JPA projection for temporal trend aggregation.
 */
public interface TemporalTrendProjection {
    Short getCultivationYear();
    UUID getSeasonId();
    String getSeasonName();
    BigDecimal getActiveArea();
    BigDecimal getCompletedArea();
    Long getTotalCultivations();
    Long getTotalFarmers();
}
