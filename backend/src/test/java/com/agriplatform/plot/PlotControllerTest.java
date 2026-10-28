package com.agriplatform.plot;

import com.agriplatform.common.config.SecurityConfig;
import com.agriplatform.common.exception.BadRequestException;
import com.agriplatform.common.exception.ConflictException;
import com.agriplatform.common.exception.GlobalExceptionHandler;
import com.agriplatform.common.exception.ResourceNotFoundException;
import com.agriplatform.common.util.SecurityUtils;
import com.agriplatform.plot.controller.PlotController;
import com.agriplatform.plot.dto.CreatePlotRequestDto;
import com.agriplatform.plot.dto.PlotResponseDto;
import com.agriplatform.plot.dto.UpdatePlotRequestDto;
import com.agriplatform.plot.service.PlotService;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller-layer tests for {@link PlotController}.
 *
 * <p>Tests HTTP semantics, authentication enforcement, request validation, error mapping,
 * and that village GPS coordinates do not appear in any response.
 */
@WebMvcTest(
    controllers = PlotController.class,
    excludeAutoConfiguration = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
    }
)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, SecurityUtils.class})
@ActiveProfiles("test")
@DisplayName("Task 07 — Plot Controller Tests")
class PlotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PlotService plotService;

    private static final String BASE_URL = "/api/v1/plots";

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
            .JwtRequestPostProcessor validJwt(UUID userId) {
        return jwt().jwt(builder -> builder
                .subject(userId.toString())
                .issuer("http://localhost/test-issuer")
                .claim("aud", List.of("authenticated"))
                .claim("role", "authenticated")
                .expiresAt(Instant.now().plusSeconds(3600))
        );
    }

    private PlotResponseDto samplePlotDto(UUID id, UUID farmerId, UUID villageId) {
        return new PlotResponseDto(
                id, farmerId, villageId,
                "Mapakshi", "Chittoor Mandal", "Chittoor", "Andhra Pradesh",
                "North Field", new BigDecimal("2.5000"), true,
                "2026-10-02T18:36:26.118966Z",
                OffsetDateTime.parse("2026-10-02T18:36:26.118966Z"),
                OffsetDateTime.parse("2026-10-02T18:36:26.118966Z")
        );
    }

    // =========================================================================
    // Authentication enforcement — all endpoints require JWT
    // =========================================================================

    @Test
    @DisplayName("56a. POST /api/v1/plots without JWT → 401")
    void post_withoutJwt_returns401() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"villageId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("56b. GET /api/v1/plots without JWT → 401")
    void get_list_withoutJwt_returns401() throws Exception {
        mockMvc.perform(get(BASE_URL)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("56c. GET /api/v1/plots/{id} without JWT → 401")
    void get_single_withoutJwt_returns401() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("56d. PUT /api/v1/plots/{id} without JWT → 401")
    void put_withoutJwt_returns401() throws Exception {
        mockMvc.perform(put(BASE_URL + "/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":\"token\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("56e. DELETE /api/v1/plots/{id} without JWT → 401")
    void delete_withoutJwt_returns401() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("56f. PATCH /api/v1/plots/{id}/reactivate without JWT → 401")
    void reactivate_withoutJwt_returns401() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/" + UUID.randomUUID() + "/reactivate"))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // Create Plot
    // =========================================================================

    @Test
    @DisplayName("1. Create plot with valid request → 201 Created")
    void createPlot_validRequest_returns201() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID villageId = UUID.randomUUID();
        PlotResponseDto dto = samplePlotDto(UUID.randomUUID(), userId, villageId);
        when(plotService.createPlot(any(), isNull())).thenReturn(dto);

        CreatePlotRequestDto req = new CreatePlotRequestDto(villageId, "North Field", new BigDecimal("2.5000"));
        mockMvc.perform(post(BASE_URL)
                        .with(validJwt(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.label").value("North Field"))
                .andExpect(jsonPath("$.data.farmerId").value(userId.toString()));
    }

    @Test
    @DisplayName("2. Create plot with null label → 201 Created (label is optional)")
    void createPlot_nullLabel_returns201() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID villageId = UUID.randomUUID();
        PlotResponseDto dto = new PlotResponseDto(
                UUID.randomUUID(), userId, villageId, "V", "M", "D", "S",
                null, null, true, "tok",
                OffsetDateTime.now(), OffsetDateTime.now());
        when(plotService.createPlot(any(), isNull())).thenReturn(dto);

        CreatePlotRequestDto req = new CreatePlotRequestDto(villageId, null, null);
        mockMvc.perform(post(BASE_URL)
                        .with(validJwt(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("8. Zero area → 400 Bad Request")
    void createPlot_zeroArea_returns400() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID villageId = UUID.randomUUID();

        // @DecimalMin(exclusive=true) constraint
        mockMvc.perform(post(BASE_URL)
                        .with(validJwt(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"villageId\":\"" + villageId + "\",\"totalAreaAcres\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("9. Negative area → 400 Bad Request")
    void createPlot_negativeArea_returns400() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID villageId = UUID.randomUUID();

        mockMvc.perform(post(BASE_URL)
                        .with(validJwt(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"villageId\":\"" + villageId + "\",\"totalAreaAcres\":-1.5}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("46. Malformed UUID in path → 400 Bad Request")
    void getPlot_malformedUuid_returns400() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(get(BASE_URL + "/not-a-uuid").with(validJwt(userId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("17. SUSPENDED farmer → 403 Forbidden")
    void createPlot_suspendedFarmer_returns403() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID villageId = UUID.randomUUID();
        when(plotService.createPlot(any(), any())).thenThrow(new AccessDeniedException("Account is suspended"));

        CreatePlotRequestDto req = new CreatePlotRequestDto(villageId, "Field", null);
        mockMvc.perform(post(BASE_URL)
                        .with(validJwt(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("18. DELETED farmer → 404 Not Found")
    void createPlot_deletedFarmer_returns404() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID villageId = UUID.randomUUID();
        when(plotService.createPlot(any(), any())).thenThrow(new ResourceNotFoundException("Farmer profile not found"));

        CreatePlotRequestDto req = new CreatePlotRequestDto(villageId, "Field", null);
        mockMvc.perform(post(BASE_URL)
                        .with(validJwt(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("45. DB duplicate label → 409 Conflict")
    void createPlot_duplicateLabel_returns409() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID villageId = UUID.randomUUID();
        when(plotService.createPlot(any(), any()))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException(
                        "duplicate key value violates unique constraint \"ux_plots_farmer_label_ci\""));

        CreatePlotRequestDto req = new CreatePlotRequestDto(villageId, "North Field", null);
        mockMvc.perform(post(BASE_URL)
                        .with(validJwt(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    // =========================================================================
    // Idempotency header
    // =========================================================================

    @Test
    @DisplayName("40. Idempotency-Key header is passed to service")
    void createPlot_idempotencyKeyPassed() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID villageId = UUID.randomUUID();
        PlotResponseDto dto = samplePlotDto(UUID.randomUUID(), userId, villageId);
        when(plotService.createPlot(any(), eq("test-key-xyz"))).thenReturn(dto);

        CreatePlotRequestDto req = new CreatePlotRequestDto(villageId, "Field", null);
        mockMvc.perform(post(BASE_URL)
                        .with(validJwt(userId))
                        .header("Idempotency-Key", "test-key-xyz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        verify(plotService).createPlot(any(), eq("test-key-xyz"));
    }

    // =========================================================================
    // GET single plot
    // =========================================================================

    @Test
    @DisplayName("getPlot → 200 OK with data")
    void getPlot_found_returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID plotId = UUID.randomUUID();
        UUID villageId = UUID.randomUUID();
        PlotResponseDto dto = samplePlotDto(plotId, userId, villageId);
        when(plotService.getPlot(plotId)).thenReturn(dto);

        mockMvc.perform(get(BASE_URL + "/" + plotId).with(validJwt(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(plotId.toString()))
                .andExpect(jsonPath("$.data.version").value("2026-10-02T18:36:26.118966Z"));
    }

    @Test
    @DisplayName("getPlot not found → 404")
    void getPlot_notFound_returns404() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID plotId = UUID.randomUUID();
        when(plotService.getPlot(plotId)).thenThrow(new ResourceNotFoundException("Plot not found"));

        mockMvc.perform(get(BASE_URL + "/" + plotId).with(validJwt(userId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("52. Plot response does NOT contain latitude or longitude fields")
    void plotResponse_doesNotContainLatLon() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID plotId = UUID.randomUUID();
        UUID villageId = UUID.randomUUID();
        PlotResponseDto dto = samplePlotDto(plotId, userId, villageId);
        when(plotService.getPlot(plotId)).thenReturn(dto);

        mockMvc.perform(get(BASE_URL + "/" + plotId).with(validJwt(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.latitude").doesNotExist())
                .andExpect(jsonPath("$.data.longitude").doesNotExist());
    }

    // =========================================================================
    // List plots
    // =========================================================================

    @Test
    @DisplayName("49. List plots is paginated")
    void listPlots_paginated_returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID villageId = UUID.randomUUID();
        PlotResponseDto dto = samplePlotDto(UUID.randomUUID(), userId, villageId);
        Page<PlotResponseDto> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1);
        when(plotService.listPlots(any(), eq("ACTIVE"))).thenReturn(page);

        mockMvc.perform(get(BASE_URL).with(validJwt(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)));
    }

    @Test
    @DisplayName("51. Status filtering passed to service")
    void listPlots_statusFilter_passedToService() throws Exception {
        UUID userId = UUID.randomUUID();
        Page<PlotResponseDto> emptyPage = new PageImpl<>(List.of());
        when(plotService.listPlots(any(), eq("INACTIVE"))).thenReturn(emptyPage);

        mockMvc.perform(get(BASE_URL + "?status=INACTIVE").with(validJwt(userId)))
                .andExpect(status().isOk());

        verify(plotService).listPlots(any(), eq("INACTIVE"));
    }

    // =========================================================================
    // Update plot
    // =========================================================================

    @Test
    @DisplayName("24. Missing expectedVersion → 400")
    void updatePlot_missingExpectedVersion_returns400() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID plotId = UUID.randomUUID();

        mockMvc.perform(put(BASE_URL + "/" + plotId)
                        .with(validJwt(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"New Label\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("26. Stale expectedVersion → 409 Conflict")
    void updatePlot_staleVersion_returns409() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID plotId = UUID.randomUUID();
        when(plotService.updatePlot(eq(plotId), any()))
                .thenThrow(new ConflictException("Plot update conflict"));

        UpdatePlotRequestDto req = new UpdatePlotRequestDto("New Label", null, "stale-token");
        mockMvc.perform(put(BASE_URL + "/" + plotId)
                        .with(validJwt(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    // =========================================================================
    // Deactivate / Reactivate
    // =========================================================================

    @Test
    @DisplayName("30. DELETE /api/v1/plots/{id} → 204 No Content (soft delete)")
    void deactivatePlot_returns204() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID plotId = UUID.randomUUID();
        // PlotService.deactivatePlot is void
        doNothing().when(plotService).deactivatePlot(any());

        mockMvc.perform(delete(BASE_URL + "/" + plotId).with(validJwt(userId)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("31. Deactivate with PLANNED cultivation → 409 Conflict (from DB trigger)")
    void deactivatePlot_plannedCultivation_returns409() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID plotId = UUID.randomUUID();
        doThrow(new org.springframework.dao.DataIntegrityViolationException(
                        "CANNOT_DEACTIVATE_PLOT_WITH_ACTIVE_CULTIVATION"))
                .when(plotService).deactivatePlot(plotId);

        mockMvc.perform(delete(BASE_URL + "/" + plotId).with(validJwt(userId)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("34. PATCH /api/v1/plots/{id}/reactivate → 200 OK")
    void reactivatePlot_returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID plotId = UUID.randomUUID();
        PlotResponseDto dto = samplePlotDto(plotId, userId, UUID.randomUUID());
        when(plotService.reactivatePlot(plotId)).thenReturn(dto);

        mockMvc.perform(patch(BASE_URL + "/" + plotId + "/reactivate").with(validJwt(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isActive").value(true));
    }
}
