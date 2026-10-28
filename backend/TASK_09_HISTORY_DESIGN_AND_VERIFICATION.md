# TASK 09 — HISTORY AND AUDIT TRAIL DESIGN AND VERIFICATION

**Project:** Agricultural Intelligence Platform Backend (`G:\farmer\agri-platform-backend`)  
**Task:** Task 09 — History & Audit Trail  
**Environment:** Production Supabase PostgreSQL (`aws-0-ap-southeast-2.pooler.supabase.com:5432`)  
**Technology Stack:** Java 21 / JVM 25, Spring Boot 3.3.4, Spring Data JPA, PostgreSQL 15, Spring Security 6  
**Final Certification Gate:** `[GREEN — TASK 09 CERTIFIED]`  
**Date:** October 4, 2026

---

## 1. Executive Summary

Task 09 (History & Audit Trail) delivers an immutable, enterprise-grade audit trail and historical tracking system for the agricultural intelligence platform. Built directly on top of the certified Task 08 (Cultivation Management), Task 07 (Plot Management), Task 06 (Farmer Profile), and Task 05 (Geography), Task 09 guarantees that every modification to a cultivation record is permanently, accurately, and immutably recorded.

### Core Architecture Principles:
1. **The System Cannot Lie:** History accurately and permanently records:
   - **WHO** changed it (`actor_id`, `actor_type`, `changed_by`)
   - **WHAT** changed (`changed_fields`, `change_type`, snapshot of prior state)
   - **WHEN** it changed (`snapshot_at`, `changed_at` in high-precision UTC)
   - **WHAT** was the previous state (prior version snapshot)
   - **WHAT** is the new state (current cultivation state and transition destination)
   - **WAS** the change actually committed (strict transactional atomicity; rollback leaves zero phantom history)
   - **CAN** anyone tamper with it (database-level trigger population, read-only REST API returning 405 on mutations)
   - **CAN** cross-tenant access occur (strict IDOR isolation returning HTTP 404)

2. **Database-Enforced History Generation:** History records are **never** trusted to the client or written by application business logic. PostgreSQL trigger functions (`private.record_intention_history`, `private.record_status_event_insert`, `private.record_status_event_update`) automatically generate audit records upon cultivation modifications.

3. **Zero Mutation Exposure:** The History REST API is strictly read-only (`GET` endpoints only). Any `POST`, `PUT`, `DELETE`, or `PATCH` request is rejected at the HTTP layer with `405 Method Not Allowed`.

---

## 2. Domain Model & Separation of Concerns

```
FARMER (auth.users → farmer_profiles)
  └── owns PLOT (public.plots)
        └── hosts CULTIVATION INTENTION (public.cultivation_intentions) [CURRENT STATE]
              │
              ├──► CULTIVATION INTENTION HISTORY (public.cultivation_intention_history) [IMMUTABLE SNAPSHOTS]
              │     └── Records OLD.* state on every field update / status change
              │
              └──► CULTIVATION STATUS EVENTS (public.cultivation_status_events) [IMMUTABLE LIFECYCLE EVENTS]
                    └── Records every lifecycle transition (from_status → to_status)
```

### Invariants:
- **Farmer ≠ Plot ≠ Cultivation ≠ History**: History is not a mutable copy, not a cache, and not an analytics database.
- **Append-Only**: Once inserted by database triggers, history records cannot be modified, deleted, or reordered.
- **Tenant Scope**: Every historical inquiry is scoped to the authenticated farmer via JWT `sub`. Access across farmer boundaries returns HTTP 404 to avoid enumeration.

---

## 3. Database Architecture & Schema Reconciliation

### 3.1 Audited Tables in Live Supabase

| Table | Columns | Purpose | Population Strategy |
|---|---|---|---|
| `public.cultivation_intention_history` | 23 columns | Complete snapshot of prior cultivation state on update | Database Trigger `trg_record_intention_history` |
| `public.cultivation_status_events` | 8 columns | Lifecycle transition journal | Triggers `trg_record_status_event_insert`, `trg_record_status_event_update` |
| `public.cultivation_intentions` | 18 columns | Current state of cultivation | Application write with OCC `version` |

### 3.2 Schema Columns for `public.cultivation_intention_history`

| Column | Type | Constraints | Description |
|---|---|---|---|
| `history_id` | UUID | PRIMARY KEY, default `gen_random_uuid()` | Unique audit row identifier |
| `intention_id` | UUID | NOT NULL, FK → `cultivation_intentions(id)` | Parent cultivation record |
| `version` | INTEGER | NOT NULL | Version of the record being snapshotted (OLD.version) |
| `snapshot_at` | TIMESTAMPTZ | NOT NULL, default `now()` | Exact timestamp of snapshot capture |
| `plot_id` | UUID | NOT NULL | Plot where cultivation was planned |
| `farmer_id` | UUID | NOT NULL | Owning farmer identifier |
| `crop_id` | UUID | NOT NULL | Crop reference identifier |
| `crop_variety_id` | UUID | NULL | Variety reference identifier |
| `season_id` | UUID | NOT NULL | Season identifier (Kharif, Rabi, Zaid) |
| `cultivation_year` | SMALLINT | NOT NULL | Cultivation cycle year |
| `cultivated_area_acres` | NUMERIC(8,2) | NOT NULL | Intended area |
| `planned_planting_date`| DATE | NOT NULL | Planned sowing date |
| `actual_planting_date` | DATE | NULL | Actual sowing date (when STARTED) |
| `actual_area_acres` | NUMERIC(8,2) | NULL | Actual harvested area |
| `harvestDate` | DATE | NULL | Actual harvest date (when COMPLETED) |
| `status` | VARCHAR(20) | NOT NULL | State at snapshot time (PLANNED, STARTED, etc.) |
| `status_reason` | TEXT | NULL | Reason for status transition |
| `source` | VARCHAR(30) | NOT NULL | Origin of change (e.g. FARMER_APP) |
| `actor_id` | UUID | NULL | Actor who initiated change (from session context) |
| `actor_type` | VARCHAR(20) | NULL | Actor type (FARMER, ADMIN, SYSTEM) |
| `change_type` | VARCHAR(30) | NOT NULL | Type of modification (FIELD_UPDATE, STATUS_CHANGE) |
| `changed_fields` | TEXT[] | NULL | PostgreSQL text array of updated column names |
| `change_reason` | TEXT | NULL | Optional client/system explanation |

### 3.3 Schema Columns for `public.cultivation_status_events`

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | UUID | PRIMARY KEY, default `gen_random_uuid()` | Unique event identifier |
| `intention_id` | UUID | NOT NULL, FK → `cultivation_intentions(id)` | Parent cultivation record |
| `from_status` | VARCHAR(20) | NULL | Prior status (`NULL` on initial creation) |
| `to_status` | VARCHAR(20) | NOT NULL | New status reached |
| `changed_by` | UUID | NOT NULL | Actor ID who performed the transition |
| `actor_type` | VARCHAR(20) | NOT NULL | Actor type (`FARMER`, `ADMIN`, `SYSTEM`) |
| `changed_at` | TIMESTAMPTZ | NOT NULL, default `now()` | High-precision transition timestamp |
| `note` | TEXT | NULL | Context or reason for status change |

---

## 4. Trigger Behavior & Actor Attribution

### 4.1 Session Actor Context Propagation
The platform utilizes PostgreSQL session-level configuration parameters scoped per transaction:
```sql
SELECT set_config('app.actor_id', '<farmer-uuid>', true),
       set_config('app.actor_type', 'FARMER', true);
```
In `CultivationServiceImpl`, every write transaction sets these parameters before issuing DML. The database triggers read these variables via `current_setting('app.actor_id', true)`.

### 4.2 Trigger Invariants:
1. **Creation Event (`trg_record_status_event_insert`)**:
   - Fires `AFTER INSERT` on `cultivation_intentions`.
   - Inserts row into `cultivation_status_events` with `from_status = NULL`, `to_status = 'PLANNED'`.
   - Does **not** insert into `cultivation_intention_history` (history captures previous states upon modification).
2. **Update Event (`trg_record_status_event_update`)**:
   - Fires `AFTER UPDATE` when `OLD.status IS DISTINCT FROM NEW.status`.
   - Logs `from_status = OLD.status`, `to_status = NEW.status`.
3. **Snapshot History (`trg_record_intention_history`)**:
   - Fires `AFTER UPDATE` on `cultivation_intentions`.
   - Stores `OLD.*` row into `cultivation_intention_history`.
   - Detects updated columns and populates `changed_fields` array.
   - Sets `change_type = 'STATUS_CHANGE'` if status changed, otherwise `'FIELD_UPDATE'`.

---

## 5. Security & IDOR Prevention

1. **Authentication Enforcement**:
   - Every request requires a valid Supabase JWT Bearer token.
   - Unauthenticated requests receive HTTP 401 Unauthorized.
2. **Owner-Scoped Queries**:
   - The authenticated user ID is extracted from the JWT `sub` claim.
   - `HistoryServiceImpl` performs `requireFarmerExists(farmerId)` and `verifyOwnership(cultivationId, farmerId)`.
   - If a farmer attempts to access another farmer's cultivation history, the service throws `ResourceNotFoundException`, mapped to **HTTP 404 Not Found**.
   - No HTTP 403 Forbidden is returned, preventing enumeration attacks.
3. **Immutability Enforcement**:
   - Controller only maps `@GetMapping`.
   - Any HTTP mutation method (`POST`, `PUT`, `DELETE`, `PATCH`) returns **HTTP 405 Method Not Allowed**.

---

## 6. Pagination & Ordering

### 6.1 Field-Change Snapshots
- **Endpoint:** `GET /api/v1/history/cultivations/{cultivationId}/snapshots`
- **Ordering:** `version DESC` (newest snapshot first, showing most recent changes immediately).
- **Secondary Determinism:** `history_id` ensures stable ordering if versions match.
- **Bounding:** Page size is bounded to a maximum of 100 (`Math.min(Math.max(size, 1), 100)`).

### 6.2 Status Lifecycle Events
- **Endpoint:** `GET /api/v1/history/cultivations/{cultivationId}/events`
- **Ordering:** `changedAt ASC` (chronological sequence of lifecycle transitions: `PLANNED → STARTED → COMPLETED`).
- **Bounding:** Page size is bounded to a maximum of 100.

---

## 7. Performance & Indexing Justification

| Query Pattern | Index in Database | Justification |
|---|---|---|
| Snapshots by Intention ID ordered by Version DESC | `idx_cih_intention_version` on `cultivation_intention_history(intention_id, version DESC)` | Enables index scan for paginated snapshot queries |
| Single snapshot lookup by History ID, Intention ID, Farmer ID | `pk_cultivation_intention_history` on `(history_id)` + composite check | Direct point-lookup in O(1) time |
| Status events by Intention ID ordered by Changed At ASC | `idx_cse_intention_changed_at` on `cultivation_status_events(intention_id, changed_at ASC)` | Eliminates sorting overhead for chronological event replay |

---

## 8. Test Matrix Summary

The Task 09 suite provides 81 comprehensive automated test scenarios across 7 test classes:

| Test Class | Focus Area | Test Count |
|---|---|---|
| `HistoryControllerTest` | WebMvc, JWT auth, parameter bounds, HTTP 405 method rejection, IDOR mapping | 20 |
| `HistoryServiceTest` | Service business logic, owner verification, DTO mappings, pagination, edge cases | 23 |
| `HistoryLifecycleAndIntegrityTest` | End-to-end lifecycle history, status transitions, field updates, null safety | 14 |
| `HistorySecurityAndImmutabilityTest` | IDOR isolation, suspended farmer access, immutability, malformed input | 10 |
| `HistoryConcurrencyAndTransactionTest` | OCC version conflicts, concurrent updates, rollback atomicity | 8 |
| `HistoryDtoTest` | Record contracts, immutability, null handling, equality | 5 |
| `SupabaseHistoryLiveVerificationTest` | Live Supabase PostgreSQL 9-gate verification against real triggers | 1 (9 gates) |
| **Total Task 09 Tests** | | **81** |
