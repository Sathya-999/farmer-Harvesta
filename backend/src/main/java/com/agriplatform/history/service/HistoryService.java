package com.agriplatform.history.service;

import com.agriplatform.history.dto.HistorySnapshotDto;
import com.agriplatform.history.dto.HistoryStatusEventDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Task 09 — History Service interface.
 *
 * <h2>Contract</h2>
 * <p>All operations are READ-ONLY against the two immutable audit tables:
 * <ul>
 *   <li>{@code public.cultivation_intention_history}</li>
 *   <li>{@code public.cultivation_status_events}</li>
 * </ul>
 * No method in this interface or its implementation may INSERT, UPDATE,
 * or DELETE history records. History is DB-trigger-generated only.
 *
 * <h2>Security</h2>
 * <p>Every method derives farmer identity exclusively from the authenticated
 * JWT {@code sub} claim via {@link com.agriplatform.common.util.SecurityUtils}.
 * Cross-farmer access always returns HTTP 404.
 */
public interface HistoryService {

    /**
     * Returns paginated field-change history snapshots for a cultivation intention.
     *
     * @param cultivationId cultivation intention UUID
     * @param pageable      pagination parameters (size capped at 100 by controller)
     * @return page of immutable snapshot DTOs ordered by version DESC (newest first)
     * @throws com.agriplatform.common.exception.ResourceNotFoundException if intention
     *         does not exist or belongs to a different farmer (IDOR protection)
     */
    Page<HistorySnapshotDto> getSnapshotsForCultivation(UUID cultivationId, Pageable pageable);

    /**
     * Returns a single history snapshot by its unique history ID, subject to
     * farmer ownership validation.
     *
     * @param cultivationId cultivation intention UUID (IDOR scope)
     * @param historyId     specific history record UUID
     * @return the matching immutable snapshot DTO
     * @throws com.agriplatform.common.exception.ResourceNotFoundException if either ID
     *         is not found or if the history record does not belong to the caller's cultivation
     */
    HistorySnapshotDto getSnapshot(UUID cultivationId, UUID historyId);

    /**
     * Returns paginated status-change events for a cultivation intention.
     *
     * @param cultivationId cultivation intention UUID
     * @param pageable      pagination parameters
     * @return page of immutable status event DTOs ordered by changedAt ASC (oldest first)
     * @throws com.agriplatform.common.exception.ResourceNotFoundException if intention
     *         does not exist or belongs to a different farmer
     */
    Page<HistoryStatusEventDto> getStatusEventsForCultivation(UUID cultivationId, Pageable pageable);
}
