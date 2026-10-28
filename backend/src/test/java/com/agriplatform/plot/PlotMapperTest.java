package com.agriplatform.plot;

import com.agriplatform.geography.entity.District;
import com.agriplatform.geography.entity.Mandal;
import com.agriplatform.geography.entity.State;
import com.agriplatform.geography.entity.Village;
import com.agriplatform.plot.dto.PlotResponseDto;
import com.agriplatform.plot.entity.Plot;
import com.agriplatform.plot.mapper.PlotMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PlotMapper}.
 * Verifies correct mapping and explicit exclusion of village GPS coordinates.
 */
@DisplayName("Task 07 — Plot Mapper Tests")
class PlotMapperTest {

    private PlotMapper mapper;
    private Plot plot;
    private Village village;
    private Mandal mandal;
    private District district;
    private State state;
    private UUID farmerId;
    private UUID villageId;

    @BeforeEach
    void setUp() {
        mapper = new PlotMapper();
        farmerId = UUID.randomUUID();
        villageId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.parse("2026-10-02T18:36:26.118966Z");

        state = new State();
        state.setId(UUID.randomUUID());
        state.setName("Andhra Pradesh");
        state.setActive(true);

        district = new District();
        district.setId(UUID.randomUUID());
        district.setName("Chittoor");
        district.setState(state);
        district.setActive(true);

        mandal = new Mandal();
        mandal.setId(UUID.randomUUID());
        mandal.setName("Chittoor Mandal");
        mandal.setDistrict(district);
        mandal.setActive(true);

        village = new Village();
        village.setId(villageId);
        village.setName("Mapakshi");
        village.setMandal(mandal);
        village.setActive(true);
        // Village has GPS coordinates — must NOT appear in plot response
        village.setLatitude(new BigDecimal("13.216400"));
        village.setLongitude(new BigDecimal("79.104600"));

        plot = new Plot();
        plot.setId(UUID.randomUUID());
        plot.setFarmerId(farmerId);
        plot.setVillage(village);
        plot.setLabel("North Field");
        plot.setTotalAreaAcres(new BigDecimal("2.5000"));
        plot.setActive(true);
        plot.setCreatedAt(now);
        plot.setUpdatedAt(now);
    }

    @Test
    @DisplayName("1. Basic mapping produces non-null DTO")
    void mapping_producesNonNullDto() {
        PlotResponseDto dto = mapper.toDto(plot);
        assertThat(dto).isNotNull();
    }

    @Test
    @DisplayName("2. Null input returns null")
    void mapping_nullInputReturnsNull() {
        assertThat(mapper.toDto(null)).isNull();
    }

    @Test
    @DisplayName("3. Plot ID mapped correctly")
    void mapping_plotIdMapped() {
        PlotResponseDto dto = mapper.toDto(plot);
        assertThat(dto.id()).isEqualTo(plot.getId());
    }

    @Test
    @DisplayName("4. Farmer ID mapped correctly")
    void mapping_farmerIdMapped() {
        PlotResponseDto dto = mapper.toDto(plot);
        assertThat(dto.farmerId()).isEqualTo(farmerId);
    }

    @Test
    @DisplayName("5. Village ID mapped correctly")
    void mapping_villageIdMapped() {
        PlotResponseDto dto = mapper.toDto(plot);
        assertThat(dto.villageId()).isEqualTo(villageId);
    }

    @Test
    @DisplayName("6. Village name mapped correctly")
    void mapping_villageNameMapped() {
        PlotResponseDto dto = mapper.toDto(plot);
        assertThat(dto.villageName()).isEqualTo("Mapakshi");
    }

    @Test
    @DisplayName("7. Mandal name mapped correctly")
    void mapping_mandalNameMapped() {
        PlotResponseDto dto = mapper.toDto(plot);
        assertThat(dto.mandalName()).isEqualTo("Chittoor Mandal");
    }

    @Test
    @DisplayName("8. District name mapped correctly")
    void mapping_districtNameMapped() {
        PlotResponseDto dto = mapper.toDto(plot);
        assertThat(dto.districtName()).isEqualTo("Chittoor");
    }

    @Test
    @DisplayName("9. State name mapped correctly")
    void mapping_stateNameMapped() {
        PlotResponseDto dto = mapper.toDto(plot);
        assertThat(dto.stateName()).isEqualTo("Andhra Pradesh");
    }

    @Test
    @DisplayName("10. Label mapped correctly")
    void mapping_labelMapped() {
        PlotResponseDto dto = mapper.toDto(plot);
        assertThat(dto.label()).isEqualTo("North Field");
    }

    @Test
    @DisplayName("11. Null label mapped as null")
    void mapping_nullLabelMapped() {
        plot.setLabel(null);
        PlotResponseDto dto = mapper.toDto(plot);
        assertThat(dto.label()).isNull();
    }

    @Test
    @DisplayName("12. Area mapped correctly")
    void mapping_areaMapped() {
        PlotResponseDto dto = mapper.toDto(plot);
        assertThat(dto.totalAreaAcres()).isEqualByComparingTo("2.5000");
    }

    @Test
    @DisplayName("13. Null area mapped as null")
    void mapping_nullAreaMapped() {
        plot.setTotalAreaAcres(null);
        PlotResponseDto dto = mapper.toDto(plot);
        assertThat(dto.totalAreaAcres()).isNull();
    }

    @Test
    @DisplayName("14. isActive mapped correctly")
    void mapping_isActiveMapped() {
        PlotResponseDto dto = mapper.toDto(plot);
        assertThat(dto.isActive()).isTrue();
    }

    @Test
    @DisplayName("15. Version token is ISO-8601 microsecond string")
    void mapping_versionTokenIsMicrosecond() {
        PlotResponseDto dto = mapper.toDto(plot);
        assertThat(dto.version()).isEqualTo("2026-10-02T18:36:26.118966Z");
        assertThat(dto.version()).contains("118966"); // confirms microsecond precision
    }

    @Test
    @DisplayName("52. Plot response does NOT expose village latitude (DTO safety invariant)")
    void mapping_doesNotExposeVillageLatitude() throws NoSuchFieldException {
        PlotResponseDto dto = mapper.toDto(plot);
        // PlotResponseDto must not have latitude field
        try {
            PlotResponseDto.class.getDeclaredField("latitude");
            throw new AssertionError("PlotResponseDto MUST NOT have a 'latitude' field");
        } catch (NoSuchFieldException e) {
            // Correct — no latitude in response
        }
    }

    @Test
    @DisplayName("52b. Plot response does NOT expose village longitude (DTO safety invariant)")
    void mapping_doesNotExposeVillageLongitude() {
        PlotResponseDto dto = mapper.toDto(plot);
        // PlotResponseDto must not have longitude field
        try {
            PlotResponseDto.class.getDeclaredField("longitude");
            throw new AssertionError("PlotResponseDto MUST NOT have a 'longitude' field");
        } catch (NoSuchFieldException e) {
            // Correct — no longitude in response
        }
    }

    @Test
    @DisplayName("17. Unicode/Indic label mapped correctly")
    void mapping_unicodeLabelMapped() {
        plot.setLabel("ఉత్తర పొలం");
        PlotResponseDto dto = mapper.toDto(plot);
        assertThat(dto.label()).isEqualTo("ఉత్తర పొలం");
    }

    @Test
    @DisplayName("18. Created-at and updated-at timestamps mapped")
    void mapping_timestampsMapped() {
        PlotResponseDto dto = mapper.toDto(plot);
        assertThat(dto.createdAt()).isNotNull();
        assertThat(dto.updatedAt()).isNotNull();
    }
}
