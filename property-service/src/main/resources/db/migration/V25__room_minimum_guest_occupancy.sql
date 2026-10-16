-- Existing room types must be explicitly configured before availability searches.
alter table room_outlet_type add column if not exists minimum_guest_occupancy integer;

