package com.agriplatform.plot;

import com.agriplatform.common.exception.GlobalExceptionHandler;
import com.agriplatform.plot.dto.PlotResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the Global Exception Handler additions made for Task 07 (P2).
 * Verifies DataIntegrityViolationException mapping to 409, message sanitization.
 */
@DisplayName("Task 07 — GlobalExceptionHandler P2 Tests")
class GlobalExceptionHandlerPlotTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Test
    @DisplayName("45. DataIntegrityViolationException → HTTP 409 (not 500)")
    void dataIntegrityViolation_maps409() {
        org.springframework.dao.DataIntegrityViolationException ex =
                new org.springframework.dao.DataIntegrityViolationException(
                        "duplicate key value violates unique constraint \"ux_plots_farmer_label_ci\"");

        var response = handler.handleDataIntegrityViolation(ex);
        assertThat(response.getStatusCode().value()).isEqualTo(409);
    }

    @Test
    @DisplayName("45. Response body does not leak raw SQL constraint details")
    void dataIntegrityViolation_doesNotLeakSql() {
        org.springframework.dao.DataIntegrityViolationException ex =
                new org.springframework.dao.DataIntegrityViolationException(
                        "duplicate key value violates unique constraint \"ux_plots_farmer_label_ci\"");

        var response = handler.handleDataIntegrityViolation(ex);
        assertThat(response.getBody()).isNotNull();
        String errorMsg = response.getBody().error();
        assertThat(errorMsg).doesNotContain("duplicate key");
        assertThat(errorMsg).doesNotContain("ux_plots_farmer_label_ci");
        assertThat(errorMsg).doesNotContain("violates");
    }

    @Test
    @DisplayName("45. ux_plots_farmer_label_ci → user-friendly label message")
    void dataIntegrityViolation_labelConstraint_friendlyMessage() {
        org.springframework.dao.DataIntegrityViolationException ex =
                new org.springframework.dao.DataIntegrityViolationException(
                        "duplicate key value violates unique constraint \"ux_plots_farmer_label_ci\"");

        var response = handler.handleDataIntegrityViolation(ex);
        assertThat(response.getBody().error()).contains("label already exists");
    }

    @Test
    @DisplayName("45. Existing exception mappings still return correct HTTP codes")
    void existingMappings_unchanged() {
        // 404 still works
        var notFound = handler.handleResourceNotFound(
                new com.agriplatform.common.exception.ResourceNotFoundException("test"));
        assertThat(notFound.getStatusCode().value()).isEqualTo(404);

        // 409 ConflictException still works
        var conflict = handler.handleConflict(
                new com.agriplatform.common.exception.ConflictException("test conflict"));
        assertThat(conflict.getStatusCode().value()).isEqualTo(409);

        // 400 BadRequest still works
        var badRequest = handler.handleBadRequest(
                new com.agriplatform.common.exception.BadRequestException("test bad request"));
        assertThat(badRequest.getStatusCode().value()).isEqualTo(400);

        // 403 AccessDenied still works
        var denied = handler.handleAccessDenied(
                new org.springframework.security.access.AccessDeniedException("forbidden"));
        assertThat(denied.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    @DisplayName("52. PlotResponseDto serialization contains no latitude or longitude keys")
    void plotResponseDto_serialization_noGpsKeys() throws Exception {
        PlotResponseDto dto = new PlotResponseDto(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Mapakshi", "Chittoor Mandal", "Chittoor", "Andhra Pradesh",
                "North Field", new BigDecimal("2.5000"), true,
                "2026-10-02T18:36:26.118966Z",
                OffsetDateTime.parse("2026-10-02T18:36:26.118966Z"),
                OffsetDateTime.parse("2026-10-02T18:36:26.118966Z")
        );

        String json = mapper.writeValueAsString(dto);
        assertThat(json).doesNotContain("latitude");
        assertThat(json).doesNotContain("longitude");
        assertThat(json).doesNotContain("lat");
        assertThat(json).doesNotContain("lon");
    }
}
