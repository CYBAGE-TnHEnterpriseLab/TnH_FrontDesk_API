-- Adds remaining BaseEntity audit columns to pms-reservation tables.
-- Idempotent: every statement uses IF NOT EXISTS guards.

-- reservation_bookings: created_at already exists; add created_by / updated_at / updated_by.
ALTER TABLE frontdeskdb.reservation_bookings ADD COLUMN IF NOT EXISTS created_by uuid;
ALTER TABLE frontdeskdb.reservation_bookings ADD COLUMN IF NOT EXISTS updated_at timestamp NOT NULL DEFAULT now();
ALTER TABLE frontdeskdb.reservation_bookings ADD COLUMN IF NOT EXISTS updated_by uuid;

-- reservation_checkin_audit: created_at already exists; add created_by / updated_at / updated_by.
ALTER TABLE frontdeskdb.reservation_checkin_audit ADD COLUMN IF NOT EXISTS created_by uuid;
ALTER TABLE frontdeskdb.reservation_checkin_audit ADD COLUMN IF NOT EXISTS updated_at timestamp NOT NULL DEFAULT now();
ALTER TABLE frontdeskdb.reservation_checkin_audit ADD COLUMN IF NOT EXISTS updated_by uuid;

-- reservation_checkin_signatures: created_at and updated_at exist; add created_by / updated_by.
ALTER TABLE frontdeskdb.reservation_checkin_signatures ADD COLUMN IF NOT EXISTS created_by uuid;
ALTER TABLE frontdeskdb.reservation_checkin_signatures ADD COLUMN IF NOT EXISTS updated_by uuid;

-- reservation_checkin_workflow: created_at and updated_at exist; add created_by / updated_by.
ALTER TABLE frontdeskdb.reservation_checkin_workflow ADD COLUMN IF NOT EXISTS created_by uuid;
ALTER TABLE frontdeskdb.reservation_checkin_workflow ADD COLUMN IF NOT EXISTS updated_by uuid;

-- reservation_payment_transactions: created_at already exists; add created_by / updated_at / updated_by.
ALTER TABLE frontdeskdb.reservation_payment_transactions ADD COLUMN IF NOT EXISTS created_by uuid;
ALTER TABLE frontdeskdb.reservation_payment_transactions ADD COLUMN IF NOT EXISTS updated_at timestamp NOT NULL DEFAULT now();
ALTER TABLE frontdeskdb.reservation_payment_transactions ADD COLUMN IF NOT EXISTS updated_by uuid;

-- arrival_records: add all audit columns.
ALTER TABLE frontdeskdb.arrival_records ADD COLUMN IF NOT EXISTS created_at timestamp NOT NULL DEFAULT now();
ALTER TABLE frontdeskdb.arrival_records ADD COLUMN IF NOT EXISTS created_by uuid;
ALTER TABLE frontdeskdb.arrival_records ADD COLUMN IF NOT EXISTS updated_at timestamp NOT NULL DEFAULT now();
ALTER TABLE frontdeskdb.arrival_records ADD COLUMN IF NOT EXISTS updated_by uuid;

-- departure_records: add all audit columns.
ALTER TABLE frontdeskdb.departure_records ADD COLUMN IF NOT EXISTS created_at timestamp NOT NULL DEFAULT now();
ALTER TABLE frontdeskdb.departure_records ADD COLUMN IF NOT EXISTS created_by uuid;
ALTER TABLE frontdeskdb.departure_records ADD COLUMN IF NOT EXISTS updated_at timestamp NOT NULL DEFAULT now();
ALTER TABLE frontdeskdb.departure_records ADD COLUMN IF NOT EXISTS updated_by uuid;
