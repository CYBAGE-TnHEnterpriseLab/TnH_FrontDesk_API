-- Create reservation_guests table for individual booking rows.
-- Apply the pms-guest migration first so guest_profiles exists.
-- booking_id references reservation_bookings.id, not confirmation_number.
-- guest_profile_id references guest_profiles.id, not its business guest_id.

CREATE TABLE IF NOT EXISTS frontdeskdb.reservation_guests (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    guest_profile_id BIGINT NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    created_by UUID,
    updated_at TIMESTAMP NOT NULL,
    updated_by UUID,
    CONSTRAINT fk_reservation_guests_booking
        FOREIGN KEY (booking_id) REFERENCES frontdeskdb.reservation_bookings (id) ON DELETE CASCADE,
    CONSTRAINT fk_reservation_guests_guest
        FOREIGN KEY (guest_profile_id) REFERENCES frontdeskdb.guest_profiles (id),
    CONSTRAINT uk_reservation_guests_booking_guest_profile
        UNIQUE (booking_id, guest_profile_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_reservation_guests_primary_per_booking
    ON frontdeskdb.reservation_guests(booking_id)
    WHERE is_primary = TRUE;

-- Create indexes for reservation_guests queries
CREATE INDEX IF NOT EXISTS idx_reservation_guests_booking
    ON frontdeskdb.reservation_guests(booking_id);

CREATE INDEX IF NOT EXISTS idx_reservation_guests_guest_profile
    ON frontdeskdb.reservation_guests(guest_profile_id);

CREATE INDEX IF NOT EXISTS idx_reservation_guests_primary
    ON frontdeskdb.reservation_guests(booking_id, is_primary);
