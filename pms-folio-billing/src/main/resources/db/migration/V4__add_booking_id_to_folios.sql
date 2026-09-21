ALTER TABLE folio_db.folios ADD COLUMN IF NOT EXISTS booking_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_folios_booking_id ON folio_db.folios (booking_id);
