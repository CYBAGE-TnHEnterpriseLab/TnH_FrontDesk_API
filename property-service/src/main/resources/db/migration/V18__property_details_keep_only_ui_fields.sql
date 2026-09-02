-- Keep Property Details schema aligned to UI fields by removing legacy email.
alter table property drop column if exists email;

