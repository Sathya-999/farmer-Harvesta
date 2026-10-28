package com.agriplatform.cultivation;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = {CultivationController.class, ReferenceDataController.class},
    excludeAutoConfiguration = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
    }
)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, SecurityUtils.class})
@ActiveProfiles("test")
@DisplayName("Task 08 — Cultivation Controller Tests")
class CultivationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CultivationService cultivationService;

    private static final String CALLER_FARMER_ID = "11111111-1111-1111-1111-111111111111";

    private CultivationResponseDto sampleResponse(UUID id, UUID plotId) {
        return new CultivationResponseDto(
                id,
                plotId,
                "North Plot",
                UUID.fromString(CALLER_FARMER_ID),
                UUID.randomUUID(),
                "Paddy",
                "Cereals",
                UUID.randomUUID(),
                "Basmati 370",
                UUID.randomUUID(),
                "Kharif",
                (short) 2026,
                new BigDecimal("5.00"),
                LocalDate.of(2026, 6, 15),
                null,
                null,
                null,
                "PLANNED",
                null,
                1,
                "FARMER_APP",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
    }

    // =========================================================================
    // POST /api/v1/cultivations
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/cultivations — 201 Created on valid input")
    void testCreateCultivationSuccess() throws Exception {
        UUID plotId = UUID.randomUUID();
        UUID cropId = UUID.randomUUID();
        UUID seasonId = UUID.randomUUID();
        CreateCultivationRequestDto request = new CreateCultivationRequestDto(
                plotId, cropId, null, seasonId, (short) 2026, new BigDecimal("5.00"), LocalDate.of(2026, 6, 15)
        );

        UUID newId = UUID.randomUUID();
        when(cultivationService.createCultivation(any(CreateCultivationRequestDto.class), any())).thenReturn(sampleResponse(newId, plotId));

        mockMvc.perform(post("/api/v1/cultivations")
                .with(jwt().jwt(j -> j.subject(CALLER_FARMER_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(newId.toString()))
                .andExpect(jsonPath("$.data.plotId").value(plotId.toString()))
                .andExpect(jsonPath("$.data.status").value("PLANNED"));
    }

    @Test
    @DisplayName("POST /api/v1/cultivations — 400 Bad Request on missing required fields")
    void testCreateCultivationMissingFields() throws Exception {
        // Missing plotId, cropId, seasonId, etc.
        String invalidJson = "{\"cultivatedAreaAcres\": 5.0}";

        mockMvc.perform(post("/api/v1/cultivations")
                .with(jwt().jwt(j -> j.subject(CALLER_FARMER_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/cultivations — 400 Bad Request on area <= 0")
    void testCreateCultivationZeroArea() throws Exception {
        CreateCultivationRequestDto request = new CreateCultivationRequestDto(
                UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(),
                (short) 2026, new BigDecimal("0.00"), LocalDate.of(2026, 6, 15)
        );

        mockMvc.perform(post("/api/v1/cultivations")
                .with(jwt().jwt(j -> j.subject(CALLER_FARMER_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error", containsString("cultivatedAreaAcres")));
    }

    @Test
    @DisplayName("POST /api/v1/cultivations — 400 Bad Request on year out of range")
    void testCreateCultivationYearOutOfRange() throws Exception {
        CreateCultivationRequestDto request = new CreateCultivationRequestDto(
                UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(),
                (short) 1999, new BigDecimal("5.00"), LocalDate.of(2026, 6, 15)
        );

        mockMvc.perform(post("/api/v1/cultivations")
                .with(jwt().jwt(j -> j.subject(CALLER_FARMER_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error", containsString("cultivationYear")));
    }

    @Test
    @DisplayName("POST /api/v1/cultivations — 401 Unauthorized without JWT")
    void testCreateCultivationUnauthorized() throws Exception {
        CreateCultivationRequestDto request = new CreateCultivationRequestDto(
                UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(),
                (short) 2026, new BigDecimal("5.00"), LocalDate.of(2026, 6, 15)
        );

        mockMvc.perform(post("/api/v1/cultivations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/cultivations — 403 Forbidden when account is suspended")
    void testCreateCultivationSuspended() throws Exception {
        CreateCultivationRequestDto request = new CreateCultivationRequestDto(
                UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(),
                (short) 2026, new BigDecimal("5.00"), LocalDate.of(2026, 6, 15)
        );

        when(cultivationService.createCultivation(any(), any()))
                .thenThrow(new AccessDeniedException("Account is suspended. Cultivation operations are not permitted."));

        mockMvc.perform(post("/api/v1/cultivations")
                .with(jwt().jwt(j -> j.subject(CALLER_FARMER_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", containsString("suspended")));
    }

    @Test
    @DisplayName("POST /api/v1/cultivations — 409 Conflict on duplicate lineage or capacity conflict")
    void testCreateCultivationConflict() throws Exception {
        CreateCultivationRequestDto request = new CreateCultivationRequestDto(
                UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(),
                (short) 2026, new BigDecimal("5.00"), LocalDate.of(2026, 6, 15)
        );

        when(cultivationService.createCultivation(any(), any()))
                .thenThrow(new ConflictException("An active cultivation cycle already exists for this plot, season, and year."));

        mockMvc.perform(post("/api/v1/cultivations")
                .with(jwt().jwt(j -> j.subject(CALLER_FARMER_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("already exists")));
    }

    // =========================================================================
    // GET /api/v1/cultivations/{id}
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/cultivations/{id} — 200 OK")
    void testGetCultivationSuccess() throws Exception {
        UUID id = UUID.randomUUID();
        when(cultivationService.getCultivation(id)).thenReturn(sampleResponse(id, UUID.randomUUID()));

        mockMvc.perform(get("/api/v1/cultivations/" + id)
                .with(jwt().jwt(j -> j.subject(CALLER_FARMER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(id.toString()));
    }

    @Test
    @DisplayName("GET /api/v1/cultivations/{id} — 404 Not Found")
    void testGetCultivationNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(cultivationService.getCultivation(id))
                .thenThrow(new ResourceNotFoundException("Cultivation intention not found with id: " + id));

        mockMvc.perform(get("/api/v1/cultivations/" + id)
                .with(jwt().jwt(j -> j.subject(CALLER_FARMER_ID))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // =========================================================================
    // GET /api/v1/cultivations (LIST)
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/cultivations — 200 OK with paginated list")
    void testListCultivations() throws Exception {
        UUID id = UUID.randomUUID();
        Page<CultivationResponseDto> page = new PageImpl<>(List.of(sampleResponse(id, UUID.randomUUID())));
        when(cultivationService.listCultivations(any(), any(), any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/cultivations?page=0&size=20&status=PLANNED")
                .with(jwt().jwt(j -> j.subject(CALLER_FARMER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(1)));
    }

    // =========================================================================
    // PUT /api/v1/cultivations/{id}
    // =========================================================================

    @Test
    @DisplayName("PUT /api/v1/cultivations/{id} — 200 OK on valid update")
    void testUpdateCultivationSuccess() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateCultivationRequestDto request = new UpdateCultivationRequestDto(
                UUID.randomUUID(), null, new BigDecimal("6.00"), LocalDate.of(2026, 6, 20), 1
        );

        when(cultivationService.updateCultivation(eq(id), any())).thenReturn(sampleResponse(id, UUID.randomUUID()));

        mockMvc.perform(put("/api/v1/cultivations/" + id)
                .with(jwt().jwt(j -> j.subject(CALLER_FARMER_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // =========================================================================
    // PATCH /api/v1/cultivations/{id}/status
    // =========================================================================

    @Test
    @DisplayName("PATCH /api/v1/cultivations/{id}/status — 200 OK on valid transition")
    void testUpdateStatusSuccess() throws Exception {
        UUID id = UUID.randomUUID();
        CultivationStatusUpdateRequestDto request = new CultivationStatusUpdateRequestDto(
                "STARTED", "Sowing begun", LocalDate.of(2026, 6, 20), new BigDecimal("5.00"), null, 1
        );

        CultivationResponseDto res = sampleResponse(id, UUID.randomUUID());
        when(cultivationService.updateCultivationStatus(eq(id), any())).thenReturn(res);

        mockMvc.perform(patch("/api/v1/cultivations/" + id + "/status")
                .with(jwt().jwt(j -> j.subject(CALLER_FARMER_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // =========================================================================
    // AUDIT EVENTS & HISTORY
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/cultivations/{id}/events — 200 OK")
    void testGetEvents() throws Exception {
        UUID id = UUID.randomUUID();
        CultivationStatusEventDto event = new CultivationStatusEventDto(
                UUID.randomUUID(), id, "PLANNED", "STARTED", UUID.randomUUID(), "FARMER", OffsetDateTime.now(), "Note"
        );
        when(cultivationService.getCultivationEvents(id)).thenReturn(List.of(event));

        mockMvc.perform(get("/api/v1/cultivations/" + id + "/events")
                .with(jwt().jwt(j -> j.subject(CALLER_FARMER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].toStatus").value("STARTED"));
    }

    @Test
    @DisplayName("GET /api/v1/cultivations/{id}/history — 200 OK")
    void testGetHistory() throws Exception {
        UUID id = UUID.randomUUID();
        CultivationHistoryDto history = new CultivationHistoryDto(
                UUID.randomUUID(), id, 1, OffsetDateTime.now(), UUID.randomUUID(), UUID.fromString(CALLER_FARMER_ID),
                UUID.randomUUID(), null, UUID.randomUUID(), (short) 2026, new BigDecimal("5.00"),
                LocalDate.of(2026, 6, 15), null, null, null, "PLANNED", null,
                "FARMER_APP", UUID.fromString(CALLER_FARMER_ID), "FARMER", "FIELD_UPDATE", List.of("crop_id"), null
        );
        when(cultivationService.getCultivationHistory(id)).thenReturn(List.of(history));

        mockMvc.perform(get("/api/v1/cultivations/" + id + "/history")
                .with(jwt().jwt(j -> j.subject(CALLER_FARMER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].version").value(1));
    }

    // =========================================================================
    // REFERENCE DATA ENDPOINTS
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/crops, /crops/{id}/varieties, /seasons — 200 OK")
    void testReferenceDataEndpoints() throws Exception {
        UUID cropId = UUID.randomUUID();
        when(cultivationService.listActiveCrops()).thenReturn(List.of(new CropResponseDto(cropId, "Paddy", "Cereals", true)));
        when(cultivationService.listActiveVarietiesForCrop(cropId)).thenReturn(List.of(new CropVarietyResponseDto(UUID.randomUUID(), cropId, "Basmati", true)));
        when(cultivationService.listActiveSeasons()).thenReturn(List.of(new SeasonResponseDto(UUID.randomUUID(), "Kharif", (short) 6, (short) 1, "Monsoon", true)));

        mockMvc.perform(get("/api/v1/crops")
                .with(jwt().jwt(j -> j.subject(CALLER_FARMER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Paddy"));

        mockMvc.perform(get("/api/v1/crops/" + cropId + "/varieties")
                .with(jwt().jwt(j -> j.subject(CALLER_FARMER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Basmati"));

        mockMvc.perform(get("/api/v1/seasons")
                .with(jwt().jwt(j -> j.subject(CALLER_FARMER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Kharif"));
    }
}
