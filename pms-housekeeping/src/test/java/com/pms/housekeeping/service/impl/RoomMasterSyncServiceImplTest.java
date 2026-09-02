package com.pms.housekeeping.service.impl;

import com.pms.housekeeping.dto.request.RoomMasterSyncRequest;
import com.pms.housekeeping.dto.response.RoomMasterSyncResponse;
import com.pms.housekeeping.entity.CleaningStatus;
import com.pms.housekeeping.entity.FrontOfficeStatus;
import com.pms.housekeeping.entity.HousekeepingPriority;
import com.pms.housekeeping.entity.HousekeepingRoomDayStatus;
import com.pms.housekeeping.entity.RoomMasterProjection;
import com.pms.housekeeping.entity.ReservationStatus;
import com.pms.housekeeping.repository.HousekeepingRoomDayStatusRepository;
import com.pms.housekeeping.repository.RoomMasterProjectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomMasterSyncServiceImplTest {

    @Mock
    private RoomMasterProjectionRepository roomMasterProjectionRepository;

    @Mock
    private HousekeepingRoomDayStatusRepository dayStatusRepository;

    @Captor
    private ArgumentCaptor<List<RoomMasterProjection>> projectionListCaptor;

    @Captor
    private ArgumentCaptor<List<HousekeepingRoomDayStatus>> statusListCaptor;

    private RoomMasterSyncServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RoomMasterSyncServiceImpl(
                roomMasterProjectionRepository, dayStatusRepository);
    }

    @Test
    void sync_shouldUpsertIncomingRoomsSeedDaysAndDeactivateMissingRooms() {
        String propertyId = UUID.randomUUID().toString();
        UUID roomTypeId = UUID.randomUUID();
        LocalDate fromDate = LocalDate.of(2026, 8, 18);
        LocalDate toDate = LocalDate.of(2026, 8, 19);

        RoomMasterProjection activeExisting = roomProjection(propertyId, "101", roomTypeId, true);
        RoomMasterProjection inactiveExisting = roomProjection(propertyId, "103", roomTypeId, false);
        when(roomMasterProjectionRepository.findAllByPropertyId(propertyId)).thenReturn(List.of(
                activeExisting,
                roomProjection(propertyId, "102", roomTypeId, true),
                inactiveExisting));

        HousekeepingRoomDayStatus existingDay = housekeepingStatus(
                propertyId, fromDate, "101", CleaningStatus.CLEAN, FrontOfficeStatus.VACANT, ReservationStatus.NOT_RESERVED);
        existingDay.setConfirmationId(null);
        existingDay.setRoomTypeId(roomTypeId);
        existingDay.setRoomTypeName("Deluxe");
        existingDay.setFloor("1");
        existingDay.setSellable(true);
        when(dayStatusRepository.findAllByPropertyIdAndBusinessDateBetween(eq(propertyId), eq(fromDate), eq(toDate)))
                .thenReturn(List.of(existingDay));

        when(roomMasterProjectionRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(dayStatusRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        RoomMasterSyncRequest request = new RoomMasterSyncRequest(
                propertyId,
                fromDate,
                toDate,
                List.of(new RoomMasterSyncRequest.RoomMasterUnit(
                        roomTypeId,
                        "Deluxe",
                        "101",
                        "1",
                        "North",
                        "CLASS-A",
                        "WiFi",
                        true,
                        true
                ))
        );

        RoomMasterSyncResponse response = service.sync(request);

        assertThat(response.syncedRooms()).isEqualTo(1);
        assertThat(response.deactivatedRooms()).isEqualTo(1);

        verify(roomMasterProjectionRepository, org.mockito.Mockito.times(2)).saveAll(projectionListCaptor.capture());
        List<RoomMasterProjection> savedProjections = new ArrayList<>();
        projectionListCaptor.getAllValues().forEach(savedProjections::addAll);
        assertThat(savedProjections).extracting(RoomMasterProjection::getRoomNumber).containsExactlyInAnyOrder("101", "102");
        assertThat(savedProjections).extracting(RoomMasterProjection::isActive).containsExactlyInAnyOrder(true, false);

        verify(dayStatusRepository, org.mockito.Mockito.times(1)).saveAll(statusListCaptor.capture());
        List<HousekeepingRoomDayStatus> savedStatuses = statusListCaptor.getValue();
        assertThat(savedStatuses).hasSize(2);
        assertThat(savedStatuses).extracting(HousekeepingRoomDayStatus::getRoomNumber).containsOnly("101");
        assertThat(savedStatuses).filteredOn(s -> s.getBusinessDate().equals(fromDate))
                .singleElement().satisfies(s -> assertThat(s.isSellable()).isTrue());
        assertThat(savedStatuses).filteredOn(s -> s.getBusinessDate().equals(toDate))
                .singleElement().satisfies(s -> assertThat(s.isSellable()).isFalse());
    }

    @Test
    void sync_shouldSkipDayCreationWhenFromDateIsAfterToDate() {
        String propertyId = UUID.randomUUID().toString();
        UUID roomTypeId = UUID.randomUUID();
        LocalDate fromDate = LocalDate.of(2026, 8, 20);
        LocalDate toDate = LocalDate.of(2026, 8, 18);

        when(roomMasterProjectionRepository.findAllByPropertyId(propertyId)).thenReturn(List.of());
        when(roomMasterProjectionRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        RoomMasterSyncRequest request = new RoomMasterSyncRequest(
                propertyId,
                fromDate,
                toDate,
                List.of(new RoomMasterSyncRequest.RoomMasterUnit(
                        roomTypeId,
                        "Suite",
                        "201",
                        "2",
                        "South",
                        "CLASS-B",
                        null,
                        false,
                        true
                ))
        );

        RoomMasterSyncResponse response = service.sync(request);

        assertThat(response.syncedRooms()).isEqualTo(1);
        assertThat(response.deactivatedRooms()).isEqualTo(0);
        verify(dayStatusRepository, never()).saveAll(anyList());
    }

    private static RoomMasterProjection roomProjection(String propertyId, String roomNumber, UUID roomTypeId, boolean active) {
        return RoomMasterProjection.builder()
                .propertyId(propertyId)
                .roomNumber(roomNumber)
                .roomTypeId(roomTypeId)
                .roomTypeName("Deluxe")
                .floor("1")
                .zone("North")
                .roomClass("CLASS-A")
                .featuresCsv("WiFi")
                .vipCapable(true)
                .active(active)
                .createdAt(LocalDateTime.of(2026, 8, 18, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 8, 18, 9, 0))
                .build();
    }

    private static HousekeepingRoomDayStatus housekeepingStatus(
            String propertyId,
            LocalDate businessDate,
            String roomNumber,
            CleaningStatus cleaningStatus,
            FrontOfficeStatus frontOfficeStatus,
            ReservationStatus reservationStatus
    ) {
        return HousekeepingRoomDayStatus.builder()
                .propertyId(propertyId)
                .businessDate(businessDate)
                .roomNumber(roomNumber)
                .roomTypeId(UUID.randomUUID())
                .roomTypeName("Deluxe")
                .floor("1")
                .cleaningStatus(cleaningStatus)
                .frontOfficeStatus(frontOfficeStatus)
                .reservationStatus(reservationStatus)
                .priority(HousekeepingPriority.NORMAL)
                .sellable(false)
                .createdAt(LocalDateTime.of(2026, 8, 18, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 8, 18, 9, 0))
                .build();
    }
}
