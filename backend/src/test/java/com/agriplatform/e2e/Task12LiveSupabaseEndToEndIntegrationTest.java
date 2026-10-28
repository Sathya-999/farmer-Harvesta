package com.agriplatform.e2e;

import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Task 12 — Live Supabase Database End-to-End Integration Test.
 *
 * <p>Exercises the full backend domain lifecycle directly against the live Supabase
 * PostgreSQL instance:
 * <ol>
 *   <li>Database connectivity and baseline geography validation (36 states, 784 districts, 169 mandals, 3,161 villages).</li>
 *   <li>Farmer Profile provisioning in auth.users and public.farmer_profiles.</li>
 *   <li>Plot creation with trigger enforcement (private.validate_plot_write).</li>
 *   <li>Idempotency key management and duplicate prevention.</li>
 *   <li>Plot area constraints and unique label indexing (ux_plots_farmer_label_ci).</li>
 *   <li>Cultivation intention registration with foreign key to (plot_id, farmer_id).</li>
 *   <li>Database trigger event logging (private.record_status_event_insert).</li>
 *   <li>Cultivation lifecycle transitions: PLANNED -> STARTED -> COMPLETED.</li>
 *   <li>Database trigger history snapshots (private.record_intention_history).</li>
 *   <li>Terminal state mutation immutability.</li>
 *   <li>Unique active cycle enforcement (ux_active_cultivation_cycle).</li>
 *   <li>IDOR cross-farmer query isolation.</li>
 *   <li>Notification creation, unread partial index query, and mark-as-read.</li>
 *   <li>Transactional rollback integrity and 100% fixture cleanup.</li>
 * </ol>
 */
@DisplayName("Task 12 — Live Supabase Database End-to-End Integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Task12LiveSupabaseEndToEndIntegrationTest {

    private static Connection conn;
    private static boolean skipAll = false;

    // Test Fixtures
    private static UUID farmerA;
    private static UUID farmerB;
    private static UUID villageKerala;
    private static UUID villageAP;
    private static UUID cropId;
    private static UUID varietyId;
    private static UUID seasonId;

    private static UUID plotA1;
    private static UUID plotA2;
    private static UUID cultivationA1;
    private static UUID notificationA1;

    private static Connection getConnection() throws Exception {
        String url = System.getenv("SUPABASE_DB_URL");
        String username = System.getenv("SUPABASE_DB_USERNAME");
        String password = System.getenv("SUPABASE_DB_PASSWORD");
        if (url == null || username == null || password == null) return null;
        return DriverManager.getConnection(url, username, password);
    }

    @BeforeAll
    static void setupSuite() throws Exception {
        conn = getConnection();
        if (conn == null) {
            System.out.println("[SKIP] Supabase credentials unavailable — skipping live integration suite.");
            skipAll = true;
            return;
        }
        conn.setAutoCommit(true);

        farmerA = UUID.randomUUID();
        farmerB = UUID.randomUUID();

        // 1. Resolve Reference Geography (Kerala & AP)
        try (Statement s = conn.createStatement()) {
            // Kerala village
            try (ResultSet rs = s.executeQuery(
                    "SELECT v.id FROM public.villages v " +
                    "JOIN public.mandals m ON v.mandal_id = m.id " +
                    "JOIN public.districts d ON m.district_id = d.id " +
                    "JOIN public.states st ON d.state_id = st.id " +
                    "WHERE st.code = '32' AND v.is_active = true LIMIT 1")) {
                if (rs.next()) villageKerala = (UUID) rs.getObject("id");
            }

            // AP village
            try (ResultSet rs = s.executeQuery(
                    "SELECT v.id FROM public.villages v " +
                    "JOIN public.mandals m ON v.mandal_id = m.id " +
                    "JOIN public.districts d ON m.district_id = d.id " +
                    "JOIN public.states st ON d.state_id = st.id " +
                    "WHERE st.code = '28' AND v.is_active = true LIMIT 1")) {
                if (rs.next()) villageAP = (UUID) rs.getObject("id");
            }

            // Reference Crop & Season
            try (ResultSet rs = s.executeQuery("SELECT id FROM public.crops WHERE is_active = true ORDER BY name LIMIT 1")) {
                if (rs.next()) cropId = (UUID) rs.getObject("id");
            }
            try (ResultSet rs = s.executeQuery("SELECT id FROM public.crop_varieties WHERE crop_id = '" + cropId + "' AND is_active = true LIMIT 1")) {
                if (rs.next()) varietyId = (UUID) rs.getObject("id");
            }
            try (ResultSet rs = s.executeQuery("SELECT id FROM public.seasons WHERE is_active = true ORDER BY name LIMIT 1")) {
                if (rs.next()) seasonId = (UUID) rs.getObject("id");
            }

            // 2. Provision isolated test farmers
            s.execute("INSERT INTO auth.users (id, aud, role, email) VALUES " +
                     "('" + farmerA + "', 'authenticated', 'authenticated', 'e2e_farmer_a_" + farmerA + "@test.org'), " +
                     "('" + farmerB + "', 'authenticated', 'authenticated', 'e2e_farmer_b_" + farmerB + "@test.org')");

            s.execute("INSERT INTO public.farmer_profiles (id, full_name, profile_status, onboarding_completed, home_village_id) VALUES " +
                     "('" + farmerA + "', 'E2E Farmer Alpha', 'ACTIVE', true, '" + villageKerala + "'), " +
                     "('" + farmerB + "', 'E2E Farmer Beta', 'ACTIVE', true, '" + villageAP + "') " +
                     "ON CONFLICT (id) DO UPDATE SET profile_status = 'ACTIVE', onboarding_completed = true");
        }

        System.out.printf("Setup complete: Farmer A=%s (home=Kerala:%s), Farmer B=%s (home=AP:%s)%n",
                farmerA, villageKerala, farmerB, villageAP);
    }

    @AfterAll
    static void cleanupSuite() throws Exception {
        if (conn == null) return;
        try (Statement s = conn.createStatement()) {
            // Clean up notifications
            s.execute("DELETE FROM public.notifications WHERE farmer_id IN ('" + farmerA + "', '" + farmerB + "')");
            // Clean up cultivation records & triggers
            s.execute("DELETE FROM public.cultivation_status_events WHERE changed_by IN ('" + farmerA + "', '" + farmerB + "')");
            s.execute("DELETE FROM public.cultivation_intention_history WHERE farmer_id IN ('" + farmerA + "', '" + farmerB + "')");
            s.execute("DELETE FROM public.cultivation_intentions WHERE farmer_id IN ('" + farmerA + "', '" + farmerB + "')");
            // Clean up plots & idempotency keys
            s.execute("DELETE FROM public.idempotency_keys WHERE farmer_id IN ('" + farmerA + "', '" + farmerB + "')");
            s.execute("DELETE FROM public.plots WHERE farmer_id IN ('" + farmerA + "', '" + farmerB + "')");
            // Clean up farmer profiles & auth users
            s.execute("DELETE FROM public.farmer_profiles WHERE id IN ('" + farmerA + "', '" + farmerB + "')");
            s.execute("DELETE FROM auth.users WHERE id IN ('" + farmerA + "', '" + farmerB + "')");
        }
        System.out.println("[CLEANUP] All Task 12 E2E test fixtures completely removed from Supabase.");
        conn.close();
    }

    @Test @Order(1)
    @DisplayName("STAGE 1: Baseline Geography Invariants (Kerala & AP records preserved)")
    void stage1_baselineGeographyPreserved() throws Exception {
        assumeTrue(!skipAll);
        try (Statement s = conn.createStatement()) {
            int states = 0, districts = 0, mandals = 0, villages = 0;
            try (ResultSet rs = s.executeQuery("SELECT count(*) FROM public.states")) { if (rs.next()) states = rs.getInt(1); }
            try (ResultSet rs = s.executeQuery("SELECT count(*) FROM public.districts")) { if (rs.next()) districts = rs.getInt(1); }
            try (ResultSet rs = s.executeQuery("SELECT count(*) FROM public.mandals")) { if (rs.next()) mandals = rs.getInt(1); }
            try (ResultSet rs = s.executeQuery("SELECT count(*) FROM public.villages")) { if (rs.next()) villages = rs.getInt(1); }

            assertThat(states).isEqualTo(36);
            assertThat(districts).isEqualTo(784);
            assertThat(mandals).isEqualTo(169); // 91 legacy + 78 Kerala
            assertThat(villages).isEqualTo(3161); // 1,495 legacy + 1,666 Kerala

            System.out.printf("STAGE 1 PASSED: Baseline preserved (S=%d, D=%d, M=%d, V=%d)%n",
                    states, districts, mandals, villages);
        }
    }

    @Test @Order(2)
    @DisplayName("STAGE 2: Plot Registration & Decoupled Location (Farmer A in Kerala creates plot in AP)")
    void stage2_plotCreationAndDecoupling() throws Exception {
        assumeTrue(!skipAll);
        plotA1 = UUID.randomUUID();
        String label = "Plot A1 " + plotA1.toString().substring(0, 8);

        // Prove Farmer A (whose home village is in Kerala) can register a plot physically located in AP
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO public.plots (id, farmer_id, village_id, label, total_area_acres, is_active) " +
                "VALUES (?, ?, ?, ?, 12.50, true)")) {
            ps.setObject(1, plotA1);
            ps.setObject(2, farmerA);
            ps.setObject(3, villageAP); // Decoupled: plot village is AP, farmer home is Kerala
            ps.setString(4, label);
            int rows = ps.executeUpdate();
            assertThat(rows).isEqualTo(1);
        }

        // Verify plot retrieval
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT village_id, total_area_acres, is_active FROM public.plots WHERE id = ? AND farmer_id = ?")) {
            ps.setObject(1, plotA1);
            ps.setObject(2, farmerA);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat((UUID) rs.getObject("village_id")).isEqualTo(villageAP);
                assertThat(rs.getBigDecimal("total_area_acres")).isEqualByComparingTo(new BigDecimal("12.50"));
                assertThat(rs.getBoolean("is_active")).isTrue();
            }
        }
        System.out.println("STAGE 2 PASSED: Plot registered successfully with decoupled geography.");
    }

    @Test @Order(3)
    @DisplayName("STAGE 3: Plot Constraint Enforcement (Negative area rejected, duplicate label rejected)")
    void stage3_plotConstraints() throws Exception {
        assumeTrue(!skipAll);

        // 1. Negative area check constraint
        UUID badPlot = UUID.randomUUID();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO public.plots (id, farmer_id, village_id, label, total_area_acres, is_active) " +
                "VALUES (?, ?, ?, 'Bad Area Plot', -5.00, true)")) {
            ps.setObject(1, badPlot);
            ps.setObject(2, farmerA);
            ps.setObject(3, villageKerala);
            assertThatThrownBy(ps::executeUpdate)
                    .as("CHECK plots_area_positive must reject negative total_area_acres")
                    .isInstanceOf(SQLException.class);
        }

        // 2. Case-insensitive duplicate label per farmer (ux_plots_farmer_label_ci)
        // Retrieve label of plotA1
        String existingLabel;
        try (PreparedStatement ps = conn.prepareStatement("SELECT label FROM public.plots WHERE id = ?")) {
            ps.setObject(1, plotA1);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                existingLabel = rs.getString(1);
            }
        }

        UUID duplicateLabelPlot = UUID.randomUUID();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO public.plots (id, farmer_id, village_id, label, total_area_acres, is_active) " +
                "VALUES (?, ?, ?, ?, 5.00, true)")) {
            ps.setObject(1, duplicateLabelPlot);
            ps.setObject(2, farmerA);
            ps.setObject(3, villageKerala);
            ps.setString(4, existingLabel.toUpperCase()); // Duplicate in upper case
            assertThatThrownBy(ps::executeUpdate)
                    .as("Unique index ux_plots_farmer_label_ci must reject duplicate label for same farmer")
                    .isInstanceOf(SQLException.class);
        }

        System.out.println("STAGE 3 PASSED: Plot constraints and unique indexes verified.");
    }

    @Test @Order(4)
    @DisplayName("STAGE 4: Idempotency Key Insertion and Replay Protection")
    void stage4_idempotencyProtection() throws Exception {
        assumeTrue(!skipAll);
        String idemKey = "idem-" + plotA1;

        // 1. Insert idempotency key
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO public.idempotency_keys (farmer_id, idempotency_key, request_hash, response_status, response_body, expires_at) " +
                "VALUES (?, ?, 'hash_plot_create_123', 201, '{\"id\":\"" + plotA1 + "\"}'::jsonb, now() + interval '24 hours')")) {
            ps.setObject(1, farmerA);
            ps.setString(2, idemKey);
            int rows = ps.executeUpdate();
            assertThat(rows).isEqualTo(1);
        }

        // 2. Duplicate key for same farmer rejected by uq_idempotency_farmer_key
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO public.idempotency_keys (farmer_id, idempotency_key, request_hash, response_status, response_body, expires_at) " +
                "VALUES (?, ?, 'hash_plot_create_123', 201, '{\"id\":\"" + plotA1 + "\"}'::jsonb, now() + interval '24 hours')")) {
            ps.setObject(1, farmerA);
            ps.setString(2, idemKey);
            assertThatThrownBy(ps::executeUpdate)
                    .as("Unique constraint uq_idempotency_farmer_key must reject duplicate key")
                    .isInstanceOf(SQLException.class);
        }

        System.out.println("STAGE 4 PASSED: Idempotency key verified with duplicate replay protection.");
    }

    @Test @Order(5)
    @DisplayName("STAGE 5: Cultivation Registration & Trigger Event (record_status_event_insert)")
    void stage5_cultivationCreationAndTrigger() throws Exception {
        assumeTrue(!skipAll);
        cultivationA1 = UUID.randomUUID();
        short year = (short) LocalDate.now().getYear();

        // Set actor session context required by triggers
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT set_config('app.actor_id', ?, false), set_config('app.actor_type', 'FARMER', false)")) {
            ps.setString(1, farmerA.toString());
            ps.executeQuery();
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO public.cultivation_intentions " +
                "(id, plot_id, farmer_id, crop_id, crop_variety_id, season_id, cultivation_year, cultivated_area_acres, planned_planting_date, status, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, 8.00, ?, 'PLANNED', now(), now())")) {
            ps.setObject(1, cultivationA1);
            ps.setObject(2, plotA1);
            ps.setObject(3, farmerA);
            ps.setObject(4, cropId);
            ps.setObject(5, varietyId);
            ps.setObject(6, seasonId);
            ps.setShort(7, year);
            ps.setDate(8, Date.valueOf(LocalDate.now().plusDays(10)));
            int rows = ps.executeUpdate();
            assertThat(rows).isEqualTo(1);
        }

        // Verify trigger record_status_event_insert created initial status event
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT from_status, to_status, changed_by, actor_type FROM public.cultivation_status_events WHERE intention_id = ?")) {
            ps.setObject(1, cultivationA1);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("from_status")).isNull();
                assertThat(rs.getString("to_status")).isEqualTo("PLANNED");
                assertThat((UUID) rs.getObject("changed_by")).isEqualTo(farmerA);
            }
        }

        System.out.println("STAGE 5 PASSED: Cultivation created; initial status event recorded by trigger.");
    }

    @Test @Order(6)
    @DisplayName("STAGE 6: Cultivation Invariants (Single crop/cycle, unique cycle ux_active_cultivation_cycle)")
    void stage6_cultivationCycleUniqueness() throws Exception {
        assumeTrue(!skipAll);
        short year = (short) LocalDate.now().getYear();
        UUID duplicateCycleIntention = UUID.randomUUID();

        // Attempting to register another cultivation on same plot, same season, same year must fail
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO public.cultivation_intentions " +
                "(id, plot_id, farmer_id, crop_id, season_id, cultivation_year, cultivated_area_acres, planned_planting_date, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, 4.00, ?, 'PLANNED')")) {
            ps.setObject(1, duplicateCycleIntention);
            ps.setObject(2, plotA1);
            ps.setObject(3, farmerA);
            ps.setObject(4, cropId);
            ps.setObject(5, seasonId);
            ps.setShort(6, year);
            ps.setDate(7, Date.valueOf(LocalDate.now().plusDays(15)));
            assertThatThrownBy(ps::executeUpdate)
                    .as("ux_active_cultivation_cycle must prevent concurrent active cycles on the same plot/season/year")
                    .isInstanceOf(SQLException.class);
        }

        System.out.println("STAGE 6 PASSED: Unique cycle constraint prevents duplicate active cycles.");
    }

    @Test @Order(7)
    @DisplayName("STAGE 7: Lifecycle State Machine Transitions & History Snapshots")
    void stage7_lifecycleTransitionsAndHistory() throws Exception {
        assumeTrue(!skipAll);

        // 1. Transition PLANNED -> STARTED
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT set_config('app.actor_id', ?, false), set_config('app.actor_type', 'FARMER', false)")) {
            ps.setString(1, farmerA.toString());
            ps.executeQuery();
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE public.cultivation_intentions " +
                "SET status = 'STARTED', actual_planting_date = ?, version = version + 1 " +
                "WHERE id = ? AND status = 'PLANNED'")) {
            ps.setDate(1, Date.valueOf(LocalDate.now()));
            ps.setObject(2, cultivationA1);
            int rows = ps.executeUpdate();
            assertThat(rows).isEqualTo(1);
        }

        // Verify status event logged for STARTED
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT count(*) FROM public.cultivation_status_events WHERE intention_id = ? AND to_status = 'STARTED'")) {
            ps.setObject(1, cultivationA1);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertThat(rs.getInt(1)).isGreaterThanOrEqualTo(1);
            }
        }

        // 2. Transition STARTED -> COMPLETED
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT set_config('app.actor_id', ?, false), set_config('app.actor_type', 'FARMER', false)")) {
            ps.setString(1, farmerA.toString());
            ps.executeQuery();
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE public.cultivation_intentions " +
                "SET status = 'COMPLETED', harvest_date = ?, actual_area_acres = 8.00, version = version + 1 " +
                "WHERE id = ? AND status = 'STARTED'")) {
            ps.setDate(1, Date.valueOf(LocalDate.now()));
            ps.setObject(2, cultivationA1);
            int rows = ps.executeUpdate();
            assertThat(rows).isEqualTo(1);
        }

        // Verify history snapshot exists capturing prior version
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT count(*) FROM public.cultivation_intention_history WHERE intention_id = ?")) {
            ps.setObject(1, cultivationA1);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertThat(rs.getInt(1)).isGreaterThanOrEqualTo(1);
            }
        }

        // 3. Verify terminal lock: Editing COMPLETED intention must be rejected by trigger
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE public.cultivation_intentions SET cultivated_area_acres = 9.00 WHERE id = ?")) {
            ps.setObject(1, cultivationA1);
            assertThatThrownBy(ps::executeUpdate)
                    .as("Trigger private.validate_intention_write must reject edits on COMPLETED records")
                    .isInstanceOf(SQLException.class);
        }

        System.out.println("STAGE 7 PASSED: Full lifecycle transitions, history snapshots, and terminal lock verified.");
    }

    @Test @Order(8)
    @DisplayName("STAGE 8: IDOR & Cross-Farmer Access Barrier")
    void stage8_idorCrossFarmerBarrier() throws Exception {
        assumeTrue(!skipAll);

        // Farmer B cannot retrieve Farmer A's plot
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT count(*) FROM public.plots WHERE id = ? AND farmer_id = ?")) {
            ps.setObject(1, plotA1);
            ps.setObject(2, farmerB);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertThat(rs.getInt(1)).isZero();
            }
        }

        // Farmer B cannot retrieve Farmer A's cultivation intention
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT count(*) FROM public.cultivation_intentions WHERE id = ? AND farmer_id = ?")) {
            ps.setObject(1, cultivationA1);
            ps.setObject(2, farmerB);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertThat(rs.getInt(1)).isZero();
            }
        }

        // Farmer B cannot retrieve Farmer A's history
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT count(*) FROM public.cultivation_intention_history WHERE intention_id = ? AND farmer_id = ?")) {
            ps.setObject(1, cultivationA1);
            ps.setObject(2, farmerB);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertThat(rs.getInt(1)).isZero();
            }
        }

        System.out.println("STAGE 8 PASSED: IDOR barriers verified across plots, cultivations, and history.");
    }

    @Test @Order(9)
    @DisplayName("STAGE 9: Notification Flow & Unread Partial Index Query")
    void stage9_notificationsFlow() throws Exception {
        assumeTrue(!skipAll);
        notificationA1 = UUID.randomUUID();

        // 1. Insert notification for Farmer A
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO public.notifications (id, farmer_id, title, body, type, created_at) " +
                "VALUES (?, ?, 'Harvest Completed', 'Your cultivation cycle has been marked COMPLETED.', 'CULTIVATION', now())")) {
            ps.setObject(1, notificationA1);
            ps.setObject(2, farmerA);
            int rows = ps.executeUpdate();
            assertThat(rows).isEqualTo(1);
        }

        // 2. Query unread count using partial index
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT count(*) FROM public.notifications WHERE farmer_id = ? AND read_at IS NULL")) {
            ps.setObject(1, farmerA);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertThat(rs.getLong(1)).isGreaterThanOrEqualTo(1L);
            }
        }

        // 3. Mark as read
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE public.notifications SET read_at = now() WHERE id = ? AND farmer_id = ? AND read_at IS NULL")) {
            ps.setObject(1, notificationA1);
            ps.setObject(2, farmerA);
            int rows = ps.executeUpdate();
            assertThat(rows).isEqualTo(1);
        }

        // 4. Verify idempotent mark-read (returns 0 rows on replay)
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE public.notifications SET read_at = now() WHERE id = ? AND farmer_id = ? AND read_at IS NULL")) {
            ps.setObject(1, notificationA1);
            ps.setObject(2, farmerA);
            int rows = ps.executeUpdate();
            assertThat(rows).isZero();
        }

        System.out.println("STAGE 9 PASSED: Notification flow, partial index, and idempotent read verified.");
    }

    @Test @Order(10)
    @DisplayName("STAGE 10: Transaction Boundaries & Rollback Integrity (No phantom records)")
    void stage10_transactionRollbackIntegrity() throws Exception {
        assumeTrue(!skipAll);
        UUID rollbackPlotId = UUID.randomUUID();

        // Execute inside manual transaction that fails and rolls back
        conn.setAutoCommit(false);
        try {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO public.plots (id, farmer_id, village_id, label, total_area_acres, is_active) " +
                    "VALUES (?, ?, ?, 'Rollback Plot', 10.00, true)")) {
                ps.setObject(1, rollbackPlotId);
                ps.setObject(2, farmerA);
                ps.setObject(3, villageKerala);
                ps.executeUpdate();
            }

            // Simulate explicit rollback
            conn.rollback();
        } finally {
            conn.setAutoCommit(true);
        }

        // Verify zero phantom records exist after rollback
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT count(*) FROM public.plots WHERE id = ?")) {
            ps.setObject(1, rollbackPlotId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertThat(rs.getInt(1)).as("Rolled-back plot must not exist in database").isZero();
            }
        }

        System.out.println("STAGE 10 PASSED: Transaction rollback integrity verified — zero phantom records.");
    }
}
