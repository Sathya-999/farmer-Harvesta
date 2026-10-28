package com.agriplatform.geography;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Supabase Live Database Verification Test")
class SupabaseLiveVerificationTest {

    @Test
    @DisplayName("Verify live Supabase connection and geography tables")
    void verifyLiveSupabaseConnection() throws Exception {
        String url = System.getenv("SUPABASE_DB_URL");
        String username = System.getenv("SUPABASE_DB_USERNAME");
        String password = System.getenv("SUPABASE_DB_PASSWORD");

        if (url == null || username == null || password == null) {
            System.out.println("Supabase credentials not in environment — skipping live test");
            return;
        }

        System.out.println("Connecting to live Supabase: " + url.replaceAll(":[^:@]+@", ":***@"));
        try (Connection conn = DriverManager.getConnection(url, username, password);
             Statement stmt = conn.createStatement()) {

            assertThat(conn.isValid(10)).isTrue();
            System.out.println("Connected successfully to Supabase PostgreSQL!");

            // Check tables exist
            String[] tables = {"states", "districts", "mandals", "villages"};
            for (String table : tables) {
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT column_name, data_type, is_nullable " +
                        "FROM information_schema.columns " +
                        "WHERE table_schema = 'public' AND table_name = '" + table + "' " +
                        "ORDER BY ordinal_position")) {

                    System.out.println("\n--- Table: public." + table + " ---");
                    boolean hasCols = false;
                    while (rs.next()) {
                        hasCols = true;
                        System.out.println("  " + rs.getString("column_name") + " : " +
                                rs.getString("data_type") + " (nullable: " + rs.getString("is_nullable") + ")");
                    }
                    assertThat(hasCols).as("Table public." + table + " must exist").isTrue();
                }

                // Count records
                try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM public." + table)) {
                    if (rs.next()) {
                        System.out.println("  Row count: " + rs.getLong(1));
                    }
                }
            }
        }
    }
}
