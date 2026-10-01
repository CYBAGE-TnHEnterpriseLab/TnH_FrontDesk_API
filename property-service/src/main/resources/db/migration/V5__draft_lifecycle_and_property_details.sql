alter table property_draft add column lifecycle_state varchar(32) default 'DRAFT' not null;
alter table property_draft add column current_step varchar(64) default 'PROPERTY_DETAILS' not null;
alter table property_draft add column completed_steps varchar(500) default '' not null;

alter table property add column email varchar(255) default '' not null;
alter table property add column contact_name varchar(255) default '' not null;
alter table property add column contact_number varchar(64) default '' not null;
alter table property add column time_zone varchar(120) default '' not null;
alter table property add column night_audit_time varchar(10) default '' not null;
alter table property add column check_in_time varchar(10) default '' not null;
alter table property add column check_out_time varchar(10) default '' not null;
alter table property add column status varchar(32) default 'ACTIVE' not null;

