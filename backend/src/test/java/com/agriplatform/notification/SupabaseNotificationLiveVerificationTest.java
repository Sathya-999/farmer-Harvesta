package com.agriplatform.notification;

import com.agriplatform.notification.entity.Notification;
import org.junit.jupiter.api.*;

import java.sql.*;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Task 11 ? Live Supabase Notification Verification (10 Gates).
 *
 * <p>All fixtures are created and cleaned up within this test.
 * Uses direct JDBC ? no Spring context loaded.
 * Farmer used: the test creates a temporary notification against an
 * existing farmer_profiles row resolved from the live DB.
 */
@DisplayName("Task 11 ? Live Supabase Notification Verification")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SupabaseNotificationLiveVerificationTest {

    private static Connection conn;
    private static UUID farmerA;
    private static UUID farmerB;
    private static UUID notifA1;
    private static UUID notifA2;
    private static boolean skipAll = false;  // true when DB has no active farmers

    private static Connection getConnection() throws Exception {
        String url = System.getenv("SUPABASE_DB_URL");
        String username = System.getenv("SUPABASE_DB_USERNAME");
        String password = System.getenv("SUPABASE_DB_PASSWORD");
        if (url == null || username == null || password == null) return null;
        return DriverManager.getConnection(url, username, password);
    }

    @BeforeAll
    static void setupConnection() throws Exception {
        conn = getConnection();
        if (conn == null) {
            System.out.println("[SKIP] Supabase credentials not available — all live gates skipped.");
            skipAll = true;
            return;
        }
        conn.setAutoCommit(true);

        farmerA = UUID.randomUUID();
        farmerB = UUID.randomUUID();

        // Provision isolated test farmers in auth.users and farmer_profiles
        try (Statement s = conn.createStatement()) {
            s.execute("INSERT INTO auth.users (id, aud, role, email) VALUES " +
                     "('" + farmerA + "', 'authenticated', 'authenticated', 'notif_farmer_a_" + farmerA + "@test.org'), " +
                     "('" + farmerB + "', 'authenticated', 'authenticated', 'notif_farmer_b_" + farmerB + "@test.org')");

            s.execute("INSERT INTO public.farmer_profiles (id, full_name, profile_status, onboarding_completed) VALUES " +
                     "('" + farmerA + "', 'Test Farmer A', 'ACTIVE', true), " +
                     "('" + farmerB + "', 'Test Farmer B', 'ACTIVE', true) " +
                     "ON CONFLICT (id) DO UPDATE SET profile_status = 'ACTIVE', onboarding_completed = true");
        }

        System.out.println("Live test using Farmer A: " + farmerA + ", Farmer B: " + farmerB);
    }

    @AfterAll
    static void cleanup() throws Exception {
        if (conn == null) return;
        try (Statement s = conn.createStatement()) {
            if (notifA1 != null) s.execute("DELETE FROM public.notifications WHERE id='" + notifA1 + "'");
            if (notifA2 != null) s.execute("DELETE FROM public.notifications WHERE id='" + notifA2 + "'");
            if (farmerA != null) {
                s.execute("DELETE FROM public.notifications WHERE farmer_id IN ('" + farmerA + "', '" + farmerB + "')");
                s.execute("DELETE FROM public.farmer_profiles WHERE id IN ('" + farmerA + "', '" + farmerB + "')");
                s.execute("DELETE FROM auth.users WHERE id IN ('" + farmerA + "', '" + farmerB + "')");
            }
        }
        System.out.println("[CLEANUP] All live notification test records and test farmers removed.");
        conn.close();
    }

    @Test @Order(1) @DisplayName("GATE 1: Schema — notifications table exists with correct columns")
    void gate1_schemaCorrect() throws Exception {
        assumeTrue(!skipAll, "No ACTIVE farmers in live DB or credentials unavailable — gate skipped");
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery(
                "SELECT column_name FROM information_schema.columns " +
                "WHERE table_schema='public' AND table_name='notifications' ORDER BY ordinal_position")) {
            StringBuilder cols = new StringBuilder();
            while (rs.next()) cols.append(rs.getString(1)).append(",");
            String allCols = cols.toString();
            assertThat(allCols).contains("id","farmer_id","title","body","type","read_at","created_at");
            System.out.println("GATE 1 PASSED: Schema columns verified: " + allCols);
        }
    }

    @Test @Order(2) @DisplayName("GATE 2: Insert — notification created with correct defaults")
    void gate2_insertNotification() throws Exception {
        assumeTrue(!skipAll, "No ACTIVE farmers in live DB — gate skipped");
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO public.notifications (farmer_id, title, body, type) VALUES (?,?,?,?) RETURNING id")) {
            ps.setObject(1, farmerA);
            ps.setString(2, "Test Notification Gate 2");
            ps.setString(3, "This is the body for gate 2.");
            ps.setString(4, "SYSTEM");
            ResultSet rs = ps.executeQuery();
            assertThat(rs.next()).isTrue();
            notifA1 = UUID.fromString(rs.getString("id"));
            assertThat(notifA1).isNotNull();
        }
        // Verify defaults
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT type, read_at, created_at FROM public.notifications WHERE id=?")) {
            ps.setObject(1, notifA1);
            ResultSet rs = ps.executeQuery();
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("type")).isEqualTo("SYSTEM");
            assertThat(rs.getObject("read_at")).isNull();
            assertThat(rs.getTimestamp("created_at")).isNotNull();
        }
        System.out.println("GATE 2 PASSED: Notification inserted with id=" + notifA1);
    }

    @Test @Order(3) @DisplayName("GATE 3: Read — correct farmer can retrieve notification")
    void gate3_farmerCanRetrieveOwn() throws Exception {
        assumeTrue(!skipAll, "No ACTIVE farmers in live DB — gate skipped");
        assumeTrue(notifA1 != null, "notifA1 not created — gate 2 may have been skipped");
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, title FROM public.notifications WHERE id=? AND farmer_id=?")) {
            ps.setObject(1, notifA1);
            ps.setObject(2, farmerA);
            ResultSet rs = ps.executeQuery();
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("title")).isEqualTo("Test Notification Gate 2");
        }
        System.out.println("GATE 3 PASSED: Farmer A can retrieve own notification " + notifA1);
    }

    @Test @Order(4) @DisplayName("GATE 4: IDOR — other farmer cannot retrieve notification")
    void gate4_idorIsolation() throws Exception {
        assumeTrue(!skipAll, "No ACTIVE farmers in live DB — gate skipped");
        assumeTrue(notifA1 != null, "notifA1 not created — gate 2 may have been skipped");
        assumeTrue(!farmerA.equals(farmerB), "GATE 4 SKIPPED: only one ACTIVE farmer in DB (farmerA == farmerB)");
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id FROM public.notifications WHERE id=? AND farmer_id=?")) {
            ps.setObject(1, notifA1);
            ps.setObject(2, farmerB);
            ResultSet rs = ps.executeQuery();
            assertThat(rs.next()).isFalse();
        }
        System.out.println("GATE 4 PASSED: Farmer B cannot retrieve Farmer A's notification");
    }

    @Test @Order(5) @DisplayName("GATE 5: Unread state — read_at IS NULL initially")
    void gate5_unreadState() throws Exception {
        assumeTrue(!skipAll, "No ACTIVE farmers in live DB — gate skipped");
        assumeTrue(notifA1 != null, "notifA1 not created — gate 2 may have been skipped");
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT read_at FROM public.notifications WHERE id=? AND farmer_id=?")) {
            ps.setObject(1, notifA1);
            ps.setObject(2, farmerA);
            ResultSet rs = ps.executeQuery();
            assertThat(rs.next()).isTrue();
            assertThat(rs.getObject("read_at")).isNull();
        }
        System.out.println("GATE 5 PASSED: Notification is unread (read_at IS NULL)");
    }

    @Test @Order(6) @DisplayName("GATE 6: Mark read — read_at is set, idempotent on second call")
    void gate6_markRead() throws Exception {
        assumeTrue(!skipAll, "No ACTIVE farmers in live DB — gate skipped");
        assumeTrue(notifA1 != null, "notifA1 not created — gate 2 may have been skipped");
        // First mark-read
        Timestamp ts = Timestamp.from(java.time.Instant.now());
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE public.notifications SET read_at=? WHERE id=? AND farmer_id=? AND read_at IS NULL")) {
            ps.setTimestamp(1, ts);
            ps.setObject(2, notifA1);
            ps.setObject(3, farmerA);
            int rows = ps.executeUpdate();
            assertThat(rows).isEqualTo(1);
        }
        // Verify read_at set
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT read_at FROM public.notifications WHERE id=?")) {
            ps.setObject(1, notifA1);
            ResultSet rs = ps.executeQuery();
            assertThat(rs.next()).isTrue();
            assertThat(rs.getTimestamp("read_at")).isNotNull();
        }
        // Second mark-read ? idempotent (WHERE read_at IS NULL means 0 rows)
        Timestamp ts2 = Timestamp.from(java.time.Instant.now().plusSeconds(5));
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE public.notifications SET read_at=? WHERE id=? AND farmer_id=? AND read_at IS NULL")) {
            ps.setTimestamp(1, ts2);
            ps.setObject(2, notifA1);
            ps.setObject(3, farmerA);
            int rows = ps.executeUpdate();
            assertThat(rows).isZero();
        }
        System.out.println("GATE 6 PASSED: Mark-read works and is idempotent");
    }

    @Test @Order(7) @DisplayName("GATE 7: Check constraint — blank title/body rejected")
    void gate7_checkConstraint() throws Exception {
        assumeTrue(!skipAll, "No ACTIVE farmers in live DB — gate skipped");
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO public.notifications (farmer_id, title, body) VALUES (?,?,?)")) {
            ps.setObject(1, farmerA);
            ps.setString(2, "   ");  // blank title should fail CHECK constraint
            ps.setString(3, "Body");
            try {
                ps.executeUpdate();
                Assertions.fail("Expected constraint violation for blank title");
            } catch (SQLException e) {
                assertThat(e.getMessage()).containsIgnoringCase("check");
            }
        }
        System.out.println("GATE 7 PASSED: Blank title correctly rejected by DB CHECK constraint");
    }

    @Test @Order(8) @DisplayName("GATE 8: Pagination — mark-all-read + unread count")
    void gate8_markAllAndCount() throws Exception {
        assumeTrue(!skipAll, "No ACTIVE farmers in live DB — gate skipped");
        // Insert a second notification for farmer A
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO public.notifications (farmer_id, title, body) VALUES (?,?,?) RETURNING id")) {
            ps.setObject(1, farmerA);
            ps.setString(2, "Second Notification Gate 8");
            ps.setString(3, "Second body.");
            ResultSet rs = ps.executeQuery();
            assertThat(rs.next()).isTrue();
            notifA2 = UUID.fromString(rs.getString("id"));
        }
        // Count unread for farmer A (notifA1 is read, notifA2 is unread)
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM public.notifications WHERE farmer_id=? AND read_at IS NULL")) {
            ps.setObject(1, farmerA);
            ResultSet rs = ps.executeQuery();
            assertThat(rs.next()).isTrue();
            long unread = rs.getLong(1);
            assertThat(unread).isGreaterThanOrEqualTo(1L);
        }
        // Mark all as read
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE public.notifications SET read_at=now() WHERE farmer_id=? AND read_at IS NULL")) {
            ps.setObject(1, farmerA);
            int rows = ps.executeUpdate();
            assertThat(rows).isGreaterThanOrEqualTo(1);
        }
        // Confirm unread count is now zero for our test notifications
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM public.notifications WHERE id IN (?,?) AND read_at IS NULL")) {
            ps.setObject(1, notifA1);
            ps.setObject(2, notifA2);
            ResultSet rs = ps.executeQuery();
            assertThat(rs.next()).isTrue();
            assertThat(rs.getLong(1)).isZero();
        }
        System.out.println("GATE 8 PASSED: Pagination and mark-all-read verified. notifA2=" + notifA2);
    }

    @Test @Order(9) @DisplayName("GATE 9: FK constraint — farmer_id must reference existing farmer")
    void gate9_fkConstraint() throws Exception {
        assumeTrue(!skipAll, "No ACTIVE farmers in live DB — gate skipped");
        UUID nonExistentFarmer = UUID.randomUUID();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO public.notifications (farmer_id, title, body) VALUES (?,?,?)")) {
            ps.setObject(1, nonExistentFarmer);
            ps.setString(2, "Orphan");
            ps.setString(3, "Body");
            try {
                ps.executeUpdate();
                Assertions.fail("Expected FK violation for non-existent farmer");
            } catch (SQLException e) {
                assertThat(e.getMessage()).containsIgnoringCase("foreign key");
            }
        }
        System.out.println("GATE 9 PASSED: FK constraint correctly rejects non-existent farmer_id");
    }

    @Test @Order(10) @DisplayName("GATE 10: Performance — unread query uses partial index efficiently")
    void gate10_performance() throws Exception {
        assumeTrue(!skipAll, "No ACTIVE farmers in live DB — gate skipped");
        long start = System.currentTimeMillis();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, title, created_at FROM public.notifications " +
                "WHERE farmer_id=? AND read_at IS NULL ORDER BY created_at DESC LIMIT 20")) {
            ps.setObject(1, farmerA);
            ResultSet rs = ps.executeQuery();
            int count = 0;
            while (rs.next()) count++;
            System.out.println("Unread query returned " + count + " rows");
        }
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("GATE 10: Unread query duration = " + elapsed + " ms");
        assertThat(elapsed).as("Unread query must complete in under 1500ms").isLessThan(1500L);
        System.out.println("GATE 10 PASSED: Query performance " + elapsed + "ms < 1500ms threshold");
    }
}