-- =====================================================
-- Update is_sellable default from FALSE to TRUE
-- =====================================================

ALTER TABLE pms_housekeeping.housekeeping_room_day_status
    ALTER COLUMN is_sellable SET DEFAULT TRUE;
