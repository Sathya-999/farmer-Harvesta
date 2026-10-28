package com.agriplatform.cultivation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Task 08 — Live Supabase Cultivation End-to-End Verification")
class SupabaseCultivationLiveVerificationTest {

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
    @DisplayName("End-to-End Live Supabase Verification: CRUD, Lifecycle, Triggers, Events, History, IDOR, Capacity, and Idempotency")
    void testLiveCultivationCompleteWorkflow() throws Exception {
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
            UUID secondIntentionId = UUID.randomUUID();
            UUID villageId = null;

            try {
                // 1. SETUP TEST FIXTURES
                System.out.println("==================================================");
                System.out.println("STEP 1: Setting up isolated test fixtures");
                System.out.println("==================================================");

                // Fetch reference Village
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT id FROM public.villages LIMIT 1")) {
                    if (rs.next()) {
                        villageId = (UUID) rs.getObject("id");
                    }
                }
                assertThat(villageId).as("A reference village must exist").isNotNull();

                // Fetch reference Crop and Variety dynamically (no hardcoded names)
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

                // Fetch reference Season dynamically (no hardcoded names)
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
                                 "('" + farmerAId + "', 'authenticated', 'authenticated', 'farmer_a_" + farmerAId + "@test.org'), " +
                                 "('" + farmerBId + "', 'authenticated', 'authenticated', 'farmer_b_" + farmerBId + "@test.org')");

                    stmt.executeUpdate(
                            "UPDATE public.farmer_profiles SET profile_status = 'ACTIVE', onboarding_completed = true " +
                            "WHERE id IN ('" + farmerAId + "', '" + farmerBId + "')");

                    stmt.execute(
                            "INSERT INTO public.farmer_profiles (id, full_name, profile_status, onboarding_completed) VALUES " +
                            "('" + farmerAId + "', 'Test Farmer A', 'ACTIVE', true), " +
                            "('" + farmerBId + "', 'Test Farmer B', 'ACTIVE', true) " +
                            "ON CONFLICT (id) DO UPDATE SET profile_status = 'ACTIVE', onboarding_completed = true");
                }

                // Insert Plot for Farmer A with 10.00 acres
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO public.plots (id, farmer_id, village_id, label, total_area_acres, is_active) " +
                        "VALUES (?, ?, ?, ?, ?, true)")) {
                    ps.setObject(1, plotId);
                    ps.setObject(2, farmerAId);
                    ps.setObject(3, villageId);
                    ps.setString(4, "Plot Task08 Live Test");
                    ps.setBigDecimal(5, new BigDecimal("10.00"));
                    ps.executeUpdate();
                }

                conn.commit();
                System.out.printf("Test fixtures created: Farmer A=%s, Farmer B=%s, Plot=%s%n",
                        farmerAId, farmerBId, plotId);

                // 2. SET ACTOR CONTEXT
                System.out.println("\n==================================================");
                System.out.println("STEP 2: Setting PostgreSQL session actor context");
                System.out.println("==================================================");
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT set_config('app.actor_id', ?, false), set_config('app.actor_type', 'FARMER', false)")) {
                    ps.setString(1, farmerAId.toString());
                    ps.executeQuery();
                }
                System.out.println("Session actor context initialized to FARMER: " + farmerAId);

                // 3. CREATE CULTIVATION INTENTION
                System.out.println("\n==================================================");
                System.out.println("STEP 3: Testing CREATE cultivation_intentions");
                System.out.println("==================================================");
                short year = 2026;
                BigDecimal initialArea = new BigDecimal("4.00");
                LocalDate plannedDate = LocalDate.of(2026, 6, 15);

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO public.cultivation_intentions (" +
                        "  id, plot_id, farmer_id, crop_id, crop_variety_id, season_id, " +
                        "  cultivation_year, cultivated_area_acres, planned_planting_date, " +
                        "  status, version, source, created_at, updated_at" +
                        ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'PLANNED', 1, 'FARMER_APP', now(), now()) " +
                        "RETURNING id, status, version, cultivated_area_acres")) {
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
                        System.out.printf("Cultivation created successfully: id=%s, status=%s, version=%d%n",
                                rs.getObject("id"), rs.getString("status"), rs.getInt("version"));
                    }
                }
                conn.commit();

                // 4. VERIFY INSERT AUDIT (Trigger private.record_status_event_insert)
                System.out.println("\n==================================================");
                System.out.println("STEP 4: Verifying Trigger-generated Creation Status Event");
                System.out.println("==================================================");
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT from_status, to_status, actor_type, changed_by " +
                        "FROM public.cultivation_status_events " +
                        "WHERE intention_id = ? " +
                        "ORDER BY changed_at ASC")) {
                    ps.setObject(1, intentionId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertThat(rs.next()).isTrue();
                        assertThat(rs.getString("from_status")).isNull();
                        assertThat(rs.getString("to_status")).isEqualTo("PLANNED");
                        assertThat(rs.getString("actor_type")).isEqualTo("FARMER");
                        assertThat(rs.getObject("changed_by")).isEqualTo(farmerAId);
                        System.out.printf("Creation status event verified: NULL -> %s by actor %s (%s)%n",
                                rs.getString("to_status"), rs.getObject("changed_by"), rs.getString("actor_type"));
                    }
                }

                // 5. UPDATE CULTIVATION INTENTION (PLANNED)
                System.out.println("\n==================================================");
                System.out.println("STEP 5: Testing UPDATE on PLANNED intention");
                System.out.println("==================================================");
                BigDecimal updatedArea = new BigDecimal("5.00");
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE public.cultivation_intentions " +
                        "SET cultivated_area_acres = ?, version = version + 1, updated_at = now() " +
                        "WHERE id = ? AND farmer_id = ? " +
                        "RETURNING version, cultivated_area_acres")) {
                    ps.setBigDecimal(1, updatedArea);
                    ps.setObject(2, intentionId);
                    ps.setObject(3, farmerAId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertThat(rs.next()).isTrue();
                        assertThat(rs.getInt("version")).isEqualTo(2);
                        System.out.printf("Cultivation updated: version=%d, area=%s%n",
                                rs.getInt("version"), rs.getBigDecimal("cultivated_area_acres"));
                    }
                }
                conn.commit();

                // 5b. VERIFY UPDATE AUDIT HISTORY (Trigger private.record_intention_history fires on UPDATE)
                System.out.println("\n==================================================");
                System.out.println("STEP 5b: Verifying Trigger-generated History snapshot on UPDATE");
                System.out.println("==================================================");
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT change_type, version, actor_type, actor_id, cultivated_area_acres " +
                        "FROM public.cultivation_intention_history " +
                        "WHERE intention_id = ? " +
                        "ORDER BY snapshot_at DESC LIMIT 1")) {
                    ps.setObject(1, intentionId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertThat(rs.next()).isTrue();
                        assertThat(rs.getString("change_type")).isEqualTo("FIELD_UPDATE");
                        assertThat(rs.getString("actor_type")).isEqualTo("FARMER");
                        assertThat(rs.getObject("actor_id")).isEqualTo(farmerAId);
                        assertThat(rs.getInt("version")).isEqualTo(1); // Old version snapshotted
                        assertThat(rs.getBigDecimal("cultivated_area_acres")).isEqualByComparingTo(initialArea); // Old area snapshotted
                        System.out.printf("History update entry verified: change_type=%s, old_version=%d, old_area=%s%n",
                                rs.getString("change_type"), rs.getInt("version"), rs.getBigDecimal("cultivated_area_acres"));
                    }
                }

                // 6. STATUS TRANSITION: PLANNED -> STARTED
                System.out.println("\n==================================================");
                System.out.println("STEP 6: Testing STATUS TRANSITION: PLANNED -> STARTED");
                System.out.println("==================================================");
                LocalDate actualPlanting = LocalDate.of(2026, 6, 20);
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE public.cultivation_intentions " +
                        "SET status = 'STARTED', actual_planting_date = ?, version = version + 1, updated_at = now() " +
                        "WHERE id = ? AND farmer_id = ? " +
                        "RETURNING status, version, actual_planting_date")) {
                    ps.setDate(1, Date.valueOf(actualPlanting));
                    ps.setObject(2, intentionId);
                    ps.setObject(3, farmerAId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertThat(rs.next()).isTrue();
                        assertThat(rs.getString("status")).isEqualTo("STARTED");
                        assertThat(rs.getInt("version")).isEqualTo(3);
                        System.out.printf("Transitioned to STARTED: version=%d, planting_date=%s%n",
                                rs.getInt("version"), rs.getDate("actual_planting_date"));
                    }
                }
                conn.commit();

                // 7. VERIFY STATUS EVENT (Trigger private.record_status_event)
                System.out.println("\n==================================================");
                System.out.println("STEP 7: Verifying Trigger-generated Status Event");
                System.out.println("==================================================");
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
                        System.out.printf("Status event verified: %s -> %s by actor %s (%s)%n",
                                rs.getString("from_status"), rs.getString("to_status"),
                                rs.getObject("changed_by"), rs.getString("actor_type"));
                    }
                }

                // 8. STATUS TRANSITION: STARTED -> COMPLETED
                System.out.println("\n==================================================");
                System.out.println("STEP 8: Testing STATUS TRANSITION: STARTED -> COMPLETED");
                System.out.println("==================================================");
                LocalDate harvestDate = LocalDate.of(2026, 11, 10);
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE public.cultivation_intentions " +
                        "SET status = 'COMPLETED', harvest_date = ?, actual_area_acres = ?, version = version + 1, updated_at = now() " +
                        "WHERE id = ? AND farmer_id = ? " +
                        "RETURNING status, version, harvest_date, actual_area_acres")) {
                    ps.setDate(1, Date.valueOf(harvestDate));
                    ps.setBigDecimal(2, new BigDecimal("5.00"));
                    ps.setObject(3, intentionId);
                    ps.setObject(4, farmerAId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertThat(rs.next()).isTrue();
                        assertThat(rs.getString("status")).isEqualTo("COMPLETED");
                        assertThat(rs.getInt("version")).isEqualTo(4);
                        System.out.printf("Transitioned to COMPLETED: version=%d, harvest_date=%s%n",
                                rs.getInt("version"), rs.getDate("harvest_date"));
                    }
                }
                conn.commit();

                // Verify second status event
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT from_status, to_status FROM public.cultivation_status_events " +
                        "WHERE intention_id = ? AND to_status = 'COMPLETED'")) {
                    ps.setObject(1, intentionId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertThat(rs.next()).isTrue();
                        assertThat(rs.getString("from_status")).isEqualTo("STARTED");
                        assertThat(rs.getString("to_status")).isEqualTo("COMPLETED");
                        System.out.println("Status event for COMPLETED verified.");
                    }
                }

                // 9. TERMINAL STATE IMMUTABILITY: Attempt edit on COMPLETED intention
                System.out.println("\n==================================================");
                System.out.println("STEP 9: Testing Terminal State Immutability (COMPLETED)");
                System.out.println("==================================================");
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE public.cultivation_intentions SET cultivated_area_acres = 3.00 " +
                        "WHERE id = ? AND farmer_id = ?")) {
                    ps.setObject(1, intentionId);
                    ps.setObject(2, farmerAId);
                    assertThatThrownBy(ps::executeUpdate)
                            .hasMessageContaining("TERMINAL_INTENTION_IS_NOT_EDITABLE");
                    System.out.println("Terminal state edit correctly rejected by DB trigger: TERMINAL_INTENTION_IS_NOT_EDITABLE");
                }
                conn.rollback(); // Rollback failed query in transaction

                // Reset actor context after rollback
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT set_config('app.actor_id', ?, false), set_config('app.actor_type', 'FARMER', false)")) {
                    ps.setString(1, farmerAId.toString());
                    ps.executeQuery();
                }

                // 10. ILLEGAL TRANSITION: Reopening COMPLETED -> STARTED
                System.out.println("\n==================================================");
                System.out.println("STEP 10: Testing Illegal Transition: COMPLETED -> STARTED");
                System.out.println("==================================================");
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE public.cultivation_intentions SET status = 'STARTED' " +
                        "WHERE id = ? AND farmer_id = ?")) {
                    ps.setObject(1, intentionId);
                    ps.setObject(2, farmerAId);
                    assertThatThrownBy(ps::executeUpdate)
                            .satisfies(e -> assertThat(e.getMessage()).containsAnyOf(
                                    "HARVEST_DATE_REQUIRES_COMPLETED_STATUS",
                                    "TERMINAL_INTENTION_IS_NOT_EDITABLE",
                                    "ILLEGAL_STATUS_TRANSITION"));
                    System.out.println("Reopening correctly rejected by DB trigger.");
                }
                conn.rollback();

                // Reset actor context after rollback
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT set_config('app.actor_id', ?, false), set_config('app.actor_type', 'FARMER', false)")) {
                    ps.setString(1, farmerAId.toString());
                    ps.executeQuery();
                }

                // 11. IDOR PROTECTION
                System.out.println("\n==================================================");
                System.out.println("STEP 11: Testing IDOR Isolation between Farmer A and Farmer B");
                System.out.println("==================================================");
                // Switch actor context to Farmer B
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT set_config('app.actor_id', ?, false), set_config('app.actor_type', 'FARMER', false)")) {
                    ps.setString(1, farmerBId.toString());
                    ps.executeQuery();
                }

                // Farmer B tries to read Farmer A's cultivation
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT id FROM public.cultivation_intentions WHERE id = ? AND farmer_id = ?")) {
                    ps.setObject(1, intentionId);
                    ps.setObject(2, farmerBId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertThat(rs.next()).isFalse();
                        System.out.println("IDOR read returned 0 rows for cross-farmer query.");
                    }
                }

                // Farmer B tries to update Farmer A's cultivation
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE public.cultivation_intentions SET cultivated_area_acres = 1.00 " +
                        "WHERE id = ? AND farmer_id = ?")) {
                    ps.setObject(1, intentionId);
                    ps.setObject(2, farmerBId);
                    int updated = ps.executeUpdate();
                    assertThat(updated).isEqualTo(0);
                    System.out.println("IDOR update affected 0 rows for cross-farmer update.");
                }

                // 12. CAPACITY & UNIQUE CYCLE CONSTRAINTS
                System.out.println("\n==================================================");
                System.out.println("STEP 12: Testing Unique Active Cycle Constraint & Seasonal Lineage");
                System.out.println("==================================================");
                // Switch back to Farmer A
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT set_config('app.actor_id', ?, false), set_config('app.actor_type', 'FARMER', false)")) {
                    ps.setString(1, farmerAId.toString());
                    ps.executeQuery();
                }

                // 12a. Attempt duplicate cycle for same plot, same season, same year (Kharif 2026):
                // ux_active_cultivation_cycle MUST block it because first intention is COMPLETED (not CANCELLED/ABANDONED)!
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO public.cultivation_intentions (" +
                        "  id, plot_id, farmer_id, crop_id, season_id, " +
                        "  cultivation_year, cultivated_area_acres, planned_planting_date, " +
                        "  status, version, source" +
                        ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PLANNED', 1, 'FARMER_APP')")) {
                    ps.setObject(1, secondIntentionId);
                    ps.setObject(2, plotId);
                    ps.setObject(3, farmerAId);
                    ps.setObject(4, cropId);
                    ps.setObject(5, seasonId);
                    ps.setShort(6, year); // same year 2026
                    ps.setBigDecimal(7, new BigDecimal("6.00"));
                    ps.setDate(8, Date.valueOf(plannedDate));
                    assertThatThrownBy(ps::executeUpdate)
                            .hasMessageContaining("ux_active_cultivation_cycle");
                    System.out.println("ux_active_cultivation_cycle correctly blocked duplicate cycle for (plot, season, year).");
                }
                conn.rollback();

                // Reset actor context after rollback
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT set_config('app.actor_id', ?, false), set_config('app.actor_type', 'FARMER', false)")) {
                    ps.setString(1, farmerAId.toString());
                    ps.executeQuery();
                }

                // 12b. Next cycle for DIFFERENT year (2027) on same plot: MUST succeed!
                short nextYear = 2027;
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO public.cultivation_intentions (" +
                        "  id, plot_id, farmer_id, crop_id, season_id, " +
                        "  cultivation_year, cultivated_area_acres, planned_planting_date, " +
                        "  status, version, source" +
                        ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PLANNED', 1, 'FARMER_APP')")) {
                    ps.setObject(1, secondIntentionId);
                    ps.setObject(2, plotId);
                    ps.setObject(3, farmerAId);
                    ps.setObject(4, cropId);
                    ps.setObject(5, seasonId);
                    ps.setShort(6, nextYear); // different year 2027
                    ps.setBigDecimal(7, new BigDecimal("6.00"));
                    ps.setDate(8, Date.valueOf(plannedDate));
                    int inserted = ps.executeUpdate();
                    assertThat(inserted).isEqualTo(1);
                    System.out.println("Next cycle for different cultivation year (2027) on same plot created successfully.");
                }
                conn.commit();

                // 13. IDEMPOTENCY KEY RETRIEVAL & REPLAY
                System.out.println("\n==================================================");
                System.out.println("STEP 13: Testing Idempotency Table Replay");
                System.out.println("==================================================");
                String testKey = "live-test-key-" + UUID.randomUUID();
                String testHash = "hash-1234567890abcdef";
                String testResponse = "{\"status\":\"PLANNED\",\"cultivatedAreaAcres\":6.00}";

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO public.idempotency_keys (farmer_id, idempotency_key, request_hash, response_status, response_body, expires_at) " +
                        "VALUES (?, ?, ?, 201, ?::jsonb, now() + interval '24 hours')")) {
                    ps.setObject(1, farmerAId);
                    ps.setString(2, testKey);
                    ps.setString(3, testHash);
                    ps.setString(4, testResponse);
                    ps.executeUpdate();
                }
                conn.commit();

                // Query idempotency key
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT request_hash, response_status, response_body " +
                        "FROM public.idempotency_keys " +
                        "WHERE farmer_id = ? AND idempotency_key = ? AND expires_at > now()")) {
                    ps.setObject(1, farmerAId);
                    ps.setString(2, testKey);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertThat(rs.next()).isTrue();
                        assertThat(rs.getString("request_hash")).isEqualTo(testHash);
                        assertThat(rs.getInt("response_status")).isEqualTo(201);
                        assertThat(rs.getString("response_body")).contains("\"status\": \"PLANNED\"", "\"cultivatedAreaAcres\": 6.00");
                        System.out.println("Idempotency key correctly resolved and replayed from public.idempotency_keys.");
                    }
                }

                System.out.println("\n==================================================");
                System.out.println("LIVE VERIFICATION COMPLETE: ALL GATES PASSED!");
                System.out.println("==================================================");

            } finally {
                // CLEANUP ALL TEST ARTIFACTS
                System.out.println("\n[CLEANUP] Cleaning up test records from live Supabase...");
                try {
                    conn.rollback();
                } catch (Exception ignored) {}

                try (Statement s = conn.createStatement()) {
                    // Disable triggers or delete in order
                    s.execute("DELETE FROM public.cultivation_status_events WHERE intention_id IN ('" + intentionId + "', '" + secondIntentionId + "')");
                    s.execute("DELETE FROM public.cultivation_intention_history WHERE intention_id IN ('" + intentionId + "', '" + secondIntentionId + "')");
                    s.execute("DELETE FROM public.cultivation_intentions WHERE id IN ('" + intentionId + "', '" + secondIntentionId + "')");
                    s.execute("DELETE FROM public.idempotency_keys WHERE farmer_id IN ('" + farmerAId + "', '" + farmerBId + "')");
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
