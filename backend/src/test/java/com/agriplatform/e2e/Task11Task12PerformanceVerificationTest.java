package com.agriplatform.e2e;

import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Task 11 + Task 12 — Real Performance & Concurrency Latency Verification.
 *
 * <p>Executes actual read and aggregate queries directly against the live Supabase
 * PostgreSQL database pooler over WAN to measure real-world p50 and p95 latencies for:
 * <ul>
 *   <li>1. Geography Lookups (States, Districts, Mandals, Villages)</li>
 *   <li>2. Farmer Profile Retrieval</li>
 *   <li>3. Plot Listing</li>
 *   <li>4. Cultivation Intention Listing</li>
 *   <li>5. Cultivation History & Status Events</li>
 *   <li>6. Regional Geography Analytics Summary</li>
 *   <li>7. Crop Distribution Analytics</li>
 *   <li>8. Notification Unread Partial-Index Lookup</li>
 * </ul>
 */
@DisplayName("Task 11 & 12 — Performance & Latency Verification")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Task11Task12PerformanceVerificationTest {

    private static Connection conn;
    private static boolean skipAll = false;

    private static UUID sampleStateId;
    private static UUID sampleDistrictId;
    private static UUID sampleMandalId;

    private static Connection getConnection() throws Exception {
        String url = System.getenv("SUPABASE_DB_URL");
        String username = System.getenv("SUPABASE_DB_USERNAME");
        String password = System.getenv("SUPABASE_DB_PASSWORD");
        if (url == null || username == null || password == null) return null;
        return DriverManager.getConnection(url, username, password);
    }

    @BeforeAll
    static void setup() throws Exception {
        conn = getConnection();
        if (conn == null) {
            System.out.println("[SKIP] Supabase credentials not available — benchmark skipped.");
            skipAll = true;
            return;
        }

        // Resolve reference IDs
        try (var s = conn.createStatement()) {
            try (ResultSet rs = s.executeQuery("SELECT id FROM public.states WHERE code = '32'")) {
                if (rs.next()) sampleStateId = (UUID) rs.getObject("id");
            }
            try (ResultSet rs = s.executeQuery("SELECT id FROM public.districts WHERE state_id = '" + sampleStateId + "' LIMIT 1")) {
                if (rs.next()) sampleDistrictId = (UUID) rs.getObject("id");
            }
            try (ResultSet rs = s.executeQuery("SELECT id FROM public.mandals WHERE district_id = '" + sampleDistrictId + "' LIMIT 1")) {
                if (rs.next()) sampleMandalId = (UUID) rs.getObject("id");
            }
        }
    }

    @AfterAll
    static void teardown() throws Exception {
        if (conn != null) conn.close();
    }

    private record LatencyResult(String operation, double p50Ms, double p95Ms, double minMs, double maxMs, int samples) {}

    private LatencyResult measureQuery(String opName, String sql, Object... params) throws Exception {
        List<Double> timings = new ArrayList<>();
        // Warm-up query
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {}
            }
        }

        // Measure 20 samples
        int iterations = 20;
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (int p = 0; p < params.length; p++) ps.setObject(p + 1, params[p]);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {}
                }
            }
            long end = System.nanoTime();
            timings.add((end - start) / 1_000_000.0);
        }

        Collections.sort(timings);
        double p50 = timings.get((int) (iterations * 0.50));
        double p95 = timings.get((int) (iterations * 0.95));
        double min = timings.get(0);
        double max = timings.get(iterations - 1);

        System.out.printf("[BENCHMARK] %-40s | p50: %6.2f ms | p95: %6.2f ms | min: %6.2f ms | max: %6.2f ms (N=%d)%n",
                opName, p50, p95, min, max, iterations);

        return new LatencyResult(opName, p50, p95, min, max, iterations);
    }

    @Test @Order(1)
    @DisplayName("BENCHMARK 1: Geography Reference Lookups")
    void benchmarkGeographyLookups() throws Exception {
        assumeTrue(!skipAll);
        System.out.println("================================================================================");
        System.out.println("TASK 11 & 12 PERFORMANCE BENCHMARK SUITE — LIVE SUPABASE DATABASE");
        System.out.println("================================================================================");

        measureQuery("Geography: Active States", "SELECT id, name, code FROM public.states WHERE is_active = true ORDER BY name");
        measureQuery("Geography: Districts for State", "SELECT id, name, code FROM public.districts WHERE state_id = ? AND is_active = true ORDER BY name", sampleStateId);
        measureQuery("Geography: Mandals for District", "SELECT id, name, code FROM public.mandals WHERE district_id = ? AND is_active = true ORDER BY name", sampleDistrictId);
        measureQuery("Geography: Villages for Mandal", "SELECT id, name, code FROM public.villages WHERE mandal_id = ? AND is_active = true ORDER BY name", sampleMandalId);
    }

    @Test @Order(2)
    @DisplayName("BENCHMARK 2: Domain Entity Lookups (Farmer, Plot, Cultivation, History)")
    void benchmarkDomainLookups() throws Exception {
        assumeTrue(!skipAll);
        UUID dummyId = UUID.randomUUID();

        measureQuery("Farmer: Profile by ID", "SELECT * FROM public.farmer_profiles WHERE id = ?", dummyId);
        measureQuery("Plot: Active Plots by Farmer", "SELECT * FROM public.plots WHERE farmer_id = ? AND is_active = true ORDER BY created_at DESC LIMIT 20", dummyId);
        measureQuery("Cultivation: Intentions by Farmer", "SELECT * FROM public.cultivation_intentions WHERE farmer_id = ? ORDER BY created_at DESC LIMIT 20", dummyId);
        measureQuery("History: Snapshots by Intention", "SELECT * FROM public.cultivation_intention_history WHERE intention_id = ? ORDER BY version DESC LIMIT 20", dummyId);
        measureQuery("History: Status Events by Intention", "SELECT * FROM public.cultivation_status_events WHERE intention_id = ? ORDER BY changed_at ASC LIMIT 20", dummyId);
    }

    @Test @Order(3)
    @DisplayName("BENCHMARK 3: Analytics Aggregations & Rollups")
    void benchmarkAnalyticsAggregations() throws Exception {
        assumeTrue(!skipAll);
        UUID dummyFarmerId = UUID.randomUUID();

        measureQuery("Analytics: Regional Geographic Summary",
                "SELECT count(DISTINCT ci.farmer_id) AS active_farmers, " +
                "coalesce(sum(ci.cultivated_area_acres), 0) AS total_active_area, " +
                "count(ci.id) AS active_cycles " +
                "FROM public.cultivation_intentions ci " +
                "JOIN public.plots p ON ci.plot_id = p.id " +
                "JOIN public.villages v ON p.village_id = v.id " +
                "JOIN public.mandals m ON v.mandal_id = m.id " +
                "JOIN public.districts d ON m.district_id = d.id " +
                "WHERE d.state_id = ? AND ci.status IN ('PLANNED', 'STARTED')", sampleStateId);

        measureQuery("Analytics: Farmer Personal Summary",
                "SELECT count(p.id) AS total_plots, coalesce(sum(p.total_area_acres), 0) AS total_capacity, " +
                "coalesce(sum(CASE WHEN ci.status IN ('PLANNED', 'STARTED') THEN ci.cultivated_area_acres ELSE 0 END), 0) AS active_cultivated " +
                "FROM public.plots p " +
                "LEFT JOIN public.cultivation_intentions ci ON p.id = ci.plot_id AND p.farmer_id = ci.farmer_id " +
                "WHERE p.farmer_id = ? AND p.is_active = true", dummyFarmerId);
    }

    @Test @Order(4)
    @DisplayName("BENCHMARK 4: Notification Partial Index Performance")
    void benchmarkNotificationPartialIndex() throws Exception {
        assumeTrue(!skipAll);
        UUID dummyFarmerId = UUID.randomUUID();

        LatencyResult result = measureQuery("Notification: Unread Count (Partial Index)",
                "SELECT count(*) FROM public.notifications WHERE farmer_id = ? AND read_at IS NULL", dummyFarmerId);

        assertThat(result.p95Ms())
                .as("Unread notification partial index lookup must be sub-second over WAN")
                .isLessThan(1000.0);

        System.out.println("================================================================================");
        System.out.println("BENCHMARK SUITE COMPLETED SUCCESSFULLY");
        System.out.println("================================================================================");
    }
}
