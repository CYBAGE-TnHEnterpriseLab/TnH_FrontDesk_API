-- Enforces the case-insensitive property code uniqueness contract atomically. The application
-- preflight (and the availability endpoint) compare with equalsIgnoreCase, but without this index
-- two concurrent publishes could both pass that check and persist the same code in different cases.
-- The service keeps property_code_lower in sync with lower(property_code); a null value marks a blank
-- code, which stays unconstrained because publish only enforces uniqueness for non-blank codes.
alter table property add column if not exists property_code_lower varchar(120);

create unique index if not exists uk_property_code_lower on property (property_code_lower);
