package com.agriplatform.cultivation.controller;

import com.agriplatform.common.response.ApiResponse;
import com.agriplatform.cultivation.dto.*;
import com.agriplatform.cultivation.service.CultivationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for Cultivation Management — {@code /api/v1/cultivations}.
 *
 * <h2>Security & IDOR</h2>
 * <p>All endpoints require a valid Supabase JWT Bearer token.
 * All operations are strictly owner-scoped via the authenticated farmer identity.
 * Cross-farmer access attempts return HTTP 404 (Not Found) to prevent resource enumeration.
 *
 * <h2>Idempotency</h2>
 * <p>{@code POST} accepts an optional {@code Idempotency-Key} request header.
 * If present, network retries return the cached original response.
 */
@RestController
@RequestMapping("/api/v1/cultivations")
public class CultivationController {

    private static final Logger log = LoggerFactory.getLogger(CultivationController.class);

    private static final int MAX_PAGE_SIZE = 100;
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    private final CultivationService cultivationService;

    public CultivationController(CultivationService cultivationService) {
        this.cultivationService = cultivationService;
    }

    /**
     * Creates a new cultivation intention for the authenticated farmer.
     *
     * @param request        cultivation creation payload
     * @param idempotencyKey optional {@code Idempotency-Key} header for at-most-once semantics
     * @return 201 Created with the new cultivation record, or cached replay
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CultivationResponseDto>> createCultivation(
            @Valid @RequestBody CreateCultivationRequestDto request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        log.debug("POST /api/v1/cultivations — idempotency-key present: {}", idempotencyKey != null);
        CultivationResponseDto response = cultivationService.createCultivation(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * Retrieves a single cultivation intention by ID.
     *
     * @param id cultivation intention UUID
     * @return 200 OK with cultivation details, or 404 if not found / foreign
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CultivationResponseDto>> getCultivation(@PathVariable UUID id) {
        CultivationResponseDto response = cultivationService.getCultivation(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Lists cultivation intentions for the authenticated farmer with pagination and optional filters.
     *
     * @param page            zero-based page index (default 0)
     * @param size            page size (default 20, max 100)
     * @param plotId          filter by plot UUID
     * @param seasonId        filter by season UUID
     * @param cultivationYear filter by cultivation year
     * @param cropId          filter by crop UUID
     * @param status          filter by status (default ALL)
     * @param sortBy          field to sort by (default createdAt)
     * @param direction       sort direction: ASC or DESC (default DESC)
     * @return 200 OK with paginated list
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<CultivationResponseDto>>> listCultivations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID plotId,
            @RequestParam(required = false) UUID seasonId,
            @RequestParam(required = false) Short cultivationYear,
            @RequestParam(required = false) UUID cropId,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {

        int boundedSize = Math.min(size, MAX_PAGE_SIZE);
        Sort.Direction sortDirection = "ASC".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        String safeSort = isSafeSortField(sortBy) ? sortBy : DEFAULT_SORT_FIELD;

        PageRequest pageable = PageRequest.of(page, boundedSize, Sort.by(sortDirection, safeSort));
        Page<CultivationResponseDto> response = cultivationService.listCultivations(
                pageable, plotId, seasonId, cultivationYear, cropId, status);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Updates mutable fields of a cultivation intention in {@code PLANNED} status.
     *
     * @param id      cultivation intention UUID
     * @param request update payload with optional {@code expectedVersion}
     * @return 200 OK with updated cultivation details
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CultivationResponseDto>> updateCultivation(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCultivationRequestDto request) {

        CultivationResponseDto response = cultivationService.updateCultivation(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Executes a lifecycle state transition on a cultivation cycle.
     *
     * @param id      cultivation intention UUID
     * @param request status update payload
     * @return 200 OK with updated cultivation details and new status
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<CultivationResponseDto>> updateCultivationStatus(
            @PathVariable UUID id,
            @Valid @RequestBody CultivationStatusUpdateRequestDto request) {

        CultivationResponseDto response = cultivationService.updateCultivationStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Retrieves the audit trail of status change events for a cultivation intention.
     *
     * @param id cultivation intention UUID
     * @return 200 OK with list of status change events
     */
    @GetMapping("/{id}/events")
    public ResponseEntity<ApiResponse<List<CultivationStatusEventDto>>> getCultivationEvents(
            @PathVariable UUID id) {

        List<CultivationStatusEventDto> events = cultivationService.getCultivationEvents(id);
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    /**
     * Retrieves the immutable version history snapshots for a cultivation intention.
     *
     * @param id cultivation intention UUID
     * @return 200 OK with list of history snapshots
     */
    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponse<List<CultivationHistoryDto>>> getCultivationHistory(
            @PathVariable UUID id) {

        List<CultivationHistoryDto> history = cultivationService.getCultivationHistory(id);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    private boolean isSafeSortField(String field) {
        return switch (field) {
            case "createdAt", "updatedAt", "cultivationYear", "cultivatedAreaAcres",
                 "plannedPlantingDate", "status" -> true;
            default -> false;
        };
    }
}
