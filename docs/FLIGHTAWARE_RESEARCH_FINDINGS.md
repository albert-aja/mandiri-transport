# FlightAware Research Module — Endpoint Behavior, Validation Report & Commercial/Technical Restrictions

> **Scope & Status:** Independent research and validation module (`com.mandiritransport.ops.flighttracking`). Separated from the production notification engine via the `FlightTrackingProvider` interface (`FlightAwareProviderAdapter`).

---

## 1. Architecture & Implemented Components

```text
Mandiri Transport
       |
       v
Flight Management (Bookings / Trips: flight_number, flight_scheduled_at)
       |
       v
FlightTrackingProvider (Provider-Agnostic Interface)
       |
       v
FlightAwareProviderAdapter
       |
       +---- FlightAwareTokenService       (HTML <script> + balanced JS object parser for trackpollGlobals)
       +---- FlightTokenCryptoService      (AES-256-GCM authenticated encryption + SHA-256 non-reversible fingerprint)
       +---- FlightAwareCredentialService  (Lifecycle state machine: UNKNOWN -> ACTIVE -> SUPERSEDED / EXPIRED / INVALID / DISABLED)
       +---- FlightAwarePollingService     (GET /ajax/trackpoll.rvt?token={TOKEN}&locale=en_US&summary=1)
       +---- FlightAwareDiffService        (Flattened JSON diff engine + operational significance classifier)
       +---- FlightDataValidationReportService (Section 9 13-point matrix + Section 10 Experiments A-E evaluator)
```

### Database Schema (`V4__flight_tracking_research_module.sql`)
1. **`flight_tracking_credentials`**:
   - Stores `id` (UUID), `flight_id` (FK -> `bookings.id`), `trip_id` (FK -> `trips.id`), `provider` (`FLIGHTAWARE`), `flight_number`, `flight_date`, `tracking_token_encrypted` (`AES/GCM/NoPadding`), `user_token_encrypted`, `has_user_token`, `token_fingerprint` (`sha256:...`), `polling_interval_seconds`, `single_flight`, `tracking_page_url`, `token_status` (`UNKNOWN`, `ACTIVE`, `SUPERSEDED`, `EXPIRED`, `INVALID`, `DISABLED`), `extracted_at`, `last_validated_at`, `last_used_at`, `expires_at` (nullable — populated only when expiration is evidenced), soft-delete (`deleted_at`), and optimistic lock (`version`).
   - Enforces at most one `ACTIVE` credential per `(provider, flight_number, flight_date)` while preserving full credential replacement history.
2. **`flight_tracking_poll_logs`**:
   - Stores `id`, `credential_id`, `request_timestamp`, `response_timestamp`, `http_status`, `response_duration_ms`, `response_valid`, `parsing_status`, `flight_identifier`, `payload_hash` (for retry deduplication), `response_payload` (raw JSON/text), `normalized_payload`, `discovered_fields_json`, `experiment_tag`, and `error_message`.

---

## 2. Observed Endpoint Behavior (`trackpollGlobals` & `trackpoll.rvt`)

### 2.1 `trackpollGlobals` Extraction Behavior
- **Location in DOM:** FlightAware public flight pages embed a `<script>` block declaring `var trackpollGlobals = { ... };`.
- **Other `TOKEN` Keys on Page:** Flight pages frequently contain unrelated analytics or configuration objects with `"TOKEN"` keys earlier in the HTML. `FlightAwareTokenService` specifically locates the `trackpollGlobals` assignment and uses a stateful balanced-brace JS object lexer (ignoring braces inside string literals and JS comments) rather than brittle substring indexing.
- **Observed Fields:**
  - `TOKEN`: Opaque hex/base64-like session token string bound to the flight tracking view.
  - `INTERVAL`: Integer polling interval in seconds (commonly `60`).
  - `SINGLE_FLIGHT`: Boolean (`true` on single-flight tracking pages).
  - `USERTOKEN`: Optional string/numeric identifier representing anonymous or logged-in viewer context.

### 2.2 `trackpoll.rvt` Polling Behavior
- **Request Format:** `GET https://www.flightaware.com/ajax/trackpoll.rvt?token={TOKEN}&locale=en_US&summary=1`
- **Response Structure (When Valid):**
  - Top-level JSON object containing a `"flights"` map keyed by FlightAware's internal flight instance ID (e.g., `"GIA190-1731196800-airline-01"`).
  - Inside each flight object, timestamps are typically represented as **UTC Unix epoch seconds** inside nested blocks (`gateDepartureTimes`, `takeoffTimes`, `gateArrivalTimes`, `landingTimes` with `scheduled`, `estimated`, `actual` keys), alongside `ident`, `flightStatus` / `phase`, `origin` (`iata`/`icao`), `destination` (`iata`/`icao`), `lastPosition` (`lat`, `lon`, `alt`, `gs`), `aircraft` (`type`, `tail`), `cancelled`, and `diverted`.
- **Failure Modes Distinguished by `FlightAwarePollingService` & `FlightAwareCredentialService`:**
  1. `ACCESS_RESTRICTED_OR_CHALLENGE`: HTTP `401`/`403`/`429`/`503` or HTML containing Cloudflare / CAPTCHA challenges. The service halts immediately without attempting any bypass.
  2. `NON_JSON_RESPONSE`: HTTP `200` returning an HTML redirect/expired-session page instead of JSON.
  3. `VALID_JSON_UNEXPECTED_SCHEMA`: Valid JSON lacking a populated `flights` object or flight identity.
  4. `VALID_TRACKING_JSON`: Valid JSON with a recognizable flight object. `FlightAwareCredentialService` additionally verifies that the returned flight identifier matches the expected flight (including Indonesian IATA <-> ICAO equivalence such as `GA190` <-> `GIA190`, `JT` <-> `LNI`, `ID` <-> `BTK`, `QG` <-> `CTV`) before marking a credential `ACTIVE`.

---

## 3. Section 9 — Flight Data Validation Matrix (13 Operational Requirements)

| Requirement | Importance | Observed JSON Path(s) in `trackpoll.rvt` | Data Type & Timezone | Dynamic Updates Across Polls? | Operational Assessment for KNO Dispatch |
|---|---|---|---|---|---|
| **Flight identification** | Critical | `flights.<key>.ident`, `displayIdent` | `STRING` (ICAO/IATA, e.g. `GIA190`) | Static per flight | **Supported** (requires IATA<->ICAO normalization `GA-190` <-> `GIA190`). |
| **Scheduled departure** | Critical | `flights.<key>.gateDepartureTimes.scheduled`, `takeoffTimes.scheduled` | `INTEGER` (`UTC_EPOCH_SECONDS`) | Static unless schedule revised | **Supported**. |
| **Estimated departure** | Critical | `flights.<key>.gateDepartureTimes.estimated`, `takeoffTimes.estimated` | `INTEGER` (`UTC_EPOCH_SECONDS`) | **Yes** (shifts with pre-departure delays) | **Supported**. |
| **Actual departure** | Critical | `flights.<key>.gateDepartureTimes.actual`, `takeoffTimes.actual` | `INTEGER` (`UTC_EPOCH_SECONDS`) | Populated once departed | **Conditional on flight phase** (null prior to pushback/takeoff). |
| **Flight status** | Critical | `flights.<key>.flightStatus`, `phase` | `STRING` (`Scheduled`, `En Route`, `Landed`) | **Yes** | **Supported**. |
| **Scheduled arrival** | Critical | `flights.<key>.gateArrivalTimes.scheduled`, `landingTimes.scheduled` | `INTEGER` (`UTC_EPOCH_SECONDS`) | Static baseline | **Supported** (primary baseline for KNO pickup buffer). |
| **Estimated arrival** | Critical | `flights.<key>.gateArrivalTimes.estimated`, `landingTimes.estimated` | `INTEGER` (`UTC_EPOCH_SECONDS`) | **Yes** (updates while airborne) | **Supported** (most critical field for adjusting driver dispatch to KNO). |
| **Actual arrival** | Critical | `flights.<key>.gateArrivalTimes.actual`, `landingTimes.actual` | `INTEGER` (`UTC_EPOCH_SECONDS`) | Populated upon landing/gate arrival | **Conditional on flight phase** (null until landing). |
| **Arrival delay** | High | `flights.<key>.gateArrivalTimes.delay` or derived `(estimated - scheduled)` | `INTEGER` (minutes / seconds) | **Yes** | **Supported** (directly or derived from `estimated - scheduled` epoch timestamps). |
| **Aircraft location** | Optional | `flights.<key>.lastPosition.{lat,lon,alt,gs}` | `NUMBER` (decimal degrees, feet, knots) | **Yes** (every poll while airborne) | **Supported while airborne**. |
| **Aircraft registration** | Optional | `flights.<key>.aircraft.tail` | `STRING` (e.g. `PK-GMA`) | Static per aircraft assignment | **Partially available** (depends on airline ADS-B / flight plan feed). |
| **Cancellation status** | High | `flights.<key>.cancelled` | `BOOLEAN` | **Yes** (if cancelled) | **Supported**. |
| **Diversion information** | High | `flights.<key>.diverted` | `BOOLEAN` | **Yes** (if diverted) | **Supported**. |

> **Important Validation Rule:** `FlightDataValidationReportService` never marks a requirement as `VERIFIED_MEANINGFUL` until actual recorded `flight_tracking_poll_logs` contain a non-null, meaningful value for that requirement.

---

## 4. Section 10 — Research Experiments (A–E) Capabilities & Findings

- **Experiment A (Token Reuse Without Browser Cookies):**
  - Tested by extracting `TOKEN` via `FlightAwareTokenService`, persisting encrypted in `flight_tracking_credentials`, and polling `trackpoll.rvt` from `FlightAwarePollingService` without sending any `Cookie` header.
  - **Finding:** When `trackpoll.rvt` accepts the signed `TOKEN` parameter directly, backend polling succeeds without browser session cookies; however, edge CDN/anti-bot rules can challenge automated non-browser traffic at any time.
- **Experiment B (Token Lifetime & Expiration):**
  - `FlightAwareCredentialService` tracks `extracted_at`, `last_validated_at`, `last_used_at`, and only populates `expires_at` when a previously `ACTIVE` credential fails validation (`ACTIVE -> EXPIRED`).
  - **Finding:** Tokens are short-lived and bound to the flight page window; they are **not** permanent API keys.
- **Experiment C (Flight Association & Cross-Flight Isolation):**
  - Tested by extracting credentials for multiple flights and verifying `verifyFlightIdentityMatch`.
  - **Finding:** Each `trackpollGlobals.TOKEN` is strictly scoped to the flight rendered on that tracking page (`SINGLE_FLIGHT: true`). Attempting to use a token from another flight is rejected by `FlightAwareCredentialService` (`token_status = INVALID`) and prevented from overwriting an existing `ACTIVE` credential.
- **Experiment D (Upcoming Flight Availability — 24h / 48h / 72h):**
  - **Finding:** Flights scheduled 24–72 hours ahead expose `scheduled` departure/arrival times, but `actual` times and `lastPosition` remain null until the flight is active.
- **Experiment E (Operational Lifecycle Updates):**
  - `FlightAwareDiffService` compares successive polls (`GET /api/v1/integrations/flightaware/credentials/{id}/diff-latest`) and highlights changes in `FLIGHT_STATUS`, `ESTIMATED_ARRIVAL`, `ACTUAL_ARRIVAL`, `ARRIVAL_DELAY`, and `AIRCRAFT_POSITION`.

---

## 5. Section 12 — Commercial & Technical Restrictions (Blockers Before Production)

1. **Undocumented Internal Endpoint:** `/ajax/trackpoll.rvt` is an internal web AJAX endpoint, not a versioned public REST API. FlightAware may alter field names, nesting, or token formats without notice.
2. **Terms of Service & Commercial Licensing:** FlightAware's public website Terms of Use prohibit automated scraping or commercial operational use without written permission. For production use in Mandiri Transport, the business should either:
   - Subscribe to **FlightAware AeroAPI** (official commercial REST API) and implement a `FlightTrackingProvider` adapter for AeroAPI, **or**
   - Continue using `ManualFlightInfoAdapter` (zero-cost manual flight number & schedule entry by the dispatcher) until volume justifies a commercial aviation API subscription.
3. **Zero Evasion Policy:** In compliance with project security guidelines, this module never bypasses CAPTCHAs, Cloudflare Turnstile challenges, rate limits, or authentication gates.

