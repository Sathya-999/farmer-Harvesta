package com.agriplatform.plot.controller;

import com.agriplatform.common.response.ApiResponse;
import com.agriplatform.plot.dto.CreatePlotRequestDto;
import com.agriplatform.plot.dto.PlotResponseDto;
import com.agriplatform.plot.dto.UpdatePlotRequestDto;
import com.agriplatform.plot.service.PlotService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for Plot Management — {@code /api/v1/plots}.
 *
 * <h2>Security</h2>
 * <p>All endpoints require a valid Supabase JWT Bearer token. The existing
 * {@code SecurityConfig.anyRequest().authenticated()} rule covers all Plot endpoints.
 * No {@code permitAll()} is added.
 *
 * <h2>Soft Delete</h2>
 * <p>The {@code DELETE /api/v1/plots/{id}} endpoint performs a soft deactivation
 * ({@code is_active = false}). Physical deletion is not permitted.
 *
 * <h2>Idempotency</h2>
 * <p>{@code POST} accepts an optional {@code Idempotency-Key} request header.
 * If present, the response may be a cached replay of a prior successful creation.
 */
@RestController
@RequestMapping("/api/v1/plots")
public class PlotController {

    private static final Logger log = LoggerFactory.getLogger(PlotController.class);

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    private final PlotService plotService;

    public PlotController(PlotService plotService) {
        this.plotService = plotService;
    }

    /**
     * Creates a new plot.
     *
     * @param request        plot creation payload
     * @param idempotencyKey optional {@code Idempotency-Key} header for at-most-once semantics
     * @return 201 Created with the new plot, or a cached replay if idempotent
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PlotResponseDto>> createPlot(
            @Valid @RequestBody CreatePlotRequestDto request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        log.debug("POST /api/v1/plots — idempotency-key present: {}", idempotencyKey != null);
        PlotResponseDto response = plotService.createPlot(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * Lists plots for the authenticated farmer (paginated).
     *
     * @param page      zero-based page index (default 0)
     * @param size      page size (default 20, max 100)
     * @param status    filter: ACTIVE (default), INACTIVE, ALL
     * @param sortBy    field to sort by (default: createdAt)
     * @param direction sort direction: ASC or DESC (default: DESC)
     * @return 200 OK with paginated plot list
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PlotResponseDto>>> listPlots(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "ACTIVE") String status,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {

        int boundedSize = Math.min(size, MAX_PAGE_SIZE);
        Sort.Direction sortDirection = "ASC".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        String safeSort = isSafeSortField(sortBy) ? sortBy : DEFAULT_SORT_FIELD;

        PageRequest pageable = PageRequest.of(page, boundedSize, Sort.by(sortDirection, safeSort));
        Page<PlotResponseDto> plots = plotService.listPlots(pageable, status);
        return ResponseEntity.ok(ApiResponse.success(plots));
    }

    /**
     * Retrieves a single plot by ID.
     *
     * @param id plot UUID path variable
     * @return 200 OK with plot details, or 404 if not found / not owned by caller
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PlotResponseDto>> getPlot(@PathVariable UUID id) {
        PlotResponseDto plot = plotService.getPlot(id);
        return ResponseEntity.ok(ApiResponse.success(plot));
    }

    /**
     * Updates mutable plot fields with optimistic concurrency control.
     *
     * @param id      plot UUID path variable
     * @param request update payload including {@code expectedVersion}
     * @return 200 OK with updated plot and new version token
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PlotResponseDto>> updatePlot(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePlotRequestDto request) {

        PlotResponseDto updated = plotService.updatePlot(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    /**
     * Soft-deactivates a plot.
     * Physical deletion is NOT performed — {@code is_active} is set to {@code false}.
     * Returns 409 if active cultivation exists.
     *
     * @param id plot UUID path variable
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivatePlot(@PathVariable UUID id) {
        plotService.deactivatePlot(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reactivates a previously deactivated plot.
     *
     * @param id plot UUID path variable
     * @return 200 OK with reactivated plot
     */
    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<ApiResponse<PlotResponseDto>> reactivatePlot(@PathVariable UUID id) {
        PlotResponseDto reactivated = plotService.reactivatePlot(id);
        return ResponseEntity.ok(ApiResponse.success(reactivated));
    }

    /**
     * Allowlist for sort field names to prevent JPQL injection or unexpected sort behavior.
     */
    private boolean isSafeSortField(String field) {
        return switch (field) {
            case "createdAt", "updatedAt", "label", "totalAreaAcres", "isActive" -> true;
            default -> false;
        };
    }
}
