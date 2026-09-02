CREATE SCHEMA IF NOT EXISTS pms_inventory;

CREATE TABLE pms_inventory.room_type_inventory_daily (
	id BIGSERIAL PRIMARY KEY,
	property_id UUID NOT NULL,
	room_type_id UUID NOT NULL,
	business_date DATE NOT NULL,
	total_inventory INTEGER NOT NULL CHECK (total_inventory >= 0),
	reserved_count INTEGER NOT NULL DEFAULT 0 CHECK (reserved_count >= 0),
	blocked_count INTEGER NOT NULL DEFAULT 0 CHECK (blocked_count >= 0),
	version BIGINT NOT NULL DEFAULT 0,
	created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
	updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
	CONSTRAINT uk_inventory_daily_property_room_type_date
		UNIQUE (property_id, room_type_id, business_date)
);

CREATE INDEX idx_inventory_daily_property_room_type_date
	ON pms_inventory.room_type_inventory_daily (property_id, room_type_id, business_date);
CREATE INDEX idx_inventory_daily_business_date
	ON pms_inventory.room_type_inventory_daily (business_date);

CREATE TABLE pms_inventory.inventory_reservation (
	id BIGSERIAL PRIMARY KEY,
	reservation_id UUID NOT NULL,
	property_id UUID NOT NULL,
	booked_room_type_id UUID NOT NULL,
	assigned_room_type_id UUID NOT NULL,
	check_in_date DATE NOT NULL,
	check_out_date DATE NOT NULL,
	quantity INTEGER NOT NULL CHECK (quantity > 0),
	status VARCHAR(32) NOT NULL,
	created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
	updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
	CONSTRAINT uk_inventory_reservation_reservation_id UNIQUE (reservation_id),
	CONSTRAINT chk_inventory_reservation_dates CHECK (check_out_date > check_in_date)
);

CREATE INDEX idx_inventory_reservation_property_assigned_dates
	ON pms_inventory.inventory_reservation (property_id, assigned_room_type_id, check_in_date, check_out_date);

CREATE TABLE pms_inventory.inventory_block (
	id BIGSERIAL PRIMARY KEY,
	property_id UUID NOT NULL,
	room_type_id UUID NOT NULL,
	from_date DATE NOT NULL,
	to_date DATE NOT NULL,
	quantity INTEGER NOT NULL CHECK (quantity > 0),
	reason VARCHAR(255) NOT NULL,
	status VARCHAR(32) NOT NULL,
	created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
	updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
	CONSTRAINT chk_inventory_block_dates CHECK (to_date > from_date)
);

CREATE INDEX idx_inventory_block_property_room_type_dates
	ON pms_inventory.inventory_block (property_id, room_type_id, from_date, to_date);

