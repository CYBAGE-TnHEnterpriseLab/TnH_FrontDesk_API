-- Ensure property_code_lower column exists and is properly populated
-- This migration handles cases where V24 migration may not have run correctly
alter table pms_property_wizard.property add column if not exists property_code_lower varchar(120);

-- Populate property_code_lower for existing records
update pms_property_wizard.property
set property_code_lower = lower(trim(property_code))
where property_code is not null
  and trim(property_code) <> ''
  and property_code_lower is null;

-- Create unique index if it doesn't exist
create unique index if not exists uk_property_code_lower on pms_property_wizard.property (property_code_lower);

