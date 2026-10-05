CREATE TABLE IF NOT EXISTS loyalty_program (
    id UUID PRIMARY KEY,
    brand_id UUID NOT NULL,
    program_code VARCHAR(50) NOT NULL,
    program_name VARCHAR(150) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_loyalty_program_brand_code UNIQUE (brand_id, program_code)
);
