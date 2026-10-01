CREATE TABLE IF NOT EXISTS frontdeskdb.reservation_checkin_id_proofs (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    confirmation_number VARCHAR(80) NOT NULL,
    property_id VARCHAR(40) NOT NULL,
    id_proof_type VARCHAR(40) NOT NULL,
    id_proof_number VARCHAR(120) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    payload_base64 TEXT NOT NULL,
    uploaded_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by uuid,
    updated_by uuid,
    CONSTRAINT uk_checkin_id_proof_booking_id UNIQUE (booking_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_checkin_id_proof_booking
    ON frontdeskdb.reservation_checkin_id_proofs(booking_id);
CREATE INDEX IF NOT EXISTS idx_checkin_id_proof_confirmation
    ON frontdeskdb.reservation_checkin_id_proofs(confirmation_number);