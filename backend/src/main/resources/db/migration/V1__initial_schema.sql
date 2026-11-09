-- ============================================================================
-- Mandiri Transport — Complete Operational & Financial Schema (Phases 1–6)
-- Compatible with Supabase PostgreSQL 16+ and H2 (PostgreSQL Mode)
-- ============================================================================

-- 1. Identity & Role-Based Access
CREATE TABLE app_users (
    id              VARCHAR(36) PRIMARY KEY,
    supabase_uid    VARCHAR(100) UNIQUE,
    username        VARCHAR(80) NOT NULL UNIQUE,
    full_name       VARCHAR(150) NOT NULL,
    phone           VARCHAR(40),
    role            VARCHAR(30) NOT NULL, -- OWNER, DRIVER
    fcm_token       VARCHAR(512),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP WITH TIME ZONE,
    version         BIGINT NOT NULL DEFAULT 0
);

-- 2. Customers, Addresses, Shared Operational Notes & WhatsApp Templates
CREATE TABLE customers (
    id                      VARCHAR(36) PRIMARY KEY,
    name                    VARCHAR(150) NOT NULL,
    phone                   VARCHAR(40) NOT NULL,
    whatsapp_phone          VARCHAR(40),
    company_name            VARCHAR(150),
    preferred_vehicle_type  VARCHAR(40),
    is_active               BOOLEAN NOT NULL DEFAULT TRUE,
    created_by              VARCHAR(36),
    updated_by              VARCHAR(36),
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at              TIMESTAMP WITH TIME ZONE,
    version                 BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_customers_phone ON customers(phone);
CREATE INDEX idx_customers_name ON customers(name);

CREATE TABLE customer_addresses (
    id              VARCHAR(36) PRIMARY KEY,
    customer_id     VARCHAR(36) NOT NULL REFERENCES customers(id),
    label           VARCHAR(80) NOT NULL,
    address_text    TEXT NOT NULL,
    latitude        NUMERIC(10, 7),
    longitude       NUMERIC(10, 7),
    google_maps_url TEXT,
    is_default      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_cust_addr_customer ON customer_addresses(customer_id);

CREATE TABLE customer_notes (
    id                  VARCHAR(36) PRIMARY KEY,
    customer_id         VARCHAR(36) NOT NULL REFERENCES customers(id),
    category            VARCHAR(40) NOT NULL DEFAULT 'OPERATIONAL', -- PREFERENCE, OPERATIONAL, LUGGAGE, TIMING
    note_text           TEXT NOT NULL,
    visible_to_drivers  BOOLEAN NOT NULL DEFAULT TRUE,
    created_by          VARCHAR(36),
    updated_by          VARCHAR(36),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at          TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_cust_notes_customer ON customer_notes(customer_id);

CREATE TABLE whatsapp_templates (
    id              VARCHAR(36) PRIMARY KEY,
    code            VARCHAR(60) NOT NULL UNIQUE,
    title           VARCHAR(120) NOT NULL,
    category        VARCHAR(40) NOT NULL,
    body_template   TEXT NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP WITH TIME ZONE
);

-- 3. Drivers & Availability Windows
CREATE TABLE drivers (
    id                  VARCHAR(36) PRIMARY KEY,
    user_id             VARCHAR(36) UNIQUE REFERENCES app_users(id),
    driver_code         VARCHAR(30) NOT NULL UNIQUE,
    full_name           VARCHAR(150) NOT NULL,
    phone               VARCHAR(40) NOT NULL,
    license_number      VARCHAR(60),
    license_expiry_date DATE,
    availability_status VARCHAR(40) NOT NULL DEFAULT 'AVAILABLE', -- AVAILABLE, UNAVAILABLE, ON_LEAVE, TEMPORARILY_UNAVAILABLE
    max_daily_hours     NUMERIC(5, 2) NOT NULL DEFAULT 10.00,
    notes               TEXT,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at          TIMESTAMP WITH TIME ZONE,
    version             BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE driver_unavailability_windows (
    id          VARCHAR(36) PRIMARY KEY,
    driver_id   VARCHAR(36) NOT NULL REFERENCES drivers(id),
    status_type VARCHAR(40) NOT NULL DEFAULT 'ON_LEAVE',
    reason      TEXT,
    start_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    end_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by  VARCHAR(36),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at  TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_driver_unavail_window ON driver_unavailability_windows(driver_id, start_at, end_at);

-- 4. Vehicles, Documents, Maintenance Schedules, Logs & Non-Trip Vehicle Expenses
CREATE TABLE vehicles (
    id                      VARCHAR(36) PRIMARY KEY,
    registration_number     VARCHAR(30) NOT NULL UNIQUE,
    name_model              VARCHAR(120) NOT NULL,
    brand                   VARCHAR(60),
    manufacture_year        INTEGER,
    vehicle_type            VARCHAR(40) NOT NULL, -- MPV, SUV, SEDAN, HIACE_VAN
    passenger_capacity      INTEGER NOT NULL,
    luggage_capacity_bags   INTEGER NOT NULL DEFAULT 4,
    status                  VARCHAR(30) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, IN_MAINTENANCE, INACTIVE
    current_odometer_km     NUMERIC(12, 1) NOT NULL DEFAULT 0,
    fuel_type               VARCHAR(30) DEFAULT 'BENSIN',
    suitability_tags        TEXT, -- CSV e.g. AIRPORT,INTERCITY,CHARTER,VIP
    notes                   TEXT,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at              TIMESTAMP WITH TIME ZONE,
    version                 BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE vehicle_documents (
    id                      VARCHAR(36) PRIMARY KEY,
    vehicle_id              VARCHAR(36) NOT NULL REFERENCES vehicles(id),
    doc_type                VARCHAR(40) NOT NULL, -- ANNUAL_TAX_STNK, FIVE_YEAR_PLATE, INSURANCE, KIR
    doc_number              VARCHAR(80),
    issue_date              DATE,
    expiry_date             DATE NOT NULL,
    reminder_days_before    INTEGER NOT NULL DEFAULT 30,
    storage_path            TEXT,
    notes                   TEXT,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at              TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_vehicle_docs_expiry ON vehicle_documents(vehicle_id, expiry_date);

CREATE TABLE vehicle_maintenance_schedules (
    id                      VARCHAR(36) PRIMARY KEY,
    vehicle_id              VARCHAR(36) NOT NULL REFERENCES vehicles(id),
    maintenance_type        VARCHAR(40) NOT NULL, -- OIL_CHANGE, TIRE_REPLACEMENT, BATTERY_REPLACEMENT, GENERAL_SERVICE
    interval_km             NUMERIC(10, 1),
    interval_months         INTEGER,
    last_done_odometer_km   NUMERIC(12, 1),
    last_done_date          DATE,
    next_due_odometer_km    NUMERIC(12, 1),
    next_due_date           DATE,
    estimated_cost          NUMERIC(15, 2),
    is_active               BOOLEAN NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at              TIMESTAMP WITH TIME ZONE
);

CREATE TABLE vehicle_maintenance_logs (
    id                  VARCHAR(36) PRIMARY KEY,
    vehicle_id          VARCHAR(36) NOT NULL REFERENCES vehicles(id),
    maintenance_type    VARCHAR(40) NOT NULL, -- OIL_CHANGE, TIRE_REPLACEMENT, BATTERY_REPLACEMENT, GENERAL_SERVICE, REPAIR
    service_date        DATE NOT NULL,
    odometer_km         NUMERIC(12, 1),
    vendor_name         VARCHAR(150),
    cost_amount         NUMERIC(15, 2) NOT NULL DEFAULT 0,
    downtime_hours      NUMERIC(6, 1) NOT NULL DEFAULT 0,
    start_at            TIMESTAMP WITH TIME ZONE,
    end_at              TIMESTAMP WITH TIME ZONE,
    description         TEXT,
    receipt_path        TEXT,
    recorded_by         VARCHAR(36),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at          TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_vehicle_maint_vehicle_date ON vehicle_maintenance_logs(vehicle_id, service_date);

CREATE TABLE vehicle_expenses (
    id                  VARCHAR(36) PRIMARY KEY,
    vehicle_id          VARCHAR(36) NOT NULL REFERENCES vehicles(id),
    maintenance_log_id  VARCHAR(36) REFERENCES vehicle_maintenance_logs(id),
    category            VARCHAR(40) NOT NULL, -- SERVICE, REPAIR, OIL, TIRE, BATTERY, TAX, INSURANCE, OTHER
    amount              NUMERIC(15, 2) NOT NULL,
    expense_date        DATE NOT NULL,
    description         TEXT,
    receipt_path        TEXT,
    recorded_by         VARCHAR(36),
    is_voided           BOOLEAN NOT NULL DEFAULT FALSE,
    void_reason         TEXT,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_vehicle_expenses_veh_date ON vehicle_expenses(vehicle_id, expense_date);

-- 5. Routes & Multi-Tier Pricing Engine
CREATE TABLE routes (
    id                              VARCHAR(36) PRIMARY KEY,
    code                            VARCHAR(40) NOT NULL UNIQUE,
    name                            VARCHAR(150) NOT NULL,
    origin_name                     VARCHAR(120) NOT NULL,
    destination_name                VARCHAR(120) NOT NULL,
    typical_distance_km             NUMERIC(8, 2) NOT NULL,
    default_duration_min            INTEGER NOT NULL,
    default_prep_buffer_min         INTEGER NOT NULL DEFAULT 15,
    default_arrival_buffer_min      INTEGER NOT NULL DEFAULT 0,
    default_reposition_buffer_min   INTEGER NOT NULL DEFAULT 15,
    is_long_distance                BOOLEAN NOT NULL DEFAULT FALSE,
    required_rest_after_min         INTEGER NOT NULL DEFAULT 0,
    is_active                       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at                      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at                      TIMESTAMP WITH TIME ZONE
);

CREATE TABLE standard_pricing_configs (
    id                      VARCHAR(36) PRIMARY KEY,
    vehicle_type            VARCHAR(40) NOT NULL UNIQUE,
    base_fare               NUMERIC(15, 2) NOT NULL,
    per_km_rate             NUMERIC(15, 2) NOT NULL,
    hourly_charter_rate     NUMERIC(15, 2) NOT NULL,
    full_day_charter_rate   NUMERIC(15, 2) NOT NULL,
    minimum_fare            NUMERIC(15, 2) NOT NULL,
    is_active               BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE route_pricing_rules (
    id              VARCHAR(36) PRIMARY KEY,
    route_id        VARCHAR(36) NOT NULL REFERENCES routes(id),
    vehicle_type    VARCHAR(40),
    trip_type       VARCHAR(40),
    suggested_fare  NUMERIC(15, 2) NOT NULL,
    notes           TEXT,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_route_pricing_lookup ON route_pricing_rules(route_id, vehicle_type, is_active);

CREATE TABLE customer_pricing_agreements (
    id              VARCHAR(36) PRIMARY KEY,
    customer_id     VARCHAR(36) NOT NULL REFERENCES customers(id),
    agreement_name  VARCHAR(150) NOT NULL,
    route_id        VARCHAR(36) REFERENCES routes(id), -- NULL = Customer General Agreement
    vehicle_type    VARCHAR(40),
    pricing_mode    VARCHAR(40) NOT NULL DEFAULT 'FIXED_FARE', -- FIXED_FARE, PER_KM, HOURLY, FULL_DAY, DISCOUNT_PERCENT
    agreed_fare     NUMERIC(15, 2),
    per_km_rate     NUMERIC(15, 2),
    hourly_rate     NUMERIC(15, 2),
    full_day_rate   NUMERIC(15, 2),
    discount_percent NUMERIC(5, 2),
    effective_from  DATE,
    effective_to    DATE,
    conditions_note TEXT,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_by      VARCHAR(36),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_cust_pricing_lookup ON customer_pricing_agreements(customer_id, route_id, is_active);

-- 6. Booking Templates & Bookings (Strictly Separate from Trips & Payments)
CREATE TABLE booking_templates (
    id                      VARCHAR(36) PRIMARY KEY,
    name                    VARCHAR(150) NOT NULL,
    customer_id             VARCHAR(36) REFERENCES customers(id),
    route_id                VARCHAR(36) REFERENCES routes(id),
    trip_type               VARCHAR(40) NOT NULL,
    booking_source          VARCHAR(40) DEFAULT 'RETURNING_CUSTOMER',
    pickup_address          TEXT NOT NULL,
    destination_address     TEXT NOT NULL,
    default_passenger_count INTEGER NOT NULL DEFAULT 1,
    default_luggage_count   INTEGER NOT NULL DEFAULT 1,
    luggage_notes           TEXT,
    preferred_vehicle_type  VARCHAR(40),
    notes                   TEXT,
    created_by              VARCHAR(36),
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at              TIMESTAMP WITH TIME ZONE
);

CREATE TABLE bookings (
    id                      VARCHAR(36) PRIMARY KEY,
    booking_code            VARCHAR(30) NOT NULL UNIQUE,
    customer_id             VARCHAR(36) NOT NULL REFERENCES customers(id),
    contact_name            VARCHAR(150) NOT NULL,
    contact_phone           VARCHAR(40) NOT NULL,
    booking_source          VARCHAR(40) NOT NULL DEFAULT 'WHATSAPP', -- WHATSAPP, PHONE_CALL, RETURNING_CUSTOMER, REFERRAL, OTHER
    trip_type               VARCHAR(40) NOT NULL, -- AIRPORT_PICKUP, AIRPORT_DROPOFF, LOCAL, INTERCITY, HOURLY_CHARTER, FULL_DAY_CHARTER, ROUND_TRIP, MULTI_STOP_CHARTER, CUSTOM
    route_id                VARCHAR(36) REFERENCES routes(id),
    requested_pickup_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    return_pickup_at        TIMESTAMP WITH TIME ZONE,
    charter_hours           INTEGER,
    pickup_address          TEXT NOT NULL,
    pickup_lat              NUMERIC(10, 7),
    pickup_lng              NUMERIC(10, 7),
    destination_address     TEXT NOT NULL,
    destination_lat         NUMERIC(10, 7),
    destination_lng         NUMERIC(10, 7),
    passenger_count         INTEGER NOT NULL DEFAULT 1,
    luggage_count           INTEGER NOT NULL DEFAULT 0,
    luggage_notes           TEXT,
    flight_number           VARCHAR(40),
    flight_scheduled_at     TIMESTAMP WITH TIME ZONE,
    suggested_fare          NUMERIC(15, 2),
    pricing_tier_used       VARCHAR(50),
    pricing_explanation     TEXT,
    agreed_fare             NUMERIC(15, 2) NOT NULL,
    special_instructions    TEXT,
    status                  VARCHAR(30) NOT NULL DEFAULT 'CONFIRMED', -- CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED
    created_by              VARCHAR(36),
    updated_by              VARCHAR(36),
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at              TIMESTAMP WITH TIME ZONE,
    version                 BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_bookings_customer ON bookings(customer_id);
CREATE INDEX idx_bookings_pickup_at ON bookings(requested_pickup_at);

-- 7. Trips (Independent Operational Execution Entity)
CREATE TABLE trips (
    id                          VARCHAR(36) PRIMARY KEY,
    trip_code                   VARCHAR(30) NOT NULL UNIQUE,
    booking_id                  VARCHAR(36) NOT NULL REFERENCES bookings(id),
    leg_number                  INTEGER NOT NULL DEFAULT 1,
    leg_label                   VARCHAR(60) DEFAULT 'Outbound',
    route_id                    VARCHAR(36) REFERENCES routes(id),
    trip_type                   VARCHAR(40) NOT NULL,
    driver_id                   VARCHAR(36) REFERENCES drivers(id),
    vehicle_id                  VARCHAR(36) REFERENCES vehicles(id),
    scheduled_pickup_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    estimated_duration_min      INTEGER NOT NULL DEFAULT 60,
    prep_buffer_min             INTEGER NOT NULL DEFAULT 15,
    arrival_buffer_min          INTEGER NOT NULL DEFAULT 0,
    reposition_buffer_min       INTEGER NOT NULL DEFAULT 15,
    reserved_start_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    reserved_end_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    pickup_address              TEXT NOT NULL,
    pickup_lat                  NUMERIC(10, 7),
    pickup_lng                  NUMERIC(10, 7),
    destination_address         TEXT NOT NULL,
    destination_lat             NUMERIC(10, 7),
    destination_lng             NUMERIC(10, 7),
    passenger_count             INTEGER NOT NULL DEFAULT 1,
    luggage_count               INTEGER NOT NULL DEFAULT 0,
    flight_number               VARCHAR(40),
    agreed_fare_snapshot        NUMERIC(15, 2) NOT NULL, -- Immutable historical fare snapshot
    status                      VARCHAR(30) NOT NULL DEFAULT 'UNASSIGNED', -- UNASSIGNED, ASSIGNED, ACCEPTED, ON_THE_WAY, ARRIVED, PASSENGER_ONBOARD, COMPLETED, CANCELLED, NO_SHOW, REASSIGNED
    cancel_or_outcome_reason    TEXT,
    conflict_override_note      TEXT,
    tracking_active             BOOLEAN NOT NULL DEFAULT FALSE,
    tracking_started_at         TIMESTAMP WITH TIME ZONE,
    tracking_ended_at           TIMESTAMP WITH TIME ZONE,
    last_gps_recorded_at        TIMESTAMP WITH TIME ZONE,
    last_gps_lat                NUMERIC(10, 7),
    last_gps_lng                NUMERIC(10, 7),
    gps_distance_km             NUMERIC(10, 2),
    odometer_start_km           NUMERIC(12, 1),
    odometer_end_km             NUMERIC(12, 1),
    actual_started_at           TIMESTAMP WITH TIME ZONE,
    actual_completed_at         TIMESTAMP WITH TIME ZONE,
    actual_duration_min         INTEGER,
    completion_notes            TEXT,
    payment_status              VARCHAR(30) NOT NULL DEFAULT 'UNPAID', -- UNPAID, PARTIALLY_PAID, PAID, OVERPAID, REFUNDED
    total_paid_amount           NUMERIC(15, 2) NOT NULL DEFAULT 0,
    cash_custody_status         VARCHAR(35) NOT NULL DEFAULT 'NO_DRIVER_CASH_DUE', -- NO_DRIVER_CASH_DUE, HELD_BY_DRIVER, PARTIALLY_HANDED_OVER, HANDED_OVER_CONFIRMED, DISPUTED
    driver_cash_held_amount     NUMERIC(15, 2) NOT NULL DEFAULT 0,
    created_by                  VARCHAR(36),
    updated_by                  VARCHAR(36),
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at                  TIMESTAMP WITH TIME ZONE,
    version                     BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_trips_driver_window ON trips(driver_id, reserved_start_at, reserved_end_at);
CREATE INDEX idx_trips_vehicle_window ON trips(vehicle_id, reserved_start_at, reserved_end_at);
CREATE INDEX idx_trips_pickup_status ON trips(scheduled_pickup_at, status);
CREATE INDEX idx_trips_booking ON trips(booking_id);

CREATE TABLE fare_override_audits (
    id                  VARCHAR(36) PRIMARY KEY,
    booking_id          VARCHAR(36) REFERENCES bookings(id),
    trip_id             VARCHAR(36) REFERENCES trips(id),
    suggested_fare      NUMERIC(15, 2),
    pricing_tier_used   VARCHAR(50),
    final_agreed_fare   NUMERIC(15, 2) NOT NULL,
    override_reason     TEXT,
    authorized_by       VARCHAR(36),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE trip_stops (
    id                  VARCHAR(36) PRIMARY KEY,
    trip_id             VARCHAR(36) NOT NULL REFERENCES trips(id),
    stop_order          INTEGER NOT NULL,
    stop_address        TEXT NOT NULL,
    latitude            NUMERIC(10, 7),
    longitude           NUMERIC(10, 7),
    planned_arrival_at  TIMESTAMP WITH TIME ZONE,
    notes               TEXT,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE trip_status_history (
    id                  VARCHAR(36) PRIMARY KEY,
    trip_id             VARCHAR(36) NOT NULL REFERENCES trips(id),
    from_status         VARCHAR(30),
    to_status           VARCHAR(30) NOT NULL,
    changed_by          VARCHAR(36),
    client_mutation_id  VARCHAR(80),
    reason_notes        TEXT,
    recorded_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_trip_status_hist_trip ON trip_status_history(trip_id, recorded_at);

CREATE TABLE trip_gps_points (
    id              VARCHAR(36) PRIMARY KEY,
    trip_id         VARCHAR(36) NOT NULL REFERENCES trips(id),
    client_point_id VARCHAR(80) NOT NULL UNIQUE,
    latitude        NUMERIC(10, 7) NOT NULL,
    longitude       NUMERIC(10, 7) NOT NULL,
    speed_kmh       NUMERIC(6, 2),
    accuracy_m      NUMERIC(6, 2),
    recorded_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    synced_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_trip_gps_trip_time ON trip_gps_points(trip_id, recorded_at);

CREATE TABLE vehicle_inspections (
    id                  VARCHAR(36) PRIMARY KEY,
    trip_id             VARCHAR(36) REFERENCES trips(id),
    vehicle_id          VARCHAR(36) NOT NULL REFERENCES vehicles(id),
    driver_id           VARCHAR(36) NOT NULL REFERENCES drivers(id),
    client_mutation_id  VARCHAR(80) UNIQUE,
    stage               VARCHAR(20) NOT NULL DEFAULT 'PRE_TRIP',
    all_passed          BOOLEAN NOT NULL DEFAULT TRUE,
    flagged_items_json  TEXT,
    photo_paths_json    TEXT,
    notes               TEXT,
    inspected_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE lost_and_found_items (
    id                  VARCHAR(36) PRIMARY KEY,
    trip_id             VARCHAR(36) NOT NULL REFERENCES trips(id),
    vehicle_id          VARCHAR(36) REFERENCES vehicles(id),
    reported_by         VARCHAR(36) NOT NULL,
    client_mutation_id  VARCHAR(80) UNIQUE,
    item_description    TEXT NOT NULL,
    found_date          DATE NOT NULL,
    photo_path          TEXT,
    status              VARCHAR(30) NOT NULL DEFAULT 'FOUND', -- FOUND, CONTACTING_CUSTOMER, RETURNED, STORED, UNRESOLVED
    notes               TEXT,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 8. Payments, Cash Custody & Handovers (Strictly Financial Accountability)
CREATE TABLE trip_payments (
    id                      VARCHAR(36) PRIMARY KEY,
    trip_id                 VARCHAR(36) NOT NULL REFERENCES trips(id),
    client_mutation_id      VARCHAR(80) UNIQUE,
    amount                  NUMERIC(15, 2) NOT NULL,
    payment_method          VARCHAR(30) NOT NULL, -- CASH, BANK_TRANSFER, OTHER
    is_refund               BOOLEAN NOT NULL DEFAULT FALSE,
    collected_by_user_id    VARCHAR(36) NOT NULL,
    collected_by_role       VARCHAR(20) NOT NULL, -- DRIVER, OWNER
    held_by_driver_id       VARCHAR(36) REFERENCES drivers(id),
    handed_over_amount      NUMERIC(15, 2) NOT NULL DEFAULT 0,
    custody_settled         BOOLEAN NOT NULL DEFAULT TRUE,
    paid_at                 TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reference_number        VARCHAR(80),
    receipt_path            TEXT,
    notes                   TEXT,
    is_voided               BOOLEAN NOT NULL DEFAULT FALSE,
    voided_by               VARCHAR(36),
    void_reason             TEXT,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_trip_payments_trip ON trip_payments(trip_id);
CREATE INDEX idx_trip_payments_driver_custody ON trip_payments(held_by_driver_id, custody_settled);

CREATE TABLE cash_handovers (
    id                      VARCHAR(36) PRIMARY KEY,
    handover_code           VARCHAR(30) NOT NULL UNIQUE,
    driver_id               VARCHAR(36) NOT NULL REFERENCES drivers(id),
    received_by_owner_id    VARCHAR(36) REFERENCES app_users(id),
    declared_amount         NUMERIC(15, 2) NOT NULL,
    confirmed_amount        NUMERIC(15, 2) NOT NULL DEFAULT 0,
    status                  VARCHAR(35) NOT NULL DEFAULT 'PENDING_CONFIRMATION', -- PENDING_CONFIRMATION, PARTIALLY_HANDED_OVER, HANDED_OVER_CONFIRMED, DISPUTED
    driver_notes            TEXT,
    owner_notes             TEXT,
    handed_over_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed_at            TIMESTAMP WITH TIME ZONE,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version                 BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE cash_handover_items (
    id              VARCHAR(36) PRIMARY KEY,
    handover_id     VARCHAR(36) NOT NULL REFERENCES cash_handovers(id),
    trip_payment_id VARCHAR(36) NOT NULL REFERENCES trip_payments(id),
    trip_id         VARCHAR(36) NOT NULL REFERENCES trips(id),
    amount_applied  NUMERIC(15, 2) NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 9. Trip Expenses & Versioned Operating-Cost Profitability Engine
CREATE TABLE trip_expenses (
    id                  VARCHAR(36) PRIMARY KEY,
    trip_id             VARCHAR(36) NOT NULL REFERENCES trips(id),
    client_mutation_id  VARCHAR(80) UNIQUE,
    category            VARCHAR(40) NOT NULL, -- FUEL, TOLL, PARKING, DRIVER_MEAL, OTHER
    amount              NUMERIC(15, 2) NOT NULL,
    paid_by_user_id     VARCHAR(36) NOT NULL,
    paid_by_role        VARCHAR(20) NOT NULL,
    fuel_liters         NUMERIC(8, 2),
    receipt_path        TEXT,
    notes               TEXT,
    incurred_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_voided           BOOLEAN NOT NULL DEFAULT FALSE,
    voided_by           VARCHAR(36),
    void_reason         TEXT,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_trip_expenses_trip ON trip_expenses(trip_id);

CREATE TABLE cost_formula_versions (
    id              VARCHAR(36) PRIMARY KEY,
    version_number  INTEGER NOT NULL UNIQUE,
    name            VARCHAR(150) NOT NULL,
    effective_from  DATE NOT NULL,
    effective_to    DATE,
    notes           TEXT,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_by      VARCHAR(36),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE cost_formula_components (
    id                      VARCHAR(36) PRIMARY KEY,
    formula_version_id      VARCHAR(36) NOT NULL REFERENCES cost_formula_versions(id),
    component_code          VARCHAR(50) NOT NULL, -- FUEL, TOLL, PARKING, TIRE_WEAR, ENGINE_OIL, MAINTENANCE_RESERVE, DEPRECIATION, DRIVER_COMPENSATION, OTHER
    name                    VARCHAR(120) NOT NULL,
    calc_type               VARCHAR(40) NOT NULL, -- FIXED_PER_TRIP, PER_KM, PERCENT_OF_FARE, HOURLY_ALLOCATION, MONTHLY_ALLOCATION, FORMULA_ALLOCATION
    reconciliation_policy   VARCHAR(40) NOT NULL, -- ACTUAL_EXPENSE_ONLY, ALLOCATED_ONLY, ACTUAL_OVERRIDES_ESTIMATE
    expense_category_link   VARCHAR(40), -- Links to trip_expenses.category when ACTUAL_OVERRIDES_ESTIMATE or ACTUAL_EXPENSE_ONLY
    fixed_amount            NUMERIC(15, 2),
    rate_value              NUMERIC(15, 4),
    monthly_amount          NUMERIC(15, 2),
    assumed_monthly_trips   INTEGER,
    expression_formula      TEXT, -- Safe math AST expression e.g. (replacement_cost / expected_lifespan_km) * distance_km
    parameters_json         TEXT, -- JSON map of variables e.g. {"replacement_cost":4000000,"expected_lifespan_km":50000}
    vehicle_type_filter     VARCHAR(40),
    sort_order              INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_cost_comp_version ON cost_formula_components(formula_version_id);

CREATE TABLE trip_cost_snapshots (
    id                          VARCHAR(36) PRIMARY KEY,
    trip_id                     VARCHAR(36) NOT NULL UNIQUE REFERENCES trips(id),
    formula_version_id          VARCHAR(36) NOT NULL REFERENCES cost_formula_versions(id),
    formula_version_number      INTEGER NOT NULL,
    distance_km_used            NUMERIC(10, 2) NOT NULL,
    distance_source             VARCHAR(30) NOT NULL, -- ODOMETER, GPS, ROUTE_DEFAULT
    duration_hours_used         NUMERIC(8, 2) NOT NULL,
    agreed_fare_snapshot        NUMERIC(15, 2) NOT NULL,
    estimated_operating_cost    NUMERIC(15, 2) NOT NULL,
    actual_expenses_total       NUMERIC(15, 2) NOT NULL,
    allocated_operating_cost    NUMERIC(15, 2) NOT NULL,
    total_effective_cost        NUMERIC(15, 2) NOT NULL,
    estimated_profit            NUMERIC(15, 2) NOT NULL,
    actual_profit               NUMERIC(15, 2) NOT NULL,
    calculated_at               TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE trip_cost_snapshot_items (
    id                      VARCHAR(36) PRIMARY KEY,
    snapshot_id             VARCHAR(36) NOT NULL REFERENCES trip_cost_snapshots(id),
    component_code          VARCHAR(50) NOT NULL,
    component_name          VARCHAR(120) NOT NULL,
    reconciliation_policy   VARCHAR(40) NOT NULL,
    estimated_amount        NUMERIC(15, 2) NOT NULL,
    actual_expense_amount   NUMERIC(15, 2) NOT NULL,
    effective_cost_amount   NUMERIC(15, 2) NOT NULL,
    calculation_detail      TEXT
);

-- 10. Notifications, Anomaly Rules, Emergency Alerts, Offline Sync & Audit Logs
CREATE TABLE notification_settings (
    id                      VARCHAR(36) PRIMARY KEY,
    reminder_type           VARCHAR(50) NOT NULL UNIQUE, -- UPCOMING_PICKUP, DRIVER_ASSIGNED, DRIVER_NOT_STARTED, OVERDUE_TRIP, VEHICLE_TAX_EXPIRY, INSURANCE_EXPIRY, SCHEDULED_MAINTENANCE, OUTSTANDING_CASH_HANDOVER
    enabled                 BOOLEAN NOT NULL DEFAULT TRUE,
    lead_time_minutes       INTEGER NOT NULL DEFAULT 60,
    recipient_scope         VARCHAR(20) NOT NULL DEFAULT 'BOTH', -- OWNER, DRIVER, BOTH
    acknowledgement_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE notifications (
    id                      VARCHAR(36) PRIMARY KEY,
    recipient_user_id       VARCHAR(36) REFERENCES app_users(id),
    recipient_role          VARCHAR(20),
    reminder_type           VARCHAR(50) NOT NULL,
    reference_type          VARCHAR(40),
    reference_id            VARCHAR(36),
    title                   VARCHAR(200) NOT NULL,
    body                    TEXT NOT NULL,
    scheduled_for           TIMESTAMP WITH TIME ZONE NOT NULL,
    dispatched_at           TIMESTAMP WITH TIME ZONE,
    status                  VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED', -- SCHEDULED, SENT, CANCELLED
    is_read                 BOOLEAN NOT NULL DEFAULT FALSE,
    acknowledged_at         TIMESTAMP WITH TIME ZONE,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_recipient ON notifications(recipient_user_id, status, scheduled_for);
CREATE INDEX idx_notifications_ref ON notifications(reference_type, reference_id, status);

CREATE TABLE anomaly_rules (
    id                      VARCHAR(36) PRIMARY KEY,
    rule_code               VARCHAR(60) NOT NULL UNIQUE,
    name                    VARCHAR(150) NOT NULL,
    enabled                 BOOLEAN NOT NULL DEFAULT TRUE,
    threshold_multiplier    NUMERIC(8, 3),
    threshold_absolute      NUMERIC(15, 2),
    min_sample_size         INTEGER NOT NULL DEFAULT 5,
    description             TEXT,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE emergency_alerts (
    id                  VARCHAR(36) PRIMARY KEY,
    driver_id           VARCHAR(36) NOT NULL REFERENCES drivers(id),
    trip_id             VARCHAR(36) REFERENCES trips(id),
    vehicle_id          VARCHAR(36) REFERENCES vehicles(id),
    latitude            NUMERIC(10, 7),
    longitude           NUMERIC(10, 7),
    message             TEXT,
    status              VARCHAR(30) NOT NULL DEFAULT 'OPEN', -- OPEN, ACKNOWLEDGED, RESOLVED
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at         TIMESTAMP WITH TIME ZONE
);

CREATE TABLE sync_mutations_log (
    client_mutation_id  VARCHAR(80) PRIMARY KEY,
    actor_user_id       VARCHAR(36) NOT NULL,
    mutation_type       VARCHAR(50) NOT NULL,
    entity_id           VARCHAR(36),
    outcome_status      VARCHAR(30) NOT NULL, -- APPLIED, CONFLICT_STAGED, DUPLICATE_IGNORED
    response_json       TEXT,
    processed_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sync_conflict_records (
    id                  VARCHAR(36) PRIMARY KEY,
    client_mutation_id  VARCHAR(80) NOT NULL,
    actor_user_id       VARCHAR(36) NOT NULL,
    entity_type         VARCHAR(40) NOT NULL,
    entity_id           VARCHAR(36) NOT NULL,
    mutation_type       VARCHAR(50) NOT NULL,
    expected_version    BIGINT,
    actual_version      BIGINT,
    client_payload_json TEXT NOT NULL,
    server_state_json   TEXT,
    conflict_reason     TEXT NOT NULL,
    resolution_status   VARCHAR(30) NOT NULL DEFAULT 'NEEDS_REVIEW', -- NEEDS_REVIEW, ACCEPTED_SERVER, MERGED_MANUALLY
    resolved_by         VARCHAR(36),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at         TIMESTAMP WITH TIME ZONE
);

CREATE TABLE audit_logs (
    id              VARCHAR(36) PRIMARY KEY,
    actor_user_id   VARCHAR(36),
    actor_role      VARCHAR(30),
    entity_type     VARCHAR(50) NOT NULL,
    entity_id       VARCHAR(36) NOT NULL,
    action          VARCHAR(50) NOT NULL,
    summary         TEXT,
    before_json     TEXT,
    after_json      TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_entity ON audit_logs(entity_type, entity_id, created_at);

