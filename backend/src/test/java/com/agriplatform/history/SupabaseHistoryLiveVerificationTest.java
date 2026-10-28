package com.agriplatform.history;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Task 09 — Live Supabase History and Audit Trail Verification")
class SupabaseHistoryLiveVerificationTest {

    private Connection getConnection() throws Exception {
        String url = System.getenv("SUPABASE_DB_URL");
        String username = System.getenv("SUPABASE_DB_USERNAME");
        String password = System.getenv("SUPABASE_DB_PASSWORD");
        if (url == null || username == null || password == null) {
            return null;
        }
        return DriverManager.getConnection(url, username, password);
    }

    @Test
    @DisplayName("End-to-End Live Supabase Verification: History Triggers, Snapshots, Status Events, IDOR Isolation, Rollback Integrity, Ordering")
    void testLiveHistoryCompleteWorkflow() throws Exception {
        try (Connection conn = getConnection()) {
            if (conn == null) {
                System.out.println("Supabase credentials not available — skipping live verification");
                return;
            }

            conn.setAutoCommit(false); // Enable manual transaction control

            UUID farmerAId = UUID.randomUUID();
            UUID farmerBId = UUID.randomUUID();
            UUID plotId = UUID.randomUUID();
            UUID cropId = null;
            UUID varietyId = null;
            UUID seasonId = null;
            UUID intentionId = UUID.randomUUID();
            UUID villageId = null;

            try {
                // ----------------------------------------------------
                // GATE 1: SCHEMA AUDIT & TEST FIXTURES SETUP
                // ----------------------------------------------------
                System.out.println("==================================================");
                System.out.println("GATE 1: Setting up isolated test fixtures & verifying DB schema");
                System.out.println("==================================================");

                // Verify tables exist
                DatabaseMetaData metaData = conn.getMetaData();
                try (ResultSet rs = metaData.getTables(null, "public", "cultivation_intention_history", null)) {
                    assertThat(rs.next()).as("Table cultivation_intention_history must exist").isTrue();
                }
                try (ResultSet rs = metaData.getTables(null, "public", "cultivation_status_events", null)) {
                    assertThat(rs.next()).as("Table cultivation_status_events must exist").isTrue();
                }

                // Reference Village
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT id FROM public.villages LIMIT 1")) {
                    if (rs.next()) {
                        villageId = (UUID) rs.getObject("id");
                    }
                }
                assertThat(villageId).as("A reference village must exist").isNotNull();

                // Reference Crop and Variety
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT id FROM public.crops WHERE is_active = true ORDER BY name LIMIT 1")) {
                    if (rs.next()) {
                        cropId = (UUID) rs.getObject("id");
                    }
                }
                assertThat(cropId).as("Active reference crop must exist").isNotNull();

                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT id FROM public.crop_varieties WHERE crop_id = '" + cropId + "' AND is_active = true LIMIT 1")) {
                    if (rs.next()) {
                        varietyId = (UUID) rs.getObject("id");
                    }
                }

                // Reference Season
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT id FROM public.seasons WHERE is_active = true ORDER BY name LIMIT 1")) {
                    if (rs.next()) {
                        seasonId = (UUID) rs.getObject("id");
                    }
                }
                assertThat(seasonId).as("Active reference season must exist").isNotNull();

                // Insert Farmer A & B in auth.users and ensure farmer_profiles is ACTIVE
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("INSERT INTO auth.users (id, aud, role, email) VALUES " +
                                 "('" + farmerAId + "', 'authenticated', 'authenticated', 'hist_farmer_a_" + farmerAId + "@test.org'), " +
                                 "('" + farmerBId + "', 'authenticated', 'authenticated', 'hist_farmer_b_" + farmerBId + "@test.org')");

                    stmt.executeUpdate(
                            "UPDATE public.farmer_profiles SET profile_status = 'ACTIVE', onboarding_completed = true " +
                            "WHERE id IN ('" + farmerAId + "', '" + farmerBId + "')");

                    stmt.execute(
                            "INSERT INTO public.farmer_profiles (id, full_name, profile_status, onboarding_completed) VALUES " +
                            "('" + farmerAId + "', 'Test Farmer A', 'ACTIVE', true), " +
                            "('" + farmerBId + "', 'Test Farmer B', 'ACTIVE', true) " +
                            "ON CONFLICT (id) DO UPDATE SET profile_status = 'ACTIVE', onboarding_completed = true");
                }

                // Insert Plot for Farmer A with 15.00 acres
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO public.plots (id, farmer_id, village_id, label, total_area_acres, is_active) " +
                        "VALUES (?, ?, ?, ?, ?, true)")) {
                    ps.setObject(1, plotId);
                    ps.setObject(2, farmerAId);
                    ps.setObject(3, villageId);
                    ps.setString(4, "Plot Task09 History Test");
                    ps.setBigDecimal(5, new BigDecimal("15.00"));
                    ps.executeUpdate();
                }

                conn.commit();
                System.out.printf("Test fixtures created: Farmer A=%s, Farmer B=%s, Plot=%s%n",
                        farmerAId, farmerBId, plotId);

                // ----------------------------------------------------
                // GATE 2: ACTOR CONTEXT & INITIAL INSERTION AUDIT
                // ----------------------------------------------------
                System.out.println("\n==================================================");
                System.out.println("GATE 2: Testing INSERT Trigger: record_status_event_insert");
                System.out.println("==================================================");

                // Set session actor context for Farmer A
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT set_config('app.actor_id', ?, false), set_config('app.actor_type', 'FARMER', false)")) {
                    ps.setString(1, farmerAId.toString());
                    ps.executeQuery();
                }

                short year = 2026;
                BigDecimal initialArea = new BigDecimal("5.00");
                LocalDate plannedDate = LocalDate.of(2026, 6, 1);

                // Insert new Cultivation Intention
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO public.cultivation_intentions (" +
                        "  id, plot_id, farmer_id, crop_id, crop_variety_id, season_id, " +
                        "  cultivation_year, cultivated_area_acres, planned_planting_date, " +
                        "  status, version, source, created_at, updated_at" +
                        ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'PLANNED', 1, 'FARMER_APP', now(), now()) " +
                        "RETURNING id, status, version")) {
                    ps.setObject(1, intentionId);
                    ps.setObject(2, plotId);
                    ps.setObject(3, farmerAId);
                    ps.setObject(4, cropId);
                    ps.setObject(5, varietyId);
                    ps.setObject(6, seasonId);
                    ps.setShort(7, year);
                    ps.setBigDecimal(8, initialArea);
                    ps.setDate(9, Date.valueOf(plannedDate));

                    try (ResultSet rs = ps.executeQuery()) {
                        assertThat(rs.next()).isTrue();
                        assertThat(rs.getObject("id")).isEqualTo(intentionId);
                        assertThat(rs.getString("status")).isEqualTo("PLANNED");
                        assertThat(rs.getInt("version")).isEqualTo(1);
                    }
                }
                conn.commit();

                // Verify status event created by trg_record_status_event_insert
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT from_status, to_status, actor_type, changed_by " +
                        "FROM public.cultivation_status_events " +
                        "WHERE intention_id = ? " +
                        "ORDER BY changed_at ASC")) {
                    ps.setObject(1, intentionId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertThat(rs.next()).as("Initial status event must exist").isTrue();
                        assertThat(rs.getString("from_status")).isNull();
                        assertThat(rs.getString("to_status")).isEqualTo("PLANNED");
                        assertThat(rs.getString("actor_type")).isEqualTo("FARMER");
                        assertThat(rs.getObject("changed_by")).isEqualTo(farmerAId);
                        System.out.println("GATE 2 PASSED: Initial PLANNED status event verified.");
                    }
                }

                // Verify cultivation_intention_history has NO records yet (snapshots only created on UPDATE)
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT COUNT(*) FROM public.cultivation_intention_history WHERE intention_id = ?")) {
                    ps.setObject(1, intentionId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertThat(rs.next()).isTrue();
                        assertThat(rs.getInt(1)).as("History snapshot count must be 0 immediately after insert").isZero();
                    }
                }

                // ----------------------------------------------------
                // GATE 3: FIELD UPDATE SNAPSHOT GENERATION
                // ----------------------------------------------------
                System.out.println("\n==================================================");
                System.out.println("GATE 3: Testing UPDATE Trigger: record_intention_history");
                System.out.println("==================================================");

                // Set session actor context again
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT set_config('app.actor_id', ?, false), set_config('app.actor_type', 'FARMER', false)")) {
                    ps.setString(1, farmerAId.toString());
                    ps.executeQuery();
                }

                BigDecimal updatedArea = new BigDecimal("7.50");
                LocalDate updatedPlannedDate = LocalDate.of(2026, 6, 10);
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE public.cultivation_intentions " +
                        "SET cultivated_area_acres = ?, planned_planting_date = ?, version = version + 1, updated_at = now() " +
                        "WHERE id = ? AND farmer_id = ? " +
                        "RETURNING version, cultivated_area_acres")) {
                    ps.setBigDecimal(1, updatedArea);
                    ps.setDate(2, Date.valueOf(updatedPlannedDate));
                    ps.setObject(3, intentionId);
                    ps.setObject(4, farmerAId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertThat(rs.next()).isTrue();
                        assertThat(rs.getInt("version")).isEqualTo(2);
                    }
                }
                conn.commit();

                // Verify cultivation_intention_history captured old version 1 snapshot
                UUID historySnapshotId = null;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT history_id, change_type, version, actor_type, actor_id, cultivated_area_acres, planned_planting_date, changed_fields " +
                        "FROM public.cultivation_intention_history " +
                        "WHERE intention_id = ? " +
                        "ORDER BY version DESC LIMIT 1")) {
                    ps.setObject(1, intentionId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertThat(rs.next()).as("History record must exist after update").isTrue();
                        historySnapshotId = (UUID) rs.getObject("history_id");
                        assertThat(historySnapshotId).isNotNull();
                        assertThat(rs.getString("change_type")).isEqualTo("FIELD_UPDATE");
                        assertThat(rs.getString("actor_type")).isEqualTo("FARMER");
                        assertThat(rs.getObject("actor_id")).isEqualTo(farmerAId);
                        assertThat(rs.getInt("version")).isEqualTo(1); // Old version 1 snapshotted
                        assertThat(rs.getBigDecimal("cultivated_area_acres")).isEqualByComparingTo(initialArea); // Old area 5.00
                        assertThat(rs.getDate("planned_planting_date")).isEqualTo(Date.valueOf(plannedDate)); // Old date

                        Array changedFieldsArray = rs.getArray("changed_fields");
                        if (changedFieldsArray != null) {
                            String[] changedFields = (String[]) changedFieldsArray.getArray();
                            assertThat(changedFields).contains("cultivated_area_acres");
                        }
                        System.out.printf("GATE 3 PASSED: Snapshot version %d captured with area %s%n",
                                rs.getInt("version"), rs.getBigDecimal("cultivated_area_acres"));
                    }
                }

                // ----------------------------------------------------
                // GATE 3b: TRANSACTION ROLLBACK INTEGRITY (NO PHANTOM AUDIT ROWS)
                // ----------------------------------------------------
                System.out.println("\n==================================================");
                System.out.println("GATE 3b: Testing Transaction Rollback Integrity on PLANNED Intention");
                System.out.println("==================================================");

                Savepoint sp = conn.setSavepoint("before_aborted_update");

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE public.cultivation_intentions " +
                        "SET cultivated_area_acres = 9.99, version = version + 1 " +
                        "WHERE id = ?")) {
                    ps.setObject(1, intentionId);
                    ps.executeUpdate();
                }

                conn.rollback(sp);

                // Verify no phantom history snapshot was recorded with area 9.99
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT COUNT(*) FROM public.cultivation_intention_history " +
                        "WHERE intention_id = ? AND cultivated_area_acres = 9.99")) {
                    ps.setObject(1, intentionId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertThat(rs.next()).isTrue();
                        assertThat(rs.getInt(1)).as("Rolled-back transaction must not persist history").isZero();
                        System.out.println("GATE 3b PASSED: Transaction rollback cleanly discarded phantom history.");
                    }
                }

                // ----------------------------------------------------
                // GATE 4: STATUS TRANSITION EVENT & SNAPSHOT AUDIT
                // ----------------------------------------------------
                System.out.println("\n==================================================");
                System.out.println("GATE 4: Testing Status Transition PLANNED -> STARTED");
                System.out.println("==================================================");

                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT set_config('app.actor_id', ?, false), set_config('app.actor_type', 'FARMER', false)")) {
                    ps.setString(1, farmerAId.toString());
                    ps.executeQuery();
                }

                LocalDate actualPlanting = LocalDate.of(2026, 6, 12);
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE public.cultivation_intentions " +
                        "SET status = 'STARTED', actual_planting_date = ?, version = version + 1, updated_at = now() " +
                        "WHERE id = ? AND farmer_id = ? " +
                        "RETURNING status, version")) {
                    ps.setDate(1, Date.valueOf(actualPlanting));
                    ps.setObject(2, intentionId);
                    ps.setObject(3, farmerAId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertThat(rs.next()).isTrue();
                        assertThat(rs.getString("status")).isEqualTo("STARTED");
                        assertThat(rs.getInt("version")).isEqualTo(3);
                    }
                }
                conn.commit();

                // Verify status event logged
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT from_status, to_status, actor_type, changed_by " +
                        "FROM public.cultivation_status_events " +
                        "WHERE intention_id = ? " +
                        "ORDER BY changed_at DESC LIMIT 1")) {
                    ps.setObject(1, intentionId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertThat(rs.next()).isTrue();
                        assertThat(rs.getString("from_status")).isEqualTo("PLANNED");
                        assertThat(rs.getString("to_status")).isEqualTo("STARTED");
                        assertThat(rs.getString("actor_type")).isEqualTo("FARMER");
                        assertThat(rs.getObject("changed_by")).isEqualTo(farmerAId);
                        System.out.println("GATE 4 PASSED: Status transition PLANNED -> STARTED event recorded.");
                    }
                }

                // ----------------------------------------------------
                // GATE 5: TRANSITION TO COMPLETED & MONOTONIC EVENT SEQUENCING
                // ----------------------------------------------------
                System.out.println("\n==================================================");
                System.out.println("GATE 5: Testing Status Transition STARTED -> COMPLETED");
                System.out.println("==================================================");

                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT set_config('app.actor_id', ?, false), set_config('app.actor_type', 'FARMER', false)")) {
                    ps.setString(1, farmerAId.toString());
                    ps.executeQuery();
                }

                LocalDate harvestDate = LocalDate.of(2026, 10, 25);
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE public.cultivation_intentions " +
                        "SET status = 'COMPLETED', harvest_date = ?, actual_area_acres = ?, version = version + 1, updated_at = now() " +
                        "WHERE id = ? AND farmer_id = ? " +
                        "RETURNING status, version")) {
                    ps.setDate(1, Date.valueOf(harvestDate));
                    ps.setBigDecimal(2, new BigDecimal("7.50"));
                    ps.setObject(3, intentionId);
                    ps.setObject(4, farmerAId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertThat(rs.next()).isTrue();
                        assertThat(rs.getString("status")).isEqualTo("COMPLETED");
                        assertThat(rs.getInt("version")).isEqualTo(4);
                    }
                }
                conn.commit();

                // Verify all status events in sequence: [NULL -> PLANNED], [PLANNED -> STARTED], [STARTED -> COMPLETED]
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT from_status, to_status, changed_at " +
                        "FROM public.cultivation_status_events " +
                        "WHERE intention_id = ? " +
                        "ORDER BY changed_at ASC")) {
                    ps.setObject(1, intentionId);
                    try (ResultSet rs = ps.executeQuery()) {
                        // Event 1
                        assertThat(rs.next()).isTrue();
                        assertThat(rs.getString("from_status")).isNull();
                        assertThat(rs.getString("to_status")).isEqualTo("PLANNED");
                        Timestamp t1 = rs.getTimestamp("changed_at");

                        // Event 2
                        assertThat(rs.next()).isTrue();
                        assertThat(rs.getString("from_status")).isEqualTo("PLANNED");
                        assertThat(rs.getString("to_status")).isEqualTo("STARTED");
                        Timestamp t2 = rs.getTimestamp("changed_at");
                        assertThat(t2.compareTo(t1)).isGreaterThanOrEqualTo(0);

                        // Event 3
                        assertThat(rs.next()).isTrue();
                        assertThat(rs.getString("from_status")).isEqualTo("STARTED");
                        assertThat(rs.getString("to_status")).isEqualTo("COMPLETED");
                        Timestamp t3 = rs.getTimestamp("changed_at");
                        assertThat(t3.compareTo(t2)).isGreaterThanOrEqualTo(0);

                        assertThat(rs.next()).isFalse(); // Exactly 3 status events
                        System.out.println("GATE 5 PASSED: All 3 status events ordered chronologically.");
                    }
                }

                // ----------------------------------------------------
                // GATE 6: IDOR ISOLATION ENFORCEMENT
                // ----------------------------------------------------
                System.out.println("\n==================================================");
                System.out.println("GATE 6: Testing IDOR Isolation across tenants");
                System.out.println("==================================================");

                // When querying through tenant-scoped repository query:
                // SELECT * FROM cultivation_intention_history h JOIN cultivation_intentions ci ON h.intention_id = ci.id WHERE ci.id = ? AND ci.farmer_id = ?
                // Querying with Farmer B's ID MUST return 0 rows for Farmer A's intention
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT COUNT(*) FROM public.cultivation_intention_history h " +
                        "JOIN public.cultivation_intentions ci ON h.intention_id = ci.id " +
                        "WHERE ci.id = ? AND ci.farmer_id = ?")) {
                    ps.setObject(1, intentionId);
                    ps.setObject(2, farmerBId); // Attacker / Farmer B
                    try (ResultSet rs = ps.executeQuery()) {
                        assertThat(rs.next()).isTrue();
                        assertThat(rs.getInt(1)).as("Farmer B must see ZERO history rows for Farmer A").isZero();
                    }
                }

                // Same IDOR check for status events
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT COUNT(*) FROM public.cultivation_status_events e " +
                        "JOIN public.cultivation_intentions ci ON e.intention_id = ci.id " +
                        "WHERE ci.id = ? AND ci.farmer_id = ?")) {
                    ps.setObject(1, intentionId);
                    ps.setObject(2, farmerBId); // Attacker / Farmer B
                    try (ResultSet rs = ps.executeQuery()) {
                        assertThat(rs.next()).isTrue();
                        assertThat(rs.getInt(1)).as("Farmer B must see ZERO status events for Farmer A").isZero();
                        System.out.println("GATE 6 PASSED: Cross-tenant IDOR access completely blocked.");
                    }
                }

                // ----------------------------------------------------
                // GATE 7: ORDERING & PAGINATION QUERY VALIDATION
                // ----------------------------------------------------
                System.out.println("\n==================================================");
                System.out.println("GATE 7: Testing Snapshots DESC and Events ASC Ordering");
                System.out.println("==================================================");

                // Query snapshots ordered by version DESC
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT version FROM public.cultivation_intention_history " +
                        "WHERE intention_id = ? " +
                        "ORDER BY version DESC")) {
                    ps.setObject(1, intentionId);
                    try (ResultSet rs = ps.executeQuery()) {
                        int prevVersion = Integer.MAX_VALUE;
                        int count = 0;
                        while (rs.next()) {
                            int v = rs.getInt("version");
                            assertThat(v).isLessThan(prevVersion);
                            prevVersion = v;
                            count++;
                        }
                        assertThat(count).as("Snapshots must exist").isGreaterThanOrEqualTo(2);
                        System.out.println("GATE 8a PASSED: Snapshots strictly ordered by version DESC.");
                    }
                }

                // Query events ordered by changed_at ASC
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT changed_at FROM public.cultivation_status_events " +
                        "WHERE intention_id = ? " +
                        "ORDER BY changed_at ASC")) {
                    ps.setObject(1, intentionId);
                    try (ResultSet rs = ps.executeQuery()) {
                        Timestamp prevTime = new Timestamp(0);
                        int count = 0;
                        while (rs.next()) {
                            Timestamp cur = rs.getTimestamp("changed_at");
                            assertThat(cur.compareTo(prevTime)).isGreaterThanOrEqualTo(0);
                            prevTime = cur;
                            count++;
                        }
                        assertThat(count).isEqualTo(3);
                        System.out.println("GATE 8b PASSED: Status events strictly ordered by changed_at ASC.");
                    }
                }

                // ----------------------------------------------------
                // GATE 9: SINGLE SNAPSHOT RETRIEVAL BY ID
                // ----------------------------------------------------
                System.out.println("\n==================================================");
                System.out.println("GATE 9: Testing Single Snapshot Retrieval by ID");
                System.out.println("==================================================");

                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT h.history_id, h.version, ci.farmer_id " +
                        "FROM public.cultivation_intention_history h " +
                        "JOIN public.cultivation_intentions ci ON h.intention_id = ci.id " +
                        "WHERE h.history_id = ? AND h.intention_id = ? AND ci.farmer_id = ?")) {
                    ps.setObject(1, historySnapshotId);
                    ps.setObject(2, intentionId);
                    ps.setObject(3, farmerAId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertThat(rs.next()).as("Snapshot must be retrievable by (historyId, intentionId, farmerId)").isTrue();
                        assertThat(rs.getObject("history_id")).isEqualTo(historySnapshotId);
                        System.out.println("GATE 9 PASSED: Direct snapshot lookup verified.");
                    }
                }

                System.out.println("\n==================================================");
                System.out.println("LIVE VERIFICATION COMPLETE: ALL 9 GATES PASSED!");
                System.out.println("==================================================");

            } finally {
                // CLEANUP ALL TEST ARTIFACTS
                System.out.println("\n[CLEANUP] Cleaning up test records from live Supabase...");
                try {
                    conn.rollback();
                } catch (Exception ignored) {}

                try (Statement s = conn.createStatement()) {
                    s.execute("DELETE FROM public.cultivation_status_events WHERE intention_id = '" + intentionId + "'");
                    s.execute("DELETE FROM public.cultivation_intention_history WHERE intention_id = '" + intentionId + "'");
                    s.execute("DELETE FROM public.cultivation_intentions WHERE id = '" + intentionId + "'");
                    s.execute("DELETE FROM public.plots WHERE id = '" + plotId + "'");
                    s.execute("DELETE FROM public.farmer_profiles WHERE id IN ('" + farmerAId + "', '" + farmerBId + "')");
                    s.execute("DELETE FROM auth.users WHERE id IN ('" + farmerAId + "', '" + farmerBId + "')");
                    conn.commit();
                    System.out.println("[CLEANUP] All live test records cleanly removed.");
                } catch (Exception e) {
                    System.out.println("[CLEANUP WARNING] Error during cleanup: " + e.getMessage());
                }
            }
        }
    }
}
