package com.agriplatform.cultivation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

@DisplayName("Task 08 — Phase 1 Live Supabase Cultivation Audit")
class SupabaseCultivationLiveAuditTest {

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
    @DisplayName("Audit live DB for Cultivation-related tables, constraints, triggers, and functions")
    void auditCultivationTables() throws Exception {
        try (Connection conn = getConnection()) {
            if (conn == null) {
                System.out.println("Supabase credentials not available — skipping live audit");
                return;
            }

            try (Statement stmt = conn.createStatement()) {
                List<String> tables = List.of(
                        "cultivation_intentions",
                        "seasons",
                        "cultivation_status_events",
                        "cultivation_intention_history",
                        "crops",
                        "crop_varieties",
                        "plots",
                        "farmer_profiles",
                        "villages"
                );

                for (String table : tables) {
                    System.out.println("\n==================================================");
                    System.out.printf("TABLE: public.%s%n", table);
                    System.out.println("==================================================");

                    // Columns
                    try (ResultSet rs = stmt.executeQuery(
                            "SELECT column_name, data_type, udt_name, is_nullable, column_default " +
                            "FROM information_schema.columns " +
                            "WHERE table_schema = 'public' AND table_name = '" + table + "' " +
                            "ORDER BY ordinal_position")) {
                        boolean found = false;
                        while (rs.next()) {
                            found = true;
                            System.out.printf("  %s : %s (%s), nullable=%s, default=%s%n",
                                    rs.getString("column_name"),
                                    rs.getString("data_type"),
                                    rs.getString("udt_name"),
                                    rs.getString("is_nullable"),
                                    rs.getString("column_default"));
                        }
                        if (!found) {
                            System.out.printf("  [WARNING] Table public.%s does NOT exist!%n", table);
                        }
                    }

                    // Constraints
                    System.out.printf("\n--- Constraints on public.%s ---%n", table);
                    try (ResultSet rs = stmt.executeQuery(
                            "SELECT c.conname, c.contype, pg_get_constraintdef(c.oid) " +
                            "FROM pg_constraint c " +
                            "JOIN pg_namespace n ON n.oid = c.connamespace " +
                            "WHERE n.nspname = 'public' AND c.conrelid = 'public." + table + "'::regclass")) {
                        while (rs.next()) {
                            System.out.printf("  Constraint: %s (type %s) -> %s%n",
                                    rs.getString(1), rs.getString(2), rs.getString(3));
                        }
                    } catch (Exception e) {
                        System.out.printf("  (Could not fetch constraints: %s)%n", e.getMessage());
                    }

                    // Indexes
                    System.out.printf("\n--- Indexes on public.%s ---%n", table);
                    try (ResultSet rs = stmt.executeQuery(
                            "SELECT indexname, indexdef FROM pg_indexes WHERE schemaname = 'public' AND tablename = '" + table + "'")) {
                        while (rs.next()) {
                            System.out.printf("  Index: %s -> %s%n", rs.getString(1), rs.getString(2));
                        }
                    } catch (Exception e) {
                        System.out.printf("  (Could not fetch indexes: %s)%n", e.getMessage());
                    }

                    // Triggers
                    System.out.printf("\n--- Triggers on public.%s ---%n", table);
                    try (ResultSet rs = stmt.executeQuery(
                            "SELECT tgname, pg_get_triggerdef(oid) FROM pg_trigger " +
                            "WHERE tgrelid = 'public." + table + "'::regclass AND NOT tgisinternal")) {
                        while (rs.next()) {
                            System.out.printf("  Trigger: %s -> %s%n", rs.getString(1), rs.getString(2));
                        }
                    } catch (Exception e) {
                        System.out.printf("  (Could not fetch triggers: %s)%n", e.getMessage());
                    }

                    // Sample row count
                    try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM public." + table)) {
                        if (rs.next()) {
                            System.out.printf("  Current Row Count: %d%n", rs.getInt(1));
                        }
                    } catch (Exception e) {
                        System.out.printf("  (Could not count rows: %s)%n", e.getMessage());
                    }
                }

                // Trigger functions in private schema: current_actor_id, current_actor_type
                System.out.println("\n==================================================");
                System.out.println("ACTOR FUNCTIONS");
                System.out.println("==================================================");
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT n.nspname, p.proname, p.prosrc " +
                        "FROM pg_proc p " +
                        "JOIN pg_namespace n ON n.oid = p.pronamespace " +
                        "WHERE p.proname IN ('current_actor_id', 'current_actor_type', 'set_actor_context', 'validate_intention_write', 'record_intention_history', 'record_status_event', 'notify_admin_intention_change') " +
                        "ORDER BY n.nspname, p.proname")) {
                    while (rs.next()) {
                        System.out.printf("\nFunction: %s.%s%nDefinition:%n%s%n",
                                rs.getString(1), rs.getString(2), rs.getString(3));
                    }
                }

                // Provision reference data (idempotent, ON CONFLICT DO NOTHING)
                System.out.println("\n==================================================");
                System.out.println("SEEDING REFERENCE DATA IF EMPTY");
                System.out.println("==================================================");
                stmt.executeUpdate(
                    "INSERT INTO public.seasons (name, anchor_month, anchor_day, description, is_active) VALUES " +
                    "('Kharif', 6, 1, 'Monsoon season crop cycle (June to October)', true), " +
                    "('Rabi', 10, 15, 'Winter season crop cycle (October to March)', true), " +
                    "('Zaid', 3, 1, 'Summer season crop cycle (March to June)', true) " +
                    "ON CONFLICT (lower(btrim(name))) DO NOTHING"
                );

                stmt.executeUpdate(
                    "INSERT INTO public.crops (name, category, is_active) VALUES " +
                    "('Paddy (Rice)', 'Cereals', true), " +
                    "('Cotton', 'Fiber Crops', true), " +
                    "('Wheat', 'Cereals', true), " +
                    "('Maize (Corn)', 'Cereals', true), " +
                    "('Soybean', 'Oilseeds', true), " +
                    "('Chilli', 'Spices', true) " +
                    "ON CONFLICT (lower(btrim(name))) DO NOTHING"
                );

                stmt.executeUpdate(
                    "INSERT INTO public.crop_varieties (crop_id, name, is_active) " +
                    "SELECT c.id, v.variety_name, true " +
                    "FROM public.crops c " +
                    "CROSS JOIN (VALUES " +
                    "  ('Paddy (Rice)', 'Basmati 370'), " +
                    "  ('Paddy (Rice)', 'BPT 5204 (Samba Mahsuri)'), " +
                    "  ('Cotton', 'Bt Cotton Bolgard II'), " +
                    "  ('Cotton', 'DCH-32'), " +
                    "  ('Wheat', 'Sharbati'), " +
                    "  ('Maize (Corn)', 'DHM 117') " +
                    ") AS v(crop_name, variety_name) " +
                    "WHERE c.name = v.crop_name " +
                    "ON CONFLICT (crop_id, lower(btrim(name))) DO NOTHING"
                );

                // Reference data in crops
                System.out.println("\n==================================================");
                System.out.println("REFERENCE DATA: public.crops (all)");
                System.out.println("==================================================");
                try (ResultSet rs = stmt.executeQuery("SELECT id, name, category, is_active FROM public.crops")) {
                    while (rs.next()) {
                        System.out.printf("  Crop: id=%s, name=%s, category=%s, is_active=%s%n",
                                rs.getObject(1), rs.getString(2), rs.getString(3), rs.getBoolean(4));
                    }
                } catch (Exception e) {
                    System.out.printf("  (Could not fetch crops: %s)%n", e.getMessage());
                }

                // Reference data in crop_varieties
                System.out.println("\n==================================================");
                System.out.println("REFERENCE DATA: public.crop_varieties (all)");
                System.out.println("==================================================");
                try (ResultSet rs = stmt.executeQuery("SELECT id, crop_id, name, is_active FROM public.crop_varieties")) {
                    while (rs.next()) {
                        System.out.printf("  Variety: id=%s, crop_id=%s, name=%s, is_active=%s%n",
                                rs.getObject(1), rs.getObject(2), rs.getString(3), rs.getBoolean(4));
                    }
                } catch (Exception e) {
                    System.out.printf("  (Could not fetch crop_varieties: %s)%n", e.getMessage());
                }

                System.out.println("\n==================================================");
                System.out.println("REFERENCE DATA: public.seasons (all)");
                System.out.println("==================================================");
                try (ResultSet rs = stmt.executeQuery("SELECT id, name, anchor_month, anchor_day, is_active FROM public.seasons")) {
                    while (rs.next()) {
                        System.out.printf("  Season: id=%s, name=%s, anchor_month=%s, anchor_day=%s, is_active=%s%n",
                                rs.getObject(1), rs.getString(2), rs.getObject(3), rs.getObject(4), rs.getBoolean(5));
                    }
                } catch (Exception e) {
                    System.out.printf("  (Could not fetch seasons: %s)%n", e.getMessage());
                }

                // Verify actor context functions within a transaction
                System.out.println("\n==================================================");
                System.out.println("VERIFYING ACTOR CONTEXT BEHAVIOR");
                System.out.println("==================================================");
                conn.setAutoCommit(false);
                UUID testActorId = UUID.randomUUID();
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT set_config('app.actor_id', '" + testActorId + "', true), " +
                        "       set_config('app.actor_type', 'FARMER', true)")) {
                    if (rs.next()) {
                        System.out.printf("  Config set: actor_id=%s, actor_type=%s%n", rs.getString(1), rs.getString(2));
                    }
                }
                try (ResultSet rs = stmt.executeQuery("SELECT private.current_actor_id(), private.current_actor_type()")) {
                    if (rs.next()) {
                        System.out.printf("  Actor resolved: id=%s, type=%s%n", rs.getObject(1), rs.getString(2));
                        org.assertj.core.api.Assertions.assertThat(rs.getObject(1)).isEqualTo(testActorId);
                        org.assertj.core.api.Assertions.assertThat(rs.getString(2)).isEqualTo("FARMER");
                        System.out.println("  --> ACTOR CONTEXT TEST PASSED!");
                    }
                }
                conn.rollback();
            }
        }
    }
}
