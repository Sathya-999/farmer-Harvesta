package com.agriplatform.analytics;

import com.agriplatform.analytics.dto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Task 10 — Analytics DTOs and Enums Unit Tests")
class AnalyticsDtoTest {

    @Nested
    @DisplayName("GeographicLevel Enum Tests")
    class GeographicLevelTests {

        @Test
        @DisplayName("fromString parses case-insensitive valid level names")
        void testValidLevelNames() {
            assertThat(GeographicLevel.fromString("village")).isEqualTo(GeographicLevel.VILLAGE);
            assertThat(GeographicLevel.fromString("MANDAL")).isEqualTo(GeographicLevel.MANDAL);
            assertThat(GeographicLevel.fromString("District")).isEqualTo(GeographicLevel.DISTRICT);
            assertThat(GeographicLevel.fromString(" state ")).isEqualTo(GeographicLevel.STATE);
        }

        @Test
        @DisplayName("fromString throws on invalid, blank, or null level")
        void testInvalidLevelNames() {
            assertThatThrownBy(() -> GeographicLevel.fromString(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not be null or blank");

            assertThatThrownBy(() -> GeographicLevel.fromString("  "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not be null or blank");

            assertThatThrownBy(() -> GeographicLevel.fromString("COUNTRY"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid geographic level");
        }
    }

    @Nested
    @DisplayName("DTO Records Construction & Immutability")
    class DtoRecordTests {

        @Test
        @DisplayName("GeographicSummaryDto record integrity")
        void testGeographicSummaryDto() {
            UUID id = UUID.randomUUID();
            UUID seasonId = UUID.randomUUID();
            GeographicSummaryDto dto = new GeographicSummaryDto(
                    GeographicLevel.MANDAL,
                    id,
                    "Atmakur",
                    "ATM-01",
                    "Anantapur",
                    seasonId,
                    "Kharif",
                    (short) 2026,
                    new BigDecimal("1250.50"),
                    45L,
                    30L,
                    35L,
                    new BigDecimal("41.68"),
                    new BigDecimal("27.79"),
                    new BigDecimal("300.00"),
                    10L,
                    2L,
                    1L
            );

            assertThat(dto.level()).isEqualTo(GeographicLevel.MANDAL);
            assertThat(dto.id()).isEqualTo(id);
            assertThat(dto.name()).isEqualTo("Atmakur");
            assertThat(dto.code()).isEqualTo("ATM-01");
            assertThat(dto.parentName()).isEqualTo("Anantapur");
            assertThat(dto.seasonId()).isEqualTo(seasonId);
            assertThat(dto.seasonName()).isEqualTo("Kharif");
            assertThat(dto.cultivationYear()).isEqualTo((short) 2026);
            assertThat(dto.activeCultivatedAreaAcres()).isEqualByComparingTo("1250.50");
            assertThat(dto.activeCultivationCount()).isEqualTo(45L);
            assertThat(dto.activeFarmerCount()).isEqualTo(30L);
            assertThat(dto.activePlotCount()).isEqualTo(35L);
            assertThat(dto.averageAreaPerFarmerAcres()).isEqualByComparingTo("41.68");
            assertThat(dto.averageAreaPerCultivationAcres()).isEqualByComparingTo("27.79");
            assertThat(dto.completedCultivatedAreaAcres()).isEqualByComparingTo("300.00");
            assertThat(dto.completedCultivationCount()).isEqualTo(10L);
            assertThat(dto.cancelledCultivationCount()).isEqualTo(2L);
            assertThat(dto.abandonedCultivationCount()).isEqualTo(1L);
        }

        @Test
        @DisplayName("CropDistributionDto record integrity")
        void testCropDistributionDto() {
            UUID cropId = UUID.randomUUID();
            CropDistributionDto dto = new CropDistributionDto(
                    cropId,
                    "Groundnut",
                    "Oilseeds",
                    new BigDecimal("800.00"),
                    30L,
                    25L,
                    new BigDecimal("64.00")
            );

            assertThat(dto.cropId()).isEqualTo(cropId);
            assertThat(dto.cropName()).isEqualTo("Groundnut");
            assertThat(dto.cropCategory()).isEqualTo("Oilseeds");
            assertThat(dto.activeCultivatedAreaAcres()).isEqualByComparingTo("800.00");
            assertThat(dto.cultivationCount()).isEqualTo(30L);
            assertThat(dto.farmerCount()).isEqualTo(25L);
            assertThat(dto.percentageShare()).isEqualByComparingTo("64.00");
        }

        @Test
        @DisplayName("TemporalTrendDto record integrity")
        void testTemporalTrendDto() {
            UUID seasonId = UUID.randomUUID();
            TemporalTrendDto dto = new TemporalTrendDto(
                    (short) 2026,
                    seasonId,
                    "Kharif",
                    new BigDecimal("1000.00"),
                    new BigDecimal("200.00"),
                    40L,
                    28L,
                    new BigDecimal("15.50")
            );

            assertThat(dto.cultivationYear()).isEqualTo((short) 2026);
            assertThat(dto.seasonId()).isEqualTo(seasonId);
            assertThat(dto.seasonName()).isEqualTo("Kharif");
            assertThat(dto.activeAreaAcres()).isEqualByComparingTo("1000.00");
            assertThat(dto.completedAreaAcres()).isEqualByComparingTo("200.00");
            assertThat(dto.totalCultivationsCount()).isEqualTo(40L);
            assertThat(dto.totalFarmersCount()).isEqualTo(28L);
            assertThat(dto.yearOverYearAreaChangePercentage()).isEqualByComparingTo("15.50");
        }

        @Test
        @DisplayName("CultivationSignalDto and AnalyticsInsightDto integrity")
        void testSignalsAndInsightsDto() {
            CultivationSignalDto signal = new CultivationSignalDto(
                    "HIGH_CROP_CONCENTRATION",
                    "OBSERVATION",
                    "High Crop Concentration",
                    "Single crop represents 68.5% of area",
                    Map.of("leadingCrop", "Groundnut", "share", 68.5)
            );

            assertThat(signal.signalKey()).isEqualTo("HIGH_CROP_CONCENTRATION");
            assertThat(signal.signalType()).isEqualTo("OBSERVATION");
            assertThat(signal.title()).isEqualTo("High Crop Concentration");
            assertThat(signal.description()).contains("68.5%");
            assertThat(signal.metricBasis()).containsEntry("leadingCrop", "Groundnut");

            AnalyticsInsightDto insight = new AnalyticsInsightDto(
                    "CROP_ALLOCATION",
                    "Dominant Primary Crop",
                    "Groundnut represents 68.5% of total acreage."
            );

            assertThat(insight.category()).isEqualTo("CROP_ALLOCATION");
            assertThat(insight.summary()).isEqualTo("Dominant Primary Crop");
            assertThat(insight.factualNarrative()).contains("Groundnut represents");
        }

        @Test
        @DisplayName("FarmerAnalyticsSummaryDto record integrity")
        void testFarmerAnalyticsSummaryDto() {
            UUID farmerId = UUID.randomUUID();
            UUID seasonId = UUID.randomUUID();
            UUID cropId = UUID.randomUUID();

            FarmerCropBreakdownDto cropBreakdown = new FarmerCropBreakdownDto(
                    cropId,
                    "Cotton",
                    "Commercial",
                    new BigDecimal("5.00"),
                    1L,
                    new BigDecimal("100.00")
            );

            FarmerAnalyticsSummaryDto dto = new FarmerAnalyticsSummaryDto(
                    farmerId,
                    seasonId,
                    "Kharif",
                    (short) 2026,
                    2L,
                    new BigDecimal("10.00"),
                    new BigDecimal("5.00"),
                    new BigDecimal("0.00"),
                    1L,
                    0L,
                    0L,
                    0L,
                    new BigDecimal("50.00"),
                    List.of(cropBreakdown)
            );

            assertThat(dto.farmerId()).isEqualTo(farmerId);
            assertThat(dto.seasonName()).isEqualTo("Kharif");
            assertThat(dto.totalPlotsCount()).isEqualTo(2L);
            assertThat(dto.totalPlotLandCapacityAcres()).isEqualByComparingTo("10.00");
            assertThat(dto.activeCultivatedAreaAcres()).isEqualByComparingTo("5.00");
            assertThat(dto.landCapacityUtilizationPercentage()).isEqualByComparingTo("50.00");
            assertThat(dto.cropBreakdown()).hasSize(1);
            assertThat(dto.cropBreakdown().get(0).cropName()).isEqualTo("Cotton");
        }
    }
}
