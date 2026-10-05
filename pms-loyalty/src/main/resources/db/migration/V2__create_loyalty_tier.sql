CREATE TABLE IF NOT EXISTS loyalty_tier (
    id UUID PRIMARY KEY,
    loyalty_program_id UUID NOT NULL,
    tier_code VARCHAR(30) NOT NULL,
    tier_name VARCHAR(100) NOT NULL,
    tier_level SMALLINT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_loyalty_tier_program_code UNIQUE (loyalty_program_id, tier_code),
    CONSTRAINT uq_loyalty_tier_program_level UNIQUE (loyalty_program_id, tier_level)
);
