CREATE TABLE IF NOT EXISTS rate_db.rate_plan_active_day (
    rate_plan_id BIGINT NOT NULL,
    day_of_week VARCHAR(20) NOT NULL,
    CONSTRAINT uk_rate_plan_active_day UNIQUE (rate_plan_id, day_of_week),
    CONSTRAINT fk_rate_plan_active_day_rate_plan
        FOREIGN KEY (rate_plan_id) REFERENCES rate_db.rate_plan (id) ON DELETE CASCADE
);

INSERT INTO rate_db.rate_plan_active_day (rate_plan_id, day_of_week)
SELECT rp.id, days.day_of_week
FROM rate_db.rate_plan rp
CROSS JOIN (VALUES
    ('MONDAY'), ('TUESDAY'), ('WEDNESDAY'), ('THURSDAY'),
    ('FRIDAY'), ('SATURDAY'), ('SUNDAY')
) AS days(day_of_week)
ON CONFLICT (rate_plan_id, day_of_week) DO NOTHING;