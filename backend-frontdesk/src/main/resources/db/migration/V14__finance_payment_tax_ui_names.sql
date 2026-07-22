alter table revenue_mapping rename column pms_item to charge_type;
alter table revenue_mapping rename column ledger_code to map_gl_account;
alter table revenue_mapping add column status varchar(40);
alter table revenue_mapping add column description varchar(255);

update revenue_mapping
set status = case
    when map_gl_account is null or trim(map_gl_account) = '' then 'PENDING'
    else 'MAPPED'
end
where status is null;

update revenue_mapping
set description = ''
where description is null;

alter table revenue_mapping alter column status set not null;

alter table payment_method rename column method_code to payment_method;
alter table payment_method rename column ledger_code to account_mapping;
alter table payment_method rename column online_enabled to allow_refund;
alter table payment_method add column active boolean;

update payment_method
set active = true
where active is null;

alter table payment_method alter column active set not null;

alter table tax_rule rename column tax_type to type;
alter table tax_rule rename column tax_value to rate;
alter table tax_rule rename column calculation_type to applicable_on;
alter table tax_rule rename column applies_per_night to active;
alter table tax_rule add column incl_excl varchar(20);
alter table tax_rule add column effective_date varchar(40);
alter table tax_rule add column status varchar(40);

update tax_rule
set incl_excl = 'EXCLUSIVE'
where incl_excl is null;

update tax_rule
set effective_date = '1970-01-01'
where effective_date is null;

update tax_rule
set status = case
    when active then 'ACTIVE'
    else 'INACTIVE'
end
where status is null;

alter table tax_rule alter column incl_excl set not null;
alter table tax_rule alter column effective_date set not null;
alter table tax_rule alter column status set not null;

