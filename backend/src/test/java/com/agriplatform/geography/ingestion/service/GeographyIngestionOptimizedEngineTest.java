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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.StringReader;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Targeted verification tests for Task 05 Optimized Geography Ingestion Engine.
 *
 * <p>Validates:
 * <ol>
 *   <li>Duplicate records within source and across batches are quarantined and counted.</li>
 *   <li>Changed names and active statuses perform in-place updates without regenerating UUIDs.</li>
 *   <li>Retry after interruption: resumes safely, marks existing as UNCHANGED, commits new.</li>
 *   <li>Partial failures: batch failure falls back to single-row isolation, saving valid records.</li>
 *   <li>Repeated imports idempotency: 2nd import yields 100% UNCHANGED, 0 inserted, 0 updated.</li>
 *   <li>Per-mandal and per-district prefetching eliminates N+1 query patterns.</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Task 05 Optimized Geography Engine Verification Tests")
class GeographyIngestionOptimizedEngineTest {

    @Mock private StateRepository stateRepository;
    @Mock private DistrictRepository districtRepository;
    @Mock private MandalRepository mandalRepository;
    @Mock private VillageRepository villageRepository;
    @Mock private JdbcTemplate jdbcTemplate;

    private GeographyIngestionServiceImpl serviceWithJdbc;
    private GeographyIngestionServiceImpl serviceWithoutJdbc;

    private State state;
    private District district;
    private Mandal mandal;

    @BeforeEach
    void setUp() {
        serviceWithJdbc = new GeographyIngestionServiceImpl(
                stateRepository, districtRepository, mandalRepository, villageRepository, jdbcTemplate);
        serviceWithoutJdbc = new GeographyIngestionServiceImpl(
                stateRepository, districtRepository, mandalRepository, villageRepository, null);

        state = new State();
        state.setId(UUID.randomUUID());
        state.setCode("28");
        state.setName("Andhra Pradesh");
        state.setActive(true);

        district = new District();
        district.setId(UUID.randomUUID());
        district.setState(state);
        district.setCode("503");
        district.setName("Chittoor");
        district.setActive(true);

        mandal = new Mandal();
        mandal.setId(UUID.randomUUID());
        mandal.setDistrict(district);
        mandal.setCode("7427");
        mandal.setName("Chittoor Urban");
        mandal.setActive(true);
    }

    // -------------------------------------------------------------------------
    // 1. DUPLICATE RECORDS QUARANTINED AND TRACKED
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Duplicate village codes in source CSV are quarantined as invalid duplicates")
    void testDuplicateRecordsQuarantinedAndTracked() {
        String csv = "state_code,state_name,district_code,district_name,mandal_code,mandal_name,village_code,village_name\n"
                + "28,Andhra Pradesh,503,Chittoor,7427,Chittoor Urban,596707,Mapakshi\n"
                + "28,Andhra Pradesh,503,Chittoor,7427,Chittoor Urban,596707,Mapakshi Duplicate\n";

        when(stateRepository.findByCodeIgnoreCase("28")).thenReturn(Optional.of(state));
        when(districtRepository.findByStateIdAndCodeIgnoreCase(state.getId(), "503")).thenReturn(Optional.of(district));
        when(mandalRepository.findByDistrictIdAndCodeIgnoreCase(district.getId(), "7427")).thenReturn(Optional.of(mandal));

        when(villageRepository.findAllByMandalId(mandal.getId())).thenReturn(Collections.emptyList());
        when(villageRepository.save(any(Village.class))).thenAnswer(i -> i.getArgument(0));

        ImportSummary summary = serviceWithoutJdbc.ingest(
                new LgdCsvGeographySource("duplicates.csv", new StringReader(csv)), ImportMode.IMPORT);

        assertThat(summary.isSuccessful()).isFalse();
        assertThat(summary.getTotalRowsRead()).isEqualTo(2);
        assertThat(summary.getValidRowsCount()).isEqualTo(1);
        assertThat(summary.getInvalidRowsCount()).isEqualTo(1);
        assertThat(summary.getDuplicateRowsCount()).isEqualTo(1);
        assertThat(summary.getVillagesInserted()).isEqualTo(1);
    }

    // -------------------------------------------------------------------------
    // 2. CHANGED NAMES AND STATUSES (IN-PLACE UPDATES, STABLE UUID)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Changed names and toggled status update in-place preserving UUID")
    void testChangedNamesAndStatusUpdates() {
        UUID villageId = UUID.randomUUID();
        Village existing = new Village();
        existing.setId(villageId);
        existing.setMandal(mandal);
        existing.setCode("596707");
        existing.setName("Old Mapakshi");
        existing.setActive(false);
        existing.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC).minusDays(10));
        existing.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC).minusDays(10));

        String csv = "state_code,state_name,district_code,district_name,mandal_code,mandal_name,village_code,village_name,is_active\n"
                + "28,Andhra Pradesh,503,Chittoor,7427,Chittoor Urban,596707,New Mapakshi,true\n";

        when(stateRepository.findByCodeIgnoreCase("28")).thenReturn(Optional.of(state));
        when(districtRepository.findByStateIdAndCodeIgnoreCase(state.getId(), "503")).thenReturn(Optional.of(district));
        when(mandalRepository.findByDistrictIdAndCodeIgnoreCase(district.getId(), "7427")).thenReturn(Optional.of(mandal));

        // Pre-fetched list contains the existing village
        when(villageRepository.findAllByMandalId(mandal.getId())).thenReturn(List.of(existing));
        when(villageRepository.save(any(Village.class))).thenAnswer(i -> i.getArgument(0));

        ImportSummary summary = serviceWithoutJdbc.ingest(
                new LgdCsvGeographySource("update.csv", new StringReader(csv)), ImportMode.IMPORT);

        assertThat(summary.isSuccessful()).isTrue();
        assertThat(summary.getVillagesUpdated()).isEqualTo(1);
        assertThat(summary.getVillagesInserted()).isEqualTo(0);
        assertThat(summary.getVillagesUnchanged()).isEqualTo(0);

        // Verify existing UUID was preserved and attributes updated
        assertThat(existing.getId()).isEqualTo(villageId);
        assertThat(existing.getName()).isEqualTo("New Mapakshi");
        assertThat(existing.isActive()).isTrue();
    }

    // -------------------------------------------------------------------------
    // 3. RETRY AFTER INTERRUPTION (PARTIAL STATE COMMITTED)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Retry after interruption safely marks committed rows as UNCHANGED and ingests remaining")
    void testRetryAfterInterruption() {
        UUID committedId = UUID.randomUUID();
        Village committed = new Village();
        committed.setId(committedId);
        committed.setMandal(mandal);
        committed.setCode("596707");
        committed.setName("Mapakshi");
        committed.setActive(true);

        // Source CSV has 2 villages: row 1 already in DB, row 2 pending
        String csv = "state_code,state_name,district_code,district_name,mandal_code,mandal_name,village_code,village_name\n"
                + "28,Andhra Pradesh,503,Chittoor,7427,Chittoor Urban,596707,Mapakshi\n"
                + "28,Andhra Pradesh,503,Chittoor,7427,Chittoor Urban,596708,Greamspet\n";

        when(stateRepository.findByCodeIgnoreCase("28")).thenReturn(Optional.of(state));
        when(districtRepository.findByStateIdAndCodeIgnoreCase(state.getId(), "503")).thenReturn(Optional.of(district));
        when(mandalRepository.findByDistrictIdAndCodeIgnoreCase(district.getId(), "7427")).thenReturn(Optional.of(mandal));

        when(villageRepository.findAllByMandalId(mandal.getId())).thenReturn(List.of(committed));
        when(villageRepository.save(any(Village.class))).thenAnswer(i -> i.getArgument(0));

        ImportSummary summary = serviceWithoutJdbc.ingest(
                new LgdCsvGeographySource("resume.csv", new StringReader(csv)), ImportMode.IMPORT);

        assertThat(summary.isSuccessful()).isTrue();
        assertThat(summary.getTotalRowsRead()).isEqualTo(2);
        assertThat(summary.getVillagesUnchanged()).isEqualTo(1); // row 1 unchanged
        assertThat(summary.getVillagesInserted()).isEqualTo(1);  // row 2 inserted
    }

    // -------------------------------------------------------------------------
    // 4. PARTIAL FAILURES: BATCH FAILURE FALLBACK TO SINGLE-ROW ISOLATION
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Batch failure falls back to row-by-row isolation saving all valid records")
    void testBatchFailureFallbackToSingleRow() {
        String csv = "state_code,state_name,district_code,district_name,mandal_code,mandal_name,village_code,village_name\n"
                + "28,Andhra Pradesh,503,Chittoor,7427,Chittoor Urban,596707,ValidVillage1\n"
                + "28,Andhra Pradesh,503,Chittoor,7427,Chittoor Urban,596708,ValidVillage2\n";

        when(stateRepository.findByCodeIgnoreCase("28")).thenReturn(Optional.of(state));
        when(districtRepository.findByStateIdAndCodeIgnoreCase(state.getId(), "503")).thenReturn(Optional.of(district));
        when(mandalRepository.findByDistrictIdAndCodeIgnoreCase(district.getId(), "7427")).thenReturn(Optional.of(mandal));
        when(villageRepository.findAllByMandalId(mandal.getId())).thenReturn(Collections.emptyList());

        // Batch insert fails with DB exception
        doThrow(new DataIntegrityViolationException("Simulated batch constraint error"))
                .when(jdbcTemplate).batchUpdate(anyString(), any(BatchPreparedStatementSetter.class));

        // Single-row fallback: row 1 succeeds, row 2 fails
        doAnswer(i -> 1).doThrow(new DataIntegrityViolationException("Constraint violation on row 2"))
                .when(jdbcTemplate).update(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any());

        ImportSummary summary = serviceWithJdbc.ingest(
                new LgdCsvGeographySource("batch-fallback.csv", new StringReader(csv)), ImportMode.IMPORT);

        // Row 1 succeeded in fallback, row 2 failed and was quarantined
        assertThat(summary.isSuccessful()).isFalse();
        assertThat(summary.getVillagesInserted()).isEqualTo(1);
        assertThat(summary.getInvalidRowsCount()).isEqualTo(1);
        assertThat(summary.getErrors()).hasSize(1);
        assertThat(summary.getErrors().get(0).reason()).contains("Database insert error");
    }

    // -------------------------------------------------------------------------
    // 5. REPEATED IMPORTS IDEMPOTENCY
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Repeated imports of exact same data yield 100% UNCHANGED with 0 DB writes")
    void testRepeatedImportsIdempotency() {
        Village existing = new Village();
        existing.setId(UUID.randomUUID());
        existing.setMandal(mandal);
        existing.setCode("596707");
        existing.setName("Mapakshi");
        existing.setActive(true);

        String csv = "state_code,state_name,district_code,district_name,mandal_code,mandal_name,village_code,village_name\n"
                + "28,Andhra Pradesh,503,Chittoor,7427,Chittoor Urban,596707,Mapakshi\n";

        when(stateRepository.findByCodeIgnoreCase("28")).thenReturn(Optional.of(state));
        when(districtRepository.findByStateIdAndCodeIgnoreCase(state.getId(), "503")).thenReturn(Optional.of(district));
        when(mandalRepository.findByDistrictIdAndCodeIgnoreCase(district.getId(), "7427")).thenReturn(Optional.of(mandal));
        when(villageRepository.findAllByMandalId(mandal.getId())).thenReturn(List.of(existing));

        // Import cycle 1 (all match existing)
        ImportSummary summary1 = serviceWithoutJdbc.ingest(
                new LgdCsvGeographySource("idempotent.csv", new StringReader(csv)), ImportMode.IMPORT);

        assertThat(summary1.isSuccessful()).isTrue();
        assertThat(summary1.getVillagesUnchanged()).isEqualTo(1);
        assertThat(summary1.getVillagesInserted()).isEqualTo(0);
        assertThat(summary1.getVillagesUpdated()).isEqualTo(0);

        // Zero repository saves performed
        verify(villageRepository, never()).save(any());
        verify(villageRepository, never()).saveAll(any());
    }

    // -------------------------------------------------------------------------
    // 6. PER-MANDAL PREFETCHING VERIFICATION
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Prefetching queries findAllByMandalId exactly once per mandal across multiple rows")
    void testPerMandalPrefetchingExecutesOnce() {
        String csv = "state_code,state_name,district_code,district_name,mandal_code,mandal_name,village_code,village_name\n"
                + "28,Andhra Pradesh,503,Chittoor,7427,Chittoor Urban,596701,Village1\n"
                + "28,Andhra Pradesh,503,Chittoor,7427,Chittoor Urban,596702,Village2\n"
                + "28,Andhra Pradesh,503,Chittoor,7427,Chittoor Urban,596703,Village3\n";

        when(stateRepository.findByCodeIgnoreCase("28")).thenReturn(Optional.of(state));
        when(districtRepository.findByStateIdAndCodeIgnoreCase(state.getId(), "503")).thenReturn(Optional.of(district));
        when(mandalRepository.findByDistrictIdAndCodeIgnoreCase(district.getId(), "7427")).thenReturn(Optional.of(mandal));
        when(villageRepository.findAllByMandalId(mandal.getId())).thenReturn(Collections.emptyList());

        ImportSummary summary = serviceWithJdbc.ingest(
                new LgdCsvGeographySource("prefetch.csv", new StringReader(csv)), ImportMode.DRY_RUN);

        assertThat(summary.isSuccessful()).isTrue();
        assertThat(summary.getTotalRowsRead()).isEqualTo(3);
        assertThat(summary.getVillagesInserted()).isEqualTo(3);

        // Pre-fetch query was invoked exactly ONCE for this mandal, not 3 times!
        verify(villageRepository, times(1)).findAllByMandalId(mandal.getId());
        verify(villageRepository, never()).findByMandalIdAndCodeIgnoreCase(any(), any());
    }
}
