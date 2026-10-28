package com.agriplatform.analytics.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Paginated crop distribution response containing the current page items and aggregate context totals.
 */
public record CropDistributionPageResponseDto(
        GeographicLevel level,
        UUID geographyId,
        String geographyName,
        BigDecimal totalActiveCultivatedAreaAcres,
        Long totalActiveCultivationsCount,
        Long totalActiveFarmersCount,
        List<CropDistributionDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
