package com.agriplatform.analytics.controller;

import com.agriplatform.analytics.dto.*;
import com.agriplatform.analytics.service.AnalyticsService;
import com.agriplatform.common.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for the Agricultural Intelligence Platform Analytics Engine — {@code /api/v1/analytics}.
 *
 * <h2>Endpoint Inventory</h2>
 * <ul>
 *   <li>{@code GET /api/v1/analytics/geography/summary} — Regional aggregate facts and metrics</li>
 *   <li>{@code GET /api/v1/analytics/geography/crops} — Paginated crop distribution within geography</li>
 *   <li>{@code GET /api/v1/analytics/geography/trends} — Chronological temporal trends across seasons/years</li>
 *   <li>{@code GET /api/v1/analytics/geography/signals} — Decision-support signals and analytical insights</li>
 *   <li>{@code GET /api/v1/analytics/farmer/summary} — Private, IDOR-protected farmer cultivation summary</li>
 * </ul>
 *
 * <h2>Security & Privacy Guarantee</h2>
 * <p>All endpoints require a valid Supabase JWT Bearer token.
 * Aggregate regional endpoints return strictly anonymized statistics without any farmer PII.
 * The farmer personal endpoint is strictly scoped to the caller's JWT {@code sub} claim.
 */
@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * Retrieves high-level aggregate summary metrics for a specified geographic boundary.
     *
     * @param level           geographic hierarchy level: VILLAGE, MANDAL, DISTRICT, STATE
     * @param id              UUID of the geographic entity
     * @param seasonId        optional season UUID filter
     * @param cultivationYear optional cultivation year filter (2000..2100)
     * @return aggregate summary DTO wrapped in ApiResponse
     */
    @GetMapping("/geography/summary")
    public ResponseEntity<ApiResponse<GeographicSummaryDto>> getGeographicSummary(
            @RequestParam("level") String level,
            @RequestParam("id") UUID id,
            @RequestParam(value = "seasonId", required = false) UUID seasonId,
            @RequestParam(value = "cultivationYear", required = false) Short cultivationYear) {

        log.debug("GET /api/v1/analytics/geography/summary - level: {}, id: {}, seasonId: {}, year: {}",
                level, id, seasonId, cultivationYear);

        GeographicLevel geoLevel = GeographicLevel.fromString(level);
        GeographicSummaryDto summary = analyticsService.getGeographicSummary(geoLevel, id, seasonId, cultivationYear);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    /**
     * Retrieves paginated crop distribution within a specified geographic boundary.
     *
     * @param level           geographic hierarchy level: VILLAGE, MANDAL, DISTRICT, STATE
     * @param id              UUID of the geographic entity
     * @param seasonId        optional season UUID filter
     * @param cultivationYear optional cultivation year filter
     * @param page            zero-based page index (default 0)
     * @param size            page size (default 20, max 100)
     * @return paginated crop distribution response
     */
    @GetMapping("/geography/crops")
    public ResponseEntity<ApiResponse<CropDistributionPageResponseDto>> getCropDistribution(
            @RequestParam("level") String level,
            @RequestParam("id") UUID id,
            @RequestParam(value = "seasonId", required = false) UUID seasonId,
            @RequestParam(value = "cultivationYear", required = false) Short cultivationYear,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        log.debug("GET /api/v1/analytics/geography/crops - level: {}, id: {}, page: {}, size: {}",
                level, id, page, size);

        GeographicLevel geoLevel = GeographicLevel.fromString(level);
        CropDistributionPageResponseDto response = analyticsService.getCropDistribution(
                geoLevel, id, seasonId, cultivationYear, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Retrieves chronological temporal trends across years and seasons for a geographic boundary.
     *
     * @param level       geographic hierarchy level: VILLAGE, MANDAL, DISTRICT, STATE
     * @param id          UUID of the geographic entity
     * @param cropId      optional crop UUID filter
     * @param startYear   optional start year filter
     * @param endYear     optional end year filter
     * @return list of chronological temporal data points
     */
    @GetMapping("/geography/trends")
    public ResponseEntity<ApiResponse<List<TemporalTrendDto>>> getTemporalTrends(
            @RequestParam("level") String level,
            @RequestParam("id") UUID id,
            @RequestParam(value = "cropId", required = false) UUID cropId,
            @RequestParam(value = "startYear", required = false) Short startYear,
            @RequestParam(value = "endYear", required = false) Short endYear) {

        log.debug("GET /api/v1/analytics/geography/trends - level: {}, id: {}, cropId: {}, startYear: {}, endYear: {}",
                level, id, cropId, startYear, endYear);

        GeographicLevel geoLevel = GeographicLevel.fromString(level);
        List<TemporalTrendDto> trends = analyticsService.getTemporalTrends(geoLevel, id, cropId, startYear, endYear);
        return ResponseEntity.ok(ApiResponse.success(trends));
    }

    /**
     * Generates deterministic, explainable decision-support signals and analytical insights.
     *
     * @param level           geographic hierarchy level: VILLAGE, MANDAL, DISTRICT, STATE
     * @param id              UUID of the geographic entity
     * @param seasonId        optional season UUID filter
     * @param cultivationYear optional cultivation year filter
     * @return signals and insights response
     */
    @GetMapping("/geography/signals")
    public ResponseEntity<ApiResponse<SignalsAndInsightsResponseDto>> getSignalsAndInsights(
            @RequestParam("level") String level,
            @RequestParam("id") UUID id,
            @RequestParam(value = "seasonId", required = false) UUID seasonId,
            @RequestParam(value = "cultivationYear", required = false) Short cultivationYear) {

        log.debug("GET /api/v1/analytics/geography/signals - level: {}, id: {}, seasonId: {}, year: {}",
                level, id, seasonId, cultivationYear);

        GeographicLevel geoLevel = GeographicLevel.fromString(level);
        SignalsAndInsightsResponseDto response = analyticsService.getSignalsAndInsights(
                geoLevel, id, seasonId, cultivationYear);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Retrieves the private, IDOR-protected cultivation and land capacity summary for the calling authenticated farmer.
     *
     * @param seasonId        optional season UUID filter
     * @param cultivationYear optional cultivation year filter
     * @return farmer personal analytics summary
     */
    @GetMapping("/farmer/summary")
    public ResponseEntity<ApiResponse<FarmerAnalyticsSummaryDto>> getFarmerSummary(
            @RequestParam(value = "seasonId", required = false) UUID seasonId,
            @RequestParam(value = "cultivationYear", required = false) Short cultivationYear) {

        log.debug("GET /api/v1/analytics/farmer/summary - seasonId: {}, year: {}", seasonId, cultivationYear);

        FarmerAnalyticsSummaryDto summary = analyticsService.getFarmerSummary(seasonId, cultivationYear);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
}
