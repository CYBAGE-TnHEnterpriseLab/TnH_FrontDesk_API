package com.pms.housekeeping.controller;

import com.pms.housekeeping.dto.request.HousekeepingRoomFilterRequest;
import com.pms.housekeeping.dto.request.UpdateHousekeepingRoomDetailsRequest;
import com.pms.housekeeping.dto.response.AssignableRoomResponse;
import com.pms.housekeeping.dto.response.HousekeepingCalendarResponse;
import com.pms.housekeeping.dto.response.HousekeepingDashboardResponse;
import com.pms.housekeeping.dto.response.HousekeepingRoomsPageResponse;
import com.pms.housekeeping.dto.response.HousekeepingRoomDetailsUpdateResponse;
import com.pms.housekeeping.entity.CleaningStatus;
import com.pms.housekeeping.entity.FrontOfficeStatus;
import com.pms.housekeeping.entity.HousekeepingPriority;
import com.pms.housekeeping.entity.ReservationStatus;
import com.pms.housekeeping.entity.StatusChangeSource;
import com.pms.housekeeping.service.HousekeepingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HousekeepingControllerTest {

    @Mock
    private HousekeepingService housekeepingService;

    private HousekeepingController controller;

    @BeforeEach
    void setUp() {
        controller = new HousekeepingController(housekeepingService);
    }

    @Test
    void dashboard_shouldDelegateToService() {
        String propertyId = UUID.randomUUID().toString();
        LocalDate businessDate = LocalDate.of(2026, 8, 18);
        HousekeepingDashboardResponse expected = new HousekeepingDashboardResponse(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11);
        when(housekeepingService.dashboard(propertyId, businessDate)).thenReturn(expected);

        HousekeepingDashboardResponse response = controller.dashboard(propertyId, businessDate);

        assertThat(response).isSameAs(expected);
        verify(housekeepingService).dashboard(propertyId, businessDate);
    }

    @Test
    void rooms_shouldDelegateToService() {
        HousekeepingRoomFilterRequest request = new HousekeepingRoomFilterRequest(
                UUID.randomUUID().toString(),
                LocalDate.of(2026, 8, 18),
                "suite",
                List.of(CleaningStatus.CLEAN),
                List.of(FrontOfficeStatus.VACANT),
                List.of(ReservationStatus.NOT_RESERVED),
                UUID.randomUUID(),
                "1",
                "Anna",
                HousekeepingPriority.VIP,
                0,
                25,
                "roomNumber",
                "desc"
        );
        HousekeepingRoomsPageResponse expected = new HousekeepingRoomsPageResponse(0, 25, 1, 1, null, List.of());
        when(housekeepingService.rooms(request)).thenReturn(expected);

        HousekeepingRoomsPageResponse response = controller.rooms(request);

        assertThat(response).isSameAs(expected);
        verify(housekeepingService).rooms(request);
    }

    @Test
    void calendar_shouldDelegateToService() {
        String propertyId = UUID.randomUUID().toString();

        LocalDate fromDate = LocalDate.of(2026, 8, 18);
        LocalDate toDate = LocalDate.of(2026, 8, 20);

        List<String> roomTypes = List.of("Deluxe", "Standard");

        HousekeepingCalendarResponse expected =
                new HousekeepingCalendarResponse(
                        propertyId,
                        fromDate,
                        toDate,
                        List.of(),
                        List.of()
                );

        when(
                housekeepingService.calendar(
                        propertyId,
                        fromDate,
                        toDate,
                        roomTypes
                )
        ).thenReturn(expected);

        HousekeepingCalendarResponse response =
                controller.calendar(
                        propertyId,
                        fromDate,
                        toDate,
                        roomTypes
                );

        assertThat(response)
                .isSameAs(expected);

        verify(housekeepingService)
                .calendar(
                        propertyId,
                        fromDate,
                        toDate,
                        roomTypes
                );
    }

    @Test
    void assignableRooms_shouldDelegateToService() {
        String propertyId = UUID.randomUUID().toString();
        UUID roomTypeId = UUID.randomUUID();
        LocalDate businessDate = LocalDate.of(2026, 8, 18);
        List<AssignableRoomResponse> expected = List.of(new AssignableRoomResponse("101", roomTypeId, "Deluxe", "1", "CLASS-A", "NORTH", "CLEAN"));
        when(housekeepingService.assignableRooms(propertyId, businessDate, roomTypeId, 10)).thenReturn(expected);

        List<AssignableRoomResponse> response = controller.assignableRooms(propertyId, roomTypeId, businessDate, 10);

        assertThat(response).isSameAs(expected);
        verify(housekeepingService).assignableRooms(propertyId, businessDate, roomTypeId, 10);
    }

    @Test
    void updateRoomDetails_shouldDelegateToService() {

        List<String> features = List.of(
                "KING_BED",
                "TV",
                "GARDEN_VIEW"
        );

        UpdateHousekeepingRoomDetailsRequest request =
                new UpdateHousekeepingRoomDetailsRequest(
                        UUID.randomUUID().toString(),
                        LocalDate.of(2026, 8, 18),
                        CleaningStatus.CLEAN,
                        FrontOfficeStatus.VACANT,
                        ReservationStatus.NOT_RESERVED,
                        "CONF-1",
                        "Alice",
                        features,
                        HousekeepingPriority.NORMAL,
                        "Guest",
                        LocalDate.of(2026, 8, 18),
                        LocalDate.of(2026, 8, 20),
                        true,
                        "system",
                        StatusChangeSource.HOUSEKEEPING,
                        LocalDateTime.of(2026, 8, 18, 10, 0)
                );

        HousekeepingRoomDetailsUpdateResponse expected =
                new HousekeepingRoomDetailsUpdateResponse(
                        request.propertyId(),
                        request.businessDate(),
                        "101",
                        "CLEAN",
                        "VACANT",
                        "Guest",
                        "NOT_RESERVED",
                        "Alice",
                        features,
                        HousekeepingPriority.NORMAL,
                        "CONF-1",
                        true,
                        LocalDateTime.of(2026, 8, 18, 11, 0),
                        LocalDateTime.of(2026, 8, 18, 10, 0)
                );

        when(housekeepingService.updateRoomDetails("101", request))
                .thenReturn(expected);

        HousekeepingRoomDetailsUpdateResponse response =
                controller.updateRoomDetails("101", request);

        assertThat(response).isSameAs(expected);

        verify(housekeepingService)
                .updateRoomDetails("101", request);
    }
}

