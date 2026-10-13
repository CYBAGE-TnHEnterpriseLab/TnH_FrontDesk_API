CREATE TABLE IF NOT EXISTS loyalty_tier_benefit (
    id UUID PRIMARY KEY,
    tier_id UUID NOT NULL,
    benefit_id UUID NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    configuration JSONB,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_loyalty_tier_benefit_tier_benefit UNIQUE (tier_id, benefit_id)
);

CREATE INDEX IF NOT EXISTS idx_loyalty_tier_benefit_tier_id ON loyalty_tier_benefit(tier_id);
