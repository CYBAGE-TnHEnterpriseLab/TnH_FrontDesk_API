ALTER TABLE housekeeping_room_day_status
    ALTER COLUMN confirmation_id TYPE VARCHAR(100)
    USING confirmation_id::text;