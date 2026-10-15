-- The reservation_guests table already exists as migration V11, but databases that were baselined or
-- created before V11 ran have reservation_bookings without it, and "validate-on-migrate: false" hides the
-- drift until the guest link insert fails. Recreated here as an idempotent migration so every environment
-- converges on the real schema without editing V11.

CREATE TABLE IF NOT EXISTS frontdeskdb.reservation_guests (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    guest_profile_id BIGINT NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    primary_booking_id BIGINT GENERATED ALWAYS AS (
        CASE WHEN is_primary THEN booking_id ELSE NULL END
    ) ${generated-column-storage},
    created_at TIMESTAMP NOT NULL,
    created_by UUID,
    updated_at TIMESTAMP NOT NULL,
    updated_by UUID,
    CONSTRAINT fk_reservation_guests_booking
        FOREIGN KEY (booking_id) REFERENCES frontdeskdb.reservation_bookings (id) ON DELETE CASCADE,
    CONSTRAINT uk_reservation_guests_booking_guest_profile
        UNIQUE (booking_id, guest_profile_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_reservation_guests_primary_per_booking
    ON frontdeskdb.reservation_guests (primary_booking_id);

CREATE INDEX IF NOT EXISTS idx_reservation_guests_booking
    ON frontdeskdb.reservation_guests (booking_id);

CREATE INDEX IF NOT EXISTS idx_reservation_guests_guest_profile
    ON frontdeskdb.reservation_guests (guest_profile_id);

CREATE INDEX IF NOT EXISTS idx_reservation_guests_primary
    ON frontdeskdb.reservation_guests (booking_id, is_primary);
