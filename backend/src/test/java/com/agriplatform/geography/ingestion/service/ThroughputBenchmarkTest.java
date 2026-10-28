package com.agriplatform.geography.ingestion.service;

import com.agriplatform.geography.ingestion.model.ImportMode;
import com.agriplatform.geography.ingestion.model.ImportSummary;
import com.agriplatform.geography.ingestion.source.LgdCsvGeographySource;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Throughput benchmark test for the geography ingestion engine.
 *
 * <p>Runs a controlled DRY_RUN (no DB writes) against a real converted CSV file
 * to measure validated throughput including DB lookups.
 *
 * <p>Also runs a small real IMPORT against Kerala (the smallest file, ~1 666 rows)
 * to measure end-to-end rows/second against the live Supabase database.
 * Kerala is idempotent — all rows will resolve to UNCHANGED on first run after
 * initial import, or INSERT on a clean DB. Either outcome is safe.
 *
 * <p>RESTRICTION: This test does NOT insert data if Kerala is already complete.
 * The ImportSummary will show unchanged=N if Kerala is already loaded.
 * It will show inserted=N if Kerala is not yet in DB (new data).
 * In both cases, the timer measures real ingestion throughput.
 */
@SpringBootTest
@ActiveProfiles("dev")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ThroughputBenchmarkTest {

    @Autowired
    private GeographyIngestionService ingestionService;

    static final String CSV_DIR = "target/preflight-csvs";
    static final String KERALA_CSV  = CSV_DIR + "/Kerala_lgd.csv";
    static final String AP_CSV      = CSV_DIR + "/Andhra_Pradesh_lgd.csv";
    static final String TS_CSV      = CSV_DIR + "/Telangana_lgd.csv";

    // ─────────────────────────────────────────────────────────────────────────
    // TEST 1 — DRY_RUN throughput (no DB writes, measures validation + lookup)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @Disabled("Manual benchmark only — measures WAN latency against live Supabase")
    @DisplayName("DRY_RUN throughput — Kerala CSV (no DB writes)")
    void dryRunThroughput_Kerala() throws Exception {
        File f = new File(KERALA_CSV);
        assumeTrue(f.exists(), "Kerala CSV not yet generated — run conversion first");

        long start = System.currentTimeMillis();
        LgdCsvGeographySource source = new LgdCsvGeographySource("kerala-dry-run", new FileInputStream(f));
        ImportSummary summary = ingestionService.ingest(source, ImportMode.DRY_RUN);
        long elapsed = System.currentTimeMillis() - start;

        System.out.println("\n=== DRY_RUN Kerala (no DB writes) ===");
        System.out.println("File:           " + f.getName() + " (" + f.length() / 1024 + " KB)");
        System.out.println("Rows read:      " + summary.getTotalRowsRead());
        System.out.println("Valid rows:     " + summary.getValidRowsCount());
        System.out.println("Invalid rows:   " + summary.getInvalidRowsCount());
        System.out.println("Would-insert:   " + summary.getTotalInserted());
        System.out.println("Would-update:   " + summary.getTotalUpdated());
        System.out.println("Would-unchanged:" + summary.getTotalUnchanged());
        System.out.println("Elapsed ms:     " + elapsed);
        double rps = summary.getTotalRowsRead() / Math.max(elapsed / 1000.0, 0.001);
        System.out.printf("Throughput:     %.1f rows/sec (DRY_RUN with DB lookup)%n", rps);

        assertEquals(0, summary.getInvalidRowsCount(), "No invalid rows expected in Kerala CSV");
        assertTrue(summary.getTotalRowsRead() > 1600, "Expected ~1666 rows, got " + summary.getTotalRowsRead());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TEST 2 — DRY_RUN on AP CSV (largest single-state file)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(2)
    @Disabled("Manual benchmark only — measures WAN latency against live Supabase")
    @DisplayName("DRY_RUN throughput — Andhra Pradesh CSV (no DB writes)")
    void dryRunThroughput_AndhraPradesh() throws Exception {
        File f = new File(AP_CSV);
        assumeTrue(f.exists(), "AP CSV not yet generated");

        long start = System.currentTimeMillis();
        LgdCsvGeographySource source = new LgdCsvGeographySource("ap-dry-run", new FileInputStream(f));
        ImportSummary summary = ingestionService.ingest(source, ImportMode.DRY_RUN);
        long elapsed = System.currentTimeMillis() - start;

        System.out.println("\n=== DRY_RUN Andhra Pradesh (no DB writes) ===");
        System.out.println("File:           " + f.getName() + " (" + f.length() / 1024 + " KB)");
        System.out.println("Rows read:      " + summary.getTotalRowsRead());
        System.out.println("Valid rows:     " + summary.getValidRowsCount());
        System.out.println("Invalid rows:   " + summary.getInvalidRowsCount());
        System.out.println("Would-insert:   " + summary.getTotalInserted());
        System.out.println("Would-unchanged:" + summary.getTotalUnchanged());
        System.out.println("Elapsed ms:     " + elapsed);
        double rps = summary.getTotalRowsRead() / Math.max(elapsed / 1000.0, 0.001);
        System.out.printf("Throughput:     %.1f rows/sec (DRY_RUN with DB lookup)%n", rps);

        // Projection: estimate time for 80,393 rows at this throughput
        if (rps > 0) {
            long projectedSec = (long)(80393 / rps);
            System.out.printf("PROJECTED full-import time at this rate: ~%d min %d sec%n",
                projectedSec / 60, projectedSec % 60);
        }

        assertEquals(0, summary.getInvalidRowsCount(), "No invalid rows expected in AP CSV");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TEST 3 — Idempotency check: DRY_RUN on Telangana
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(3)
    @Disabled("Manual benchmark only — measures WAN latency against live Supabase")
    @DisplayName("DRY_RUN idempotency — Telangana CSV (no DB writes)")
    void dryRunIdempotency_Telangana() throws Exception {
        File f = new File(TS_CSV);
        assumeTrue(f.exists(), "Telangana CSV not yet generated");

        long start = System.currentTimeMillis();
        LgdCsvGeographySource source = new LgdCsvGeographySource("ts-dry-run", new FileInputStream(f));
        ImportSummary summary = ingestionService.ingest(source, ImportMode.DRY_RUN);
        long elapsed = System.currentTimeMillis() - start;

        System.out.println("\n=== DRY_RUN Telangana (idempotency check) ===");
        System.out.println("Rows read:    " + summary.getTotalRowsRead());
        System.out.println("Valid:        " + summary.getValidRowsCount());
        System.out.println("Invalid:      " + summary.getInvalidRowsCount());
        System.out.println("Would-insert: " + summary.getTotalInserted());
        System.out.println("Unchanged:    " + summary.getTotalUnchanged());
        System.out.println("Elapsed ms:   " + elapsed);

        assertEquals(0, summary.getInvalidRowsCount());
        // Telangana's 33 districts are already in DB → all district rows resolve unchanged
        assertEquals(33, summary.getDistrictsUnchanged(), "All 33 TS districts should resolve unchanged (already in DB)");
    }
}
