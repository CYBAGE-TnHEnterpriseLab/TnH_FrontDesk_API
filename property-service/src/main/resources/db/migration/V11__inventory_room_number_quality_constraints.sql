update inventory_room
set room_number = trim(room_number)
where room_number is not null;

alter table inventory_room
add constraint ck_inventory_room_number_not_blank
check (length(trim(room_number)) > 0);

