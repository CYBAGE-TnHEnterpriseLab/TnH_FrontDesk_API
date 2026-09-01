CREATE SCHEMA IF NOT EXISTS folio_db;
SET SCHEMA 'folio_db';

CREATE TABLE IF NOT EXISTS folios (
    id BIGSERIAL PRIMARY KEY,
    confirmation_number VARCHAR(80) NOT NULL,
    folio_code VARCHAR(30) NOT NULL,
    guest_name VARCHAR(160) NOT NULL,
    room_no VARCHAR(40) NOT NULL,
    total_charges NUMERIC(19,2) NOT NULL,
    total_payment NUMERIC(19,2) NOT NULL,
    outstanding_balance NUMERIC(19,2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    last_updated_at TIMESTAMP NOT NULL,
    transactions_json TEXT,
    CONSTRAINT uk_folio_confirmation_folio_code UNIQUE (confirmation_number, folio_code)
);

CREATE INDEX IF NOT EXISTS idx_folios_confirmation_number ON folios (confirmation_number);