package com.agriplatform.geography;

import com.agriplatform.common.config.SecurityConfig;
import com.agriplatform.common.exception.GlobalExceptionHandler;
import com.agriplatform.geography.controller.GeographyController;
import com.agriplatform.geography.dto.DistrictDto;
import com.agriplatform.geography.dto.MandalDto;
import com.agriplatform.geography.dto.StateDto;
import com.agriplatform.geography.dto.VillageDto;
import com.agriplatform.geography.service.GeographyService;
import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end dynamic hierarchy navigation test for Requirement 14.
 *
 * <p>Flow:
 * 1. GET /api/v1/geography/states
 * 2. Select returned state ID dynamically (no hardcoded UUIDs).
 * 3. GET /api/v1/geography/states/{stateId}/districts
 * 4. Select returned district ID dynamically.
 * 5. GET /api/v1/geography/districts/{districtId}/mandals
 * 6. Select returned mandal ID dynamically.
 * 7. GET /api/v1/geography/mandals/{mandalId}/villages
 * 8. Verify the complete 4-level chain.
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
@DisplayName("Requirement 14 — Dynamic Geography Hierarchy Navigation Test")
class GeographyHierarchyEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GeographyService geographyService;

    @MockBean
    private com.agriplatform.geography.service.GeographyLocationService locationService;

    @MockBean
    private com.agriplatform.geography.service.GeographyLibraryIntegrationService libraryService;

    @MockBean
    private com.agriplatform.geography.service.OnDemandGeographySyncService syncService;

    @Test
    @DisplayName("Dynamic 4-level hierarchy chain traversal without hardcoded IDs")
    void traverseCompleteHierarchyDynamically() throws Exception {
        UUID dynamicStateId    = UUID.randomUUID();
        UUID dynamicDistrictId = UUID.randomUUID();
        UUID dynamicMandalId   = UUID.randomUUID();
        UUID dynamicVillageId  = UUID.randomUUID();

        // 1. Mock service responses using dynamic UUIDs
        StateDto stateDto = new StateDto(dynamicStateId, "Andhra Pradesh", "28", true);
        when(geographyService.getAllActiveStates()).thenReturn(List.of(stateDto));

        DistrictDto districtDto = new DistrictDto(dynamicDistrictId, dynamicStateId, "Ananthapuramu", "502", true);
        when(geographyService.getActiveDistrictsByState(dynamicStateId)).thenReturn(List.of(districtDto));

        MandalDto mandalDto = new MandalDto(dynamicMandalId, dynamicDistrictId, "Atmakur", "5032", true);
        when(geographyService.getActiveMandalsByDistrict(dynamicDistrictId)).thenReturn(List.of(mandalDto));

        VillageDto villageDto = new VillageDto(
                dynamicVillageId, dynamicMandalId, "Madakasira", "597123",
                new BigDecimal("14.083300"), new BigDecimal("77.266700"), true);
        when(geographyService.getActiveVillagesByMandal(dynamicMandalId)).thenReturn(List.of(villageDto));

        // Step 1: GET /api/v1/geography/states
        MvcResult stateResult = mockMvc.perform(get("/api/v1/geography/states")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode stateJson = objectMapper.readTree(stateResult.getResponse().getContentAsString());
        assertThat(stateJson.get("success").asBoolean()).isTrue();
        JsonNode statesArray = stateJson.get("data");
        assertThat(statesArray.isArray()).isTrue();
        assertThat(statesArray.size()).isGreaterThanOrEqualTo(1);

        // Dynamically extract stateId from response (NO hardcoded ID)
        String extractedStateId = statesArray.get(0).get("id").asText();
        UUID resolvedStateId = UUID.fromString(extractedStateId);
        assertThat(resolvedStateId).isEqualTo(dynamicStateId);

        // Step 2: GET /api/v1/geography/states/{stateId}/districts
        MvcResult districtResult = mockMvc.perform(
                get("/api/v1/geography/states/{stateId}/districts", resolvedStateId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode districtJson = objectMapper.readTree(districtResult.getResponse().getContentAsString());
        assertThat(districtJson.get("success").asBoolean()).isTrue();
        JsonNode districtsArray = districtJson.get("data");
        assertThat(districtsArray.isArray()).isTrue();
        assertThat(districtsArray.size()).isGreaterThanOrEqualTo(1);

        // Dynamically extract districtId from response
        String extractedDistrictId = districtsArray.get(0).get("id").asText();
        UUID resolvedDistrictId = UUID.fromString(extractedDistrictId);
        assertThat(resolvedDistrictId).isEqualTo(dynamicDistrictId);

        // Step 3: GET /api/v1/geography/districts/{districtId}/mandals
        MvcResult mandalResult = mockMvc.perform(
                get("/api/v1/geography/districts/{districtId}/mandals", resolvedDistrictId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode mandalJson = objectMapper.readTree(mandalResult.getResponse().getContentAsString());
        assertThat(mandalJson.get("success").asBoolean()).isTrue();
        JsonNode mandalsArray = mandalJson.get("data");
        assertThat(mandalsArray.isArray()).isTrue();
        assertThat(mandalsArray.size()).isGreaterThanOrEqualTo(1);

        // Dynamically extract mandalId from response
        String extractedMandalId = mandalsArray.get(0).get("id").asText();
        UUID resolvedMandalId = UUID.fromString(extractedMandalId);
        assertThat(resolvedMandalId).isEqualTo(dynamicMandalId);

        // Step 4: GET /api/v1/geography/mandals/{mandalId}/villages
        MvcResult villageResult = mockMvc.perform(
                get("/api/v1/geography/mandals/{mandalId}/villages", resolvedMandalId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode villageJson = objectMapper.readTree(villageResult.getResponse().getContentAsString());
        assertThat(villageJson.get("success").asBoolean()).isTrue();
        JsonNode villagesArray = villageJson.get("data");
        assertThat(villagesArray.isArray()).isTrue();
        assertThat(villagesArray.size()).isGreaterThanOrEqualTo(1);

        String extractedVillageId = villagesArray.get(0).get("id").asText();
        UUID resolvedVillageId = UUID.fromString(extractedVillageId);
        assertThat(resolvedVillageId).isEqualTo(dynamicVillageId);
        assertThat(villagesArray.get(0).get("name").asText()).isEqualTo("Madakasira");
        assertThat(villagesArray.get(0).get("code").asText()).isEqualTo("597123");
    }
}
