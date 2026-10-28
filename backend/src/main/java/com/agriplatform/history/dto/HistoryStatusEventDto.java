package com.agriplatform.history.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * API response representing one immutable status-change event from
 * {@code public.cultivation_status_events}.
 *
 * <h2>Immutability</h2>
 * <p>Events are created exclusively by DB triggers:
 * <ul>
 *   <li>{@code trg_record_status_event_insert} — INSERT on cultivation_intentions</li>
 *   <li>{@code trg_record_status_event_update} — UPDATE when status changes</li>
 * </ul>
 * The application layer has READ-ONLY access to this table.
 *
 * <h2>Actor Attribution</h2>
 * <p>{@code changedBy} is populated by the trigger from the PostgreSQL session
 * setting {@code app.actor_id}. The client never controls this value.
 */
public record HistoryStatusEventDto(
    UUID eventId,
    UUID intentionId,
    String fromStatus,
    String toStatus,
    UUID changedBy,
    String actorType,
    OffsetDateTime changedAt,
    String note
) {}
