-- Folio transaction history is serialized as JSON and can exceed 255 characters.
-- This preserves existing values while removing the VARCHAR(255) limit.
ALTER TABLE folios
    ALTER COLUMN transactions_json TYPE TEXT;
