INSERT INTO loyalty_program (id, brand_id, program_code, program_name, status, created_at, updated_at)
VALUES ('12345678-1234-1234-1234-123456789012', '00000000-0000-0000-0000-000000000000', 'DEFAULT', 'Default Loyalty Program', 'ACTIVE', now(), now())
ON CONFLICT (id) DO NOTHING;

INSERT INTO loyalty_tier (id, loyalty_program_id, tier_code, tier_name, tier_level, active, created_at, updated_at)
VALUES ('22345678-1234-1234-1234-123456789012', '12345678-1234-1234-1234-123456789012', 'SILVER', 'Silver', 1, TRUE, now(), now())
ON CONFLICT (id) DO NOTHING;

INSERT INTO loyalty_tier (id, loyalty_program_id, tier_code, tier_name, tier_level, active, created_at, updated_at)
VALUES ('32345678-1234-1234-1234-123456789012', '12345678-1234-1234-1234-123456789012', 'GOLD', 'Gold', 2, TRUE, now(), now())
ON CONFLICT (id) DO NOTHING;

INSERT INTO loyalty_tier (id, loyalty_program_id, tier_code, tier_name, tier_level, active, created_at, updated_at)
VALUES ('42345678-1234-1234-1234-123456789012', '12345678-1234-1234-1234-123456789012', 'PLATINUM', 'Platinum', 3, TRUE, now(), now())
ON CONFLICT (id) DO NOTHING;
