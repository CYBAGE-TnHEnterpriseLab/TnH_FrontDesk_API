package com.pms.inventory.reservation.service;

import com.pms.inventory.common.exception.InsufficientInventoryException;
import com.pms.inventory.common.exception.InventoryException;
import com.pms.inventory.inventory.entity.RoomTypeInventoryDaily;
import com.pms.inventory.inventory.service.InventoryService;
import com.pms.inventory.reservation.dto.request.ChangeAssignedRoomTypeRequest;
import com.pms.inventory.reservation.dto.request.ReserveInventoryRequest;
import com.pms.inventory.reservation.dto.response.InventoryReservationResponse;
import com.pms.inventory.reservation.entity.InventoryReservation;
import com.pms.inventory.reservation.enums.InventoryReservationStatus;
import com.pms.inventory.reservation.mapper.InventoryReservationMapper;
import com.pms.inventory.reservation.repository.InventoryReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryReservationServiceTest {

    @Mock
    private InventoryReservationRepository reservationRepository;
    @Mock
    private InventoryService inventoryService;
    @Mock
    private InventoryReservationMapper mapper;

    @InjectMocks
    private InventoryReservationService service;

    private ReserveInventoryRequest reserveRequest;
    private InventoryReservation reservation;

    @BeforeEach
    void setUp() {
        reserveRequest = new ReserveInventoryRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDate.of(2026, 7, 20),
                LocalDate.of(2026, 7, 22),
                1
        );

        reservation = InventoryReservation.builder()
                .reservationId(reserveRequest.reservationId())
                .propertyId(reserveRequest.propertyId())
                .bookedRoomTypeId(reserveRequest.bookedRoomTypeId())
                .assignedRoomTypeId(reserveRequest.assignedRoomTypeId())
                .checkInDate(reserveRequest.checkInDate())
                .checkOutDate(reserveRequest.checkOutDate())
                .quantity(reserveRequest.quantity())
                .status(InventoryReservationStatus.RESERVED)
                .build();
    }

    @Test
    void reserveInventorySuccessfully() {
        List<RoomTypeInventoryDaily> rows = List.of(new RoomTypeInventoryDaily());
        InventoryReservationResponse expected = new InventoryReservationResponse(
                reserveRequest.reservationId(),
                reserveRequest.propertyId(),
                reserveRequest.bookedRoomTypeId(),
                reserveRequest.assignedRoomTypeId(),
                reserveRequest.checkInDate(),
                reserveRequest.checkOutDate(),
                reserveRequest.quantity(),
                InventoryReservationStatus.RESERVED,
                false
        );

        when(reservationRepository.findByReservationId(reserveRequest.reservationId())).thenReturn(Optional.empty());
        when(inventoryService.lockInventoryRange(
                reserveRequest.propertyId(),
                reserveRequest.assignedRoomTypeId(),
                reserveRequest.checkInDate(),
                reserveRequest.checkOutDate()
        )).thenReturn(rows);
        when(reservationRepository.save(any(InventoryReservation.class))).thenReturn(reservation);
        when(mapper.toResponse(reservation, false)).thenReturn(expected);

        InventoryReservationResponse response = service.reserve(reserveRequest);

        assertEquals(expected, response);
        verify(inventoryService).ensureSufficientInventory(rows, 1);
        verify(inventoryService).increaseReserved(rows, 1);
    }

    @Test
    void reserveInventoryFailsWhenInsufficient() {
        List<RoomTypeInventoryDaily> rows = List.of(new RoomTypeInventoryDaily());
        when(reservationRepository.findByReservationId(reserveRequest.reservationId())).thenReturn(Optional.empty());
        when(inventoryService.lockInventoryRange(any(), any(), any(), any())).thenReturn(rows);
        doThrow(new InsufficientInventoryException("Insufficient")).when(inventoryService).ensureSufficientInventory(rows, 1);

        assertThrows(InsufficientInventoryException.class, () -> service.reserve(reserveRequest));
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void duplicateReservationRequestReturnsIdempotentResponse() {
        InventoryReservationResponse expected = new InventoryReservationResponse(
                reservation.getReservationId(),
                reservation.getPropertyId(),
                reservation.getBookedRoomTypeId(),
                reservation.getAssignedRoomTypeId(),
                reservation.getCheckInDate(),
                reservation.getCheckOutDate(),
                reservation.getQuantity(),
                InventoryReservationStatus.RESERVED,
                true
        );
        when(reservationRepository.findByReservationId(reserveRequest.reservationId())).thenReturn(Optional.of(reservation));
        when(mapper.toResponse(reservation, true)).thenReturn(expected);

        InventoryReservationResponse response = service.reserve(reserveRequest);

        assertEquals(true, response.idempotent());
        verify(inventoryService, never()).lockInventoryRange(any(), any(), any(), any());
    }

    @Test
    void duplicateReservationWithReleasedStatusThrowsConflict() {
        reservation.setStatus(InventoryReservationStatus.RELEASED);
        when(reservationRepository.findByReservationId(reserveRequest.reservationId())).thenReturn(Optional.of(reservation));

        assertThrows(InventoryException.class, () -> service.reserve(reserveRequest));
    }

    @Test
    void releaseInventorySuccessfully() {
        List<RoomTypeInventoryDaily> rows = List.of(new RoomTypeInventoryDaily());
        InventoryReservationResponse expected = new InventoryReservationResponse(
                reservation.getReservationId(), reservation.getPropertyId(), reservation.getBookedRoomTypeId(),
                reservation.getAssignedRoomTypeId(), reservation.getCheckInDate(), reservation.getCheckOutDate(),
                reservation.getQuantity(), InventoryReservationStatus.RELEASED, false
        );

        when(reservationRepository.findByReservationId(reservation.getReservationId())).thenReturn(Optional.of(reservation));
        when(inventoryService.lockInventoryRange(any(), any(), any(), any())).thenReturn(rows);
        when(mapper.toResponse(any(InventoryReservation.class), eq(false))).thenReturn(expected);

        InventoryReservationResponse response = service.release(reservation.getReservationId());

        assertEquals(InventoryReservationStatus.RELEASED, reservation.getStatus());
        assertEquals(expected, response);
        verify(inventoryService).decreaseReserved(rows, reservation.getQuantity());
    }

    @Test
    void duplicateReleaseIsIdempotent() {
        reservation.setStatus(InventoryReservationStatus.RELEASED);
        InventoryReservationResponse expected = new InventoryReservationResponse(
                reservation.getReservationId(), reservation.getPropertyId(), reservation.getBookedRoomTypeId(),
                reservation.getAssignedRoomTypeId(), reservation.getCheckInDate(), reservation.getCheckOutDate(),
                reservation.getQuantity(), InventoryReservationStatus.RELEASED, true
        );

        when(reservationRepository.findByReservationId(reservation.getReservationId())).thenReturn(Optional.of(reservation));
        when(mapper.toResponse(reservation, true)).thenReturn(expected);

        InventoryReservationResponse response = service.release(reservation.getReservationId());

        assertEquals(true, response.idempotent());
        verify(inventoryService, never()).decreaseReserved(anyList(), anyInt());
    }

    @Test
    void roomTypeReassignmentFromDeluxeToSuiteSuccess() {
        UUID newRoomType = UUID.randomUUID();
        List<RoomTypeInventoryDaily> oldRows = List.of(new RoomTypeInventoryDaily());
        List<RoomTypeInventoryDaily> newRows = List.of(new RoomTypeInventoryDaily());
        InventoryReservationResponse expected = new InventoryReservationResponse(
                reservation.getReservationId(), reservation.getPropertyId(), reservation.getBookedRoomTypeId(),
                newRoomType, reservation.getCheckInDate(), reservation.getCheckOutDate(), reservation.getQuantity(),
                InventoryReservationStatus.RESERVED, false
        );

        when(reservationRepository.findByReservationId(reservation.getReservationId())).thenReturn(Optional.of(reservation));
        when(inventoryService.lockInventoryRange(
                reservation.getPropertyId(), reservation.getAssignedRoomTypeId(), reservation.getCheckInDate(), reservation.getCheckOutDate()
        )).thenReturn(oldRows);
        when(inventoryService.lockInventoryRange(
                reservation.getPropertyId(), newRoomType, reservation.getCheckInDate(), reservation.getCheckOutDate()
        )).thenReturn(newRows);
        when(mapper.toResponse(any(InventoryReservation.class), eq(false))).thenReturn(expected);

        InventoryReservationResponse response = service.changeAssignedRoomType(
                reservation.getReservationId(),
                new ChangeAssignedRoomTypeRequest(newRoomType)
        );

        assertEquals(newRoomType, reservation.getAssignedRoomTypeId());
        assertEquals(expected, response);
        verify(inventoryService).decreaseReserved(oldRows, reservation.getQuantity());
        verify(inventoryService).increaseReserved(newRows, reservation.getQuantity());
    }

    @Test
    void roomTypeReassignmentFailsWhenUnavailable() {
        UUID newRoomType = UUID.randomUUID();
        List<RoomTypeInventoryDaily> oldRows = List.of(new RoomTypeInventoryDaily());
        List<RoomTypeInventoryDaily> newRows = List.of(new RoomTypeInventoryDaily());

        when(reservationRepository.findByReservationId(reservation.getReservationId())).thenReturn(Optional.of(reservation));
        when(inventoryService.lockInventoryRange(
                reservation.getPropertyId(), reservation.getAssignedRoomTypeId(), reservation.getCheckInDate(), reservation.getCheckOutDate()
        )).thenReturn(oldRows);
        when(inventoryService.lockInventoryRange(
                reservation.getPropertyId(), newRoomType, reservation.getCheckInDate(), reservation.getCheckOutDate()
        )).thenReturn(newRows);
        doThrow(new InsufficientInventoryException("No inventory")).when(inventoryService)
                .ensureSufficientInventory(newRows, reservation.getQuantity());

        assertThrows(InsufficientInventoryException.class,
                () -> service.changeAssignedRoomType(reservation.getReservationId(), new ChangeAssignedRoomTypeRequest(newRoomType)));
        verify(inventoryService, never()).decreaseReserved(oldRows, reservation.getQuantity());
    }

    @Test
    void checkoutDateIsExclusiveOnLockingRange() {
        List<RoomTypeInventoryDaily> rows = List.of(new RoomTypeInventoryDaily());
        when(reservationRepository.findByReservationId(reserveRequest.reservationId())).thenReturn(Optional.empty());
        when(inventoryService.lockInventoryRange(any(), any(), any(), any())).thenReturn(rows);
        when(reservationRepository.save(any(InventoryReservation.class))).thenReturn(reservation);
        when(mapper.toResponse(any(InventoryReservation.class), eq(false))).thenReturn(new InventoryReservationResponse(
                reservation.getReservationId(), reservation.getPropertyId(), reservation.getBookedRoomTypeId(),
                reservation.getAssignedRoomTypeId(), reservation.getCheckInDate(), reservation.getCheckOutDate(),
                reservation.getQuantity(), InventoryReservationStatus.RESERVED, false
        ));

        service.reserve(reserveRequest);

        ArgumentCaptor<LocalDate> checkOutCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(inventoryService).lockInventoryRange(
                eq(reserveRequest.propertyId()),
                eq(reserveRequest.assignedRoomTypeId()),
                eq(reserveRequest.checkInDate()),
                checkOutCaptor.capture()
        );
        assertEquals(reserveRequest.checkOutDate(), checkOutCaptor.getValue());
    }
}


