package com.pms.housekeeping.model;

import com.pms.housekeeping.common.exception.BadRequestException;
import com.pms.housekeeping.common.exception.ErrorResponse;
import com.pms.housekeeping.common.exception.HousekeepingException;
import com.pms.housekeeping.common.exception.HousekeepingNotFoundException;
import com.pms.housekeeping.dto.request.HousekeepingRoomFilterRequest;
import com.pms.housekeeping.dto.request.RoomMasterSyncRequest;
import com.pms.housekeeping.dto.request.UpdateHousekeepingStatusRequest;
import com.pms.housekeeping.dto.response.AssignableRoomResponse;
import com.pms.housekeeping.dto.response.CalendarDateResponse;
import com.pms.housekeeping.dto.response.CalendarRoomDayResponse;
import com.pms.housekeeping.dto.response.CalendarRoomResponse;
import com.pms.housekeeping.dto.response.CalendarRoomTypeResponse;
import com.pms.housekeeping.dto.response.HousekeepingCalendarResponse;
import com.pms.housekeeping.dto.response.HousekeepingDashboardResponse;
import com.pms.housekeeping.dto.response.HousekeepingFiltersResponse;
import com.pms.housekeeping.dto.response.HousekeepingRoomRowResponse;
import com.pms.housekeeping.dto.response.HousekeepingRoomsPageResponse;
import com.pms.housekeeping.dto.response.HousekeepingStatusUpdateResponse;
import com.pms.housekeeping.dto.response.RoomMasterSyncResponse;
import com.pms.housekeeping.dto.response.RoomTypeOptionResponse;
import com.pms.housekeeping.entity.CleaningStatus;
import com.pms.housekeeping.entity.FrontOfficeStatus;
import com.pms.housekeeping.entity.HousekeepingPriority;
import com.pms.housekeeping.entity.HousekeepingRoomDayStatus;
import com.pms.housekeeping.entity.HousekeepingRoomDayStatusHistory;
import com.pms.housekeeping.entity.ReservationStatus;
import com.pms.housekeeping.entity.RoomMasterProjection;
import com.pms.housekeeping.entity.StatusChangeSource;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DomainModelSmokeTest {

    @Test
    void recordsEnumsAndEntities_shouldBeInstantiableAndExposeValues() {
        String propertyId = UUID.randomUUID().toString();
        UUID roomTypeId = UUID.randomUUID();
        LocalDate businessDate = LocalDate.of(2026, 8, 18);
        LocalDateTime now = LocalDateTime.of(2026, 8, 18, 10, 0);

        assertThat(CleaningStatus.valueOf("CLEAN")).isEqualTo(CleaningStatus.CLEAN);
        assertThat(FrontOfficeStatus.valueOf("VACANT")).isEqualTo(FrontOfficeStatus.VACANT);
        assertThat(ReservationStatus.valueOf("NOT_RESERVED")).isEqualTo(ReservationStatus.NOT_RESERVED);
        assertThat(HousekeepingPriority.valueOf("NORMAL")).isEqualTo(HousekeepingPriority.NORMAL);
        assertThat(StatusChangeSource.valueOf("SYSTEM")).isEqualTo(StatusChangeSource.SYSTEM);

        HousekeepingRoomDayStatus dayStatus = HousekeepingRoomDayStatus.builder()
                .propertyId(propertyId)
                .businessDate(businessDate)
                .roomNumber("101")
                .roomTypeId(roomTypeId)
                .roomTypeName("Deluxe")
                .floor("1")
                .cleaningStatus(CleaningStatus.CLEAN)
                .frontOfficeStatus(FrontOfficeStatus.VACANT)
                .reservationStatus(ReservationStatus.NOT_RESERVED)
                .priority(HousekeepingPriority.NORMAL)
                .sellable(true)
                .updatedBy("tester")
                .createdAt(now)
                .updatedAt(now)
                .build();

        RoomMasterProjection projection = RoomMasterProjection.builder()
                .propertyId(propertyId)
                .roomNumber("101")
                .roomTypeId(roomTypeId)
                .roomTypeName("Deluxe")
                .floor("1")
                .zone("North")
                .roomClass("CLASS-A")
                .featuresCsv("WiFi")
                .vipCapable(true)
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        HousekeepingRoomDayStatusHistory history = HousekeepingRoomDayStatusHistory.builder()
                .propertyId(propertyId)
                .businessDate(businessDate)
                .roomNumber("101")
                .changedField("cleaningStatus")
                .oldValue("DIRTY")
                .newValue("CLEAN")
                .changedAt(now)
                .changedBy("tester")
                .sourceModule(StatusChangeSource.HOUSEKEEPING)
                .reason("manual")
                .build();

        assertThat(dayStatus.getRoomNumber()).isEqualTo("101");
        assertThat(projection.getRoomClass()).isEqualTo("CLASS-A");
        assertThat(history.getSourceModule()).isEqualTo(StatusChangeSource.HOUSEKEEPING);

        HousekeepingDashboardResponse dashboard = new HousekeepingDashboardResponse(10, 2, 3, 1, 1, 1, 0, 1, 1, 2, 3);
        RoomTypeOptionResponse roomTypeOption = new RoomTypeOptionResponse(roomTypeId, "Deluxe");
        HousekeepingFiltersResponse filters = new HousekeepingFiltersResponse(List.of(roomTypeOption), List.of("1"), List.of("Anna"));
        HousekeepingRoomRowResponse roomRow = new HousekeepingRoomRowResponse(
                "101", roomTypeId, "Deluxe", "1", "CLEAN", "VACANT", "NOT_RESERVED", "Guest",
                businessDate, businessDate.plusDays(1), "Anna", now, HousekeepingPriority.NORMAL, true, null, "WiFi"
        );
        HousekeepingRoomsPageResponse roomsPage = new HousekeepingRoomsPageResponse(0, 50, 1, 1, filters, List.of(roomRow));
        CalendarDateResponse calDate = new CalendarDateResponse(businessDate, "MONDAY", 18);
        CalendarRoomDayResponse calDay = new CalendarRoomDayResponse(businessDate, "CLEAN", "VACANT", "NOT_RESERVED", "Guest", businessDate, businessDate.plusDays(1), "Anna", "NORMAL", true, null);
        CalendarRoomResponse calRoom = new CalendarRoomResponse("101", "1", List.of(calDay));
        CalendarRoomTypeResponse calType = new CalendarRoomTypeResponse(roomTypeId, "Deluxe", List.of(calRoom));
        HousekeepingCalendarResponse calendar = new HousekeepingCalendarResponse(propertyId, businessDate, businessDate.plusDays(1), List.of(calDate), List.of(calType));
        AssignableRoomResponse assignable = new AssignableRoomResponse("101", roomTypeId, "Deluxe", "1", "CLASS-A", "North", "CLEAN");
        HousekeepingStatusUpdateResponse statusUpdate = new HousekeepingStatusUpdateResponse(
                propertyId, businessDate, "101", "CLEAN", "VACANT", "Guest", "NOT_RESERVED", "Anna", HousekeepingPriority.NORMAL, null, true, now, now
        );
        RoomMasterSyncResponse syncResponse = new RoomMasterSyncResponse(5, 1);

        assertThat(dashboard.totalRooms()).isEqualTo(10);
        assertThat(roomsPage.rooms()).hasSize(1);
        assertThat(calendar.roomTypes()).hasSize(1);
        assertThat(assignable.cleaningStatus()).isEqualTo("CLEAN");
        assertThat(statusUpdate.sellable()).isTrue();
        assertThat(syncResponse.syncedRooms()).isEqualTo(5);

        HousekeepingRoomFilterRequest filterRequest = new HousekeepingRoomFilterRequest(
                propertyId, businessDate, "101", List.of(CleaningStatus.CLEAN), List.of(FrontOfficeStatus.VACANT),
                List.of(ReservationStatus.NOT_RESERVED), roomTypeId, "1", "Anna", HousekeepingPriority.NORMAL, 0, 50, "roomNumber", "asc"
        );
        RoomMasterSyncRequest syncRequest = new RoomMasterSyncRequest(
                propertyId,
                businessDate,
                businessDate.plusDays(1),
                List.of(new RoomMasterSyncRequest.RoomMasterUnit(roomTypeId, "Deluxe", "101", "1", "North", "CLASS-A", "WiFi", true, true))
        );
        UpdateHousekeepingStatusRequest updateRequest = new UpdateHousekeepingStatusRequest(
                propertyId, businessDate, CleaningStatus.CLEAN, FrontOfficeStatus.VACANT, ReservationStatus.NOT_RESERVED,
                null, "Anna", HousekeepingPriority.NORMAL, "Guest", businessDate, businessDate.plusDays(1), true, "tester", StatusChangeSource.SYSTEM, now
        );

        assertThat(filterRequest.propertyId()).isEqualTo(propertyId);
        assertThat(syncRequest.rooms()).hasSize(1);
        assertThat(updateRequest.sourceModule()).isEqualTo(StatusChangeSource.SYSTEM);

        ErrorResponse error = new ErrorResponse(Instant.now(), 400, "BAD_REQUEST", "invalid", "/api/test");
        assertThat(error.status()).isEqualTo(400);

        HousekeepingException housekeepingException = new HousekeepingException("conflict", new IllegalStateException("cause"));
        HousekeepingNotFoundException notFoundException = new HousekeepingNotFoundException("missing");
        BadRequestException badRequestException = new BadRequestException("bad");

        assertThat(housekeepingException.getMessage()).isEqualTo("conflict");
        assertThat(housekeepingException.getCause()).isNotNull();
        assertThat(notFoundException.getMessage()).isEqualTo("missing");
        assertThat(badRequestException.getMessage()).isEqualTo("bad");
    }
}

