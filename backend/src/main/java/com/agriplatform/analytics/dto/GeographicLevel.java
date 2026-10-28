package com.agriplatform.analytics.dto;

/**
 * Enumeration of supported geographic aggregation levels in the Agricultural Intelligence Platform.
 *
 * <p>Hierarchy:
 * <pre>
 *   VILLAGE (plots reside directly in villages)
 *      ↓
 *   MANDAL (sub-district grouping of villages)
 *      ↓
 *   DISTRICT (administrative grouping of mandals)
 *      ↓
 *   STATE (state-level grouping of districts)
 * </pre>
 */
public enum GeographicLevel {
    VILLAGE,
    MANDAL,
    DISTRICT,
    STATE;

    /**
     * Parses a case-insensitive string into a {@link GeographicLevel}.
     *
     * @param value string name of the geographic level
     * @return the corresponding {@link GeographicLevel}
     * @throws IllegalArgumentException if the level string is invalid or null
     */
    public static GeographicLevel fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Geographic level must not be null or blank");
        }
        try {
            return GeographicLevel.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid geographic level: '" + value + "'. Permitted values: VILLAGE, MANDAL, DISTRICT, STATE.");
        }
    }
}
