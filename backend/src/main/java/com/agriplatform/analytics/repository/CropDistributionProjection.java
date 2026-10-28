package com.agriplatform.analytics.repository;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Spring Data JPA projection for crop distribution aggregation.
 */
public interface CropDistributionProjection {
    UUID getCropId();
    String getCropName();
    String getCropCategory();
    BigDecimal getTotalArea();
    Long getCultivationCount();
    Long getFarmerCount();
}
