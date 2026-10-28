# Frontend Known Blockers & External Roadmap Dependencies
**Project:** Harvest(a) Agricultural Intelligence Platform  
**Date:** 2026-10-19  

This document details all external dependencies, missing third-party credentials, and roadmap milestones that prevent complete production operation of specific secondary capabilities.

---

## 1. Missing Third-Party Credentials

### `NEXT_PUBLIC_SUPABASE_ANON_KEY`
- **Affected Routes:** `/login`, `/register`, `/reset-password`
- **Description:** `.env.local` contains the project URL (`https://ienrhuqcbdkdexjwqkul.supabase.co`), but `NEXT_PUBLIC_SUPABASE_ANON_KEY` is empty.
- **Impact:** Direct calls from the browser to Supabase Auth (`/auth/v1/token`, `/auth/v1/signup`) fail with HTTP 401 without an API key.
- **Current Behavior:** The frontend uses guarded development fallback (`NEXT_PUBLIC_ENABLE_DEV_AUTH_FALLBACK=true`) for local testing. In production, it gracefully reports that the authentication service is not configured.
- **Remediation Required:** Add the public Supabase anonymous key (`NEXT_PUBLIC_SUPABASE_ANON_KEY`) from the Supabase project dashboard to `.env.local`.

---

## 2. Password Recovery SMTP Gateway

### Supabase Auth Custom SMTP
- **Affected Route:** `/reset-password`
- **Description:** Password reset requires an active SMTP provider (SendGrid, AWS SES, or Resend) configured in Supabase Auth to send reset email links.
- **Current Behavior:** The password recovery page displays an explicit notice informing the user that the recovery service is awaiting email gateway configuration, preventing fake success messages.
- **Remediation Required:** Configure SMTP credentials in the Supabase Auth Settings.

---

## 3. Scheduled Beta V2 Roadmap Features

### A. Live APMC Mandi Price Ticker (Agmarknet / e-NAM)
- **Affected Route:** `/market`
- **Description:** Government e-NAM / Agmarknet API integration requires an official institutional API gateway key.
- **Current Behavior:** Replaced fake simulated prices with an honest status card and displayed the farmer's real registered cultivations approaching harvest.

### B. Physical In-Situ IoT Sensor Telemetry
- **Affected Routes:** `/field`, `/dashboard`
- **Description:** Real-time NPK ppm levels, capacitive soil moisture percentages, and canopy thermal sensors require physical hardware deployment.
- **Current Behavior:** Converted lower dashboard cards to display verified land capacity and cultivation lifecycle counts. Sensor hardware is flagged as Beta V2.

### C. Peer Farmer Social Network & Communal Buying
- **Affected Route:** `/community`
- **Description:** Peer-to-peer farmer discovery requires consent-driven privacy mechanisms to avoid leaking private land holding data.
- **Current Behavior:** Displays an explicit privacy notice and routes farmers to verified aggregate district data under Regional Intelligence (`/analytics`).
