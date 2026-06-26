create schema if not exists property_wizard;

-- Move existing application tables from public to property_wizard.
-- Keep Flyway metadata in public so migration tracking remains intact.
do $$
declare
    table_rec record;
    seq_rec record;
begin
    for table_rec in
        select tablename
        from pg_tables
        where schemaname = 'public'
          and tablename <> 'flyway_schema_history'
    loop
        execute format('alter table public.%I set schema property_wizard', table_rec.tablename);
    end loop;

    for seq_rec in
        select sequencename
        from pg_sequences
        where schemaname = 'public'
    loop
        execute format('alter sequence public.%I set schema property_wizard', seq_rec.sequencename);
    end loop;
end
$$;
