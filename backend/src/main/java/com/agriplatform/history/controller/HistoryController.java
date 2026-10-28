package com.agriplatform.history.controller;

import com.agriplatform.common.response.ApiResponse;
import com.agriplatform.history.dto.HistorySnapshotDto;
import com.agriplatform.history.dto.HistoryStatusEventDto;
import com.agriplatform.history.service.HistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Task 09 — History REST Controller.
 *
 * <h2>Endpoint Design</h2>
 * <pre>
 * GET /api/v1/history/cultivations/{cultivationId}/snapshots
 *   → Paginated field-change history snapshots (version DESC)
 *
 * GET /api/v1/history/cultivations/{cultivationId}/snapshots/{historyId}
 *   → Single immutable history snapshot
 *
 * GET /api/v1/history/cultivations/{cultivationId}/events
 *   → Paginated status-change events (changedAt ASC)
 * </pre>
 *
 * <h2>Security</h2>
 * <p>All endpoints require a valid Supabase JWT Bearer token.
 * Every response is scoped to the authenticated farmer; cross-farmer
 * access returns HTTP 404.
 *
 * <h2>Immutability</h2>
 * <p>This controller exposes ONLY GET methods. No write operations are
 * possible via this API. History records are created exclusively by
 * DB triggers and are immutable.
 *
 * <h2>Pagination</h2>
 * <p>Page size is bounded at {@value #MAX_PAGE_SIZE} regardless of the
 * {@code size} query parameter supplied by the client.
 */
@RestController
@RequestMapping("/api/v1/history/cultivations")
public class HistoryController {

    private static final Logger log = LoggerFactory.getLogger(HistoryController.class);
    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    /**
     * Returns paginated field-change history snapshots for a cultivation intention.
     *
     * <p>Ordered by {@code version DESC} (newest snapshot first).
     *
     * @param cultivationId UUID of the cultivation intention
     * @param page          zero-based page index (default 0)
     * @param size          page size (default 20, max 100)
     * @return 200 OK with paginated {@link HistorySnapshotDto} records
     */
    @GetMapping("/{cultivationId}/snapshots")
    public ResponseEntity<ApiResponse<Page<HistorySnapshotDto>>> getSnapshots(
            @PathVariable UUID cultivationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        int boundedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        PageRequest pageable = PageRequest.of(page, boundedSize, Sort.by(Sort.Direction.DESC, "version"));

        log.debug("GET snapshots: cultivationId={}, page={}, size={}", cultivationId, page, boundedSize);
        Page<HistorySnapshotDto> result = historyService.getSnapshotsForCultivation(cultivationId, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Returns a single immutable history snapshot.
     *
     * @param cultivationId UUID of the cultivation intention (IDOR scope)
     * @param historyId     UUID of the specific history snapshot
     * @return 200 OK with a single {@link HistorySnapshotDto}
     */
    @GetMapping("/{cultivationId}/snapshots/{historyId}")
    public ResponseEntity<ApiResponse<HistorySnapshotDto>> getSnapshot(
            @PathVariable UUID cultivationId,
            @PathVariable UUID historyId) {

        log.debug("GET snapshot: cultivationId={}, historyId={}", cultivationId, historyId);
        HistorySnapshotDto result = historyService.getSnapshot(cultivationId, historyId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Returns paginated status-change events for a cultivation intention.
     *
     * <p>Ordered by {@code changedAt ASC} (chronological, oldest first).
     *
     * @param cultivationId UUID of the cultivation intention
     * @param page          zero-based page index (default 0)
     * @param size          page size (default 20, max 100)
     * @return 200 OK with paginated {@link HistoryStatusEventDto} records
     */
    @GetMapping("/{cultivationId}/events")
    public ResponseEntity<ApiResponse<Page<HistoryStatusEventDto>>> getEvents(
            @PathVariable UUID cultivationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        int boundedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        PageRequest pageable = PageRequest.of(page, boundedSize, Sort.by(Sort.Direction.ASC, "changedAt"));

        log.debug("GET events: cultivationId={}, page={}, size={}", cultivationId, page, boundedSize);
        Page<HistoryStatusEventDto> result = historyService.getStatusEventsForCultivation(cultivationId, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
