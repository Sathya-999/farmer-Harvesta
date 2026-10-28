# Frontend API Connection Matrix
**Project:** Harvest(a) — Agricultural Intelligence & Coordination Platform  
**Environment:** Next.js 16.2.12 (Frontend), Spring Boot 3.3.4 / Java 25 (Backend), Supabase PostgreSQL  
**Verification Date:** 2026-10-19  

This matrix documents all 20 frontend routes, their corresponding Spring Boot backend endpoints, HTTP methods, authentication/authorization requirements, data contracts, and integration status.

---

## Route Connection Summary

| # | Route | Component | Method & Backend Endpoints | Auth Level | Final Status |
|---|---|---|---|---|---|
| 1 | `/` | `components/splash/AgriculturalSplashScreen.tsx` | N/A (Client splash with 2s transition) | Public | **CONNECTED** |
| 2 | `/login` | `app/login/page.tsx` | `POST ${SUPABASE_URL}/auth/v1/token` | Public | **CONNECTED** (Dev fallback enabled) |
| 3 | `/register` | `app/register/page.tsx` | `POST ${SUPABASE_URL}/auth/v1/signup` | Public | **CONNECTED** (Dev fallback enabled) |
| 4 | `/reset-password` | `app/reset-password/page.tsx` | `POST ${SUPABASE_URL}/auth/v1/recover` | Public | **PARTIALLY CONNECTED** (Pending SMTP) |
| 5 | `/onboarding` | `app/onboarding/page.tsx` | `POST /api/v1/farmers/onboarding`, `GET /api/v1/geography/states/**` | Bearer JWT (`FARMER`) | **CONNECTED** |
| 6 | `/dashboard` | `app/dashboard/page.tsx` & `HarvestaDashboard.tsx` | `GET /api/v1/farmers/me`, `GET /api/v1/plots`, `GET /api/v1/cultivations`, `GET /api/v1/analytics/farmer/summary`, `GET /api/v1/notifications/unread-count` | Bearer JWT (`FARMER`) | **CONNECTED** |
| 7 | `/profile` | `app/profile/page.tsx` | `GET /api/v1/farmers/me`, `PUT /api/v1/farmers/me` | Bearer JWT (`FARMER`) | **CONNECTED** |
| 8 | `/plots` | `app/plots/page.tsx` | `GET /api/v1/plots?page=0&size=50&status=ACTIVE` | Bearer JWT (`FARMER`) | **CONNECTED** |
| 9 | `/plots/new` | `app/plots/new/page.tsx` | `POST /api/v1/plots`, Cascading Geography (`GET /api/v1/geography/**`), `POST /api/v1/geography/resolve` | Bearer JWT (`FARMER`) | **CONNECTED** |
| 10 | `/plots/[id]` | `app/plots/[id]/page.tsx` | `GET /api/v1/plots/{id}`, `PUT /api/v1/plots/{id}`, `DELETE /api/v1/plots/{id}`, `PATCH /api/v1/plots/{id}/reactivate` | Bearer JWT (`FARMER`) | **CONNECTED** |
| 11 | `/cultivations` | `app/cultivations/page.tsx` | `GET /api/v1/cultivations?page=0&size=50` | Bearer JWT (`FARMER`) | **CONNECTED** |
| 12 | `/cultivations/new` | `app/cultivations/new/page.tsx` | `POST /api/v1/cultivations`, `GET /api/v1/plots`, `GET /api/v1/crops`, `GET /api/v1/crops/{id}/varieties`, `GET /api/v1/seasons` | Bearer JWT (`FARMER`) | **CONNECTED** |
| 13 | `/cultivations/[id]` | `app/cultivations/[id]/page.tsx` | `GET /api/v1/cultivations/{id}`, `PATCH /api/v1/cultivations/{id}/status`, `GET /api/v1/cultivations/{id}/events` | Bearer JWT (`FARMER`) | **CONNECTED** |
| 14 | `/history` | `app/history/page.tsx` | `GET /api/v1/history/cultivations/{id}/snapshots`, `GET /api/v1/history/cultivations/{id}/events` | Bearer JWT (`FARMER`) | **CONNECTED** |
| 15 | `/analytics` | `app/analytics/page.tsx` | `GET /api/v1/analytics/farmer/summary`, `GET /api/v1/analytics/geography/summary`, `GET /api/v1/analytics/geography/crops`, `GET /api/v1/analytics/geography/signals` | Bearer JWT (`FARMER`) | **CONNECTED** |
| 16 | `/notifications` | `app/notifications/page.tsx` | `GET /api/v1/notifications`, `GET /api/v1/notifications/unread-count`, `PATCH /api/v1/notifications/{id}/read`, `PATCH /api/v1/notifications/read-all` | Bearer JWT (`FARMER`) | **CONNECTED** |
| 17 | `/settings` | `app/settings/page.tsx` | Client preference persistence & profile cache invalidation | Bearer JWT (`FARMER`) | **CONNECTED** |
| 18 | `/field` | `app/field/page.tsx` | `GET /api/v1/plots`, `GET /api/v1/cultivations` | Bearer JWT (`FARMER`) | **CONNECTED** (Telemetry marked Beta V2) |
| 19 | `/market` | `app/market/page.tsx` | `GET /api/v1/cultivations` | Bearer JWT (`FARMER`) | **CONNECTED** (e-NAM marked Beta V2) |
| 20 | `/community` | `app/community/page.tsx` | Privacy notice & link to `/analytics` | Bearer JWT (`FARMER`) | **CONNECTED** (Network marked Beta V2) |

---

## Detailed Route Specifications

### 1. Landing / Splash Page (`/`)
- **Component:** `components/splash/AgriculturalSplashScreen.tsx`
- **Endpoints:** None (Static client presentation)
- **Data Displayed:** Brand title, agricultural mission, 2,000ms transition progress bar, and "Enter Now" bypass link leading to `/register`.
- **Status:** **CONNECTED**

### 2. Login Page (`/login`)
- **Component:** `app/login/page.tsx`
- **Endpoints:** `POST ${SUPABASE_URL}/auth/v1/token?grant_type=password`
- **Auth Level:** Public
- **Input / Validation:** Email/phone required, password >= 6 characters.
- **Handling:** Saves JWT to `localStorage` (`sf_auth_token`) and redirects to safe internal return URL. In non-production without `NEXT_PUBLIC_SUPABASE_ANON_KEY`, guarded dev fallback allows testing when `NEXT_PUBLIC_ENABLE_DEV_AUTH_FALLBACK=true`.
- **Status:** **CONNECTED**

### 3. Registration Page (`/register`)
- **Component:** `app/register/page.tsx`
- **Endpoints:** `POST ${SUPABASE_URL}/auth/v1/signup`
- **Auth Level:** Public
- **Validation:** First name, last name, valid email, password >= 8 characters.
- **Status:** **CONNECTED**

### 4. Password Recovery (`/reset-password`)
- **Component:** `app/reset-password/page.tsx`
- **Endpoints:** `POST ${SUPABASE_URL}/auth/v1/recover`
- **Auth Level:** Public
- **Status:** **PARTIALLY CONNECTED** (Truthfully reports missing Supabase anon key/SMTP gateway configuration rather than simulating fake success).

### 5. Farmer Onboarding (`/onboarding`)
- **Component:** `app/onboarding/page.tsx`
- **Endpoints:**
  - `POST /api/v1/farmers/onboarding`
  - `GET /api/v1/geography/states`
  - `GET /api/v1/geography/states/{id}/districts`
  - `GET /api/v1/geography/districts/{id}/mandals`
  - `GET /api/v1/geography/mandals/{id}/villages`
- **Auth Level:** Authenticated (`FARMER`)
- **Data Displayed:** Cascading administrative selection for farmer's home village.
- **Status:** **CONNECTED**

### 6. Farmer Dashboard (`/dashboard`)
- **Component:** `app/dashboard/page.tsx` & `HarvestaDashboard.tsx`
- **Endpoints:**
  - `GET /api/v1/farmers/me`
  - `GET /api/v1/plots?page=0&size=50&status=ACTIVE`
  - `GET /api/v1/cultivations?page=0&size=50`
  - `GET /api/v1/analytics/farmer/summary`
  - `GET /api/v1/notifications/unread-count`
- **Data Displayed:**
  - Real registered plot holding label and acreage.
  - Real location (village, district, state) replacing hardcoded coordinates.
  - Real crop cycle name and active status.
  - Real land capacity allocation and utilization percentage.
  - Zero fake sensor values (NPK, NDVI, soil moisture replaced with verified holding metrics).
- **Status:** **CONNECTED**

### 7. Farmer Profile (`/profile`)
- **Component:** `app/profile/page.tsx`
- **Endpoints:**
  - `GET /api/v1/farmers/me`
  - `PUT /api/v1/farmers/me` (with optimistic concurrency check via `expectedVersion`)
- **Security:** IDOR-protected; derives identity solely from backend JWT subject (`sub`).
- **Status:** **CONNECTED**

### 8. My Plots (`/plots`)
- **Component:** `app/plots/page.tsx`
- **Endpoints:** `GET /api/v1/plots?page=0&size=50&status=ACTIVE`
- **Data Displayed:** Real plot labels, total acreage, hectares conversion, administrative village, and active status.
- **Status:** **CONNECTED**

### 9. Add Plot (`/plots/new`)
- **Component:** `app/plots/new/page.tsx`
- **Endpoints:**
  - `POST /api/v1/plots` (with `Idempotency-Key` header)
  - `POST /api/v1/geography/resolve` (GPS advisory resolution)
  - `GET /api/v1/geography/states/**` (cascading selector)
- **Validation:** Positive acreage > 0, valid `villageId` verified in database.
- **Status:** **CONNECTED**

### 10. View / Edit Plot (`/plots/[id]`)
- **Component:** `app/plots/[id]/page.tsx`
- **Endpoints:**
  - `GET /api/v1/plots/{id}`
  - `PUT /api/v1/plots/{id}`
  - `DELETE /api/v1/plots/{id}` (soft deactivation)
  - `PATCH /api/v1/plots/{id}/reactivate`
- **Status:** **CONNECTED**

### 11. My Cultivations (`/cultivations`)
- **Component:** `app/cultivations/page.tsx`
- **Endpoints:** `GET /api/v1/cultivations?page=0&size=50`
- **Data Displayed:** Real crop names, linked plot labels, cultivated acreage, season, cultivation year, and lifecycle status (`PLANNED`, `STARTED`, `COMPLETED`, `CANCELLED`, `ABANDONED`).
- **Status:** **CONNECTED**

### 12. Add Cultivation (`/cultivations/new`)
- **Component:** `app/cultivations/new/page.tsx`
- **Endpoints:**
  - `POST /api/v1/cultivations`
  - `GET /api/v1/crops`
  - `GET /api/v1/crops/{cropId}/varieties`
  - `GET /api/v1/seasons`
  - `GET /api/v1/plots`
- **Validation:** Area constraint (cultivated area <= plot total area), non-null planting date, single active cycle per plot holding.
- **Status:** **CONNECTED**

### 13. Cultivation Lifecycle Detail (`/cultivations/[id]`)
- **Component:** `app/cultivations/[id]/page.tsx`
- **Endpoints:**
  - `GET /api/v1/cultivations/{id}`
  - `PATCH /api/v1/cultivations/{id}/status` (valid state transitions: `PLANNED -> STARTED`, `STARTED -> COMPLETED`, etc.)
  - `GET /api/v1/cultivations/{id}/events` (lifecycle transition audit events)
- **Status:** **CONNECTED**

### 14. Cultivation History (`/history`)
- **Component:** `app/history/page.tsx`
- **Endpoints:**
  - `GET /api/v1/history/cultivations/{id}/snapshots`
  - `GET /api/v1/history/cultivations/{id}/events`
- **Nature:** Strictly read-only chronological audit records.
- **Status:** **CONNECTED**

### 15. Regional Analytics (`/analytics`)
- **Component:** `app/analytics/page.tsx`
- **Endpoints:**
  - `GET /api/v1/analytics/farmer/summary`
  - `GET /api/v1/analytics/geography/summary`
  - `GET /api/v1/analytics/geography/crops`
  - `GET /api/v1/analytics/geography/signals`
- **Data Displayed:** Real aggregate holding metrics, regional crop distributions, and agronomic signals.
- **Status:** **CONNECTED**

### 16. Notifications (`/notifications`)
- **Component:** `app/notifications/page.tsx`
- **Endpoints:**
  - `GET /api/v1/notifications`
  - `GET /api/v1/notifications/unread-count`
  - `PATCH /api/v1/notifications/{id}/read`
  - `PATCH /api/v1/notifications/read-all`
- **Status:** **CONNECTED**

### 17. Settings (`/settings`)
- **Component:** `app/settings/page.tsx`
- **Endpoints:** Client state management, local cache eviction, language selection.
- **Status:** **CONNECTED**

### 18. Field Plot Workspace (`/field`)
- **Component:** `app/field/page.tsx`
- **Endpoints:** `GET /api/v1/plots`, `GET /api/v1/cultivations`
- **Status:** **CONNECTED** (Physical IoT probes explicitly designated as Beta V2 roadmap capability).

### 19. Market Gateway (`/market`)
- **Component:** `app/market/page.tsx`
- **Endpoints:** `GET /api/v1/cultivations`
- **Status:** **CONNECTED** (Displays farmer's actual crops reaching market; e-NAM/APMC integration truthfully flagged as Beta V2).

### 20. Community Exchange (`/community`)
- **Component:** `app/community/page.tsx`
- **Endpoints:** Navigational routing to `/analytics` and `/dashboard`
- **Status:** **CONNECTED** (Peer discovery flagged as Beta V2 with strict privacy preservation).
