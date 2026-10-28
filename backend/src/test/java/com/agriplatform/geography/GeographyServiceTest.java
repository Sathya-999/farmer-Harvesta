package com.agriplatform.geography;

import com.agriplatform.common.exception.ResourceNotFoundException;
import com.agriplatform.geography.dto.DistrictDto;
import com.agriplatform.geography.dto.MandalDto;
import com.agriplatform.geography.dto.StateDto;
import com.agriplatform.geography.dto.VillageDto;
import com.agriplatform.geography.entity.District;
import com.agriplatform.geography.entity.Mandal;
import com.agriplatform.geography.entity.State;
import com.agriplatform.geography.entity.Village;
import com.agriplatform.geography.mapper.GeographyMapper;
import com.agriplatform.geography.repository.DistrictRepository;
import com.agriplatform.geography.repository.MandalRepository;
import com.agriplatform.geography.repository.StateRepository;
import com.agriplatform.geography.repository.VillageRepository;
import com.agriplatform.geography.service.GeographyService;
import com.agriplatform.geography.service.GeographyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Task 05 — Geography Service Unit Tests")
class GeographyServiceTest {

    @Mock
    private StateRepository stateRepository;

    @Mock
    private DistrictRepository districtRepository;

    @Mock
    private MandalRepository mandalRepository;

    @Mock
    private VillageRepository villageRepository;

    private GeographyService geographyService;

    @BeforeEach
    void setUp() {
        geographyService = new GeographyServiceImpl(
                stateRepository,
                districtRepository,
                mandalRepository,
                villageRepository,
                new GeographyMapper()
        );
    }

    // =========================================================================
    // States
    // =========================================================================

    @Test
    @DisplayName("getAllActiveStates returns mapped StateDto list from repository")
    void get_all_active_states_returns_mapped_list() {
        State state = new State();
        state.setId(UUID.randomUUID());
        state.setName("Andhra Pradesh");
        state.setCode("AP");
        state.setActive(true);

        when(stateRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(state));

        List<StateDto> result = geographyService.getAllActiveStates();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Andhra Pradesh");
        assertThat(result.get(0).code()).isEqualTo("AP");
        verify(stateRepository, times(1)).findByIsActiveTrueOrderByNameAsc();
    }

    // =========================================================================
    // Districts
    // =========================================================================

    @Test
    @DisplayName("getActiveDistrictsByState returns mapped districts when state exists")
    void get_active_districts_by_state_success() {
        UUID stateId = UUID.randomUUID();
        State state = new State();
        state.setId(stateId);

        District district = new District();
        district.setId(UUID.randomUUID());
        district.setState(state);
        district.setName("Guntur");
        district.setCode("GNT");
        district.setActive(true);

        when(stateRepository.existsById(stateId)).thenReturn(true);
        when(districtRepository.findActiveByStateId(stateId)).thenReturn(List.of(district));

        List<DistrictDto> result = geographyService.getActiveDistrictsByState(stateId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Guntur");
        assertThat(result.get(0).stateId()).isEqualTo(stateId);
        verify(stateRepository, times(1)).existsById(stateId);
        verify(districtRepository, times(1)).findActiveByStateId(stateId);
    }

    @Test
    @DisplayName("getActiveDistrictsByState throws ResourceNotFoundException when state does not exist")
    void get_active_districts_by_state_not_found() {
        UUID unknownStateId = UUID.randomUUID();
        when(stateRepository.existsById(unknownStateId)).thenReturn(false);

        assertThatThrownBy(() -> geographyService.getActiveDistrictsByState(unknownStateId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("State not found: " + unknownStateId);

        verify(stateRepository, times(1)).existsById(unknownStateId);
        verifyNoInteractions(districtRepository);
    }

    // =========================================================================
    // Mandals
    // =========================================================================

    @Test
    @DisplayName("getActiveMandalsByDistrict returns mapped mandals when district exists")
    void get_active_mandals_by_district_success() {
        UUID districtId = UUID.randomUUID();
        District district = new District();
        district.setId(districtId);

        Mandal mandal = new Mandal();
        mandal.setId(UUID.randomUUID());
        mandal.setDistrict(district);
        mandal.setName("Tenali");
        mandal.setCode("TNL");
        mandal.setActive(true);

        when(districtRepository.existsById(districtId)).thenReturn(true);
        when(mandalRepository.findActiveByDistrictId(districtId)).thenReturn(List.of(mandal));

        List<MandalDto> result = geographyService.getActiveMandalsByDistrict(districtId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Tenali");
        assertThat(result.get(0).districtId()).isEqualTo(districtId);
        verify(districtRepository, times(1)).existsById(districtId);
        verify(mandalRepository, times(1)).findActiveByDistrictId(districtId);
    }

    @Test
    @DisplayName("getActiveMandalsByDistrict throws ResourceNotFoundException when district does not exist")
    void get_active_mandals_by_district_not_found() {
        UUID unknownDistrictId = UUID.randomUUID();
        when(districtRepository.existsById(unknownDistrictId)).thenReturn(false);

        assertThatThrownBy(() -> geographyService.getActiveMandalsByDistrict(unknownDistrictId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("District not found: " + unknownDistrictId);

        verify(districtRepository, times(1)).existsById(unknownDistrictId);
        verifyNoInteractions(mandalRepository);
    }

    // =========================================================================
    // Villages
    // =========================================================================

    @Test
    @DisplayName("getActiveVillagesByMandal returns mapped villages when mandal exists")
    void get_active_villages_by_mandal_success() {
        UUID mandalId = UUID.randomUUID();
        Mandal mandal = new Mandal();
        mandal.setId(mandalId);

        Village village = new Village();
        village.setId(UUID.randomUUID());
        village.setMandal(mandal);
        village.setName("Kolluru");
        village.setCode("KLL");
        village.setLatitude(new BigDecimal("16.123400"));
        village.setLongitude(new BigDecimal("80.567800"));
        village.setActive(true);

        when(mandalRepository.existsById(mandalId)).thenReturn(true);
        when(villageRepository.findActiveByMandalId(mandalId)).thenReturn(List.of(village));

        List<VillageDto> result = geographyService.getActiveVillagesByMandal(mandalId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Kolluru");
        assertThat(result.get(0).mandalId()).isEqualTo(mandalId);
        assertThat(result.get(0).latitude()).isEqualTo(new BigDecimal("16.123400"));
        assertThat(result.get(0).longitude()).isEqualTo(new BigDecimal("80.567800"));
        verify(mandalRepository, times(1)).existsById(mandalId);
        verify(villageRepository, times(1)).findActiveByMandalId(mandalId);
    }

    @Test
    @DisplayName("getActiveVillagesByMandal throws ResourceNotFoundException when mandal does not exist")
    void get_active_villages_by_mandal_not_found() {
        UUID unknownMandalId = UUID.randomUUID();
        when(mandalRepository.existsById(unknownMandalId)).thenReturn(false);

        assertThatThrownBy(() -> geographyService.getActiveVillagesByMandal(unknownMandalId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Mandal not found: " + unknownMandalId);

        verify(mandalRepository, times(1)).existsById(unknownMandalId);
        verifyNoInteractions(villageRepository);
    }

    // =========================================================================
    // Hierarchy completeness test
    // =========================================================================

    @Test
    @DisplayName("Complete hierarchy State -> District -> Mandal -> Village resolution flow")
    void complete_hierarchy_navigation_flow() {
        UUID stateId = UUID.randomUUID();
        UUID districtId = UUID.randomUUID();
        UUID mandalId = UUID.randomUUID();
        UUID villageId = UUID.randomUUID();

        State state = new State();
        state.setId(stateId);
        state.setName("Andhra Pradesh");

        District district = new District();
        district.setId(districtId);
        district.setState(state);
        district.setName("Guntur");

        Mandal mandal = new Mandal();
        mandal.setId(mandalId);
        mandal.setDistrict(district);
        mandal.setName("Tenali");

        Village village = new Village();
        village.setId(villageId);
        village.setMandal(mandal);
        village.setName("Kolluru");

        when(stateRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(state));
        when(stateRepository.existsById(stateId)).thenReturn(true);
        when(districtRepository.findActiveByStateId(stateId)).thenReturn(List.of(district));
        when(districtRepository.existsById(districtId)).thenReturn(true);
        when(mandalRepository.findActiveByDistrictId(districtId)).thenReturn(List.of(mandal));
        when(mandalRepository.existsById(mandalId)).thenReturn(true);
        when(villageRepository.findActiveByMandalId(mandalId)).thenReturn(List.of(village));

        List<StateDto> states = geographyService.getAllActiveStates();
        assertThat(states).hasSize(1);
        UUID resolvedStateId = states.get(0).id();

        List<DistrictDto> districts = geographyService.getActiveDistrictsByState(resolvedStateId);
        assertThat(districts).hasSize(1);
        UUID resolvedDistrictId = districts.get(0).id();

        List<MandalDto> mandals = geographyService.getActiveMandalsByDistrict(resolvedDistrictId);
        assertThat(mandals).hasSize(1);
        UUID resolvedMandalId = mandals.get(0).id();

        List<VillageDto> villages = geographyService.getActiveVillagesByMandal(resolvedMandalId);
        assertThat(villages).hasSize(1);
        assertThat(villages.get(0).id()).isEqualTo(villageId);
        assertThat(villages.get(0).name()).isEqualTo("Kolluru");
    }
}
