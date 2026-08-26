CREATE SCHEMA IF NOT EXISTS folio_db;
SET search_path TO folio_db;

CREATE TABLE IF NOT EXISTS folios (
    id BIGSERIAL PRIMARY KEY,
    property_id VARCHAR(40) NOT NULL,
    confirmation_number VARCHAR(80) NOT NULL,
    folio_status VARCHAR(30) NOT NULL,
    folio_type VARCHAR(30) NOT NULL,
    folio_name VARCHAR(160) NOT NULL,
    folio_balance NUMERIC(12,2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_folio_confirmation_number UNIQUE (confirmation_number)
);


CREATE INDEX IF NOT EXISTS idx_folios_property_id ON folios (property_id);  
CREATE INDEX IF NOT EXISTS idx_folios_confirmation_number ON folios (confirmation_number);