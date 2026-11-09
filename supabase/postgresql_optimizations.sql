-- Optional PostgreSQL / Supabase native range & GiST index optimizations
-- Run automatically or on Supabase SQL Editor after Flyway V1 migration completes.

CREATE EXTENSION IF NOT EXISTS btree_gist;

-- High-speed buffered reservation window conflict detection for Drivers (sub-5ms overlap queries)
CREATE INDEX IF NOT EXISTS idx_trips_driver_window_gist
    ON trips USING gist (driver_id, tstzrange(reserved_start_at, reserved_end_at, '[)'))
    WHERE deleted_at IS NULL
      AND driver_id IS NOT NULL
      AND status NOT IN ('CANCELLED', 'NO_SHOW', 'REASSIGNED', 'COMPLETED');

-- High-speed buffered reservation window conflict detection for Vehicles
CREATE INDEX IF NOT EXISTS idx_trips_vehicle_window_gist
    ON trips USING gist (vehicle_id, tstzrange(reserved_start_at, reserved_end_at, '[)'))
    WHERE deleted_at IS NULL
      AND vehicle_id IS NOT NULL
      AND status NOT IN ('CANCELLED', 'NO_SHOW', 'REASSIGNED', 'COMPLETED');

