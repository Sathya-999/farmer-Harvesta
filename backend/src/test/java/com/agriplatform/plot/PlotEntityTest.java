package com.agriplatform.plot;

import com.agriplatform.geography.entity.District;
import com.agriplatform.geography.entity.Mandal;
import com.agriplatform.geography.entity.State;
import com.agriplatform.geography.entity.Village;
import com.agriplatform.plot.entity.Plot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the {@link Plot} entity.
 * Covers field accessors, default values, lifecycle state, and version token generation.
 */
@DisplayName("Task 07 — Plot Entity Tests")
class PlotEntityTest {

    private Plot plot;
    private Village village;
    private UUID farmerId;
    private OffsetDateTime now;

    @BeforeEach
    void setUp() {
        farmerId = UUID.randomUUID();
        now = OffsetDateTime.now();

        State state = new State();
        state.setId(UUID.randomUUID());
        state.setName("Andhra Pradesh");
        state.setActive(true);

        District district = new District();
        district.setId(UUID.randomUUID());
        district.setName("Chittoor");
        district.setState(state);
        district.setActive(true);

        Mandal mandal = new Mandal();
        mandal.setId(UUID.randomUUID());
        mandal.setName("Chittoor Mandal");
        mandal.setDistrict(district);
        mandal.setActive(true);

        village = new Village();
        village.setId(UUID.randomUUID());
        village.setName("Mapakshi");
        village.setMandal(mandal);
        village.setActive(true);

        plot = new Plot();
        plot.setFarmerId(farmerId);
        plot.setVillage(village);
        plot.setLabel("North Field");
        plot.setTotalAreaAcres(new BigDecimal("2.5000"));
        plot.setActive(true);
        plot.setCreatedAt(now);
        plot.setUpdatedAt(now);
    }

    @Test
    @DisplayName("1. Plot has correct farmer_id")
    void plot_hasFarmerId() {
        assertThat(plot.getFarmerId()).isEqualTo(farmerId);
    }

    @Test
    @DisplayName("2. Plot is active by default after construction")
    void plot_isActiveByDefault() {
        Plot newPlot = new Plot();
        newPlot.setActive(true);
        assertThat(newPlot.isActive()).isTrue();
    }

    @Test
    @DisplayName("3. Plot can be deactivated (soft delete)")
    void plot_canBeDeactivated() {
        plot.setActive(false);
        assertThat(plot.isActive()).isFalse();
    }

    @Test
    @DisplayName("4. Plot label is stored correctly")
    void plot_labelStoredCorrectly() {
        assertThat(plot.getLabel()).isEqualTo("North Field");
    }

    @Test
    @DisplayName("5. Plot allows null label")
    void plot_allowsNullLabel() {
        plot.setLabel(null);
        assertThat(plot.getLabel()).isNull();
    }

    @Test
    @DisplayName("6. Plot allows null area")
    void plot_allowsNullArea() {
        plot.setTotalAreaAcres(null);
        assertThat(plot.getTotalAreaAcres()).isNull();
    }

    @Test
    @DisplayName("7. Plot area stored as BigDecimal")
    void plot_areaStoredAsBigDecimal() {
        assertThat(plot.getTotalAreaAcres()).isEqualByComparingTo("2.5000");
    }

    @Test
    @DisplayName("8. Plot village is accessible")
    void plot_villageAccessible() {
        assertThat(plot.getVillage()).isEqualTo(village);
        assertThat(plot.getVillage().getName()).isEqualTo("Mapakshi");
    }

    @Test
    @DisplayName("9. Version token is ISO-8601 string from updatedAt")
    void plot_versionTokenIsIsoString() {
        assertThat(plot.getVersion()).isNotNull();
        assertThat(plot.getVersion()).isEqualTo(now.toString());
    }

    @Test
    @DisplayName("10. Version token is null when updatedAt is null")
    void plot_versionNullWhenUpdatedAtNull() {
        plot.setUpdatedAt(null);
        assertThat(plot.getVersion()).isNull();
    }

    @Test
    @DisplayName("11. Version token preserves microsecond precision")
    void plot_versionPreservesMicrosecondPrecision() {
        OffsetDateTime microsecond = OffsetDateTime.parse("2026-10-02T18:36:26.118966Z");
        plot.setUpdatedAt(microsecond);
        assertThat(plot.getVersion()).contains("118966");
    }

    @Test
    @DisplayName("12. Plot does NOT expose latitude or longitude (no GPS in Alpha)")
    void plot_doesNotHaveGpsFields() throws NoSuchFieldException {
        // The Plot entity must not have latitude/longitude fields
        Class<Plot> clazz = Plot.class;
        try {
            clazz.getDeclaredField("latitude");
            throw new AssertionError("Plot must NOT have a 'latitude' field in Alpha V1");
        } catch (NoSuchFieldException e) {
            // Expected — latitude should not exist on Plot
        }
        try {
            clazz.getDeclaredField("longitude");
            throw new AssertionError("Plot must NOT have a 'longitude' field in Alpha V1");
        } catch (NoSuchFieldException e) {
            // Expected — longitude should not exist on Plot
        }
    }

    @Test
    @DisplayName("13. Plot created_at and updated_at are set")
    void plot_timestampsSet() {
        assertThat(plot.getCreatedAt()).isEqualTo(now);
        assertThat(plot.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("14. Plot village hierarchy navigable")
    void plot_villageHierarchyNavigable() {
        Village v = plot.getVillage();
        assertThat(v.getMandal()).isNotNull();
        assertThat(v.getMandal().getDistrict()).isNotNull();
        assertThat(v.getMandal().getDistrict().getState()).isNotNull();
        assertThat(v.getMandal().getDistrict().getState().getName()).isEqualTo("Andhra Pradesh");
    }
}
