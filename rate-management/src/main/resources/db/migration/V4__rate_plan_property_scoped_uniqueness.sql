ALTER TABLE rate_plan
    DROP CONSTRAINT IF EXISTS uk_rate_plan_code;

ALTER TABLE rate_plan
    DROP CONSTRAINT IF EXISTS uk_rate_plan_property_name,
    DROP CONSTRAINT IF EXISTS uk_rate_plan_property_code;

CREATE UNIQUE INDEX IF NOT EXISTS uk_rate_plan_property_name_ci
    ON rate_plan (LOWER(property_id), LOWER(name));

CREATE UNIQUE INDEX IF NOT EXISTS uk_rate_plan_property_code_ci
    ON rate_plan (LOWER(property_id), LOWER(code));