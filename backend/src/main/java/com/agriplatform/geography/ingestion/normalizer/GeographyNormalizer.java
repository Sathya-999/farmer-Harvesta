package com.agriplatform.geography.ingestion.normalizer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Text and coordinate normalization utility for geography master data.
 *
 * <p>Enforces:
 * <ul>
 *   <li>Unicode NFC canonical equivalence (preserves Indian scripts and accents).</li>
 *   <li>Whitespace trimming and multiple-space collapse.</li>
 *   <li>Standardized code formatting.</li>
 *   <li>High-precision coordinate parsing to {@link BigDecimal} (scale 6).</li>
 * </ul>
 */
public final class GeographyNormalizer {

    private static final Pattern MULTI_SPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern CODE_CLEAN_PATTERN = Pattern.compile("[^a-zA-Z0-9-_.]");

    private GeographyNormalizer() {}

    /**
     * Normalizes geography names: trims whitespace, applies Unicode NFC canonical decomposition/composition,
     * and collapses consecutive whitespace into a single ASCII space.
     *
     * @param input the raw name string
     * @return normalized text, or null if input was null or blank
     */
    public static String normalizeText(String input) {
        if (input == null) {
            return null;
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        // Normalize Unicode to NFC
        String nfc = Normalizer.normalize(trimmed, Normalizer.Form.NFC);
        // Collapse internal whitespace
        return MULTI_SPACE_PATTERN.matcher(nfc).replaceAll(" ");
    }

    /**
     * Normalizes administrative codes (LGD codes, ISO codes, Census codes).
     * Trims whitespace and strips surrounding invalid control characters.
     *
     * @param input the raw code
     * @return normalized code, or null if input was null or blank
     */
    public static String normalizeCode(String input) {
        if (input == null) {
            return null;
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toUpperCase();
    }

    /**
     * Parses and normalizes geographic coordinate strings into {@link BigDecimal} with scale 6.
     *
     * @param input coordinate string
     * @param min minimum allowed value (e.g. -90 for latitude, -180 for longitude)
     * @param max maximum allowed value (e.g. +90 for latitude, +180 for longitude)
     * @param fieldName name of field for error reporting
     * @return parsed BigDecimal, or null if input was null or blank
     * @throws IllegalArgumentException if coordinate is malformed or outside [min, max]
     */
    public static BigDecimal parseCoordinate(String input, BigDecimal min, BigDecimal max, String fieldName) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String trimmed = input.trim();
        try {
            BigDecimal value = new BigDecimal(trimmed);
            if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
                throw new IllegalArgumentException(
                        String.format("%s out of valid bounds [%s, %s]: %s", fieldName, min, max, trimmed));
            }
            return value.setScale(6, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    String.format("Invalid numeric format for %s: '%s'", fieldName, trimmed));
        }
    }
}
