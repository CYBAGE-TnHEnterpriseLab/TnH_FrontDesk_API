alter table property_draft add column created_by uuid default '00000000-0000-0000-0000-000000000000' not null;
alter table property_draft add column updated_by uuid default '00000000-0000-0000-0000-000000000000' not null;
alter table property_draft add column published_by uuid;

alter table property add column created_by uuid default '00000000-0000-0000-0000-000000000000' not null;

