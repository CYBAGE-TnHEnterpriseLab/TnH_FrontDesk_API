alter table property add column property_code varchar(120) default '' not null;
alter table property add column property_type varchar(80) default '' not null;
alter table property add column total_no_of_rooms integer default 0 not null;
alter table property add column total_no_of_floors integer default 0 not null;
alter table property add column state varchar(150) default '' not null;
alter table property add column zip_code varchar(40) default '' not null;
alter table property add column website varchar(255) default '' not null;

