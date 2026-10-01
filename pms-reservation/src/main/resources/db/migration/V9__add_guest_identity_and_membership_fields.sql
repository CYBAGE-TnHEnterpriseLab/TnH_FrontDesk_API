ALTER TABLE frontdeskdb.reservation_bookings
    ADD COLUMN IF NOT EXISTS date_of_birth DATE;

ALTER TABLE frontdeskdb.reservation_bookings
    ADD COLUMN IF NOT EXISTS state VARCHAR(80);

ALTER TABLE frontdeskdb.reservation_bookings
    ADD COLUMN IF NOT EXISTS id_type VARCHAR(40);

ALTER TABLE frontdeskdb.reservation_bookings
    ADD COLUMN IF NOT EXISTS id_number VARCHAR(120);

ALTER TABLE frontdeskdb.reservation_bookings
    ADD COLUMN IF NOT EXISTS enroll_guest BOOLEAN NOT NULL DEFAULT FALSE;