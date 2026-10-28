package com.agriplatform.geography.dto;

import java.time.OffsetDateTime;

/**
 * Status of an asynchronous on-demand state synchronization job.
 */
public record StateSyncStatusDto(
        String stateCode,
        String stateName,
        String status, // IDLE, IN_PROGRESS, COMPLETED, FAILED
        long processedRows,
        long totalExpectedRows,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        String message
) {
    public static StateSyncStatusDto idle(String stateCode, String stateName) {
        return new StateSyncStatusDto(stateCode, stateName, "IDLE", 0, 0, null, null, "No synchronization currently running for state");
    }

    public static StateSyncStatusDto inProgress(String stateCode, String stateName, long processed, long total, OffsetDateTime startedAt) {
        return new StateSyncStatusDto(stateCode, stateName, "IN_PROGRESS", processed, total, startedAt, null, "Synchronization is actively processing");
    }

    public static StateSyncStatusDto completed(String stateCode, String stateName, long total, OffsetDateTime startedAt, OffsetDateTime completedAt, String msg) {
        return new StateSyncStatusDto(stateCode, stateName, "COMPLETED", total, total, startedAt, completedAt, msg);
    }

    public static StateSyncStatusDto failed(String stateCode, String stateName, OffsetDateTime startedAt, String errMsg) {
        return new StateSyncStatusDto(stateCode, stateName, "FAILED", 0, 0, startedAt, OffsetDateTime.now(), errMsg);
    }
}
