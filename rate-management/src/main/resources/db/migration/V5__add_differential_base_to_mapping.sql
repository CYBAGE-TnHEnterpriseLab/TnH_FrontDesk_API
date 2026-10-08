ALTER TABLE rate_db.master_room_room_type_mapping
    ADD COLUMN IF NOT EXISTS differential_type VARCHAR(20);

ALTER TABLE rate_db.master_room_room_type_mapping
    ADD COLUMN IF NOT EXISTS differential_value NUMERIC(10, 2);