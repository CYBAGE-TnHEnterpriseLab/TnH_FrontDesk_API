alter table property_draft add column created_by varchar(120) default 'system' not null;
alter table property_draft add column updated_by varchar(120) default 'system' not null;
alter table property_draft add column published_by varchar(120);

alter table property add column created_by varchar(120) default 'system' not null;

