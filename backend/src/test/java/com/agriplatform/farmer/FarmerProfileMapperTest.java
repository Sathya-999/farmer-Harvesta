package com.agriplatform.farmer;

import com.agriplatform.farmer.dto.FarmerProfileResponseDto;
import com.agriplatform.farmer.entity.FarmerProfile;
import com.agriplatform.farmer.mapper.FarmerProfileMapper;
import com.agriplatform.geography.entity.District;
import com.agriplatform.geography.entity.Mandal;
import com.agriplatform.geography.entity.State;
import com.agriplatform.geography.entity.Village;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FarmerProfile Mapper Unit Tests")
class FarmerProfileMapperTest {

    private final FarmerProfileMapper mapper = new FarmerProfileMapper();

    @Test
    @DisplayName("Maps null entity to null DTO")
    void testMapNull() {
        assertThat(mapper.toDto(null)).isNull();
    }

    @Test
    @DisplayName("Maps entity without village (skeleton profile)")
    void testMapEntityWithoutVillage() {
        UUID farmerId = UUID.randomUUID();
        FarmerProfile profile = new FarmerProfile();
        profile.setId(farmerId);
        profile.setFullName("Anand Sharma");
        profile.setProfileStatus("ACTIVE");
        profile.setOnboardingCompleted(false);
        OffsetDateTime now = OffsetDateTime.now();
        profile.setCreatedAt(now);
        profile.setUpdatedAt(now);

        FarmerProfileResponseDto dto = mapper.toDto(profile);

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(farmerId);
        assertThat(dto.fullName()).isEqualTo("Anand Sharma");
        assertThat(dto.homeVillageId()).isNull();
        assertThat(dto.villageName()).isNull();
        assertThat(dto.mandalId()).isNull();
        assertThat(dto.mandalName()).isNull();
        assertThat(dto.districtId()).isNull();
        assertThat(dto.districtName()).isNull();
        assertThat(dto.stateId()).isNull();
        assertThat(dto.stateName()).isNull();
        assertThat(dto.profileStatus()).isEqualTo("ACTIVE");
        assertThat(dto.onboardingCompleted()).isFalse();
        assertThat(dto.version()).isEqualTo(now.toInstant().toEpochMilli());
    }

    @Test
    @DisplayName("Maps entity with complete geography hierarchy")
    void testMapEntityWithCompleteGeography() {
        UUID stateId = UUID.randomUUID();
        State state = new State();
        state.setId(stateId);
        state.setName("Andhra Pradesh");

        UUID districtId = UUID.randomUUID();
        District district = new District();
        district.setId(districtId);
        district.setName("Chittoor");
        district.setState(state);

        UUID mandalId = UUID.randomUUID();
        Mandal mandal = new Mandal();
        mandal.setId(mandalId);
        mandal.setName("Chittoor Urban");
        mandal.setDistrict(district);

        UUID villageId = UUID.randomUUID();
        Village village = new Village();
        village.setId(villageId);
        village.setName("Mapakshi");
        village.setMandal(mandal);

        UUID farmerId = UUID.randomUUID();
        FarmerProfile profile = new FarmerProfile();
        profile.setId(farmerId);
        profile.setFullName("Venkat Rao");
        profile.setHomeVillage(village);
        profile.setProfileStatus("ACTIVE");
        profile.setOnboardingCompleted(true);
        OffsetDateTime now = OffsetDateTime.now();
        profile.setCreatedAt(now);
        profile.setUpdatedAt(now);

        FarmerProfileResponseDto dto = mapper.toDto(profile);

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(farmerId);
        assertThat(dto.fullName()).isEqualTo("Venkat Rao");
        assertThat(dto.homeVillageId()).isEqualTo(villageId);
        assertThat(dto.villageName()).isEqualTo("Mapakshi");
        assertThat(dto.mandalId()).isEqualTo(mandalId);
        assertThat(dto.mandalName()).isEqualTo("Chittoor Urban");
        assertThat(dto.districtId()).isEqualTo(districtId);
        assertThat(dto.districtName()).isEqualTo("Chittoor");
        assertThat(dto.stateId()).isEqualTo(stateId);
        assertThat(dto.stateName()).isEqualTo("Andhra Pradesh");
        assertThat(dto.onboardingCompleted()).isTrue();
        assertThat(dto.version()).isEqualTo(now.toInstant().toEpochMilli());
    }
}
