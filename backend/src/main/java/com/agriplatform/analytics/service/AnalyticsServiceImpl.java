package com.agriplatform.analytics.service;

import com.agriplatform.analytics.dto.*;
import com.agriplatform.analytics.repository.*;
import com.agriplatform.common.exception.BadRequestException;
import com.agriplatform.common.exception.ResourceNotFoundException;
import com.agriplatform.common.util.SecurityUtils;
import com.agriplatform.cultivation.entity.Crop;
import com.agriplatform.cultivation.entity.Season;
import com.agriplatform.cultivation.repository.CropRepository;
import com.agriplatform.cultivation.repository.SeasonRepository;
import com.agriplatform.farmer.entity.FarmerProfile;
import com.agriplatform.farmer.repository.FarmerProfileRepository;
import com.agriplatform.geography.entity.District;
import com.agriplatform.geography.entity.Mandal;
import com.agriplatform.geography.entity.State;
import com.agriplatform.geography.entity.Village;
import com.agriplatform.geography.repository.DistrictRepository;
import com.agriplatform.geography.repository.MandalRepository;
import com.agriplatform.geography.repository.StateRepository;
import com.agriplatform.geography.repository.VillageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Production implementation of {@link AnalyticsService}.
 *
 * <h2>Architectural Guarantees</h2>
 * <ul>
 *   <li>Database-side aggregation: No N+1 queries; heavy grouping and aggregation occur in PostgreSQL.</li>
 *   <li>Strict State Separation: Current state ({@code PLANNED}, {@code STARTED}) is strictly isolated
 *       from historical terminal states ({@code COMPLETED}, {@code CANCELLED}, {@code ABANDONED}).</li>
 *   <li>Double-Counting Defense: Cultivation history snapshots are never included in current acreage totals.</li>
 *   <li>Zero PII Leakage: Aggregate endpoints expose exclusively anonymous statistics.</li>
 *   <li>Decision-Support Only: Signals and insights are deterministic, explainable, and non-prescriptive.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsServiceImpl.class);

    private static final Short MIN_YEAR = 2000;
    private static final Short MAX_YEAR = 2100;
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");
    private static final BigDecimal ZERO_AREA = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final AnalyticsRepository analyticsRepository;
    private final VillageRepository villageRepository;
    private final MandalRepository mandalRepository;
    private final DistrictRepository districtRepository;
    private final StateRepository stateRepository;
    private final CropRepository cropRepository;
    private final SeasonRepository seasonRepository;
    private final FarmerProfileRepository farmerProfileRepository;
    private final SecurityUtils securityUtils;

    public AnalyticsServiceImpl(
            AnalyticsRepository analyticsRepository,
            VillageRepository villageRepository,
            MandalRepository mandalRepository,
            DistrictRepository districtRepository,
            StateRepository stateRepository,
            CropRepository cropRepository,
            SeasonRepository seasonRepository,
            FarmerProfileRepository farmerProfileRepository,
            SecurityUtils securityUtils) {
        this.analyticsRepository = analyticsRepository;
        this.villageRepository = villageRepository;
        this.mandalRepository = mandalRepository;
        this.districtRepository = districtRepository;
        this.stateRepository = stateRepository;
        this.cropRepository = cropRepository;
        this.seasonRepository = seasonRepository;
        this.farmerProfileRepository = farmerProfileRepository;
        this.securityUtils = securityUtils;
    }

    @Override
    public GeographicSummaryDto getGeographicSummary(
            GeographicLevel level, UUID geographyId, UUID seasonId, Short cultivationYear) {

        validateLevelAndId(level, geographyId);
        validateYear(cultivationYear);

        GeographyMetadata geoMeta = resolveGeographyMetadata(level, geographyId);
        String seasonName = resolveSeasonName(seasonId);

        GeographicSummaryProjection projection = switch (level) {
            case VILLAGE -> analyticsRepository.aggregateVillageSummary(geographyId, seasonId, cultivationYear);
            case MANDAL -> analyticsRepository.aggregateMandalSummary(geographyId, seasonId, cultivationYear);
            case DISTRICT -> analyticsRepository.aggregateDistrictSummary(geographyId, seasonId, cultivationYear);
            case STATE -> analyticsRepository.aggregateStateSummary(geographyId, seasonId, cultivationYear);
        };

        BigDecimal activeArea = projection.getActiveArea() != null
                ? projection.getActiveArea().setScale(2, RoundingMode.HALF_UP) : ZERO_AREA;
        Long activeCultivations = projection.getActiveCultivations() != null ? projection.getActiveCultivations() : 0L;
        Long activeFarmers = projection.getActiveFarmers() != null ? projection.getActiveFarmers() : 0L;
        Long activePlots = projection.getActivePlots() != null ? projection.getActivePlots() : 0L;

        BigDecimal completedArea = projection.getCompletedArea() != null
                ? projection.getCompletedArea().setScale(2, RoundingMode.HALF_UP) : ZERO_AREA;
        Long completedCultivations = projection.getCompletedCultivations() != null ? projection.getCompletedCultivations() : 0L;
        Long cancelledCultivations = projection.getCancelledCultivations() != null ? projection.getCancelledCultivations() : 0L;
        Long abandonedCultivations = projection.getAbandonedCultivations() != null ? projection.getAbandonedCultivations() : 0L;

        BigDecimal avgAreaPerFarmer = activeFarmers > 0
                ? activeArea.divide(BigDecimal.valueOf(activeFarmers), 2, RoundingMode.HALF_UP)
                : ZERO_AREA;

        BigDecimal avgAreaPerCultivation = activeCultivations > 0
                ? activeArea.divide(BigDecimal.valueOf(activeCultivations), 2, RoundingMode.HALF_UP)
                : ZERO_AREA;

        return new GeographicSummaryDto(
                level,
                geographyId,
                geoMeta.name(),
                geoMeta.code(),
                geoMeta.parentName(),
                seasonId,
                seasonName,
                cultivationYear,
                activeArea,
                activeCultivations,
                activeFarmers,
                activePlots,
                avgAreaPerFarmer,
                avgAreaPerCultivation,
                completedArea,
                completedCultivations,
                cancelledCultivations,
                abandonedCultivations
        );
    }

    @Override
    public CropDistributionPageResponseDto getCropDistribution(
            GeographicLevel level, UUID geographyId, UUID seasonId, Short cultivationYear, int page, int size) {

        validateLevelAndId(level, geographyId);
        validateYear(cultivationYear);
        validatePagination(page, size);

        GeographyMetadata geoMeta = resolveGeographyMetadata(level, geographyId);
        resolveSeasonName(seasonId); // validates season existence if non-null

        List<CropDistributionProjection> projections = switch (level) {
            case VILLAGE -> analyticsRepository.findCropDistributionForVillage(geographyId, seasonId, cultivationYear);
            case MANDAL -> analyticsRepository.findCropDistributionForMandal(geographyId, seasonId, cultivationYear);
            case DISTRICT -> analyticsRepository.findCropDistributionForDistrict(geographyId, seasonId, cultivationYear);
            case STATE -> analyticsRepository.findCropDistributionForState(geographyId, seasonId, cultivationYear);
        };

        // Calculate grand totals across all crops in this geography for percentages
        BigDecimal totalActiveArea = BigDecimal.ZERO;
        long totalCultivations = 0L;
        long totalFarmers = 0L;

        for (CropDistributionProjection p : projections) {
            if (p.getTotalArea() != null) {
                totalActiveArea = totalActiveArea.add(p.getTotalArea());
            }
            if (p.getCultivationCount() != null) {
                totalCultivations += p.getCultivationCount();
            }
            if (p.getFarmerCount() != null) {
                totalFarmers += p.getFarmerCount();
            }
        }
        totalActiveArea = totalActiveArea.setScale(2, RoundingMode.HALF_UP);

        List<CropDistributionDto> allCropDtos = new ArrayList<>(projections.size());
        for (CropDistributionProjection p : projections) {
            BigDecimal area = p.getTotalArea() != null ? p.getTotalArea().setScale(2, RoundingMode.HALF_UP) : ZERO_AREA;
            BigDecimal share = totalActiveArea.compareTo(BigDecimal.ZERO) > 0
                    ? area.multiply(HUNDRED).divide(totalActiveArea, 2, RoundingMode.HALF_UP)
                    : ZERO_AREA;

            allCropDtos.add(new CropDistributionDto(
                    p.getCropId(),
                    p.getCropName(),
                    p.getCropCategory(),
                    area,
                    p.getCultivationCount() != null ? p.getCultivationCount() : 0L,
                    p.getFarmerCount() != null ? p.getFarmerCount() : 0L,
                    share
            ));
        }

        // Apply pagination
        int totalElements = allCropDtos.size();
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        int fromIndex = page * size;
        List<CropDistributionDto> pagedContent;

        if (fromIndex >= totalElements) {
            pagedContent = Collections.emptyList();
        } else {
            int toIndex = Math.min(fromIndex + size, totalElements);
            pagedContent = allCropDtos.subList(fromIndex, toIndex);
        }

        return new CropDistributionPageResponseDto(
                level,
                geographyId,
                geoMeta.name(),
                totalActiveArea,
                totalCultivations,
                totalFarmers,
                pagedContent,
                page,
                size,
                totalElements,
                totalPages
        );
    }

    @Override
    public List<TemporalTrendDto> getTemporalTrends(
            GeographicLevel level, UUID geographyId, UUID cropId, Short startYear, Short endYear) {

        validateLevelAndId(level, geographyId);
        validateYear(startYear);
        validateYear(endYear);

        if (startYear != null && endYear != null && startYear > endYear) {
            throw new BadRequestException("startYear (" + startYear + ") cannot be greater than endYear (" + endYear + ")");
        }

        resolveGeographyMetadata(level, geographyId);

        if (cropId != null) {
            cropRepository.findById(cropId)
                    .orElseThrow(() -> new ResourceNotFoundException("Crop not found: " + cropId));
        }

        List<TemporalTrendProjection> projections = switch (level) {
            case VILLAGE -> analyticsRepository.findTemporalTrendsForVillage(geographyId, cropId, startYear, endYear);
            case MANDAL -> analyticsRepository.findTemporalTrendsForMandal(geographyId, cropId, startYear, endYear);
            case DISTRICT -> analyticsRepository.findTemporalTrendsForDistrict(geographyId, cropId, startYear, endYear);
            case STATE -> analyticsRepository.findTemporalTrendsForState(geographyId, cropId, startYear, endYear);
        };

        List<TemporalTrendDto> result = new ArrayList<>(projections.size());
        Map<String, BigDecimal> prevYearAreaBySeason = new HashMap<>();

        for (TemporalTrendProjection p : projections) {
            BigDecimal activeArea = p.getActiveArea() != null ? p.getActiveArea().setScale(2, RoundingMode.HALF_UP) : ZERO_AREA;
            BigDecimal completedArea = p.getCompletedArea() != null ? p.getCompletedArea().setScale(2, RoundingMode.HALF_UP) : ZERO_AREA;
            BigDecimal totalCycleArea = activeArea.add(completedArea);

            String seasonKey = p.getSeasonName() != null ? p.getSeasonName().toLowerCase() : "unknown";
            BigDecimal priorArea = prevYearAreaBySeason.get(seasonKey);
            BigDecimal yoyChange = null;

            if (priorArea != null && priorArea.compareTo(BigDecimal.ZERO) > 0) {
                yoyChange = totalCycleArea.subtract(priorArea)
                        .multiply(HUNDRED)
                        .divide(priorArea, 2, RoundingMode.HALF_UP);
            }

            prevYearAreaBySeason.put(seasonKey, totalCycleArea);

            result.add(new TemporalTrendDto(
                    p.getCultivationYear(),
                    p.getSeasonId(),
                    p.getSeasonName(),
                    activeArea,
                    completedArea,
                    p.getTotalCultivations() != null ? p.getTotalCultivations() : 0L,
                    p.getTotalFarmers() != null ? p.getTotalFarmers() : 0L,
                    yoyChange
            ));
        }

        return result;
    }

    @Override
    public SignalsAndInsightsResponseDto getSignalsAndInsights(
            GeographicLevel level, UUID geographyId, UUID seasonId, Short cultivationYear) {

        validateLevelAndId(level, geographyId);
        validateYear(cultivationYear);

        GeographyMetadata geoMeta = resolveGeographyMetadata(level, geographyId);
        resolveSeasonName(seasonId);

        // Fetch summary facts
        GeographicSummaryDto summary = getGeographicSummary(level, geographyId, seasonId, cultivationYear);

        // Fetch crop distributions
        CropDistributionPageResponseDto cropDist = getCropDistribution(level, geographyId, seasonId, cultivationYear, 0, 100);
        List<CropDistributionDto> crops = cropDist.content();

        List<CultivationSignalDto> signals = new ArrayList<>();
        List<AnalyticsInsightDto> insights = new ArrayList<>();

        // 1. Crop Concentration Signal & Insight
        if (summary.activeCultivatedAreaAcres().compareTo(BigDecimal.ZERO) == 0) {
            signals.add(new CultivationSignalDto(
                    "NO_ACTIVE_CULTIVATION_DATA",
                    "INFORMATION",
                    "No Active Cultivation Data",
                    "No active cultivation intentions (PLANNED or STARTED) are currently recorded for the specified criteria.",
                    Map.of("totalActiveAreaAcres", ZERO_AREA, "activeCultivationCount", 0)
            ));
            insights.add(new AnalyticsInsightDto(
                    "CROP_ALLOCATION",
                    "No Active Cultivation Intentions",
                    "No active cultivation intentions have been recorded in " + geoMeta.name() + " for the selected criteria."
            ));
        } else if (!crops.isEmpty()) {
            CropDistributionDto topCrop = crops.get(0);
            BigDecimal topShare = topCrop.percentageShare();

            if (topShare.compareTo(new BigDecimal("60.00")) >= 0) {
                signals.add(new CultivationSignalDto(
                        "HIGH_CROP_CONCENTRATION",
                        "OBSERVATION",
                        "High Crop Concentration",
                        "Single crop (" + topCrop.cropName() + ") constitutes " + topShare + "% of active cultivation area in this region.",
                        Map.of("leadingCrop", topCrop.cropName(), "percentageShare", topShare, "areaAcres", topCrop.activeCultivatedAreaAcres())
                ));
                insights.add(new AnalyticsInsightDto(
                        "CROP_ALLOCATION",
                        "Dominant Primary Crop Allocation",
                        "High concentration observed: " + topCrop.cropName() + " represents " + topShare + "% (" +
                                topCrop.activeCultivatedAreaAcres() + " acres) of all active cultivation area in " + geoMeta.name() + "."
                ));
            } else if (topShare.compareTo(new BigDecimal("40.00")) >= 0) {
                signals.add(new CultivationSignalDto(
                        "MODERATE_CROP_CONCENTRATION",
                        "OBSERVATION",
                        "Moderate Crop Concentration",
                        "Leading crop (" + topCrop.cropName() + ") constitutes " + topShare + "% of active cultivation area.",
                        Map.of("leadingCrop", topCrop.cropName(), "percentageShare", topShare, "areaAcres", topCrop.activeCultivatedAreaAcres())
                ));
                insights.add(new AnalyticsInsightDto(
                        "CROP_ALLOCATION",
                        "Primary Crop Plurality",
                        "Moderate concentration: " + topCrop.cropName() + " leads regional planned acreage at " + topShare + "% (" +
                                topCrop.activeCultivatedAreaAcres() + " acres), alongside " + (crops.size() - 1) + " other registered crops."
                ));
            } else {
                signals.add(new CultivationSignalDto(
                        "BALANCED_CROP_DISTRIBUTION",
                        "INFORMATION",
                        "Balanced Crop Distribution",
                        "Active cultivation area is distributed across multiple crops with no single crop exceeding 40% of acreage.",
                        Map.of("leadingCrop", topCrop.cropName(), "percentageShare", topShare, "totalDistinctCrops", crops.size())
                ));
                insights.add(new AnalyticsInsightDto(
                        "CROP_ALLOCATION",
                        "Diversified Cultivation Portfolio",
                        "Regional cultivation is diversified across " + crops.size() + " crops, with " + topCrop.cropName() +
                                " holding the largest individual share at " + topShare + "%."
                ));
            }
        }

        // 2. Farmer Participation Density Signal & Insight
        if (summary.activeFarmerCount() >= 10) {
            signals.add(new CultivationSignalDto(
                    "STRONG_FARMER_PARTICIPATION",
                    "INFORMATION",
                    "Active Farmer Engagement",
                    summary.activeFarmerCount() + " active farmers registered cultivation intentions across " + summary.activePlotCount() + " plots.",
                    Map.of("activeFarmers", summary.activeFarmerCount(), "activePlots", summary.activePlotCount(), "averageAreaPerFarmer", summary.averageAreaPerFarmerAcres())
            ));
            insights.add(new AnalyticsInsightDto(
                    "PARTICIPATION",
                    "Widespread Farmer Engagement",
                    "Broad participation documented in " + geoMeta.name() + " with " + summary.activeFarmerCount() +
                            " active farmers managing " + summary.activePlotCount() + " plots with an average allocation of " +
                            summary.averageAreaPerFarmerAcres() + " acres per farmer."
            ));
        } else if (summary.activeFarmerCount() > 0) {
            signals.add(new CultivationSignalDto(
                    "EMERGING_FARMER_PARTICIPATION",
                    "INFORMATION",
                    "Emerging Farmer Participation",
                    "Recorded participation currently stands at " + summary.activeFarmerCount() + " active farmers across " + summary.activePlotCount() + " plots.",
                    Map.of("activeFarmers", summary.activeFarmerCount(), "activePlots", summary.activePlotCount())
            ));
            insights.add(new AnalyticsInsightDto(
                    "PARTICIPATION",
                    "Initial Participation Baseline",
                    "Participation is in an emerging phase with " + summary.activeFarmerCount() + " active farmers recorded."
            ));
        }

        return new SignalsAndInsightsResponseDto(
                level,
                geographyId,
                geoMeta.name(),
                seasonId,
                cultivationYear,
                signals,
                insights
        );
    }

    @Override
    public FarmerAnalyticsSummaryDto getFarmerSummary(UUID seasonId, Short cultivationYear) {
        UUID farmerId = securityUtils.getAuthenticatedUserId();
        validateYear(cultivationYear);

        FarmerProfile profile = farmerProfileRepository.findById(farmerId)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer profile not found for user: " + farmerId));

        if (FarmerProfile.STATUS_DELETED.equals(profile.getProfileStatus())) {
            throw new ResourceNotFoundException("Farmer profile is deleted: " + farmerId);
        }

        String seasonName = resolveSeasonName(seasonId);

        // Aggregate plots
        FarmerPlotSummaryProjection plotSummary = analyticsRepository.aggregateFarmerPlots(farmerId);
        Long totalPlots = plotSummary.getTotalPlots() != null ? plotSummary.getTotalPlots() : 0L;
        BigDecimal totalLandCapacity = plotSummary.getTotalLandCapacity() != null
                ? plotSummary.getTotalLandCapacity().setScale(2, RoundingMode.HALF_UP) : ZERO_AREA;

        // Aggregate cultivations
        FarmerCultivationSummaryProjection cultSummary = analyticsRepository.aggregateFarmerCultivation(
                farmerId, seasonId, cultivationYear);

        BigDecimal activeArea = cultSummary.getActiveArea() != null
                ? cultSummary.getActiveArea().setScale(2, RoundingMode.HALF_UP) : ZERO_AREA;
        BigDecimal completedArea = cultSummary.getCompletedArea() != null
                ? cultSummary.getCompletedArea().setScale(2, RoundingMode.HALF_UP) : ZERO_AREA;
        Long activeCount = cultSummary.getActiveCount() != null ? cultSummary.getActiveCount() : 0L;
        Long completedCount = cultSummary.getCompletedCount() != null ? cultSummary.getCompletedCount() : 0L;
        Long cancelledCount = cultSummary.getCancelledCount() != null ? cultSummary.getCancelledCount() : 0L;
        Long abandonedCount = cultSummary.getAbandonedCount() != null ? cultSummary.getAbandonedCount() : 0L;

        BigDecimal utilizationRate = totalLandCapacity.compareTo(BigDecimal.ZERO) > 0
                ? activeArea.multiply(HUNDRED).divide(totalLandCapacity, 2, RoundingMode.HALF_UP)
                : ZERO_AREA;

        // Fetch crop breakdown
        List<FarmerCropBreakdownProjection> cropProjections = analyticsRepository.findFarmerCropBreakdown(
                farmerId, seasonId, cultivationYear);

        List<FarmerCropBreakdownDto> cropBreakdowns = new ArrayList<>(cropProjections.size());
        for (FarmerCropBreakdownProjection cp : cropProjections) {
            BigDecimal cropArea = cp.getActiveArea() != null ? cp.getActiveArea().setScale(2, RoundingMode.HALF_UP) : ZERO_AREA;
            BigDecimal share = activeArea.compareTo(BigDecimal.ZERO) > 0
                    ? cropArea.multiply(HUNDRED).divide(activeArea, 2, RoundingMode.HALF_UP)
                    : ZERO_AREA;

            cropBreakdowns.add(new FarmerCropBreakdownDto(
                    cp.getCropId(),
                    cp.getCropName(),
                    cp.getCropCategory(),
                    cropArea,
                    cp.getCultivationCyclesCount() != null ? cp.getCultivationCyclesCount() : 0L,
                    share
            ));
        }

        return new FarmerAnalyticsSummaryDto(
                farmerId,
                seasonId,
                seasonName,
                cultivationYear,
                totalPlots,
                totalLandCapacity,
                activeArea,
                completedArea,
                activeCount,
                completedCount,
                cancelledCount,
                abandonedCount,
                utilizationRate,
                cropBreakdowns
        );
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private void validateLevelAndId(GeographicLevel level, UUID geographyId) {
        if (level == null) {
            throw new BadRequestException("Geographic level must not be null");
        }
        if (geographyId == null) {
            throw new BadRequestException("Geography ID must not be null");
        }
    }

    private void validateYear(Short year) {
        if (year != null && (year < MIN_YEAR || year > MAX_YEAR)) {
            throw new BadRequestException("Cultivation year must be between " + MIN_YEAR + " and " + MAX_YEAR + ": " + year);
        }
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new BadRequestException("Page index must not be negative: " + page);
        }
        if (size < 1 || size > 100) {
            throw new BadRequestException("Page size must be between 1 and 100: " + size);
        }
    }

    private String resolveSeasonName(UUID seasonId) {
        if (seasonId == null) {
            return null;
        }
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new ResourceNotFoundException("Season not found: " + seasonId));
        return season.getName();
    }

    private GeographyMetadata resolveGeographyMetadata(GeographicLevel level, UUID geographyId) {
        return switch (level) {
            case VILLAGE -> {
                Village village = villageRepository.findById(geographyId)
                        .orElseThrow(() -> new ResourceNotFoundException("Village not found: " + geographyId));
                String parent = village.getMandal() != null ? village.getMandal().getName() : null;
                yield new GeographyMetadata(village.getName(), village.getCode(), parent);
            }
            case MANDAL -> {
                Mandal mandal = mandalRepository.findById(geographyId)
                        .orElseThrow(() -> new ResourceNotFoundException("Mandal not found: " + geographyId));
                String parent = mandal.getDistrict() != null ? mandal.getDistrict().getName() : null;
                yield new GeographyMetadata(mandal.getName(), mandal.getCode(), parent);
            }
            case DISTRICT -> {
                District district = districtRepository.findById(geographyId)
                        .orElseThrow(() -> new ResourceNotFoundException("District not found: " + geographyId));
                String parent = district.getState() != null ? district.getState().getName() : null;
                yield new GeographyMetadata(district.getName(), district.getCode(), parent);
            }
            case STATE -> {
                State state = stateRepository.findById(geographyId)
                        .orElseThrow(() -> new ResourceNotFoundException("State not found: " + geographyId));
                yield new GeographyMetadata(state.getName(), state.getCode(), "India");
            }
        };
    }

    private record GeographyMetadata(String name, String code, String parentName) {}
}
