# Mandiri Transport — Taxi Operations, Dispatch & Financial Accountability System

Internal mobile-first operations, manual dispatch, cash accountability, configurable operating-cost formula, and business intelligence platform built for a family-owned transportation business in **Medan, Indonesia** (Kualanamu Airport / KNO transfers, Medan local rides, Berastagi / Kuala Tanjung / Tanjung Morawa intercity routes, and hourly/full-day charters).

> **Scope Notice:** Internal operational & financial bookkeeping platform only. Contains **no** public customer registration, **no** public ride-hailing matching, **no** background employee surveillance outside active trips, and **no** payment gateway or banking transaction execution.

---

## Architecture & Stack

- **`backend/`** — Java 21, Spring Boot 3.4, MyBatis-Plus, Flyway SQL Migrations, Spring Security OAuth2 JWT (Supabase JWT + local dev role switcher), JUnit 5 test suite.
- **`mobile/`** — Flutter 3.44 / Dart 3.12, role-based UI (`OWNER` Dispatcher vs `DRIVER`), persistent offline cache & outbox synchronization (`client_mutation_id` idempotency + non-destructive conflict staging), active-trip-only GPS tracking, Google Maps navigation deep links, WhatsApp click-to-chat message templates.
- **`supabase/` & `docker-compose.yml`** — Supabase PostgreSQL 16 schema + `btree_gist` `tstzrange` buffered schedule-conflict indexes.

### External Service Cost Transparency
| Integration | Purpose | Cost Model |
|---|---|---|
| **Supabase (PostgreSQL + Auth + Storage)** | Primary relational database, JWT authentication, receipt/inspection photo storage | Free tier sufficient for small family fleet; Pro tier optional as storage grows |
| **Firebase Cloud Messaging (FCM)** | Push notifications for pickup, dispatch, tax/insurance, and cash-handover reminders | **$0** (free service) |
| **Google Maps URL Deep Links** | Turn-by-turn navigation & saved customer address pins (`https://www.google.com/maps/dir/?api=1...`) | **$0** (no paid Maps/Directions/Places API key required) |
| **WhatsApp Click-to-Chat (`wa.me`)** | Prefilled customer communication templates (`https://wa.me/62...`) confirmed manually by user | **$0** (no paid WhatsApp Business API required) |
| **Flight Data API** | Deferred by design; manual flight number & schedule notes supported via clean adapter | **$0** |

---

## Quick Start

### 1. Run Backend (`backend/`)

**Zero-Setup Local Dev Mode** (uses PostgreSQL-compatible embedded file DB pre-seeded with Medan routes, fleet, customers, cost formulas, and trips):
```powershell
cd backend
.\mvnw.cmd spring-boot:run
```
Backend listens on `http://localhost:8080`.

**Supabase / Docker PostgreSQL Mode:**
```powershell
docker compose up -d
cd backend
$env:SPRING_PROFILES_ACTIVE="supabase"
.\mvnw.cmd spring-boot:run
```
Or point `SUPABASE_DB_URL`, `SUPABASE_DB_USER`, `SUPABASE_DB_PASSWORD`, and `SUPABASE_JWT_SECRET` (see `.env.example`) to your cloud Supabase project.

**Run Backend Automated Tests:**
```powershell
cd backend
.\mvnw.cmd test
```

### 2. Run Mobile App (`mobile/`)

```powershell
cd mobile
flutter pub get
flutter run
```

**Run Mobile Analyzer & Automated Tests:**
```powershell
cd mobile
flutter analyze
flutter test
```

