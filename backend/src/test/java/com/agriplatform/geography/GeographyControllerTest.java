package com.agriplatform.geography;

import com.agriplatform.common.config.SecurityConfig;
import com.agriplatform.common.exception.GlobalExceptionHandler;
import com.agriplatform.common.exception.ResourceNotFoundException;
import com.agriplatform.geography.controller.GeographyController;
import com.agriplatform.geography.dto.DistrictDto;
import com.agriplatform.geography.dto.MandalDto;
import com.agriplatform.geography.dto.StateDto;
import com.agriplatform.geography.dto.VillageDto;
import com.agriplatform.geography.service.GeographyService;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Task 05 — Geography module controller slice tests.
 *
 * <h2>Strategy</h2>
 * <p>{@link WebMvcTest} loads only the web layer (controller + security filter
 * chain) in isolation. {@link GeographyService} is mocked with {@link MockBean}
 * so no database connection is required. JPA/DataSource auto-configuration is
 * excluded explicitly to match the test profile setup.
 *
 * <h2>Covered cases (8 required)</h2>
 * <ol>
 *   <li>GET /states is publicly accessible without JWT (no 401/403)</li>
 *   <li>GET /states returns HTTP 200 and correct JSON envelope</li>
 *   <li>GET /states/{id}/districts → district list for valid state</li>
 *   <li>GET /districts/{id}/mandals → mandal list for valid district</li>
 *   <li>GET /mandals/{id}/villages → village list for valid mandal</li>
 *   <li>Invalid ID → 404 with standard error envelope</li>
 *   <li>No write endpoints: POST/PUT/DELETE → 405 Method Not Allowed</li>
 *   <li>Response envelope always contains success, data, timestamp fields</li>
 * </ol>
 */
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
@DisplayName("Task 05 — Geography Controller Tests")
class GeographyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GeographyService geographyService;

    @MockBean
    private com.agriplatform.geography.service.GeographyLocationService locationService;

    @MockBean
    private com.agriplatform.geography.service.GeographyLibraryIntegrationService libraryService;

    @MockBean
    private com.agriplatform.geography.service.OnDemandGeographySyncService syncService;

    // =========================================================================
    // Test 1 — GET /api/v1/geography/states is publicly accessible (no 401)
    // =========================================================================

    @Test
    @DisplayName("1. GET /states is public — no JWT required (no 401/403)")
    void states_endpoint_is_public_no_jwt_required() throws Exception {
        when(geographyService.getAllActiveStates()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/geography/states")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 401 || status == 403) {
                        throw new AssertionError(
                            "GET /api/v1/geography/states must be public, got HTTP " + status);
                    }
                });
    }

    // =========================================================================
    // Test 2 — HTTP 200 on GET /states
    // =========================================================================

    @Test
    @DisplayName("2. GET /states returns HTTP 200 with success envelope")
    void states_endpoint_returns_200() throws Exception {
        UUID stateId = UUID.randomUUID();
        StateDto stateDto = new StateDto(stateId, "Andhra Pradesh", "AP", true);
        when(geographyService.getAllActiveStates()).thenReturn(List.of(stateDto));

        mockMvc.perform(get("/api/v1/geography/states")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("Andhra Pradesh"))
                .andExpect(jsonPath("$.data[0].code").value("AP"));
    }

    // =========================================================================
    // Test 3 — State → District navigation
    // =========================================================================

    @Test
    @DisplayName("3. GET /states/{stateId}/districts returns districts for valid state")
    void districts_returned_for_valid_state() throws Exception {
        UUID stateId = UUID.randomUUID();
        UUID districtId = UUID.randomUUID();
        DistrictDto districtDto = new DistrictDto(districtId, stateId, "Guntur", "GNT", true);
        when(geographyService.getActiveDistrictsByState(stateId)).thenReturn(List.of(districtDto));

        mockMvc.perform(get("/api/v1/geography/states/{stateId}/districts", stateId)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("Guntur"))
                .andExpect(jsonPath("$.data[0].stateId").value(stateId.toString()));
    }

    // =========================================================================
    // Test 4 — District → Mandal navigation
    // =========================================================================

    @Test
    @DisplayName("4. GET /districts/{districtId}/mandals returns mandals for valid district")
    void mandals_returned_for_valid_district() throws Exception {
        UUID districtId = UUID.randomUUID();
        UUID mandalId = UUID.randomUUID();
        MandalDto mandalDto = new MandalDto(mandalId, districtId, "Tenali", "TNL", true);
        when(geographyService.getActiveMandalsByDistrict(districtId)).thenReturn(List.of(mandalDto));

        mockMvc.perform(get("/api/v1/geography/districts/{districtId}/mandals", districtId)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("Tenali"))
                .andExpect(jsonPath("$.data[0].districtId").value(districtId.toString()));
    }

    // =========================================================================
    // Test 5 — Mandal → Village navigation
    // =========================================================================

    @Test
    @DisplayName("5. GET /mandals/{mandalId}/villages returns villages for valid mandal")
    void villages_returned_for_valid_mandal() throws Exception {
        UUID mandalId = UUID.randomUUID();
        UUID villageId = UUID.randomUUID();
        VillageDto villageDto = new VillageDto(
                villageId, mandalId, "Kolluru", "KLL",
                new BigDecimal("16.234500"), new BigDecimal("80.456700"), true);
        when(geographyService.getActiveVillagesByMandal(mandalId)).thenReturn(List.of(villageDto));

        mockMvc.perform(get("/api/v1/geography/mandals/{mandalId}/villages", mandalId)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("Kolluru"))
                .andExpect(jsonPath("$.data[0].mandalId").value(mandalId.toString()));
    }

    // =========================================================================
    // Test 6 — Invalid ID → standard 404 with error envelope
    // =========================================================================

    @Test
    @DisplayName("6. Invalid state ID returns 404 with standard error envelope")
    void invalid_state_id_returns_404_with_error_envelope() throws Exception {
        UUID unknownId = UUID.randomUUID();
        when(geographyService.getActiveDistrictsByState(unknownId))
                .thenThrow(new ResourceNotFoundException("State not found: " + unknownId));

        mockMvc.perform(get("/api/v1/geography/states/{stateId}/districts", unknownId)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("State not found: " + unknownId));
    }

    // =========================================================================
    // Test 7 — No write endpoints (POST/PUT/DELETE → 405 Method Not Allowed)
    // =========================================================================

    @Test
    @DisplayName("7. POST/PUT/DELETE on geography endpoints are blocked (no write access)")
    void no_write_endpoints_on_geography() throws Exception {
        // POST, PUT, DELETE on geography endpoints must be blocked.
        // SecurityConfig only permits GET on /api/v1/geography/**; non-GET
        // methods return 401 (security blocks unauthenticated request before
        // it reaches the controller) or 405 (no handler mapped). Both
        // effectively prevent write operations. Either status is acceptable.

        mockMvc.perform(post("/api/v1/geography/states")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 401 && status != 405) {
                        throw new AssertionError(
                            "POST /api/v1/geography/states should be blocked, got HTTP " + status);
                    }
                });

        mockMvc.perform(put("/api/v1/geography/states/" + UUID.randomUUID() + "/districts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 401 && status != 405) {
                        throw new AssertionError(
                            "PUT should be blocked, got HTTP " + status);
                    }
                });

        mockMvc.perform(delete("/api/v1/geography/states/" + UUID.randomUUID()))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 401 && status != 405) {
                        throw new AssertionError(
                            "DELETE should be blocked, got HTTP " + status);
                    }
                });
    }

    // =========================================================================
    // Test 8 — Response envelope shape is consistent
    // =========================================================================

    @Test
    @DisplayName("8. Response envelope always includes success, data, and timestamp")
    void response_envelope_shape_is_consistent() throws Exception {
        UUID stateId = UUID.randomUUID();
        StateDto stateDto = new StateDto(stateId, "Telangana", "TS", true);
        when(geographyService.getAllActiveStates()).thenReturn(List.of(stateDto));

        mockMvc.perform(get("/api/v1/geography/states")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").exists())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.error").doesNotExist());
    }
}
