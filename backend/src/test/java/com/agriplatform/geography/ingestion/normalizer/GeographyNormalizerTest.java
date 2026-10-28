package com.agriplatform.geography.ingestion.normalizer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GeographyNormalizer Unit Tests")
class GeographyNormalizerTest {

    private static final BigDecimal LAT_MIN = new BigDecimal("-90.0");
    private static final BigDecimal LAT_MAX = new BigDecimal("90.0");
    private static final BigDecimal LON_MIN = new BigDecimal("-180.0");
    private static final BigDecimal LON_MAX = new BigDecimal("180.0");

    @Test
    @DisplayName("Should normalize whitespace and trim text")
    void testNormalizeWhitespace() {
        assertThat(GeographyNormalizer.normalizeText("   Bangalore   South   ")).isEqualTo("Bangalore South");
        assertThat(GeographyNormalizer.normalizeText("   ")).isNull();
        assertThat(GeographyNormalizer.normalizeText(null)).isNull();
    }

    @Test
    @DisplayName("Should apply Unicode NFC normalization for Indian language scripts")
    void testUnicodeNormalization() {
        // Devanagari combined characters
        String devanagari = "उत्तर प्रदेश";
        assertThat(GeographyNormalizer.normalizeText(devanagari)).isEqualTo(devanagari);

        // Telugu script
        String telugu = "అనంతపురం";
        assertThat(GeographyNormalizer.normalizeText(telugu)).isEqualTo(telugu);

        // Tamil script
        String tamil = "தமிழ்நாடு";
        assertThat(GeographyNormalizer.normalizeText(tamil)).isEqualTo(tamil);
    }

    @Test
    @DisplayName("Should normalize administrative codes")
    void testNormalizeCode() {
        assertThat(GeographyNormalizer.normalizeCode("  ap  ")).isEqualTo("AP");
        assertThat(GeographyNormalizer.normalizeCode("lgd_502")).isEqualTo("LGD_502");
        assertThat(GeographyNormalizer.normalizeCode("   ")).isNull();
        assertThat(GeographyNormalizer.normalizeCode(null)).isNull();
    }

    @Test
    @DisplayName("Should parse valid coordinates with 6-digit precision scale")
    void testValidCoordinates() {
        BigDecimal lat = GeographyNormalizer.parseCoordinate("14.681888", LAT_MIN, LAT_MAX, "latitude");
        assertThat(lat).isNotNull();
        assertThat(lat.scale()).isEqualTo(6);
        assertThat(lat).isEqualByComparingTo("14.681888");

        BigDecimal lon = GeographyNormalizer.parseCoordinate("77.600591", LON_MIN, LON_MAX, "longitude");
        assertThat(lon).isNotNull();
        assertThat(lon.scale()).isEqualTo(6);
        assertThat(lon).isEqualByComparingTo("77.600591");
    }

    @Test
    @DisplayName("Should throw exception for out-of-bounds latitude")
    void testOutOfBoundsLatitude() {
        assertThatThrownBy(() -> GeographyNormalizer.parseCoordinate("95.123", LAT_MIN, LAT_MAX, "latitude"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("out of valid bounds");

        assertThatThrownBy(() -> GeographyNormalizer.parseCoordinate("-91.0", LAT_MIN, LAT_MAX, "latitude"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("out of valid bounds");
    }

    @Test
    @DisplayName("Should throw exception for out-of-bounds longitude")
    void testOutOfBoundsLongitude() {
        assertThatThrownBy(() -> GeographyNormalizer.parseCoordinate("185.0", LON_MIN, LON_MAX, "longitude"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("out of valid bounds");

        assertThatThrownBy(() -> GeographyNormalizer.parseCoordinate("-181.0", LON_MIN, LON_MAX, "longitude"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("out of valid bounds");
    }

    @Test
    @DisplayName("Should throw exception for non-numeric coordinates")
    void testNonNumericCoordinates() {
        assertThatThrownBy(() -> GeographyNormalizer.parseCoordinate("abc", LAT_MIN, LAT_MAX, "latitude"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid numeric format");
    }
}
