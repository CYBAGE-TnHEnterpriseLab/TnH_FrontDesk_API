SET search_path TO pms_inventory;

ALTER TABLE room_type_inventory_daily
    ALTER COLUMN property_id TYPE VARCHAR(255) USING property_id::text,
    ALTER COLUMN room_type_id TYPE VARCHAR(255) USING room_type_id::text;

ALTER TABLE inventory_reservation
    RENAME COLUMN reservation_id TO confirmation_number;

ALTER TABLE inventory_reservation
    ALTER COLUMN confirmation_number TYPE VARCHAR(80) USING confirmation_number::text,
    ALTER COLUMN property_id TYPE VARCHAR(255) USING property_id::text,
    ALTER COLUMN booked_room_type_id TYPE VARCHAR(255) USING booked_room_type_id::text,
    ALTER COLUMN assigned_room_type_id TYPE VARCHAR(255) USING assigned_room_type_id::text;

ALTER TABLE inventory_block
    ALTER COLUMN property_id TYPE VARCHAR(255) USING property_id::text,
    ALTER COLUMN room_type_id TYPE VARCHAR(255) USING room_type_id::text;

ALTER INDEX IF EXISTS uk_inventory_reservation_reservation_id
    RENAME TO uk_inventory_reservation_confirmation_number;
