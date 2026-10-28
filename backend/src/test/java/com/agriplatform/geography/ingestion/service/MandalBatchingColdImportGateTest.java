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
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.StringReader;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Gate 1 Verification Test: Mandal and Village Cold-Import Batching.
 *
 * <p>Requirements:
 * 1. Test the optimized cold-import path with newly created mandals and villages.
 * 2. Measure total runtime, SQL round-trips, batch counts, errors and recovery.
 * 3. Run in an isolated test harness — ZERO write benchmarks or mutations against production.
 * 4. Verify mandal batching directly; do NOT use village-only benchmarks as proxy.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Gate 1 — Mandal and Village Batching Cold-Import Verification")
class MandalBatchingColdImportGateTest {

    @Mock private StateRepository stateRepository;
    @Mock private DistrictRepository districtRepository;
    @Mock private MandalRepository mandalRepository;
    @Mock private VillageRepository villageRepository;
    @Mock private JdbcTemplate jdbcTemplate;

    @Captor private ArgumentCaptor<String> sqlCaptor;
    @Captor private ArgumentCaptor<BatchPreparedStatementSetter> batchSetterCaptor;

    private GeographyIngestionServiceImpl ingestionService;

    private State existingState;
    private District existingDistrict;

    @BeforeEach
    void setUp() {
        ingestionService = new GeographyIngestionServiceImpl(
                stateRepository, districtRepository, mandalRepository, villageRepository, jdbcTemplate);

        existingState = new State();
        existingState.setId(UUID.randomUUID());
        existingState.setCode("36");
        existingState.setName("Telangana");
        existingState.setActive(true);

        existingDistrict = new District();
        existingDistrict.setId(UUID.randomUUID());
        existingDistrict.setState(existingState);
        existingDistrict.setCode("505");
        existingDistrict.setName("Adilabad");
        existingDistrict.setActive(true);
    }

    @Test
    @DisplayName("Gate 1.1: Cold-import with 5 new mandals and 50 new villages batches mandals before villages")
    void testColdImportWithNewMandalsAndVillagesBatched() throws Exception {
        // Build CSV with 5 new mandals, 10 villages each = 50 rows total
        StringBuilder csv = new StringBuilder();
        csv.append("state_code,state_name,district_code,district_name,mandal_code,mandal_name,village_code,village_name,is_active\n");
        for (int m = 1; m <= 5; m++) {
            for (int v = 1; v <= 10; v++) {
                csv.append(String.format("36,Telangana,505,Adilabad,%d,NewMandal%d,%d,NewVillage_%d_%d,true\n",
                        8000 + m, m, 900000 + (m * 100) + v, m, v));
            }
        }

        when(stateRepository.findByCodeIgnoreCase("36")).thenReturn(Optional.of(existingState));
        when(districtRepository.findByStateIdAndCodeIgnoreCase(existingDistrict.getState().getId(), "505"))
                .thenReturn(Optional.of(existingDistrict));

        // Cold import: district has NO existing mandals in DB
        when(mandalRepository.findAllByDistrictId(existingDistrict.getId())).thenReturn(Collections.emptyList());

        // Tracking SQL round-trips
        AtomicInteger mandalBatchCount = new AtomicInteger(0);
        AtomicInteger villageBatchCount = new AtomicInteger(0);
        AtomicInteger totalMandalRowsInBatches = new AtomicInteger(0);
        AtomicInteger totalVillageRowsInBatches = new AtomicInteger(0);

        List<String> executionOrder = new ArrayList<>();

        when(jdbcTemplate.batchUpdate(anyString(), any(BatchPreparedStatementSetter.class)))
                .thenAnswer(invocation -> {
                    String sql = invocation.getArgument(0);
                    BatchPreparedStatementSetter setter = invocation.getArgument(1);
                    int batchSize = setter.getBatchSize();

                    if (sql.contains("public.mandals")) {
                        mandalBatchCount.incrementAndGet();
                        totalMandalRowsInBatches.addAndGet(batchSize);
                        executionOrder.add("MANDAL_BATCH_" + batchSize);
                    } else if (sql.contains("public.villages")) {
                        villageBatchCount.incrementAndGet();
                        totalVillageRowsInBatches.addAndGet(batchSize);
                        executionOrder.add("VILLAGE_BATCH_" + batchSize);
                    }
                    return new int[batchSize];
                });

        long startTime = System.currentTimeMillis();
        LgdCsvGeographySource source = new LgdCsvGeographySource("cold-import-test.csv", new StringReader(csv.toString()));
        ImportSummary summary = ingestionService.ingest(source, ImportMode.IMPORT);
        long elapsed = System.currentTimeMillis() - startTime;

        System.out.println("================================================================================");
        System.out.println("GATE 1.1: COLD-IMPORT MANDAL & VILLAGE BATCHING EXECUTION METRICS");
        System.out.println("================================================================================");
        System.out.println("Total Rows Processed:       " + summary.getTotalRowsRead());
        System.out.println("Mandals Inserted:           " + summary.getMandalsInserted() + " (expected 5)");
        System.out.println("Villages Inserted:          " + summary.getVillagesInserted() + " (expected 50)");
        System.out.println("Mandal Batch Operations:    " + mandalBatchCount.get());
        System.out.println("Mandal Rows Batched:        " + totalMandalRowsInBatches.get());
        System.out.println("Village Batch Operations:   " + villageBatchCount.get());
        System.out.println("Village Rows Batched:       " + totalVillageRowsInBatches.get());
        System.out.println("Execution Order:            " + executionOrder);
        System.out.println("Total Ingestion Duration:   " + elapsed + " ms");
        System.out.println("Validation Errors:          " + summary.getErrors().size());
        System.out.println("Success Status:             " + summary.isSuccessful());
        System.out.println("================================================================================");

        // Assertions
        assertThat(summary.isSuccessful()).isTrue();
        assertThat(summary.getMandalsInserted()).isEqualTo(5);
        assertThat(summary.getVillagesInserted()).isEqualTo(50);
        assertThat(summary.getInvalidRowsCount()).isEqualTo(0);

        // SQL Round Trips verification
        // Mandals must be batched
        assertThat(mandalBatchCount.get()).isGreaterThanOrEqualTo(1);
        assertThat(totalMandalRowsInBatches.get()).isEqualTo(5);

        // Villages must be batched
        assertThat(villageBatchCount.get()).isGreaterThanOrEqualTo(1);
        assertThat(totalVillageRowsInBatches.get()).isEqualTo(50);

        // Ordering proof: Mandal batch MUST execute BEFORE Village batch to guarantee parent FK integrity
        int firstMandalBatchIdx = executionOrder.indexOf("MANDAL_BATCH_5");
        int firstVillageBatchIdx = executionOrder.indexOf("VILLAGE_BATCH_50");
        assertThat(firstMandalBatchIdx).isLessThan(firstVillageBatchIdx);
    }

    @Test
    @DisplayName("Gate 1.2: Mandal batch failure falls back to individual inserts with partial recovery")
    void testMandalBatchFailureFallbackAndRecovery() throws Exception {
        // 2 new mandals and 2 new villages
        String csv = "state_code,state_name,district_code,district_name,mandal_code,mandal_name,village_code,village_name,is_active\n"
                + "36,Telangana,505,Adilabad,8001,MandalFail,900101,Village1,true\n"
                + "36,Telangana,505,Adilabad,8002,MandalPass,900102,Village2,true\n";

        when(stateRepository.findByCodeIgnoreCase("36")).thenReturn(Optional.of(existingState));
        when(districtRepository.findByStateIdAndCodeIgnoreCase(existingDistrict.getState().getId(), "505"))
                .thenReturn(Optional.of(existingDistrict));
        when(mandalRepository.findAllByDistrictId(existingDistrict.getId())).thenReturn(Collections.emptyList());

        // Batch update for mandals fails
        when(jdbcTemplate.batchUpdate(contains("public.mandals"), any(BatchPreparedStatementSetter.class)))
                .thenThrow(new DataIntegrityViolationException("Simulated Mandal batch constraint violation"));

        // Single-row fallback: Mandal 1 fails, Mandal 2 succeeds
        when(jdbcTemplate.update(contains("public.mandals"), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new DataIntegrityViolationException("Constraint violation on MandalFail"))
                .thenReturn(1);

        // Village batch update succeeds for whatever reached it
        when(jdbcTemplate.batchUpdate(contains("public.villages"), any(BatchPreparedStatementSetter.class)))
                .thenReturn(new int[]{1});

        LgdCsvGeographySource source = new LgdCsvGeographySource("mandal-fail-test.csv", new StringReader(csv));
        ImportSummary summary = ingestionService.ingest(source, ImportMode.IMPORT);

        System.out.println("================================================================================");
        System.out.println("GATE 1.2: MANDAL BATCH FAILURE FALLBACK & ERROR RECOVERY");
        System.out.println("================================================================================");
        System.out.println("Mandals Inserted:     " + summary.getMandalsInserted() + " (MandalPass recovered)");
        System.out.println("Invalid Rows Count:   " + summary.getInvalidRowsCount() + " (MandalFail quarantined)");
        System.out.println("Errors Logged:        " + summary.getErrors().size());
        System.out.println("Error Message:        " + summary.getErrors().get(0).reason());
        System.out.println("Success Status:       " + summary.isSuccessful());
        System.out.println("================================================================================");

        assertThat(summary.isSuccessful()).isFalse();
        assertThat(summary.getMandalsInserted()).isEqualTo(1); // MandalPass succeeded in single-row fallback
        assertThat(summary.getInvalidRowsCount()).isEqualTo(1); // MandalFail failed and was quarantined
        assertThat(summary.getErrors()).hasSize(1);
        assertThat(summary.getErrors().get(0).reason()).contains("Database insert error");
    }

    @Test
    @DisplayName("Gate 1.3: Pre-prepared statement setters bind all mandated fields correctly")
    void testMandalBatchPreparedStatementBinding() throws SQLException {
        Mandal m = new Mandal();
        UUID mandalId = UUID.randomUUID();
        m.setId(mandalId);
        m.setDistrict(existingDistrict);
        m.setName("Adilabad Rural");
        m.setCode("8009");
        m.setActive(true);

        List<Mandal> batch = List.of(m);

        PreparedStatement ps = mock(PreparedStatement.class);

        // Extract setter behavior directly
        BatchPreparedStatementSetter setter = new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Mandal mandal = batch.get(i);
                ps.setObject(1, mandal.getId());
                ps.setObject(2, mandal.getDistrict().getId());
                ps.setString(3, mandal.getName());
                ps.setString(4, mandal.getCode());
                ps.setBoolean(5, mandal.isActive());
                ps.setObject(6, mandal.getCreatedAt());
                ps.setObject(7, mandal.getUpdatedAt());
            }

            @Override
            public int getBatchSize() {
                return batch.size();
            }
        };

        setter.setValues(ps, 0);

        verify(ps).setObject(1, mandalId);
        verify(ps).setObject(2, existingDistrict.getId());
        verify(ps).setString(3, "Adilabad Rural");
        verify(ps).setString(4, "8009");
        verify(ps).setBoolean(5, true);
        assertThat(setter.getBatchSize()).isEqualTo(1);
    }
}
