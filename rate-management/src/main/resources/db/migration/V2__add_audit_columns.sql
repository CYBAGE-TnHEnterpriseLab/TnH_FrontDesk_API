-- Adds BaseEntity audit columns (created_at, updated_at, created_by, updated_by) to rate-management tables.
-- created_by/updated_by are UUID-backed to match the common BaseEntity audit contract.
-- Idempotent: every statement uses IF NOT EXISTS guards.

ALTER TABLE rate_db.rate_plan ADD COLUMN IF NOT EXISTS created_at timestamp NOT NULL DEFAULT now();
ALTER TABLE rate_db.rate_plan ADD COLUMN IF NOT EXISTS updated_at timestamp NOT NULL DEFAULT now();
ALTER TABLE rate_db.rate_plan ADD COLUMN IF NOT EXISTS created_by uuid;
ALTER TABLE rate_db.rate_plan ADD COLUMN IF NOT EXISTS updated_by uuid;

ALTER TABLE rate_db.policy_mapping ADD COLUMN IF NOT EXISTS created_at timestamp NOT NULL DEFAULT now();
ALTER TABLE rate_db.policy_mapping ADD COLUMN IF NOT EXISTS updated_at timestamp NOT NULL DEFAULT now();
ALTER TABLE rate_db.policy_mapping ADD COLUMN IF NOT EXISTS created_by uuid;
ALTER TABLE rate_db.policy_mapping ADD COLUMN IF NOT EXISTS updated_by uuid;

ALTER TABLE rate_db.master_room ADD COLUMN IF NOT EXISTS created_at timestamp NOT NULL DEFAULT now();
ALTER TABLE rate_db.master_room ADD COLUMN IF NOT EXISTS updated_at timestamp NOT NULL DEFAULT now();
ALTER TABLE rate_db.master_room ADD COLUMN IF NOT EXISTS created_by uuid;
ALTER TABLE rate_db.master_room ADD COLUMN IF NOT EXISTS updated_by uuid;

ALTER TABLE rate_db.master_room_pricing ADD COLUMN IF NOT EXISTS created_at timestamp NOT NULL DEFAULT now();
ALTER TABLE rate_db.master_room_pricing ADD COLUMN IF NOT EXISTS updated_at timestamp NOT NULL DEFAULT now();
ALTER TABLE rate_db.master_room_pricing ADD COLUMN IF NOT EXISTS created_by uuid;
ALTER TABLE rate_db.master_room_pricing ADD COLUMN IF NOT EXISTS updated_by uuid;

ALTER TABLE rate_db.master_room_room_type_mapping ADD COLUMN IF NOT EXISTS created_at timestamp NOT NULL DEFAULT now();
ALTER TABLE rate_db.master_room_room_type_mapping ADD COLUMN IF NOT EXISTS updated_at timestamp NOT NULL DEFAULT now();
ALTER TABLE rate_db.master_room_room_type_mapping ADD COLUMN IF NOT EXISTS created_by uuid;
ALTER TABLE rate_db.master_room_room_type_mapping ADD COLUMN IF NOT EXISTS updated_by uuid;
