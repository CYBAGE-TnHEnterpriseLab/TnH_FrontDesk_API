DO $$
DECLARE
    constraint_name TEXT;
BEGIN
    SELECT tc.constraint_name
      INTO constraint_name
      FROM information_schema.table_constraints tc
      JOIN information_schema.constraint_column_usage ccu
        ON ccu.constraint_schema = tc.constraint_schema
       AND ccu.constraint_name = tc.constraint_name
     WHERE tc.table_schema = current_schema()
       AND tc.table_name = 'folios'
       AND tc.constraint_type = 'UNIQUE'
     GROUP BY tc.constraint_name
    HAVING COUNT(*) = 1 AND MAX(ccu.column_name) = 'confirmation_number'
     LIMIT 1;

    IF constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE folios DROP CONSTRAINT %I', constraint_name);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_folio_confirmation_folio_code
    ON folios (confirmation_number, folio_code);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = current_schema() AND table_name = 'folios' AND column_name = 'property_id') THEN
        ALTER TABLE folios ALTER COLUMN property_id SET DEFAULT '';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = current_schema() AND table_name = 'folios' AND column_name = 'folio_status') THEN
        ALTER TABLE folios ALTER COLUMN folio_status SET DEFAULT 'OPEN';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = current_schema() AND table_name = 'folios' AND column_name = 'folio_type') THEN
        ALTER TABLE folios ALTER COLUMN folio_type SET DEFAULT 'ROOM';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = current_schema() AND table_name = 'folios' AND column_name = 'folio_name') THEN
        ALTER TABLE folios ALTER COLUMN folio_name SET DEFAULT '';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = current_schema() AND table_name = 'folios' AND column_name = 'folio_balance') THEN
        ALTER TABLE folios ALTER COLUMN folio_balance SET DEFAULT 0;
    END IF;
END $$;