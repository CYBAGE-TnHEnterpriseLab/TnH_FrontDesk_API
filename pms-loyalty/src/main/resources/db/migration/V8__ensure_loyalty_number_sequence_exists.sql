-- The sequence is addressed through a schema-qualified name, so make sure it exists in the schema the
-- entities use. Kept as its own migration because editing V3 would fail checksum validation on
-- databases where V3 has already been applied.
CREATE SEQUENCE IF NOT EXISTS pms_loyalty.loyalty_number_seq
    START WITH 10000000
    INCREMENT BY 1
    MINVALUE 10000000;
