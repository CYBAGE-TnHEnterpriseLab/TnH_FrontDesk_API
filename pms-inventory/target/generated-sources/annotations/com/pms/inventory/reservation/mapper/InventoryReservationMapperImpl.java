package com.pms.inventory.reservation.mapper;

import com.pms.inventory.reservation.dto.response.InventoryReservationResponse;
import com.pms.inventory.reservation.entity.InventoryReservation;
import com.pms.inventory.reservation.enums.InventoryReservationStatus;
import java.time.LocalDate;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-07-22T15:28:13+0530",
    comments = "version: 1.6.0, compiler: javac, environment: Java 21.0.2 (Oracle Corporation)"
)
@Component
public class InventoryReservationMapperImpl implements InventoryReservationMapper {

    @Override
    public InventoryReservationResponse toResponse(InventoryReservation reservation, boolean idempotent) {
        if ( reservation == null ) {
            return null;
        }

        UUID reservationId = null;
        UUID propertyId = null;
        UUID bookedRoomTypeId = null;
        UUID assignedRoomTypeId = null;
        LocalDate checkInDate = null;
        LocalDate checkOutDate = null;
        Integer quantity = null;
        InventoryReservationStatus status = null;
        if ( reservation != null ) {
            reservationId = reservation.getReservationId();
            propertyId = reservation.getPropertyId();
            bookedRoomTypeId = reservation.getBookedRoomTypeId();
            assignedRoomTypeId = reservation.getAssignedRoomTypeId();
            checkInDate = reservation.getCheckInDate();
            checkOutDate = reservation.getCheckOutDate();
            quantity = reservation.getQuantity();
            status = reservation.getStatus();
        }
        boolean idempotent1 = false;
        idempotent1 = idempotent;

        InventoryReservationResponse inventoryReservationResponse = new InventoryReservationResponse( reservationId, propertyId, bookedRoomTypeId, assignedRoomTypeId, checkInDate, checkOutDate, quantity, status, idempotent1 );

        return inventoryReservationResponse;
    }
}
