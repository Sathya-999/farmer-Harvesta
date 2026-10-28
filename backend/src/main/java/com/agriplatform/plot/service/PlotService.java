package com.agriplatform.plot.service;

import com.agriplatform.plot.dto.CreatePlotRequestDto;
import com.agriplatform.plot.dto.PlotResponseDto;
import com.agriplatform.plot.dto.UpdatePlotRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface for Plot Management operations.
 *
 * <h2>Security Contract</h2>
 * <p>All operations derive the farmer identity from the JWT {@code sub} claim via
 * {@code SecurityUtils.getAuthenticatedUserId()}. Client-supplied farmer IDs are ignored.
 */
public interface PlotService {

    /**
     * Creates a new plot for the authenticated farmer.
     * Supports idempotency via {@code idempotencyKey} header.
     *
     * @param request        plot creation details
     * @param idempotencyKey optional idempotency key header value (null = skip idempotency check)
     * @return created plot response (may be a cached replay if idempotent)
     */
    PlotResponseDto createPlot(CreatePlotRequestDto request, String idempotencyKey);

    /**
     * Retrieves a single plot by ID for the authenticated farmer.
     *
     * @param plotId plot UUID
     * @return plot details
     */
    PlotResponseDto getPlot(UUID plotId);

    /**
     * Lists plots for the authenticated farmer with pagination.
     *
     * @param pageable sort and pagination parameters
     * @param status   filter: "ACTIVE", "INACTIVE", or "ALL"
     * @return paginated list of plots
     */
    Page<PlotResponseDto> listPlots(Pageable pageable, String status);

    /**
     * Updates mutable plot fields with optimistic concurrency control.
     *
     * @param plotId  plot UUID
     * @param request update details including {@code expectedVersion} token
     * @return updated plot response with new version token
     */
    PlotResponseDto updatePlot(UUID plotId, UpdatePlotRequestDto request);

    /**
     * Soft-deactivates a plot (sets {@code is_active = false}).
     * Blocked if active cultivation intentions exist.
     *
     * @param plotId plot UUID
     */
    void deactivatePlot(UUID plotId);

    /**
     * Reactivates a previously deactivated plot.
     *
     * @param plotId plot UUID
     * @return reactivated plot response
     */
    PlotResponseDto reactivatePlot(UUID plotId);
}
