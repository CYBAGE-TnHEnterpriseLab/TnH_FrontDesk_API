CREATE UNIQUE INDEX IF NOT EXISTS uk_folio_confirmation_folio_code
    ON folio_db.folios (confirmation_number, folio_code);

ALTER TABLE folio_db.folios ALTER COLUMN property_id SET DEFAULT '';
ALTER TABLE folio_db.folios ALTER COLUMN folio_status SET DEFAULT 'OPEN';
ALTER TABLE folio_db.folios ALTER COLUMN folio_type SET DEFAULT 'ROOM';
ALTER TABLE folio_db.folios ALTER COLUMN folio_name SET DEFAULT '';
ALTER TABLE folio_db.folios ALTER COLUMN folio_balance SET DEFAULT 0;