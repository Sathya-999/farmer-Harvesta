package com.agriplatform.analytics;

import com.agriplatform.analytics.controller.AnalyticsController;
import com.agriplatform.analytics.dto.*;
import com.agriplatform.analytics.service.AnalyticsService;
import com.agriplatform.common.config.SecurityConfig;
import com.agriplatform.common.exception.GlobalExceptionHandler;
import com.agriplatform.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = {AnalyticsController.class},
    excludeAutoConfiguration = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
    }
)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
@DisplayName("Task 10 — AnalyticsController WebMvc Tests")
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    private static final String BASE_URL = "/api/v1/analytics";
    private static final UUID TEST_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    // =========================================================================
    // 1. AUTHENTICATION GATES
    // =========================================================================

    @Nested
    @DisplayName("Authentication Enforcement")
    class AuthenticationTests {

        @Test
        @DisplayName("All endpoints reject unauthenticated requests with 401")
        void testUnauthenticatedRequests() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(get(BASE_URL + "/geography/summary").param("level", "MANDAL").param("id", id.toString()))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(get(BASE_URL + "/geography/crops").param("level", "MANDAL").param("id", id.toString()))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(get(BASE_URL + "/geography/trends").param("level", "MANDAL").param("id", id.toString()))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(get(BASE_URL + "/geography/signals").param("level", "MANDAL").param("id", id.toString()))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(get(BASE_URL + "/farmer/summary"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // =========================================================================
    // 2. GEOGRAPHIC SUMMARY ENDPOINT
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/analytics/geography/summary")
    class GeographySummaryEndpointTests {

        @Test
        @DisplayName("Returns 200 OK with GeographicSummaryDto payload")
        void testGetSummarySuccess() throws Exception {
            UUID mandalId = UUID.randomUUID();
            GeographicSummaryDto dto = new GeographicSummaryDto(
                    GeographicLevel.MANDAL,
                    mandalId,
                    "Atmakur",
                    "ATM",
                    "Anantapur",
                    null,
                    null,
                    (short) 2026,
                    new BigDecimal("1500.00"),
                    50L,
                    35L,
                    40L,
                    new BigDecimal("42.86"),
                    new BigDecimal("30.00"),
                    new BigDecimal("200.00"),
                    8L,
                    1L,
                    0L
            );

            when(analyticsService.getGeographicSummary(eq(GeographicLevel.MANDAL), eq(mandalId), any(), eq((short) 2026)))
                    .thenReturn(dto);

            mockMvc.perform(get(BASE_URL + "/geography/summary")
                            .param("level", "MANDAL")
                            .param("id", mandalId.toString())
                            .param("cultivationYear", "2026")
                            .with(jwt().jwt(j -> j.subject(TEST_USER_ID.toString()).claim("aud", List.of("authenticated")))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.name").value("Atmakur"))
                    .andExpect(jsonPath("$.data.parentName").value("Anantapur"))
                    .andExpect(jsonPath("$.data.activeCultivatedAreaAcres").value(1500.00))
                    .andExpect(jsonPath("$.data.activeCultivationCount").value(50))
                    .andExpect(jsonPath("$.data.activeFarmerCount").value(35))
                    .andExpect(jsonPath("$.data.averageAreaPerFarmerAcres").value(42.86));
        }

        @Test
        @DisplayName("Returns 404 when geography does not exist")
        void testGetSummaryNotFound() throws Exception {
            UUID unknownId = UUID.randomUUID();
            when(analyticsService.getGeographicSummary(any(), eq(unknownId), any(), any()))
                    .thenThrow(new ResourceNotFoundException("Mandal not found: " + unknownId));

            mockMvc.perform(get(BASE_URL + "/geography/summary")
                            .param("level", "MANDAL")
                            .param("id", unknownId.toString())
                            .with(jwt().jwt(j -> j.subject(TEST_USER_ID.toString()).claim("aud", List.of("authenticated")))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error").value(containsString("Mandal not found")));
        }

        @Test
        @DisplayName("Returns 400 when level is invalid")
        void testGetSummaryInvalidLevel() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(get(BASE_URL + "/geography/summary")
                            .param("level", "REGION")
                            .param("id", id.toString())
                            .with(jwt().jwt(j -> j.subject(TEST_USER_ID.toString()).claim("aud", List.of("authenticated")))))
                    .andExpect(status().isBadRequest());
        }
    }

    // =========================================================================
    // 3. CROP DISTRIBUTION ENDPOINT
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/analytics/geography/crops")
    class CropDistributionEndpointTests {

        @Test
        @DisplayName("Returns 200 OK with paginated crop distribution")
        void testGetCropDistributionSuccess() throws Exception {
            UUID mandalId = UUID.randomUUID();
            UUID cropId = UUID.randomUUID();

            CropDistributionDto cropDto = new CropDistributionDto(
                    cropId, "Groundnut", "Oilseeds", new BigDecimal("500.00"), 20L, 15L, new BigDecimal("62.50"));

            CropDistributionPageResponseDto pageResponse = new CropDistributionPageResponseDto(
                    GeographicLevel.MANDAL, mandalId, "Atmakur", new BigDecimal("800.00"), 35L, 25L,
                    List.of(cropDto), 0, 20, 1, 1);

            when(analyticsService.getCropDistribution(eq(GeographicLevel.MANDAL), eq(mandalId), any(), any(), eq(0), eq(20)))
                    .thenReturn(pageResponse);

            mockMvc.perform(get(BASE_URL + "/geography/crops")
                            .param("level", "MANDAL")
                            .param("id", mandalId.toString())
                            .param("page", "0")
                            .param("size", "20")
                            .with(jwt().jwt(j -> j.subject(TEST_USER_ID.toString()).claim("aud", List.of("authenticated")))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.geographyName").value("Atmakur"))
                    .andExpect(jsonPath("$.data.totalActiveCultivatedAreaAcres").value(800.00))
                    .andExpect(jsonPath("$.data.content[0].cropName").value("Groundnut"))
                    .andExpect(jsonPath("$.data.content[0].percentageShare").value(62.50));
        }
    }

    // =========================================================================
    // 4. TEMPORAL TRENDS ENDPOINT
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/analytics/geography/trends")
    class TemporalTrendsEndpointTests {

        @Test
        @DisplayName("Returns 200 OK with chronological trends")
        void testGetTemporalTrendsSuccess() throws Exception {
            UUID districtId = UUID.randomUUID();
            UUID seasonId = UUID.randomUUID();

            TemporalTrendDto trend = new TemporalTrendDto(
                    (short) 2026, seasonId, "Kharif", new BigDecimal("1200.00"), new BigDecimal("0.00"),
                    35L, 28L, new BigDecimal("20.00"));

            when(analyticsService.getTemporalTrends(eq(GeographicLevel.DISTRICT), eq(districtId), any(), any(), any()))
                    .thenReturn(List.of(trend));

            mockMvc.perform(get(BASE_URL + "/geography/trends")
                            .param("level", "DISTRICT")
                            .param("id", districtId.toString())
                            .with(jwt().jwt(j -> j.subject(TEST_USER_ID.toString()).claim("aud", List.of("authenticated")))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].cultivationYear").value(2026))
                    .andExpect(jsonPath("$.data[0].yearOverYearAreaChangePercentage").value(20.00));
        }
    }

    // =========================================================================
    // 5. SIGNALS AND INSIGHTS ENDPOINT
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/analytics/geography/signals")
    class SignalsAndInsightsEndpointTests {

        @Test
        @DisplayName("Returns 200 OK with verified signals and explainable insights")
        void testGetSignalsAndInsightsSuccess() throws Exception {
            UUID villageId = UUID.randomUUID();
            CultivationSignalDto signal = new CultivationSignalDto(
                    "HIGH_CROP_CONCENTRATION", "OBSERVATION", "High Crop Concentration",
                    "Single crop constitutes 65% of area", Map.of("leadingCrop", "Groundnut"));

            AnalyticsInsightDto insight = new AnalyticsInsightDto(
                    "CROP_ALLOCATION", "Dominant Crop", "Groundnut represents 65% of acreage.");

            SignalsAndInsightsResponseDto response = new SignalsAndInsightsResponseDto(
                    GeographicLevel.VILLAGE, villageId, "Thopudurthi", null, null,
                    List.of(signal), List.of(insight));

            when(analyticsService.getSignalsAndInsights(eq(GeographicLevel.VILLAGE), eq(villageId), any(), any()))
                    .thenReturn(response);

            mockMvc.perform(get(BASE_URL + "/geography/signals")
                            .param("level", "VILLAGE")
                            .param("id", villageId.toString())
                            .with(jwt().jwt(j -> j.subject(TEST_USER_ID.toString()).claim("aud", List.of("authenticated")))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.signals[0].signalKey").value("HIGH_CROP_CONCENTRATION"))
                    .andExpect(jsonPath("$.data.insights[0].category").value("CROP_ALLOCATION"));
        }
    }

    // =========================================================================
    // 6. FARMER PERSONAL SUMMARY ENDPOINT
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/analytics/farmer/summary")
    class FarmerSummaryEndpointTests {

        @Test
        @DisplayName("Returns 200 OK with calling farmer's personal analytics")
        void testGetFarmerSummarySuccess() throws Exception {
            FarmerAnalyticsSummaryDto summary = new FarmerAnalyticsSummaryDto(
                    TEST_USER_ID, null, null, (short) 2026, 2L,
                    new BigDecimal("10.00"), new BigDecimal("6.00"), new BigDecimal("0.00"),
                    2L, 0L, 0L, 0L, new BigDecimal("60.00"), Collections.emptyList());

            when(analyticsService.getFarmerSummary(any(), eq((short) 2026))).thenReturn(summary);

            mockMvc.perform(get(BASE_URL + "/farmer/summary")
                            .param("cultivationYear", "2026")
                            .with(jwt().jwt(j -> j.subject(TEST_USER_ID.toString()).claim("aud", List.of("authenticated")))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.farmerId").value(TEST_USER_ID.toString()))
                    .andExpect(jsonPath("$.data.totalPlotLandCapacityAcres").value(10.00))
                    .andExpect(jsonPath("$.data.activeCultivatedAreaAcres").value(6.00))
                    .andExpect(jsonPath("$.data.landCapacityUtilizationPercentage").value(60.00));
        }
    }

    // =========================================================================
    // 7. IMMUTABILITY / METHOD RESTRICTIONS
    // =========================================================================

    @Nested
    @DisplayName("HTTP Method Restrictions")
    class HttpMethodTests {

        @Test
        @DisplayName("Mutating methods (POST, PUT, DELETE, PATCH) return 405 Method Not Allowed")
        void testMutatingMethodsNotAllowed() throws Exception {
            mockMvc.perform(post(BASE_URL + "/geography/summary")
                            .with(jwt().jwt(j -> j.subject(TEST_USER_ID.toString()).claim("aud", List.of("authenticated")))))
                    .andExpect(status().isMethodNotAllowed());

            mockMvc.perform(put(BASE_URL + "/geography/summary")
                            .with(jwt().jwt(j -> j.subject(TEST_USER_ID.toString()).claim("aud", List.of("authenticated")))))
                    .andExpect(status().isMethodNotAllowed());

            mockMvc.perform(delete(BASE_URL + "/geography/summary")
                            .with(jwt().jwt(j -> j.subject(TEST_USER_ID.toString()).claim("aud", List.of("authenticated")))))
                    .andExpect(status().isMethodNotAllowed());
        }
    }
}
