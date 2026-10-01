ALTER TABLE frontdeskdb.reservation_checkin_signatures
    ADD COLUMN IF NOT EXISTS check_in_channel VARCHAR(20);

ALTER TABLE frontdeskdb.reservation_checkin_id_proofs
    ADD COLUMN IF NOT EXISTS check_in_channel VARCHAR(20);
