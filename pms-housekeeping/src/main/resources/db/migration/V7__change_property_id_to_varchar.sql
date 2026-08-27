ALTER TABLE room_master_projection
ALTER COLUMN property_id TYPE VARCHAR(36) USING property_id::text;
