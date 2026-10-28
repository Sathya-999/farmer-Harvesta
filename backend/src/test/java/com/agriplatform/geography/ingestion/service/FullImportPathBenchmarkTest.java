package com.agriplatform.geography.ingestion.service;

import com.agriplatform.geography.ingestion.model.ImportMode;
import com.agriplatform.geography.ingestion.model.ImportSummary;
import com.agriplatform.geography.ingestion.source.LgdCsvGeographySource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.io.StringReader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Full-path benchmark test for the optimized Geography Engine.
 *
 * <p>Measures:
 * 1. Optimized DRY_RUN throughput (pre-fetching vs legacy 446ms/row).
 * 2. Optimized full-write path (JDBC batching) inside an isolated @Transactional @Rollback test
 *    to measure real inserts/updates over the network with ZERO production data pollution.
 */
@SpringBootTest
@ActiveProfiles("dev")
class FullImportPathBenchmarkTest {

    @Autowired
    private GeographyIngestionService ingestionService;

    static final String KERALA_CSV = "target/preflight-csvs/Kerala_lgd.csv";

    @Test
    @DisplayName("Optimized DRY_RUN throughput on Kerala (1 666 rows)")
    void testOptimizedDryRunKeralaThroughput() throws Exception {
        File f = new File(KERALA_CSV);
        assumeTrue(f.exists(), "Kerala CSV missing");

        long start = System.currentTimeMillis();
        LgdCsvGeographySource source = new LgdCsvGeographySource("kerala-dry-run", new FileInputStream(f));
        ImportSummary summary = ingestionService.ingest(source, ImportMode.DRY_RUN);
        long elapsed = System.currentTimeMillis() - start;

        double rps = summary.getTotalRowsRead() / Math.max(elapsed / 1000.0, 0.001);
        System.out.println("\n=======================================================");
        System.out.println("OPTIMIZED DRY_RUN BENCHMARK (Kerala - 1,666 rows)");
        System.out.println("=======================================================");
        System.out.println("Rows read:        " + summary.getTotalRowsRead());
        System.out.println("Valid rows:       " + summary.getValidRowsCount());
        System.out.println("Invalid rows:     " + summary.getInvalidRowsCount());
        System.out.println("Would-insert:     " + summary.getTotalInserted());
        System.out.println("Would-unchanged:  " + summary.getTotalUnchanged());
        System.out.println("Elapsed time:     " + elapsed + " ms (" + (elapsed / 1000.0) + " s)");
        System.out.printf("Throughput:       %.1f rows/sec%n", rps);
        System.out.println("=======================================================\n");

        assertThat(summary.getInvalidRowsCount()).isEqualTo(0);
        assertThat(summary.getTotalRowsRead()).isGreaterThan(1600);
    }

    @Test
    @Transactional
    @Rollback(true)
    @DisplayName("Optimized FULL-PATH (real batch inserts/updates) with automatic ROLLBACK")
    void testOptimizedFullWritePathWithRollback() throws Exception {
        // Representative 500-row batch across real AP districts/mandals
        // This triggers real JDBC batchUpdate writes to the database over WAN
        // Spring automatically rolls back the transaction at test end (0 production mutation)
        StringBuilder sb = new StringBuilder();
        sb.append("state_code,state_name,district_code,district_name,mandal_code,mandal_name,village_code,village_name,is_active\n");
        for (int i = 1; i <= 500; i++) {
            sb.append(String.format("28,Andhra Pradesh,503,Chittoor,7427,Chittoor Urban,%d,BenchVillage%d,true\n", 990000 + i, i));
        }

        long start = System.currentTimeMillis();
        LgdCsvGeographySource source = new LgdCsvGeographySource("batch-benchmark", new StringReader(sb.toString()));
        ImportSummary summary = ingestionService.ingest(source, ImportMode.IMPORT);
        long elapsed = System.currentTimeMillis() - start;

        double rps = summary.getTotalRowsRead() / Math.max(elapsed / 1000.0, 0.001);
        System.out.println("\n=======================================================");
        System.out.println("OPTIMIZED FULL-PATH WRITE BENCHMARK (500 real batch writes, rolled back)");
        System.out.println("=======================================================");
        System.out.println("Rows read:        " + summary.getTotalRowsRead());
        System.out.println("Villages Inserted:" + summary.getVillagesInserted());
        System.out.println("Elapsed time:     " + elapsed + " ms (" + (elapsed / 1000.0) + " s)");
        System.out.printf("Write Throughput: %.1f rows/sec%n", rps);
        System.out.printf("Projected 80k:    ~%.1f minutes%n", (80383 / rps) / 60.0);
        System.out.println("=======================================================\n");

        assertThat(summary.getVillagesInserted()).isEqualTo(500);
    }
}
