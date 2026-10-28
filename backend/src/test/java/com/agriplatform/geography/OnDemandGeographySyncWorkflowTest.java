package com.agriplatform.geography;

import com.agriplatform.common.config.SecurityConfig;
import com.agriplatform.common.exception.GlobalExceptionHandler;
import com.agriplatform.geography.controller.GeographyController;
import com.agriplatform.geography.dto.LocationResolutionDto;
import com.agriplatform.geography.dto.StateCoverageDto;
import com.agriplatform.geography.dto.StateSyncStatusDto;
import com.agriplatform.geography.service.GeographyLibraryIntegrationService;
import com.agriplatform.geography.service.GeographyLocationService;
import com.agriplatform.geography.service.GeographyService;
import com.agriplatform.geography.service.OnDemandGeographySyncService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = GeographyController.class,
    excludeAutoConfiguration = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
    }
)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
@DisplayName("Phase C & E — GPS Location Resolution, Coverage Detection, and On-Demand Synchronization")
class OnDemandGeographySyncWorkflowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GeographyService geographyService;

    @MockBean
    private GeographyLocationService locationService;

    @MockBean
    private GeographyLibraryIntegrationService libraryService;

    @MockBean
    private OnDemandGeographySyncService syncService;

    // -------------------------------------------------------------------------
    // 1. GPS RESOLUTION FIXTURE: THOPUDURTHI / ANANTAPUR, AP
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GPS Resolution: Coordinates near Thopudurthi, Anantapur suggest Andhra Pradesh with manual confirmation")
    void testGpsResolutionThopudurthiFixture() throws Exception {
        BigDecimal lat = new BigDecimal("14.505000");
        BigDecimal lon = new BigDecimal("77.448000");

        LocationResolutionDto suggestion = LocationResolutionDto.createSuggestion(
                lat, lon,
                "28", "Andhra Pradesh",
                "502", "Ananthapuramu",
                "5032", "Atmakur",
                "597123", "Thopudurthi",
                "Candidate location suggested based on GPS centroid near Anantapur. Please confirm or correct your mandal and village."
        );

        when(locationService.resolveCandidateLocation(lat, lon)).thenReturn(suggestion);

        mockMvc.perform(get("/api/v1/geography/resolve-location")
                        .param("lat", "14.505000")
                        .param("lon", "77.448000")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.candidateStateCode").value("28"))
                .andExpect(jsonPath("$.data.candidateStateName").value("Andhra Pradesh"))
                .andExpect(jsonPath("$.data.candidateDistrictName").value("Ananthapuramu"))
                .andExpect(jsonPath("$.data.candidateMandalName").value("Atmakur"))
                .andExpect(jsonPath("$.data.candidateVillageName").value("Thopudurthi"))
                .andExpect(jsonPath("$.data.manualConfirmationRequired").value(true))
                .andExpect(jsonPath("$.data.isExactBoundary").value(false));

        verify(locationService, times(1)).resolveCandidateLocation(lat, lon);
    }

    @Test
    @DisplayName("GPS Resolution: Missing coordinates gracefully prompts manual dropdown selection")
    void testGpsResolutionMissingCoordinates() throws Exception {
        LocationResolutionDto fallback = LocationResolutionDto.createSuggestion(
                null, null, null, null, null, null, null, null, null, null,
                "No coordinates provided. Please select state manually from dropdown."
        );
        when(locationService.resolveCandidateLocation(null, null)).thenReturn(fallback);

        mockMvc.perform(get("/api/v1/geography/resolve-location")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.manualConfirmationRequired").value(true))
                .andExpect(jsonPath("$.data.advisoryMessage").value("No coordinates provided. Please select state manually from dropdown."));
    }

    // -------------------------------------------------------------------------
    // 2. STATE COVERAGE DETECTION
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Coverage Detection: Andhra Pradesh detected as PARTIAL coverage (82/688 mandals)")
    void testCoverageDetectionPartialAndhraPradesh() throws Exception {
        StateCoverageDto apCoverage = new StateCoverageDto(
                "28", "Andhra Pradesh",
                28, 28,
                82, 688,
                1319, 17957,
                "PARTIAL",
                true
        );

        when(libraryService.checkStateCoverage("28")).thenReturn(apCoverage);

        mockMvc.perform(get("/api/v1/geography/states/28/coverage")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.stateCode").value("28"))
                .andExpect(jsonPath("$.data.coverageStatus").value("PARTIAL"))
                .andExpect(jsonPath("$.data.dbMandals").value(82))
                .andExpect(jsonPath("$.data.expectedMandals").value(688))
                .andExpect(jsonPath("$.data.syncRequired").value(true));
    }

    @Test
    @DisplayName("Coverage Detection: Kerala detected as COMPLETE coverage (78/78 mandals, 1666 villages)")
    void testCoverageDetectionCompleteKerala() throws Exception {
        StateCoverageDto klCoverage = new StateCoverageDto(
                "32", "Kerala",
                14, 14,
                78, 78,
                1666, 1666,
                "COMPLETE",
                false
        );

        when(libraryService.checkStateCoverage("32")).thenReturn(klCoverage);

        mockMvc.perform(get("/api/v1/geography/states/32/coverage")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.stateCode").value("32"))
                .andExpect(jsonPath("$.data.coverageStatus").value("COMPLETE"))
                .andExpect(jsonPath("$.data.syncRequired").value(false));
    }

    // -------------------------------------------------------------------------
    // 3. ON-DEMAND SYNCHRONIZATION ENDPOINTS
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("On-Demand Sync: Authenticated farmer requests Telangana sync, returns accepted status")
    void testFarmerRequestsStateSync() throws Exception {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        StateSyncStatusDto inProgress = StateSyncStatusDto.inProgress("36", "Telangana", 0, 11308, now);

        when(syncService.requestStateSynchronization("36")).thenReturn(inProgress);

        mockMvc.perform(post("/api/v1/geography/states/36/sync")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_authenticated"))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.stateCode").value("36"))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.totalExpectedRows").value(11308));

        verify(syncService, times(1)).requestStateSynchronization("36");
    }

    @Test
    @DisplayName("On-Demand Sync: Polling sync status returns current progress")
    void testPollSyncStatus() throws Exception {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        StateSyncStatusDto statusDto = StateSyncStatusDto.inProgress("36", "Telangana", 5000, 11308, now);

        when(syncService.getSyncStatus("36")).thenReturn(statusDto);

        mockMvc.perform(get("/api/v1/geography/states/36/sync-status")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.stateCode").value("36"))
                .andExpect(jsonPath("$.data.processedRows").value(5000))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("On-Demand Sync: Unauthenticated caller receives HTTP 401 on sync trigger")
    void testUnauthenticatedSyncTriggerRejected() throws Exception {
        mockMvc.perform(post("/api/v1/geography/states/36/sync"))
                .andExpect(status().isUnauthorized());

        verify(syncService, never()).requestStateSynchronization(any());
    }
}
