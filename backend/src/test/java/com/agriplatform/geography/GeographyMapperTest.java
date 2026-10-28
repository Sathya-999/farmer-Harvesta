package com.agriplatform.geography;

import com.agriplatform.geography.dto.DistrictDto;
import com.agriplatform.geography.dto.MandalDto;
import com.agriplatform.geography.dto.StateDto;
import com.agriplatform.geography.dto.VillageDto;
import com.agriplatform.geography.entity.District;
import com.agriplatform.geography.entity.Mandal;
import com.agriplatform.geography.entity.State;
import com.agriplatform.geography.entity.Village;
import com.agriplatform.geography.mapper.GeographyMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Task 05 — Geography Mapper Unit Tests")
class GeographyMapperTest {

    private GeographyMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new GeographyMapper();
    }

    // =========================================================================
    // State mapping tests
    // =========================================================================

    @Test
    @DisplayName("Maps State entity to StateDto with all fields")
    void maps_state_to_state_dto() {
        UUID id = UUID.randomUUID();
        State state = new State();
        state.setId(id);
        state.setName("Andhra Pradesh");
        state.setCode("AP");
        state.setActive(true);
        state.setCreatedAt(OffsetDateTime.now());
        state.setUpdatedAt(OffsetDateTime.now());

        StateDto dto = mapper.toStateDto(state);

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(id);
        assertThat(dto.name()).isEqualTo("Andhra Pradesh");
        assertThat(dto.code()).isEqualTo("AP");
        assertThat(dto.isActive()).isTrue();
    }

    @Test
    @DisplayName("Maps list of State entities preserving order and content")
    void maps_state_list_to_state_dto_list() {
        State s1 = new State();
        s1.setId(UUID.randomUUID());
        s1.setName("Andhra Pradesh");
        s1.setCode("AP");
        s1.setActive(true);

        State s2 = new State();
        s2.setId(UUID.randomUUID());
        s2.setName("Telangana");
        s2.setCode("TS");
        s2.setActive(true);

        List<StateDto> dtoList = mapper.toStateDtoList(List.of(s1, s2));

        assertThat(dtoList).hasSize(2);
        assertThat(dtoList.get(0).name()).isEqualTo("Andhra Pradesh");
        assertThat(dtoList.get(1).name()).isEqualTo("Telangana");
    }

    // =========================================================================
    // District mapping tests
    // =========================================================================

    @Test
    @DisplayName("Maps District entity to DistrictDto with stateId extracted from parent")
    void maps_district_to_district_dto() {
        UUID stateId = UUID.randomUUID();
        State state = new State();
        state.setId(stateId);

        UUID districtId = UUID.randomUUID();
        District district = new District();
        district.setId(districtId);
        district.setState(state);
        district.setName("Guntur");
        district.setCode("GNT");
        district.setActive(true);

        DistrictDto dto = mapper.toDistrictDto(district);

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(districtId);
        assertThat(dto.stateId()).isEqualTo(stateId);
        assertThat(dto.name()).isEqualTo("Guntur");
        assertThat(dto.code()).isEqualTo("GNT");
        assertThat(dto.isActive()).isTrue();
    }

    @Test
    @DisplayName("Maps District entity with null optional code")
    void maps_district_with_null_code() {
        UUID stateId = UUID.randomUUID();
        State state = new State();
        state.setId(stateId);

        District district = new District();
        district.setId(UUID.randomUUID());
        district.setState(state);
        district.setName("Krishna");
        district.setCode(null);
        district.setActive(false);

        DistrictDto dto = mapper.toDistrictDto(district);

        assertThat(dto.code()).isNull();
        assertThat(dto.isActive()).isFalse();
    }

    // =========================================================================
    // Mandal mapping tests
    // =========================================================================

    @Test
    @DisplayName("Maps Mandal entity to MandalDto with districtId extracted from parent")
    void maps_mandal_to_mandal_dto() {
        UUID districtId = UUID.randomUUID();
        District district = new District();
        district.setId(districtId);

        UUID mandalId = UUID.randomUUID();
        Mandal mandal = new Mandal();
        mandal.setId(mandalId);
        mandal.setDistrict(district);
        mandal.setName("Tenali");
        mandal.setCode("TNL");
        mandal.setActive(true);

        MandalDto dto = mapper.toMandalDto(mandal);

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(mandalId);
        assertThat(dto.districtId()).isEqualTo(districtId);
        assertThat(dto.name()).isEqualTo("Tenali");
        assertThat(dto.code()).isEqualTo("TNL");
        assertThat(dto.isActive()).isTrue();
    }

    // =========================================================================
    // Village mapping tests
    // =========================================================================

    @Test
    @DisplayName("Maps Village entity to VillageDto including latitude and longitude coordinates")
    void maps_village_to_village_dto_with_coordinates() {
        UUID mandalId = UUID.randomUUID();
        Mandal mandal = new Mandal();
        mandal.setId(mandalId);

        UUID villageId = UUID.randomUUID();
        Village village = new Village();
        village.setId(villageId);
        village.setMandal(mandal);
        village.setName("Kolluru");
        village.setCode("KLL");
        village.setLatitude(new BigDecimal("16.123456"));
        village.setLongitude(new BigDecimal("80.654321"));
        village.setActive(true);

        VillageDto dto = mapper.toVillageDto(village);

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(villageId);
        assertThat(dto.mandalId()).isEqualTo(mandalId);
        assertThat(dto.name()).isEqualTo("Kolluru");
        assertThat(dto.code()).isEqualTo("KLL");
        assertThat(dto.latitude()).isEqualTo(new BigDecimal("16.123456"));
        assertThat(dto.longitude()).isEqualTo(new BigDecimal("80.654321"));
        assertThat(dto.isActive()).isTrue();
    }

    @Test
    @DisplayName("Maps Village entity with null coordinates and null code")
    void maps_village_with_null_coordinates_and_code() {
        UUID mandalId = UUID.randomUUID();
        Mandal mandal = new Mandal();
        mandal.setId(mandalId);

        Village village = new Village();
        village.setId(UUID.randomUUID());
        village.setMandal(mandal);
        village.setName("Remote Village");
        village.setCode(null);
        village.setLatitude(null);
        village.setLongitude(null);
        village.setActive(true);

        VillageDto dto = mapper.toVillageDto(village);

        assertThat(dto.code()).isNull();
        assertThat(dto.latitude()).isNull();
        assertThat(dto.longitude()).isNull();
    }
}
