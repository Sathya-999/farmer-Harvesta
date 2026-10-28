package com.agriplatform.farmer;

import com.agriplatform.common.config.SecurityConfig;
import com.agriplatform.common.exception.BadRequestException;
import com.agriplatform.common.exception.ConflictException;
import com.agriplatform.common.exception.GlobalExceptionHandler;
import com.agriplatform.common.exception.ResourceNotFoundException;
import com.agriplatform.common.util.SecurityUtils;
import com.agriplatform.farmer.controller.FarmerProfileController;
import com.agriplatform.farmer.dto.CreateFarmerProfileRequestDto;
import com.agriplatform.farmer.dto.FarmerProfileResponseDto;
import com.agriplatform.farmer.dto.UpdateFarmerProfileRequestDto;
import com.agriplatform.farmer.service.FarmerProfileService;
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

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
@DisplayName("Task 06 — Farmer Profile Controller Tests")
class FarmerProfileControllerTest {

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

    // =========================================================================
    // Authentication enforcement (401 without JWT)
    // =========================================================================

    @Test
    @DisplayName("1. Unauthenticated GET /api/v1/farmers/me returns 401")
    void get_without_token_returns_401() throws Exception {
        mockMvc.perform(get(BASE_URL).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("2. Unauthenticated POST /api/v1/farmers/me returns 401")
    void post_without_token_returns_401() throws Exception {
        CreateFarmerProfileRequestDto req = new CreateFarmerProfileRequestDto(
                "Ramesh Kumar", UUID.randomUUID(), null, null, null
        );

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("3. Unauthenticated PUT /api/v1/farmers/me returns 401")
    void put_without_token_returns_401() throws Exception {
        UpdateFarmerProfileRequestDto req = new UpdateFarmerProfileRequestDto(
                "Ramesh Kumar", null, null, null, null, null, 1000L
        );

        mockMvc.perform(put(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // GET /api/v1/farmers/me
    // =========================================================================

    @Test
    @DisplayName("4. Authenticated GET /api/v1/farmers/me returns 200 with profile")
    void get_profile_success_returns_200() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID villageId = UUID.randomUUID();
        UUID mandalId = UUID.randomUUID();
        UUID districtId = UUID.randomUUID();
        UUID stateId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        FarmerProfileResponseDto dto = new FarmerProfileResponseDto(
                userId, "Ramesh Kumar", villageId, "Mapakshi",
                mandalId, "Chittoor Urban", districtId, "Chittoor",
                stateId, "Andhra Pradesh", "ACTIVE", true,
                now.toInstant().toEpochMilli(), now, now
        );

        when(farmerProfileService.getProfile()).thenReturn(dto);

        mockMvc.perform(get(BASE_URL)
                        .with(validJwt(userId))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(userId.toString()))
                .andExpect(jsonPath("$.data.fullName").value("Ramesh Kumar"))
                .andExpect(jsonPath("$.data.villageName").value("Mapakshi"))
                .andExpect(jsonPath("$.data.mandalName").value("Chittoor Urban"))
                .andExpect(jsonPath("$.data.districtName").value("Chittoor"))
                .andExpect(jsonPath("$.data.stateName").value("Andhra Pradesh"))
                .andExpect(jsonPath("$.data.onboardingCompleted").value(true))
                .andExpect(jsonPath("$.data.version").value(now.toInstant().toEpochMilli()));
    }

    @Test
    @DisplayName("5. GET /api/v1/farmers/me returns 404 when profile not found")
    void get_profile_not_found_returns_404() throws Exception {
        UUID userId = UUID.randomUUID();
        when(farmerProfileService.getProfile())
                .thenThrow(new ResourceNotFoundException("Farmer profile not found for user: " + userId));

        mockMvc.perform(get(BASE_URL)
                        .with(validJwt(userId))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Farmer profile not found for user: " + userId));
    }

    // =========================================================================
    // POST /api/v1/farmers/me (Initialization)
    // =========================================================================

    @Test
    @DisplayName("6. Authenticated POST /api/v1/farmers/me returns 201 Created")
    void post_profile_init_success_returns_201() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID villageId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        CreateFarmerProfileRequestDto req = new CreateFarmerProfileRequestDto(
                "Suresh Patel", villageId, null, null, null
        );

        FarmerProfileResponseDto dto = new FarmerProfileResponseDto(
                userId, "Suresh Patel", villageId, "Mapakshi",
                UUID.randomUUID(), "Chittoor Urban", UUID.randomUUID(), "Chittoor",
                UUID.randomUUID(), "Andhra Pradesh", "ACTIVE", true,
                now.toInstant().toEpochMilli(), now, now
        );

        when(farmerProfileService.initProfile(any(CreateFarmerProfileRequestDto.class))).thenReturn(dto);

        mockMvc.perform(post(BASE_URL)
                        .with(validJwt(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(userId.toString()))
                .andExpect(jsonPath("$.data.fullName").value("Suresh Patel"))
                .andExpect(jsonPath("$.data.onboardingCompleted").value(true));
    }

    @Test
    @DisplayName("7. Duplicate POST /api/v1/farmers/me returns 409 Conflict")
    void post_profile_duplicate_returns_409() throws Exception {
        UUID userId = UUID.randomUUID();
        CreateFarmerProfileRequestDto req = new CreateFarmerProfileRequestDto(
                "Suresh Patel", UUID.randomUUID(), null, null, null
        );

        when(farmerProfileService.initProfile(any(CreateFarmerProfileRequestDto.class)))
                .thenThrow(new ConflictException("Farmer profile has already been initialized"));

        mockMvc.perform(post(BASE_URL)
                        .with(validJwt(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Farmer profile has already been initialized"));
    }

    @Test
    @DisplayName("8. POST /api/v1/farmers/me with blank name returns 400 Bad Request")
    void post_profile_blank_name_returns_400() throws Exception {
        UUID userId = UUID.randomUUID();
        CreateFarmerProfileRequestDto req = new CreateFarmerProfileRequestDto(
                "  ", UUID.randomUUID(), null, null, null
        );

        mockMvc.perform(post(BASE_URL)
                        .with(validJwt(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // =========================================================================
    // PUT /api/v1/farmers/me (Update & Concurrency)
    // =========================================================================

    @Test
    @DisplayName("9. Authenticated PUT /api/v1/farmers/me returns 200 OK with matching version")
    void put_profile_success_returns_200() throws Exception {
        UUID userId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        Long version = now.toInstant().toEpochMilli();

        UpdateFarmerProfileRequestDto req = new UpdateFarmerProfileRequestDto(
                "Ramesh Sharma", null, null, null, null, null, version
        );

        FarmerProfileResponseDto dto = new FarmerProfileResponseDto(
                userId, "Ramesh Sharma", UUID.randomUUID(), "Mapakshi",
                UUID.randomUUID(), "Chittoor Urban", UUID.randomUUID(), "Chittoor",
                UUID.randomUUID(), "Andhra Pradesh", "ACTIVE", true,
                version + 100, now, now.plusSeconds(1)
        );

        when(farmerProfileService.updateProfile(any(UpdateFarmerProfileRequestDto.class))).thenReturn(dto);

        mockMvc.perform(put(BASE_URL)
                        .with(validJwt(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Ramesh Sharma"));
    }

    @Test
    @DisplayName("10. PUT /api/v1/farmers/me with stale version returns 409 Conflict")
    void put_profile_stale_version_returns_409() throws Exception {
        UUID userId = UUID.randomUUID();
        UpdateFarmerProfileRequestDto req = new UpdateFarmerProfileRequestDto(
                "Ramesh Sharma", null, null, null, null, null, 1000L
        );

        when(farmerProfileService.updateProfile(any(UpdateFarmerProfileRequestDto.class)))
                .thenThrow(new ConflictException("Optimistic lock conflict: expected version 1000 but current is 2000"));

        mockMvc.perform(put(BASE_URL)
                        .with(validJwt(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value(containsString("Optimistic lock conflict")));
    }

    @Test
    @DisplayName("11. Geography hierarchy mismatch returns 400 Bad Request")
    void put_profile_hierarchy_mismatch_returns_400() throws Exception {
        UUID userId = UUID.randomUUID();
        UpdateFarmerProfileRequestDto req = new UpdateFarmerProfileRequestDto(
                null, UUID.randomUUID(), UUID.randomUUID(), null, null, null, 1000L
        );

        when(farmerProfileService.updateProfile(any(UpdateFarmerProfileRequestDto.class)))
                .thenThrow(new BadRequestException("Provided mandalId does not match village mandal"));

        mockMvc.perform(put(BASE_URL)
                        .with(validJwt(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value(containsString("does not match village mandal")));
    }

    @Test
    @DisplayName("12. IDOR Protection: Query parameters or body attempts to specify farmer_id are ignored")
    void idor_attempt_ignored_and_uses_jwt_identity() throws Exception {
        UUID trueUserId = UUID.randomUUID();
        UUID spoofedUserId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        FarmerProfileResponseDto dto = new FarmerProfileResponseDto(
                trueUserId, "True Farmer", UUID.randomUUID(), "Mapakshi",
                UUID.randomUUID(), "Chittoor Urban", UUID.randomUUID(), "Chittoor",
                UUID.randomUUID(), "Andhra Pradesh", "ACTIVE", true,
                now.toInstant().toEpochMilli(), now, now
        );

        when(farmerProfileService.getProfile()).thenReturn(dto);

        // Attacker attempts to pass ?farmer_id=<spoofedUserId> or body with spoofed id
        mockMvc.perform(get(BASE_URL)
                        .param("farmer_id", spoofedUserId.toString())
                        .with(validJwt(trueUserId))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(trueUserId.toString()));
    }
}
