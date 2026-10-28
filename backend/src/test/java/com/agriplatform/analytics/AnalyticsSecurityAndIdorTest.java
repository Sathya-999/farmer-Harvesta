package com.agriplatform.analytics;

import com.agriplatform.analytics.controller.AnalyticsController;
import com.agriplatform.analytics.dto.FarmerAnalyticsSummaryDto;
import com.agriplatform.analytics.dto.GeographicLevel;
import com.agriplatform.analytics.dto.GeographicSummaryDto;
import com.agriplatform.analytics.service.AnalyticsService;
import com.agriplatform.common.config.SecurityConfig;
import com.agriplatform.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
@DisplayName("Task 10 — Analytics Security and IDOR Protection Tests")
class AnalyticsSecurityAndIdorTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    private static final String BASE_URL = "/api/v1/analytics";

    @Test
    @DisplayName("Farmer cannot supply farmerId parameter to override JWT identity (Strict IDOR)")
    void testFarmerCannotOverrideIdentity() throws Exception {
        UUID authenticatedFarmerId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID victimFarmerId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        FarmerAnalyticsSummaryDto authenticatedSummary = new FarmerAnalyticsSummaryDto(
                authenticatedFarmerId, null, null, null, 1L,
                new BigDecimal("5.00"), new BigDecimal("3.00"), new BigDecimal("0.00"),
                1L, 0L, 0L, 0L, new BigDecimal("60.00"), Collections.emptyList());

        when(analyticsService.getFarmerSummary(any(), any())).thenReturn(authenticatedSummary);

        // Client attempts to pass farmerId=victimFarmerId in query string
        mockMvc.perform(get(BASE_URL + "/farmer/summary")
                        .param("farmerId", victimFarmerId.toString())
                        .with(jwt().jwt(j -> j.subject(authenticatedFarmerId.toString()).claim("aud", List.of("authenticated")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.farmerId").value(authenticatedFarmerId.toString()));

        // Service is invoked without victim ID
        verify(analyticsService).getFarmerSummary(null, null);
    }

    @Test
    @DisplayName("Aggregate geographic endpoints leak zero PII in JSON payload")
    void testZeroPiiInAggregateEndpoints() throws Exception {
        UUID villageId = UUID.randomUUID();
        GeographicSummaryDto dto = new GeographicSummaryDto(
                GeographicLevel.VILLAGE, villageId, "Thopudurthi", "THOP", "Atmakur",
                null, null, (short) 2026, new BigDecimal("100.00"),
                10L, 8L, 9L, new BigDecimal("12.50"), new BigDecimal("10.00"),
                BigDecimal.ZERO, 0L, 0L, 0L);

        when(analyticsService.getGeographicSummary(eq(GeographicLevel.VILLAGE), eq(villageId), any(), any()))
                .thenReturn(dto);

        MvcResult result = mockMvc.perform(get(BASE_URL + "/geography/summary")
                        .param("level", "VILLAGE")
                        .param("id", villageId.toString())
                        .with(jwt().jwt(j -> j.subject(UUID.randomUUID().toString()).claim("aud", List.of("authenticated")))))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();

        // Must not contain any personal identifier keys
        assertThat(responseBody)
                .doesNotContain("\"fullName\"")
                .doesNotContain("\"phone\"")
                .doesNotContain("\"mobile\"")
                .doesNotContain("\"farmerId\"")
                .doesNotContain("\"farmerName\"")
                .doesNotContain("\"email\"");
    }
}
