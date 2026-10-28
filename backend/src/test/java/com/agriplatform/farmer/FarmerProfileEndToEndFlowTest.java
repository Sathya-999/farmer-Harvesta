package com.agriplatform.farmer;

import com.agriplatform.common.config.SecurityConfig;
import com.agriplatform.common.exception.ConflictException;
import com.agriplatform.common.exception.GlobalExceptionHandler;
import com.agriplatform.common.exception.ResourceNotFoundException;
import com.agriplatform.common.util.SecurityUtils;
import com.agriplatform.farmer.controller.FarmerProfileController;
import com.agriplatform.farmer.dto.CreateFarmerProfileRequestDto;
import com.agriplatform.farmer.dto.FarmerProfileResponseDto;
import com.agriplatform.farmer.dto.UpdateFarmerProfileRequestDto;
import com.agriplatform.farmer.service.FarmerProfileService;
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

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = FarmerProfileController.class,
    excludeAutoConfiguration = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
    }
)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, SecurityUtils.class})
@ActiveProfiles("test")
@DisplayName("Task 06 — Full Farmer Profile Lifecycle End-to-End Flow")
class FarmerProfileEndToEndFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FarmerProfileService farmerProfileService;

    private static final String BASE_URL = "/api/v1/farmers/me";

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor validJwt(UUID userId) {
        return jwt().jwt(builder -> builder
                .subject(userId.toString())
                .issuer("http://localhost/test-issuer")
                .claim("aud", List.of("authenticated"))
                .claim("role", "authenticated")
                .expiresAt(Instant.now().plusSeconds(3600))
        );
    }

    @Test
    @DisplayName("Complete Farmer Lifecycle: Signup -> 404 -> Init -> 409 Duplicate -> Get -> 409 Stale Update -> Valid Update")
    void testCompleteFarmerLifecycle() throws Exception {
        UUID farmerId = UUID.randomUUID();
        UUID villageId = UUID.randomUUID();
        UUID mandalId = UUID.randomUUID();
        UUID districtId = UUID.randomUUID();
        UUID stateId = UUID.randomUUID();

        // -------------------------------------------------------------
        // Step 1: Pre-onboarding profile check returns 404 Not Found
        // -------------------------------------------------------------
        doThrow(new ResourceNotFoundException("Farmer profile not found for user: " + farmerId))
                .when(farmerProfileService).getProfile();

        mockMvc.perform(get(BASE_URL)
                        .with(validJwt(farmerId))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        // -------------------------------------------------------------
        // Step 2: Farmer completes onboarding (POST /api/v1/farmers/me)
        // -------------------------------------------------------------
        OffsetDateTime t1 = OffsetDateTime.now();
        Long version1 = t1.toInstant().toEpochMilli();

        FarmerProfileResponseDto initializedProfile = new FarmerProfileResponseDto(
                farmerId, "Anand Rao", villageId, "Mapakshi",
                mandalId, "Chittoor Urban", districtId, "Chittoor",
                stateId, "Andhra Pradesh", "ACTIVE", true,
                version1, t1, t1
        );

        doReturn(initializedProfile).when(farmerProfileService).initProfile(any(CreateFarmerProfileRequestDto.class));

        CreateFarmerProfileRequestDto createReq = new CreateFarmerProfileRequestDto(
                "Anand Rao", villageId, mandalId, districtId, stateId
        );

        MvcResult initResult = mockMvc.perform(post(BASE_URL)
                        .with(validJwt(farmerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode initJson = objectMapper.readTree(initResult.getResponse().getContentAsString());
        assertThat(initJson.get("success").asBoolean()).isTrue();
        assertThat(initJson.get("data").get("fullName").asText()).isEqualTo("Anand Rao");
        assertThat(initJson.get("data").get("villageName").asText()).isEqualTo("Mapakshi");
        assertThat(initJson.get("data").get("stateName").asText()).isEqualTo("Andhra Pradesh");
        assertThat(initJson.get("data").get("onboardingCompleted").asBoolean()).isTrue();
        Long extractedVersion = initJson.get("data").get("version").asLong();
        assertThat(extractedVersion).isEqualTo(version1);

        // -------------------------------------------------------------
        // Step 3: Duplicate onboarding attempt returns 409 Conflict
        // -------------------------------------------------------------
        doThrow(new ConflictException("Farmer profile has already been initialized"))
                .when(farmerProfileService).initProfile(any(CreateFarmerProfileRequestDto.class));

        mockMvc.perform(post(BASE_URL)
                        .with(validJwt(farmerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isConflict());

        // -------------------------------------------------------------
        // Step 4: GET /api/v1/farmers/me returns the active profile
        // -------------------------------------------------------------
        doReturn(initializedProfile).when(farmerProfileService).getProfile();

        MvcResult getResult = mockMvc.perform(get(BASE_URL)
                        .with(validJwt(farmerId))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode getJson = objectMapper.readTree(getResult.getResponse().getContentAsString());
        assertThat(getJson.get("data").get("id").asText()).isEqualTo(farmerId.toString());

        // -------------------------------------------------------------
        // Step 5: Update with STALE version token returns 409 Conflict
        // -------------------------------------------------------------
        Long staleVersion = extractedVersion - 5000;
        UpdateFarmerProfileRequestDto staleUpdateReq = new UpdateFarmerProfileRequestDto(
                "Anand K. Rao", null, null, null, null, null, staleVersion
        );

        doThrow(new ConflictException("Optimistic lock conflict: expected version " + staleVersion))
                .when(farmerProfileService).updateProfile(any(UpdateFarmerProfileRequestDto.class));

        mockMvc.perform(put(BASE_URL)
                        .with(validJwt(farmerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staleUpdateReq)))
                .andExpect(status().isConflict());

        // -------------------------------------------------------------
        // Step 6: Update with VALID version token succeeds (200 OK)
        // -------------------------------------------------------------
        OffsetDateTime t2 = t1.plusMinutes(2);
        Long version2 = t2.toInstant().toEpochMilli();

        FarmerProfileResponseDto updatedProfile = new FarmerProfileResponseDto(
                farmerId, "Anand K. Rao", villageId, "Mapakshi",
                mandalId, "Chittoor Urban", districtId, "Chittoor",
                stateId, "Andhra Pradesh", "ACTIVE", true,
                version2, t1, t2
        );

        doReturn(updatedProfile).when(farmerProfileService).updateProfile(any(UpdateFarmerProfileRequestDto.class));

        UpdateFarmerProfileRequestDto validUpdateReq = new UpdateFarmerProfileRequestDto(
                "Anand K. Rao", null, null, null, null, null, extractedVersion
        );

        MvcResult updateResult = mockMvc.perform(put(BASE_URL)
                        .with(validJwt(farmerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUpdateReq)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode updateJson = objectMapper.readTree(updateResult.getResponse().getContentAsString());
        assertThat(updateJson.get("success").asBoolean()).isTrue();
        assertThat(updateJson.get("data").get("fullName").asText()).isEqualTo("Anand K. Rao");
        assertThat(updateJson.get("data").get("version").asLong()).isEqualTo(version2);
    }
}
