package com.agriplatform.plot;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Supabase Plot Live DB Audit & Migration Test")
class SupabasePlotLiveAuditTest {

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
    @DisplayName("Audit live DB for plots, cultivation_intentions, and apply idempotency_keys migration")
    void auditAndApplyIdempotencySchema() throws Exception {
        try (Connection conn = getConnection()) {
            if (conn == null) {
                System.out.println("Supabase credentials not available — skipping live audit");
                return;
            }

            try (Statement stmt = conn.createStatement()) {
                System.out.println("==================================================");
                System.out.println("1. AUDITING public.plots");
                System.out.println("==================================================");
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT column_name, data_type, udt_name, is_nullable, column_default " +
                        "FROM information_schema.columns " +
                        "WHERE table_schema = 'public' AND table_name = 'plots' " +
                        "ORDER BY ordinal_position")) {
                    while (rs.next()) {
                        System.out.printf("  %s : %s (%s), nullable=%s, default=%s%n",
                                rs.getString("column_name"),
                                rs.getString("data_type"),
                                rs.getString("udt_name"),
                                rs.getString("is_nullable"),
                                rs.getString("column_default"));
                    }
                }

                System.out.println("\n--- Constraints on public.plots ---");
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT conname, contype, pg_get_constraintdef(c.oid) " +
                        "FROM pg_constraint c " +
                        "JOIN pg_namespace n ON n.oid = c.connamespace " +
                        "WHERE n.nspname = 'public' AND conrelid = 'public.plots'::regclass")) {
                    while (rs.next()) {
                        System.out.printf("  Constraint: %s (type %s) -> %s%n",
                                rs.getString(1), rs.getString(2), rs.getString(3));
                    }
                }

                System.out.println("\n--- Indexes on public.plots ---");
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT indexname, indexdef FROM pg_indexes WHERE schemaname = 'public' AND tablename = 'plots'")) {
                    while (rs.next()) {
                        System.out.printf("  Index: %s -> %s%n", rs.getString(1), rs.getString(2));
                    }
                }

                System.out.println("\n--- Triggers on public.plots ---");
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT tgname, pg_get_triggerdef(oid) FROM pg_trigger WHERE tgrelid = 'public.plots'::regclass AND NOT tgisinternal")) {
                    while (rs.next()) {
                        System.out.printf("  Trigger: %s -> %s%n", rs.getString(1), rs.getString(2));
                    }
                }

                System.out.println("\n==================================================");
                System.out.println("2. AUDITING public.cultivation_intentions");
                System.out.println("==================================================");
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT column_name, data_type, udt_name, is_nullable " +
                        "FROM information_schema.columns " +
                        "WHERE table_schema = 'public' AND table_name = 'cultivation_intentions' " +
                        "ORDER BY ordinal_position")) {
                    while (rs.next()) {
                        System.out.printf("  %s : %s (%s), nullable=%s%n",
                                rs.getString("column_name"),
                                rs.getString("data_type"),
                                rs.getString("udt_name"),
                                rs.getString("is_nullable"));
                    }
                }

                System.out.println("\n--- Constraints on public.cultivation_intentions ---");
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT conname, contype, pg_get_constraintdef(c.oid) " +
                        "FROM pg_constraint c " +
                        "JOIN pg_namespace n ON n.oid = c.connamespace " +
                        "WHERE n.nspname = 'public' AND conrelid = 'public.cultivation_intentions'::regclass")) {
                    while (rs.next()) {
                        System.out.printf("  Constraint: %s (type %s) -> %s%n",
                                rs.getString(1), rs.getString(2), rs.getString(3));
                    }
                }

                System.out.println("\n==================================================");
                System.out.println("3. CHECKING public.idempotency_keys");
                System.out.println("==================================================");
                boolean exists = false;
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'idempotency_keys')")) {
                    if (rs.next()) {
                        exists = rs.getBoolean(1);
                    }
                }
                System.out.println("public.idempotency_keys exists: " + exists);

                if (!exists) {
                    System.out.println("\nApplying migration for public.idempotency_keys...");
                    String createTableSql =
                            "CREATE TABLE public.idempotency_keys (\n" +
                            "    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),\n" +
                            "    farmer_id UUID NOT NULL REFERENCES public.farmer_profiles(id) ON DELETE CASCADE,\n" +
                            "    idempotency_key VARCHAR(128) NOT NULL,\n" +
                            "    request_hash VARCHAR(64) NOT NULL,\n" +
                            "    response_status INT NOT NULL,\n" +
                            "    response_body JSONB NOT NULL,\n" +
                            "    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),\n" +
                            "    expires_at TIMESTAMPTZ NOT NULL,\n" +
                            "    CONSTRAINT uq_idempotency_farmer_key UNIQUE (farmer_id, idempotency_key)\n" +
                            ");";
                    stmt.execute(createTableSql);
                    System.out.println("Table public.idempotency_keys created successfully.");

                    String createIndexSql = "CREATE INDEX idx_idempotency_expires_at ON public.idempotency_keys(expires_at);";
                    stmt.execute(createIndexSql);
                    System.out.println("Index idx_idempotency_expires_at created successfully.");
                }

                System.out.println("\n--- VERIFYING public.idempotency_keys SCHEMA ---");
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT column_name, data_type, udt_name, is_nullable, column_default " +
                        "FROM information_schema.columns " +
                        "WHERE table_schema = 'public' AND table_name = 'idempotency_keys' " +
                        "ORDER BY ordinal_position")) {
                    int colCount = 0;
                    while (rs.next()) {
                        colCount++;
                        System.out.printf("  Column: %s : %s (%s), nullable=%s, default=%s%n",
                                rs.getString("column_name"),
                                rs.getString("data_type"),
                                rs.getString("udt_name"),
                                rs.getString("is_nullable"),
                                rs.getString("column_default"));
                    }
                    assertThat(colCount).isEqualTo(8);
                }

                System.out.println("\n--- VERIFYING CONSTRAINTS ON public.idempotency_keys ---");
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT conname, contype, pg_get_constraintdef(c.oid) " +
                        "FROM pg_constraint c " +
                        "JOIN pg_namespace n ON n.oid = c.connamespace " +
                        "WHERE n.nspname = 'public' AND conrelid = 'public.idempotency_keys'::regclass")) {
                    while (rs.next()) {
                        System.out.printf("  Constraint: %s (type %s) -> %s%n",
                                rs.getString(1), rs.getString(2), rs.getString(3));
                    }
                }

                System.out.println("\n--- VERIFYING INDEXES ON public.idempotency_keys ---");
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT indexname, indexdef FROM pg_indexes WHERE schemaname = 'public' AND tablename = 'idempotency_keys'")) {
                    while (rs.next()) {
                        System.out.printf("  Index: %s -> %s%n", rs.getString(1), rs.getString(2));
                    }
                }

                System.out.println("\n--- TRIGGER FUNCTION private.validate_plot_write ---");
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT prosrc FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace " +
                        "WHERE n.nspname = 'private' AND p.proname = 'validate_plot_write'")) {
                    if (rs.next()) {
                        System.out.println(rs.getString(1));
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("Live Supabase End-to-End Database Lifecycle Verification")
    void testLivePlotLifecycleAndConstraints() throws Exception {
        try (Connection conn = getConnection()) {
            if (conn == null) {
                System.out.println("Supabase credentials not available — skipping live test");
                return;
            }

            conn.setAutoCommit(true);
            java.util.UUID farmerId = null;
            java.util.UUID plotId = null;

            try (Statement stmt = conn.createStatement()) {
                // 1. Find an active village
                java.util.UUID villageId = null;
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT id FROM public.villages WHERE is_active = true LIMIT 1")) {
                    if (rs.next()) {
                        villageId = (java.util.UUID) rs.getObject("id");
                    }
                }
                assertThat(villageId).as("Must have at least one active village").isNotNull();

                // 2. Create isolated test user & active farmer_profile
                java.util.UUID testAuthId = java.util.UUID.randomUUID();
                stmt.execute("INSERT INTO auth.users (id, aud, role, email) " +
                             "VALUES ('" + testAuthId + "', 'authenticated', 'authenticated', 'test_" + testAuthId + "@example.com')");
                int updated = stmt.executeUpdate(
                        "UPDATE public.farmer_profiles SET profile_status = 'ACTIVE', onboarding_completed = true " +
                        "WHERE id = '" + testAuthId + "'");
                if (updated == 0) {
                    stmt.execute("INSERT INTO public.farmer_profiles (id, full_name, profile_status, onboarding_completed) " +
                                 "VALUES ('" + testAuthId + "', 'Live Test Farmer', 'ACTIVE', true)");
                }
                farmerId = testAuthId;
                System.out.printf("Live test using Farmer: %s, Village: %s%n", farmerId, villageId);

                // 3. Test Insert Plot
                plotId = java.util.UUID.randomUUID();
                String testLabel = "TestPlot_" + plotId.toString().substring(0, 8);
                String initialVersion = null;

                try (java.sql.PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO public.plots (id, farmer_id, village_id, label, total_area_acres, is_active, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, 5.5000, true, now(), now()) RETURNING to_char(updated_at, 'YYYY-MM-DD\"T\"HH24:MI:SS.US\"Z\"')")) {
                    ps.setObject(1, plotId);
                    ps.setObject(2, farmerId);
                    ps.setObject(3, villageId);
                    ps.setString(4, testLabel);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertThat(rs.next()).isTrue();
                        initialVersion = rs.getString(1);
                        System.out.printf("Plot inserted: %s, initial version: %s%n", plotId, initialVersion);
                    }
                }
                assertThat(initialVersion).isNotNull();

                // 4. Test Idempotency insert & unique constraint
                String idempotencyKey = "key-" + plotId;
                try (java.sql.PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO public.idempotency_keys (farmer_id, idempotency_key, request_hash, response_status, response_body, expires_at) " +
                        "VALUES (?, ?, 'dummyhash123', 201, '{\"plotId\":\"test\"}'::jsonb, now() + interval '24 hours')")) {
                    ps.setObject(1, farmerId);
                    ps.setString(2, idempotencyKey);
                    int inserted = ps.executeUpdate();
                    assertThat(inserted).isEqualTo(1);
                    System.out.println("Idempotency key inserted successfully.");
                }

                // Verify duplicate idempotency key rejected by uq_idempotency_farmer_key
                boolean dupKeyRejected = false;
                try (java.sql.PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO public.idempotency_keys (farmer_id, idempotency_key, request_hash, response_status, response_body, expires_at) " +
                        "VALUES (?, ?, 'dummyhash456', 201, '{\"plotId\":\"test\"}'::jsonb, now() + interval '24 hours')")) {
                    ps.setObject(1, farmerId);
                    ps.setString(2, idempotencyKey);
                    ps.executeUpdate();
                } catch (java.sql.SQLException e) {
                    dupKeyRejected = true;
                    System.out.println("Duplicate idempotency key correctly rejected: " + e.getMessage());
                }
                assertThat(dupKeyRejected).isTrue();

                // 5. Test Case-insensitive duplicate label rejected by ux_plots_farmer_label_ci
                boolean dupLabelRejected = false;
                try (java.sql.PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO public.plots (id, farmer_id, village_id, label, total_area_acres, is_active) " +
                        "VALUES (gen_random_uuid(), ?, ?, ?, 3.0, true)")) {
                    ps.setObject(1, farmerId);
                    ps.setObject(2, villageId);
                    ps.setString(3, "  " + testLabel.toUpperCase() + "  "); // whitespace and uppercase variation
                    ps.executeUpdate();
                } catch (java.sql.SQLException e) {
                    dupLabelRejected = true;
                    System.out.println("Duplicate CI label correctly rejected by ux_plots_farmer_label_ci: " + e.getMessage());
                }
                assertThat(dupLabelRejected).isTrue();

                // 6. Test Optimistic Concurrency Update with valid version
                Thread.sleep(100);
                String updatedVersion = null;
                try (java.sql.PreparedStatement ps = conn.prepareStatement(
                        "UPDATE public.plots SET label = ?, total_area_acres = 6.0000, updated_at = now() " +
                        "WHERE id = ? AND farmer_id = ? AND updated_at = CAST(? AS timestamptz) " +
                        "RETURNING to_char(updated_at, 'YYYY-MM-DD\"T\"HH24:MI:SS.US\"Z\"')")) {
                    ps.setString(1, testLabel + "_Updated");
                    ps.setObject(2, plotId);
                    ps.setObject(3, farmerId);
                    ps.setString(4, initialVersion);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertThat(rs.next()).isTrue();
                        updatedVersion = rs.getString(1);
                        System.out.printf("Optimistic update succeeded! Old version: %s, New version: %s%n",
                                initialVersion, updatedVersion);
                    }
                }
                assertThat(updatedVersion).isNotEqualTo(initialVersion);

                // 7. Test Stale Version Update (must update 0 rows)
                try (java.sql.PreparedStatement ps = conn.prepareStatement(
                        "UPDATE public.plots SET label = 'Stale' " +
                        "WHERE id = ? AND farmer_id = ? AND updated_at = CAST(? AS timestamptz)")) {
                    ps.setObject(1, plotId);
                    ps.setObject(2, farmerId);
                    ps.setString(3, initialVersion); // using old initial version!
                    int rows = ps.executeUpdate();
                    assertThat(rows).isEqualTo(0);
                    System.out.println("Stale version update correctly affected 0 rows.");
                }

                // 8. Test Soft Deactivate
                try (java.sql.PreparedStatement ps = conn.prepareStatement(
                        "UPDATE public.plots SET is_active = false, updated_at = now() " +
                        "WHERE id = ? AND farmer_id = ? AND is_active = true")) {
                    ps.setObject(1, plotId);
                    ps.setObject(2, farmerId);
                    int rows = ps.executeUpdate();
                    assertThat(rows).isEqualTo(1);
                    System.out.println("Plot soft deactivation succeeded (1 row updated).");
                }

                // 9. Second Deactivation attempt on already inactive plot (must update 0 rows)
                try (java.sql.PreparedStatement ps = conn.prepareStatement(
                        "UPDATE public.plots SET is_active = false, updated_at = now() " +
                        "WHERE id = ? AND farmer_id = ? AND is_active = true")) {
                    ps.setObject(1, plotId);
                    ps.setObject(2, farmerId);
                    int rows = ps.executeUpdate();
                    assertThat(rows).isEqualTo(0);
                    System.out.println("Already-inactive plot deactivation correctly affected 0 rows.");
                }

                // 10. Reactivation
                try (java.sql.PreparedStatement ps = conn.prepareStatement(
                        "UPDATE public.plots SET is_active = true, updated_at = now() " +
                        "WHERE id = ? AND farmer_id = ? AND is_active = false")) {
                    ps.setObject(1, plotId);
                    ps.setObject(2, farmerId);
                    int rows = ps.executeUpdate();
                    assertThat(rows).isEqualTo(1);
                    System.out.println("Plot reactivation succeeded (1 row updated).");
                }

                // 11. IDOR protection check at DB level
                java.util.UUID otherFarmerId = java.util.UUID.randomUUID();
                try (java.sql.PreparedStatement ps = conn.prepareStatement(
                        "SELECT id FROM public.plots WHERE id = ? AND farmer_id = ?")) {
                    ps.setObject(1, plotId);
                    ps.setObject(2, otherFarmerId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertThat(rs.next()).isFalse();
                        System.out.println("IDOR query with foreign farmer returned 0 rows as expected.");
                    }
                }
            } finally {
                // Completely clean up test records from live DB
                if (plotId != null) {
                    try (Statement s = conn.createStatement()) {
                        s.execute("DELETE FROM public.plots WHERE id = '" + plotId + "'");
                    } catch (Exception ignored) {}
                }
                if (farmerId != null) {
                    try (Statement s = conn.createStatement()) {
                        s.execute("DELETE FROM public.idempotency_keys WHERE farmer_id = '" + farmerId + "'");
                        s.execute("DELETE FROM public.farmer_profiles WHERE id = '" + farmerId + "'");
                        s.execute("DELETE FROM auth.users WHERE id = '" + farmerId + "'");
                    } catch (Exception ignored) {}
                }
                System.out.println("Live test complete — test data cleanly removed from Supabase.");
            }
        }
    }
}
