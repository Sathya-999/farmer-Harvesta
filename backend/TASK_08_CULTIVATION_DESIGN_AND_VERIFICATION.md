# TASK 08 — CULTIVATION MANAGEMENT DESIGN AND VERIFICATION

**Project:** Agricultural Intelligence Platform Backend (`G:\farmer\agri-platform-backend`)  
**Task:** Task 08 — Cultivation Management  
**Environment:** Production Supabase PostgreSQL (`aws-0-ap-southeast-2.pooler.supabase.com:5432`)  
**Technology Stack:** Java 21 / JVM 25, Spring Boot 3.3.4, Spring Data JPA, PostgreSQL 15, Spring Security 6  
**Final Certification Gate:** `[GREEN — TASK 08 CERTIFIED]`  
**Date:** October 3, 2026

---

## 1. Executive Summary

Task 08 (Cultivation Management) has been implemented, tested, and certified for production readiness against the live Supabase database. It delivers a complete cultivation lifecycle management system built on Task 07 (Plot Management), consistent with Task 05 (Geography) and Task 06 (Farmer Profile).

Key deliverables:
1. **Live Supabase DB Reconciliation:** Audited all cultivation tables, constraints, and trigger functions against live Supabase before writing a single line of code.
2. **Full Domain Model:** `CultivationIntention`, `CultivationStatusEvent`, `CultivationIntentionHistory`, `Season`, `Crop`, `CropVariety` entities aligned precisely to live DB columns and types.
3. **Lifecycle State Machine:** `PLANNED → STARTED → COMPLETED` (terminal), `CANCELLED` / `ABANDONED` (terminal from any non-terminal), fully enforced by the live DB trigger `private.validate_intention_write()`.
4. **Security:** IDOR-proof via JWT `sub` claim; cross-farmer access returns HTTP 404.
5. **Concurrency:** Pessimistic `FOR UPDATE` row lock on `Plot` serializes area capacity checks; OCC `version` guards field updates.
6. **Idempotency:** SHA-256 keyed 24-hour replay via `public.idempotency_keys`.
7. **Reference Data API:** Zero hardcoded reference data; crops, varieties, seasons served dynamically.
8. **Test Execution:** 79 Task 08 tests + 323 regression tests = **402 total, all passing**.

---

## 2. Domain Architecture

### 2.1 Domain Separation

```
FARMER (auth.users → farmer_profiles)
  └── owns PLOT (public.plots)
        └── hosts CULTIVATION INTENTION (public.cultivation_intentions)
              ├── SEASON (public.seasons)          — reference
              ├── CROP (public.crops)              — reference
              └── CROP VARIETY (public.crop_varieties) — reference
```

**Identity Rule:** A cultivation intention is identified by its **lineage**:
`(plot_id, season_id, cultivation_year)` — not by farmer alone, not by plot alone.

### 2.2 Entities

| Entity | Table | Purpose |
|---|---|---|
| `CultivationIntention` | `public.cultivation_intentions` | Core record: plan + execution data for a plot/season/year cycle |
| `CultivationStatusEvent` | `public.cultivation_status_events` | Immutable event log per lifecycle transition |
| `CultivationIntentionHistory` | `public.cultivation_intention_history` | Immutable field-change snapshots |
| `Season` | `public.seasons` | KHARIF / RABI / ZAID reference data |
| `Crop` | `public.crops` | Crop master reference |
| `CropVariety` | `public.crop_varieties` | Per-crop variety reference (compound FK: `(id, crop_id)`) |

### 2.3 Lifecycle State Machine

```
                  ┌─────────────┐
    [CREATE] ───► │   PLANNED   │ ◄── field UPDATEs allowed
                  └──────┬──────┘
                         │ PATCH /status → STARTED
                         │ (actual_planting_date required)
                  ┌──────▼──────┐
                  │   STARTED   │ ◄── actual field UPDATEs allowed
                  └──────┬──────┘
                         │ PATCH /status → COMPLETED
                         │ (harvest_date + actual_area_acres required)
                  ┌──────▼──────┐
                  │  COMPLETED  │ ── TERMINAL (no edits, no reopen)
                  └─────────────┘

From PLANNED or STARTED:
  → CANCELLED  (terminal; excluded from unique active cycle index)
  → ABANDONED  (terminal; excluded from unique active cycle index)
```

**Key invariant:** `COMPLETED` records remain in `ux_active_cultivation_cycle` — a cycle that was completed cannot be re-planned for the same `(plot_id, season_id, cultivation_year)`. Only `CANCELLED`/`ABANDONED` allow re-planning.

---

## 3. Live Database Reconciliation

### 3.1 Tables Confirmed on Live Supabase

| Table | Columns | Key Constraints | Triggers |
|---|---|---|---|
| `cultivation_intentions` | 18 | PK, FK to plots/farmer (composite), FK to crops/seasons, 7 CHECK constraints, unique index `ux_active_cultivation_cycle` | `trg_validate_intention_write`, `trg_record_intention_history`, `trg_record_status_event_insert` |
| `cultivation_status_events` | 6 | PK, FK to cultivation_intentions | Populated by trigger only |
| `cultivation_intention_history` | 10+ | PK, FK to cultivation_intentions | Populated by trigger only |
| `seasons` | 6 | PK, unique code | Seeded idempotently |
| `crops` | 5 | PK, unique code | Seeded idempotently |
| `crop_varieties` | 6 | PK, compound FK `(id, crop_id)` | Seeded idempotently |
| `idempotency_keys` | 8 | PK, UNIQUE `(farmer_id, idempotency_key)`, FK cascade delete | Reused from Task 07 |

### 3.2 Live Trigger Functions Reconciled

| Function | Fires On | Key Behaviour |
|---|---|---|
| `private.validate_intention_write()` | BEFORE INSERT OR UPDATE | Reads `app.actor_type` (must be `FARMER`/`ADMIN`/`SYSTEM`); validates status transitions; rejects terminal edits; enforces actual field constraints |
| `private.record_intention_history()` | AFTER UPDATE | Snapshots `OLD.*` with `change_type = 'FIELD_UPDATE'` or `'STATUS_CHANGE'`; stores `snapshot_at` timestamp |
| `private.record_status_event()` | AFTER INSERT | Creates first status event: `NULL → PLANNED` with actor context |
| `private.current_actor_id()` | Session function | Reads `app.actor_id` pg session setting |
| `private.current_actor_type()` | Session function | Reads `app.actor_type` pg session setting |

### 3.3 Critical Trigger Invariants

1. **Session actor context is mandatory.** Every write transaction executes inside Spring's `@Transactional` boundary:
   ```sql
   SELECT set_config('app.actor_id', :actorId, true),
          set_config('app.actor_type', 'FARMER', true)
   ```
   `is_local = true` ensures the setting is scoped to the transaction, not the connection lifetime.

2. **`version` is trigger-managed.** On UPDATE, `NEW.version = OLD.version + 1` is set by the trigger. The application never sets `version` directly.

3. **History fires AFTER UPDATE only.** The INSERT event is captured by `trg_record_status_event_insert` (not history).

4. **`ux_active_cultivation_cycle` partial index.** Only `CANCELLED` and `ABANDONED` statuses are excluded from uniqueness; `COMPLETED` remains protected.

---

## 4. Security Design

### 4.1 IDOR Protection

- `farmerId` extracted exclusively from JWT `sub` claim via `SecurityUtils.getAuthenticatedUserId()`.
- All repository queries filter by `farmer_id = :farmerId`.
- Cross-farmer access always returns HTTP 404 — no data enumeration possible.
- **Verified live:** Farmer B's queries against Farmer A's cultivation records return 0 rows.

### 4.2 Farmer Lifecycle Gate

```java
requireActiveFarmer(farmerId):
  - Profile not found → HTTP 404
  - status = DELETED  → HTTP 404
  - status = SUSPENDED → HTTP 403
  - status = ACTIVE   → proceed
```

### 4.3 DB-Level Plot Ownership

The live DB constraint `cultivation_plot_owner_fk` enforces:
```sql
FOREIGN KEY (plot_id, farmer_id) REFERENCES plots(id, farmer_id)
```
Even if application-layer validation is bypassed, the DB rejects cross-farmer plot references.

---

## 5. Concurrency Design

### 5.1 Pessimistic Plot Lock (Area Capacity Check)

```java
Plot plot = plotRepository.findByIdWithLock(
    request.plotId(), LockModeType.PESSIMISTIC_WRITE);
// → SELECT ... FROM plots WHERE id = ? FOR UPDATE

BigDecimal activeAllocations = cultivationRepository.sumActiveAcres(plotId);
if (activeAllocations.add(request.cultivatedAreaAcres())
        .compareTo(plot.getTotalAreaAcres()) > 0) {
    throw new ConflictException("Area allocation exceeds plot capacity");
}
```

The `FOR UPDATE` lock on the `Plot` row serializes all concurrent CREATE requests for the same plot, preventing race conditions in area capacity validation.

### 5.2 Optimistic Concurrency (Version Field)

- `version` is an integer starting at 1, incremented by DB trigger on every UPDATE.
- Update and status-change operations include the current version in the request; stale versions are detected and rejected.

### 5.3 Concurrent Idempotency Key Safety

- The `UNIQUE (farmer_id, idempotency_key)` constraint on `idempotency_keys` means only one concurrent CREATE can win; the loser gets a `DataIntegrityViolationException` which the service translates to a 409 Conflict.

---

## 6. API Contract

### Cultivation Intentions (`/api/v1/cultivations`)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/cultivations` | JWT | Create cultivation intention (idempotent via `Idempotency-Key` header) |
| GET | `/api/v1/cultivations/{id}` | JWT | Get by ID (IDOR-protected) |
| GET | `/api/v1/cultivations` | JWT | Paginated list (farmer-scoped, filterable) |
| PUT | `/api/v1/cultivations/{id}` | JWT | Update fields on non-terminal intention |
| PATCH | `/api/v1/cultivations/{id}/status` | JWT | Advance lifecycle state |
| GET | `/api/v1/cultivations/{id}/events` | JWT | Status event audit log |
| GET | `/api/v1/cultivations/{id}/history` | JWT | Field-change history snapshots |

### Reference Data (public, no auth)

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/crops` | All active crops |
| GET | `/api/v1/crops/{cropId}/varieties` | Varieties for a crop |
| GET | `/api/v1/seasons` | All active seasons |

### DB → HTTP Error Mapping

| DB Exception | HTTP | Meaning |
|---|---|---|
| `ILLEGAL_STATUS_TRANSITION` | 409 | Invalid state transition |
| `TERMINAL_INTENTION_IS_NOT_EDITABLE` | 409 | Cannot edit terminal record |
| `ACTUAL_FIELDS_NOT_ALLOWED_FOR_STATUS` | 400 | Actual fields in wrong state |
| `HARVEST_DATE_REQUIRES_COMPLETED_STATUS` | 400 | Harvest date requires COMPLETED |
| `ux_active_cultivation_cycle` | 409 | Duplicate active cycle |

---

## 7. Regression Safety

All prior task contracts were verified not modified by Task 08:

| Task | Description | Verification |
|---|---|---|
| Task 05 | Geography (states, districts, mandals, villages, LGD ingestion) | All 129 tests passed |
| Task 06 | Farmer Profile (CRUD, status, IDOR) | All 35 tests passed |
| Task 07 | Plot Management (CRUD, area guard, idempotency) | All 97 tests passed |
| Common | Security config, actuator, JWT validator | All 62 tests passed |
