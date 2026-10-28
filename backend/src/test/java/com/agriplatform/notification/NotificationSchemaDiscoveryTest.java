package com.agriplatform.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Task 11 ? Read-only live Supabase schema discovery for notification-related tables.
 * No data is inserted, modified, or deleted.
 */
@DisplayName("Task 11 ? Notification Schema Discovery in Live Supabase")
class NotificationSchemaDiscoveryTest {

    private Connection getConnection() throws Exception {
        String url      = System.getenv("SUPABASE_DB_URL");
        String username = System.getenv("SUPABASE_DB_USERNAME");
        String password = System.getenv("SUPABASE_DB_PASSWORD");
        if (url == null || username == null || password == null) return null;
        return DriverManager.getConnection(url, username, password);
    }

    @Test
    @DisplayName("Discover all notification-related tables, columns, constraints, indexes, and triggers in Supabase")
    void discoverNotificationSchema() throws Exception {
        try (Connection conn = getConnection()) {
            if (conn == null) {
                System.out.println("Supabase credentials not available ? skipping live schema discovery");
                return;
            }
            try (Statement stmt = conn.createStatement()) {

                System.out.println("\n========================================");
                System.out.println("STEP 1: Tables containing 'notification'");
                System.out.println("========================================");
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT table_schema, table_name FROM information_schema.tables " +
                        "WHERE table_name ILIKE '%notification%' ORDER BY table_schema, table_name")) {
                    boolean found = false;
                    while (rs.next()) {
                        System.out.printf("  %s.%s%n", rs.getString("table_schema"), rs.getString("table_name"));
                        found = true;
                    }
                    if (!found) System.out.println("  [NO notification tables found in any schema]");
                }

                String[] candidates = {"farmer_notifications","notifications","user_notifications","platform_notifications"};
                for (String table : candidates) {
                    System.out.printf("%n========================================%nCOLUMNS: public.%s%n========================================%n", table);
                    try (ResultSet rs = stmt.executeQuery(
                            "SELECT column_name, udt_name, is_nullable, column_default " +
                            "FROM information_schema.columns " +
                            "WHERE table_schema='public' AND table_name='" + table + "' ORDER BY ordinal_position")) {
                        boolean found = false;
                        while (rs.next()) {
                            System.out.printf("  %-30s | %-20s | nullable=%-3s | default=%s%n",
                                    rs.getString("column_name"), rs.getString("udt_name"),
                                    rs.getString("is_nullable"), rs.getString("column_default"));
                            found = true;
                        }
                        if (!found) System.out.printf("  [public.%s does not exist]%n", table);
                    }
                    try (ResultSet rs = stmt.executeQuery(
                            "SELECT c.conname, c.contype, pg_get_constraintdef(c.oid) AS def " +
                            "FROM pg_constraint c JOIN pg_class t ON t.oid=c.conrelid " +
                            "JOIN pg_namespace n ON n.oid=t.relnamespace " +
                            "WHERE n.nspname='public' AND t.relname='" + table + "' ORDER BY c.contype,c.conname")) {
                        while (rs.next())
                            System.out.printf("  CONSTRAINT %-40s type=%s : %s%n",
                                    rs.getString("conname"), rs.getString("contype"), rs.getString("def"));
                    }
                    try (ResultSet rs = stmt.executeQuery(
                            "SELECT indexname, indexdef FROM pg_indexes " +
                            "WHERE schemaname='public' AND tablename='" + table + "' ORDER BY indexname")) {
                        while (rs.next())
                            System.out.printf("  INDEX %-40s : %s%n", rs.getString("indexname"), rs.getString("indexdef"));
                    }
                    try (ResultSet rs = stmt.executeQuery(
                            "SELECT trigger_name, event_manipulation, action_timing FROM information_schema.triggers " +
                            "WHERE event_object_schema='public' AND event_object_table='" + table + "' ORDER BY trigger_name")) {
                        while (rs.next())
                            System.out.printf("  TRIGGER %-30s %s %s%n",
                                    rs.getString("trigger_name"), rs.getString("action_timing"), rs.getString("event_manipulation"));
                    }
                }

                System.out.println("\n========================================");
                System.out.println("STEP 3: Notification-related enum types");
                System.out.println("========================================");
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT t.typname, e.enumlabel FROM pg_type t JOIN pg_enum e ON e.enumtypid=t.oid " +
                        "WHERE t.typname ILIKE '%notification%' ORDER BY t.typname, e.enumsortorder")) {
                    boolean found = false;
                    while (rs.next()) {
                        System.out.printf("  %s: %s%n", rs.getString("typname"), rs.getString("enumlabel"));
                        found = true;
                    }
                    if (!found) System.out.println("  [No notification enum types found]");
                }

                System.out.println("\n========================================");
                System.out.println("STEP 4: FK references involving notification tables");
                System.out.println("========================================");
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT tc.table_name, kcu.column_name, ccu.table_name AS ftable, ccu.column_name AS fcol " +
                        "FROM information_schema.table_constraints tc " +
                        "JOIN information_schema.key_column_usage kcu ON kcu.constraint_name=tc.constraint_name " +
                        "JOIN information_schema.constraint_column_usage ccu ON ccu.constraint_name=tc.constraint_name " +
                        "WHERE tc.constraint_type='FOREIGN KEY' AND tc.table_schema='public' " +
                        "AND (tc.table_name ILIKE '%notification%' OR ccu.table_name ILIKE '%notification%') " +
                        "ORDER BY tc.table_name, kcu.column_name")) {
                    boolean found = false;
                    while (rs.next()) {
                        System.out.printf("  %s.%s -> %s.%s%n",
                                rs.getString("table_name"), rs.getString("column_name"),
                                rs.getString("ftable"), rs.getString("fcol"));
                        found = true;
                    }
                    if (!found) System.out.println("  [No FK relationships involving notification tables]");
                }

                System.out.println("\n========================================");
                System.out.println("SCHEMA DISCOVERY COMPLETE");
                System.out.println("========================================");
            }
        }
    }
}
