-- Adds missing BaseEntity audit columns to pms-inventory tables.
-- created_by/updated_by are UUID-backed to match the common BaseEntity audit contract.
-- Idempotent: every statement uses IF NOT EXISTS guards.

ALTER TABLE pms_inventory.room_type_inventory_daily ADD COLUMN IF NOT EXISTS created_by uuid;
ALTER TABLE pms_inventory.room_type_inventory_daily ADD COLUMN IF NOT EXISTS updated_by uuid;

ALTER TABLE pms_inventory.inventory_reservation ADD COLUMN IF NOT EXISTS created_by uuid;
ALTER TABLE pms_inventory.inventory_reservation ADD COLUMN IF NOT EXISTS updated_by uuid;

ALTER TABLE pms_inventory.inventory_block ADD COLUMN IF NOT EXISTS created_by uuid;
ALTER TABLE pms_inventory.inventory_block ADD COLUMN IF NOT EXISTS updated_by uuid;
