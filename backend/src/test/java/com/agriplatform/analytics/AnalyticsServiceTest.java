package com.agriplatform.analytics;

import com.agriplatform.analytics.dto.*;
import com.agriplatform.analytics.repository.*;
import com.agriplatform.analytics.service.AnalyticsServiceImpl;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Task 10 — AnalyticsService Unit Tests")
class AnalyticsServiceTest {

    @Mock private AnalyticsRepository analyticsRepository;
    @Mock private VillageRepository villageRepository;
    @Mock private MandalRepository mandalRepository;
    @Mock private DistrictRepository districtRepository;
    @Mock private StateRepository stateRepository;
    @Mock private CropRepository cropRepository;
    @Mock private SeasonRepository seasonRepository;
    @Mock private FarmerProfileRepository farmerProfileRepository;
    @Mock private SecurityUtils securityUtils;

    @InjectMocks
    private AnalyticsServiceImpl analyticsService;

    private UUID villageId;
    private UUID mandalId;
    private UUID districtId;
    private UUID stateId;
    private UUID seasonId;
    private UUID farmerId;
    private UUID crop1Id;
    private UUID crop2Id;

    private Village village;
    private Mandal mandal;
    private District district;
    private State state;
    private Season season;
    private FarmerProfile farmerProfile;

    @BeforeEach
    void setUp() {
        villageId = UUID.randomUUID();
        mandalId = UUID.randomUUID();
        districtId = UUID.randomUUID();
        stateId = UUID.randomUUID();
        seasonId = UUID.randomUUID();
        farmerId = UUID.randomUUID();
        crop1Id = UUID.randomUUID();
        crop2Id = UUID.randomUUID();

        state = new State();
        state.setId(stateId);
        state.setName("Andhra Pradesh");
        state.setCode("AP");

        district = new District();
        district.setId(districtId);
        district.setName("Anantapur");
        district.setCode("ATP");
        district.setState(state);

        mandal = new Mandal();
        mandal.setId(mandalId);
        mandal.setName("Atmakur");
        mandal.setCode("ATM");
        mandal.setDistrict(district);

        village = new Village();
        village.setId(villageId);
        village.setName("Thopudurthi");
        village.setCode("THOP");
        village.setMandal(mandal);

        season = new Season();
        season.setId(seasonId);
        season.setName("Kharif");

        farmerProfile = new FarmerProfile();
        farmerProfile.setId(farmerId);
        farmerProfile.setFullName("Ravi Kumar");
        farmerProfile.setProfileStatus(FarmerProfile.STATUS_ACTIVE);
    }

    // =========================================================================
    // 1. GEOGRAPHIC SUMMARY TESTS
    // =========================================================================

    @Nested
    @DisplayName("Geographic Summary Aggregation Tests")
    class GeographicSummaryTests {

        @Test
        @DisplayName("Village level summary aggregates correctly with safe averages")
        void testVillageSummarySuccess() {
            when(villageRepository.findById(villageId)).thenReturn(Optional.of(village));
            when(seasonRepository.findById(seasonId)).thenReturn(Optional.of(season));

            GeographicSummaryProjection mockProj = mock(GeographicSummaryProjection.class);
            when(mockProj.getActiveArea()).thenReturn(new BigDecimal("150.50"));
            when(mockProj.getActiveCultivations()).thenReturn(10L);
            when(mockProj.getActiveFarmers()).thenReturn(5L);
            when(mockProj.getActivePlots()).thenReturn(8L);
            when(mockProj.getCompletedArea()).thenReturn(new BigDecimal("40.00"));
            when(mockProj.getCompletedCultivations()).thenReturn(3L);
            when(mockProj.getCancelledCultivations()).thenReturn(1L);
            when(mockProj.getAbandonedCultivations()).thenReturn(0L);

            when(analyticsRepository.aggregateVillageSummary(villageId, seasonId, (short) 2026))
                    .thenReturn(mockProj);

            GeographicSummaryDto result = analyticsService.getGeographicSummary(
                    GeographicLevel.VILLAGE, villageId, seasonId, (short) 2026);

            assertThat(result).isNotNull();
            assertThat(result.level()).isEqualTo(GeographicLevel.VILLAGE);
            assertThat(result.name()).isEqualTo("Thopudurthi");
            assertThat(result.parentName()).isEqualTo("Atmakur");
            assertThat(result.seasonName()).isEqualTo("Kharif");
            assertThat(result.cultivationYear()).isEqualTo((short) 2026);
            assertThat(result.activeCultivatedAreaAcres()).isEqualByComparingTo("150.50");
            assertThat(result.activeCultivationCount()).isEqualTo(10L);
            assertThat(result.activeFarmerCount()).isEqualTo(5L);
            assertThat(result.activePlotCount()).isEqualTo(8L);
            // 150.50 / 5 = 30.10
            assertThat(result.averageAreaPerFarmerAcres()).isEqualByComparingTo("30.10");
            // 150.50 / 10 = 15.05
            assertThat(result.averageAreaPerCultivationAcres()).isEqualByComparingTo("15.05");
            assertThat(result.completedCultivatedAreaAcres()).isEqualByComparingTo("40.00");
            assertThat(result.completedCultivationCount()).isEqualTo(3L);
            assertThat(result.cancelledCultivationCount()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Zero cultivation data returns zero area and avoids division by zero")
        void testZeroCultivationData() {
            when(mandalRepository.findById(mandalId)).thenReturn(Optional.of(mandal));

            GeographicSummaryProjection mockProj = mock(GeographicSummaryProjection.class);
            when(mockProj.getActiveArea()).thenReturn(null);
            when(mockProj.getActiveCultivations()).thenReturn(null);
            when(mockProj.getActiveFarmers()).thenReturn(null);
            when(mockProj.getActivePlots()).thenReturn(null);
            when(mockProj.getCompletedArea()).thenReturn(null);
            when(mockProj.getCompletedCultivations()).thenReturn(null);
            when(mockProj.getCancelledCultivations()).thenReturn(null);
            when(mockProj.getAbandonedCultivations()).thenReturn(null);

            when(analyticsRepository.aggregateMandalSummary(mandalId, null, null))
                    .thenReturn(mockProj);

            GeographicSummaryDto result = analyticsService.getGeographicSummary(
                    GeographicLevel.MANDAL, mandalId, null, null);

            assertThat(result.activeCultivatedAreaAcres()).isEqualByComparingTo("0.00");
            assertThat(result.activeCultivationCount()).isEqualTo(0L);
            assertThat(result.activeFarmerCount()).isEqualTo(0L);
            assertThat(result.activePlotCount()).isEqualTo(0L);
            assertThat(result.averageAreaPerFarmerAcres()).isEqualByComparingTo("0.00");
            assertThat(result.averageAreaPerCultivationAcres()).isEqualByComparingTo("0.00");
            assertThat(result.completedCultivatedAreaAcres()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("Non-existent geography throws ResourceNotFoundException")
        void testNonExistentGeography() {
            UUID unknownId = UUID.randomUUID();
            when(districtRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> analyticsService.getGeographicSummary(GeographicLevel.DISTRICT, unknownId, null, null))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("District not found");
        }

        @Test
        @DisplayName("Invalid cultivation year throws BadRequestException")
        void testInvalidYear() {
            assertThatThrownBy(() -> analyticsService.getGeographicSummary(GeographicLevel.VILLAGE, villageId, null, (short) 1999))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("between 2000 and 2100");

            assertThatThrownBy(() -> analyticsService.getGeographicSummary(GeographicLevel.VILLAGE, villageId, null, (short) 2101))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("between 2000 and 2100");
        }
    }

    // =========================================================================
    // 2. CROP DISTRIBUTION TESTS
    // =========================================================================

    @Nested
    @DisplayName("Crop Distribution Analytics Tests")
    class CropDistributionTests {

        @Test
        @DisplayName("Computes percentage shares and ranks crops by area descending")
        void testCropDistributionRankingAndPercentage() {
            when(mandalRepository.findById(mandalId)).thenReturn(Optional.of(mandal));

            CropDistributionProjection p1 = mock(CropDistributionProjection.class);
            when(p1.getCropId()).thenReturn(crop1Id);
            when(p1.getCropName()).thenReturn("Groundnut");
            when(p1.getCropCategory()).thenReturn("Oilseeds");
            when(p1.getTotalArea()).thenReturn(new BigDecimal("75.00"));
            when(p1.getCultivationCount()).thenReturn(15L);
            when(p1.getFarmerCount()).thenReturn(12L);

            CropDistributionProjection p2 = mock(CropDistributionProjection.class);
            when(p2.getCropId()).thenReturn(crop2Id);
            when(p2.getCropName()).thenReturn("Cotton");
            when(p2.getCropCategory()).thenReturn("Commercial");
            when(p2.getTotalArea()).thenReturn(new BigDecimal("25.00"));
            when(p2.getCultivationCount()).thenReturn(5L);
            when(p2.getFarmerCount()).thenReturn(4L);

            when(analyticsRepository.findCropDistributionForMandal(mandalId, null, null))
                    .thenReturn(List.of(p1, p2));

            CropDistributionPageResponseDto response = analyticsService.getCropDistribution(
                    GeographicLevel.MANDAL, mandalId, null, null, 0, 20);

            assertThat(response).isNotNull();
            assertThat(response.totalActiveCultivatedAreaAcres()).isEqualByComparingTo("100.00");
            assertThat(response.totalActiveCultivationsCount()).isEqualTo(20L);
            assertThat(response.totalActiveFarmersCount()).isEqualTo(16L);
            assertThat(response.content()).hasSize(2);

            // Groundnut: 75.00 / 100.00 = 75.00%
            CropDistributionDto groundnut = response.content().get(0);
            assertThat(groundnut.cropName()).isEqualTo("Groundnut");
            assertThat(groundnut.activeCultivatedAreaAcres()).isEqualByComparingTo("75.00");
            assertThat(groundnut.percentageShare()).isEqualByComparingTo("75.00");

            // Cotton: 25.00 / 100.00 = 25.00%
            CropDistributionDto cotton = response.content().get(1);
            assertThat(cotton.cropName()).isEqualTo("Cotton");
            assertThat(cotton.activeCultivatedAreaAcres()).isEqualByComparingTo("25.00");
            assertThat(cotton.percentageShare()).isEqualByComparingTo("25.00");
        }

        @Test
        @DisplayName("Pagination bounds check and slicing")
        void testCropDistributionPagination() {
            when(villageRepository.findById(villageId)).thenReturn(Optional.of(village));

            CropDistributionProjection p1 = mock(CropDistributionProjection.class);
            when(p1.getCropId()).thenReturn(crop1Id);
            when(p1.getCropName()).thenReturn("Groundnut");
            when(p1.getTotalArea()).thenReturn(new BigDecimal("50.00"));

            CropDistributionProjection p2 = mock(CropDistributionProjection.class);
            when(p2.getCropId()).thenReturn(crop2Id);
            when(p2.getCropName()).thenReturn("Cotton");
            when(p2.getTotalArea()).thenReturn(new BigDecimal("30.00"));

            when(analyticsRepository.findCropDistributionForVillage(villageId, null, null))
                    .thenReturn(List.of(p1, p2));

            // Page 0 with size 1 -> contains only Groundnut
            CropDistributionPageResponseDto page0 = analyticsService.getCropDistribution(
                    GeographicLevel.VILLAGE, villageId, null, null, 0, 1);
            assertThat(page0.content()).hasSize(1);
            assertThat(page0.content().get(0).cropName()).isEqualTo("Groundnut");
            assertThat(page0.page()).isEqualTo(0);
            assertThat(page0.size()).isEqualTo(1);
            assertThat(page0.totalPages()).isEqualTo(2);

            // Page 1 with size 1 -> contains Cotton
            CropDistributionPageResponseDto page1 = analyticsService.getCropDistribution(
                    GeographicLevel.VILLAGE, villageId, null, null, 1, 1);
            assertThat(page1.content()).hasSize(1);
            assertThat(page1.content().get(0).cropName()).isEqualTo("Cotton");

            // Page 2 (out of bounds) -> empty list
            CropDistributionPageResponseDto page2 = analyticsService.getCropDistribution(
                    GeographicLevel.VILLAGE, villageId, null, null, 2, 1);
            assertThat(page2.content()).isEmpty();
        }

        @Test
        @DisplayName("Invalid pagination arguments throw BadRequestException")
        void testInvalidPaginationArgs() {
            assertThatThrownBy(() -> analyticsService.getCropDistribution(GeographicLevel.VILLAGE, villageId, null, null, -1, 20))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Page index must not be negative");

            assertThatThrownBy(() -> analyticsService.getCropDistribution(GeographicLevel.VILLAGE, villageId, null, null, 0, 0))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("between 1 and 100");

            assertThatThrownBy(() -> analyticsService.getCropDistribution(GeographicLevel.VILLAGE, villageId, null, null, 0, 101))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("between 1 and 100");
        }
    }

    // =========================================================================
    // 3. TEMPORAL TRENDS TESTS
    // =========================================================================

    @Nested
    @DisplayName("Temporal Trends Analytics Tests")
    class TemporalTrendsTests {

        @Test
        @DisplayName("Computes YoY area change percentage across consecutive years")
        void testTemporalTrendsYoYCalculation() {
            when(districtRepository.findById(districtId)).thenReturn(Optional.of(district));

            TemporalTrendProjection t2025 = mock(TemporalTrendProjection.class);
            when(t2025.getCultivationYear()).thenReturn((short) 2025);
            when(t2025.getSeasonId()).thenReturn(seasonId);
            when(t2025.getSeasonName()).thenReturn("Kharif");
            when(t2025.getActiveArea()).thenReturn(new BigDecimal("100.00"));
            when(t2025.getCompletedArea()).thenReturn(new BigDecimal("0.00"));
            when(t2025.getTotalCultivations()).thenReturn(20L);
            when(t2025.getTotalFarmers()).thenReturn(15L);

            TemporalTrendProjection t2026 = mock(TemporalTrendProjection.class);
            when(t2026.getCultivationYear()).thenReturn((short) 2026);
            when(t2026.getSeasonId()).thenReturn(seasonId);
            when(t2026.getSeasonName()).thenReturn("Kharif");
            when(t2026.getActiveArea()).thenReturn(new BigDecimal("125.00"));
            when(t2026.getCompletedArea()).thenReturn(new BigDecimal("0.00"));
            when(t2026.getTotalCultivations()).thenReturn(25L);
            when(t2026.getTotalFarmers()).thenReturn(18L);

            when(analyticsRepository.findTemporalTrendsForDistrict(districtId, null, (short) 2025, (short) 2026))
                    .thenReturn(List.of(t2025, t2026));

            List<TemporalTrendDto> trends = analyticsService.getTemporalTrends(
                    GeographicLevel.DISTRICT, districtId, null, (short) 2025, (short) 2026);

            assertThat(trends).hasSize(2);

            // 2025 has no prior year -> YoY is null
            assertThat(trends.get(0).cultivationYear()).isEqualTo((short) 2025);
            assertThat(trends.get(0).yearOverYearAreaChangePercentage()).isNull();

            // 2026 vs 2025: (125 - 100) / 100 = +25.00%
            assertThat(trends.get(1).cultivationYear()).isEqualTo((short) 2026);
            assertThat(trends.get(1).yearOverYearAreaChangePercentage()).isEqualByComparingTo("25.00");
        }

        @Test
        @DisplayName("startYear greater than endYear throws BadRequestException")
        void testInvalidYearRange() {
            assertThatThrownBy(() -> analyticsService.getTemporalTrends(
                    GeographicLevel.DISTRICT, districtId, null, (short) 2026, (short) 2025))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("startYear (2026) cannot be greater than endYear (2025)");
        }
    }

    // =========================================================================
    // 4. SIGNALS AND INSIGHTS TESTS
    // =========================================================================

    @Nested
    @DisplayName("Signals and Insights Generation Tests")
    class SignalsAndInsightsTests {

        @Test
        @DisplayName("Generates HIGH_CROP_CONCENTRATION when top crop exceeds 60%")
        void testHighConcentrationSignal() {
            when(mandalRepository.findById(mandalId)).thenReturn(Optional.of(mandal));

            // Summary mock
            GeographicSummaryProjection summaryProj = mock(GeographicSummaryProjection.class);
            when(summaryProj.getActiveArea()).thenReturn(new BigDecimal("100.00"));
            when(summaryProj.getActiveCultivations()).thenReturn(20L);
            when(summaryProj.getActiveFarmers()).thenReturn(15L);
            when(summaryProj.getActivePlots()).thenReturn(18L);
            when(analyticsRepository.aggregateMandalSummary(mandalId, null, null)).thenReturn(summaryProj);

            // Crop distribution mock
            CropDistributionProjection p1 = mock(CropDistributionProjection.class);
            when(p1.getCropId()).thenReturn(crop1Id);
            when(p1.getCropName()).thenReturn("Groundnut");
            when(p1.getCropCategory()).thenReturn("Oilseeds");
            when(p1.getTotalArea()).thenReturn(new BigDecimal("68.50"));
            when(p1.getCultivationCount()).thenReturn(14L);
            when(p1.getFarmerCount()).thenReturn(10L);

            when(analyticsRepository.findCropDistributionForMandal(mandalId, null, null))
                    .thenReturn(List.of(p1));

            SignalsAndInsightsResponseDto response = analyticsService.getSignalsAndInsights(
                    GeographicLevel.MANDAL, mandalId, null, null);

            assertThat(response).isNotNull();
            assertThat(response.signals()).isNotEmpty();

            // High concentration signal
            Optional<CultivationSignalDto> concentrationSignal = response.signals().stream()
                    .filter(s -> "HIGH_CROP_CONCENTRATION".equals(s.signalKey()))
                    .findFirst();
            assertThat(concentrationSignal).isPresent();
            assertThat(concentrationSignal.get().signalType()).isEqualTo("OBSERVATION");
            assertThat(concentrationSignal.get().description()).contains("Groundnut");

            // Verify insight does NOT contain prescriptive words
            assertThat(response.insights()).isNotEmpty();
            for (AnalyticsInsightDto insight : response.insights()) {
                assertThat(insight.factualNarrative())
                        .doesNotContain("Do not grow")
                        .doesNotContain("You should grow")
                        .doesNotContain("Grow ");
            }
        }

        @Test
        @DisplayName("Generates NO_ACTIVE_CULTIVATION_DATA signal when area is zero")
        void testNoDataSignal() {
            when(mandalRepository.findById(mandalId)).thenReturn(Optional.of(mandal));

            GeographicSummaryProjection summaryProj = mock(GeographicSummaryProjection.class);
            when(summaryProj.getActiveArea()).thenReturn(BigDecimal.ZERO);
            when(analyticsRepository.aggregateMandalSummary(mandalId, null, null)).thenReturn(summaryProj);
            when(analyticsRepository.findCropDistributionForMandal(mandalId, null, null)).thenReturn(Collections.emptyList());

            SignalsAndInsightsResponseDto response = analyticsService.getSignalsAndInsights(
                    GeographicLevel.MANDAL, mandalId, null, null);

            assertThat(response.signals()).hasSize(1);
            assertThat(response.signals().get(0).signalKey()).isEqualTo("NO_ACTIVE_CULTIVATION_DATA");
            assertThat(response.insights().get(0).summary()).contains("No Active Cultivation Intentions");
        }
    }

    // =========================================================================
    // 5. FARMER PERSONAL ANALYTICS TESTS
    // =========================================================================

    @Nested
    @DisplayName("Farmer Personal Analytics Summary Tests")
    class FarmerPersonalAnalyticsTests {

        @Test
        @DisplayName("Computes farmer capacity utilization rate and crop breakdowns accurately")
        void testFarmerSummarySuccess() {
            when(securityUtils.getAuthenticatedUserId()).thenReturn(farmerId);
            when(farmerProfileRepository.findById(farmerId)).thenReturn(Optional.of(farmerProfile));
            when(seasonRepository.findById(seasonId)).thenReturn(Optional.of(season));

            // Plot capacity
            FarmerPlotSummaryProjection plotProj = mock(FarmerPlotSummaryProjection.class);
            when(plotProj.getTotalPlots()).thenReturn(2L);
            when(plotProj.getTotalLandCapacity()).thenReturn(new BigDecimal("10.00"));
            when(analyticsRepository.aggregateFarmerPlots(farmerId)).thenReturn(plotProj);

            // Cultivation summary
            FarmerCultivationSummaryProjection cultProj = mock(FarmerCultivationSummaryProjection.class);
            when(cultProj.getActiveArea()).thenReturn(new BigDecimal("6.00"));
            when(cultProj.getCompletedArea()).thenReturn(new BigDecimal("0.00"));
            when(cultProj.getActiveCount()).thenReturn(2L);
            when(cultProj.getCompletedCount()).thenReturn(0L);
            when(cultProj.getCancelledCount()).thenReturn(0L);
            when(cultProj.getAbandonedCount()).thenReturn(0L);
            when(analyticsRepository.aggregateFarmerCultivation(farmerId, seasonId, (short) 2026))
                    .thenReturn(cultProj);

            // Crop breakdown
            FarmerCropBreakdownProjection cb1 = mock(FarmerCropBreakdownProjection.class);
            when(cb1.getCropId()).thenReturn(crop1Id);
            when(cb1.getCropName()).thenReturn("Cotton");
            when(cb1.getCropCategory()).thenReturn("Commercial");
            when(cb1.getActiveArea()).thenReturn(new BigDecimal("6.00"));
            when(cb1.getCultivationCyclesCount()).thenReturn(2L);

            when(analyticsRepository.findFarmerCropBreakdown(farmerId, seasonId, (short) 2026))
                    .thenReturn(List.of(cb1));

            FarmerAnalyticsSummaryDto summary = analyticsService.getFarmerSummary(seasonId, (short) 2026);

            assertThat(summary).isNotNull();
            assertThat(summary.farmerId()).isEqualTo(farmerId);
            assertThat(summary.seasonName()).isEqualTo("Kharif");
            assertThat(summary.totalPlotsCount()).isEqualTo(2L);
            assertThat(summary.totalPlotLandCapacityAcres()).isEqualByComparingTo("10.00");
            assertThat(summary.activeCultivatedAreaAcres()).isEqualByComparingTo("6.00");
            // Utilization rate: (6.00 / 10.00) * 100 = 60.00%
            assertThat(summary.landCapacityUtilizationPercentage()).isEqualByComparingTo("60.00");
            assertThat(summary.cropBreakdown()).hasSize(1);
            assertThat(summary.cropBreakdown().get(0).cropName()).isEqualTo("Cotton");
            assertThat(summary.cropBreakdown().get(0).percentageOfFarmerActiveArea()).isEqualByComparingTo("100.00");
        }

        @Test
        @DisplayName("Deleted farmer profile throws ResourceNotFoundException")
        void testDeletedFarmerThrowsNotFound() {
            when(securityUtils.getAuthenticatedUserId()).thenReturn(farmerId);
            FarmerProfile deletedProfile = new FarmerProfile();
            deletedProfile.setId(farmerId);
            deletedProfile.setProfileStatus(FarmerProfile.STATUS_DELETED);

            when(farmerProfileRepository.findById(farmerId)).thenReturn(Optional.of(deletedProfile));

            assertThatThrownBy(() -> analyticsService.getFarmerSummary(null, null))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Farmer profile is deleted");
        }
    }
}
