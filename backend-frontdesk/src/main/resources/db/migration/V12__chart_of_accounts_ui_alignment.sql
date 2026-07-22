alter table chart_of_account add column ledger_type varchar(80);
alter table chart_of_account add column active boolean;

update chart_of_account
set ledger_type = 'UNSPECIFIED'
where ledger_type is null;

update chart_of_account
set active = true
where active is null;

alter table chart_of_account alter column ledger_type set not null;
alter table chart_of_account alter column active set not null;

