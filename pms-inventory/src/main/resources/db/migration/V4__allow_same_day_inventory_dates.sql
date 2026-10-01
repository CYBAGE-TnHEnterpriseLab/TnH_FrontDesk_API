ALTER TABLE pms_inventory.inventory_reservation
    DROP CONSTRAINT IF EXISTS chk_inventory_reservation_dates;

ALTER TABLE pms_inventory.inventory_reservation
    ADD CONSTRAINT chk_inventory_reservation_dates
        CHECK (check_out_date >= check_in_date);

ALTER TABLE pms_inventory.inventory_block
    DROP CONSTRAINT IF EXISTS chk_inventory_block_dates;

ALTER TABLE pms_inventory.inventory_block
    ADD CONSTRAINT chk_inventory_block_dates
        CHECK (to_date >= from_date);
