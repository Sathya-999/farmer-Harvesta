package com.agriplatform.geography.ingestion.model;

/**
 * Detailed error reported during geography validation or synchronization.
 */
public record ValidationError(
        long lineNumber,
        String level,
        String field,
        String rejectedValue,
        String reason
) {
    @Override
    public String toString() {
        return String.format("[Line %d] %s.%s = '%s': %s", lineNumber, level, field, rejectedValue, reason);
    }
}
