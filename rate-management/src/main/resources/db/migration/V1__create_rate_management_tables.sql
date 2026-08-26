CREATE SCHEMA IF NOT EXISTS rate_db;
SET search_path TO rate_db;

CREATE TABLE IF NOT EXISTS rate_plan (
    id BIGSERIAL PRIMARY KEY,
    property_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(255) NOT NULL,
    occupancy_type VARCHAR(255) NOT NULL,
    meal_option VARCHAR(255),
    inclusion VARCHAR(255),
    type VARCHAR(255) NOT NULL,
    calculation_method VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    adjustment_value DOUBLE PRECISION,
    manual_amount DOUBLE PRECISION,
    parent_rate_plan_id BIGINT,
    CONSTRAINT uk_rate_plan_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS rate_plan_manual_occupancy_price (
    rate_plan_id BIGINT NOT NULL,
    occupancy_type VARCHAR(255),
    manual_amount DOUBLE PRECISION,
    CONSTRAINT fk_rate_plan_manual_occ_rate_plan
        FOREIGN KEY (rate_plan_id) REFERENCES rate_plan (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS rate_plan_room_type (
    rate_plan_id BIGINT NOT NULL,
    room_type_id BIGINT NOT NULL,
    CONSTRAINT fk_rate_plan_room_type_rate_plan
        FOREIGN KEY (rate_plan_id) REFERENCES rate_plan (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS rate_plan_policy (
    rate_plan_id BIGINT NOT NULL,
    policy_index INTEGER,
    policy_id VARCHAR(255),
    CONSTRAINT fk_rate_plan_policy_rate_plan
        FOREIGN KEY (rate_plan_id) REFERENCES rate_plan (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS policy_mapping (
    id BIGSERIAL PRIMARY KEY,
    policy_id VARCHAR(255) NOT NULL,
    rate_plan_id VARCHAR(255),
    property_id VARCHAR(255) NOT NULL,
    service_type VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS master_room (
    id BIGSERIAL PRIMARY KEY,
    property_id VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    meal_option VARCHAR(255),
    inclusion VARCHAR(255),
    CONSTRAINT uk_master_room_property_name UNIQUE (property_id, name)
);

CREATE TABLE IF NOT EXISTS master_room_pricing (
    id BIGSERIAL PRIMARY KEY,
    master_room_id BIGINT,
    room_type_id BIGINT,
    inherited BOOLEAN,
    parent_pricing_id BIGINT,
    occupancy_type VARCHAR(255) NOT NULL,
    price DOUBLE PRECISION NOT NULL,
    CONSTRAINT uk_room_type_occupancy UNIQUE (room_type_id, occupancy_type),
    CONSTRAINT fk_master_room_pricing_master_room
        FOREIGN KEY (master_room_id) REFERENCES master_room (id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS master_room_room_type_mapping (
    id BIGSERIAL PRIMARY KEY,
    master_room_id BIGINT NOT NULL,
    room_type_id BIGINT NOT NULL,
    CONSTRAINT fk_master_room_room_type_mapping_master_room
        FOREIGN KEY (master_room_id) REFERENCES master_room (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_rate_plan_property_id ON rate_plan(property_id);
CREATE INDEX IF NOT EXISTS idx_rate_plan_start_end_date ON rate_plan(start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_policy_mapping_property_service ON policy_mapping(property_id, service_type);
CREATE INDEX IF NOT EXISTS idx_master_room_property_id ON master_room(property_id);
CREATE INDEX IF NOT EXISTS idx_master_room_pricing_master_room_id ON master_room_pricing(master_room_id);
CREATE INDEX IF NOT EXISTS idx_master_room_pricing_room_type_id ON master_room_pricing(room_type_id);
CREATE INDEX IF NOT EXISTS idx_master_room_room_type_mapping_room_type_id ON master_room_room_type_mapping(room_type_id);

