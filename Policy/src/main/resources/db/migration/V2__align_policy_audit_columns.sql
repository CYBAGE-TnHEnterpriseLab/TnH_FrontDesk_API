-- Aligns policies table with common BaseEntity audit contract.
-- Changes created_by from VARCHAR to UUID, and adds updated_at / updated_by.
-- Safe for both H2 (tests) and PostgreSQL (production).

ALTER TABLE policy_db.policies ADD COLUMN IF NOT EXISTS created_at timestamp NOT NULL DEFAULT now();
ALTER TABLE policy_db.policies ADD COLUMN IF NOT EXISTS updated_at timestamp NOT NULL DEFAULT now();
ALTER TABLE policy_db.policies ADD COLUMN IF NOT EXISTS updated_by uuid;

ALTER TABLE policy_db.policies ALTER COLUMN created_by TYPE uuid USING NULL;
