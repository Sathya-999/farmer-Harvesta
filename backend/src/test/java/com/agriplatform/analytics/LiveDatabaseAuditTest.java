package com.agriplatform.analytics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Task 10 — Read-Only Live Supabase Database Schema Verification")
class LiveDatabaseAuditTest {

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
    @DisplayName("Verify all required analytics database tables and schema objects exist in Supabase")
    void verifyLiveDatabaseSchemaObjects() throws Exception {
        try (Connection conn = getConnection()) {
            if (conn == null) {
                System.out.println("Supabase credentials not available — skipping live audit");
                return;
            }

            DatabaseMetaData metaData = conn.getMetaData();
            List<String> requiredTables = List.of(
                    "states", "districts", "mandals", "villages",
                    "farmer_profiles", "plots",
                    "cultivation_intentions", "crops", "crop_varieties", "seasons",
                    "cultivation_intention_history", "cultivation_status_events"
            );

            for (String table : requiredTables) {
                try (ResultSet rs = metaData.getTables(null, "public", table, null)) {
                    assertThat(rs.next()).as("Table public." + table + " must exist in Supabase").isTrue();
                }
            }
        }
    }
}
