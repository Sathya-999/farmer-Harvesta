package com.agriplatform.analytics;

import com.agriplatform.analytics.dto.*;
import com.agriplatform.analytics.repository.AnalyticsRepository;
import com.agriplatform.analytics.repository.CropDistributionProjection;
import com.agriplatform.analytics.repository.GeographicSummaryProjection;
import com.agriplatform.analytics.service.AnalyticsServiceImpl;
import com.agriplatform.common.util.SecurityUtils;
import com.agriplatform.cultivation.entity.Season;
import com.agriplatform.cultivation.repository.CropRepository;
import com.agriplatform.cultivation.repository.SeasonRepository;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Task 10 — Aggregation Integrity and Status Semantics Tests")
class AnalyticsAggregationIntegrityTest {

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

    private UUID mandalId;
    private Mandal mandal;

    @BeforeEach
    void setUp() {
        mandalId = UUID.randomUUID();
        State state = new State();
        state.setName("Andhra Pradesh");

        District district = new District();
        district.setName("Anantapur");
        district.setState(state);

        mandal = new Mandal();
        mandal.setId(mandalId);
        mandal.setName("Atmakur");
        mandal.setDistrict(district);
    }

    @Test
    @DisplayName("Active cultivation area strictly excludes COMPLETED, CANCELLED, and ABANDONED")
    void testActiveAreaExcludesTerminalStatuses() {
        when(mandalRepository.findById(mandalId)).thenReturn(Optional.of(mandal));

        GeographicSummaryProjection mockProj = mock(GeographicSummaryProjection.class);
        // PLANNED (10.00) + STARTED (15.50) = 25.50 active area
        when(mockProj.getActiveArea()).thenReturn(new BigDecimal("25.50"));
        when(mockProj.getActiveCultivations()).thenReturn(2L);
        when(mockProj.getActiveFarmers()).thenReturn(2L);
        when(mockProj.getActivePlots()).thenReturn(2L);

        // COMPLETED = 50.00
        when(mockProj.getCompletedArea()).thenReturn(new BigDecimal("50.00"));
        when(mockProj.getCompletedCultivations()).thenReturn(4L);
        when(mockProj.getCancelledCultivations()).thenReturn(1L);
        when(mockProj.getAbandonedCultivations()).thenReturn(1L);

        when(analyticsRepository.aggregateMandalSummary(mandalId, null, null)).thenReturn(mockProj);

        GeographicSummaryDto summary = analyticsService.getGeographicSummary(
                GeographicLevel.MANDAL, mandalId, null, null);

        // Active area must NOT include completed, cancelled, or abandoned
        assertThat(summary.activeCultivatedAreaAcres()).isEqualByComparingTo("25.50");
        assertThat(summary.activeCultivationCount()).isEqualTo(2L);
        assertThat(summary.completedCultivatedAreaAcres()).isEqualByComparingTo("50.00");
        assertThat(summary.completedCultivationCount()).isEqualTo(4L);
        assertThat(summary.cancelledCultivationCount()).isEqualTo(1L);
        assertThat(summary.abandonedCultivationCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Exact decimal precision preserved across fractional acres")
    void testFractionalAcreagePrecision() {
        when(mandalRepository.findById(mandalId)).thenReturn(Optional.of(mandal));

        // 3 plots: 2.75 acres + 0.33 acres + 1.25 acres = 4.33 acres
        GeographicSummaryProjection mockProj = mock(GeographicSummaryProjection.class);
        when(mockProj.getActiveArea()).thenReturn(new BigDecimal("4.33"));
        when(mockProj.getActiveCultivations()).thenReturn(3L);
        when(mockProj.getActiveFarmers()).thenReturn(2L);
        when(mockProj.getActivePlots()).thenReturn(3L);

        when(analyticsRepository.aggregateMandalSummary(mandalId, null, null)).thenReturn(mockProj);

        GeographicSummaryDto summary = analyticsService.getGeographicSummary(
                GeographicLevel.MANDAL, mandalId, null, null);

        assertThat(summary.activeCultivatedAreaAcres()).isEqualByComparingTo("4.33");
        // 4.33 / 2 = 2.165 -> rounded to 2.17
        assertThat(summary.averageAreaPerFarmerAcres()).isEqualByComparingTo("2.17");
        // 4.33 / 3 = 1.443 -> rounded to 1.44
        assertThat(summary.averageAreaPerCultivationAcres()).isEqualByComparingTo("1.44");
    }

    @Test
    @DisplayName("Empty geography returns zero summary and empty crop lists safely")
    void testEmptyGeographyAggregation() {
        when(mandalRepository.findById(mandalId)).thenReturn(Optional.of(mandal));

        GeographicSummaryProjection mockProj = mock(GeographicSummaryProjection.class);
        when(mockProj.getActiveArea()).thenReturn(BigDecimal.ZERO);
        when(mockProj.getActiveCultivations()).thenReturn(0L);
        when(mockProj.getActiveFarmers()).thenReturn(0L);
        when(mockProj.getActivePlots()).thenReturn(0L);
        when(mockProj.getCompletedArea()).thenReturn(BigDecimal.ZERO);
        when(mockProj.getCompletedCultivations()).thenReturn(0L);
        when(mockProj.getCancelledCultivations()).thenReturn(0L);
        when(mockProj.getAbandonedCultivations()).thenReturn(0L);

        when(analyticsRepository.aggregateMandalSummary(mandalId, null, null)).thenReturn(mockProj);
        when(analyticsRepository.findCropDistributionForMandal(mandalId, null, null)).thenReturn(Collections.emptyList());

        GeographicSummaryDto summary = analyticsService.getGeographicSummary(
                GeographicLevel.MANDAL, mandalId, null, null);
        assertThat(summary.activeCultivatedAreaAcres()).isEqualByComparingTo("0.00");
        assertThat(summary.activeCultivationCount()).isEqualTo(0L);
        assertThat(summary.averageAreaPerFarmerAcres()).isEqualByComparingTo("0.00");

        CropDistributionPageResponseDto cropDist = analyticsService.getCropDistribution(
                GeographicLevel.MANDAL, mandalId, null, null, 0, 20);
        assertThat(cropDist.totalActiveCultivatedAreaAcres()).isEqualByComparingTo("0.00");
        assertThat(cropDist.content()).isEmpty();
    }
}
