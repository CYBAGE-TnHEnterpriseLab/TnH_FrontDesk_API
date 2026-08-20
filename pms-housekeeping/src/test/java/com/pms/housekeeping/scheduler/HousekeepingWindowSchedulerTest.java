package com.pms.housekeeping.scheduler;

import com.pms.housekeeping.dto.request.RoomMasterSyncRequest;
import com.pms.housekeeping.entity.RoomMasterProjection;
import com.pms.housekeeping.repository.RoomMasterProjectionRepository;
import com.pms.housekeeping.service.RoomMasterSyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HousekeepingWindowSchedulerTest {

    @Mock
    private RoomMasterProjectionRepository roomMasterProjectionRepository;

    @Mock
    private RoomMasterSyncService roomMasterSyncService;

    @Captor
    private ArgumentCaptor<RoomMasterSyncRequest> requestCaptor;

    @Test
    void addTailDayForAllProperties_shouldSyncActiveRoomsGroupedByProperty() {
        UUID propertyA = UUID.randomUUID();
        UUID propertyB = UUID.randomUUID();

        RoomMasterProjection activeA1 = projection(propertyA, "101", true);
        RoomMasterProjection activeA2 = projection(propertyA, "102", true);
        RoomMasterProjection inactiveA = projection(propertyA, "199", false);
        RoomMasterProjection activeB1 = projection(propertyB, "201", true);

        when(roomMasterProjectionRepository.findAll()).thenReturn(List.of(activeA1, activeA2, inactiveA, activeB1));

        HousekeepingWindowScheduler scheduler = new HousekeepingWindowScheduler(
                roomMasterProjectionRepository,
                roomMasterSyncService,
                30
        );

        scheduler.addTailDayForAllProperties();

        verify(roomMasterSyncService, org.mockito.Mockito.times(2)).sync(requestCaptor.capture());
        List<RoomMasterSyncRequest> requests = requestCaptor.getAllValues();

        LocalDate expectedFromDate = LocalDate.now().plusDays(30);
        LocalDate expectedToDate = expectedFromDate.plusDays(1);

        assertThat(requests).allMatch(req -> req.fromDate().equals(expectedFromDate) && req.toDate().equals(expectedToDate));

        RoomMasterSyncRequest requestForA = requests.stream().filter(r -> r.propertyId().equals(propertyA)).findFirst().orElseThrow();
        RoomMasterSyncRequest requestForB = requests.stream().filter(r -> r.propertyId().equals(propertyB)).findFirst().orElseThrow();

        assertThat(requestForA.rooms()).extracting(RoomMasterSyncRequest.RoomMasterUnit::roomNumber)
                .containsExactlyInAnyOrder("101", "102");
        assertThat(requestForA.rooms()).extracting(RoomMasterSyncRequest.RoomMasterUnit::active)
                .containsOnly(true);
        assertThat(requestForB.rooms()).extracting(RoomMasterSyncRequest.RoomMasterUnit::roomNumber)
                .containsExactly("201");
    }

    @Test
    void addTailDayForAllProperties_shouldClampHorizonToOneAndSkipWhenNoActiveRooms() {
        UUID propertyId = UUID.randomUUID();
        when(roomMasterProjectionRepository.findAll()).thenReturn(List.of(projection(propertyId, "301", false)));

        HousekeepingWindowScheduler scheduler = new HousekeepingWindowScheduler(
                roomMasterProjectionRepository,
                roomMasterSyncService,
                0
        );

        scheduler.addTailDayForAllProperties();

        verify(roomMasterSyncService, never()).sync(org.mockito.ArgumentMatchers.any());
    }

    private static RoomMasterProjection projection(UUID propertyId, String roomNumber, boolean active) {
        return RoomMasterProjection.builder()
                .propertyId(propertyId)
                .roomNumber(roomNumber)
                .roomTypeId(UUID.randomUUID())
                .roomTypeName("Deluxe")
                .floor("1")
                .zone("A")
                .roomClass("CLASS-A")
                .featuresCsv("WiFi")
                .vipCapable(true)
                .active(active)
                .createdAt(LocalDateTime.of(2026, 8, 18, 0, 0))
                .updatedAt(LocalDateTime.of(2026, 8, 18, 0, 0))
                .build();
    }
}

