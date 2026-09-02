package com.pms.housekeeping.service.impl;

import com.pms.housekeeping.common.exception.HousekeepingNotFoundException;
import com.pms.housekeeping.dto.request.HousekeepingRoomFilterRequest;
import com.pms.housekeeping.dto.request.UpdateHousekeepingStatusRequest;
import com.pms.housekeeping.dto.response.AssignableRoomResponse;
import com.pms.housekeeping.dto.response.CalendarDateResponse;
import com.pms.housekeeping.dto.response.CalendarRoomResponse;
import com.pms.housekeeping.dto.response.CalendarRoomTypeResponse;
import com.pms.housekeeping.dto.response.HousekeepingCalendarResponse;
import com.pms.housekeeping.dto.response.HousekeepingDashboardResponse;
import com.pms.housekeeping.dto.response.HousekeepingRoomRowResponse;
import com.pms.housekeeping.dto.response.HousekeepingRoomsPageResponse;
import com.pms.housekeeping.dto.response.HousekeepingStatusUpdateResponse;
import com.pms.housekeeping.dto.response.RoomTypeOptionResponse;
import com.pms.housekeeping.entity.CleaningStatus;
import com.pms.housekeeping.entity.FrontOfficeStatus;
import com.pms.housekeeping.entity.HousekeepingPriority;
import com.pms.housekeeping.entity.HousekeepingRoomDayStatus;
import com.pms.housekeeping.entity.HousekeepingRoomDayStatusHistory;
import com.pms.housekeeping.entity.ReservationStatus;
import com.pms.housekeeping.entity.RoomMasterProjection;
import com.pms.housekeeping.entity.StatusChangeSource;
import com.pms.housekeeping.repository.HousekeepingRoomDayStatusHistoryRepository;
import com.pms.housekeeping.repository.HousekeepingRoomDayStatusRepository;
import com.pms.housekeeping.repository.RoomMasterProjectionRepository;
import com.pms.common.security.CurrentUserProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HousekeepingServiceImplTest {

    @Mock
    private HousekeepingRoomDayStatusRepository dayStatusRepository;

    @Mock
    private HousekeepingRoomDayStatusHistoryRepository historyRepository;

    @Mock
    private RoomMasterProjectionRepository roomMasterProjectionRepository;

    @Mock
    private HousekeepingRoomDayStatusRepository housekeepingRoomDayStatusRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;

    @Captor
    private ArgumentCaptor<HousekeepingRoomDayStatusHistory> historyCaptor;

    private HousekeepingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new HousekeepingServiceImpl(
                dayStatusRepository,
                historyRepository,
                roomMasterProjectionRepository,
                housekeepingRoomDayStatusRepository,
                currentUserProvider
        );
    }

    @Test
    void dashboard_shouldCalculateAllCounters() {
        String propertyId = UUID.randomUUID().toString();
        LocalDate businessDate = LocalDate.of(2026, 8, 18);
        List<HousekeepingRoomDayStatus> rows = List.of(
                status(propertyId, businessDate, "101", CleaningStatus.CLEAN, FrontOfficeStatus.VACANT, ReservationStatus.ARRIVAL),
                status(propertyId, businessDate, "102", CleaningStatus.DIRTY, FrontOfficeStatus.VACANT, ReservationStatus.DEPARTURE),
                status(propertyId, businessDate, "103", CleaningStatus.CLEAN, FrontOfficeStatus.OCCUPIED, ReservationStatus.IN_HOUSE),
                status(propertyId, businessDate, "104", CleaningStatus.DIRTY, FrontOfficeStatus.OCCUPIED, ReservationStatus.NOT_RESERVED),
                status(propertyId, businessDate, "105", CleaningStatus.OUT_OF_ORDER, FrontOfficeStatus.VACANT, ReservationStatus.NOT_RESERVED),
                status(propertyId, businessDate, "106", CleaningStatus.OUT_OF_SERVICE, FrontOfficeStatus.VACANT, ReservationStatus.NOT_RESERVED),
                status(propertyId, businessDate, "107", CleaningStatus.INSPECTED, FrontOfficeStatus.VACANT, ReservationStatus.NOT_RESERVED),
                status(propertyId, businessDate, "108", CleaningStatus.PICKUP, FrontOfficeStatus.VACANT, ReservationStatus.NOT_RESERVED)
        );
        when(dayStatusRepository.findAllByPropertyIdAndBusinessDate(propertyId, businessDate)).thenReturn(rows);

        HousekeepingDashboardResponse response = service.dashboard(propertyId, businessDate);

        assertThat(response.totalRooms()).isEqualTo(8);
        assertThat(response.vacantClean()).isEqualTo(1);
        assertThat(response.vacantDirty()).isEqualTo(1);
        assertThat(response.occupiedClean()).isEqualTo(1);
        assertThat(response.occupiedDirty()).isEqualTo(1);
        assertThat(response.outOfOrder()).isEqualTo(1);
        assertThat(response.outOfService()).isEqualTo(1);
        assertThat(response.inspected()).isEqualTo(1);
        assertThat(response.pickup()).isEqualTo(1);
        assertThat(response.arrivals()).isEqualTo(1);
        assertThat(response.departures()).isEqualTo(1);
    }

    @Test
    void rooms_shouldApplyDefaultPagingSortingAndFilters() {
        String propertyId = UUID.randomUUID().toString();
        LocalDate businessDate = LocalDate.of(2026, 8, 18);
        HousekeepingRoomDayStatus row = status(propertyId, businessDate, "201", CleaningStatus.CLEAN, FrontOfficeStatus.VACANT, ReservationStatus.NOT_RESERVED);
        row.setRoomTypeId("13");
        row.setRoomTypeName("Deluxe");
        row.setFloor("2");
        row.setGuestDisplayName("Jane Doe");
        row.setArrivalDate(LocalDate.of(2026, 8, 20));
        row.setDepartureDate(LocalDate.of(2026, 8, 22));
        row.setAttendantName("Alice");
        row.setLastCleanedAt(LocalDateTime.of(2026, 8, 18, 10, 0));
        row.setPriority(HousekeepingPriority.HIGH);
        row.setSellable(true);
        row.setConfirmationId("CONF-1");
        row.setFeaturesCsv("WiFi,TV");

        Page<HousekeepingRoomDayStatus> roomPage = new PageImpl<>(List.of(row), PageRequest.of(0, 50, Sort.by(Sort.Direction.ASC, "roomNumber")), 1);
        when(dayStatusRepository.findAll(org.mockito.ArgumentMatchers.<Specification<HousekeepingRoomDayStatus>>any(), pageableCaptor.capture())).thenReturn(roomPage);
        when(dayStatusRepository.findDistinctRoomTypes(propertyId, businessDate)).thenReturn(List.of(new RoomTypeOptionResponse("13", "Deluxe")));
        when(dayStatusRepository.findDistinctFloors(propertyId, businessDate)).thenReturn(List.of("2"));
        when(dayStatusRepository.findDistinctAttendants(propertyId, businessDate)).thenReturn(List.of("Alice"));

        HousekeepingRoomFilterRequest request = new HousekeepingRoomFilterRequest(
                propertyId,
                businessDate,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                50,
                null,
                null
        );

        HousekeepingRoomsPageResponse response = service.rooms(request);

        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);
        Sort.Order roomOrder = pageableCaptor.getValue().getSort().getOrderFor("roomNumber");
        assertThat(roomOrder).isNotNull();
        assertThat(roomOrder.getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(response.page()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(50);
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(1);
        assertThat(response.filterOptions().roomTypes()).hasSize(1);
        assertThat(response.rooms()).hasSize(1);

        HousekeepingRoomRowResponse room = response.rooms().getFirst();
        assertThat(room.roomNumber()).isEqualTo("201");
        assertThat(room.roomTypeName()).isEqualTo("Deluxe");
        assertThat(room.floor()).isEqualTo("2");
        assertThat(room.cleaningStatus()).isEqualTo("CLEAN");
        assertThat(room.frontOfficeStatus()).isEqualTo("VACANT");
        assertThat(room.reservationStatus()).isEqualTo("NOT_RESERVED");
        assertThat(room.guestDisplayName()).isEqualTo("Jane Doe");
        assertThat(room.attendantName()).isEqualTo("Alice");
        assertThat(room.lastCleanedAt()).isEqualTo(LocalDateTime.of(2026, 8, 18, 10, 0));
        assertThat(room.priority()).isEqualTo(HousekeepingPriority.HIGH);
        assertThat(room.sellable()).isTrue();
        assertThat(room.confirmationId()).isEqualTo("CONF-1");
        assertThat(room.featuresCsv()).isEqualTo("WiFi,TV");
    }

    @Test
    void rooms_shouldMapSortFieldsCorrectly() {
        Map<String, String> mappings = new HashMap<>();
        mappings.put("roomNumber", "roomNumber");
        mappings.put("arrivalDate", "arrivalDate");
        mappings.put("departureDate", "departureDate");
        mappings.put("guestName", "guestDisplayName");
        mappings.put("cleaningStatus", "cleaningStatus");
        mappings.put("frontOfficeStatus", "frontOfficeStatus");
        mappings.put("reservationStatus", "reservationStatus");
        mappings.put("attendant", "attendantName");
        mappings.put("priority", "priority");
        mappings.put("unknown", "roomNumber");

        mappings.forEach((sortBy, expectedField) -> {
            Sort sort = invokeBuildSort(sortBy, "desc");
            Sort.Order order = sort.getOrderFor(expectedField);
            assertThat(order).isNotNull();
            assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
        });
    }

    private Sort invokeBuildSort(String sortBy, String sortDirection) {
        try {
            Method method = HousekeepingServiceImpl.class.getDeclaredMethod("buildSort", String.class, String.class);
            method.setAccessible(true);
            return (Sort) method.invoke(service, sortBy, sortDirection);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Test
    void calendar_shouldRejectInvalidDateRange() {
        assertThatThrownBy(() -> service.calendar(
                UUID.randomUUID().toString(),
                LocalDate.of(2026, 8, 20),
                LocalDate.of(2026, 8, 18),
                null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fromDate must be before or equal to toDate");
    }

    @Test
    void calendar_shouldBuildNestedDateAndRoomStructure() {
        String propertyId = UUID.randomUUID().toString();
        String roomTypeId = "13";

        LocalDate fromDate = LocalDate.of(2026, 8, 18);
        LocalDate toDate = LocalDate.of(2026, 8, 20);

        List<String> roomTypes = List.of("Deluxe");

        HousekeepingRoomDayStatus room101Day1 =
                status(
                        propertyId,
                        fromDate,
                        "101",
                        CleaningStatus.CLEAN,
                        FrontOfficeStatus.VACANT,
                        ReservationStatus.ARRIVAL
                );

        room101Day1.setRoomTypeId(roomTypeId);
        room101Day1.setRoomTypeName("Deluxe");
        room101Day1.setFloor("1");
        room101Day1.setGuestDisplayName("Guest One");
        room101Day1.setArrivalDate(
                LocalDate.of(2026, 8, 18)
        );
        room101Day1.setDepartureDate(
                LocalDate.of(2026, 8, 20)
        );
        room101Day1.setAttendantName("Anna");
        room101Day1.setPriority(
                HousekeepingPriority.NORMAL
        );
        room101Day1.setSellable(true);
        room101Day1.setConfirmationId(
                UUID.randomUUID().toString()
        );

        HousekeepingRoomDayStatus room101Day3 =
                status(
                        propertyId,
                        toDate,
                        "101",
                        null,
                        FrontOfficeStatus.OCCUPIED,
                        ReservationStatus.DEPARTURE
                );

        room101Day3.setRoomTypeId(roomTypeId);
        room101Day3.setRoomTypeName("Deluxe");
        room101Day3.setFloor("1");
        room101Day3.setGuestDisplayName("Guest One");
        room101Day3.setArrivalDate(
                LocalDate.of(2026, 8, 18)
        );
        room101Day3.setDepartureDate(
                LocalDate.of(2026, 8, 20)
        );
        room101Day3.setAttendantName("Anna");
        room101Day3.setPriority(
                HousekeepingPriority.HIGH
        );
        room101Day3.setSellable(false);
        room101Day3.setConfirmationId(null);

        HousekeepingRoomDayStatus room102Day2 =
                status(
                        propertyId,
                        fromDate.plusDays(1),
                        "102",
                        CleaningStatus.DIRTY,
                        FrontOfficeStatus.VACANT,
                        ReservationStatus.NOT_RESERVED
                );

        room102Day2.setRoomTypeId(roomTypeId);
        room102Day2.setRoomTypeName("Deluxe");
        room102Day2.setFloor("1");
        room102Day2.setGuestDisplayName(null);
        room102Day2.setAttendantName(null);
        room102Day2.setPriority(
                HousekeepingPriority.NORMAL
        );
        room102Day2.setSellable(false);

        when(
                housekeepingRoomDayStatusRepository.findCalendarData(
                        propertyId,
                        fromDate,
                        toDate,
                        roomTypes
                )
        ).thenReturn(
                List.of(
                        room101Day1,
                        room101Day3,
                        room102Day2
                )
        );

        HousekeepingCalendarResponse response =
                service.calendar(
                        propertyId,
                        fromDate,
                        toDate,
                        roomTypes
                );

        assertThat(response.propertyId())
                .isEqualTo(propertyId);

        assertThat(response.fromDate())
                .isEqualTo(fromDate);

        assertThat(response.toDate())
                .isEqualTo(toDate);

        assertThat(response.dates())
                .hasSize(3);

        assertThat(response.dates().getFirst())
                .isEqualTo(
                        new CalendarDateResponse(
                                fromDate,
                                fromDate.getDayOfWeek().name(),
                                fromDate.getDayOfMonth()
                        )
                );

        assertThat(response.roomTypes())
                .hasSize(1);

        CalendarRoomTypeResponse roomType =
                response.roomTypes().getFirst();

        assertThat(roomType.roomTypeId())
                .isEqualTo(roomTypeId);

        assertThat(roomType.roomTypeName())
                .isEqualTo("Deluxe");

        assertThat(roomType.rooms())
                .hasSize(2);

        CalendarRoomResponse room101 =
                roomType.rooms().getFirst();

        assertThat(room101.roomNumber())
                .isEqualTo("101");

        assertThat(room101.floor())
                .isEqualTo("1");

        assertThat(room101.days())
                .hasSize(3);

        assertThat(room101.days().get(0).cleaningStatus())
                .isEqualTo("CLEAN");

        assertThat(room101.days().get(1).cleaningStatus())
                .isNull();

        assertThat(room101.days().get(2).frontOfficeStatus())
                .isEqualTo("OCCUPIED");

        assertThat(room101.days().get(2).priority())
                .isEqualTo("HIGH");

        assertThat(room101.days().get(2).assignedReservationId())
                .isNull();

        CalendarRoomResponse room102 =
                roomType.rooms().get(1);

        assertThat(room102.roomNumber())
                .isEqualTo("102");

        assertThat(room102.days().get(0).cleaningStatus())
                .isNull();

        assertThat(room102.days().get(1).cleaningStatus())
                .isEqualTo("DIRTY");

        assertThat(room102.days().get(2).sellable())
                .isNull();
    }

//    @Test
//    void calendar_shouldBuildNestedDateAndRoomStructure() {
//        String propertyId = UUID.randomUUID().toString();
//        UUID roomTypeId = UUID.randomUUID();
//        LocalDate fromDate = LocalDate.of(2026, 8, 18);
//        LocalDate toDate = LocalDate.of(2026, 8, 20);
//
//        HousekeepingRoomDayStatus room101Day1 = status(propertyId, fromDate, "101", CleaningStatus.CLEAN, FrontOfficeStatus.VACANT, ReservationStatus.ARRIVAL);
//        room101Day1.setRoomTypeId(roomTypeId);
//        room101Day1.setRoomTypeName("Deluxe");
//        room101Day1.setFloor("1");
//        room101Day1.setGuestDisplayName("Guest One");
//        room101Day1.setArrivalDate(LocalDate.of(2026, 8, 18));
//        room101Day1.setDepartureDate(LocalDate.of(2026, 8, 20));
//        room101Day1.setAttendantName("Anna");
//        room101Day1.setPriority(HousekeepingPriority.NORMAL);
//        room101Day1.setSellable(true);
//        room101Day1.setConfirmationId(UUID.randomUUID().toString());
//
//        HousekeepingRoomDayStatus room101Day3 = status(propertyId, toDate, "101", null, FrontOfficeStatus.OCCUPIED, ReservationStatus.DEPARTURE);
//        room101Day3.setRoomTypeId(roomTypeId);
//        room101Day3.setRoomTypeName("Deluxe");
//        room101Day3.setFloor("1");
//        room101Day3.setGuestDisplayName("Guest One");
//        room101Day3.setArrivalDate(LocalDate.of(2026, 8, 18));
//        room101Day3.setDepartureDate(LocalDate.of(2026, 8, 20));
//        room101Day3.setAttendantName("Anna");
//        room101Day3.setPriority(HousekeepingPriority.HIGH);
//        room101Day3.setSellable(false);
//        room101Day3.setConfirmationId(null);
//
//        HousekeepingRoomDayStatus room102Day2 = status(propertyId, fromDate.plusDays(1), "102", CleaningStatus.DIRTY, FrontOfficeStatus.VACANT, ReservationStatus.NOT_RESERVED);
//        room102Day2.setRoomTypeId(roomTypeId);
//        room102Day2.setRoomTypeName("Deluxe");
//        room102Day2.setFloor("1");
//        room102Day2.setGuestDisplayName(null);
//        room102Day2.setAttendantName(null);
//        room102Day2.setPriority(HousekeepingPriority.NORMAL);
//        room102Day2.setSellable(false);
//
//        when(housekeepingRoomDayStatusRepository.findCalendarData(propertyId, fromDate, toDate, "Deluxe"))
//                .thenReturn(List.of(room101Day1, room101Day3, room102Day2));
//
//        HousekeepingCalendarResponse response = service.calendar(propertyId, fromDate, toDate, roomTypeId);
//
//        assertThat(response.propertyId()).isEqualTo(propertyId);
//        assertThat(response.fromDate()).isEqualTo(fromDate);
//        assertThat(response.toDate()).isEqualTo(toDate);
//        assertThat(response.dates()).hasSize(3);
//        assertThat(response.dates().getFirst()).isEqualTo(new CalendarDateResponse(fromDate, fromDate.getDayOfWeek().name(), fromDate.getDayOfMonth()));
//        assertThat(response.roomTypes()).hasSize(1);
//
//        CalendarRoomTypeResponse roomType = response.roomTypes().getFirst();
//        assertThat(roomType.roomTypeId()).isEqualTo(roomTypeId);
//        assertThat(roomType.roomTypeName()).isEqualTo("Deluxe");
//        assertThat(roomType.rooms()).hasSize(2);
//
//        CalendarRoomResponse room101 = roomType.rooms().getFirst();
//        assertThat(room101.roomNumber()).isEqualTo("101");
//        assertThat(room101.floor()).isEqualTo("1");
//        assertThat(room101.days()).hasSize(3);
//        assertThat(room101.days().get(0).cleaningStatus()).isEqualTo("CLEAN");
//        assertThat(room101.days().get(1).cleaningStatus()).isNull();
//        assertThat(room101.days().get(2).frontOfficeStatus()).isEqualTo("OCCUPIED");
//        assertThat(room101.days().get(2).priority()).isEqualTo("HIGH");
//        assertThat(room101.days().get(2).assignedReservationId()).isNull();
//
//        CalendarRoomResponse room102 = roomType.rooms().get(1);
//        assertThat(room102.roomNumber()).isEqualTo("102");
//        assertThat(room102.days().get(0).cleaningStatus()).isNull();
//        assertThat(room102.days().get(1).cleaningStatus()).isEqualTo("DIRTY");
//        assertThat(room102.days().get(2).sellable()).isNull();
//    }

    @Test
    void assignableRooms_shouldClampLimitAndEnrichResults() {
        String propertyId = UUID.randomUUID().toString();
        String roomTypeId = "13";
        LocalDate businessDate = LocalDate.of(2026, 8, 18);

        List<HousekeepingRoomDayStatus> rows = new ArrayList<>();
        for (int i = 1; i <= 201; i++) {
            HousekeepingRoomDayStatus row = status(propertyId, businessDate, String.format("%03d", i), CleaningStatus.CLEAN, FrontOfficeStatus.VACANT, ReservationStatus.NOT_RESERVED);
            row.setRoomTypeId(roomTypeId);
            row.setSellable(true);
            rows.add(row);
        }
        when(dayStatusRepository.findTop200ByPropertyIdAndBusinessDateAndRoomTypeIdAndSellableTrueAndConfirmationIdIsNullAndCleaningStatusInAndFrontOfficeStatusOrderByRoomNumberAsc(
                eq(propertyId),
                eq(businessDate),
                eq(roomTypeId),
                any(),
                eq(FrontOfficeStatus.VACANT)
        )).thenReturn(rows);

        RoomMasterProjection projection1 = RoomMasterProjection.builder()
                .propertyId(propertyId)
                .roomNumber("001")
                .roomTypeId(roomTypeId)
                .roomTypeName("Deluxe")
                .floor("1")
                .roomClass("CLASS-A")
                .zone("NORTH")
                .featuresCsv("WiFi")
                .vipCapable(true)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        RoomMasterProjection projection2 = RoomMasterProjection.builder()
                .propertyId(propertyId)
                .roomNumber("002")
                .roomTypeId(roomTypeId)
                .roomTypeName("Deluxe")
                .floor("2")
                .roomClass("CLASS-B")
                .zone("SOUTH")
                .featuresCsv("TV")
                .vipCapable(false)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(roomMasterProjectionRepository.findAllByPropertyId(propertyId)).thenReturn(List.of(projection1, projection2));

        List<AssignableRoomResponse> response = service.assignableRooms(propertyId, businessDate, roomTypeId, 500);

        assertThat(response).hasSize(200);
        assertThat(response.get(0)).isEqualTo(new AssignableRoomResponse("001", roomTypeId, "Deluxe", "1", "CLASS-A", "NORTH", "CLEAN"));
        assertThat(response.get(1)).isEqualTo(new AssignableRoomResponse("002", roomTypeId, "Deluxe", "2", "CLASS-B", "SOUTH", "CLEAN"));
        assertThat(response.get(199).roomNumber()).isEqualTo("200");
    }

    @Test
    void assignableRooms_shouldReturnAtLeastOneRoomEvenWhenLimitIsZero() {
        String propertyId = UUID.randomUUID().toString();
        String roomTypeId = "14";
        LocalDate businessDate = LocalDate.of(2026, 8, 18);
        HousekeepingRoomDayStatus row = status(propertyId, businessDate, "301", CleaningStatus.INSPECTED, FrontOfficeStatus.VACANT, ReservationStatus.NOT_RESERVED);
        row.setRoomTypeId(roomTypeId);
        row.setSellable(true);

        when(dayStatusRepository.findTop200ByPropertyIdAndBusinessDateAndRoomTypeIdAndSellableTrueAndConfirmationIdIsNullAndCleaningStatusInAndFrontOfficeStatusOrderByRoomNumberAsc(
                eq(propertyId),
                eq(businessDate),
                eq(roomTypeId),
                any(),
                eq(FrontOfficeStatus.VACANT)
        )).thenReturn(List.of(row));
        when(roomMasterProjectionRepository.findAllByPropertyId(propertyId)).thenReturn(List.of());

        List<AssignableRoomResponse> response = service.assignableRooms(propertyId, businessDate, roomTypeId, 0);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().roomNumber()).isEqualTo("301");
        assertThat(response.getFirst().roomTypeName()).isNull();
    }

    @Test
    void updateRoomStatus_shouldThrowWhenRoomIsMissing() {
        String propertyId = UUID.randomUUID().toString();
        LocalDate businessDate = LocalDate.of(2026, 8, 18);
        UpdateHousekeepingStatusRequest request = new UpdateHousekeepingStatusRequest(
                propertyId,
                businessDate,
                CleaningStatus.CLEAN,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                StatusChangeSource.SYSTEM,
                null
        );

        when(currentUserProvider.getCurrentUsername()).thenReturn("alice");
        when(dayStatusRepository.findByPropertyIdAndBusinessDateAndRoomNumber(propertyId, businessDate, "404")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateRoomStatus("404", request))
                .isInstanceOf(HousekeepingNotFoundException.class)
                .hasMessageContaining("Housekeeping status not found for room: 404");
        verifyNoInteractions(historyRepository);
    }

    @Test
    void updateRoomStatus_shouldApplyChangesSaveHistoryAndComputeSellable() {
        String propertyId = UUID.randomUUID().toString();
        LocalDate businessDate = LocalDate.of(2026, 8, 18);
        HousekeepingRoomDayStatus row = status(propertyId, businessDate, "501", CleaningStatus.DIRTY, FrontOfficeStatus.VACANT, ReservationStatus.NOT_RESERVED);
        row.setAttendantName("Old Attendant");
        row.setPriority(HousekeepingPriority.NORMAL);
        row.setGuestDisplayName("Old Guest");
        row.setArrivalDate(LocalDate.of(2026, 8, 17));
        row.setDepartureDate(LocalDate.of(2026, 8, 19));
        row.setConfirmationId(null);
        row.setSellable(false);

        when(dayStatusRepository.findByPropertyIdAndBusinessDateAndRoomNumber(propertyId, businessDate, "501")).thenReturn(Optional.of(row));
        when(dayStatusRepository.save(any(HousekeepingRoomDayStatus.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(currentUserProvider.getCurrentUsername()).thenReturn("11111111-1111-1111-1111-111111111111");

        SecurityContextHolder.setContext(
                new SecurityContextImpl(new UsernamePasswordAuthenticationToken("11111111-1111-1111-1111-111111111111", null, List.of())));

        UpdateHousekeepingStatusRequest request = new UpdateHousekeepingStatusRequest(
                propertyId,
                businessDate,
                CleaningStatus.CLEAN,
                FrontOfficeStatus.VACANT,
                ReservationStatus.NOT_RESERVED,
                "CONF-123",
                "New Attendant",
                HousekeepingPriority.HIGH,
                "New Guest",
                LocalDate.of(2026, 8, 18),
                LocalDate.of(2026, 8, 20),
                null,
                null,
                StatusChangeSource.HOUSEKEEPING,
                null
        );

        HousekeepingStatusUpdateResponse response = service.updateRoomStatus("501", request);

        assertThat(response.roomNumber()).isEqualTo("501");
        assertThat(response.cleaningStatus()).isEqualTo("CLEAN");
        assertThat(response.frontOfficeStatus()).isEqualTo("VACANT");
        assertThat(response.reservationStatus()).isEqualTo("NOT_RESERVED");
        assertThat(response.attendantName()).isEqualTo("New Attendant");
        assertThat(response.priority()).isEqualTo(HousekeepingPriority.HIGH);
        assertThat(response.confirmationId()).isEqualTo("CONF-123");
        assertThat(response.sellable()).isTrue();
        assertThat(response.updatedAt()).isNotNull();
        assertThat(response.lastCleanedAt()).isNotNull();

        ArgumentCaptor<HousekeepingRoomDayStatus> savedCaptor = ArgumentCaptor.forClass(HousekeepingRoomDayStatus.class);
        verify(dayStatusRepository).save(savedCaptor.capture());
        HousekeepingRoomDayStatus saved = savedCaptor.getValue();
        assertThat(saved.getUpdatedBy()).isEqualTo(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        assertThat(saved.getCleaningStatus()).isEqualTo(CleaningStatus.CLEAN);
        assertThat(saved.getFrontOfficeStatus()).isEqualTo(FrontOfficeStatus.VACANT);
        assertThat(saved.getReservationStatus()).isEqualTo(ReservationStatus.NOT_RESERVED);
        assertThat(saved.getAttendantName()).isEqualTo("New Attendant");
        assertThat(saved.getPriority()).isEqualTo(HousekeepingPriority.HIGH);
        assertThat(saved.getConfirmationId()).isEqualTo("CONF-123");
        assertThat(saved.getGuestDisplayName()).isEqualTo("New Guest");
        assertThat(saved.getArrivalDate()).isEqualTo(LocalDate.of(2026, 8, 18));
        assertThat(saved.getDepartureDate()).isEqualTo(LocalDate.of(2026, 8, 20));
        assertThat(saved.isSellable()).isTrue();
        assertThat(saved.getLastCleanedAt()).isNotNull();

        verify(historyRepository, org.mockito.Mockito.times(4)).save(historyCaptor.capture());
        List<HousekeepingRoomDayStatusHistory> histories = historyCaptor.getAllValues();
        assertThat(histories).extracting(HousekeepingRoomDayStatusHistory::getChangedField)
                .containsExactly(
                        "cleaningStatus",
                        "assignedReservationId",
                        "attendantName",
                        "priority"
                );
        assertThat(histories).allMatch(h -> h.getChangedBy().equals("11111111-1111-1111-1111-111111111111") && h.getSourceModule() == StatusChangeSource.HOUSEKEEPING);
    }

    @Test
    void updateRoomStatus_shouldComputeSellableAndSkipNoOpChanges() {
        String propertyId = UUID.randomUUID().toString();
        LocalDate businessDate = LocalDate.of(2026, 8, 18);
        HousekeepingRoomDayStatus row = status(propertyId, businessDate, "601", CleaningStatus.CLEAN, FrontOfficeStatus.VACANT, ReservationStatus.NOT_RESERVED);
        row.setAttendantName("Alice");
        row.setPriority(HousekeepingPriority.NORMAL);
        row.setConfirmationId("CONF-EXISTING");
        row.setSellable(true);

        when(dayStatusRepository.findByPropertyIdAndBusinessDateAndRoomNumber(propertyId, businessDate, "601")).thenReturn(Optional.of(row));
        when(dayStatusRepository.save(any(HousekeepingRoomDayStatus.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SecurityContextHolder.setContext(
                new SecurityContextImpl(new UsernamePasswordAuthenticationToken("11111111-1111-1111-1111-111111111111", null, List.of())));

        UpdateHousekeepingStatusRequest request = new UpdateHousekeepingStatusRequest(
                propertyId,
                businessDate,
                CleaningStatus.CLEAN,
                FrontOfficeStatus.VACANT,
                ReservationStatus.NOT_RESERVED,
                "CONF-EXISTING",
                "Alice",
                HousekeepingPriority.NORMAL,
                null,
                null,
                null,
                false,
                null,
                StatusChangeSource.RESERVATION,
                null
        );

        HousekeepingStatusUpdateResponse response = service.updateRoomStatus("601", request);

        assertThat(response.sellable()).isTrue();
        assertThat(response.cleaningStatus()).isEqualTo("CLEAN");
        assertThat(response.confirmationId()).isEqualTo("CONF-EXISTING");
        verify(historyRepository, never()).save(any());
    }

    @Test
    void updateRoomStatus_shouldComputeSellableWhenEligibleAndSellableIsNotProvided() {
        String propertyId = UUID.randomUUID().toString();
        LocalDate businessDate = LocalDate.of(2026, 8, 18);
        HousekeepingRoomDayStatus row = status(propertyId, businessDate, "602", CleaningStatus.CLEAN, FrontOfficeStatus.VACANT, ReservationStatus.NOT_RESERVED);
        row.setConfirmationId(null);
        row.setPriority(HousekeepingPriority.NORMAL);
        row.setSellable(false);

        when(dayStatusRepository.findByPropertyIdAndBusinessDateAndRoomNumber(propertyId, businessDate, "602")).thenReturn(Optional.of(row));
        when(dayStatusRepository.save(any(HousekeepingRoomDayStatus.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SecurityContextHolder.setContext(
                new SecurityContextImpl(new UsernamePasswordAuthenticationToken("11111111-1111-1111-1111-111111111111", null, List.of())));

        UpdateHousekeepingStatusRequest request = new UpdateHousekeepingStatusRequest(
                propertyId,
                businessDate,
                CleaningStatus.CLEAN,
                FrontOfficeStatus.VACANT,
                ReservationStatus.NOT_RESERVED,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                StatusChangeSource.SYSTEM,
                null
        );

        HousekeepingStatusUpdateResponse response = service.updateRoomStatus("602", request);

        assertThat(response.sellable()).isTrue();
        assertThat(response.cleaningStatus()).isEqualTo("CLEAN");
        assertThat(response.confirmationId()).isNull();
        verify(historyRepository, never()).save(any());
    }

    private static HousekeepingRoomDayStatus status(
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
                .roomTypeId("13")
                .roomTypeName("Standard")
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









