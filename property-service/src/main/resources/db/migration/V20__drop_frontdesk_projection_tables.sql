-- Drops legacy frontdesk projection objects in one idempotent statement.
-- Works for both PostgreSQL and H2 even when schema FRONTDESK is missing.
drop schema if exists frontdesk cascade;

