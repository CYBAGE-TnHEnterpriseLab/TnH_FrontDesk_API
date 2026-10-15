ALTER TABLE frontdeskdb.reservation_bookings
    ADD COLUMN IF NOT EXISTS age_of_child_1 INTEGER;

ALTER TABLE frontdeskdb.reservation_bookings
    ADD COLUMN IF NOT EXISTS age_of_child_2 INTEGER;
