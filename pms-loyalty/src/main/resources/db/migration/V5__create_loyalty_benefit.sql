CREATE TABLE IF NOT EXISTS loyalty_benefit (
    id UUID PRIMARY KEY,
    benefit_code VARCHAR(60) NOT NULL,
    benefit_name VARCHAR(150) NOT NULL,
    benefit_type VARCHAR(40) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_loyalty_benefit_code UNIQUE (benefit_code)
);
