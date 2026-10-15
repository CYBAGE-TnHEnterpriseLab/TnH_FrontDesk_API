ALTER TABLE pms_housekeeping.room_master_projection
    ALTER COLUMN room_type_id TYPE VARCHAR(100)
    USING room_type_id::text;

ALTER TABLE pms_housekeeping.housekeeping_room_day_status
    ALTER COLUMN room_type_id TYPE VARCHAR(100)
    USING room_type_id::text;