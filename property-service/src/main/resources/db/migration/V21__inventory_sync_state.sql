CREATE TABLE IF NOT EXISTS inventory_sync_state (
    property_id VARCHAR(36) PRIMARY KEY,
    status VARCHAR(32) NOT NULL,
    last_request_id VARCHAR(64),
    last_error TEXT,
    retry_count INTEGER NOT NULL DEFAULT 0,
    last_synced_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

