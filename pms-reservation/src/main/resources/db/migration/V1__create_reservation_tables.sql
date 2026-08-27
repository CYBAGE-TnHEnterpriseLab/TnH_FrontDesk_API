CREATE SCHEMA IF NOT EXISTS frontdeskdb;
SET search_path TO frontdeskdb;

CREATE TABLE IF NOT EXISTS reservation_bookings (
    id BIGSERIAL PRIMARY KEY,
    confirmation_number VARCHAR(80) NOT NULL,
    reservation_status VARCHAR(30) NOT NULL,
    property_id VARCHAR(40) NOT NULL,
    salutation VARCHAR(20) NOT NULL,
    vip_tag BOOLEAN NOT NULL,
    guest_name VARCHAR(160) NOT NULL,
    guest_names_encoded VARCHAR(4000) NOT NULL,
    personal_email VARCHAR(160) NOT NULL,
    official_email VARCHAR(160) NOT NULL,
    city VARCHAR(80) NOT NULL,
    country VARCHAR(80) NOT NULL,
    zip_code VARCHAR(20) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    mobile_number VARCHAR(20) NOT NULL,
    loyalty_number VARCHAR(40),
    company VARCHAR(120),
    guest_group VARCHAR(120),
    source VARCHAR(120),
    agent VARCHAR(120),
    arrival_date DATE NOT NULL,
    departure_date DATE NOT NULL,
    adult_count INTEGER NOT NULL,
    child_count INTEGER NOT NULL,
    reservation_type VARCHAR(20) NOT NULL,
    room_type VARCHAR(40) NOT NULL,
    assigned_room_no VARCHAR(20),
    floor INTEGER,
    rate_code VARCHAR(40) NOT NULL,
    number_of_rooms INTEGER NOT NULL,
    rate NUMERIC(12,2) NOT NULL,
    total_rate NUMERIC(12,2) NOT NULL,
    payment VARCHAR(40) NOT NULL,
    payment_type VARCHAR(40) NOT NULL,
    eta TIME NOT NULL,
    check_out_time TIME NOT NULL,
    dnm BOOLEAN NOT NULL,
    no_post BOOLEAN NOT NULL,
    guest_balance NUMERIC(12,2) NOT NULL,
    special_requests VARCHAR(500),
    discount NUMERIC(12,2) NOT NULL,
    alerts_messages VARCHAR(500),
    inventory_deducted_at TIMESTAMP,
    inventory_synced_at TIMESTAMP,
    check_in_completed_at TIMESTAMP,
    check_in_completed_by VARCHAR(160),
    check_in_business_date DATE,
    check_out_completed_at TIMESTAMP,
    check_out_completed_by VARCHAR(160),
    check_out_business_date DATE,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_reservation_bookings_confirmation_number UNIQUE (confirmation_number)
);

CREATE TABLE IF NOT EXISTS reservation_checkin_audit (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    confirmation_number VARCHAR(80) NOT NULL,
    property_id VARCHAR(40) NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    event_message VARCHAR(500) NOT NULL,
    changed_fields VARCHAR(4000),
    actor VARCHAR(160) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS reservation_checkin_signatures (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    confirmation_number VARCHAR(80) NOT NULL,
    property_id VARCHAR(40) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    payload_base64 TEXT NOT NULL,
    signed_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_checkin_signature_booking_id UNIQUE (booking_id)
);

CREATE TABLE IF NOT EXISTS reservation_checkin_workflow (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    confirmation_number VARCHAR(80) NOT NULL,
    property_id VARCHAR(40) NOT NULL,
    current_step VARCHAR(40) NOT NULL,
    guest_details_completed_at TIMESTAMP,
    room_stay_completed_at TIMESTAMP,
    signature_completed_at TIMESTAMP,
    payment_validated_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_checkin_workflow_booking_id UNIQUE (booking_id)
);

CREATE TABLE IF NOT EXISTS reservation_payment_transactions (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    confirmation_number VARCHAR(80) NOT NULL,
    property_id VARCHAR(40) NOT NULL,
    payment_mode VARCHAR(40) NOT NULL,
    amount NUMERIC(12,2) NOT NULL,
    transaction_status VARCHAR(20) NOT NULL,
    transaction_reference VARCHAR(120) NOT NULL,
    processor_name VARCHAR(80) NOT NULL,
    failure_reason VARCHAR(500),
    processed_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS arrival_records (
    id BIGSERIAL PRIMARY KEY,
    business_date DATE NOT NULL,
    property_id VARCHAR(40) NOT NULL,
    status VARCHAR(10),
    salutation VARCHAR(20),
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80) NOT NULL,
    room_no VARCHAR(15),
    reservation_type VARCHAR(40),
    city VARCHAR(80),
    rate_code VARCHAR(40),
    check_in_date DATE NOT NULL,
    check_out_date DATE NOT NULL,
    room_nights INTEGER NOT NULL,
    room_status VARCHAR(40),
    corporate_code VARCHAR(40),
    room_type VARCHAR(40),
    confirmation_number VARCHAR(60) NOT NULL,
    company VARCHAR(100),
    sharing_status VARCHAR(1),
    floor INTEGER,
    balance NUMERIC(12,2),
    loyalty_membership_status VARCHAR(60),
    source_last_synced_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_arrival_property_business_confirmation
        UNIQUE (property_id, business_date, confirmation_number)
);

CREATE TABLE IF NOT EXISTS departure_records (
    id BIGSERIAL PRIMARY KEY,
    business_date DATE NOT NULL,
    property_id VARCHAR(40) NOT NULL,
    status VARCHAR(10),
    salutation VARCHAR(20),
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80) NOT NULL,
    room_no VARCHAR(15),
    reservation_type VARCHAR(40),
    city VARCHAR(80),
    rate_code VARCHAR(40),
    check_in_date DATE NOT NULL,
    check_out_date DATE NOT NULL,
    room_nights INTEGER NOT NULL,
    room_status VARCHAR(40),
    corporate_code VARCHAR(40),
    room_type VARCHAR(40),
    confirmation_number VARCHAR(60) NOT NULL,
    company VARCHAR(100),
    sharing_status VARCHAR(1),
    floor INTEGER,
    balance NUMERIC(12,2),
    loyalty_membership_status VARCHAR(60),
    source_last_synced_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_departure_property_business_confirmation
        UNIQUE (property_id, business_date, confirmation_number)
);

CREATE TABLE IF NOT EXISTS housekeeping_room_status (
    id BIGSERIAL PRIMARY KEY,
    property_id VARCHAR(40) NOT NULL,
    business_date DATE NOT NULL,
    confirmation_number VARCHAR(80) NOT NULL,
    room_no VARCHAR(20),
    room_status VARCHAR(20) NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_reservation_property_arrival
    ON reservation_bookings(property_id, arrival_date);
CREATE INDEX IF NOT EXISTS idx_reservation_arrival_departure
    ON reservation_bookings(arrival_date, departure_date);
CREATE INDEX IF NOT EXISTS idx_reservation_guest_name
    ON reservation_bookings(guest_name);
CREATE INDEX IF NOT EXISTS idx_reservation_confirmation
    ON reservation_bookings(confirmation_number);

CREATE INDEX IF NOT EXISTS idx_checkin_audit_booking
    ON reservation_checkin_audit(booking_id);
CREATE INDEX IF NOT EXISTS idx_checkin_audit_confirmation
    ON reservation_checkin_audit(confirmation_number);
CREATE INDEX IF NOT EXISTS idx_checkin_audit_created_at
    ON reservation_checkin_audit(created_at);

CREATE UNIQUE INDEX IF NOT EXISTS idx_checkin_signature_booking
    ON reservation_checkin_signatures(booking_id);
CREATE INDEX IF NOT EXISTS idx_checkin_signature_confirmation
    ON reservation_checkin_signatures(confirmation_number);

CREATE UNIQUE INDEX IF NOT EXISTS idx_checkin_workflow_booking
    ON reservation_checkin_workflow(booking_id);
CREATE INDEX IF NOT EXISTS idx_checkin_workflow_confirmation
    ON reservation_checkin_workflow(confirmation_number);

CREATE INDEX IF NOT EXISTS idx_payment_txn_booking
    ON reservation_payment_transactions(booking_id);
CREATE INDEX IF NOT EXISTS idx_payment_txn_confirmation
    ON reservation_payment_transactions(confirmation_number);

CREATE INDEX IF NOT EXISTS idx_arrival_property_business_date
    ON arrival_records(property_id, business_date);
CREATE INDEX IF NOT EXISTS idx_arrival_property_business_checkin
    ON arrival_records(property_id, business_date, check_in_date);
CREATE INDEX IF NOT EXISTS idx_arrival_business_date
    ON arrival_records(business_date);
CREATE INDEX IF NOT EXISTS idx_arrival_confirmation
    ON arrival_records(confirmation_number);
CREATE INDEX IF NOT EXISTS idx_arrival_guest_name
    ON arrival_records(first_name, last_name);

CREATE INDEX IF NOT EXISTS idx_departure_property_business_date
    ON departure_records(property_id, business_date);
CREATE INDEX IF NOT EXISTS idx_departure_property_business_checkout
    ON departure_records(property_id, business_date, check_out_date);
CREATE INDEX IF NOT EXISTS idx_departure_business_date
    ON departure_records(business_date);
CREATE INDEX IF NOT EXISTS idx_departure_confirmation
    ON departure_records(confirmation_number);
CREATE INDEX IF NOT EXISTS idx_departure_guest_name
    ON departure_records(first_name, last_name);

CREATE UNIQUE INDEX IF NOT EXISTS idx_hk_property_date_confirmation
    ON housekeeping_room_status(property_id, business_date, confirmation_number);
CREATE INDEX IF NOT EXISTS idx_hk_property_date_status
    ON housekeeping_room_status(property_id, business_date, room_status);

