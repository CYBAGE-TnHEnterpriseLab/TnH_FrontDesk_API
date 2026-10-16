-- Enforces the case-insensitive property code uniqueness contract atomically. The application
-- preflight (and the availability endpoint) compare with equalsIgnoreCase, but without this index
-- two concurrent publishes could both pass that check and persist the same code in different cases.
-- The service keeps property_code_lower in sync with lower(property_code); a null value marks a blank
-- code, which stays unconstrained because publish only enforces uniqueness for non-blank codes.
alter table pms_property_wizard.property add column if not exists property_code_lower varchar(120);

update pms_property_wizard.property
set property_code_lower = lower(trim(property_code))
where property_code is not null and trim(property_code) <> '';

create unique index if not exists uk_property_code_lower on pms_property_wizard.property (property_code_lower);
