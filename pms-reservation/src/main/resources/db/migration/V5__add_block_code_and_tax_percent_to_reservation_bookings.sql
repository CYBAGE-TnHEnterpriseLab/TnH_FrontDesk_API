ALTER TABLE frontdeskdb.reservation_bookings
    ADD COLUMN IF NOT EXISTS block_code VARCHAR(80),
    ADD COLUMN IF NOT EXISTS tax_percent NUMERIC(5,2);