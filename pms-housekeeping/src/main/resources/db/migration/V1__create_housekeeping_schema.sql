-- =====================================================
-- HOUSEKEEPING SCHEMA
-- Consolidated migration: creates all tables with final
-- column names and types to match JPA entities.
-- =====================================================

CREATE SCHEMA IF NOT EXISTS pms_housekeeping;


-- =====================================================
-- ROOM MASTER PROJECTION
-- Read-only projection synchronized from Property Service
-- =====================================================
CREATE TABLE pms_housekeeping.room_master_projection (

    id BIGSERIAL PRIMARY KEY,

    property_id VARCHAR(36) NOT NULL,

    room_number VARCHAR(32) NOT NULL,

    room_type_id UUID NOT NULL,

    room_type_name VARCHAR(100) NOT NULL,

    floor VARCHAR(50),

    zone VARCHAR(50),

    room_class VARCHAR(50),

    features_csv VARCHAR(4000),

    vip_capable BOOLEAN NOT NULL DEFAULT FALSE,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    version BIGINT NOT NULL DEFAULT 0,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    created_by UUID,

    updated_by UUID,

    CONSTRAINT uk_room_master_property_room
        UNIQUE(property_id, room_number)

);

CREATE INDEX idx_room_master_property
ON pms_housekeeping.room_master_projection(property_id);

CREATE INDEX idx_room_master_room_type
ON pms_housekeeping.room_master_projection(property_id, room_type_id);

CREATE INDEX idx_room_master_active
ON pms_housekeeping.room_master_projection(property_id, active);

CREATE INDEX idx_room_master_floor
ON pms_housekeeping.room_master_projection(property_id, floor);


-- =====================================================
-- HOUSEKEEPING ROOM DAILY STATUS
-- Main table used by Housekeeping screen
-- =====================================================

CREATE TABLE pms_housekeeping.housekeeping_room_day_status (

    id BIGSERIAL PRIMARY KEY,

    property_id VARCHAR(36) NOT NULL,

    business_date DATE NOT NULL,

    room_number VARCHAR(32) NOT NULL,

    room_type_id UUID NOT NULL,

    room_type_name VARCHAR(100) NOT NULL,

    floor VARCHAR(50),

    cleaning_status VARCHAR(30) NOT NULL,

    front_office_status VARCHAR(30) NOT NULL,

    reservation_status VARCHAR(30) NOT NULL,

    confirmation_id VARCHAR(100),

    attendant_name VARCHAR(150),

    guest_display_name VARCHAR(200),

    arrival_date DATE,

    departure_date DATE,

    last_cleaned_at TIMESTAMP,

    priority VARCHAR(30) NOT NULL DEFAULT 'NORMAL',

    is_sellable BOOLEAN NOT NULL DEFAULT FALSE,

    features_csv VARCHAR(500),

    version BIGINT NOT NULL DEFAULT 0,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    created_by UUID,

    updated_by UUID,

    CONSTRAINT uk_hk_room_day
        UNIQUE(property_id, business_date, room_number)

);


-- =====================================================
-- INDEXES
-- =====================================================

CREATE INDEX idx_hk_property_date
ON pms_housekeeping.housekeeping_room_day_status(property_id, business_date);

CREATE INDEX idx_hk_room_type
ON pms_housekeeping.housekeeping_room_day_status(property_id, business_date, room_type_id);

CREATE INDEX idx_hk_cleaning
ON pms_housekeeping.housekeeping_room_day_status(property_id, business_date, cleaning_status);

CREATE INDEX idx_hk_front_office
ON pms_housekeeping.housekeeping_room_day_status(property_id, business_date, front_office_status);

CREATE INDEX idx_hk_reservation
ON pms_housekeeping.housekeeping_room_day_status(property_id, business_date, reservation_status);

CREATE INDEX idx_hk_floor
ON pms_housekeeping.housekeeping_room_day_status(property_id, business_date, floor);

CREATE INDEX idx_hk_attendant
ON pms_housekeeping.housekeeping_room_day_status(property_id, business_date, attendant_name);

CREATE INDEX idx_hk_priority
ON pms_housekeeping.housekeeping_room_day_status(property_id, business_date, priority);

CREATE INDEX idx_hk_sellable
ON pms_housekeeping.housekeeping_room_day_status(property_id, business_date, is_sellable);

CREATE INDEX idx_hk_room_number
ON pms_housekeeping.housekeeping_room_day_status(property_id, room_number);


-- =====================================================
-- HISTORY TABLE
-- =====================================================

CREATE TABLE pms_housekeeping.housekeeping_room_day_status_history (

    id BIGSERIAL PRIMARY KEY,

    property_id VARCHAR(36) NOT NULL,

    business_date DATE NOT NULL,

    room_number VARCHAR(32) NOT NULL,

    changed_field VARCHAR(64) NOT NULL,

    old_value VARCHAR(500),

    new_value VARCHAR(500),

    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    changed_by VARCHAR(120),

    source_module VARCHAR(64) NOT NULL,

    reason VARCHAR(500),

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    created_by UUID,

    updated_by UUID

);

CREATE INDEX idx_hk_history_room
ON pms_housekeeping.housekeeping_room_day_status_history
(property_id, room_number);

CREATE INDEX idx_hk_history_business_date
ON pms_housekeeping.housekeeping_room_day_status_history
(property_id, business_date);

CREATE INDEX idx_hk_history_changed_at
ON pms_housekeeping.housekeeping_room_day_status_history
(changed_at DESC);