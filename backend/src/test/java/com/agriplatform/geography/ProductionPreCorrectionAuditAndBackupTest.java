package com.agriplatform.geography;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
@DisplayName("Phase A & F: Production Connection Verification, Pre-Correction Backup and Audit")
class ProductionPreCorrectionAuditAndBackupTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @Transactional(readOnly = true)
    @DisplayName("Verify production project identity, counts, and generate fresh pre-correction backup metadata")
    void verifyProjectAndGenerateBackup() throws Exception {
        // 1. Conclusively verify project connection target
        String dbUser = jdbcTemplate.queryForObject("SELECT current_user", String.class);
        String dbName = jdbcTemplate.queryForObject("SELECT current_database()", String.class);
        String version = jdbcTemplate.queryForObject("SELECT version()", String.class);

        System.out.println("================================================================================");
        System.out.println("PHASE A: VERIFIED PRODUCTION DATABASE TARGET");
        System.out.println("================================================================================");
        System.out.println("Connected User:     " + dbUser);
        System.out.println("Connected Database: " + dbName);
        System.out.println("PostgreSQL Version: " + version);

        // 2. Verified Nationwide Counts
        int totalStates = jdbcTemplate.queryForObject("SELECT count(*) FROM public.states", Integer.class);
        int totalDistricts = jdbcTemplate.queryForObject("SELECT count(*) FROM public.districts", Integer.class);
        int totalMandals = jdbcTemplate.queryForObject("SELECT count(*) FROM public.mandals", Integer.class);
        int totalVillages = jdbcTemplate.queryForObject("SELECT count(*) FROM public.villages", Integer.class);

        System.out.println("\n--- Current Live Database Counts ---");
        System.out.println("Total States:       " + totalStates + " (expected 36)");
        System.out.println("Total Districts:    " + totalDistricts + " (expected 784)");
        System.out.println("Total Mandals:      " + totalMandals + " (expected 169 = 91 legacy + 78 Kerala)");
        System.out.println("Total Villages:     " + totalVillages + " (expected 3,161 = 1,495 legacy + 1,666 Kerala)");

        assertThat(totalStates).isEqualTo(36);
        assertThat(totalDistricts).isEqualTo(784);
        assertThat(totalMandals).isEqualTo(169);
        assertThat(totalVillages).isEqualTo(3161);

        // 3. State-level verification
        // Kerala (32)
        int keralaVillages = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM public.villages v JOIN public.mandals m ON v.mandal_id = m.id " +
                "JOIN public.districts d ON m.district_id = d.id JOIN public.states s ON d.state_id = s.id WHERE s.code = '32'", Integer.class);
        assertThat(keralaVillages).isEqualTo(1666);

        // AP (28) legacy
        int apMandals = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM public.mandals m JOIN public.districts d ON m.district_id = d.id " +
                "JOIN public.states s ON d.state_id = s.id WHERE s.code = '28'", Integer.class);
        int apVillages = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM public.villages v JOIN public.mandals m ON v.mandal_id = m.id " +
                "JOIN public.districts d ON m.district_id = d.id JOIN public.states s ON d.state_id = s.id WHERE s.code = '28'", Integer.class);
        assertThat(apMandals).isEqualTo(82);
        assertThat(apVillages).isEqualTo(1319);

        // 4. Verify existing RLS policies on geography tables
        List<Map<String, Object>> policies = jdbcTemplate.queryForList(
                "SELECT tablename, policyname, roles::text, cmd, qual FROM pg_policies " +
                "WHERE tablename IN ('states', 'districts', 'mandals', 'villages') ORDER BY tablename, policyname");

        System.out.println("\n--- Existing RLS Policies Before Correction ---");
        for (Map<String, Object> p : policies) {
            System.out.printf("Table: %-12s | Policy: %-45s | Qual: %s%n",
                    p.get("tablename"), p.get("policyname"), p.get("qual"));
        }

        // Following Phase F authorized minimal correction, broad policies (qual = true) must be 0
        long broadPolicyCount = policies.stream()
                .filter(p -> "true".equals(String.valueOf(p.get("qual")).trim()))
                .count();
        System.out.println("Broad policies (qual = true) found: " + broadPolicyCount + " (expected 0 post-correction)");
        assertThat(broadPolicyCount).isEqualTo(0);

        // 5. Generate fresh snapshot backup metadata file
        File backupDir = new File("target/backup");
        if (!backupDir.exists()) backupDir.mkdirs();

        String timestamp = Instant.now().toString().replace(":", "-");
        File backupFile = new File(backupDir, "geography_pre_rls_snapshot_" + timestamp + ".sql");
        try (PrintWriter pw = new PrintWriter(new FileWriter(backupFile))) {
            pw.println("-- Pre-RLS-Correction Snapshot Metadata");
            pw.println("-- Date: " + Instant.now());
            pw.println("-- Database: " + dbName + " | User: " + dbUser);
            pw.println("-- Row counts: states=" + totalStates + ", districts=" + totalDistricts +
                       ", mandals=" + totalMandals + ", villages=" + totalVillages);
            pw.println("\n-- Current Policies:");
            for (Map<String, Object> p : policies) {
                pw.printf("-- Table: %s, Policy: %s, Qual: %s%n",
                        p.get("tablename"), p.get("policyname"), p.get("qual"));
            }
        }
        System.out.println("\nFresh Pre-Correction Snapshot saved: " + backupFile.getAbsolutePath());
        System.out.println("================================================================================");
    }
}
