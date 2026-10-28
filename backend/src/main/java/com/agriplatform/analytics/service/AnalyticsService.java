package com.agriplatform.analytics.service;

import com.agriplatform.analytics.dto.*;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for the Agricultural Intelligence Platform Analytics Engine.
 *
 * <h2>Domain Guarantees</h2>
 * <ul>
 *   <li>Decision support only: zero prescriptive/autonomous recommendations.</li>
 *   <li>Strict separation of current state ({@code PLANNED}, {@code STARTED}) from historical terminal states.</li>
 *   <li>Zero PII leakage in aggregate intelligence queries.</li>
 *   <li>Owner-scoped IDOR protection on farmer personal summary.</li>
 * </ul>
 */
public interface AnalyticsService {

    /**
     * Computes the high-level aggregate summary metrics for a given geographic boundary.
     *
     * @param level           geographic hierarchy level (VILLAGE, MANDAL, DISTRICT, STATE)
     * @param geographyId     UUID of the geographic entity
     * @param seasonId        optional season UUID filter
     * @param cultivationYear optional cultivation year filter (2000..2100)
     * @return aggregate summary DTO
     */
    GeographicSummaryDto getGeographicSummary(GeographicLevel level, UUID geographyId, UUID seasonId, Short cultivationYear);

    /**
     * Computes paginated crop distribution within a geographic boundary.
     *
     * @param level           geographic hierarchy level (VILLAGE, MANDAL, DISTRICT, STATE)
     * @param geographyId     UUID of the geographic entity
     * @param seasonId        optional season UUID filter
     * @param cultivationYear optional cultivation year filter
     * @param page            zero-based page index (min 0)
     * @param size            page size (min 1, max 100)
     * @return paginated crop distribution response
     */
    CropDistributionPageResponseDto getCropDistribution(GeographicLevel level, UUID geographyId, UUID seasonId, Short cultivationYear, int page, int size);

    /**
     * Retrieves historical temporal trends across years and seasons for a geographic boundary.
     *
     * @param level       geographic hierarchy level (VILLAGE, MANDAL, DISTRICT, STATE)
     * @param geographyId UUID of the geographic entity
     * @param cropId      optional crop UUID filter
     * @param startYear   optional start year filter
     * @param endYear     optional end year filter
     * @return list of chronological temporal data points
     */
    List<TemporalTrendDto> getTemporalTrends(GeographicLevel level, UUID geographyId, UUID cropId, Short startYear, Short endYear);

    /**
     * Generates deterministic, explainable decision-support signals and analytical insights.
     *
     * @param level           geographic hierarchy level (VILLAGE, MANDAL, DISTRICT, STATE)
     * @param geographyId     UUID of the geographic entity
     * @param seasonId        optional season UUID filter
     * @param cultivationYear optional cultivation year filter
     * @return signals and insights response
     */
    SignalsAndInsightsResponseDto getSignalsAndInsights(GeographicLevel level, UUID geographyId, UUID seasonId, Short cultivationYear);

    /**
     * Retrieves the private, IDOR-protected cultivation and land capacity summary for the calling authenticated farmer.
     *
     * @param seasonId        optional season UUID filter
     * @param cultivationYear optional cultivation year filter
     * @return farmer personal analytics summary
     */
    FarmerAnalyticsSummaryDto getFarmerSummary(UUID seasonId, Short cultivationYear);
}
