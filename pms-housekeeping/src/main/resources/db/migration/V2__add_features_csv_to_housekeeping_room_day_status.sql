-- V2__add_features_csv_to_housekeeping_room_day_status.sql

ALTER TABLE housekeeping_room_day_status
ADD COLUMN features_csv VARCHAR(500);