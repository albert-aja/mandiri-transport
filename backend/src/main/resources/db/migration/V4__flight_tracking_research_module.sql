-- ============================================================================
-- V4: FlightAware Research Module — Persistent Encrypted Tracking Credentials
--     & Polling Response History
-- ============================================================================

CREATE TABLE flight_tracking_credentials (
    id                          VARCHAR(36) PRIMARY KEY,
    flight_id                   VARCHAR(36) REFERENCES bookings(id),
    trip_id                     VARCHAR(36) REFERENCES trips(id),
    provider                    VARCHAR(40) NOT NULL DEFAULT 'FLIGHTAWARE',
    flight_number               VARCHAR(40) NOT NULL,
    flight_date                 DATE NOT NULL,
    tracking_token_encrypted    TEXT NOT NULL,
    user_token_encrypted        TEXT,
    has_user_token              BOOLEAN NOT NULL DEFAULT FALSE,
    token_fingerprint           VARCHAR(64) NOT NULL,
    polling_interval_seconds    INTEGER,
    single_flight               BOOLEAN,
    tracking_page_url           TEXT NOT NULL,
    token_status                VARCHAR(30) NOT NULL DEFAULT 'UNKNOWN', -- ACTIVE, EXPIRED, INVALID, UNKNOWN, SUPERSEDED, DISABLED
    extracted_at                TIMESTAMP WITH TIME ZONE NOT NULL,
    last_validated_at           TIMESTAMP WITH TIME ZONE,
    last_used_at                TIMESTAMP WITH TIME ZONE,
    expires_at                  TIMESTAMP WITH TIME ZONE,
    validation_notes            TEXT,
    created_by                  VARCHAR(36),
    updated_by                  VARCHAR(36),
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at                  TIMESTAMP WITH TIME ZONE,
    version                     BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_ft_cred_flight_id ON flight_tracking_credentials(flight_id);
CREATE INDEX idx_ft_cred_flight_number ON flight_tracking_credentials(flight_number);
CREATE INDEX idx_ft_cred_flight_date ON flight_tracking_credentials(flight_date);
CREATE INDEX idx_ft_cred_token_status ON flight_tracking_credentials(token_status);
CREATE INDEX idx_ft_cred_provider_instance ON flight_tracking_credentials(provider, flight_number, flight_date, token_status);

CREATE TABLE flight_tracking_poll_logs (
    id                      VARCHAR(36) PRIMARY KEY,
    credential_id           VARCHAR(36) NOT NULL REFERENCES flight_tracking_credentials(id),
    request_timestamp       TIMESTAMP WITH TIME ZONE NOT NULL,
    response_timestamp      TIMESTAMP WITH TIME ZONE NOT NULL,
    http_status             INTEGER NOT NULL,
    response_duration_ms    BIGINT NOT NULL,
    response_valid          BOOLEAN NOT NULL DEFAULT FALSE,
    parsing_status          VARCHAR(40) NOT NULL, -- VALID_TRACKING_JSON, VALID_JSON_UNEXPECTED_SCHEMA, NON_JSON_RESPONSE, ACCESS_RESTRICTED_OR_CHALLENGE, HTTP_ERROR, NETWORK_ERROR
    flight_identifier       VARCHAR(80),
    payload_hash            VARCHAR(64),
    response_payload        TEXT,
    normalized_payload      TEXT,
    discovered_fields_json  TEXT,
    experiment_tag          VARCHAR(60),
    error_message           TEXT,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ft_poll_cred_time ON flight_tracking_poll_logs(credential_id, request_timestamp DESC);
CREATE INDEX idx_ft_poll_valid ON flight_tracking_poll_logs(credential_id, response_valid);

