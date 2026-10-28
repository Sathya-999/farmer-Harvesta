package com.agriplatform.geography.ingestion.service;

import com.agriplatform.geography.entity.District;
import com.agriplatform.geography.entity.Mandal;
import com.agriplatform.geography.entity.State;
import com.agriplatform.geography.entity.Village;
import com.agriplatform.geography.ingestion.model.ImportMode;
import com.agriplatform.geography.ingestion.model.ImportSummary;
import com.agriplatform.geography.ingestion.source.LgdCsvGeographySource;
import com.agriplatform.geography.repository.DistrictRepository;
import com.agriplatform.geography.repository.MandalRepository;
import com.agriplatform.geography.repository.StateRepository;
import com.agriplatform.geography.repository.VillageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.StringReader;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GeographyIngestionService Unit Tests")
class GeographyIngestionServiceTest {

    @Mock
    private StateRepository stateRepository;

    @Mock
    private DistrictRepository districtRepository;

    @Mock
    private MandalRepository mandalRepository;

    @Mock
    private VillageRepository villageRepository;

    private GeographyIngestionServiceImpl ingestionService;

    @BeforeEach
    void setUp() {
        ingestionService = new GeographyIngestionServiceImpl(
                stateRepository, districtRepository, mandalRepository, villageRepository);
    }

    @Test
    @DisplayName("Should successfully ingest complete hierarchy in IMPORT mode")
    void testCompleteHierarchyImport() {
        String csv = "state_code,state_name,district_code,district_name,mandal_code,mandal_name,village_code,village_name,latitude,longitude\n" +
                "TS,Telangana,501,Rangareddy,5001,Rajendranagar,50001,Budvel,17.318,78.406\n";

        when(stateRepository.findByCodeIgnoreCase("TS")).thenReturn(Optional.empty());
        when(stateRepository.save(any(State.class))).thenAnswer(i -> i.getArgument(0));

        when(districtRepository.findByStateIdAndCodeIgnoreCase(any(UUID.class), eq("501"))).thenReturn(Optional.empty());
        when(districtRepository.save(any(District.class))).thenAnswer(i -> i.getArgument(0));

        when(mandalRepository.findByDistrictIdAndCodeIgnoreCase(any(UUID.class), eq("5001"))).thenReturn(Optional.empty());
        when(mandalRepository.save(any(Mandal.class))).thenAnswer(i -> i.getArgument(0));

        when(villageRepository.findByMandalIdAndCodeIgnoreCase(any(UUID.class), eq("50001"))).thenReturn(Optional.empty());
        when(villageRepository.save(any(Village.class))).thenAnswer(i -> i.getArgument(0));

        LgdCsvGeographySource source = new LgdCsvGeographySource("test.csv", new StringReader(csv));
        ImportSummary summary = ingestionService.ingest(source, ImportMode.IMPORT);

        assertThat(summary.isSuccessful()).isTrue();
        assertThat(summary.getTotalRowsRead()).isEqualTo(1);
        assertThat(summary.getValidRowsCount()).isEqualTo(1);
        assertThat(summary.getInvalidRowsCount()).isEqualTo(0);
        assertThat(summary.getStatesInserted()).isEqualTo(1);
        assertThat(summary.getDistrictsInserted()).isEqualTo(1);
        assertThat(summary.getMandalsInserted()).isEqualTo(1);
        assertThat(summary.getVillagesInserted()).isEqualTo(1);

        verify(stateRepository, times(1)).save(any(State.class));
        verify(districtRepository, times(1)).save(any(District.class));
        verify(mandalRepository, times(1)).save(any(Mandal.class));
        verify(villageRepository, times(1)).save(any(Village.class));
    }

    @Test
    @DisplayName("Should simulate counts without writing to database in DRY_RUN mode")
    void testDryRunModeDoesNotPersist() {
        String csv = "state_code,state_name,district_code,district_name,mandal_code,mandal_name,village_code,village_name,latitude,longitude\n" +
                "TS,Telangana,501,Rangareddy,5001,Rajendranagar,50001,Budvel,17.318,78.406\n";

        when(stateRepository.findByCodeIgnoreCase("TS")).thenReturn(Optional.empty());
        when(districtRepository.findByStateIdAndCodeIgnoreCase(any(UUID.class), eq("501"))).thenReturn(Optional.empty());
        when(mandalRepository.findByDistrictIdAndCodeIgnoreCase(any(UUID.class), eq("5001"))).thenReturn(Optional.empty());
        when(villageRepository.findByMandalIdAndCodeIgnoreCase(any(UUID.class), eq("50001"))).thenReturn(Optional.empty());

        LgdCsvGeographySource source = new LgdCsvGeographySource("test.csv", new StringReader(csv));
        ImportSummary summary = ingestionService.ingest(source, ImportMode.DRY_RUN);

        assertThat(summary.isSuccessful()).isTrue();
        assertThat(summary.getMode()).isEqualTo(ImportMode.DRY_RUN);
        assertThat(summary.getStatesInserted()).isEqualTo(1);
        assertThat(summary.getDistrictsInserted()).isEqualTo(1);
        assertThat(summary.getMandalsInserted()).isEqualTo(1);
        assertThat(summary.getVillagesInserted()).isEqualTo(1);

        // Verify NO saves were performed
        verify(stateRepository, never()).save(any(State.class));
        verify(districtRepository, never()).save(any(District.class));
        verify(mandalRepository, never()).save(any(Mandal.class));
        verify(villageRepository, never()).save(any(Village.class));
    }

    @Test
    @DisplayName("Should be idempotent on repeat import: mark existing records as UNCHANGED with 0 duplicates")
    void testIdempotentRepeatImport() {
        String csv = "state_code,state_name,district_code,district_name,mandal_code,mandal_name,village_code,village_name,latitude,longitude\n" +
                "TS,Telangana,501,Rangareddy,5001,Rajendranagar,50001,Budvel,17.318000,78.406000\n";

        UUID stateId = UUID.randomUUID();
        State existingState = new State();
        existingState.setId(stateId);
        existingState.setCode("TS");
        existingState.setName("Telangana");
        existingState.setActive(true);

        UUID districtId = UUID.randomUUID();
        District existingDistrict = new District();
        existingDistrict.setId(districtId);
        existingDistrict.setState(existingState);
        existingDistrict.setCode("501");
        existingDistrict.setName("Rangareddy");
        existingDistrict.setActive(true);

        UUID mandalId = UUID.randomUUID();
        Mandal existingMandal = new Mandal();
        existingMandal.setId(mandalId);
        existingMandal.setDistrict(existingDistrict);
        existingMandal.setCode("5001");
        existingMandal.setName("Rajendranagar");
        existingMandal.setActive(true);

        UUID villageId = UUID.randomUUID();
        Village existingVillage = new Village();
        existingVillage.setId(villageId);
        existingVillage.setMandal(existingMandal);
        existingVillage.setCode("50001");
        existingVillage.setName("Budvel");
        existingVillage.setLatitude(new BigDecimal("17.318000"));
        existingVillage.setLongitude(new BigDecimal("78.406000"));
        existingVillage.setActive(true);

        when(stateRepository.findByCodeIgnoreCase("TS")).thenReturn(Optional.of(existingState));
        when(districtRepository.findByStateIdAndCodeIgnoreCase(stateId, "501")).thenReturn(Optional.of(existingDistrict));
        when(mandalRepository.findByDistrictIdAndCodeIgnoreCase(districtId, "5001")).thenReturn(Optional.of(existingMandal));
        when(villageRepository.findByMandalIdAndCodeIgnoreCase(mandalId, "50001")).thenReturn(Optional.of(existingVillage));

        LgdCsvGeographySource source = new LgdCsvGeographySource("repeat.csv", new StringReader(csv));
        ImportSummary summary = ingestionService.ingest(source, ImportMode.IMPORT);

        assertThat(summary.isSuccessful()).isTrue();
        assertThat(summary.getStatesUnchanged()).isEqualTo(1);
        assertThat(summary.getDistrictsUnchanged()).isEqualTo(1);
        assertThat(summary.getMandalsUnchanged()).isEqualTo(1);
        assertThat(summary.getVillagesUnchanged()).isEqualTo(1);

        assertThat(summary.getStatesInserted()).isEqualTo(0);
        assertThat(summary.getDistrictsInserted()).isEqualTo(0);
        assertThat(summary.getMandalsInserted()).isEqualTo(0);
        assertThat(summary.getVillagesInserted()).isEqualTo(0);

        verify(stateRepository, never()).save(any(State.class));
        verify(villageRepository, never()).save(any(Village.class));
    }

    @Test
    @DisplayName("Should update entity attributes in-place while preserving local UUID PK")
    void testUpdateInPlacePreservingUuid() {
        String csv = "state_code,state_name,district_code,district_name,mandal_code,mandal_name,village_code,village_name,latitude,longitude\n" +
                "TS,Telangana,501,Rangareddy,5001,Rajendranagar,50001,Budvel Updated,17.350000,78.420000\n";

        UUID stateId = UUID.randomUUID();
        State existingState = new State();
        existingState.setId(stateId);
        existingState.setCode("TS");
        existingState.setName("Telangana");
        existingState.setActive(true);

        UUID districtId = UUID.randomUUID();
        District existingDistrict = new District();
        existingDistrict.setId(districtId);
        existingDistrict.setState(existingState);
        existingDistrict.setCode("501");
        existingDistrict.setName("Rangareddy");
        existingDistrict.setActive(true);

        UUID mandalId = UUID.randomUUID();
        Mandal existingMandal = new Mandal();
        existingMandal.setId(mandalId);
        existingMandal.setDistrict(existingDistrict);
        existingMandal.setCode("5001");
        existingMandal.setName("Rajendranagar");
        existingMandal.setActive(true);

        UUID villageId = UUID.randomUUID();
        Village existingVillage = new Village();
        existingVillage.setId(villageId);
        existingVillage.setMandal(existingMandal);
        existingVillage.setCode("50001");
        existingVillage.setName("Budvel Old"); // Different name
        existingVillage.setLatitude(new BigDecimal("17.318000")); // Different lat
        existingVillage.setLongitude(new BigDecimal("78.406000"));
        existingVillage.setActive(true);

        when(stateRepository.findByCodeIgnoreCase("TS")).thenReturn(Optional.of(existingState));
        when(districtRepository.findByStateIdAndCodeIgnoreCase(stateId, "501")).thenReturn(Optional.of(existingDistrict));
        when(mandalRepository.findByDistrictIdAndCodeIgnoreCase(districtId, "5001")).thenReturn(Optional.of(existingMandal));
        when(villageRepository.findByMandalIdAndCodeIgnoreCase(mandalId, "50001")).thenReturn(Optional.of(existingVillage));
        when(villageRepository.save(any(Village.class))).thenAnswer(i -> i.getArgument(0));

        LgdCsvGeographySource source = new LgdCsvGeographySource("update.csv", new StringReader(csv));
        ImportSummary summary = ingestionService.ingest(source, ImportMode.IMPORT);

        assertThat(summary.isSuccessful()).isTrue();
        assertThat(summary.getVillagesUpdated()).isEqualTo(1);
        assertThat(summary.getVillagesInserted()).isEqualTo(0);

        verify(villageRepository, times(1)).save(argThat(v ->
                v.getId().equals(villageId) && // UUID preserved!
                v.getName().equals("Budvel Updated") &&
                v.getLatitude().compareTo(new BigDecimal("17.350000")) == 0
        ));
    }

    @Test
    @DisplayName("Should quarantine and reject invalid rows without affecting valid rows")
    void testPartialFailureRejection() {
        String csv = "state_code,state_name,district_code,district_name,mandal_code,mandal_name,village_code,village_name,latitude,longitude\n" +
                "TS,Telangana,501,Rangareddy,5001,Rajendranagar,50001,Budvel,999.0,78.406\n" + // Invalid lat
                "TS,Telangana,501,Rangareddy,5001,Rajendranagar,50002,Attapur,17.350,78.420\n";  // Valid

        when(stateRepository.findByCodeIgnoreCase("TS")).thenReturn(Optional.empty());
        when(stateRepository.save(any(State.class))).thenAnswer(i -> i.getArgument(0));

        when(districtRepository.findByStateIdAndCodeIgnoreCase(any(UUID.class), eq("501"))).thenReturn(Optional.empty());
        when(districtRepository.save(any(District.class))).thenAnswer(i -> i.getArgument(0));

        when(mandalRepository.findByDistrictIdAndCodeIgnoreCase(any(UUID.class), eq("5001"))).thenReturn(Optional.empty());
        when(mandalRepository.save(any(Mandal.class))).thenAnswer(i -> i.getArgument(0));

        when(villageRepository.findByMandalIdAndCodeIgnoreCase(any(UUID.class), eq("50002"))).thenReturn(Optional.empty());
        when(villageRepository.save(any(Village.class))).thenAnswer(i -> i.getArgument(0));

        LgdCsvGeographySource source = new LgdCsvGeographySource("partial.csv", new StringReader(csv));
        ImportSummary summary = ingestionService.ingest(source, ImportMode.IMPORT);

        assertThat(summary.isSuccessful()).isFalse(); // Flagged false because errors were present
        assertThat(summary.getTotalRowsRead()).isEqualTo(2);
        assertThat(summary.getValidRowsCount()).isEqualTo(1);
        assertThat(summary.getInvalidRowsCount()).isEqualTo(1);
        assertThat(summary.getVillagesInserted()).isEqualTo(1);
        assertThat(summary.getErrors()).hasSize(1);
        assertThat(summary.getErrors().get(0).field()).isEqualTo("latitude");
    }

    @Test
    @DisplayName("Should correctly preserve Indian Unicode scripts")
    void testUnicodePreservation() {
        String csv = "state_code,state_name,district_code,district_name,mandal_code,mandal_name,village_code,village_name\n" +
                "AP,ఆంధ్రప్రదేశ్,502,అనంతపురం,5002,ఆత్మకూరు,50003,ఆత్మకూరు గ్రామం\n";

        when(stateRepository.findByCodeIgnoreCase("AP")).thenReturn(Optional.empty());
        when(stateRepository.save(any(State.class))).thenAnswer(i -> i.getArgument(0));

        when(districtRepository.findByStateIdAndCodeIgnoreCase(any(UUID.class), eq("502"))).thenReturn(Optional.empty());
        when(districtRepository.save(any(District.class))).thenAnswer(i -> i.getArgument(0));

        when(mandalRepository.findByDistrictIdAndCodeIgnoreCase(any(UUID.class), eq("5002"))).thenReturn(Optional.empty());
        when(mandalRepository.save(any(Mandal.class))).thenAnswer(i -> i.getArgument(0));

        when(villageRepository.findByMandalIdAndCodeIgnoreCase(any(UUID.class), eq("50003"))).thenReturn(Optional.empty());
        when(villageRepository.save(any(Village.class))).thenAnswer(i -> i.getArgument(0));

        LgdCsvGeographySource source = new LgdCsvGeographySource("unicode.csv", new StringReader(csv));
        ImportSummary summary = ingestionService.ingest(source, ImportMode.IMPORT);

        assertThat(summary.isSuccessful()).isTrue();
        assertThat(summary.getVillagesInserted()).isEqualTo(1);

        verify(stateRepository).save(argThat(s -> s.getName().equals("ఆంధ్రప్రదేశ్")));
        verify(districtRepository).save(argThat(d -> d.getName().equals("అనంతపురం")));
        verify(mandalRepository).save(argThat(m -> m.getName().equals("ఆత్మకూరు")));
        verify(villageRepository).save(argThat(v -> v.getName().equals("ఆత్మకూరు గ్రామం")));
    }

    @Test
    @DisplayName("Should insert both villages when same name in same mandal has different LGD codes")
    void testSameMandalSameVillageNameDifferentCodesAreBothInserted() {
        String csv = "state_code,state_name,district_code,district_name,mandal_code,mandal_name,village_code,village_name\n" +
                "28,Andhra Pradesh,503,Chittoor,7427,Chittoor Urban,596707,Mapakshi\n" +
                "28,Andhra Pradesh,503,Chittoor,7427,Chittoor Urban,596708,Mapakshi\n";

        when(stateRepository.findByCodeIgnoreCase("28")).thenReturn(Optional.empty());
        when(stateRepository.save(any(State.class))).thenAnswer(i -> i.getArgument(0));

        when(districtRepository.findByStateIdAndCodeIgnoreCase(any(UUID.class), eq("503"))).thenReturn(Optional.empty());
        when(districtRepository.save(any(District.class))).thenAnswer(i -> i.getArgument(0));

        when(mandalRepository.findByDistrictIdAndCodeIgnoreCase(any(UUID.class), eq("7427"))).thenReturn(Optional.empty());
        when(mandalRepository.save(any(Mandal.class))).thenAnswer(i -> i.getArgument(0));

        when(villageRepository.findByMandalIdAndCodeIgnoreCase(any(UUID.class), eq("596707"))).thenReturn(Optional.empty());
        when(villageRepository.findByMandalIdAndCodeIgnoreCase(any(UUID.class), eq("596708"))).thenReturn(Optional.empty());
        when(villageRepository.save(any(Village.class))).thenAnswer(i -> i.getArgument(0));

        LgdCsvGeographySource source = new LgdCsvGeographySource("same_name.csv", new StringReader(csv));
        ImportSummary summary = ingestionService.ingest(source, ImportMode.IMPORT);

        assertThat(summary.isSuccessful()).isTrue();
        assertThat(summary.getTotalRowsRead()).isEqualTo(2);
        assertThat(summary.getValidRowsCount()).isEqualTo(2);
        assertThat(summary.getInvalidRowsCount()).isEqualTo(0);
        assertThat(summary.getDuplicateRowsCount()).isEqualTo(0);
        assertThat(summary.getVillagesInserted()).isEqualTo(2);

        verify(villageRepository, times(2)).save(any(Village.class));
    }
}
