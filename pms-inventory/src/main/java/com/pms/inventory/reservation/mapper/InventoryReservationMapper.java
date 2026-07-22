package com.pms.inventory.reservation.mapper;

import com.pms.inventory.reservation.dto.response.InventoryReservationResponse;
import com.pms.inventory.reservation.entity.InventoryReservation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InventoryReservationMapper {

    @Mapping(target = "idempotent", source = "idempotent")
    InventoryReservationResponse toResponse(InventoryReservation reservation, boolean idempotent);
}

