package com.agriplatform.analytics.repository;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Spring Data JPA projection for farmer crop-level breakdown.
 */
public interface FarmerCropBreakdownProjection {
    UUID getCropId();
    String getCropName();
    String getCropCategory();
    BigDecimal getActiveArea();
    Long getCultivationCyclesCount();
}
