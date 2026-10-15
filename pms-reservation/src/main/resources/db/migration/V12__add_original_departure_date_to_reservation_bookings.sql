ALTER TABLE frontdeskdb.reservation_bookings
    ADD COLUMN IF NOT EXISTS original_departure_date DATE;
