ALTER TABLE frontdeskdb.reservation_bookings
    DROP CONSTRAINT IF EXISTS uk_reservation_bookings_confirmation_number;

CREATE INDEX IF NOT EXISTS idx_reservation_confirmation_id
    ON frontdeskdb.reservation_bookings (confirmation_number, id);