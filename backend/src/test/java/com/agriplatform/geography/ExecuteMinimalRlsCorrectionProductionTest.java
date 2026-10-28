package com.agriplatform.geography;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase F: Controlled Production RLS Minimal Correction.
 *
 * <p>Authorized Scope:
 * Drops only the four verified redundant broad SELECT policies on geography tables:
 * - DROP POLICY "states_authenticated_select" ON public.states
 * - DROP POLICY "districts_authenticated_select" ON public.districts
 * - DROP POLICY "mandals_authenticated_select" ON public.mandals
 * - DROP POLICY "villages_authenticated_select" ON public.villages
 *
 * <p>Preserves all table grants, indexes, constraints, active policies, and data.
 */
@SpringBootTest
@ActiveProfiles("dev")
@DisplayName("Phase F: Authorized Production Minimal RLS Correction")
class ExecuteMinimalRlsCorrectionProductionTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Execute authorized minimal 4-policy RLS correction and verify post-change policies")
    void executeAuthorizedRlsCorrection() throws Exception {
        System.out.println("================================================================================");
        System.out.println("PHASE F: EXECUTING AUTHORIZED MINIMAL RLS CORRECTION");
        System.out.println("================================================================================");

        // 1. Pre-execution safety check
        String dbUser = jdbcTemplate.queryForObject("SELECT current_user", String.class);
        String dbName = jdbcTemplate.queryForObject("SELECT current_database()", String.class);
        assertThat(dbName).isEqualTo("postgres");

        // 2. Execute minimal DROP POLICY statements
        jdbcTemplate.execute("DROP POLICY IF EXISTS \"states_authenticated_select\" ON public.states");
        jdbcTemplate.execute("DROP POLICY IF EXISTS \"districts_authenticated_select\" ON public.districts");
        jdbcTemplate.execute("DROP POLICY IF EXISTS \"mandals_authenticated_select\" ON public.mandals");
        jdbcTemplate.execute("DROP POLICY IF EXISTS \"villages_authenticated_select\" ON public.villages");

        System.out.println("Executed 4 DROP POLICY statements successfully.");

        // 3. Post-execution policy inspection
        List<Map<String, Object>> remainingPolicies = jdbcTemplate.queryForList(
                "SELECT tablename, policyname, roles::text, cmd, qual FROM pg_policies " +
                "WHERE tablename IN ('states', 'districts', 'mandals', 'villages') ORDER BY tablename, policyname");

        System.out.println("\n--- Post-Correction RLS Policies in Production ---");
        for (Map<String, Object> p : remainingPolicies) {
            System.out.printf("Table: %-12s | Policy: %-45s | Qual: %s%n",
                    p.get("tablename"), p.get("policyname"), p.get("qual"));
        }

        // Must have exactly 4 policies remaining (one per table, each with is_active = true)
        assertThat(remainingPolicies).hasSize(4);

        long broadRemaining = remainingPolicies.stream()
                .filter(p -> "true".equals(String.valueOf(p.get("qual")).trim()))
                .count();
        System.out.println("\nBroad policies remaining (qual = true): " + broadRemaining + " (expected 0)");
        assertThat(broadRemaining).isEqualTo(0);

        for (Map<String, Object> p : remainingPolicies) {
            String qual = String.valueOf(p.get("qual"));
            assertThat(qual).contains("is_active = true");
        }

        // 4. Verify data integrity and counts are completely unchanged
        int totalStates = jdbcTemplate.queryForObject("SELECT count(*) FROM public.states", Integer.class);
        int totalDistricts = jdbcTemplate.queryForObject("SELECT count(*) FROM public.districts", Integer.class);
        int totalMandals = jdbcTemplate.queryForObject("SELECT count(*) FROM public.mandals", Integer.class);
        int totalVillages = jdbcTemplate.queryForObject("SELECT count(*) FROM public.villages", Integer.class);

        System.out.println("\n--- Post-Correction Integrity & Row Counts ---");
        System.out.println("States:     " + totalStates + " (expected 36)");
        System.out.println("Districts:  " + totalDistricts + " (expected 784)");
        System.out.println("Mandals:    " + totalMandals + " (expected 169)");
        System.out.println("Villages:   " + totalVillages + " (expected 3,161)");

        assertThat(totalStates).isEqualTo(36);
        assertThat(totalDistricts).isEqualTo(784);
        assertThat(totalMandals).isEqualTo(169);
        assertThat(totalVillages).isEqualTo(3161);

        // 5. Save post-correction audit record
        File backupDir = new File("target/backup");
        if (!backupDir.exists()) backupDir.mkdirs();

        File auditFile = new File(backupDir, "geography_post_rls_correction_audit.sql");
        try (PrintWriter pw = new PrintWriter(new FileWriter(auditFile))) {
            pw.println("-- Post-RLS-Correction Audit Record");
            pw.println("-- Timestamp: " + Instant.now());
            pw.println("-- Database: " + dbName + " | User: " + dbUser);
            pw.println("-- Status: All 4 redundant broad policies successfully removed.");
            pw.println("-- Remaining Active Policies:");
            for (Map<String, Object> p : remainingPolicies) {
                pw.printf("-- Table: %s | Policy: %s | Qual: %s%n",
                        p.get("tablename"), p.get("policyname"), p.get("qual"));
            }
        }
        System.out.println("\nPost-Correction Audit saved to: " + auditFile.getAbsolutePath());
        System.out.println("================================================================================");
    }
}
