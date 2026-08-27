package com.pms.reservation.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.pms.guestlisting.exception.BadRequestException;
import com.pms.housekeeping.entity.HousekeepingRoomStatusRecord;
import com.pms.housekeeping.repository.HousekeepingRoomStatusRepository;
import com.pms.reservation.config.PropertyWizardServiceProperties;
import com.pms.reservation.dto.ReservationRoomCalendarResponseDto;
import com.pms.reservation.entity.ReservationBookingRecord;
import com.pms.reservation.integration.PropertyInventoryPort;
import com.pms.reservation.integration.HousekeepingRoomCalendarClient;
import com.pms.reservation.integration.dto.PropertyRoomInventoryDto;
import com.pms.reservation.repository.ReservationBookingRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReservationRoomCalendarServiceImplTest {

    @Mock
    private PropertyWizardServiceProperties propertyWizardServiceProperties;

    @Mock
    private PropertyInventoryPort propertyInventoryPort;

    @Mock
    private HousekeepingRoomCalendarClient housekeepingRoomCalendarClient;

    @Mock
    private ReservationBookingRepository reservationBookingRepository;

    @Mock
    private HousekeepingRoomStatusRepository housekeepingRoomStatusRepository;

    @InjectMocks
    private ReservationRoomCalendarServiceImpl reservationRoomCalendarService;

    @Test
    void getRoomCalendarShouldBuildRoomWiseCalendarForAssignment() {
        when(propertyWizardServiceProperties.isEnabled()).thenReturn(true);

        PropertyRoomInventoryDto room101 = new PropertyRoomInventoryDto();
        room101.setRoomNumber("101");
        room101.setRoomType("King");

        PropertyRoomInventoryDto room102 = new PropertyRoomInventoryDto();
        room102.setRoomNumber("102");
        room102.setRoomType("King");

        when(housekeepingRoomCalendarClient.fetchRooms(
                "7cfd4559-b6f3-4b7d-b933-e93018ac1d47",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 3)
        )).thenReturn(List.of(room101, room102));

        ReservationBookingRecord booking101 = ReservationBookingRecord.builder()
                .id(901L)
                .propertyId("7cfd4559-b6f3-4b7d-b933-e93018ac1d47")
                .confirmationNumber("C-101")
                .reservationStatus("CONFIRMED")
                .assignedRoomNo("101")
                .roomType("King")
                .arrivalDate(LocalDate.of(2026, 8, 1))
                .departureDate(LocalDate.of(2026, 8, 3))
                .build();

        ReservationBookingRecord booking102 = ReservationBookingRecord.builder()
                .id(902L)
                .propertyId("7cfd4559-b6f3-4b7d-b933-e93018ac1d47")
                .confirmationNumber("C-102")
                .reservationStatus("CHECKED_IN")
                .assignedRoomNo("102")
                .roomType("King")
                .arrivalDate(LocalDate.of(2026, 8, 1))
                .departureDate(LocalDate.of(2026, 8, 4))
                .build();

        when(reservationBookingRepository.findByPropertyIdAndAssignedRoomNoIsNotNullAndArrivalDateLessThanAndDepartureDateGreaterThan(
                "7cfd4559-b6f3-4b7d-b933-e93018ac1d47",
                LocalDate.of(2026, 8, 4),
                LocalDate.of(2026, 8, 1)
        )).thenReturn(List.of(booking101, booking102));

        HousekeepingRoomStatusRecord statusRecord = HousekeepingRoomStatusRecord.builder()
                .propertyId("7cfd4559-b6f3-4b7d-b933-e93018ac1d47")
                .businessDate(LocalDate.of(2026, 8, 2))
                .confirmationNumber("C-101")
                .roomNo("101")
                .roomStatus("DIRTY")
                .build();

        when(housekeepingRoomStatusRepository.findByPropertyIdAndBusinessDateBetweenAndRoomNoIsNotNull(
                "7cfd4559-b6f3-4b7d-b933-e93018ac1d47",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 3)
        )).thenReturn(List.of(statusRecord));

        ReservationRoomCalendarResponseDto response = reservationRoomCalendarService.getRoomCalendar(
                "7cfd4559-b6f3-4b7d-b933-e93018ac1d47",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 3),
                List.of("King")
        );

        assertThat(response.getRooms()).hasSize(2);

        ReservationRoomCalendarResponseDto.RoomCalendarRowDto room101Row = response.getRooms().stream()
                .filter(item -> "101".equals(item.getRoomNo()))
                .findFirst()
                .orElseThrow();
        ReservationRoomCalendarResponseDto.RoomCalendarRowDto room102Row = response.getRooms().stream()
                .filter(item -> "102".equals(item.getRoomNo()))
                .findFirst()
                .orElseThrow();

        assertThat(room101Row.getCalendar()).extracting(ReservationRoomCalendarResponseDto.RoomCalendarCellDto::getStatus)
                .containsExactly("BOOKED", "DIRTY", "AVAILABLE");
        assertThat(room102Row.getCalendar()).extracting(ReservationRoomCalendarResponseDto.RoomCalendarCellDto::getStatus)
                .containsExactly("OCCUPIED", "OCCUPIED", "OCCUPIED");

        ReservationRoomCalendarResponseDto.RoomCalendarDaySummaryDto day1 = response.getSummary().stream()
                .filter(item -> LocalDate.of(2026, 8, 1).equals(item.getDate()))
                .findFirst()
                .orElseThrow();
        assertThat(day1.getBookedRooms()).isEqualTo(1);
        assertThat(day1.getOccupiedRooms()).isEqualTo(1);

        ReservationRoomCalendarResponseDto.RoomCalendarDaySummaryDto day3 = response.getSummary().stream()
                .filter(item -> LocalDate.of(2026, 8, 3).equals(item.getDate()))
                .findFirst()
                .orElseThrow();
        assertThat(day3.getAvailableRooms()).isEqualTo(1);
        assertThat(day3.getOccupiedRooms()).isEqualTo(1);
    }

    @Test
    void getRoomCalendarShouldRejectWhenPropertyWizardDisabled() {
        when(propertyWizardServiceProperties.isEnabled()).thenReturn(false);

        assertThatThrownBy(() -> reservationRoomCalendarService.getRoomCalendar(
                "7cfd4559-b6f3-4b7d-b933-e93018ac1d47",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 3),
                List.of("King")
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Room calendar is unavailable because Property Wizard integration is disabled");
    }

    @Test
    void getRoomCalendarShouldSupportMultipleRoomTypesInSingleRequest() {
        when(propertyWizardServiceProperties.isEnabled()).thenReturn(true);

        PropertyRoomInventoryDto room101 = new PropertyRoomInventoryDto();
        room101.setRoomNumber("101");
        room101.setRoomType("King");

        PropertyRoomInventoryDto room201 = new PropertyRoomInventoryDto();
        room201.setRoomNumber("201");
        room201.setRoomType("Suite");

        PropertyRoomInventoryDto room301 = new PropertyRoomInventoryDto();
        room301.setRoomNumber("301");
        room301.setRoomType("Twin");

        when(housekeepingRoomCalendarClient.fetchRooms(
                "7cfd4559-b6f3-4b7d-b933-e93018ac1d47",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 2)
        )).thenReturn(List.of(room101, room201, room301));

        when(reservationBookingRepository.findByPropertyIdAndAssignedRoomNoIsNotNullAndArrivalDateLessThanAndDepartureDateGreaterThan(
                "7cfd4559-b6f3-4b7d-b933-e93018ac1d47",
                LocalDate.of(2026, 8, 3),
                LocalDate.of(2026, 8, 1)
        )).thenReturn(List.of());

        when(housekeepingRoomStatusRepository.findByPropertyIdAndBusinessDateBetweenAndRoomNoIsNotNull(
                "7cfd4559-b6f3-4b7d-b933-e93018ac1d47",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 2)
        )).thenReturn(List.of());

        ReservationRoomCalendarResponseDto response = reservationRoomCalendarService.getRoomCalendar(
                "7cfd4559-b6f3-4b7d-b933-e93018ac1d47",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 2),
                List.of("King", "Suite")
        );

        assertThat(response.getRoomTypes()).containsExactly("King", "Suite");
        assertThat(response.getRooms()).extracting(ReservationRoomCalendarResponseDto.RoomCalendarRowDto::getRoomNo)
                .containsExactly("101", "201");
    }
}
