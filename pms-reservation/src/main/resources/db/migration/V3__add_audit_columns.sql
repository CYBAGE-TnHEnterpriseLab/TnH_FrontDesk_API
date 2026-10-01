-- Adds missing BaseEntity audit columns to pms-reservation tables that now extend BaseEntity.
-- created_by/updated_by are UUID-backed to match the common BaseEntity audit contract.
-- Idempotent: every statement uses IF NOT EXISTS guards.

-- housekeeping_room_status: updated_at already exists; add created_at / created_by / updated_by.
ALTER TABLE frontdeskdb.housekeeping_room_status ADD COLUMN IF NOT EXISTS created_at timestamp NOT NULL DEFAULT now();
ALTER TABLE frontdeskdb.housekeeping_room_status ADD COLUMN IF NOT EXISTS created_by uuid;
ALTER TABLE frontdeskdb.housekeeping_room_status ADD COLUMN IF NOT EXISTS updated_by uuid;
