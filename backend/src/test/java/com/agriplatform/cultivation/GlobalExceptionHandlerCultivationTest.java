package com.agriplatform.cultivation;

import com.agriplatform.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Task 08 — GlobalExceptionHandler Cultivation Tests")
class GlobalExceptionHandlerCultivationTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("ux_active_cultivation_cycle maps to user-friendly message and HTTP 409")
    void testUxActiveCultivationCycle() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "duplicate key value violates unique constraint \"ux_active_cultivation_cycle\"");
        var response = handler.handleDataIntegrityViolation(ex);
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().error()).contains("An active cultivation cycle already exists for this plot, season, and year.");
    }

    @Test
    @DisplayName("cultivation_variety_matches_crop maps to variety mismatch message")
    void testVarietyMatchesCrop() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "violates foreign key constraint \"cultivation_variety_matches_crop\"");
        var response = handler.handleDataIntegrityViolation(ex);
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().error()).contains("The selected variety does not belong to the selected crop.");
    }

    @Test
    @DisplayName("Trigger error ILLEGAL_STATUS_TRANSITION maps to friendly message")
    void testIllegalStatusTransition() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "ERROR: ILLEGAL_STATUS_TRANSITION: PLANNED -> ABANDONED");
        var response = handler.handleDataIntegrityViolation(ex);
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().error()).contains("Invalid status transition for cultivation cycle.");
    }

    @Test
    @DisplayName("Trigger error TERMINAL_INTENTION_IS_NOT_EDITABLE maps to friendly message")
    void testTerminalIntentionNotEditable() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "ERROR: TERMINAL_INTENTION_IS_NOT_EDITABLE");
        var response = handler.handleDataIntegrityViolation(ex);
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().error()).contains("Terminal cultivation cycle cannot be edited.");
    }

    @Test
    @DisplayName("Trigger error CULTIVATION_CYCLE_IDENTITY_IMMUTABLE_USE_NEW_INTENTION maps to friendly message")
    void testIdentityImmutable() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "ERROR: CULTIVATION_CYCLE_IDENTITY_IMMUTABLE_USE_NEW_INTENTION");
        var response = handler.handleDataIntegrityViolation(ex);
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().error()).contains("Cultivation cycle identity (plot, season, year) is immutable.");
    }

    @Test
    @DisplayName("Trigger error PLOT_INACTIVE maps to friendly message")
    void testPlotInactive() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "ERROR: PLOT_INACTIVE");
        var response = handler.handleDataIntegrityViolation(ex);
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().error()).contains("Cannot create cultivation on an inactive plot.");
    }

    @Test
    @DisplayName("Trigger error ACTOR_CONTEXT_REQUIRED maps to friendly message")
    void testActorContextRequired() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "ERROR: ACTOR_CONTEXT_REQUIRED");
        var response = handler.handleDataIntegrityViolation(ex);
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().error()).contains("Actor context is required for this operation.");
    }
}
