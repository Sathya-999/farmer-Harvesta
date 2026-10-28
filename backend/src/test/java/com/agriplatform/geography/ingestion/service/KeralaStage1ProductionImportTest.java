package com.agriplatform.geography.ingestion.service;

import com.agriplatform.geography.ingestion.model.ImportMode;
import com.agriplatform.geography.ingestion.model.ImportSummary;
import com.agriplatform.geography.ingestion.source.LgdCsvGeographySource;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.FileInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Stage 1 — Kerala Production Geography Import Runner & Verification.
 *
 * <p>Authorized by USER on 2026-10-18:
 * "I authorize Stage 1 — Kerala only of the Task 05 production geography import."
 *
 * <p>Executes:
 * 1. Pre-import verification of DB target and baseline counts.
 * 2. Stage 1 live import of Kerala (1,666 rows) using optimized engine with JDBC batching.
 * 3. Deep post-import database verification (78 new mandals, 1,666 new villages, 0 orphans, 0 duplicates).
 * 4. Full idempotency verification (re-importing Kerala produces 0 inserts, 0 updates, 100% unchanged).
 */
@SpringBootTest
@ActiveProfiles("dev")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class KeralaStage1ProductionImportTest {

    @Autowired
    private GeographyIngestionService ingestionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    static final String KERALA_CSV = "target/preflight-csvs/Kerala_lgd.csv";

    @Test
    @Order(1)
    @DisplayName("Stage 1 — Execute Kerala Production Import & Reconcile DB")
    void executeStage1KeralaProductionImport() throws Exception {
        File f = new File(KERALA_CSV);
        assumeTrue(f.exists(), "Kerala CSV missing at " + KERALA_CSV);

        // 1. Confirm Project Identity & Baseline
        String currentDbUser = jdbcTemplate.queryForObject("SELECT current_user", String.class);
        String currentDbName = jdbcTemplate.queryForObject("SELECT current_database()", String.class);
        System.out.println("========================================================================");
        System.out.println("STAGE 1 — KERALA PRODUCTION IMPORT START");
        System.out.println("========================================================================");
        System.out.println("Connected DB User:     " + currentDbUser);
        System.out.println("Connected Database:    " + currentDbName);

        Integer preKeralaDistricts = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM public.districts d JOIN public.states s ON d.state_id = s.id WHERE s.code = '32'", Integer.class);
        Integer preKeralaMandals = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM public.mandals m JOIN public.districts d ON m.district_id = d.id JOIN public.states s ON d.state_id = s.id WHERE s.code = '32'", Integer.class);
        Integer preKeralaVillages = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM public.villages v JOIN public.mandals m ON v.mandal_id = m.id JOIN public.districts d ON m.district_id = d.id JOIN public.states s ON d.state_id = s.id WHERE s.code = '32'", Integer.class);

        System.out.println("Pre-Import Kerala Baseline in DB:");
        System.out.println("  Districts (Code 32): " + preKeralaDistricts + " (expected 14)");
        System.out.println("  Mandals   (Code 32): " + preKeralaMandals);
        System.out.println("  Villages  (Code 32): " + preKeralaVillages);

        assertThat(preKeralaDistricts).isEqualTo(14);

        if (preKeralaMandals == 0) {
            // 2. Execute Stage 1 Import (Live Database Mutation)
            long startTime = System.currentTimeMillis();
            LgdCsvGeographySource source = new LgdCsvGeographySource("Kerala_lgd.csv", new FileInputStream(f));
            ImportSummary summary = ingestionService.ingest(source, ImportMode.IMPORT);
            long elapsed = System.currentTimeMillis() - startTime;

            System.out.println("\n--- Import Execution Summary ---");
            System.out.println("Source:                " + summary.getSourceName());
            System.out.println("Mode:                  " + summary.getMode());
            System.out.println("Duration:              " + elapsed + " ms (" + (elapsed / 1000.0) + " s)");
            System.out.println("Total Rows Read:       " + summary.getTotalRowsRead());
            System.out.println("Valid Rows Count:      " + summary.getValidRowsCount());
            System.out.println("Invalid Rows Count:    " + summary.getInvalidRowsCount());
            System.out.println("States Unchanged:      " + summary.getStatesUnchanged());
            System.out.println("Districts Unchanged:   " + summary.getDistrictsUnchanged());
            System.out.println("Mandals Inserted:      " + summary.getMandalsInserted());
            System.out.println("Villages Inserted:     " + summary.getVillagesInserted());
            System.out.println("Successful:            " + summary.isSuccessful());

            assertThat(summary.isSuccessful()).isTrue();
            assertThat(summary.getInvalidRowsCount()).isEqualTo(0);
            assertThat(summary.getTotalRowsRead()).isEqualTo(1666);
            assertThat(summary.getMandalsInserted()).isEqualTo(78);
            assertThat(summary.getVillagesInserted()).isEqualTo(1666);
            assertThat(summary.getDistrictsUnchanged()).isEqualTo(14);
            assertThat(summary.getStatesUnchanged()).isEqualTo(1);
        } else {
            System.out.println("Kerala stage 1 data already imported (" + preKeralaMandals + " mandals, " + preKeralaVillages + " villages). Skipping re-insertion step in Test 1.");
            assertThat(preKeralaMandals).isEqualTo(78);
            assertThat(preKeralaVillages).isEqualTo(1666);
        }

        // 3. Post-Import Live Database Verification
        Integer postKeralaMandals = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM public.mandals m JOIN public.districts d ON m.district_id = d.id JOIN public.states s ON d.state_id = s.id WHERE s.code = '32'", Integer.class);
        Integer postKeralaVillages = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM public.villages v JOIN public.mandals m ON v.mandal_id = m.id JOIN public.districts d ON m.district_id = d.id JOIN public.states s ON d.state_id = s.id WHERE s.code = '32'", Integer.class);

        Integer totalDbMandals = jdbcTemplate.queryForObject("SELECT count(*) FROM public.mandals", Integer.class);
        Integer totalDbVillages = jdbcTemplate.queryForObject("SELECT count(*) FROM public.villages", Integer.class);

        System.out.println("\n--- Post-Import Live Database Counts ---");
        System.out.println("Kerala Mandals in DB:  " + postKeralaMandals + " (expected 78)");
        System.out.println("Kerala Villages in DB: " + postKeralaVillages + " (expected 1,666)");
        System.out.println("Total Mandals in DB:   " + totalDbMandals + " (expected 91 + 78 = 169)");
        System.out.println("Total Villages in DB:  " + totalDbVillages + " (expected 1,495 + 1,666 = 3,161)");

        assertThat(postKeralaMandals).isEqualTo(78);
        assertThat(postKeralaVillages).isEqualTo(1666);
        assertThat(totalDbMandals).isEqualTo(169);
        assertThat(totalDbVillages).isEqualTo(3161);

        // 4. Data Integrity Checks (0 orphans, 0 duplicates, all active)
        Integer orphanMandals = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM public.mandals m WHERE m.district_id NOT IN (SELECT id FROM public.districts)", Integer.class);
        Integer orphanVillages = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM public.villages v WHERE v.mandal_id NOT IN (SELECT id FROM public.mandals)", Integer.class);
        Integer duplicateVillagesInMandal = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM (SELECT mandal_id, lower(code), count(*) FROM public.villages WHERE code IS NOT NULL GROUP BY mandal_id, lower(code) HAVING count(*) > 1) t", Integer.class);
        Integer nullCodeVillagesInKerala = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM public.villages v JOIN public.mandals m ON v.mandal_id = m.id JOIN public.districts d ON m.district_id = d.id JOIN public.states s ON d.state_id = s.id WHERE s.code = '32' AND (v.code IS NULL OR trim(v.code) = '')", Integer.class);

        System.out.println("\n--- Data Integrity Verification ---");
        System.out.println("Orphan Mandals:        " + orphanMandals + " (expected 0)");
        System.out.println("Orphan Villages:       " + orphanVillages + " (expected 0)");
        System.out.println("Duplicate Village IDs: " + duplicateVillagesInMandal + " (expected 0)");
        System.out.println("Blank Code Villages:   " + nullCodeVillagesInKerala + " (expected 0)");

        assertThat(orphanMandals).isEqualTo(0);
        assertThat(orphanVillages).isEqualTo(0);
        assertThat(duplicateVillagesInMandal).isEqualTo(0);
        assertThat(nullCodeVillagesInKerala).isEqualTo(0);
    }

    @Test
    @Order(2)
    @DisplayName("Stage 1 — Verify Re-import Idempotency on Kerala (0 Inserts, 0 Updates, 100% Unchanged)")
    void verifyKeralaReimportIdempotency() throws Exception {
        File f = new File(KERALA_CSV);
        assumeTrue(f.exists(), "Kerala CSV missing at " + KERALA_CSV);

        System.out.println("\n========================================================================");
        System.out.println("STAGE 1 — KERALA RE-IMPORT IDEMPOTENCY TEST");
        System.out.println("========================================================================");

        long startTime = System.currentTimeMillis();
        LgdCsvGeographySource source = new LgdCsvGeographySource("Kerala_lgd.csv", new FileInputStream(f));
        ImportSummary summary = ingestionService.ingest(source, ImportMode.IMPORT);
        long elapsed = System.currentTimeMillis() - startTime;

        System.out.println("Re-import Duration:    " + elapsed + " ms (" + (elapsed / 1000.0) + " s)");
        System.out.println("Total Rows Read:       " + summary.getTotalRowsRead());
        System.out.println("Mandals Inserted:      " + summary.getMandalsInserted() + " (expected 0)");
        System.out.println("Mandals Updated:       " + summary.getMandalsUpdated() + " (expected 0)");
        System.out.println("Mandals Unchanged:     " + summary.getMandalsUnchanged() + " (expected 78)");
        System.out.println("Villages Inserted:     " + summary.getVillagesInserted() + " (expected 0)");
        System.out.println("Villages Updated:      " + summary.getVillagesUpdated() + " (expected 0)");
        System.out.println("Villages Unchanged:    " + summary.getVillagesUnchanged() + " (expected 1,666)");
        System.out.println("Total Inserted:        " + summary.getTotalInserted() + " (expected 0)");
        System.out.println("Total Updated:         " + summary.getTotalUpdated() + " (expected 0)");
        System.out.println("Total Unchanged:       " + summary.getTotalUnchanged() + " (expected 1,759)");
        System.out.println("Successful:            " + summary.isSuccessful());

        assertThat(summary.isSuccessful()).isTrue();
        assertThat(summary.getMandalsInserted()).isEqualTo(0);
        assertThat(summary.getMandalsUpdated()).isEqualTo(0);
        assertThat(summary.getMandalsUnchanged()).isEqualTo(78);
        assertThat(summary.getVillagesInserted()).isEqualTo(0);
        assertThat(summary.getVillagesUpdated()).isEqualTo(0);
        assertThat(summary.getVillagesUnchanged()).isEqualTo(1666);
        assertThat(summary.getTotalInserted()).isEqualTo(0);
        assertThat(summary.getTotalUpdated()).isEqualTo(0);

        // Verify total DB counts did not change
        Integer totalDbMandals = jdbcTemplate.queryForObject("SELECT count(*) FROM public.mandals", Integer.class);
        Integer totalDbVillages = jdbcTemplate.queryForObject("SELECT count(*) FROM public.villages", Integer.class);
        assertThat(totalDbMandals).isEqualTo(169);
        assertThat(totalDbVillages).isEqualTo(3161);
        System.out.println("========================================================================\n");
    }
}
