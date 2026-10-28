# Agricultural Intelligence Platform — Backend

> **Farmer-first Modular Monolith** built with Spring Boot 3.x / Java 21 / Maven.

---

## Architecture

This project is a **Modular Monolith** — a single deployable Spring Boot application organised into
vertical feature slices, each with its own package hierarchy (controller → service → repository → entity → dto → mapper).

### Feature Modules

| Module          | Purpose                                              |
|-----------------|------------------------------------------------------|
| `auth`          | Shared authentication infrastructure (JWT / Supabase Auth) |
| `farmer`        | Farmer profile management                            |
| `plot`          | Farmer plot / land management                        |
| `cultivation`   | Cultivation intentions and crop plans                |
| `history`       | Read-only cultivation history (database-trigger-driven) |
| `analytics`     | Deterministic agricultural analysis                  |
| `notification`  | Farmer notification records                          |
| `geography`     | Geographic and agricultural reference data           |
| `common`        | Shared infrastructure (config, exceptions, responses, util) |

---

## Technology Stack

| Concern          | Choice                              |
|------------------|-------------------------------------|
| Language         | Java 21                             |
| Framework        | Spring Boot 3.x                     |
| Build            | Maven                               |
| Database         | PostgreSQL on Supabase (future)     |
| Authentication   | Supabase Auth + JWT (future)        |
| Frontend         | Flutter + Dart (separate repo)      |

---

## Project Structure

```
agri-platform-backend/
├── pom.xml
├── .gitignore
├── .env.example
├── README.md
└── src/
    ├── main/
    │   ├── java/com/agriplatform/
    │   │   ├── AgriPlatformApplication.java
    │   │   ├── auth/
    │   │   ├── farmer/
    │   │   ├── plot/
    │   │   ├── cultivation/
    │   │   ├── history/
    │   │   ├── analytics/
    │   │   ├── notification/
    │   │   ├── geography/
    │   │   └── common/
    │   └── resources/
    └── test/
        └── java/com/agriplatform/
```

---

## Getting Started

```bash
# Copy environment template
cp .env.example .env
# (Fill in values — see .env.example for required keys)

# Run with Maven
./mvnw spring-boot:run
```

---

## Status

- [x] Task 01 — Project skeleton created
- [ ] Task 02 — Application configuration
- [ ] Task 03 — Database integration
- [ ] Task 04 — Authentication / Security
- [ ] Task 05 — Farmer module implementation
