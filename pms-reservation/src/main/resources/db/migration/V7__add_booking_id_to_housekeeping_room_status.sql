ALTER TABLE frontdeskdb.housekeeping_room_status
    ADD COLUMN IF NOT EXISTS booking_id BIGINT;

DROP INDEX IF EXISTS frontdeskdb.idx_hk_property_date_confirmation;

CREATE UNIQUE INDEX IF NOT EXISTS idx_hk_property_date_booking_unique
    ON frontdeskdb.housekeeping_room_status(property_id, business_date, booking_id);