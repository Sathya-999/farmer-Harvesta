package com.agriplatform.history.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * API response representing one immutable historical snapshot row from
 * {@code public.cultivation_intention_history}.
 *
 * <h2>Immutability</h2>
 * <p>Snapshots are created exclusively by the DB trigger
 * {@code trg_record_intention_history}. No REST endpoint, repository
 * method, or application code may INSERT, UPDATE, or DELETE these records.
 *
 * <h2>Actor Attribution</h2>
 * <p>{@code actorId} is populated from the PostgreSQL session setting
 * {@code app.actor_id} by the trigger at commit time. The client never
 * controls this value.
 */
public record HistorySnapshotDto(
    UUID historyId,
    UUID intentionId,
    Integer version,
    OffsetDateTime snapshotAt,
    UUID plotId,
    UUID farmerId,
    UUID cropId,
    UUID cropVarietyId,
    UUID seasonId,
    Short cultivationYear,
    BigDecimal cultivatedAreaAcres,
    LocalDate plannedPlantingDate,
    LocalDate actualPlantingDate,
    BigDecimal actualAreaAcres,
    LocalDate harvestDate,
    String status,
    String statusReason,
    String source,
    UUID actorId,
    String actorType,
    String changeType,
    List<String> changedFields,
    String changeReason
) {}
