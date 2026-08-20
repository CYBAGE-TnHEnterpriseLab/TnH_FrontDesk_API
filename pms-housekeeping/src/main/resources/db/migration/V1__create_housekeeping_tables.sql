-- =====================================================
-- ROOM MASTER PROJECTION
-- Read-only projection synchronized from Property Service
-- =====================================================

CREATE TABLE room_master_projection (

    id BIGSERIAL PRIMARY KEY,

    property_id UUID NOT NULL,

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

    CONSTRAINT uk_room_master_property_room
        UNIQUE(property_id, room_number)

);

CREATE INDEX idx_room_master_property
ON room_master_projection(property_id);

CREATE INDEX idx_room_master_room_type
ON room_master_projection(property_id, room_type_id);

CREATE INDEX idx_room_master_active
ON room_master_projection(property_id, active);

CREATE INDEX idx_room_master_floor
ON room_master_projection(property_id, floor);



-- =====================================================
-- HOUSEKEEPING ROOM DAILY STATUS
-- Main table used by Housekeeping screen
-- =====================================================

CREATE TABLE housekeeping_room_day_status (

    id BIGSERIAL PRIMARY KEY,

    property_id UUID NOT NULL,

    business_date DATE NOT NULL,

    room_number VARCHAR(32) NOT NULL,

    room_type_id UUID NOT NULL,

    room_type_name VARCHAR(100) NOT NULL,

    floor VARCHAR(50),

    cleaning_status VARCHAR(30) NOT NULL,

    front_office_status VARCHAR(30) NOT NULL,

    reservation_status VARCHAR(30) NOT NULL,

    assigned_reservation_id UUID,

    attendant_name VARCHAR(150),

    guest_display_name VARCHAR(200),

    arrival_date DATE,

    departure_date DATE,

    last_cleaned_at TIMESTAMP,

    priority VARCHAR(30) NOT NULL DEFAULT 'NORMAL',

    is_sellable BOOLEAN NOT NULL DEFAULT FALSE,

    updated_by VARCHAR(120),

    version BIGINT NOT NULL DEFAULT 0,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_hk_room_day
        UNIQUE(property_id, business_date, room_number)

);



-- =====================================================
-- INDEXES
-- =====================================================

CREATE INDEX idx_hk_property_date
ON housekeeping_room_day_status(property_id, business_date);

CREATE INDEX idx_hk_room_type
ON housekeeping_room_day_status(property_id, business_date, room_type_id);

CREATE INDEX idx_hk_cleaning
ON housekeeping_room_day_status(property_id, business_date, cleaning_status);

CREATE INDEX idx_hk_front_office
ON housekeeping_room_day_status(property_id, business_date, front_office_status);

CREATE INDEX idx_hk_reservation
ON housekeeping_room_day_status(property_id, business_date, reservation_status);

CREATE INDEX idx_hk_floor
ON housekeeping_room_day_status(property_id, business_date, floor);

CREATE INDEX idx_hk_attendant
ON housekeeping_room_day_status(property_id, business_date, attendant_name);

CREATE INDEX idx_hk_priority
ON housekeeping_room_day_status(property_id, business_date, priority);

CREATE INDEX idx_hk_sellable
ON housekeeping_room_day_status(property_id, business_date, is_sellable);

CREATE INDEX idx_hk_room_number
ON housekeeping_room_day_status(property_id, room_number);



-- =====================================================
-- HISTORY TABLE
-- =====================================================

CREATE TABLE housekeeping_room_day_status_history (

    id BIGSERIAL PRIMARY KEY,

    property_id UUID NOT NULL,

    business_date DATE NOT NULL,

    room_number VARCHAR(32) NOT NULL,

    changed_field VARCHAR(64) NOT NULL,

    old_value VARCHAR(500),

    new_value VARCHAR(500),

    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    changed_by VARCHAR(120),

    source_module VARCHAR(64) NOT NULL,

    reason VARCHAR(500)

);

CREATE INDEX idx_hk_history_room
ON housekeeping_room_day_status_history
(property_id, room_number);

CREATE INDEX idx_hk_history_business_date
ON housekeeping_room_day_status_history
(property_id, business_date);

CREATE INDEX idx_hk_history_changed_at
ON housekeeping_room_day_status_history
(changed_at DESC);
