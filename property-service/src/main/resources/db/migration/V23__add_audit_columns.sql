-- Adds audit columns (created_at, updated_at, created_by, updated_by) to property-service tables.
-- created_by/updated_by are UUID-backed to match the common BaseEntity audit contract.
-- Idempotent: every statement uses IF NOT EXISTS / DROP NOT NULL guards.

-- Tables that had no audit columns at all
ALTER TABLE tax_rule ADD COLUMN IF NOT EXISTS created_at timestamp NOT NULL DEFAULT now();
ALTER TABLE tax_rule ADD COLUMN IF NOT EXISTS updated_at timestamp NOT NULL DEFAULT now();
ALTER TABLE tax_rule ADD COLUMN IF NOT EXISTS created_by uuid;
ALTER TABLE tax_rule ADD COLUMN IF NOT EXISTS updated_by uuid;

ALTER TABLE revenue_mapping ADD COLUMN IF NOT EXISTS created_at timestamp NOT NULL DEFAULT now();
ALTER TABLE revenue_mapping ADD COLUMN IF NOT EXISTS updated_at timestamp NOT NULL DEFAULT now();
ALTER TABLE revenue_mapping ADD COLUMN IF NOT EXISTS created_by uuid;
ALTER TABLE revenue_mapping ADD COLUMN IF NOT EXISTS updated_by uuid;

ALTER TABLE chart_of_account ADD COLUMN IF NOT EXISTS created_at timestamp NOT NULL DEFAULT now();
ALTER TABLE chart_of_account ADD COLUMN IF NOT EXISTS updated_at timestamp NOT NULL DEFAULT now();
ALTER TABLE chart_of_account ADD COLUMN IF NOT EXISTS created_by uuid;
ALTER TABLE chart_of_account ADD COLUMN IF NOT EXISTS updated_by uuid;

ALTER TABLE payment_method ADD COLUMN IF NOT EXISTS created_at timestamp NOT NULL DEFAULT now();
ALTER TABLE payment_method ADD COLUMN IF NOT EXISTS updated_at timestamp NOT NULL DEFAULT now();
ALTER TABLE payment_method ADD COLUMN IF NOT EXISTS created_by uuid;
ALTER TABLE payment_method ADD COLUMN IF NOT EXISTS updated_by uuid;

ALTER TABLE room_outlet_type ADD COLUMN IF NOT EXISTS created_at timestamp NOT NULL DEFAULT now();
ALTER TABLE room_outlet_type ADD COLUMN IF NOT EXISTS updated_at timestamp NOT NULL DEFAULT now();
ALTER TABLE room_outlet_type ADD COLUMN IF NOT EXISTS created_by uuid;
ALTER TABLE room_outlet_type ADD COLUMN IF NOT EXISTS updated_by uuid;

ALTER TABLE floor_property_area ADD COLUMN IF NOT EXISTS created_at timestamp NOT NULL DEFAULT now();
ALTER TABLE floor_property_area ADD COLUMN IF NOT EXISTS updated_at timestamp NOT NULL DEFAULT now();
ALTER TABLE floor_property_area ADD COLUMN IF NOT EXISTS created_by uuid;
ALTER TABLE floor_property_area ADD COLUMN IF NOT EXISTS updated_by uuid;

ALTER TABLE property_area ADD COLUMN IF NOT EXISTS created_at timestamp NOT NULL DEFAULT now();
ALTER TABLE property_area ADD COLUMN IF NOT EXISTS updated_at timestamp NOT NULL DEFAULT now();
ALTER TABLE property_area ADD COLUMN IF NOT EXISTS created_by uuid;
ALTER TABLE property_area ADD COLUMN IF NOT EXISTS updated_by uuid;

ALTER TABLE inventory_room ADD COLUMN IF NOT EXISTS created_at timestamp NOT NULL DEFAULT now();
ALTER TABLE inventory_room ADD COLUMN IF NOT EXISTS updated_at timestamp NOT NULL DEFAULT now();
ALTER TABLE inventory_room ADD COLUMN IF NOT EXISTS created_by uuid;
ALTER TABLE inventory_room ADD COLUMN IF NOT EXISTS updated_by uuid;

ALTER TABLE floor_configuration ADD COLUMN IF NOT EXISTS created_at timestamp NOT NULL DEFAULT now();
ALTER TABLE floor_configuration ADD COLUMN IF NOT EXISTS updated_at timestamp NOT NULL DEFAULT now();
ALTER TABLE floor_configuration ADD COLUMN IF NOT EXISTS created_by uuid;
ALTER TABLE floor_configuration ADD COLUMN IF NOT EXISTS updated_by uuid;

ALTER TABLE guest_service_amenity ADD COLUMN IF NOT EXISTS created_at timestamp NOT NULL DEFAULT now();
ALTER TABLE guest_service_amenity ADD COLUMN IF NOT EXISTS updated_at timestamp NOT NULL DEFAULT now();
ALTER TABLE guest_service_amenity ADD COLUMN IF NOT EXISTS created_by uuid;
ALTER TABLE guest_service_amenity ADD COLUMN IF NOT EXISTS updated_by uuid;

ALTER TABLE property_overview ADD COLUMN IF NOT EXISTS created_at timestamp NOT NULL DEFAULT now();
ALTER TABLE property_overview ADD COLUMN IF NOT EXISTS updated_at timestamp NOT NULL DEFAULT now();
ALTER TABLE property_overview ADD COLUMN IF NOT EXISTS created_by uuid;
ALTER TABLE property_overview ADD COLUMN IF NOT EXISTS updated_by uuid;

ALTER TABLE nearby_location_accessibility ADD COLUMN IF NOT EXISTS created_at timestamp NOT NULL DEFAULT now();
ALTER TABLE nearby_location_accessibility ADD COLUMN IF NOT EXISTS updated_at timestamp NOT NULL DEFAULT now();
ALTER TABLE nearby_location_accessibility ADD COLUMN IF NOT EXISTS created_by uuid;
ALTER TABLE nearby_location_accessibility ADD COLUMN IF NOT EXISTS updated_by uuid;

-- property: created_at / created_by already exist; add the missing updated_* columns.
ALTER TABLE property ADD COLUMN IF NOT EXISTS updated_at timestamp NOT NULL DEFAULT now();
ALTER TABLE property ADD COLUMN IF NOT EXISTS updated_by uuid;
-- created_by currently NOT NULL (holds UUIDs); keep as-is.

-- inventory_sync_state: updated_at exists; add created_at / created_by / updated_by.
ALTER TABLE inventory_sync_state ADD COLUMN IF NOT EXISTS created_at timestamp NOT NULL DEFAULT now();
ALTER TABLE inventory_sync_state ADD COLUMN IF NOT EXISTS created_by uuid;
ALTER TABLE inventory_sync_state ADD COLUMN IF NOT EXISTS updated_by uuid;
