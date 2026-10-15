-- Existing room types must be explicitly configured before availability searches.
alter table room_outlet_type add column minimum_guest_occupancy integer;

alter table room_outlet_type add constraint ck_room_guest_occupancy
    check (minimum_guest_occupancy >= 1 and minimum_guest_occupancy <= maximum_guest_occupancy);
