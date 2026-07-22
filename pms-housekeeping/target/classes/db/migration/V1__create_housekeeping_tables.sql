CREATE TABLE room_master_projection (
    id BIGSERIAL PRIMARY KEY,
    property_id UUID NOT NULL,
    room_number VARCHAR(32) NOT NULL,
    room_type_id UUID NOT NULL,
    room_type_name VARCHAR(120) NOT NULL,
    floor VARCHAR(120),
    zone VARCHAR(120),
    room_class VARCHAR(120),
    features_csv VARCHAR(4000),
    vip_capable BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_room_master_property_room UNIQUE (property_id, room_number)
);

CREATE INDEX idx_room_master_property_type_active
    ON room_master_projection (property_id, room_type_id, active);

CREATE TABLE housekeeping_room_day_status (
    id BIGSERIAL PRIMARY KEY,
    property_id UUID NOT NULL,
    business_date DATE NOT NULL,
    room_number VARCHAR(32) NOT NULL,
    room_type_id UUID NOT NULL,
    cleaning_status VARCHAR(32) NOT NULL,
    front_office_status VARCHAR(32) NOT NULL,
    reservation_status VARCHAR(32) NOT NULL,
    assigned_reservation_id UUID,
    attendant_name VARCHAR(160),
    last_cleaned_at TIMESTAMP WITHOUT TIME ZONE,
    priority VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
    is_sellable BOOLEAN NOT NULL DEFAULT FALSE,
    guest_display_name VARCHAR(200),
    arrival_date DATE,
    departure_date DATE,
    status_changed_at TIMESTAMP WITHOUT TIME ZONE,
    fo_status_changed_at TIMESTAMP WITHOUT TIME ZONE,
    reservation_status_changed_at TIMESTAMP WITHOUT TIME ZONE,
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_hk_room_day UNIQUE (property_id, business_date, room_number)
);

CREATE INDEX idx_hk_room_day_property_date
    ON housekeeping_room_day_status (property_id, business_date);

CREATE INDEX idx_hk_room_day_filters
    ON housekeeping_room_day_status (
        property_id,
        business_date,
        room_type_id,
        cleaning_status,
        front_office_status,
        reservation_status,
        is_sellable
    );

CREATE TABLE housekeeping_room_day_status_history (
    id BIGSERIAL PRIMARY KEY,
    property_id UUID NOT NULL,
    business_date DATE NOT NULL,
    room_number VARCHAR(32) NOT NULL,
    changed_field VARCHAR(64) NOT NULL,
    old_value VARCHAR(160),
    new_value VARCHAR(160),
    changed_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    changed_by VARCHAR(120),
    source_module VARCHAR(64) NOT NULL,
    reason VARCHAR(500)
);

CREATE INDEX idx_hk_history_room_date
    ON housekeeping_room_day_status_history (property_id, room_number, business_date, changed_at DESC);

