package com.agriplatform.analytics.dto;

import java.util.List;
import java.util.UUID;

/**
 * Combined response payload containing verified signals and explainable decision-support insights.
 */
public record SignalsAndInsightsResponseDto(
        GeographicLevel level,
        UUID geographyId,
        String geographyName,
        UUID seasonId,
        Short cultivationYear,
        List<CultivationSignalDto> signals,
        List<AnalyticsInsightDto> insights
) {}
