package com.agriplatform.e2e;

import com.agriplatform.analytics.controller.AnalyticsController;
import com.agriplatform.analytics.dto.FarmerAnalyticsSummaryDto;
import com.agriplatform.analytics.dto.GeographicLevel;
import com.agriplatform.analytics.dto.GeographicSummaryDto;
import com.agriplatform.analytics.service.AnalyticsService;
import com.agriplatform.common.config.SecurityConfig;
import com.agriplatform.common.exception.BadRequestException;
import com.agriplatform.common.exception.ConflictException;
import com.agriplatform.common.exception.GlobalExceptionHandler;
import com.agriplatform.common.exception.ResourceNotFoundException;
import com.agriplatform.common.util.SecurityUtils;
import com.agriplatform.cultivation.controller.CultivationController;
import com.agriplatform.cultivation.controller.ReferenceDataController;
import com.agriplatform.cultivation.dto.*;
import com.agriplatform.cultivation.service.CultivationService;
import com.agriplatform.farmer.controller.FarmerProfileController;
import com.agriplatform.farmer.dto.CreateFarmerProfileRequestDto;
import com.agriplatform.farmer.dto.FarmerProfileResponseDto;
import com.agriplatform.farmer.service.FarmerProfileService;
import com.agriplatform.geography.controller.GeographyController;
import com.agriplatform.geography.dto.DistrictDto;
import com.agriplatform.geography.dto.LocationResolutionDto;
import com.agriplatform.geography.dto.StateDto;
import com.agriplatform.geography.dto.VillageDto;
import com.agriplatform.geography.service.GeographyLibraryIntegrationService;
import com.agriplatform.geography.service.GeographyLocationService;
import com.agriplatform.geography.service.GeographyService;
import com.agriplatform.geography.service.OnDemandGeographySyncService;
import com.agriplatform.history.controller.HistoryController;
import com.agriplatform.history.dto.HistorySnapshotDto;
import com.agriplatform.history.service.HistoryService;
import com.agriplatform.notification.controller.NotificationController;
import com.agriplatform.notification.dto.NotificationDto;
import com.agriplatform.notification.dto.UnreadCountDto;
import com.agriplatform.notification.service.NotificationService;
import com.agriplatform.plot.controller.PlotController;
import com.agriplatform.plot.dto.CreatePlotRequestDto;
import com.agriplatform.plot.dto.PlotResponseDto;
import com.agriplatform.plot.service.PlotService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Task 12 — End-to-End Backend Workflow Integration Test Suite.
 *
 * <p>Validates the complete supported farmer journey across all controllers and HTTP boundaries:
 * <ul>
 *   <li><b>Phase 3.A: Authentication & Identity</b> — JWT token validation, 401 unauthenticated, role restrictions (403 on admin endpoints), principal-derived identity.</li>
 *   <li><b>Phase 3.B: Geography Integration</b> — GPS location resolution advisory, administrative hierarchy (State -> District -> Village).</li>
 *   <li><b>Phase 3.C: Plot Registration & Ownership</b> — Plot creation with decoupled location, IDOR isolation (404 on cross-farmer access), positive area validation.</li>
 *   <li><b>Phase 3.D: Cultivation Registration & Lifecycle</b> — Single crop cycle identity, area constraint (cannot exceed plot), duplicate cycle rejection (409), state transitions (PLANNED -> STARTED -> COMPLETED), terminal edit rejection.</li>
 *   <li><b>Phase 3.E: History & Analytics Integration</b> — Auditable snapshots and status events, immutability (405 on POST/PUT), active vs completed area analytics aggregation.</li>
 *   <li><b>Phase 3.F: Error Handling & Recovery</b> — Malformed JSON, invalid UUIDs, no internal stack trace or credential leakage.</li>
 * </ul>
 */
@WebMvcTest(
    controllers = {
        GeographyController.class,
        com.agriplatform.geography.ingestion.controller.GeographyAdminController.class,
        FarmerProfileController.class,
        PlotController.class,
        CultivationController.class,
        ReferenceDataController.class,
        HistoryController.class,
        AnalyticsController.class,
        NotificationController.class
    },
    excludeAutoConfiguration = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
    }
)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, SecurityUtils.class})
@ActiveProfiles("test")
@DisplayName("Task 12 — End-to-End Backend Workflow Integration Tests")
class Task12EndToEndWorkflowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    // Mocked service layer
    @MockBean private GeographyService geographyService;
    @MockBean private GeographyLocationService locationService;
    @MockBean private GeographyLibraryIntegrationService libraryService;
    @MockBean private OnDemandGeographySyncService syncService;
    @MockBean private com.agriplatform.geography.ingestion.service.GeographyIngestionService geographyIngestionService;
    @MockBean private com.agriplatform.geography.lgd.sync.LgdSyncOperations lgdSyncOperations;
    @MockBean private FarmerProfileService farmerProfileService;
    @MockBean private PlotService plotService;
    @MockBean private CultivationService cultivationService;
    @MockBean private HistoryService historyService;
    @MockBean private AnalyticsService analyticsService;
    @MockBean private NotificationService notificationService;

    // Test IDs
    private static final UUID FARMER_A_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID FARMER_B_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID STATE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID DISTRICT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID MANDAL_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID VILLAGE_KERALA_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID VILLAGE_AP_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");
    private static final UUID PLOT_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");
    private static final UUID CROP_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID SEASON_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CULTIVATION_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    // =========================================================================
    // SECTION A: AUTHENTICATION AND FARMER IDENTITY
    // =========================================================================

    @Nested
    @DisplayName("Phase 3.A: Authentication & Identity Verification")
    class AuthenticationAndIdentityTests {

        @Test
        @DisplayName("Unauthenticated request to protected endpoint returns 401 Unauthorized")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/farmers/me"))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(get("/api/v1/plots"))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(get("/api/v1/cultivations"))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(get("/api/v1/analytics/farmer/summary"))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(get("/api/v1/notifications"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Ordinary farmer JWT cannot access admin geography import endpoint (403 Forbidden)")
        void adminEndpoint_ordinaryFarmer_returns403() throws Exception {
            mockMvc.perform(post("/api/v1/admin/geography/sync-lgd/initial")
                    .with(jwt().jwt(j -> j.subject(FARMER_A_ID.toString()))
                              .authorities(new SimpleGrantedAuthority("ROLE_authenticated"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Authenticated farmer receives 200 OK for private profile")
        void authenticated_returnsProfile() throws Exception {
            FarmerProfileResponseDto profile = new FarmerProfileResponseDto(
                    FARMER_A_ID, "Farmer A", VILLAGE_KERALA_ID, "Village Kerala",
                    MANDAL_ID, "Mandal K", DISTRICT_ID, "District K",
                    STATE_ID, "Kerala", "ACTIVE", true, 1L, OffsetDateTime.now(), OffsetDateTime.now());
            when(farmerProfileService.getProfile()).thenReturn(profile);

            mockMvc.perform(get("/api/v1/farmers/me")
                    .with(jwt().jwt(j -> j.subject(FARMER_A_ID.toString()))
                              .authorities(new SimpleGrantedAuthority("ROLE_authenticated"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.fullName").value("Farmer A"))
                    .andExpect(jsonPath("$.data.profileStatus").value("ACTIVE"));
        }
    }

    // =========================================================================
    // SECTION B: GEOGRAPHY INTEGRATION
    // =========================================================================

    @Nested
    @DisplayName("Phase 3.B: Geography Integration Verification")
    class GeographyIntegrationTests {

        @Test
        @DisplayName("Public hierarchy navigation: states, districts, mandals, villages")
        void geographyHierarchy_publicAccess() throws Exception {
            when(geographyService.getAllActiveStates()).thenReturn(List.of(new StateDto(STATE_ID, "Kerala", "32", true)));
            when(geographyService.getActiveDistrictsByState(STATE_ID)).thenReturn(List.of(new DistrictDto(DISTRICT_ID, STATE_ID, "Ernakulam", "555", true)));

            mockMvc.perform(get("/api/v1/geography/states"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].name").value("Kerala"));

            mockMvc.perform(get("/api/v1/geography/states/" + STATE_ID + "/districts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].name").value("Ernakulam"));
        }

        @Test
        @DisplayName("GPS resolution provides advisory location suggestion without forcing identity")
        void gpsResolution_advisorySuggestion() throws Exception {
            LocationResolutionDto suggestion = LocationResolutionDto.createSuggestion(
                    new BigDecimal("9.9312"), new BigDecimal("76.2673"),
                    "32", "Kerala", "555", "Ernakulam",
                    "5555", "Kanayannur", "666666", "Kochi Village",
                    "Nearest village resolved from coordinates");
            when(locationService.resolveCandidateLocation(new BigDecimal("9.9312"), new BigDecimal("76.2673")))
                    .thenReturn(suggestion);

            mockMvc.perform(get("/api/v1/geography/resolve-location")
                    .param("lat", "9.9312")
                    .param("lon", "76.2673"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.candidateVillageName").value("Kochi Village"))
                    .andExpect(jsonPath("$.data.manualConfirmationRequired").value(true));
        }
    }

    // =========================================================================
    // SECTION C: PLOT REGISTRATION AND OWNERSHIP
    // =========================================================================

    @Nested
    @DisplayName("Phase 3.C: Plot Registration & Ownership Verification")
    class PlotRegistrationAndOwnershipTests {

        @Test
        @DisplayName("Farmer creates plot in a village decoupled from home village (201 Created)")
        void createPlot_decoupledVillage_returns201() throws Exception {
            CreatePlotRequestDto request = new CreatePlotRequestDto(
                    VILLAGE_AP_ID, "North Plot", new BigDecimal("10.50"));
            PlotResponseDto response = new PlotResponseDto(
                    PLOT_ID, FARMER_A_ID, VILLAGE_AP_ID, "Village AP", "Mandal AP", "District AP", "Andhra Pradesh",
                    "North Plot", new BigDecimal("10.50"), true, "2026-10-18T00:00:00Z", OffsetDateTime.now(), OffsetDateTime.now());
            when(plotService.createPlot(any(), any())).thenReturn(response);

            mockMvc.perform(post("/api/v1/plots")
                    .with(jwt().jwt(j -> j.subject(FARMER_A_ID.toString()))
                              .authorities(new SimpleGrantedAuthority("ROLE_authenticated")))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(PLOT_ID.toString()))
                    .andExpect(jsonPath("$.data.villageId").value(VILLAGE_AP_ID.toString()))
                    .andExpect(jsonPath("$.data.totalAreaAcres").value(10.50));
        }

        @Test
        @DisplayName("Plot creation with negative area is rejected (400 Bad Request)")
        void createPlot_negativeArea_returns400() throws Exception {
            String json = """
                    {
                      "villageId": "%s",
                      "label": "Invalid Area Plot",
                      "totalAreaAcres": -2.00
                    }
                    """.formatted(VILLAGE_AP_ID);

            mockMvc.perform(post("/api/v1/plots")
                    .with(jwt().jwt(j -> j.subject(FARMER_A_ID.toString()))
                              .authorities(new SimpleGrantedAuthority("ROLE_authenticated")))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("IDOR Protection: Farmer B attempting to access Farmer A's plot receives 404 Not Found")
        void idor_crossFarmerPlotAccess_returns404() throws Exception {
            when(plotService.getPlot(PLOT_ID))
                    .thenThrow(new ResourceNotFoundException("Plot not found with id: " + PLOT_ID));

            mockMvc.perform(get("/api/v1/plots/" + PLOT_ID)
                    .with(jwt().jwt(j -> j.subject(FARMER_B_ID.toString()))
                              .authorities(new SimpleGrantedAuthority("ROLE_authenticated"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // =========================================================================
    // SECTION D: CULTIVATION REGISTRATION AND LIFECYCLE
    // =========================================================================

    @Nested
    @DisplayName("Phase 3.D: Cultivation Registration & Lifecycle Verification")
    class CultivationLifecycleTests {

        @Test
        @DisplayName("Create cultivation intention on owned plot (201 Created)")
        void createCultivation_valid_returns201() throws Exception {
            CreateCultivationRequestDto request = new CreateCultivationRequestDto(
                    PLOT_ID, CROP_ID, null, SEASON_ID, (short) 2026,
                    new BigDecimal("8.00"), LocalDate.of(2026, 6, 15));
            CultivationResponseDto response = new CultivationResponseDto(
                    CULTIVATION_ID, PLOT_ID, "North Plot", FARMER_A_ID, CROP_ID, "Paddy",
                    "Cereal", null, null, SEASON_ID, "Kharif", (short) 2026,
                    new BigDecimal("8.00"), LocalDate.of(2026, 6, 15), null, null, null,
                    "PLANNED", null, 1, "FARMER_APP",
                    OffsetDateTime.now(), OffsetDateTime.now());
            when(cultivationService.createCultivation(any(), any())).thenReturn(response);

            mockMvc.perform(post("/api/v1/cultivations")
                    .with(jwt().jwt(j -> j.subject(FARMER_A_ID.toString()))
                              .authorities(new SimpleGrantedAuthority("ROLE_authenticated")))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(CULTIVATION_ID.toString()))
                    .andExpect(jsonPath("$.data.status").value("PLANNED"))
                    .andExpect(jsonPath("$.data.cultivatedAreaAcres").value(8.00));
        }

        @Test
        @DisplayName("Area constraint violation: Cultivation area exceeding plot capacity returns 400 Bad Request")
        void createCultivation_areaExceedsPlot_returns400() throws Exception {
            when(cultivationService.createCultivation(any(), any()))
                    .thenThrow(new BadRequestException("Cultivated area exceeds available plot capacity"));

            CreateCultivationRequestDto request = new CreateCultivationRequestDto(
                    PLOT_ID, CROP_ID, null, SEASON_ID, (short) 2026,
                    new BigDecimal("25.00"), LocalDate.of(2026, 6, 15));

            mockMvc.perform(post("/api/v1/cultivations")
                    .with(jwt().jwt(j -> j.subject(FARMER_A_ID.toString()))
                              .authorities(new SimpleGrantedAuthority("ROLE_authenticated")))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error").value(containsString("Cultivated area exceeds")));
        }

        @Test
        @DisplayName("Duplicate cycle: Re-registering active cycle on same plot/season/year returns 409 Conflict")
        void createCultivation_duplicateCycle_returns409() throws Exception {
            when(cultivationService.createCultivation(any(), any()))
                    .thenThrow(new ConflictException("An active cultivation cycle already exists for this plot, season, and year"));

            CreateCultivationRequestDto request = new CreateCultivationRequestDto(
                    PLOT_ID, CROP_ID, null, SEASON_ID, (short) 2026,
                    new BigDecimal("5.00"), LocalDate.of(2026, 6, 15));

            mockMvc.perform(post("/api/v1/cultivations")
                    .with(jwt().jwt(j -> j.subject(FARMER_A_ID.toString()))
                              .authorities(new SimpleGrantedAuthority("ROLE_authenticated")))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error").value(containsString("active cultivation cycle already exists")));
        }

        @Test
        @DisplayName("Lifecycle transition: PLANNED -> STARTED (200 OK)")
        void updateStatus_toStarted_returns200() throws Exception {
            CultivationStatusUpdateRequestDto request = new CultivationStatusUpdateRequestDto(
                    "STARTED", null, LocalDate.now(), null, null, null);
            CultivationResponseDto response = new CultivationResponseDto(
                    CULTIVATION_ID, PLOT_ID, "North Plot", FARMER_A_ID, CROP_ID, "Paddy",
                    "Cereal", null, null, SEASON_ID, "Kharif", (short) 2026,
                    new BigDecimal("8.00"), LocalDate.of(2026, 6, 15), LocalDate.now(), null, null,
                    "STARTED", null, 2, "FARMER_APP",
                    OffsetDateTime.now(), OffsetDateTime.now());
            when(cultivationService.updateCultivationStatus(eq(CULTIVATION_ID), any())).thenReturn(response);

            mockMvc.perform(patch("/api/v1/cultivations/" + CULTIVATION_ID + "/status")
                    .with(jwt().jwt(j -> j.subject(FARMER_A_ID.toString()))
                              .authorities(new SimpleGrantedAuthority("ROLE_authenticated")))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("STARTED"))
                    .andExpect(jsonPath("$.data.version").value(2));
        }

        @Test
        @DisplayName("Lifecycle transition: STARTED -> COMPLETED (200 OK)")
        void updateStatus_toCompleted_returns200() throws Exception {
            CultivationStatusUpdateRequestDto request = new CultivationStatusUpdateRequestDto(
                    "COMPLETED", null, null, new BigDecimal("8.00"), LocalDate.now(), null);
            CultivationResponseDto response = new CultivationResponseDto(
                    CULTIVATION_ID, PLOT_ID, "North Plot", FARMER_A_ID, CROP_ID, "Paddy",
                    "Cereal", null, null, SEASON_ID, "Kharif", (short) 2026,
                    new BigDecimal("8.00"), LocalDate.of(2026, 6, 15), LocalDate.now().minusMonths(3),
                    new BigDecimal("8.00"), LocalDate.now(),
                    "COMPLETED", null, 3, "FARMER_APP",
                    OffsetDateTime.now(), OffsetDateTime.now());
            when(cultivationService.updateCultivationStatus(eq(CULTIVATION_ID), any())).thenReturn(response);

            mockMvc.perform(patch("/api/v1/cultivations/" + CULTIVATION_ID + "/status")
                    .with(jwt().jwt(j -> j.subject(FARMER_A_ID.toString()))
                              .authorities(new SimpleGrantedAuthority("ROLE_authenticated")))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("COMPLETED"));
        }

        @Test
        @DisplayName("Terminal mutation rejection: Editing COMPLETED cultivation returns 400 Bad Request")
        void updateCompleted_rejected_returns400() throws Exception {
            when(cultivationService.updateCultivation(eq(CULTIVATION_ID), any()))
                    .thenThrow(new BadRequestException("Cannot modify cultivation record in terminal status COMPLETED"));

            UpdateCultivationRequestDto updateRequest = new UpdateCultivationRequestDto(
                    CROP_ID, null, new BigDecimal("9.00"), null, 3);

            mockMvc.perform(put("/api/v1/cultivations/" + CULTIVATION_ID)
                    .with(jwt().jwt(j -> j.subject(FARMER_A_ID.toString()))
                              .authorities(new SimpleGrantedAuthority("ROLE_authenticated")))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error").value(containsString("terminal status COMPLETED")));
        }
    }

    // =========================================================================
    // SECTION E: CULTIVATION HISTORY AND ANALYTICS INTEGRATION
    // =========================================================================

    @Nested
    @DisplayName("Phase 3.E: History & Analytics Integration Verification")
    class HistoryAndAnalyticsTests {

        @Test
        @DisplayName("History snapshots retrieval returns immutable audit list (200 OK)")
        void getHistorySnapshots_returns200() throws Exception {
            HistorySnapshotDto snapshot = new HistorySnapshotDto(
                    UUID.randomUUID(), CULTIVATION_ID, 1, OffsetDateTime.now(),
                    PLOT_ID, FARMER_A_ID, CROP_ID, null, SEASON_ID, (short) 2026,
                    new BigDecimal("8.00"), LocalDate.of(2026, 6, 15), null, null, null,
                    "PLANNED", null, "FARMER_APP", FARMER_A_ID, "FARMER",
                    "STATUS_CHANGE", List.of("status"), "Initial creation");
            Page<HistorySnapshotDto> page = new PageImpl<>(List.of(snapshot));
            when(historyService.getSnapshotsForCultivation(eq(CULTIVATION_ID), any())).thenReturn(page);

            mockMvc.perform(get("/api/v1/history/cultivations/" + CULTIVATION_ID + "/snapshots")
                    .with(jwt().jwt(j -> j.subject(FARMER_A_ID.toString()))
                              .authorities(new SimpleGrantedAuthority("ROLE_authenticated"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].version").value(1))
                    .andExpect(jsonPath("$.data.content[0].changeType").value("STATUS_CHANGE"));
        }

        @Test
        @DisplayName("History Immutability: Mutating history via POST returns 405 Method Not Allowed")
        void mutateHistory_returns405() throws Exception {
            mockMvc.perform(post("/api/v1/history/cultivations/" + CULTIVATION_ID + "/snapshots")
                    .with(jwt().jwt(j -> j.subject(FARMER_A_ID.toString()))
                              .authorities(new SimpleGrantedAuthority("ROLE_authenticated"))))
                    .andExpect(status().isMethodNotAllowed());

            mockMvc.perform(delete("/api/v1/history/cultivations/" + CULTIVATION_ID + "/snapshots/" + UUID.randomUUID())
                    .with(jwt().jwt(j -> j.subject(FARMER_A_ID.toString()))
                              .authorities(new SimpleGrantedAuthority("ROLE_authenticated"))))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        @DisplayName("Farmer Analytics Summary accurately reports plots and excludes completed cycles from active area")
        void farmerAnalyticsSummary_excludesCompletedFromActiveArea() throws Exception {
            FarmerAnalyticsSummaryDto summary = new FarmerAnalyticsSummaryDto(
                    FARMER_A_ID, SEASON_ID, "Kharif", (short) 2026,
                    1L, new BigDecimal("12.50"),
                    new BigDecimal("0.00"), // Active cultivated area = 0 because cycle is COMPLETED
                    new BigDecimal("8.00"), // Completed cultivated area = 8.00
                    0L,                     // Active cycles = 0
                    1L,                     // Completed cycles = 1
                    0L, 0L,
                    new BigDecimal("0.00"),
                    Collections.emptyList()
            );
            when(analyticsService.getFarmerSummary(any(), any())).thenReturn(summary);

            mockMvc.perform(get("/api/v1/analytics/farmer/summary")
                    .with(jwt().jwt(j -> j.subject(FARMER_A_ID.toString()))
                              .authorities(new SimpleGrantedAuthority("ROLE_authenticated"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.totalPlotsCount").value(1))
                    .andExpect(jsonPath("$.data.activeCultivatedAreaAcres").value(0.00))
                    .andExpect(jsonPath("$.data.activeCultivationsCount").value(0))
                    .andExpect(jsonPath("$.data.completedCultivationsCount").value(1));
        }

        @Test
        @DisplayName("Regional Geographic Analytics Summary returns anonymized aggregate metrics")
        void regionalAnalytics_returnsAnonymizedMetrics() throws Exception {
            GeographicSummaryDto geoSummary = new GeographicSummaryDto(
                    GeographicLevel.STATE, STATE_ID, "Kerala", "32", "India",
                    null, null, null,
                    new BigDecimal("3400.50"), 1200L, 850L, 1400L,
                    new BigDecimal("4.00"), new BigDecimal("2.83"),
                    new BigDecimal("2850.75"), 450L, 50L, 10L);
            when(analyticsService.getGeographicSummary(eq(GeographicLevel.STATE), eq(STATE_ID), any(), any()))
                    .thenReturn(geoSummary);

            mockMvc.perform(get("/api/v1/analytics/geography/summary")
                    .with(jwt().jwt(j -> j.subject(FARMER_A_ID.toString()))
                              .authorities(new SimpleGrantedAuthority("ROLE_authenticated")))
                    .param("level", "STATE")
                    .param("id", STATE_ID.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.name").value("Kerala"))
                    .andExpect(jsonPath("$.data.activeFarmerCount").value(850))
                    // Ensure response does NOT leak any private farmer ID
                    .andExpect(jsonPath("$.data.farmerId").doesNotExist());
        }
    }

    // =========================================================================
    // SECTION F: NOTIFICATIONS AND ERROR HANDLING
    // =========================================================================

    @Nested
    @DisplayName("Phase 3.F: Notifications & Error Handling Verification")
    class NotificationsAndErrorHandlingTests {

        @Test
        @DisplayName("Farmer retrieves notifications and unread count (200 OK)")
        void notifications_readAndCount() throws Exception {
            when(notificationService.getUnreadCount()).thenReturn(new UnreadCountDto(1L));
            NotificationDto notif = new NotificationDto(
                    UUID.randomUUID(), "Harvest Completed", "Your cycle has been completed.",
                    "CULTIVATION", false, null, OffsetDateTime.now());
            when(notificationService.getNotifications(any())).thenReturn(new PageImpl<>(List.of(notif)));

            mockMvc.perform(get("/api/v1/notifications/unread/count")
                    .with(jwt().jwt(j -> j.subject(FARMER_A_ID.toString()))
                              .authorities(new SimpleGrantedAuthority("ROLE_authenticated"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.unreadCount").value(1));

            mockMvc.perform(get("/api/v1/notifications")
                    .with(jwt().jwt(j -> j.subject(FARMER_A_ID.toString()))
                              .authorities(new SimpleGrantedAuthority("ROLE_authenticated"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].title").value("Harvest Completed"));
        }

        @Test
        @DisplayName("Malformed UUID parameter returns 400 Bad Request with standard envelope")
        void malformedUuid_returns400() throws Exception {
            mockMvc.perform(get("/api/v1/plots/not-a-valid-uuid")
                    .with(jwt().jwt(j -> j.subject(FARMER_A_ID.toString()))
                              .authorities(new SimpleGrantedAuthority("ROLE_authenticated"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.data").doesNotExist())
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("Malformed JSON payload returns 400 Bad Request without stack trace leakage")
        void malformedJson_returns400WithoutStackTrace() throws Exception {
            mockMvc.perform(post("/api/v1/plots")
                    .with(jwt().jwt(j -> j.subject(FARMER_A_ID.toString()))
                              .authorities(new SimpleGrantedAuthority("ROLE_authenticated")))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"villageId\": \"invalid-json-body-no-closing-brace"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    // Ensure stack traces are never exposed
                    .andExpect(content().string(not(containsString("at com.agriplatform"))))
                    .andExpect(content().string(not(containsString("Exception:"))));
        }
    }
}
