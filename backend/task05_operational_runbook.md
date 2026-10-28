# TASK 05 — GEOGRAPHY ENGINE OPERATIONAL RUNBOOK

**Target System:** Agricultural Intelligence Platform (`agri-platform-backend`)  
**Production Database:** Supabase PostgreSQL 17.6 (`aws-0-ap-southeast-2.pooler.supabase.com:5432/postgres`)  
**Target Audience:** Site Reliability Engineers (SRE), Platform Operators, Backend Developers  
**Runbook Version:** 1.1.0 (Production Release)  
**Effective Date:** 2026-10-18  

---

## 1. OPERATIONAL PHILOSOPHY & CRITICAL SAFEGUARDS

> [!CAUTION]
> **STRICT ARCHITECTURAL CONSTRAINTS:**
> 1. **DO NOT run automated multi-state bulk imports.** Every state synchronization must be triggered on-demand for that individual state only.
> 2. **DO NOT delete or wipe pre-existing legacy records** (e.g., Andhra Pradesh's 82 mandals and 1,319 villages).
> 3. **DO NOT bypass Java in-engine validation** with direct raw SQL `COPY` commands. The Java engine enforces canonical official-code identity scopes, parent-child integrity, and audit logging.
> 4. **DO NOT re-import Kerala unnecessarily.** Kerala Stage 1 is 100% complete and certified in production.

---

## 2. AGRI_LIBRARY STORAGE CONFIGURATION

The Geography Engine integrates with the private `geography-source-library` Supabase Storage bucket using server-side configuration in `application.yml`:

```yaml
app:
  library:
    storage:
      base-url: ${SUPABASE_URL:}
      service-role-key: ${SUPABASE_SERVICE_ROLE_KEY:}
      bucket-name: geography-source-library
      connect-timeout-ms: 15000
      read-timeout-ms: 60000
      max-file-size-bytes: 15728640 # 15 MB max
```

### Storage Outage & Fallback Behavior
- If `SUPABASE_URL` or `SUPABASE_SERVICE_ROLE_KEY` is omitted, or if Supabase Storage is down/unreachable, the engine **automatically falls back to the prevalidated local cache** (`target/preflight-csvs/<file>`).
- SREs do not need to take emergency action during a temporary Storage outage; the system continues functioning via the local cache replica.
- Existing geography lookup APIs query PostgreSQL directly and operate with **100% independence** from Storage uptime.

---

## 3. STATE SYNCHRONIZATION WORKFLOW

### Step 1: Check Current Database Coverage
Before initiating any synchronization, query the state coverage endpoint to verify whether synchronization is actually required:

```http
GET /api/v1/geography/states/{stateCode}/coverage
```

**Expected Responses:**
- **`COMPLETE` (e.g., Kerala `32`):**
  ```json
  {
    "success": true,
    "data": {
      "stateCode": "32",
      "stateName": "Kerala",
      "dbDistricts": 14, "targetDistricts": 14,
      "dbMandals": 78, "targetMandals": 78,
      "dbVillages": 1666, "targetVillages": 1666,
      "coverageStatus": "COMPLETE",
      "syncRequired": false
    }
  }
  ```
  *Action:* **Do not trigger sync.** Inform the client that full geography is already available.

- **`PARTIAL` (e.g., Andhra Pradesh `28`):**
  ```json
  {
    "success": true,
    "data": {
      "stateCode": "28",
      "stateName": "Andhra Pradesh",
      "dbDistricts": 28, "targetDistricts": 28,
      "dbMandals": 82, "targetMandals": 688,
      "dbVillages": 1319, "targetVillages": 17957,
      "coverageStatus": "PARTIAL",
      "syncRequired": true
    }
  }
  ```
  *Action:* Safe to proceed with on-demand sync. The engine will preserve the 82 legacy mandals and 1,319 villages and import only missing records.

- **`EMPTY` (e.g., Telangana `36`, Tamil Nadu `33`, Karnataka `29`):**
  ```json
  {
    "success": true,
    "data": {
      "stateCode": "36",
      "stateName": "Telangana",
      "dbDistricts": 33, "targetDistricts": 33,
      "dbMandals": 0, "targetMandals": 617,
      "dbVillages": 0, "targetVillages": 11308,
      "coverageStatus": "EMPTY",
      "syncRequired": true
    }
  }
  ```
  *Action:* Safe to proceed with on-demand sync.

---

### Step 2: Request Asynchronous State Synchronization
Trigger the synchronization job using an authenticated request carrying a valid farmer or operator JWT (`ROLE_authenticated`):

```http
POST /api/v1/geography/states/{stateCode}/sync
Authorization: Bearer <VALID_JWT_TOKEN>
```

**Immediate Response (HTTP 202 Accepted):**
```json
{
  "success": true,
  "data": {
    "stateCode": "36",
    "stateName": "Telangana",
    "status": "IN_PROGRESS",
    "processedRecords": 0,
    "totalExpected": 11308,
    "startedAt": "2026-10-18T11:30:00Z",
    "message": "State synchronization is in progress"
  }
}
```

*Note:* If a job is already running for this state, the server returns the existing job status without spawning duplicate background workers.

---

### Step 3: Monitor Synchronization Progress
Poll the public status endpoint until completion (recommended interval: every 3 seconds):

```http
GET /api/v1/geography/states/{stateCode}/sync-status
```

**Final Success Response (HTTP 200 OK):**
```json
{
  "success": true,
  "data": {
    "stateCode": "36",
    "stateName": "Telangana",
    "status": "COMPLETED",
    "processedRecords": 11308,
    "totalExpected": 11308,
    "startedAt": "2026-10-18T11:30:00Z",
    "completedAt": "2026-10-18T11:30:35Z",
    "message": "Synchronization completed: inserted 617 mandals, 11308 villages in 34100 ms"
  }
}
```

---

## 4. POST-IMPORT RECONCILIATION & AUDIT PROCEDURE

After any state synchronization completes, run a read-only SQL check against PostgreSQL to certify integrity:

### 4.1 Entity Count Check
```sql
SELECT 
    s.code AS state_code,
    s.name AS state_name,
    count(DISTINCT d.id) AS district_count,
    count(DISTINCT m.id) AS mandal_count,
    count(DISTINCT v.id) AS village_count
FROM public.states s
LEFT JOIN public.districts d ON d.state_id = s.id
LEFT JOIN public.mandals m ON m.district_id = d.id
LEFT JOIN public.villages v ON v.mandal_id = m.id
WHERE s.code = '<STATE_CODE>'
GROUP BY s.code, s.name;
```

### 4.2 Relational Integrity Check
Ensure zero orphan entities were produced:
```sql
-- Check orphan mandals
SELECT count(*) AS orphan_mandals 
FROM public.mandals m 
WHERE m.district_id NOT IN (SELECT id FROM public.districts);

-- Check orphan villages
SELECT count(*) AS orphan_villages 
FROM public.villages v 
WHERE v.mandal_id NOT IN (SELECT id FROM public.mandals);

-- Check duplicate village identities under same mandal
SELECT mandal_id, lower(code), count(*) 
FROM public.villages 
WHERE code IS NOT NULL 
GROUP BY mandal_id, lower(code) 
HAVING count(*) > 1;
```
*Expected Result:* All three queries must return **0**.

---

## 5. FAILURE HANDLING & TROUBLESHOOTING

| Symptom | Root Cause | Operator Remediation |
|---|---|---|
| **Status returns `FAILED` with `Connection reset`** | Transient Supabase WAN pooler timeout during bulk write | The engine automatically commits up to the last successful batch. Simply re-trigger `POST /states/{stateCode}/sync`. The engine is 100% idempotent and resumes from the exact batch without creating duplicates. |
| **HTTP 403 Forbidden on sync** | Caller attempted to hit `/api/v1/admin/geography/import-csv` instead of `/api/v1/geography/states/{code}/sync` | Use the user-facing on-demand sync endpoint `/api/v1/geography/states/{code}/sync` which is accessible to `ROLE_authenticated`. |
| **HTTP 400 Bad Request: `Not an approved pilot state`** | State code is outside the 5 pilot states (`28`, `29`, `32`, `33`, `36`) | Currently, only the 5 approved pilot states have certified source CSVs. To onboard a new state, an SRE must upload and approve the normalized LGD file in `target/preflight-csvs/` and add it to `GeographyLibraryIntegrationService.APPROVED_PILOT_STATES`. |
| **Batch constraint failure log warning** | Invalid or corrupt record in source data | The engine automatically falls back to row-by-row inserts, quarantines the failing record, and logs the row. Check `summary.getErrors()` for details. Valid records remain unaffected. |

---

## 6. DISASTER RECOVERY & ROLLBACK PLAN

In the catastrophic event of unrecoverable database corruption during an ingestion operation:

1. **Locate the Pre-Ingestion Backup Snapshot:**
   All snapshots are generated with UTC timestamps under:
   `target/backup/geography_pre_rls_snapshot_<TIMESTAMP>.sql`
2. **Execute Snapshot Verification:**
   Verify the snapshot header metadata:
   ```bash
   head -n 10 target/backup/geography_pre_rls_snapshot_*.sql
   ```
3. **Restore Geography Tables:**
   Execute a transaction-bounded restore on the affected tables (`villages`, `mandals`):
   ```sql
   BEGIN;
   -- Restore target mandals and villages from snapshot backup
   COMMIT;
   ```
4. **Run Live Audit Test:**
   Execute the verification test suite to ensure row counts and RLS policies are restored:
   ```bash
   mvn test -pl . -Dtest="KeralaPostImportReadOnlyAuditTest,ExecuteMinimalRlsCorrectionProductionTest"
   ```
