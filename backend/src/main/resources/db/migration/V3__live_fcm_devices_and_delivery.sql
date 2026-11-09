-- ============================================================================
-- V3: Multi-Device FCM Registry & Live Push Delivery Audit Columns
-- ============================================================================

CREATE TABLE user_fcm_devices (
    id                  VARCHAR(36) PRIMARY KEY,
    user_id             VARCHAR(36) NOT NULL REFERENCES app_users(id),
    fcm_token           VARCHAR(512) NOT NULL UNIQUE,
    platform            VARCHAR(30) NOT NULL DEFAULT 'ANDROID',
    device_label        VARCHAR(120),
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    last_registered_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_push_at        TIMESTAMP WITH TIME ZONE,
    last_error          TEXT,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_fcm_devices_user_active ON user_fcm_devices(user_id, is_active);

ALTER TABLE notifications ADD COLUMN fcm_delivery_status VARCHAR(30) DEFAULT 'IN_APP_ONLY';
ALTER TABLE notifications ADD COLUMN fcm_message_id VARCHAR(200);
ALTER TABLE notifications ADD COLUMN fcm_error TEXT;

