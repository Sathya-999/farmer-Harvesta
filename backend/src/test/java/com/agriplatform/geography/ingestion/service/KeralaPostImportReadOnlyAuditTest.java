package com.agriplatform.geography.ingestion.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Strict Read-Only Audit for Post-Kerala Production State.
 *
 * <p>Guarantees:
 * - Read-only queries only (SELECT).
 * - Zero database mutations, zero imports.
 * - Reconciles live DB counts against Kerala_lgd.csv.
 * - Confirms other states (Telangana, AP, TN, Karnataka) remain untouched.
 */
@SpringBootTest
@ActiveProfiles("dev")
@Transactional(readOnly = true)
class KeralaPostImportReadOnlyAuditTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Read-Only Production Audit: Kerala Post-Import Counts & Pilot State Isolation")
    void auditPostKeralaProductionDatabase() throws Exception {
        System.out.println("\n========================================================================");
        System.out.println("READ-ONLY PRODUCTION AUDIT: POST-KERALA VALIDATION");
        System.out.println("========================================================================");

        // 1. Connection Identity
        String dbUser = jdbcTemplate.queryForObject("SELECT current_user", String.class);
        String dbName = jdbcTemplate.queryForObject("SELECT current_database()", String.class);
        System.out.println("Connected DB User:        " + dbUser);
        System.out.println("Connected Database:       " + dbName);

        // 2. Nationwide Counts
        Integer totalStates = jdbcTemplate.queryForObject("SELECT count(*) FROM public.states", Integer.class);
        Integer totalDistricts = jdbcTemplate.queryForObject("SELECT count(*) FROM public.districts", Integer.class);
        Integer totalMandals = jdbcTemplate.queryForObject("SELECT count(*) FROM public.mandals", Integer.class);
        Integer totalVillages = jdbcTemplate.queryForObject("SELECT count(*) FROM public.villages", Integer.class);

        System.out.println("\n--- Nationwide Totals ---");
        System.out.println("Total States:             " + totalStates + " (expected 36)");
        System.out.println("Total Districts:          " + totalDistricts + " (expected 784)");
        System.out.println("Total Mandals:            " + totalMandals + " (expected 169 = 91 legacy + 78 Kerala)");
        System.out.println("Total Villages:           " + totalVillages + " (expected 3,161 = 1,495 legacy + 1,666 Kerala)");

        assertThat(totalStates).isEqualTo(36);
        assertThat(totalDistricts).isEqualTo(784);
        assertThat(totalMandals).isEqualTo(169);
        assertThat(totalVillages).isEqualTo(3161);

        // 3. Kerala Specific Counts (State Code 32)
        Integer keralaDistricts = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM public.districts d JOIN public.states s ON d.state_id = s.id WHERE s.code = '32'", Integer.class);
        Integer keralaMandals = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM public.mandals m JOIN public.districts d ON m.district_id = d.id JOIN public.states s ON d.state_id = s.id WHERE s.code = '32'", Integer.class);
        Integer keralaVillages = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM public.villages v JOIN public.mandals m ON v.mandal_id = m.id JOIN public.districts d ON m.district_id = d.id JOIN public.states s ON d.state_id = s.id WHERE s.code = '32'", Integer.class);

        System.out.println("\n--- Kerala Entity Counts (Code 32) ---");
        System.out.println("Kerala Districts:         " + keralaDistricts + " (expected 14)");
        System.out.println("Kerala Mandals:           " + keralaMandals + " (expected 78)");
        System.out.println("Kerala Villages:          " + keralaVillages + " (expected 1,666)");

        assertThat(keralaDistricts).isEqualTo(14);
        assertThat(keralaMandals).isEqualTo(78);
        assertThat(keralaVillages).isEqualTo(1666);

        // 4. Pilot State Isolation Verification (Other states must remain untouched)
        // AP: 28 districts, 91 legacy mandals, 1495 legacy villages
        Integer apDistricts = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM public.districts d JOIN public.states s ON d.state_id = s.id WHERE s.code = '28'", Integer.class);
        Integer apMandals = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM public.mandals m JOIN public.districts d ON m.district_id = d.id JOIN public.states s ON d.state_id = s.id WHERE s.code = '28'", Integer.class);
        Integer apVillages = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM public.villages v JOIN public.mandals m ON v.mandal_id = m.id JOIN public.districts d ON m.district_id = d.id JOIN public.states s ON d.state_id = s.id WHERE s.code = '28'", Integer.class);

        // Telangana (36): 33 districts, 0 mandals, 0 villages
        Integer tgDistricts = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM public.districts d JOIN public.states s ON d.state_id = s.id WHERE s.code = '36'", Integer.class);
        Integer tgMandals = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM public.mandals m JOIN public.districts d ON m.district_id = d.id JOIN public.states s ON d.state_id = s.id WHERE s.code = '36'", Integer.class);
        Integer tgVillages = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM public.villages v JOIN public.mandals m ON v.mandal_id = m.id JOIN public.districts d ON m.district_id = d.id JOIN public.states s ON d.state_id = s.id WHERE s.code = '36'", Integer.class);

        // Tamil Nadu (33): 38 districts, 0 mandals, 0 villages
        Integer tnDistricts = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM public.districts d JOIN public.states s ON d.state_id = s.id WHERE s.code = '33'", Integer.class);
        Integer tnMandals = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM public.mandals m JOIN public.districts d ON m.district_id = d.id JOIN public.states s ON d.state_id = s.id WHERE s.code = '33'", Integer.class);
        Integer tnVillages = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM public.villages v JOIN public.mandals m ON v.mandal_id = m.id JOIN public.districts d ON m.district_id = d.id JOIN public.states s ON d.state_id = s.id WHERE s.code = '33'", Integer.class);

        // Karnataka (29): 31 districts, 0 mandals, 0 villages
        Integer kaDistricts = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM public.districts d JOIN public.states s ON d.state_id = s.id WHERE s.code = '29'", Integer.class);
        Integer kaMandals = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM public.mandals m JOIN public.districts d ON m.district_id = d.id JOIN public.states s ON d.state_id = s.id WHERE s.code = '29'", Integer.class);
        Integer kaVillages = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM public.villages v JOIN public.mandals m ON v.mandal_id = m.id JOIN public.districts d ON m.district_id = d.id JOIN public.states s ON d.state_id = s.id WHERE s.code = '29'", Integer.class);

        System.out.println("\n--- Pilot States Isolation Verification ---");
        System.out.println("Andhra Pradesh (28):      Districts=" + apDistricts + ", Mandals=" + apMandals + ", Villages=" + apVillages);
        System.out.println("Telangana (36):           Districts=" + tgDistricts + ", Mandals=" + tgMandals + ", Villages=" + tgVillages);
        System.out.println("Tamil Nadu (33):          Districts=" + tnDistricts + ", Mandals=" + tnMandals + ", Villages=" + tnVillages);
        System.out.println("Karnataka (29):           Districts=" + kaDistricts + ", Mandals=" + kaMandals + ", Villages=" + kaVillages);

        assertThat(apDistricts).isEqualTo(28);
        assertThat(apMandals).isEqualTo(82);
        assertThat(apVillages).isEqualTo(1319);

        assertThat(tgDistricts).isEqualTo(33);
        assertThat(tgMandals).isEqualTo(0);
        assertThat(tgVillages).isEqualTo(0);

        assertThat(tnDistricts).isEqualTo(38);
        assertThat(tnMandals).isEqualTo(0);
        assertThat(tnVillages).isEqualTo(0);

        assertThat(kaDistricts).isEqualTo(31);
        assertThat(kaMandals).isEqualTo(0);
        assertThat(kaVillages).isEqualTo(0);

        // 5. Data Integrity Checks
        Integer orphanMandals = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM public.mandals m WHERE m.district_id NOT IN (SELECT id FROM public.districts)", Integer.class);
        Integer orphanVillages = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM public.villages v WHERE v.mandal_id NOT IN (SELECT id FROM public.mandals)", Integer.class);
        Integer duplicateVillages = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM (SELECT mandal_id, lower(code) FROM public.villages WHERE code IS NOT NULL GROUP BY mandal_id, lower(code) HAVING count(*) > 1) t", Integer.class);
        Integer nullCodeKeralaVillages = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM public.villages v JOIN public.mandals m ON v.mandal_id = m.id JOIN public.districts d ON m.district_id = d.id JOIN public.states s ON d.state_id = s.id WHERE s.code = '32' AND (v.code IS NULL OR trim(v.code) = '')", Integer.class);

        System.out.println("\n--- Integrity Audit ---");
        System.out.println("Orphan Mandals:           " + orphanMandals + " (expected 0)");
        System.out.println("Orphan Villages:          " + orphanVillages + " (expected 0)");
        System.out.println("Duplicate Village Keys:   " + duplicateVillages + " (expected 0)");
        System.out.println("Null/Blank Kerala Codes:  " + nullCodeKeralaVillages + " (expected 0)");

        assertThat(orphanMandals).isEqualTo(0);
        assertThat(orphanVillages).isEqualTo(0);
        assertThat(duplicateVillages).isEqualTo(0);
        assertThat(nullCodeKeralaVillages).isEqualTo(0);

        // 6. Source File Cross-Reconciliation (Kerala_lgd.csv vs DB)
        Set<String> csvCodes = new HashSet<>();
        try (BufferedReader br = new BufferedReader(new FileReader("target/preflight-csvs/Kerala_lgd.csv"))) {
            String line = br.readLine(); // Header
            while ((line = br.readLine()) != null) {
                String[] cols = line.split(",", -1);
                if (cols.length >= 7 && !cols[6].isBlank()) {
                    csvCodes.add(cols[6].trim().toLowerCase());
                }
            }
        }
        System.out.println("\n--- Source CSV vs Database Cross-Reconciliation ---");
        System.out.println("Unique Village Codes in Kerala_lgd.csv: " + csvCodes.size());
        assertThat(csvCodes.size()).isEqualTo(1666);

        List<String> dbKeralaCodes = jdbcTemplate.queryForList(
                "SELECT lower(trim(v.code)) FROM public.villages v " +
                "JOIN public.mandals m ON v.mandal_id = m.id " +
                "JOIN public.districts d ON m.district_id = d.id " +
                "JOIN public.states s ON d.state_id = s.id WHERE s.code = '32'", String.class);

        Set<String> dbCodeSet = new HashSet<>(dbKeralaCodes);
        System.out.println("Unique Village Codes in DB for Kerala:  " + dbCodeSet.size());
        assertThat(dbCodeSet.size()).isEqualTo(1666);
        assertThat(dbCodeSet).containsExactlyInAnyOrderElementsOf(csvCodes);
        System.out.println("Source vs Database Match: 100% exact parity (1,666 / 1,666 village codes matched).");
        System.out.println("========================================================================\n");
    }
}
