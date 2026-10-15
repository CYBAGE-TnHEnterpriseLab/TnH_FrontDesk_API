ALTER TABLE frontdeskdb.reservation_bookings
    ADD COLUMN IF NOT EXISTS address VARCHAR(255);
