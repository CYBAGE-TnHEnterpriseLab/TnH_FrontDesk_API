package com.pms.inventory.reservation.service;

import com.pms.inventory.common.exception.InventoryException;
import com.pms.inventory.common.exception.InventoryNotFoundException;
import com.pms.inventory.inventory.entity.RoomTypeInventoryDaily;
import com.pms.inventory.inventory.service.InventoryService;
import com.pms.inventory.reservation.dto.request.ChangeAssignedRoomTypeRequest;
import com.pms.inventory.reservation.dto.request.ReserveInventoryRequest;
import com.pms.inventory.reservation.dto.response.InventoryReservationResponse;
import com.pms.inventory.reservation.entity.InventoryReservation;
import com.pms.inventory.reservation.enums.InventoryReservationStatus;
import com.pms.inventory.reservation.mapper.InventoryReservationMapper;
import com.pms.inventory.reservation.repository.InventoryReservationRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class InventoryReservationService {

    private final InventoryReservationRepository reservationRepository;
    private final InventoryService inventoryService;
    private final InventoryReservationMapper reservationMapper;

    public InventoryReservationService(
            InventoryReservationRepository reservationRepository,
            InventoryService inventoryService,
            InventoryReservationMapper reservationMapper
    ) {
        this.reservationRepository = reservationRepository;
        this.inventoryService = inventoryService;
        this.reservationMapper = reservationMapper;
    }

    @Transactional
    public InventoryReservationResponse reserve(ReserveInventoryRequest request) {
        InventoryReservation existing = reservationRepository.findByReservationId(request.reservationId()).orElse(null);
        if (existing != null) {
            if (existing.getStatus() == InventoryReservationStatus.RESERVED) {
                return reservationMapper.toResponse(existing, true);
            }
            throw new InventoryException("Reservation is already processed with status " + existing.getStatus());
        }

        List<RoomTypeInventoryDaily> rows = inventoryService.lockInventoryRange(
                request.propertyId(),
                request.assignedRoomTypeId(),
                request.checkInDate(),
                request.checkOutDate()
        );

        inventoryService.ensureSufficientInventory(rows, request.quantity());
        inventoryService.increaseReserved(rows, request.quantity());

        InventoryReservation reservation = InventoryReservation.builder()
                .reservationId(request.reservationId())
                .propertyId(request.propertyId())
                .bookedRoomTypeId(request.bookedRoomTypeId())
                .assignedRoomTypeId(request.assignedRoomTypeId())
                .checkInDate(request.checkInDate())
                .checkOutDate(request.checkOutDate())
                .quantity(request.quantity())
                .status(InventoryReservationStatus.RESERVED)
                .build();

        try {
            reservation = reservationRepository.save(reservation);
        } catch (DataIntegrityViolationException ex) {
            InventoryReservation duplicate = reservationRepository.findByReservationId(request.reservationId())
                    .orElseThrow(() -> ex);
            if (duplicate.getStatus() == InventoryReservationStatus.RESERVED) {
                return reservationMapper.toResponse(duplicate, true);
            }
            throw ex;
        }

        return reservationMapper.toResponse(reservation, false);
    }

    @Transactional
    public InventoryReservationResponse release(UUID reservationId) {
        InventoryReservation reservation = reservationRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory reservation not found"));

        if (reservation.getStatus() == InventoryReservationStatus.RELEASED
                || reservation.getStatus() == InventoryReservationStatus.CANCELLED) {
            return reservationMapper.toResponse(reservation, true);
        }

        List<RoomTypeInventoryDaily> rows = inventoryService.lockInventoryRange(
                reservation.getPropertyId(),
                reservation.getAssignedRoomTypeId(),
                reservation.getCheckInDate(),
                reservation.getCheckOutDate()
        );
        inventoryService.decreaseReserved(rows, reservation.getQuantity());

        reservation.setStatus(InventoryReservationStatus.RELEASED);

        return reservationMapper.toResponse(reservation, false);
    }

    @Transactional
    public InventoryReservationResponse changeAssignedRoomType(
            UUID reservationId,
            ChangeAssignedRoomTypeRequest request
    ) {
        InventoryReservation reservation = reservationRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory reservation not found"));

        if (reservation.getStatus() != InventoryReservationStatus.RESERVED) {
            throw new InventoryException("Only active reservations can be reassigned");
        }
        if (reservation.getAssignedRoomTypeId().equals(request.assignedRoomTypeId())) {
            return reservationMapper.toResponse(reservation, true);
        }

        List<RoomTypeInventoryDaily> oldRows = inventoryService.lockInventoryRange(
                reservation.getPropertyId(),
                reservation.getAssignedRoomTypeId(),
                reservation.getCheckInDate(),
                reservation.getCheckOutDate()
        );
        List<RoomTypeInventoryDaily> newRows = inventoryService.lockInventoryRange(
                reservation.getPropertyId(),
                request.assignedRoomTypeId(),
                reservation.getCheckInDate(),
                reservation.getCheckOutDate()
        );

        inventoryService.ensureSufficientInventory(newRows, reservation.getQuantity());
        inventoryService.decreaseReserved(oldRows, reservation.getQuantity());
        inventoryService.increaseReserved(newRows, reservation.getQuantity());

        reservation.setAssignedRoomTypeId(request.assignedRoomTypeId());

        return reservationMapper.toResponse(reservation, false);
    }
}

