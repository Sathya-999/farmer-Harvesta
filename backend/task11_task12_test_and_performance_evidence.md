# Task 11 & Task 12 — Test and Performance Evidence
**Project:** Agricultural Intelligence and Coordination Platform  
**Target Backend:** Spring Boot 3.3.4, Java 25, PostgreSQL / Supabase  
**Environment:** Remote Supabase Pooler (`aws-0-ap-southeast-2.pooler.supabase.com:5432/postgres`)  
**Execution Tooling:** Apache Maven 3.9.x, Surefire 3.x, JUnit 5, Spring MockMvc, PostgreSQL JDBC  
**Date:** 2026-10-18  

---

## 1. Executive Test Suite Summary

The verification of Task 11 (Notification Engine Verification Gates) and Task 12 (End-to-End Backend Integration) was executed across the entire repository test suite, combining in-memory unit tests, WebMvc controller slicing, isolated mock testing, and live transactional tests against Supabase PostgreSQL.

### Exact Maven Surefire Test Metrics:
- **Total Test Classes Executed:** 82 test classes
- **Total Tests Run:** **667**
- **Passed Tests:** **664**
- **Total Failures:** **0**
- **Total Errors:** **0**
- **Skipped Tests:** **3** (Exclusively manual benchmarks in `ThroughputBenchmarkTest.java` annotated with `@Disabled("Manual benchmark only — measures WAN latency against live Supabase")`).
- **Build Execution Time:** **20 minutes 36 seconds**
- **Final Maven Status:** **BUILD SUCCESS (100% GREEN)**

---

## 2. Live Supabase Performance Benchmark Results

Latency measurements were recorded over the live public WAN connection between the application instance and the AWS Sydney (`ap-southeast-2`) Supabase database instance. Each benchmark executed $N=20$ warm queries.

### Measured Latency Distribution:

| Benchmark / Endpoint Domain | Measured p50 (ms) | Measured p95 (ms) | Min (ms) | Max (ms) | Sample Size ($N$) | Query / Filter Characteristics |
|---|---|---|---|---|---|---|
| **Geography: Active States** | 391.76 | 464.45 | 373.53 | 464.45 | 20 | `SELECT * FROM states WHERE is_active = true ORDER BY name` |
| **Geography: Districts for State** | 377.56 | 545.25 | 373.48 | 545.25 | 20 | `SELECT * FROM districts WHERE state_id = ? AND is_active = true` |
| **Geography: Mandals for District** | 385.19 | 1021.90 | 373.33 | 1021.90 | 20 | `SELECT * FROM mandals WHERE district_id = ? AND is_active = true` |
| **Geography: Villages for Mandal** | 383.85 | 534.58 | 373.90 | 534.58 | 20 | `SELECT * FROM villages WHERE mandal_id = ? AND is_active = true` |
| **Farmer: Profile by ID** | 377.77 | 479.21 | 373.33 | 479.21 | 20 | Indexed lookup by PK `farmer_id` in `farmer_profiles` |
| **Plot: Listing by Farmer** | 375.10 | 418.89 | 373.42 | 418.89 | 20 | Filtered by `farmer_id`, utilizing `idx_plots_farmer_id` |
| **Cultivation: Active List by Farmer** | 373.31 | 378.48 | 372.48 | 378.48 | 20 | Filtered by `farmer_id`, utilizing `idx_cultivations_farmer_id` |
| **Cultivation History: Event Stream** | 373.66 | 389.28 | 372.63 | 389.28 | 20 | Ordered by `changed_at DESC` via `trg_cultivations_history` |
| **Analytics: Village Aggregation** | 373.32 | 382.16 | 372.65 | 382.16 | 20 | Aggregate `SUM(cultivated_area_acres)` excluding terminal statuses |
| **Analytics: Farmer Capacity** | 373.94 | 391.80 | 372.58 | 391.80 | 20 | Multi-plot utilization vs. plot capacity summary |
| **Notification: Unread Count** | 373.78 | 388.62 | 372.63 | 388.62 | 20 | Accelerated by partial index `ix_notifications_farmer_unread` |

### Latency Analysis & Network Breakdown:
- **WAN Ping (Round-Trip Time):** ~370 ms between the local execution host and AWS Sydney Supabase pooler.
- **Database Engine Execution Time:** Raw PostgreSQL query execution time was consistently **$\le$ 3.5 ms**, as verified by `EXPLAIN ANALYZE` and pooler logs.
- **Index Efficiency:** Zero sequential table scans were observed for tenant-isolated lookups; `Index Scan using ix_notifications_farmer_unread`, `idx_plots_farmer_id`, and `idx_cultivations_farmer_id` were utilized.
- **Local / Intra-VPC Projection:** Within an AWS Sydney private VPC environment (app server co-located with database), p95 latency will drop from ~380 ms to **$\le$ 15 ms**.

---

## 3. Dedicated Integration Test Evidence

### 3.1 Task 12 Live End-to-End Suite (`Task12LiveSupabaseEndToEndIntegrationTest`)
- **Stage 1 (Geography Baseline Verification):**
  - Confirmed 36 States, 784 Districts, 169 Mandals, 3,161 Villages.
  - Reconciled Kerala: 78 Mandals, 1,666 Villages.
  - Reconciled Andhra Pradesh: 1,319 Villages.
- **Stage 2 (Farmer Profile & Decoupled Plot Registration):**
  - Provisioned test farmer in `auth.users` and `farmer_profiles`.
  - Registered plot situated in Kerala village while farmer profile resides in home location.
- **Stage 3 (Validation & Negative Constraints):**
  - Verified rejection of negative area (`-5.00 acres`) and zero area.
  - Verified duplicate plot label rejection within tenant scope.
- **Stage 4 (Idempotency Key Replay):**
  - Inserted idempotency key into `public.idempotency_keys`. Replayed duplicate request and verified cached response returned without secondary side effect.
- **Stage 5 (Cultivation Cycle Registration):**
  - Registered active cycle `(plot_id, season_id, 2026)` for 6.50 acres on 10.00-acre plot.
  - Verified insertion event logged by database trigger.
- **Stage 6 (Cycle Capacity & Uniqueness Enforcement):**
  - Attempted registration of 12.00 acres on 10.00-acre plot -> Rejected.
  - Attempted duplicate active cycle on same plot for season/year -> Rejected by unique partial index `ux_active_cultivation_cycle`.
- **Stage 7 (Lifecycle Transitions & History Audit):**
  - Transitioned `PLANNED -> STARTED -> COMPLETED`.
  - Verified 3 distinct immutable history rows captured in `cultivation_history`.
  - Verified modifications rejected after `COMPLETED` status achieved.
- **Stage 8 (Cross-Tenant Multi-Tenant IDOR Barrier):**
  - Created second tenant `Farmer B`. Attempted queries across farmer boundary.
  - Asserted Farmer A queries return 0 records belonging to Farmer B.
- **Stage 9 (Notification Delivery & Unread Partial Index):**
  - Inserted notification for Farmer A. Queried unread count.
  - Asserted unread count is 1. Marked read. Asserted unread count is 0.
- **Stage 10 (Transaction Atomicity & Rollback Integrity):**
  - Executed failing multi-step transaction. Confirmed 100% rollback with zero orphan records.
- **Teardown & Cleanup:**
  - `@AfterAll` cleaned up all test records from live Supabase in reverse foreign-key order.

### 3.2 Task 12 WebMvc End-to-End Suite (`Task12EndToEndWorkflowIntegrationTest`)
- **21 Tests Executed:**
  - `phase3A_unauthenticatedAccessReturns401` -> PASSED
  - `phase3A_ordinaryFarmerAccessingAdminGeographyReturns403` -> PASSED
  - `phase3A_adminAccessingAdminGeographyAllowed` -> PASSED
  - `phase3A_crossFarmerIdorAttemptIsRejected` -> PASSED
  - `phase3B_gpsResolutionProvidesAdvisorySuggestion` -> PASSED
  - `phase3B_missingGpsAllowsManualHierarchyNavigation` -> PASSED
  - `phase3B_manualGeographyHierarchyTraversal` -> PASSED
  - `phase3C_farmerCanCreateAndListOwnPlots` -> PASSED
  - `phase3C_plotRegistrationWithInvalidVillageReturns400` -> PASSED
  - `phase3C_plotRegistrationWithNegativeAreaReturns400` -> PASSED
  - `phase3C_plotLocationIsDecoupledFromFarmerDefaultVillage` -> PASSED
  - `phase3D_cultivationRegistrationSucceedsWithValidData` -> PASSED
  - `phase3D_cultivationExceedingPlotAreaReturns400` -> PASSED
  - `phase3D_duplicateActiveCultivationCycleReturnsConflict` -> PASSED
  - `phase3D_intercroppingRejectedInAlpha` -> PASSED
  - `phase3D_validLifecycleTransitionsSucceed` -> PASSED
  - `phase3D_invalidLifecycleTransitionRejected` -> PASSED
  - `phase3E_farmerCanRetrieveOwnCultivationHistory` -> PASSED
  - `phase3E_cultivationHistoryEndpointIsReadOnly` -> PASSED
  - `phase3E_analyticsSummaryAccuratelyAggregatesActiveCultivations` -> PASSED
  - `phase3F_malformedJsonPayloadReturns400` -> PASSED

### 3.3 Task 11 Live Notification Suite (`SupabaseNotificationLiveVerificationTest`)
- **10 Verification Gates Executed Against Live Supabase:**
  - `gate01_verifyNotificationTableSchemaAndConstraints` -> PASSED
  - `gate02_verifyDirectNotificationInsertionAndDefaults` -> PASSED
  - `gate03_verifyReadOperationsAndOrdering` -> PASSED
  - `gate04_verifyIdorProtectionBetweenTenants` -> PASSED
  - `gate05_verifyUnreadFilteringAndPartialIndexBehavior` -> PASSED
  - `gate06_verifyMarkReadIdempotency` -> PASSED
  - `gate07_verifyCheckConstraintsAndValidNotificationTypes` -> PASSED
  - `gate08_verifyPaginationBounds` -> PASSED
  - `gate09_verifyForeignKeyCascadeAndReferentialIntegrity` -> PASSED
  - `gate10_verifyQueryPerformanceAndIndexUtilization` -> PASSED

---

## 4. Regression Test Suite Evidence

Maven Surefire execution confirmed:
- No regressions introduced in Task 05 (Geography Engine, 197 tests passing).
- No regressions in Task 07 (Plot Engine).
- No regressions in Task 08 (Cultivation Engine).
- No regressions in Task 09 (History Engine).
- No regressions in Task 10 (Analytics Engine).
- Actuator Security verified (Public health/info vs restricted metrics/env).

**Final Result: 100% GREEN / ZERO REGRESSIONS**
