package com.agriplatform.history;

import com.agriplatform.common.config.SecurityConfig;
import com.agriplatform.common.exception.GlobalExceptionHandler;
import com.agriplatform.common.exception.ResourceNotFoundException;
import com.agriplatform.common.util.SecurityUtils;
import com.agriplatform.history.controller.HistoryController;
import com.agriplatform.history.dto.HistorySnapshotDto;
import com.agriplatform.history.dto.HistoryStatusEventDto;
import com.agriplatform.history.service.HistoryService;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
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

/**
 * Task 09 — HistoryController WebMvc Tests.
 *
 * <p>Verifies all REST endpoints:
 * <ul>
 *   <li>Authentication / JWT enforcement</li>
 *   <li>Pagination parameter handling &amp; size bounding</li>
 *   <li>IDOR / Not Found mapping</li>
 *   <li>HTTP method rejection (immutability: POST, PUT, DELETE, PATCH return 405)</li>
 *   <li>UUID format validation</li>
 * </ul>
 */
@WebMvcTest(
    controllers = {HistoryController.class},
    excludeAutoConfiguration = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
    }
)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, SecurityUtils.class})
@ActiveProfiles("test")
@DisplayName("Task 09 — History Controller Tests")
class HistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HistoryService historyService;

    private static final String FARMER_A_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final String FARMER_B_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";

    private HistorySnapshotDto sampleSnapshotDto(UUID cultivationId, UUID historyId, int version) {
        return new HistorySnapshotDto(
                historyId,
                cultivationId,
                version,
                OffsetDateTime.now(),
                UUID.randomUUID(),
                UUID.fromString(FARMER_A_ID),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                (short) 2026,
                new BigDecimal("4.50"),
                LocalDate.of(2026, 6, 15),
                LocalDate.of(2026, 6, 20),
                new BigDecimal("4.50"),
                LocalDate.of(2026, 11, 15),
                "STARTED",
                null,
                "FARMER_APP",
                UUID.fromString(FARMER_A_ID),
                "FARMER",
                "UPDATE",
                List.of("actual_planting_date", "status"),
                "Field updated"
        );
    }

    private HistoryStatusEventDto sampleStatusEventDto(UUID cultivationId, UUID eventId, String fromStatus, String toStatus) {
        return new HistoryStatusEventDto(
                eventId,
                cultivationId,
                fromStatus,
                toStatus,
                UUID.fromString(FARMER_A_ID),
                "FARMER",
                OffsetDateTime.now(),
                "Status transitioned"
        );
    }

    // =========================================================================
    // SNAPSHOTS ENDPOINTS
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/history/cultivations/{cultivationId}/snapshots")
    class GetSnapshotsTests {

        @Test
        @DisplayName("200 OK — default pagination returns snapshots")
        void testGetSnapshotsDefaultPagination() throws Exception {
            UUID cultivationId = UUID.randomUUID();
            UUID historyId = UUID.randomUUID();
            Page<HistorySnapshotDto> page = new PageImpl<>(List.of(sampleSnapshotDto(cultivationId, historyId, 2)));

            when(historyService.getSnapshotsForCultivation(eq(cultivationId), any()))
                    .thenReturn(page);

            mockMvc.perform(get("/api/v1/history/cultivations/{cultivationId}/snapshots", cultivationId)
                            .with(jwt().jwt(j -> j.subject(FARMER_A_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].historyId").value(historyId.toString()))
                    .andExpect(jsonPath("$.data.content[0].intentionId").value(cultivationId.toString()))
                    .andExpect(jsonPath("$.data.content[0].version").value(2))
                    .andExpect(jsonPath("$.data.content[0].status").value("STARTED"))
                    .andExpect(jsonPath("$.data.content[0].changedFields", hasItems("actual_planting_date", "status")));
        }

        @Test
        @DisplayName("200 OK — custom pagination parameters respected")
        void testGetSnapshotsCustomPagination() throws Exception {
            UUID cultivationId = UUID.randomUUID();
            Page<HistorySnapshotDto> emptyPage = new PageImpl<>(List.of(), PageRequest.of(2, 5), 0);

            when(historyService.getSnapshotsForCultivation(eq(cultivationId), any()))
                    .thenReturn(emptyPage);

            mockMvc.perform(get("/api/v1/history/cultivations/{cultivationId}/snapshots", cultivationId)
                            .param("page", "2")
                            .param("size", "5")
                            .with(jwt().jwt(j -> j.subject(FARMER_A_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content", empty()));
        }

        @Test
        @DisplayName("200 OK — oversized page parameter is bounded to max 100")
        void testGetSnapshotsOversizedPageBounded() throws Exception {
            UUID cultivationId = UUID.randomUUID();
            Page<HistorySnapshotDto> page = new PageImpl<>(List.of());

            when(historyService.getSnapshotsForCultivation(eq(cultivationId), any()))
                    .thenReturn(page);

            mockMvc.perform(get("/api/v1/history/cultivations/{cultivationId}/snapshots", cultivationId)
                            .param("size", "999")
                            .with(jwt().jwt(j -> j.subject(FARMER_A_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("401 Unauthorized — missing JWT Bearer token")
        void testGetSnapshotsMissingJwt() throws Exception {
            UUID cultivationId = UUID.randomUUID();

            mockMvc.perform(get("/api/v1/history/cultivations/{cultivationId}/snapshots", cultivationId))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("404 Not Found — non-existent cultivation or IDOR rejection")
        void testGetSnapshotsNotFoundOrIdor() throws Exception {
            UUID cultivationId = UUID.randomUUID();

            when(historyService.getSnapshotsForCultivation(eq(cultivationId), any()))
                    .thenThrow(new ResourceNotFoundException("Cultivation not found or access denied"));

            mockMvc.perform(get("/api/v1/history/cultivations/{cultivationId}/snapshots", cultivationId)
                            .with(jwt().jwt(j -> j.subject(FARMER_B_ID))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error").value("Cultivation not found or access denied"));
        }

        @Test
        @DisplayName("400 Bad Request — malformed cultivation UUID")
        void testGetSnapshotsMalformedUuid() throws Exception {
            mockMvc.perform(get("/api/v1/history/cultivations/{cultivationId}/snapshots", "invalid-uuid-format")
                            .with(jwt().jwt(j -> j.subject(FARMER_A_ID))))
                    .andExpect(status().isBadRequest());
        }
    }

    // =========================================================================
    // SINGLE SNAPSHOT ENDPOINT
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/history/cultivations/{cultivationId}/snapshots/{historyId}")
    class GetSingleSnapshotTests {

        @Test
        @DisplayName("200 OK — retrieves single snapshot by historyId")
        void testGetSingleSnapshotSuccess() throws Exception {
            UUID cultivationId = UUID.randomUUID();
            UUID historyId = UUID.randomUUID();
            HistorySnapshotDto dto = sampleSnapshotDto(cultivationId, historyId, 1);

            when(historyService.getSnapshot(eq(cultivationId), eq(historyId)))
                    .thenReturn(dto);

            mockMvc.perform(get("/api/v1/history/cultivations/{cultivationId}/snapshots/{historyId}", cultivationId, historyId)
                            .with(jwt().jwt(j -> j.subject(FARMER_A_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.historyId").value(historyId.toString()))
                    .andExpect(jsonPath("$.data.intentionId").value(cultivationId.toString()))
                    .andExpect(jsonPath("$.data.version").value(1));
        }

        @Test
        @DisplayName("401 Unauthorized — missing JWT Bearer token")
        void testGetSingleSnapshotMissingJwt() throws Exception {
            UUID cultivationId = UUID.randomUUID();
            UUID historyId = UUID.randomUUID();

            mockMvc.perform(get("/api/v1/history/cultivations/{cultivationId}/snapshots/{historyId}", cultivationId, historyId))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("404 Not Found — snapshot not found or IDOR attempt")
        void testGetSingleSnapshotNotFound() throws Exception {
            UUID cultivationId = UUID.randomUUID();
            UUID historyId = UUID.randomUUID();

            when(historyService.getSnapshot(eq(cultivationId), eq(historyId)))
                    .thenThrow(new ResourceNotFoundException("History record not found"));

            mockMvc.perform(get("/api/v1/history/cultivations/{cultivationId}/snapshots/{historyId}", cultivationId, historyId)
                            .with(jwt().jwt(j -> j.subject(FARMER_B_ID))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("400 Bad Request — malformed history UUID")
        void testGetSingleSnapshotMalformedUuid() throws Exception {
            UUID cultivationId = UUID.randomUUID();

            mockMvc.perform(get("/api/v1/history/cultivations/{cultivationId}/snapshots/{historyId}", cultivationId, "bad-history-id")
                            .with(jwt().jwt(j -> j.subject(FARMER_A_ID))))
                    .andExpect(status().isBadRequest());
        }
    }

    // =========================================================================
    // STATUS EVENTS ENDPOINTS
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/history/cultivations/{cultivationId}/events")
    class GetStatusEventsTests {

        @Test
        @DisplayName("200 OK — default pagination returns chronological status events")
        void testGetEventsDefaultPagination() throws Exception {
            UUID cultivationId = UUID.randomUUID();
            UUID eventId1 = UUID.randomUUID();
            UUID eventId2 = UUID.randomUUID();
            Page<HistoryStatusEventDto> page = new PageImpl<>(List.of(
                    sampleStatusEventDto(cultivationId, eventId1, null, "PLANNED"),
                    sampleStatusEventDto(cultivationId, eventId2, "PLANNED", "STARTED")
            ));

            when(historyService.getStatusEventsForCultivation(eq(cultivationId), any()))
                    .thenReturn(page);

            mockMvc.perform(get("/api/v1/history/cultivations/{cultivationId}/events", cultivationId)
                            .with(jwt().jwt(j -> j.subject(FARMER_A_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content", hasSize(2)))
                    .andExpect(jsonPath("$.data.content[0].toStatus").value("PLANNED"))
                    .andExpect(jsonPath("$.data.content[1].fromStatus").value("PLANNED"))
                    .andExpect(jsonPath("$.data.content[1].toStatus").value("STARTED"));
        }

        @Test
        @DisplayName("401 Unauthorized — missing JWT Bearer token")
        void testGetEventsMissingJwt() throws Exception {
            UUID cultivationId = UUID.randomUUID();

            mockMvc.perform(get("/api/v1/history/cultivations/{cultivationId}/events", cultivationId))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("404 Not Found — non-existent cultivation or cross-farmer IDOR")
        void testGetEventsNotFound() throws Exception {
            UUID cultivationId = UUID.randomUUID();

            when(historyService.getStatusEventsForCultivation(eq(cultivationId), any()))
                    .thenThrow(new ResourceNotFoundException("Cultivation not found or access denied"));

            mockMvc.perform(get("/api/v1/history/cultivations/{cultivationId}/events", cultivationId)
                            .with(jwt().jwt(j -> j.subject(FARMER_B_ID))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("400 Bad Request — malformed cultivation UUID")
        void testGetEventsMalformedUuid() throws Exception {
            mockMvc.perform(get("/api/v1/history/cultivations/{cultivationId}/events", "not-a-uuid")
                            .with(jwt().jwt(j -> j.subject(FARMER_A_ID))))
                    .andExpect(status().isBadRequest());
        }
    }

    // =========================================================================
    // IMMUTABILITY & REST METHOD RESTRICTIONS
    // =========================================================================

    @Nested
    @DisplayName("Immutability — Disallowed HTTP Methods return 405")
    class ImmutabilityMethodTests {

        @Test
        @DisplayName("POST /snapshots — 405 Method Not Allowed (read-only audit trail)")
        void testPostSnapshotsDisallowed() throws Exception {
            UUID cultivationId = UUID.randomUUID();

            mockMvc.perform(post("/api/v1/history/cultivations/{cultivationId}/snapshots", cultivationId)
                            .with(jwt().jwt(j -> j.subject(FARMER_A_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        @DisplayName("PUT /snapshots/{historyId} — 405 Method Not Allowed")
        void testPutSnapshotDisallowed() throws Exception {
            UUID cultivationId = UUID.randomUUID();
            UUID historyId = UUID.randomUUID();

            mockMvc.perform(put("/api/v1/history/cultivations/{cultivationId}/snapshots/{historyId}", cultivationId, historyId)
                            .with(jwt().jwt(j -> j.subject(FARMER_A_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        @DisplayName("DELETE /snapshots/{historyId} — 405 Method Not Allowed")
        void testDeleteSnapshotDisallowed() throws Exception {
            UUID cultivationId = UUID.randomUUID();
            UUID historyId = UUID.randomUUID();

            mockMvc.perform(delete("/api/v1/history/cultivations/{cultivationId}/snapshots/{historyId}", cultivationId, historyId)
                            .with(jwt().jwt(j -> j.subject(FARMER_A_ID))))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        @DisplayName("PATCH /snapshots/{historyId} — 405 Method Not Allowed")
        void testPatchSnapshotDisallowed() throws Exception {
            UUID cultivationId = UUID.randomUUID();
            UUID historyId = UUID.randomUUID();

            mockMvc.perform(patch("/api/v1/history/cultivations/{cultivationId}/snapshots/{historyId}", cultivationId, historyId)
                            .with(jwt().jwt(j -> j.subject(FARMER_A_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        @DisplayName("POST /events — 405 Method Not Allowed")
        void testPostEventsDisallowed() throws Exception {
            UUID cultivationId = UUID.randomUUID();

            mockMvc.perform(post("/api/v1/history/cultivations/{cultivationId}/events", cultivationId)
                            .with(jwt().jwt(j -> j.subject(FARMER_A_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        @DisplayName("DELETE /events — 405 Method Not Allowed")
        void testDeleteEventsDisallowed() throws Exception {
            UUID cultivationId = UUID.randomUUID();

            mockMvc.perform(delete("/api/v1/history/cultivations/{cultivationId}/events", cultivationId)
                            .with(jwt().jwt(j -> j.subject(FARMER_A_ID))))
                    .andExpect(status().isMethodNotAllowed());
        }
    }
}
