CREATE SCHEMA IF NOT EXISTS frontdeskdb;

CREATE TABLE IF NOT EXISTS frontdeskdb.guest_profiles (
    id BIGSERIAL PRIMARY KEY,
    guest_id VARCHAR(80) NOT NULL,
    property_id VARCHAR(36) NOT NULL,
    salutation VARCHAR(20),
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80) NOT NULL,
    personal_email VARCHAR(160),
    official_email VARCHAR(160),
    phone_number VARCHAR(20),
    mobile_number VARCHAR(20),
    address VARCHAR(255),
    city VARCHAR(80),
    state VARCHAR(80),
    country VARCHAR(80),
    postal_code VARCHAR(20),
    nationality VARCHAR(80),
    date_of_birth DATE,
    gender VARCHAR(20),
    company_name VARCHAR(120),
    vip_status BOOLEAN NOT NULL DEFAULT FALSE,
    id_type VARCHAR(40),
    id_number VARCHAR(80),
    id_document_path VARCHAR(500),
    loyalty_membership_number VARCHAR(40),
    loyalty_tier VARCHAR(40),
    created_at TIMESTAMP NOT NULL,
    created_by UUID,
    updated_at TIMESTAMP NOT NULL,
    updated_by UUID,
    CONSTRAINT uk_guest_profiles_guest_id UNIQUE (guest_id)
);

CREATE INDEX IF NOT EXISTS idx_guest_profiles_property_phone
    ON frontdeskdb.guest_profiles(property_id, phone_number);

CREATE INDEX IF NOT EXISTS idx_guest_profiles_property_personal_email
    ON frontdeskdb.guest_profiles(property_id, personal_email);

CREATE INDEX IF NOT EXISTS idx_guest_profiles_property_official_email
    ON frontdeskdb.guest_profiles(property_id, official_email);

CREATE INDEX IF NOT EXISTS idx_guest_profiles_property_loyalty
    ON frontdeskdb.guest_profiles(property_id, loyalty_membership_number);

CREATE INDEX IF NOT EXISTS idx_guest_profiles_property_name
    ON frontdeskdb.guest_profiles(property_id, first_name, last_name);
