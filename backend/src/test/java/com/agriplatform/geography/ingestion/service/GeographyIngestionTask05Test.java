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

/**
 * Task 05 targeted regression tests for the Geography Engine.
 *
 * <p>Covers:
 * <ol>
 *   <li>Blank code cannot erase a known official code (code-stability on update).</li>
 *   <li>UUID primary-key stability across ingestion cycles.</li>
 *   <li>Parent-child integrity: village references correct mandal UUID after repeated imports.</li>
 *   <li>Mapakshi 596707/596708 regression.</li>
 *   <li>Partial-failure recovery: valid rows proceed after invalid rows are quarantined.</li>
 *   <li>Idempotency: repeat import produces 0 inserts, 0 duplicates, correct UNCHANGED count.</li>
 *   <li>Source-coverage assertion: the production_lgd_geography.csv fixture covers 91 mandals
 *       and 1495 villages.</li>
 *   <li>DRY_RUN mode: no DB writes, correct simulated counts.</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Task 05 Geography Engine Targeted Regression Tests")
class GeographyIngestionTask05Test {

    @Mock private StateRepository    stateRepository;
    @Mock private DistrictRepository districtRepository;
    @Mock private MandalRepository   mandalRepository;
    @Mock private VillageRepository  villageRepository;

    private GeographyIngestionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new GeographyIngestionServiceImpl(
                stateRepository, districtRepository, mandalRepository, villageRepository);
    }

    // -------------------------------------------------------------------------
    // 1. BLANK CODE CANNOT ERASE KNOWN OFFICIAL CODE
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Blank village code in source does not erase existing official code")
    void testBlankVillageCodeDoesNotEraseExistingCode() {
        UUID stateId    = UUID.randomUUID();
        UUID districtId = UUID.randomUUID();
        UUID mandalId   = UUID.randomUUID();
        UUID villageId  = UUID.randomUUID();

        State s = makeState(stateId, "28", "Andhra Pradesh");
        District d = makeDistrict(districtId, s, "503", "Chittoor");
        Mandal m = makeMandal(mandalId, d, "7427", "Chittoor Urban");

        Village existing = new Village();
        existing.setId(villageId); existing.setMandal(m); existing.setCode("596707");
        existing.setName("Mapakshi"); existing.setActive(true);
        existing.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        existing.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        // blank village_code in source row
        String csv = "state_code,state_name,district_code,district_name,mandal_code,mandal_name,village_code,village_name\n"
                + "28,Andhra Pradesh,503,Chittoor,7427,Chittoor Urban,,Mapakshi\n";

        when(stateRepository.findByCodeIgnoreCase("28")).thenReturn(Optional.of(s));
        when(districtRepository.findByStateIdAndCodeIgnoreCase(stateId, "503")).thenReturn(Optional.of(d));
        when(mandalRepository.findByDistrictIdAndCodeIgnoreCase(districtId, "7427")).thenReturn(Optional.of(m));
        when(villageRepository.findByMandalIdAndNameIgnoreCase(mandalId, "Mapakshi")).thenReturn(Optional.of(existing));

        ImportSummary summary = service.ingest(
                new LgdCsvGeographySource("blank-code.csv", new StringReader(csv)), ImportMode.IMPORT);

        assertThat(summary.isSuccessful()).isTrue();
        assertThat(summary.getVillagesUnchanged()).isEqualTo(1);
        assertThat(summary.getVillagesInserted()).isEqualTo(0);
        // Existing official code preserved — save() NEVER called with null code
        verify(villageRepository, never()).save(argThat(v -> v.getCode() == null));
        assertThat(existing.getCode()).isEqualTo("596707");
    }

    @Test
    @DisplayName("Blank mandal code in source does not erase existing official code")
    void testBlankMandalCodeDoesNotEraseExistingCode() {
        UUID stateId    = UUID.randomUUID();
        UUID districtId = UUID.randomUUID();
        UUID mandalId   = UUID.randomUUID();

        State s = makeState(stateId, "36", "Telangana");
        District d = makeDistrict(districtId, s, "501", "Rangareddy");
        Mandal existing = makeMandal(mandalId, d, "5001", "Rajendranagar");

        String csv = "state_code,state_name,district_code,district_name,mandal_code,mandal_name\n"
                + "36,Telangana,501,Rangareddy,,Rajendranagar\n";

        when(stateRepository.findByCodeIgnoreCase("36")).thenReturn(Optional.of(s));
        when(districtRepository.findByStateIdAndCodeIgnoreCase(stateId, "501")).thenReturn(Optional.of(d));
        when(mandalRepository.findByDistrictIdAndNameIgnoreCase(districtId, "Rajendranagar")).thenReturn(Optional.of(existing));

        ImportSummary summary = service.ingest(
                new LgdCsvGeographySource("blank-mandal.csv", new StringReader(csv)), ImportMode.IMPORT);

        assertThat(summary.isSuccessful()).isTrue();
        assertThat(summary.getMandalsUnchanged()).isEqualTo(1);
        assertThat(existing.getCode()).isEqualTo("5001");
        verify(mandalRepository, never()).save(argThat(mn -> mn.getCode() == null));
    }

    // -------------------------------------------------------------------------
    // 2. UUID PRIMARY-KEY STABILITY
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("UUID is preserved on in-place name update — not replaced")
    void testUuidPreservedOnUpdate() {
        UUID stateId    = UUID.randomUUID();
        UUID districtId = UUID.randomUUID();
        UUID mandalId   = UUID.randomUUID();
        UUID villageId  = UUID.randomUUID();

        State s = makeState(stateId, "36", "Telangana");
        District d = makeDistrict(districtId, s, "501", "Rangareddy");
        Mandal m = makeMandal(mandalId, d, "5001", "Rajendranagar");

        Village existing = new Village();
        existing.setId(villageId); existing.setMandal(m); existing.setCode("50001");
        existing.setName("Old Name"); existing.setActive(true);
        existing.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        existing.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        String csv = "state_code,state_name,district_code,district_name,mandal_code,mandal_name,village_code,village_name\n"
                + "36,Telangana,501,Rangareddy,5001,Rajendranagar,50001,New Name\n";

        when(stateRepository.findByCodeIgnoreCase("36")).thenReturn(Optional.of(s));
        when(districtRepository.findByStateIdAndCodeIgnoreCase(stateId, "501")).thenReturn(Optional.of(d));
        when(mandalRepository.findByDistrictIdAndCodeIgnoreCase(districtId, "5001")).thenReturn(Optional.of(m));
        when(villageRepository.findByMandalIdAndCodeIgnoreCase(mandalId, "50001")).thenReturn(Optional.of(existing));
        when(villageRepository.save(any(Village.class))).thenAnswer(i -> i.getArgument(0));

        ImportSummary summary = service.ingest(
                new LgdCsvGeographySource("update.csv", new StringReader(csv)), ImportMode.IMPORT);

        assertThat(summary.isSuccessful()).isTrue();
        assertThat(summary.getVillagesUpdated()).isEqualTo(1);
        verify(villageRepository).save(argThat(v ->
                v.getId().equals(villageId) && "New Name".equals(v.getName()) && "50001".equals(v.getCode())));
    }

    // -------------------------------------------------------------------------
    // 3. PARENT-CHILD INTEGRITY
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Newly inserted village carries the correct mandal UUID as foreign key")
    void testNewVillageCarriesCorrectMandalForeignKey() {
        UUID stateId    = UUID.randomUUID();
        UUID districtId = UUID.randomUUID();
        UUID mandalId   = UUID.randomUUID();

        State s = makeState(stateId, "28", "Andhra Pradesh");
        District d = makeDistrict(districtId, s, "503", "Chittoor");
        Mandal m = makeMandal(mandalId, d, "7427", "Chittoor Urban");

        String csv = "state_code,state_name,district_code,district_name,mandal_code,mandal_name,village_code,village_name\n"
                + "28,Andhra Pradesh,503,Chittoor,7427,Chittoor Urban,596707,Mapakshi\n";

        when(stateRepository.findByCodeIgnoreCase("28")).thenReturn(Optional.of(s));
        when(districtRepository.findByStateIdAndCodeIgnoreCase(stateId, "503")).thenReturn(Optional.of(d));
        when(mandalRepository.findByDistrictIdAndCodeIgnoreCase(districtId, "7427")).thenReturn(Optional.of(m));
        when(villageRepository.findByMandalIdAndCodeIgnoreCase(mandalId, "596707")).thenReturn(Optional.empty());
        when(villageRepository.save(any(Village.class))).thenAnswer(i -> i.getArgument(0));

        ImportSummary summary = service.ingest(
                new LgdCsvGeographySource("fk.csv", new StringReader(csv)), ImportMode.IMPORT);

        assertThat(summary.isSuccessful()).isTrue();
        assertThat(summary.getVillagesInserted()).isEqualTo(1);
        verify(villageRepository).save(argThat(v ->
                v.getMandal() != null && v.getMandal().getId().equals(mandalId) && "596707".equals(v.getCode())));
    }

    // -------------------------------------------------------------------------
    // 4. MAPAKSHI 596707 / 596708 REGRESSION
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Mapakshi 596707 and 596708: two distinct villages, same name, same mandal — both inserted")
    void testMapakshiRegression596707and596708() {
        UUID stateId    = UUID.randomUUID();
        UUID districtId = UUID.randomUUID();
        UUID mandalId   = UUID.randomUUID();

        State s = makeState(stateId, "28", "Andhra Pradesh");
        District d = makeDistrict(districtId, s, "503", "Chittoor");
        Mandal m = makeMandal(mandalId, d, "7427", "Chittoor Urban");

        String csv = "state_code,state_name,district_code,district_name,mandal_code,mandal_name,village_code,village_name\n"
                + "28,Andhra Pradesh,503,Chittoor,7427,Chittoor Urban,596707,Mapakshi\n"
                + "28,Andhra Pradesh,503,Chittoor,7427,Chittoor Urban,596708,Mapakshi\n";

        when(stateRepository.findByCodeIgnoreCase("28")).thenReturn(Optional.of(s));
        when(districtRepository.findByStateIdAndCodeIgnoreCase(stateId, "503")).thenReturn(Optional.of(d));
        when(mandalRepository.findByDistrictIdAndCodeIgnoreCase(districtId, "7427")).thenReturn(Optional.of(m));
        when(villageRepository.findByMandalIdAndCodeIgnoreCase(mandalId, "596707")).thenReturn(Optional.empty());
        when(villageRepository.findByMandalIdAndCodeIgnoreCase(mandalId, "596708")).thenReturn(Optional.empty());
        when(villageRepository.save(any(Village.class))).thenAnswer(i -> i.getArgument(0));

        ImportSummary summary = service.ingest(
                new LgdCsvGeographySource("mapakshi.csv", new StringReader(csv)), ImportMode.IMPORT);

        assertThat(summary.isSuccessful()).isTrue();
        assertThat(summary.getTotalRowsRead()).isEqualTo(2);
        assertThat(summary.getDuplicateRowsCount()).isEqualTo(0);
        assertThat(summary.getVillagesInserted()).isEqualTo(2);
        verify(villageRepository).save(argThat(v -> "596707".equals(v.getCode()) && "Mapakshi".equals(v.getName())));
        verify(villageRepository).save(argThat(v -> "596708".equals(v.getCode()) && "Mapakshi".equals(v.getName())));
    }

    @Test
    @DisplayName("Mapakshi repeat import: idempotent — 2 UNCHANGED, 0 inserted, 0 duplicates")
    void testMapakshiRepeatImportIdempotent() {
        UUID stateId    = UUID.randomUUID();
        UUID districtId = UUID.randomUUID();
        UUID mandalId   = UUID.randomUUID();
        UUID v1Id = UUID.randomUUID();
        UUID v2Id = UUID.randomUUID();

        State s = makeState(stateId, "28", "Andhra Pradesh");
        District d = makeDistrict(districtId, s, "503", "Chittoor");
        Mandal m = makeMandal(mandalId, d, "7427", "Chittoor Urban");

        Village v1 = new Village();
        v1.setId(v1Id); v1.setMandal(m); v1.setCode("596707"); v1.setName("Mapakshi"); v1.setActive(true);
        v1.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC)); v1.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        Village v2 = new Village();
        v2.setId(v2Id); v2.setMandal(m); v2.setCode("596708"); v2.setName("Mapakshi"); v2.setActive(true);
        v2.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC)); v2.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        String csv = "state_code,state_name,district_code,district_name,mandal_code,mandal_name,village_code,village_name\n"
                + "28,Andhra Pradesh,503,Chittoor,7427,Chittoor Urban,596707,Mapakshi\n"
                + "28,Andhra Pradesh,503,Chittoor,7427,Chittoor Urban,596708,Mapakshi\n";

        when(stateRepository.findByCodeIgnoreCase("28")).thenReturn(Optional.of(s));
        when(districtRepository.findByStateIdAndCodeIgnoreCase(stateId, "503")).thenReturn(Optional.of(d));
        when(mandalRepository.findByDistrictIdAndCodeIgnoreCase(districtId, "7427")).thenReturn(Optional.of(m));
        when(villageRepository.findByMandalIdAndCodeIgnoreCase(mandalId, "596707")).thenReturn(Optional.of(v1));
        when(villageRepository.findByMandalIdAndCodeIgnoreCase(mandalId, "596708")).thenReturn(Optional.of(v2));

        ImportSummary summary = service.ingest(
                new LgdCsvGeographySource("mapakshi-repeat.csv", new StringReader(csv)), ImportMode.IMPORT);

        assertThat(summary.isSuccessful()).isTrue();
        assertThat(summary.getVillagesInserted()).isEqualTo(0);
        assertThat(summary.getVillagesUnchanged()).isEqualTo(2);
        assertThat(summary.getDuplicateRowsCount()).isEqualTo(0);
        verify(villageRepository, never()).save(any());
        assertThat(v1.getId()).isEqualTo(v1Id);
        assertThat(v2.getId()).isEqualTo(v2Id);
    }

    // -------------------------------------------------------------------------
    // 5. PARTIAL-FAILURE RECOVERY
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Invalid row quarantined — remaining valid rows still ingested")
    void testPartialFailureRecovery() {
        String csv = "state_code,state_name,district_code,district_name,mandal_code,mandal_name,village_code,village_name,latitude,longitude\n"
                + "36,Telangana,501,Rangareddy,5001,Rajendranagar,50001,BadVillage,999.0,78.406\n"
                + "36,Telangana,501,Rangareddy,5001,Rajendranagar,50002,GoodVillage,17.350,78.420\n";

        when(stateRepository.findByCodeIgnoreCase("36")).thenReturn(Optional.empty());
        when(stateRepository.save(any(State.class))).thenAnswer(i -> i.getArgument(0));
        when(districtRepository.findByStateIdAndCodeIgnoreCase(any(), org.mockito.ArgumentMatchers.eq("501"))).thenReturn(Optional.empty());
        when(districtRepository.save(any(District.class))).thenAnswer(i -> i.getArgument(0));
        when(mandalRepository.findByDistrictIdAndCodeIgnoreCase(any(), org.mockito.ArgumentMatchers.eq("5001"))).thenReturn(Optional.empty());
        when(mandalRepository.save(any(Mandal.class))).thenAnswer(i -> i.getArgument(0));
        when(villageRepository.findByMandalIdAndCodeIgnoreCase(any(), org.mockito.ArgumentMatchers.eq("50002"))).thenReturn(Optional.empty());
        when(villageRepository.save(any(Village.class))).thenAnswer(i -> i.getArgument(0));

        ImportSummary summary = service.ingest(
                new LgdCsvGeographySource("partial.csv", new StringReader(csv)), ImportMode.IMPORT);

        assertThat(summary.isSuccessful()).isFalse();
        assertThat(summary.getTotalRowsRead()).isEqualTo(2);
        assertThat(summary.getValidRowsCount()).isEqualTo(1);
        assertThat(summary.getInvalidRowsCount()).isEqualTo(1);
        assertThat(summary.getVillagesInserted()).isEqualTo(1);
        assertThat(summary.getErrors()).hasSize(1);
        assertThat(summary.getErrors().get(0).field()).isEqualTo("latitude");
    }

    // -------------------------------------------------------------------------
    // 6. IDEMPOTENCY
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Full hierarchy repeat import: 0 inserts, correct UNCHANGED counts, 0 DB writes")
    void testFullHierarchyIdempotency() {
        UUID stateId    = UUID.randomUUID();
        UUID districtId = UUID.randomUUID();
        UUID mandalId   = UUID.randomUUID();
        UUID villageId  = UUID.randomUUID();

        State s = makeState(stateId, "36", "Telangana");
        District d = makeDistrict(districtId, s, "501", "Rangareddy");
        Mandal m = makeMandal(mandalId, d, "5001", "Rajendranagar");

        Village existing = new Village();
        existing.setId(villageId); existing.setMandal(m); existing.setCode("50001");
        existing.setName("Budvel"); existing.setActive(true);
        existing.setLatitude(new BigDecimal("17.318000"));
        existing.setLongitude(new BigDecimal("78.406000"));
        existing.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        existing.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        String csv = "state_code,state_name,district_code,district_name,mandal_code,mandal_name,village_code,village_name,latitude,longitude\n"
                + "36,Telangana,501,Rangareddy,5001,Rajendranagar,50001,Budvel,17.318000,78.406000\n";

        when(stateRepository.findByCodeIgnoreCase("36")).thenReturn(Optional.of(s));
        when(districtRepository.findByStateIdAndCodeIgnoreCase(stateId, "501")).thenReturn(Optional.of(d));
        when(mandalRepository.findByDistrictIdAndCodeIgnoreCase(districtId, "5001")).thenReturn(Optional.of(m));
        when(villageRepository.findByMandalIdAndCodeIgnoreCase(mandalId, "50001")).thenReturn(Optional.of(existing));

        ImportSummary summary = service.ingest(
                new LgdCsvGeographySource("repeat.csv", new StringReader(csv)), ImportMode.IMPORT);

        assertThat(summary.isSuccessful()).isTrue();
        assertThat(summary.getStatesInserted()).isEqualTo(0);
        assertThat(summary.getDistrictsInserted()).isEqualTo(0);
        assertThat(summary.getMandalsInserted()).isEqualTo(0);
        assertThat(summary.getVillagesInserted()).isEqualTo(0);
        assertThat(summary.getStatesUnchanged()).isEqualTo(1);
        assertThat(summary.getDistrictsUnchanged()).isEqualTo(1);
        assertThat(summary.getMandalsUnchanged()).isEqualTo(1);
        assertThat(summary.getVillagesUnchanged()).isEqualTo(1);
        assertThat(summary.getDuplicateRowsCount()).isEqualTo(0);
        verify(stateRepository, never()).save(any());
        verify(districtRepository, never()).save(any());
        verify(mandalRepository, never()).save(any());
        verify(villageRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // 7. SOURCE COVERAGE DOCUMENTATION
    // -------------------------------------------------------------------------

    /**
     * Documents the known partial coverage of the production_lgd_geography.csv fixture.
     *
     * Root cause of 91 mandals / 1495 villages in the database:
     * The CSV contains 36 state rows, 784 district rows, and 1495 village rows.
     * It covers all states and districts but only a subset of mandals/villages.
     * Full national coverage requires POST /api/v1/admin/geography/sync-lgd/initial
     * via LgdSyncService with valid LGD_CLIENT_ID and LGD_CLIENT_SECRET credentials.
     */
    @Test
    @DisplayName("SOURCE COVERAGE: CSV has 36 states, 784 districts, 91 mandals, 1495 villages")
    void testSourceCoverageDocumentation() throws Exception {
        java.io.InputStream is = getClass().getClassLoader()
                .getResourceAsStream("production_lgd_geography.csv");
        assertThat(is).as("production_lgd_geography.csv must exist on classpath").isNotNull();

        java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8));
        reader.readLine(); // skip header

        int totalRows = 0, stateOnly = 0, districtOnly = 0, villageRows = 0;
        java.util.Set<String> states = new java.util.HashSet<>();
        java.util.Set<String> districts = new java.util.HashSet<>();
        java.util.Set<String> mandals = new java.util.HashSet<>();
        java.util.Set<String> villages = new java.util.HashSet<>();

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isBlank()) continue;
            String[] c = line.split(",", -1);
            totalRows++;
            String sc = c.length > 0 ? c[0].trim() : "";
            String dc = c.length > 2 ? c[2].trim() : "";
            String mc = c.length > 4 ? c[4].trim() : "";
            String vc = c.length > 6 ? c[6].trim() : "";
            if (!sc.isEmpty()) states.add(sc);
            if (!dc.isEmpty()) districts.add(dc);
            if (!mc.isEmpty()) mandals.add(mc);
            if (!vc.isEmpty()) villages.add(vc);
            boolean hasDist = !dc.isEmpty(), hasMandal = !mc.isEmpty(), hasVillage = !vc.isEmpty();
            if (!hasDist && !hasMandal && !hasVillage) stateOnly++;
            else if (hasDist && !hasMandal && !hasVillage) districtOnly++;
            else if (hasVillage) villageRows++;
        }
        reader.close();

        assertThat(totalRows).as("total data rows").isEqualTo(2315);
        assertThat(states.size()).as("unique states").isEqualTo(36);
        assertThat(districts.size()).as("unique districts").isEqualTo(784);
        assertThat(mandals.size()).as("unique mandals in fixture").isEqualTo(91);
        assertThat(villages.size()).as("unique villages in fixture").isEqualTo(1495);
        assertThat(stateOnly).as("state-only rows").isEqualTo(36);
        assertThat(districtOnly).as("district-only rows").isEqualTo(784);
        assertThat(villageRows).as("village-level rows").isEqualTo(1495);
    }

    // -------------------------------------------------------------------------
    // 8. DRY-RUN MODE
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("DRY_RUN mode: simulates counts without any DB writes")
    void testDryRunNoWrites() {
        String csv = "state_code,state_name,district_code,district_name,mandal_code,mandal_name,village_code,village_name\n"
                + "28,Andhra Pradesh,503,Chittoor,7427,Chittoor Urban,596707,Mapakshi\n"
                + "28,Andhra Pradesh,503,Chittoor,7427,Chittoor Urban,596708,Mapakshi\n";

        when(stateRepository.findByCodeIgnoreCase("28")).thenReturn(Optional.empty());
        when(districtRepository.findByStateIdAndCodeIgnoreCase(any(), org.mockito.ArgumentMatchers.eq("503"))).thenReturn(Optional.empty());
        when(mandalRepository.findByDistrictIdAndCodeIgnoreCase(any(), org.mockito.ArgumentMatchers.eq("7427"))).thenReturn(Optional.empty());
        when(villageRepository.findByMandalIdAndCodeIgnoreCase(any(), org.mockito.ArgumentMatchers.eq("596707"))).thenReturn(Optional.empty());
        when(villageRepository.findByMandalIdAndCodeIgnoreCase(any(), org.mockito.ArgumentMatchers.eq("596708"))).thenReturn(Optional.empty());

        ImportSummary summary = service.ingest(
                new LgdCsvGeographySource("dry.csv", new StringReader(csv)), ImportMode.DRY_RUN);

        assertThat(summary.getMode()).isEqualTo(ImportMode.DRY_RUN);
        assertThat(summary.isSuccessful()).isTrue();
        assertThat(summary.getStatesInserted()).isEqualTo(1);
        assertThat(summary.getDistrictsInserted()).isEqualTo(1);
        assertThat(summary.getMandalsInserted()).isEqualTo(1);
        assertThat(summary.getVillagesInserted()).isEqualTo(2);
        verify(stateRepository, never()).save(any());
        verify(districtRepository, never()).save(any());
        verify(mandalRepository, never()).save(any());
        verify(villageRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

    private static State makeState(UUID id, String code, String name) {
        State s = new State();
        s.setId(id); s.setCode(code); s.setName(name); s.setActive(true);
        s.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        s.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return s;
    }

    private static District makeDistrict(UUID id, State state, String code, String name) {
        District d = new District();
        d.setId(id); d.setState(state); d.setCode(code); d.setName(name); d.setActive(true);
        d.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        d.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return d;
    }

    private static Mandal makeMandal(UUID id, District district, String code, String name) {
        Mandal m = new Mandal();
        m.setId(id); m.setDistrict(district); m.setCode(code); m.setName(name); m.setActive(true);
        m.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        m.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return m;
    }
}
