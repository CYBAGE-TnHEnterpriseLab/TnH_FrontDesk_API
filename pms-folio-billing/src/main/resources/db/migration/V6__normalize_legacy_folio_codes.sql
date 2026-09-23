DO $$
DECLARE
    folio_record RECORD;
    candidate_code TEXT;
    code_point INTEGER;
BEGIN
    FOR folio_record IN
        SELECT id, confirmation_number
        FROM folio_db.folios
        WHERE UPPER(folio_code) LIKE 'A-B%'
        ORDER BY id
    LOOP
        candidate_code := NULL;

        FOR code_point IN 65..90 LOOP
            IF NOT EXISTS (
                SELECT 1
                FROM folio_db.folios existing_folio
                WHERE existing_folio.id <> folio_record.id
                  AND existing_folio.confirmation_number = folio_record.confirmation_number
                  AND existing_folio.folio_code = CHR(code_point)
            ) THEN
                candidate_code := CHR(code_point);
                EXIT;
            END IF;
        END LOOP;

        IF candidate_code IS NOT NULL THEN
            UPDATE folio_db.folios
            SET folio_code = candidate_code
            WHERE id = folio_record.id;
        END IF;
    END LOOP;
END $$;
