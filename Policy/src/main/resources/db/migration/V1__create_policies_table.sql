CREATE SCHEMA IF NOT EXISTS policy_db;

CREATE TABLE IF NOT EXISTS policy_db.policies (
    id BIGSERIAL PRIMARY KEY,
    policy_name VARCHAR(200) NOT NULL,
    policy_type VARCHAR(255) NOT NULL,
    service_type VARCHAR(255) NOT NULL,
    used_by VARCHAR(255) NOT NULL,
    policy_code VARCHAR(255) NOT NULL,
    policy_category VARCHAR(255) NOT NULL,
    priority INTEGER,
    description TEXT,
    effective_date DATE,
    effective_to DATE,
    status VARCHAR(255) NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    action VARCHAR(255),
    policy_count INTEGER,
    property_id VARCHAR(255),
    property_code VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_policies_policy_code ON policy_db.policies(policy_code);
CREATE INDEX IF NOT EXISTS idx_policies_property_id ON policy_db.policies(property_id);
CREATE INDEX IF NOT EXISTS idx_policies_service_type ON policy_db.policies(service_type);
CREATE INDEX IF NOT EXISTS idx_policies_status ON policy_db.policies(status);

