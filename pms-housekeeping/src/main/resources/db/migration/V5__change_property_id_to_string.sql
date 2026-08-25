-- Change property_id from UUID to VARCHAR(36) to align with other services
ALTER TABLE housekeeping_room_day_status
    ALTER COLUMN property_id TYPE VARCHAR(36) USING property_id::text;

ALTER TABLE housekeeping_room_day_status_history
    ALTER COLUMN property_id TYPE VARCHAR(36) USING property_id::text;
