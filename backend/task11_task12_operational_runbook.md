# Task 11 & Task 12 — Operational Runbook
**Project:** Agricultural Intelligence and Coordination Platform  
**Target Environment:** Production / Staging  
**Platform Stack:** Spring Boot 3.3.4, Java 25, PostgreSQL / Supabase  
**Date:** 2026-10-18  

---

## 1. Architecture & Operational Overview

The platform backend provides REST APIs and real-time transaction processing for farmers, administrative operators, and regional agricultural officers.

### Core Subsystems:
1. **Security & Identity:** Stateless Supabase JWT validation, Spring Security filters, and principal-based tenant context (`app.actor_id`, `app.actor_type`).
2. **Geography Engine:** Hierarchical LGD administrative geography (States, Districts, Mandals, Villages) with GPS advisory resolution and on-demand, state-isolated synchronization from `Agri_Library`.
3. **Plot Management:** Geographic parcel registry with village linkage, strict boundary and acreage validation, and tenant isolation.
4. **Cultivation & Crop Management:** Crop cycle tracking by `(plot_id, season_id, cultivation_year)`, capacity checks, state transitions (`PLANNED -> STARTED -> COMPLETED`), and immutable history triggers.
5. **Notification Delivery:** Farmer notifications with unread state tracking, partial index acceleration, and multi-tenant isolation.
6. **Analytics & Aggregation:** Village-level and farmer-level land utilization summaries excluding terminal cultivation statuses.

---

## 2. Environment Variables & Secret Configuration

The following environment variables must be configured in production (e.g., via Kubernetes secrets, AWS Secrets Manager, or container environment). **Never commit or log plain-text secrets.**

| Environment Variable | Description | Example / Allowed Format |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `prod` (or `staging`, `test`) |
| `SERVER_PORT` | HTTP server port | `8080` |
| `DATABASE_URL` | PostgreSQL JDBC Connection URL | `jdbc:postgresql://<pooler-host>:5432/<db-name>?sslmode=require` |
| `DATABASE_USER` | Database username | `postgres.<project-ref>` |
| `DATABASE_PASSWORD` | Database user password | `<strong-db-password>` |
| `SUPABASE_JWT_SECRET` | Secret used to verify Supabase JWTs | `<base64-or-raw-jwt-secret>` |
| `AGRI_LIBRARY_BASE_URL` | Base URL for private storage bucket | `https://<storage-ref>.supabase.co/storage/v1` |
| `AGRI_LIBRARY_SERVICE_ROLE_KEY` | Server-side key for library access | `<server-side-service-key>` (Never sent to client) |
| `MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE` | Actuator endpoints to expose | `health,info,metrics` |

---

## 3. Deployment & Startup Verification Checklist

Before directing live farmer traffic to the backend, complete the following pre-flight verification:

1. **Database Connectivity:**
   - Verify connection pool health using `/actuator/health`.
   - Confirm connection to the Supabase Transaction Pooler (`pooler.supabase.com:5432`) or direct database (`:6543`).
2. **RLS Policy Verification:**
   - Execute the pre-flight read-only check:
     ```sql
     SELECT tablename, policyname, qual 
     FROM pg_policies 
     WHERE schemaname = 'public' AND tablename IN ('states', 'districts', 'mandals', 'villages');
     ```
   - Confirm only the 4 active-only policies (`is_active = true`) exist, and zero broad `USING (true)` policies exist.
3. **Baseline Geography Counts:**
   - Ensure baseline geography records are intact:
     ```sql
     SELECT 
       (SELECT count(*) FROM public.states) as states_count,
       (SELECT count(*) FROM public.districts) as districts_count,
       (SELECT count(*) FROM public.mandals) as mandals_count,
       (SELECT count(*) FROM public.villages) as villages_count;
     ```
   - Target production counts: States: 36, Districts: 784, Mandals: 169 (including Kerala 78), Villages: 3,161 (including Kerala 1,666 and AP 1,319).
4. **Health & Readiness Endpoints:**
   - Probe `GET http://localhost:8080/actuator/health` -> HTTP 200 `{"status": "UP"}`.

---

## 4. Operational Procedures & Runbooks

### 4.1 On-Demand Geography State Synchronization
- **Pre-condition:** A farmer attempts to register a plot in a state that has not yet been synchronized into `mandals` and `villages` (e.g., Telangana, Karnataka, or Tamil Nadu).
- **Security Constraint:** Only authorized administrative callers (`ROLE_admin`) may invoke the synchronization endpoint:
  ```http
  POST /api/v1/admin/geography/sync/state/{stateLgdCode}
  Authorization: Bearer <admin-jwt-token>
  ```
- **Execution Rules:**
  - The engine retrieves the validated CSV for that specific state only from `Agri_Library`.
  - Batching is bounded (1,000 records per JDBC batch) to prevent memory saturation and connection timeouts.
  - Zero bulk-import of unrelated states.
  - In the event of network disruption, the transaction rolls back cleanly; re-running the command is completely idempotent.

### 4.2 Handling Database Session Actor Context
- The PostgreSQL trigger `private.validate_intention_write()` requires session actor metadata for mutations on auditable tables (`cultivations`, `plots`).
- In backend Java services, this is automatically set by the transaction aspect.
- If performing manual operational queries or maintenance via `psql`:
  ```sql
  BEGIN;
  SELECT set_config('app.actor_id', '00000000-0000-0000-0000-000000000000', true);
  SELECT set_config('app.actor_type', 'ADMIN', true);
  -- perform maintenance DML
  COMMIT;
  ```

### 4.3 Monitoring & Alerting Thresholds
- **Latency Target:**
  - Common read operations (dashboard, plots, cultivations): p95 $\le$ 450 ms over public WAN; p95 $\le$ 30 ms within VPC.
  - Write operations (plot creation, lifecycle updates): p95 $\le$ 600 ms over WAN.
- **Alert Triggers:**
  - HTTP 5xx Error Rate > 1% over 5-minute rolling window -> Page on-call engineering.
  - Database Connection Pool Saturation > 80% active connections -> Alert infrastructure team.
  - Actuator `/actuator/health` status != `UP` -> Trigger container restart.

### 4.4 Failure Recovery & Incident Triage

| Symptom | Probable Cause | Action |
|---|---|---|
| HTTP 401 on all requests | Supabase JWT secret mismatch or expired token | Check `SUPABASE_JWT_SECRET` environment variable and client clock drift. |
| HTTP 403 on admin sync | Caller lacks `ROLE_admin` claim in JWT | Verify Supabase user role in `auth.users` app_metadata / user_metadata. |
| HTTP 400 "Cultivated area exceeds plot capacity" | Client attempting to register area > plot size | Explain boundary rule to user; verify plot `total_area_acres`. |
| HTTP 409 "Active cultivation cycle already exists" | Duplicate submission for `(plot_id, season_id, year)` | Verify if farmer already started this cycle; inspect status transitions. |
| Database connection pool timeout | Supabase connection exhaustion or long-running transaction | Check Supabase pooler metrics; ensure JDBC connections use `autoCommit=true` outside `@Transactional`. |
