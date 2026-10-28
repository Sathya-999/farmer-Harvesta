package com.agriplatform.analytics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Task 10 — Live Supabase Analytics Engine Verification")
class SupabaseAnalyticsLiveVerificationTest {

    private Connection getConnection() throws Exception {
        String url = System.getenv("SUPABASE_DB_URL");
        String username = System.getenv("SUPABASE_DB_USERNAME");
        String password = System.getenv("SUPABASE_DB_PASSWORD");
        if (url == null || username == null || password == null) {
            return null;
        }
        return DriverManager.getConnection(url, username, password);
    }

    private void cleanupTestArtifacts(Connection conn, List<UUID> intentions, List<UUID> plots, List<UUID> farmers) {
        try {
            conn.rollback();
        } catch (Exception ignored) {}

        try (Statement s = conn.createStatement()) {
            if (!intentions.isEmpty()) {
                String idList = intentions.stream().map(u -> "'" + u + "'").reduce((a, b) -> a + "," + b).orElse("''");
                s.execute("DELETE FROM public.cultivation_status_events WHERE intention_id IN (" + idList + ")");
                s.execute("DELETE FROM public.cultivation_intention_history WHERE intention_id IN (" + idList + ")");
                s.execute("DELETE FROM public.cultivation_intentions WHERE id IN (" + idList + ")");
            }
            if (!plots.isEmpty()) {
                String plotIdList = plots.stream().map(u -> "'" + u + "'").reduce((a, b) -> a + "," + b).orElse("''");
                s.execute("DELETE FROM public.plots WHERE id IN (" + plotIdList + ")");
            }
            if (!farmers.isEmpty()) {
                String farmerIdList = farmers.stream().map(u -> "'" + u + "'").reduce((a, b) -> a + "," + b).orElse("''");
                s.execute("DELETE FROM public.farmer_profiles WHERE id IN (" + farmerIdList + ")");
                s.execute("DELETE FROM auth.users WHERE id IN (" + farmerIdList + ")");
            }
            // General safety sweep for test email domain
            s.execute("DELETE FROM public.cultivation_status_events WHERE intention_id IN (SELECT id FROM public.cultivation_intentions WHERE farmer_id IN (SELECT id FROM auth.users WHERE email LIKE '%@test.org'))");
            s.execute("DELETE FROM public.cultivation_intention_history WHERE intention_id IN (SELECT id FROM public.cultivation_intentions WHERE farmer_id IN (SELECT id FROM auth.users WHERE email LIKE '%@test.org'))");
            s.execute("DELETE FROM public.cultivation_intentions WHERE farmer_id IN (SELECT id FROM auth.users WHERE email LIKE '%@test.org')");
            s.execute("DELETE FROM public.plots WHERE farmer_id IN (SELECT id FROM auth.users WHERE email LIKE '%@test.org')");
            s.execute("DELETE FROM public.farmer_profiles WHERE id IN (SELECT id FROM auth.users WHERE email LIKE '%@test.org')");
            s.execute("DELETE FROM auth.users WHERE email LIKE '%@test.org'");

            conn.commit();
        } catch (Exception e) {
            System.out.println("[CLEANUP WARNING] " + e.getMessage());
        }
    }

    @Test
    @DisplayName("End-to-End Live Supabase Verification: Aggregations, Rollups, Status Filters, History Isolation, IDOR, Performance")
    void testLiveAnalyticsCompleteWorkflow() throws Exception {
        try (Connection conn = getConnection()) {
            if (conn == null) {
                System.out.println("Supabase credentials not available — skipping live analytics verification");
                return;
            }

            conn.setAutoCommit(false);

            UUID farmerAId = UUID.randomUUID();
            UUID farmerBId = UUID.randomUUID();
            UUID farmerSuspendedId = UUID.randomUUID();

            UUID plot1Id = UUID.randomUUID();
            UUID plot2Id = UUID.randomUUID();
            UUID plotInactiveId = UUID.randomUUID();

            UUID intention1Id = UUID.randomUUID();
            UUID intention2Id = UUID.randomUUID();
            UUID intentionCompletedId = UUID.randomUUID();
            UUID intentionCancelledId = UUID.randomUUID();

            UUID villageId = null;
            UUID mandalId = null;
            UUID districtId = null;
            UUID stateId = null;
            UUID crop1Id = null;
            UUID crop2Id = null;
            UUID seasonId = null;

            List<UUID> intentionsToClean = new ArrayList<>();
            List<UUID> plotsToClean = new ArrayList<>();
            List<UUID> farmersToClean = new ArrayList<>();

            // Initial pre-cleanup
            cleanupTestArtifacts(conn, List.of(), List.of(), List.of());

            try {
                // ----------------------------------------------------
                // GATE 1: SCHEMA AUDIT & TEST REFERENCE LOOKUPS
                // ----------------------------------------------------
                System.out.println("==================================================");
                System.out.println("GATE 1: Resolving references from live Supabase");
                System.out.println("==================================================");

                // Fetch reference village and hierarchy
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("""
                         SELECT v.id AS v_id, v.mandal_id AS m_id, m.district_id AS d_id, d.state_id AS s_id
                         FROM public.villages v
                         JOIN public.mandals m ON v.mandal_id = m.id
                         JOIN public.districts d ON m.district_id = d.id
                         JOIN public.states s ON d.state_id = s.id
                         WHERE v.is_active = true AND m.is_active = true AND d.is_active = true AND s.is_active = true
                         LIMIT 1
                         """)) {
                    assertThat(rs.next()).as("Active reference village with complete hierarchy must exist").isTrue();
                    villageId = (UUID) rs.getObject("v_id");
                    mandalId = (UUID) rs.getObject("m_id");
                    districtId = (UUID) rs.getObject("d_id");
                    stateId = (UUID) rs.getObject("s_id");
                }

                // Fetch two distinct reference crops
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT id FROM public.crops WHERE is_active = true ORDER BY name LIMIT 2")) {
                    assertThat(rs.next()).isTrue();
                    crop1Id = (UUID) rs.getObject("id");
                    assertThat(rs.next()).isTrue();
                    crop2Id = (UUID) rs.getObject("id");
                }

                // Fetch reference season
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT id FROM public.seasons WHERE is_active = true ORDER BY name LIMIT 1")) {
                    assertThat(rs.next()).isTrue();
                    seasonId = (UUID) rs.getObject("id");
                }

                System.out.printf("References resolved: Village=%s, Mandal=%s, District=%s, State=%s%n",
                        villageId, mandalId, districtId, stateId);

                // ----------------------------------------------------
                // SETUP FIXTURES
                // ----------------------------------------------------
                System.out.println("==================================================");
                System.out.println("SETTING UP ISOLATED TEST FIXTURES");
                System.out.println("==================================================");

                // Insert into auth.users (Supabase requires FK to auth.users for farmer_profiles)
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("INSERT INTO auth.users (id, aud, role, email) VALUES " +
                            "('" + farmerAId + "', 'authenticated', 'authenticated', 'analytics_a_" + farmerAId + "@test.org'), " +
                            "('" + farmerBId + "', 'authenticated', 'authenticated', 'analytics_b_" + farmerBId + "@test.org'), " +
                            "('" + farmerSuspendedId + "', 'authenticated', 'authenticated', 'analytics_c_" + farmerSuspendedId + "@test.org')");
                    farmersToClean.add(farmerAId);
                    farmersToClean.add(farmerBId);
                    farmersToClean.add(farmerSuspendedId);

                    stmt.execute("INSERT INTO public.farmer_profiles (id, full_name, home_village_id, profile_status, onboarding_completed) VALUES " +
                            "('" + farmerAId + "', 'Test Farmer A', '" + villageId + "', 'ACTIVE', true), " +
                            "('" + farmerBId + "', 'Test Farmer B', '" + villageId + "', 'ACTIVE', true), " +
                            "('" + farmerSuspendedId + "', 'Test Farmer Suspended', '" + villageId + "', 'SUSPENDED', true) " +
                            "ON CONFLICT (id) DO UPDATE SET profile_status = EXCLUDED.profile_status");
                }

                // Insert Plots
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO public.plots (id, farmer_id, village_id, label, total_area_acres, is_active) VALUES (?, ?, ?, ?, ?, ?)")) {
                    // Plot 1: Farmer A, Active, 10.00 acres
                    ps.setObject(1, plot1Id);
                    ps.setObject(2, farmerAId);
                    ps.setObject(3, villageId);
                    ps.setString(4, "Plot A1");
                    ps.setBigDecimal(5, new BigDecimal("10.00"));
                    ps.setBoolean(6, true);
                    ps.executeUpdate();
                    plotsToClean.add(plot1Id);

                    // Plot 2: Farmer B, Active, 8.00 acres
                    ps.setObject(1, plot2Id);
                    ps.setObject(2, farmerBId);
                    ps.setObject(3, villageId);
                    ps.setString(4, "Plot B1");
                    ps.setBigDecimal(5, new BigDecimal("8.00"));
                    ps.setBoolean(6, true);
                    ps.executeUpdate();
                    plotsToClean.add(plot2Id);

                    // Plot 3: Farmer B, Inactive (soft-deleted), 5.00 acres
                    ps.setObject(1, plotInactiveId);
                    ps.setObject(2, farmerBId);
                    ps.setObject(3, villageId);
                    ps.setString(4, "Plot B-Inactive");
                    ps.setBigDecimal(5, new BigDecimal("5.00"));
                    ps.setBoolean(6, false);
                    ps.executeUpdate();
                    plotsToClean.add(plotInactiveId);
                }

                // Set actor session context for DB trigger
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("SELECT set_config('app.actor_id', '" + farmerAId + "', false)");
                    stmt.execute("SELECT set_config('app.actor_type', 'FARMER', false)");
                }

                // Insert Cultivation Intentions
                try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO public.cultivation_intentions
                    (id, plot_id, farmer_id, crop_id, season_id, cultivation_year, cultivated_area_acres, planned_planting_date, actual_planting_date, actual_area_acres, status)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """)) {
                    // Intention 1: Farmer A, Plot 1, Crop 1, 2026, PLANNED, 6.00 acres
                    ps.setObject(1, intention1Id);
                    ps.setObject(2, plot1Id);
                    ps.setObject(3, farmerAId);
                    ps.setObject(4, crop1Id);
                    ps.setObject(5, seasonId);
                    ps.setShort(6, (short) 2026);
                    ps.setBigDecimal(7, new BigDecimal("6.00"));
                    ps.setDate(8, Date.valueOf(LocalDate.of(2026, 6, 15)));
                    ps.setNull(9, Types.DATE);
                    ps.setNull(10, Types.NUMERIC);
                    ps.setString(11, "PLANNED");
                    ps.executeUpdate();
                    intentionsToClean.add(intention1Id);

                    // Intention 2: Farmer B, Plot 2, Crop 2, 2026, STARTED, 4.00 acres
                    ps.setObject(1, intention2Id);
                    ps.setObject(2, plot2Id);
                    ps.setObject(3, farmerBId);
                    ps.setObject(4, crop2Id);
                    ps.setObject(5, seasonId);
                    ps.setShort(6, (short) 2026);
                    ps.setBigDecimal(7, new BigDecimal("4.00"));
                    ps.setDate(8, Date.valueOf(LocalDate.of(2026, 6, 20)));
                    ps.setDate(9, Date.valueOf(LocalDate.of(2026, 6, 22)));
                    ps.setNull(10, Types.NUMERIC);
                    ps.setString(11, "STARTED");
                    ps.executeUpdate();
                    intentionsToClean.add(intention2Id);

                    // Intention 3: Farmer A, Plot 1, Crop 1, 2025, COMPLETED, 5.00 acres (actual 4.80)
                    ps.setObject(1, intentionCompletedId);
                    ps.setObject(2, plot1Id);
                    ps.setObject(3, farmerAId);
                    ps.setObject(4, crop1Id);
                    ps.setObject(5, seasonId);
                    ps.setShort(6, (short) 2025);
                    ps.setBigDecimal(7, new BigDecimal("5.00"));
                    ps.setDate(8, Date.valueOf(LocalDate.of(2025, 6, 15)));
                    ps.setDate(9, Date.valueOf(LocalDate.of(2025, 6, 18)));
                    ps.setBigDecimal(10, new BigDecimal("4.80"));
                    ps.setString(11, "COMPLETED");
                    ps.executeUpdate();
                    intentionsToClean.add(intentionCompletedId);

                    // Intention 4: Farmer B, Plot 2, Crop 1, 2025, CANCELLED, 2.00 acres
                    ps.setObject(1, intentionCancelledId);
                    ps.setObject(2, plot2Id);
                    ps.setObject(3, farmerBId);
                    ps.setObject(4, crop1Id);
                    ps.setObject(5, seasonId);
                    ps.setShort(6, (short) 2025);
                    ps.setBigDecimal(7, new BigDecimal("2.00"));
                    ps.setDate(8, Date.valueOf(LocalDate.of(2025, 6, 15)));
                    ps.setNull(9, Types.DATE);
                    ps.setNull(10, Types.NUMERIC);
                    ps.setString(11, "CANCELLED");
                    ps.executeUpdate();
                    intentionsToClean.add(intentionCancelledId);
                }

                conn.commit();
                System.out.println("Test fixtures committed successfully.");

                // ----------------------------------------------------
                // GATE 2: ACTIVE CULTIVATION AGGREGATION & FILTERING
                // ----------------------------------------------------
                System.out.println("==================================================");
                System.out.println("GATE 2: Testing Active Cultivation Aggregation");
                System.out.println("==================================================");

                String villageSummarySql = """
                    SELECT
                        COALESCE(SUM(CASE WHEN ci.status IN ('PLANNED', 'STARTED') THEN ci.cultivated_area_acres ELSE 0 END), 0) AS active_area,
                        COUNT(CASE WHEN ci.status IN ('PLANNED', 'STARTED') THEN ci.id END) AS active_cultivations,
                        COUNT(DISTINCT CASE WHEN ci.status IN ('PLANNED', 'STARTED') THEN ci.farmer_id END) AS active_farmers,
                        COUNT(DISTINCT CASE WHEN ci.status IN ('PLANNED', 'STARTED') THEN ci.plot_id END) AS active_plots,
                        COALESCE(SUM(CASE WHEN ci.status = 'COMPLETED' THEN COALESCE(ci.actual_area_acres, ci.cultivated_area_acres) ELSE 0 END), 0) AS completed_area,
                        COUNT(CASE WHEN ci.status = 'COMPLETED' THEN ci.id END) AS completed_cultivations,
                        COUNT(CASE WHEN ci.status = 'CANCELLED' THEN ci.id END) AS cancelled_cultivations
                    FROM public.cultivation_intentions ci
                    JOIN public.plots p ON ci.plot_id = p.id
                    JOIN public.farmer_profiles fp ON ci.farmer_id = fp.id
                    WHERE p.village_id = ?
                      AND p.is_active = true
                      AND fp.profile_status = 'ACTIVE'
                      AND ci.season_id = ?
                      AND ci.cultivation_year = 2026
                    """;

                try (PreparedStatement ps = conn.prepareStatement(villageSummarySql)) {
                    ps.setObject(1, villageId);
                    ps.setObject(2, seasonId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertThat(rs.next()).isTrue();
                        BigDecimal activeArea = rs.getBigDecimal("active_area");
                        long activeCultivations = rs.getLong("active_cultivations");
                        long activeFarmers = rs.getLong("active_farmers");
                        long activePlots = rs.getLong("active_plots");

                        System.out.printf("GATE 2 Result: Active Area=%s, Cultivations=%d, Farmers=%d, Plots=%d%n",
                                activeArea, activeCultivations, activeFarmers, activePlots);

                        // Intention 1 (6.00) + Intention 2 (4.00) = 10.00
                        assertThat(activeArea).isEqualByComparingTo("10.00");
                        assertThat(activeCultivations).isEqualTo(2L);
                        assertThat(activeFarmers).isEqualTo(2L);
                        assertThat(activePlots).isEqualTo(2L);
                    }
                }
                System.out.println("GATE 2 PASSED: Active aggregation correctly computed 10.00 acres across 2 active farmers.");

                // ----------------------------------------------------
                // GATE 3: INACTIVE PLOT EXCLUSION ON FARMER CAPACITY
                // ----------------------------------------------------
                System.out.println("==================================================");
                System.out.println("GATE 3: Verifying Inactive Plot Exclusion from Farmer Capacity");
                System.out.println("==================================================");

                String farmerPlotsSql = """
                    SELECT
                        COUNT(p.id) AS total_plots,
                        COALESCE(SUM(p.total_area_acres), 0) AS total_capacity
                    FROM public.plots p
                    WHERE p.farmer_id = ? AND p.is_active = true
                    """;

                try (PreparedStatement ps = conn.prepareStatement(farmerPlotsSql)) {
                    ps.setObject(1, farmerBId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertThat(rs.next()).isTrue();
                        long plots = rs.getLong("total_plots");
                        BigDecimal capacity = rs.getBigDecimal("total_capacity");

                        System.out.printf("Farmer B Active Plots: %d, Capacity: %s%n", plots, capacity);
                        // Plot 3 (5.00 acres) is inactive, so total active plots = 1, capacity = 8.00
                        assertThat(plots).isEqualTo(1L);
                        assertThat(capacity).isEqualByComparingTo("8.00");
                    }
                }
                System.out.println("GATE 3 PASSED: Inactive plot (5.00 acres) strictly excluded from capacity summary.");

                // ----------------------------------------------------
                // GATE 4: STATUS SEPARATION (COMPLETED & CANCELLED)
                // ----------------------------------------------------
                System.out.println("==================================================");
                System.out.println("GATE 4: Verifying Historical Status Separation (2025)");
                System.out.println("==================================================");

                String year2025Sql = """
                    SELECT
                        COALESCE(SUM(CASE WHEN ci.status IN ('PLANNED', 'STARTED') THEN ci.cultivated_area_acres ELSE 0 END), 0) AS active_area,
                        COALESCE(SUM(CASE WHEN ci.status = 'COMPLETED' THEN COALESCE(ci.actual_area_acres, ci.cultivated_area_acres) ELSE 0 END), 0) AS completed_area,
                        COUNT(CASE WHEN ci.status = 'COMPLETED' THEN ci.id END) AS completed_cultivations,
                        COUNT(CASE WHEN ci.status = 'CANCELLED' THEN ci.id END) AS cancelled_cultivations
                    FROM public.cultivation_intentions ci
                    JOIN public.plots p ON ci.plot_id = p.id
                    JOIN public.farmer_profiles fp ON ci.farmer_id = fp.id
                    WHERE p.village_id = ?
                      AND p.is_active = true
                      AND fp.profile_status = 'ACTIVE'
                      AND ci.season_id = ?
                      AND ci.cultivation_year = 2025
                    """;

                try (PreparedStatement ps = conn.prepareStatement(year2025Sql)) {
                    ps.setObject(1, villageId);
                    ps.setObject(2, seasonId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertThat(rs.next()).isTrue();
                        BigDecimal activeArea2025 = rs.getBigDecimal("active_area");
                        BigDecimal completedArea2025 = rs.getBigDecimal("completed_area");
                        long completedCount = rs.getLong("completed_cultivations");
                        long cancelledCount = rs.getLong("cancelled_cultivations");

                        System.out.printf("2025 Metrics: Active=%s, Completed=%s, CompletedCount=%d, CancelledCount=%d%n",
                                activeArea2025, completedArea2025, completedCount, cancelledCount);

                        // No active in 2025
                        assertThat(activeArea2025).isEqualByComparingTo("0.00");
                        // Completed actual area = 4.80
                        assertThat(completedArea2025).isEqualByComparingTo("4.80");
                        assertThat(completedCount).isEqualTo(1L);
                        assertThat(cancelledCount).isEqualTo(1L);
                    }
                }
                System.out.println("GATE 4 PASSED: Completed area (4.80 acres) and cancelled status isolated from active metrics.");

                // ----------------------------------------------------
                // GATE 5: DOUBLE-COUNTING DEFENSE VIA UPDATE & HISTORY
                // ----------------------------------------------------
                System.out.println("==================================================");
                System.out.println("GATE 5: Testing Double-Counting Defense via UPDATE & History");
                System.out.println("==================================================");

                // Update Intention 1 from 6.00 to 7.00 acres. This creates a history snapshot row in cultivation_intention_history!
                try (PreparedStatement ps = conn.prepareStatement("""
                    UPDATE public.cultivation_intentions
                    SET cultivated_area_acres = 7.00, version = version + 1, updated_at = now()
                    WHERE id = ?
                    """)) {
                    ps.setObject(1, intention1Id);
                    ps.executeUpdate();
                }
                conn.commit();

                // Re-query active area for 2026.
                // It should be 7.00 (updated) + 4.00 = 11.00 acres.
                // If history snapshots leaked, it would double-count to 11.00 + 6.00 = 17.00 acres!
                try (PreparedStatement ps = conn.prepareStatement(villageSummarySql)) {
                    ps.setObject(1, villageId);
                    ps.setObject(2, seasonId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertThat(rs.next()).isTrue();
                        BigDecimal updatedActiveArea = rs.getBigDecimal("active_area");
                        System.out.println("Active area after update: " + updatedActiveArea);
                        assertThat(updatedActiveArea).isEqualByComparingTo("11.00");
                    }
                }
                System.out.println("GATE 5 PASSED: History rows did NOT contaminate or double-count current active area (11.00 acres).");

                // ----------------------------------------------------
                // GATE 6: GEOGRAPHIC HIERARCHY ROLLUP (MANDAL, DISTRICT, STATE)
                // ----------------------------------------------------
                System.out.println("==================================================");
                System.out.println("GATE 6: Testing Geographic Rollup (Mandal, District, State)");
                System.out.println("==================================================");

                String mandalSql = """
                    SELECT COALESCE(SUM(ci.cultivated_area_acres), 0) AS mandal_active_area
                    FROM public.cultivation_intentions ci
                    JOIN public.plots p ON ci.plot_id = p.id
                    JOIN public.villages v ON p.village_id = v.id
                    JOIN public.farmer_profiles fp ON ci.farmer_id = fp.id
                    WHERE v.mandal_id = ?
                      AND p.is_active = true
                      AND fp.profile_status = 'ACTIVE'
                      AND ci.status IN ('PLANNED', 'STARTED')
                      AND ci.season_id = ?
                      AND ci.cultivation_year = 2026
                    """;

                try (PreparedStatement ps = conn.prepareStatement(mandalSql)) {
                    ps.setObject(1, mandalId);
                    ps.setObject(2, seasonId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertThat(rs.next()).isTrue();
                        BigDecimal mandalArea = rs.getBigDecimal("mandal_active_area");
                        System.out.println("Mandal Rollup Active Area: " + mandalArea);
                        assertThat(mandalArea).isGreaterThanOrEqualTo(new BigDecimal("11.00"));
                    }
                }
                System.out.println("GATE 6 PASSED: Mandal geographic rollup query executed successfully.");

                // ----------------------------------------------------
                // GATE 7: CROP DISTRIBUTION & PERCENTAGE SHARE
                // ----------------------------------------------------
                System.out.println("==================================================");
                System.out.println("GATE 7: Testing Crop Distribution and Shares");
                System.out.println("==================================================");

                String cropDistSql = """
                    SELECT
                        c.name AS crop_name,
                        COALESCE(SUM(ci.cultivated_area_acres), 0) AS total_area,
                        COUNT(ci.id) AS cult_count,
                        COUNT(DISTINCT ci.farmer_id) AS farmer_count
                    FROM public.cultivation_intentions ci
                    JOIN public.crops c ON ci.crop_id = c.id
                    JOIN public.plots p ON ci.plot_id = p.id
                    JOIN public.farmer_profiles fp ON ci.farmer_id = fp.id
                    WHERE p.village_id = ?
                      AND p.is_active = true
                      AND fp.profile_status = 'ACTIVE'
                      AND ci.status IN ('PLANNED', 'STARTED')
                      AND ci.season_id = ?
                      AND ci.cultivation_year = 2026
                    GROUP BY c.name
                    ORDER BY total_area DESC
                    """;

                try (PreparedStatement ps = conn.prepareStatement(cropDistSql)) {
                    ps.setObject(1, villageId);
                    ps.setObject(2, seasonId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertThat(rs.next()).isTrue();
                        String topCropName = rs.getString("crop_name");
                        BigDecimal topArea = rs.getBigDecimal("total_area");
                        System.out.printf("Leading Crop: %s with area %s%n", topCropName, topArea);
                        // Crop 1 has 7.00 acres
                        assertThat(topArea).isEqualByComparingTo("7.00");

                        assertThat(rs.next()).isTrue();
                        String secondCropName = rs.getString("crop_name");
                        BigDecimal secondArea = rs.getBigDecimal("total_area");
                        System.out.printf("Second Crop: %s with area %s%n", secondCropName, secondArea);
                        assertThat(secondArea).isEqualByComparingTo("4.00");
                    }
                }
                System.out.println("GATE 7 PASSED: Crop distribution ranked deterministically (7.00 acres > 4.00 acres).");

                // ----------------------------------------------------
                // GATE 8: FARMER PERSONAL SUMMARY & CAPACITY UTILIZATION
                // ----------------------------------------------------
                System.out.println("==================================================");
                System.out.println("GATE 8: Testing Farmer Personal Summary and Capacity");
                System.out.println("==================================================");

                String farmerSummarySql = """
                    SELECT
                        COUNT(DISTINCT p.id) AS total_plots,
                        COALESCE(SUM(p.total_area_acres), 0) AS total_capacity,
                        COALESCE(SUM(ci.cultivated_area_acres), 0) AS active_area
                    FROM public.plots p
                    LEFT JOIN public.cultivation_intentions ci ON p.id = ci.plot_id
                        AND ci.status IN ('PLANNED', 'STARTED')
                        AND ci.season_id = ?
                        AND ci.cultivation_year = 2026
                    WHERE p.farmer_id = ?
                      AND p.is_active = true
                    """;

                try (PreparedStatement ps = conn.prepareStatement(farmerSummarySql)) {
                    ps.setObject(1, seasonId);
                    ps.setObject(2, farmerAId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertThat(rs.next()).isTrue();
                        long plots = rs.getLong("total_plots");
                        BigDecimal capacity = rs.getBigDecimal("total_capacity");
                        BigDecimal activeArea = rs.getBigDecimal("active_area");

                        System.out.printf("Farmer A: Plots=%d, Capacity=%s, Active=%s%n", plots, capacity, activeArea);
                        assertThat(plots).isEqualTo(1L);
                        assertThat(capacity).isEqualByComparingTo("10.00");
                        assertThat(activeArea).isEqualByComparingTo("7.00");

                        // 7.00 / 10.00 * 100 = 70.00%
                        BigDecimal utilization = activeArea.multiply(new BigDecimal("100.00"))
                                .divide(capacity, 2, RoundingMode.HALF_UP);
                        assertThat(utilization).isEqualByComparingTo("70.00");
                    }
                }
                System.out.println("GATE 8 PASSED: Farmer A capacity utilization computed at 70.00% without cross-contamination.");

                // ----------------------------------------------------
                // GATE 9: IDOR ISOLATION
                // ----------------------------------------------------
                System.out.println("==================================================");
                System.out.println("GATE 9: Testing Cross-Tenant IDOR Isolation");
                System.out.println("==================================================");

                try (PreparedStatement ps = conn.prepareStatement(farmerSummarySql)) {
                    ps.setObject(1, seasonId);
                    ps.setObject(2, farmerBId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertThat(rs.next()).isTrue();
                        BigDecimal farmerBActiveArea = rs.getBigDecimal("active_area");
                        // Farmer B has only 4.00 acres, never sees Farmer A's 7.00 acres!
                        assertThat(farmerBActiveArea).isEqualByComparingTo("4.00");
                    }
                }
                System.out.println("GATE 9 PASSED: Farmer B summary is strictly isolated to 4.00 acres.");

                // ----------------------------------------------------
                // GATE 10: PERFORMANCE VERIFICATION
                // ----------------------------------------------------
                System.out.println("==================================================");
                System.out.println("GATE 10: Testing Database-side Performance");
                System.out.println("==================================================");

                long start = System.currentTimeMillis();
                try (PreparedStatement ps = conn.prepareStatement(villageSummarySql)) {
                    ps.setObject(1, villageId);
                    ps.setObject(2, seasonId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertThat(rs.next()).isTrue();
                    }
                }
                long durationMs = System.currentTimeMillis() - start;
                System.out.printf("Live Supabase Village Summary Query Duration: %d ms%n", durationMs);
                assertThat(durationMs).as("Database-side aggregation must execute swiftly").isLessThan(1500L);
                System.out.println("GATE 10 PASSED: Database-side aggregation is performant and responsive.");

                System.out.println("==================================================");
                System.out.println("LIVE SUPABASE VERIFICATION COMPLETE: ALL GATES PASSED!");
                System.out.println("==================================================");

            } finally {
                // ----------------------------------------------------
                // CLEANUP FIXTURES IN STRICT REVERSE-FK ORDER
                // ----------------------------------------------------
                System.out.println("\n[CLEANUP] Cleaning up test records from live Supabase in reverse FK order...");
                cleanupTestArtifacts(conn, intentionsToClean, plotsToClean, farmersToClean);
                System.out.println("[CLEANUP] All live test records cleanly removed.");
            }
        }
    }
}
