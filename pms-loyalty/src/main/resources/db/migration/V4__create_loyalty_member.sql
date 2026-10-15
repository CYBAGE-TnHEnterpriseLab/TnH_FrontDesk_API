CREATE TABLE IF NOT EXISTS loyalty_member (
    id UUID PRIMARY KEY,
    loyalty_program_id UUID NOT NULL,
    guest_id UUID NOT NULL,
    loyalty_number VARCHAR(30) NOT NULL,
    current_tier_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    enrollment_source VARCHAR(30) NOT NULL,
    enrolled_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_loyalty_member_number UNIQUE (loyalty_number),
    CONSTRAINT uq_loyalty_member_program_guest UNIQUE (loyalty_program_id, guest_id)
);

CREATE INDEX IF NOT EXISTS idx_loyalty_member_guest_id ON loyalty_member(guest_id);
CREATE INDEX IF NOT EXISTS idx_loyalty_member_program_id ON loyalty_member(loyalty_program_id);
CREATE INDEX IF NOT EXISTS idx_loyalty_member_current_tier_id ON loyalty_member(current_tier_id);
