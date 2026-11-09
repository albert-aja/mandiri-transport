# Mandiri Transport — Internal Taxi Operations & Dispatch System

## 1. Project Overview & Core Philosophy

**Mandiri Transport** is a mobile-first, internal **Taxi Operations, Dispatch & Financial Accountability System** purpose-built for a family-owned transportation business in **Medan, Indonesia** handling Kualanamu International Airport (**KNO**) transfers, Medan city rides, intercity routes (**Berastagi, Kuala Tanjung, Tanjung Morawa**), and hourly/full-day/multi-stop charters.

> **Strict Scope Boundary (Internal Operations & Money Management Only — Not a Banking or Public Ride-Hailing App):**
> - **Purely money & accountability management (not a banking app):** This application does **not** execute bank transfers, integrate payment gateways, hold e-wallet balances, or perform banking activities. All payment, cash custody, expense, and profitability modules strictly record operational financial transactions, physical cash accountability between Driver and Owner, and business cost calculations.
> - **Internal manual dispatch (not public ride-hailing):** Zero public customer sign-up, zero public driver discovery, zero automated dispatch reassignments, and zero off-trip employee GPS surveillance. Every recommendation explains its deterministic reasoning while preserving final decision authority with the Owner/Dispatcher.

### Technology Stack
- **Mobile Application (`mobile/`):** Flutter 3.44 / Dart 3.12, Riverpod 3.5 (`Notifier<AppState>`), Material 3 high-contrast Bahasa Indonesia operational UI, `firebase_core` + `firebase_messaging` live background/foreground push integration (with conditional `google-services.json` Gradle auto-wiring & runtime `FirebaseOptions`), `SharedPreferences`-backed persistent offline database & idempotent outbox queue (`uuid`), `connectivity_plus` auto-sync, `geolocator` active-trip-only GPS capture, and `url_launcher` zero-cost Google Maps & WhatsApp `wa.me` deep links.
- **Backend (`backend/`):** Java 21, Spring Boot 3.4 modular monolith, MyBatis-Plus 3.5 (with optimistic locking & soft deletion), Flyway SQL migrations (`V1`–`V3`), Google Service Account RS256 OAuth2 + FCM HTTP v1 live push gateway, Spring Security 6 stateless JWT (validating Supabase HS256 JWTs + local dev role switcher), and JUnit 5 unit & full-stack integration tests.
- **Database & Infrastructure:** Supabase PostgreSQL 16 (`supabase` profile + `docker-compose.yml` + `supabase/postgresql_optimizations.sql` GiST `tstzrange` indexes) and zero-setup PostgreSQL-compatible embedded file DB (`dev` profile) pre-seeded with realistic Medan operational data.

---

## 2. Implemented Features (Detailed Explanation)

### 2.1 Monorepo Foundation, Relational Schema & Medan Operational Seed Data
- **32-Table Normalized Relational Schema (`V1__initial_schema.sql`):** Maintains strict separation between **Bookings** (`bookings`, `booking_templates`, `fare_override_audits`), **Trips** (`trips`, `trip_stops`, `trip_status_history`, `trip_gps_points`, `vehicle_inspections`, `lost_and_found_items`), and **Financial Records** (`trip_payments`, `cash_handovers`, `cash_handover_items`, `trip_expenses`, `vehicle_expenses`, `cost_formula_versions`, `cost_formula_components`, `trip_cost_snapshots`, `trip_cost_snapshot_items`). All mutable operational entities carry audit metadata, soft-delete (`deleted_at`), and `version` columns; financial records are never hard-deleted.
- **PostgreSQL Buffered Window Indexing (`supabase/postgresql_optimizations.sql`):** Provides `btree_gist` partial indexes over `tstzrange(reserved_start_at, reserved_end_at, '[)')` separately on `driver_id` and `vehicle_id` for sub-5ms conflict checks.
- **Authentic Medan Seed Dataset (`V2__seed_medan_operational_data.sql` & `medan_seed_data.dart`):** Pre-populates Owner (`Pak Hendra`), 4 Medan Drivers (`Bang Budi`, `Bang Andi`, `Bang Rudi`, `Bang Dedi`), 4 Vehicles (Innova Reborn, Avanza Veloz, Hiace Commuter, Fortuner VRZ), Medan/KNO/Berastagi/Kuala Tanjung/Tanjung Morawa routes, negotiated customer agreements (Ibu Ratna, PT Sawit Makmur, Ko Ahong), Versioned Cost Formulas (`v1` & `v2`), and historical + upcoming trips.

### 2.2 Authentication, Role-Based Access Control (RBAC) & Auditability
- **Dual-Mode Stateless JWT Security (`auth` module):** Validates cloud Supabase HS256 JWTs (`sub` -> `app_users.supabase_uid`) in production and supports instant 1-tap role switching (`/api/v1/auth/dev-login`) in local development.
- **Strict Role Isolation (`OWNER` vs `DRIVER`):**
  - `OWNER` has full access to bookings, dispatch, pricing, fleet, cash handover confirmation, cost formulas, and Business Intelligence.
  - `DRIVER` is strictly scoped at the SQL/service layer to trips assigned to their `driverId`, can read shared customer notes (`visible_to_drivers = true`) without edit permissions, and can record field updates (status, active GPS, optional inspections, customer payments, trip expenses, lost-and-found) on their own trips.
- **Comprehensive Audit Trail (`AuditService`):** Logs actor, role, entity, before/after state, fare overrides, payment voids, and handover confirmations in `audit_logs`.

### 2.3 Customer Management, Shared Preferences & Zero-Cost Communication
- **Customer Profiles (`customer` module & `owner_customers_tab.dart`):** Stores customer contacts, company affiliations, preferred vehicle types, booking history, negotiated pricing agreements, and saved frequent addresses (auto-linked to zero-cost Google Maps URLs).
- **Operational Customer Notes:** Categorized free-text notes (`PREFERENCE`, `OPERATIONAL`, `LUGGAGE`, `TIMING` — e.g., *"2 koper besar, kabin bebas bau rokok"*). Visible read-only on the Driver's trip card; editable exclusively by `OWNER`.
- **Reusable WhatsApp Message Templates (`WhatsappTemplateController` & Mobile Picker):** Interpolates `{customer_name}`, `{pickup_time}`, `{pickup_address}`, `{destination_address}`, `{driver_name}`, `{driver_phone}`, `{vehicle_plate}`, `{vehicle_model}`, and `{agreed_fare}` into click-to-chat `https://wa.me/62...` deep links. Never sends messages automatically.

### 2.4 Driver & Vehicle Operations Management
- **Independent Driver Management (`driver` module):** Tracks license expiries, maximum daily target hours, independent availability states (`AVAILABLE`, `UNAVAILABLE`, `ON_LEAVE`, `TEMPORARILY_UNAVAILABLE`), and scheduled leave/unavailability windows (`driver_unavailability_windows`).
- **Focused Vehicle Management (`vehicle` module):** Tracks registration plates (`BK ...`), capacity (passengers & luggage bags), odometer, suitability tags, document expiration reminders (`ANNUAL_TAX_STNK`, `FIVE_YEAR_PLATE`, `INSURANCE`, `KIR`), recurring maintenance schedules, and service/repair logs (`OIL_CHANGE`, `TIRE_REPLACEMENT`, `BATTERY_REPLACEMENT`, `GENERAL_SERVICE`, `REPAIR`) that atomically update vehicle odometer, next service due thresholds, and non-trip `vehicle_expenses`.

### 2.5 Quick Booking (30–60s), Multi-Leg Trips, Multi-Stop Charters & Templates
- **30–60 Second Quick Booking (`BookingService` & `quick_booking_sheet.dart`):** Requires only minimal inputs (Customer selection or inline new customer name/phone, Pickup, Destination, Date/Time, Trip Type, Agreed Fare) with Rupiah thousand-dot formatting and quick denomination chips (`Pas Sesuai Tarif`, `+Rp50rb`, `+Rp100rb`).
- **Multi-Trip & Charter Support:** Supports `AIRPORT_PICKUP`, `AIRPORT_DROPOFF`, `LOCAL`, `INTERCITY`, `HOURLY_CHARTER`, `FULL_DAY_CHARTER`, `ROUND_TRIP` (automatically generating Outbound Leg 1 + Return Leg 2 and pairing reverse routes such as `MDN_KNO` <-> `KNO_MDN` with its 30-minute baggage collection buffer), `MULTI_STOP_CHARTER` (`trip_stops`), and `CUSTOM`.
- **Prefill-Only Booking Templates:** Saves frequent customer/route arrangements for 1-tap form prefilling without ever auto-scheduling trips.
- **Flight Metadata Readiness (`FlightInfoAdapter`):** Captures manual flight codes (e.g., `GA-190`) and scheduled arrival notes behind a clean adapter interface with zero external flight API cost or dependency.

### 2.6 Manual Dispatch & Buffered Schedule Conflict Engine
- **Buffered Reservation Windows (`DispatchService`):** Calculates `reserved_start_at = scheduled_pickup_at - prep_buffer_min` and `reserved_end_at = scheduled_pickup_at + estimated_duration_min + arrival_buffer_min + reposition_buffer_min` (plus mandatory post-trip rest buffers on long-distance intercity routes).
- **Independent Driver & Vehicle Conflict Checks:** Evaluates driver availability, leave windows, and overlapping driver reservations separately from vehicle maintenance status, service downtime, and overlapping vehicle reservations.
- **Non-Silent Conflict Protection:** Returns HTTP `409 SCHEDULE_CONFLICT` with the conflicting trip code, exact overlapping window, and ranked conflict-free alternatives unless the Owner explicitly confirms an override with a documented reason.

### 2.7 Multi-Model Pricing Engine & Immutable Fare Integrity
- **5-Tier Deterministic Priority Waterfall (`PricingEngineService`):**
  1. **Customer + Route Agreement** (matching customer, route, optional vehicle type, and effective/expiration dates).
  2. **Customer General Agreement** (customer-wide fixed, per-km, charter, or percentage discount agreement).
  3. **Route Pricing Rule** (standard route + vehicle type + trip type tariff).
  4. **Standard Calculation** (distance-based `base_fare + per_km * distance` or hourly/full-day charter rates).
  5. **Manual / Custom Negotiated Fare**.
- **Fare Snapshot Integrity:** Preserves both `suggested_fare` and `agreed_fare` on bookings, logs overrides to `fare_override_audits`, and writes an immutable `agreed_fare_snapshot` onto each `Trip` so future tariff changes never alter historical trip revenue.

### 2.8 Customer Payments Ledger & Separate Driver Cash Custody
- **Multi-Payment Bookkeeping (`FinanceService`):** Supports deposits, partial payments, full payments, overpayments, refunds, and Owner-only soft-voids (`is_voided`) across `CASH`, `BANK_TRANSFER`, and `OTHER` methods (`UNPAID`, `PARTIALLY_PAID`, `PAID`, `OVERPAID`, `REFUNDED`).
- **Orthogonal Driver Cash Custody & Handover Workflow:**
  - Cash collected by a Driver marks the trip financially `PAID` while setting physical custody to `HELD_BY_DRIVER` (`driver_cash_held_amount`).
  - Bank transfers or payments received directly by the Owner automatically stay `NO_DRIVER_CASH_DUE`.
  - Supports partial handovers (`PARTIALLY_HANDED_OVER`), full Owner confirmation (`HANDED_OVER_CONFIRMED`), and discrepancy review (`DISPUTED`) via `cash_handovers` and `cash_handover_items`.

### 2.9 Custom Versioned Operating-Cost & Profitability Engine
- **6 Component Calculation Modes (`CostEngineService` & `FormulaExpressionEvaluator`):** Supports `FIXED_PER_TRIP`, `PER_KM`, `PERCENT_OF_FARE`, `HOURLY_ALLOCATION`, `MONTHLY_ALLOCATION`, and `FORMULA_ALLOCATION` evaluated by a safe recursive-descent mathematical AST parser (supporting expressions like `(replacement_cost / expected_lifespan_km) * distance_km` for Tire Wear and `(service_cost / service_interval_km) * distance_km` for Engine Oil without `eval()` security risks).
- **Double-Counting Prevention:** Each component enforces a `reconciliation_policy`:
  - `ACTUAL_EXPENSE_ONLY` (e.g., Toll & Parking receipts).
  - `ALLOCATED_ONLY` (e.g., Tire wear, Oil allocation, Maintenance reserve, Depreciation, Driver % commission — kept separate from shop `vehicle_expenses`).
  - `ACTUAL_OVERRIDES_ESTIMATE` (e.g., Fuel — uses formula estimate prior to receipt entry, then switches to actual fuel receipt in Actual Profit while keeping Estimated Cost for variance analysis).
- **Formula Versioning & Reproducibility:** Locks immutable `trip_cost_snapshots` and line items against the `cost_formula_version` effective on the trip date so future formula revisions never alter historical profitability reports.

### 2.10 Road-Friendly Trip Execution, Active-Only GPS, Inspections, Lost & Found & SOS
- **Driver Road UX (`driver_shell.dart` & `airport_signboard_screen.dart`):** Features a thumb-zone single-tap status progression bar (`ASSIGNED` -> `ACCEPTED` -> `ON_THE_WAY` -> `ARRIVED` -> `PASSENGER_ONBOARD` -> `COMPLETED`) with a **5-second Undo SnackBar**, plus a **1-Tap Full-Screen Passenger Name Signboard** for KNO airport arrival gate pickups.
- **Privacy-Safe Active-Trip GPS Tracking (`TripService` & `AppController`):** GPS capture can only be started explicitly by the driver during active trip states and hard-stops immediately upon trip completion/cancellation. Displays `last_gps_recorded_at` staleness badges so cached coordinates are never mistaken for live locations, calculates Haversine `gps_distance_km`, and reconciles against optional start/end odometer readings.
- **Optional Non-Blocking Field Tools:** Includes 1-tap **"Semua Aman"** (`allPassed=true`) or itemized Vehicle Inspection, lightweight Lost & Found reporting (`FOUND`, `CONTACTING_CUSTOMER`, `RETURNED`, `STORED`, `UNRESOLVED`), optional completion notes, and a **1-Tap Emergency Assistance (SOS)** alert that logs coordinates and prepares an emergency WhatsApp link to the Owner.

### 2.11 Offline-First Mobile Persistence & Idempotent Synchronization
- **Local Cache & Outbox Engine (`offline_store.dart` & `SyncService`):** Caches assigned trips, customer contacts/notes, routes, and WhatsApp templates locally. Offline status updates, payments, expenses, inspections, lost-and-found reports, and active-trip GPS points queue locally with unique `clientMutationId` UUIDs and flush automatically when connectivity returns (with a persistent top Offline/Outbox sync pill).
- **Idempotency & Zero-Data-Loss Conflict Handling:** Server deduplicates retries via `sync_mutations_log(client_mutation_id)`. If an offline `TRIP_STATUS` update conflicts with a newer server `version` (e.g., concurrent Owner reassignment), the conflict is staged in `sync_conflict_records` (`CONFLICT_STAGED`) without overwriting server state, while 100% of driver-recorded financial payments, expenses, inspections, and notes are preserved.

### 2.12 Deterministic Smart Operations, BI Explorer & Configurable Reminders
- **Smart Operational Decision Support (Section 13):**
  - **13.1 Driver Workload Balancing:** Ranks available conflict-free drivers by daily reserved & active hours with plain-language explanations.
  - **13.2 Route Performance History:** Aggregates duration, distance, fuel, operating cost, fare, and net profit per route/vehicle/time-of-day, enforcing a `minSampleSize = 5` guard before presenting statistical estimates.
  - **13.3 Vehicle Suitability Suggestions:** Scores active, conflict-free vehicles against passenger count, luggage load, suitability tags, maintenance status, and customer vehicle preferences.
  - **13.4 Interactive Booking Conflict Simulator:** Previews buffered reservation windows, conflicts, and alternatives prior to dispatch confirmation.
  - **13.5 Customer Profitability Analysis:** Summarizes per-customer revenue, actual expenses, allocated costs, net profit, and frequency while treating negotiated relationship fares respectfully.
  - **13.6 Operational Anomaly Detection:** Evaluates configurable multiplier/absolute rules (`anomaly_rules` with minimum sample-size guards) for abnormal fuel burn, duration spikes, unexpected expenses, high maintenance, aged driver cash, and estimate variance — always explaining observed value, baseline, threshold, and neutral review hints without accusing misconduct.
  - **13.7 Vehicle Replacement Intelligence:** Tracks maintenance cost trends, cost/km, downtime hours, repair count, utilization, and net contribution (flagging high-cost aging units like `BK 4455 MT` with evidence).
- **Central Cross-Filtered Business Intelligence Explorer (`owner_bi_tab.dart` & `/api/v1/bi/explorer`):** Filters across date range, customer, driver, vehicle, and route to answer key operational and financial questions clearly.
- **Configurable Non-Blocking Notifications (`NotificationService`):** Supports pickup lead times (`15m`, `30m`, `1h`, `2h`, custom), recipient scopes (`OWNER`, `DRIVER`, `BOTH`), automatic rescheduling on pickup time edits, tax/insurance/KIR/maintenance/cash reminders, and optional non-blocking acknowledgement.

### 2.13 Live Firebase Cloud Messaging (FCM HTTP v1) & Multi-Device Push Delivery
- **Zero-Bloat Google OAuth2 RS256 + FCM HTTP v1 Gateway (`FirebaseHttpV1PushGateway.java`):** Implements native Java 21 PKCS#8 RSA (`SHA256withRSA`) JWT assertion signing against Google's OAuth2 token endpoint (`https://www.googleapis.com/auth/firebase.messaging`), thread-safe bearer token caching with automatic pre-expiry refresh, and live HTTP v1 push dispatch (`POST /v1/projects/{projectId}/messages:send`) configured with Android high-priority operational channel `mandiri_ops_high_importance`, APNs sound/headers, and structured deep-link `data` payloads (`notificationId`, `reminderType`, `referenceType`, `referenceId`). Automatically falls back to the in-app notification inbox (`SKIPPED_NO_CREDENTIALS`) when `firebase-service-account.json` or `FCM_SERVICE_ACCOUNT_JSON` is not yet provisioned.
- **Multi-Device Token Registry & Shared-Phone Safety (`V3__live_fcm_devices_and_delivery.sql`, `UserFcmDevice`, `AuthController`):**
  - Stores active device tokens per user (`ANDROID`, `IOS`, `WEB`, `OTHER`) in `user_fcm_devices` (`POST /api/v1/auth/fcm-token`) so an Owner using both an office tablet and personal phone receives pushes on all active devices simultaneously.
  - Automatically unbinds a token from any previous user when a shared garage phone switches accounts and explicitly deactivates the token on logout (`POST /api/v1/auth/fcm-token/unregister`) to prevent cross-driver notification leaks.
  - Detects `"UNREGISTERED"` / invalid token responses from FCM HTTP v1 and automatically marks stale device tokens `is_active = false` while recording `fcm_delivery_status`, `fcm_message_id`, and `fcm_error` on `notifications`.
- **Scheduled Operational Reminder Scanner & Live Diagnostics (`NotificationService` & `NotificationController`):**
  - Background `@Scheduled` scanner (`scanOperationalReminders`, also triggerable on-demand via `POST /api/v1/notifications/scan-reminders`) evaluates `notification_settings` with deduplication across upcoming trip pickups, expiring vehicle documents (`ANNUAL_TAX_STNK`, `INSURANCE`, `KIR`), due maintenance schedules (by date or remaining odometer km), and outstanding driver cash handovers (`driver_cash_held_amount > 0`).
  - Provides live readiness diagnostics (`GET /api/v1/notifications/fcm-status`) and end-to-end live test push (`POST /api/v1/notifications/test-push`).
- **Mobile Native Android/iOS FCM Wiring, Background Isolate & Foreground Banner (`fcm_push_service.dart`, `app_controller.dart`, `owner_bi_tab.dart`):**
  - Conditional Gradle Google Services plugin (`if (file("google-services.json").exists()) apply(plugin = "com.google.gms.google-services")` in `mobile/android/app/build.gradle.kts`) builds cleanly out-of-the-box and activates native Firebase resources automatically the moment `google-services.json` is dropped in `mobile/android/app/` (with ready templates `google-services.json.example`, `GoogleService-Info.plist.example`, and `firebase-service-account.json.example`, plus an in-app runtime `FirebaseOptions` configuration dialog).
  - Configures Android 13+ `POST_NOTIFICATIONS` permission, default high-importance channel `mandiri_ops_high_importance`, top-level `@pragma('vm:entry-point')` background isolate handler (`mandiriFirebaseMessagingBackgroundHandler`) that persists incoming pushes into `OfflineStore`, non-blocking foreground `LivePushBanner` on both Owner and Driver shells with 1-tap target screen routing, and an Owner **"Live Firebase Cloud Messaging (FCM) & Push Real-Time"** diagnostics + **"Uji Push FCM"** panel.

---

## 3. Project Todo List & Next-Horizon Recommendations

- [ ] **[Storage & Media] Client-Side Receipt Photo Compression & Supabase Storage Direct Upload:** Compress camera photos of toll/fuel/service receipts on the device (`<= 300 KB` JPEG/WebP) before uploading to Supabase Storage to save mobile data and free-tier storage quota.
- [ ] **[UI/UX] Shareable Corporate Trip Summary Sheet (PDF / Image):** Generate a clean, printable/WhatsApp-shareable trip & invoice statement for corporate clients (e.g., PT Sawit Makmur monthly billing recap) — strictly informational bookkeeping, no payment gateway links.
- [ ] **[UI/UX] Visual Daily Schedule Timeline (Gantt Strip) on Dispatch Tab:** Add a compact horizontal hour-by-hour bar (`05:00–24:00 WIB`) visualizing driver and vehicle buffered blocks side-by-side above the list view.
- [ ] **[UI/UX] Night / Early-Morning High-Comfort Dark Theme:** Add an optional dark theme toggle for drivers operating 04:00 WIB KNO airport drop-offs and late-night arrivals.
- [ ] **[Operations] CSV / Excel Bookkeeping Export:** Add 1-tap CSV export on the BI Explorer & Finance tabs so the owner can archive monthly trip, expense, and cash-handover ledgers offline.
- [ ] **[Security UX] Optional Local PIN Lock for Owner Financial & BI Tabs:** Allow the owner to set a quick 4-digit local PIN when handing an office tablet to an assistant dispatcher so booking/dispatch stays open while profitability margins stay private.
- [ ] **[Notifications UX] Quiet Hours / Shift-Aware PushFilter for Drivers on Leave:** Suppress non-emergency operational push notifications automatically for drivers marked `ON_LEAVE` or during configurable rest hours while preserving emergency SOS alerts.
- [ ] **[Performance] Incremental Delta Sync (`updatedSince` Cursor) on `/api/v1/trips` & `/api/v1/notifications`:** Fetch only records modified after `lastSyncedAt` to minimize mobile data usage on weak intercity cellular connections (Berastagi / Kuala Tanjung corridors).
