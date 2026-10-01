ALTER TABLE folio_db.folios
    DROP CONSTRAINT IF EXISTS uk_folio_confirmation_folio_code;

DROP INDEX IF EXISTS folio_db.uk_folio_confirmation_folio_code;

CREATE UNIQUE INDEX IF NOT EXISTS uk_folio_booking_folio_code
    ON folio_db.folios (booking_id, folio_code)
    WHERE booking_id IS NOT NULL;