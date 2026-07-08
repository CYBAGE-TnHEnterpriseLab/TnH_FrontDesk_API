package com.pms.reservation.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pms.guestlisting.exception.BadRequestException;
import com.pms.reservation.config.PropertyWizardServiceProperties;
import com.pms.reservation.dto.ReservationBookingRequestDto;
import com.pms.reservation.dto.ReservationBookingResponseDto;
import com.pms.reservation.entity.ReservationBookingRecord;
import com.pms.reservation.integration.PropertyInventoryPort;
import com.pms.reservation.integration.dto.PropertyInventoryValidationResponse;
import com.pms.reservation.mapper.ReservationBookingMapper;
import com.pms.reservation.repository.ReservationBookingRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReservationBookingServiceImplTest {

    @Mock
    private ReservationBookingRepository reservationBookingRepository;

    @Mock
        private PropertyInventoryPort propertyInventoryPort;

    @Mock
    private PropertyWizardServiceProperties propertyWizardServiceProperties;

    @Mock
    private ReservationBookingMapper reservationBookingMapper;

    @InjectMocks
    private ReservationBookingServiceImpl reservationBookingService;

    @Test
    void createBookingShouldPersistAndReturnResponseWithGeneratedConfirmation() {
        ReservationBookingRequestDto request = validRequest();
        when(propertyWizardServiceProperties.isEnabled()).thenReturn(false);

        ReservationBookingRecord toSave = ReservationBookingRecord.builder()
                .propertyId(request.getPropertyId())
                .roomType(request.getRoomType())
                .numberOfRooms(request.getNumberOfRooms())
                .build();

        ReservationBookingRecord savedRecord = ReservationBookingRecord.builder()
                .id(99L)
                .confirmationNumber("PROP001-20260101123000000-123")
                .reservationStatus("CONFIRMED")
                .guestName(request.getGuestName())
                .guestNamesEncoded("QWxleCBKb2huc29u")
                .reservationType(request.getReservationType())
                .totalRate(new BigDecimal("8500.00"))
                .build();

        ReservationBookingResponseDto responseDto = ReservationBookingResponseDto.builder()
                .bookingId(99L)
                .confirmationNumber("PROP001-20260101123000000-123")
                .reservationStatus("CONFIRMED")
                .guestName("Alex Johnson")
                .guestNames(List.of("Alex Johnson"))
                .reservationType("GTD")
                .totalRate(new BigDecimal("8500.00"))
                .build();

        when(reservationBookingMapper.toEntity(request)).thenReturn(toSave);
        when(reservationBookingRepository.save(any(ReservationBookingRecord.class))).thenReturn(savedRecord);
        when(reservationBookingMapper.toResponse(savedRecord)).thenReturn(responseDto);

        ReservationBookingResponseDto response = reservationBookingService.createBooking(request);

        assertThat(response.getBookingId()).isEqualTo(99L);
        assertThat(response.getConfirmationNumber()).isEqualTo("PROP001-20260101123000000-123");
        assertThat(response.getReservationStatus()).isEqualTo("CONFIRMED");
        assertThat(response.getGuestName()).isEqualTo("Alex Johnson");
        assertThat(response.getGuestNames()).containsExactly("Alex Johnson");
        assertThat(response.getReservationType()).isEqualTo("GTD");
        assertThat(response.getTotalRate()).isEqualByComparingTo("8500.00");

        ArgumentCaptor<ReservationBookingRecord> recordCaptor = ArgumentCaptor.forClass(ReservationBookingRecord.class);
        verify(reservationBookingRepository).save(recordCaptor.capture());
        ReservationBookingRecord persistedRecord = recordCaptor.getValue();
        assertThat(persistedRecord.getConfirmationNumber()).isNotBlank();
        assertThat(persistedRecord.getReservationStatus()).isEqualTo("CONFIRMED");
        assertThat(persistedRecord.getInventoryDeductedAt()).isNull();
        assertThat(persistedRecord.getInventorySyncedAt()).isNull();
        verify(propertyInventoryPort, never()).validateInventory(any(), any(), any());
        verify(propertyInventoryPort, never()).deductInventory(any());
        verify(propertyInventoryPort, never()).syncInventory(any());
    }

    @Test
        void createBookingShouldDeductAndSyncInventoryWhenPropertyWizardEnabled() {
        ReservationBookingRequestDto request = validRequest();
        when(propertyWizardServiceProperties.isEnabled()).thenReturn(true);
        when(propertyInventoryPort.validateInventory(eq("PROP001"), eq("Deluxe King"), eq(1)))
                .thenReturn(validationResponse(true, true, 5));

        ReservationBookingRecord toSave = ReservationBookingRecord.builder()
                .propertyId(request.getPropertyId())
                .roomType(request.getRoomType())
                .numberOfRooms(request.getNumberOfRooms())
                .guestNamesEncoded("QWxleCBKb2huc29u")
                .totalRate(new BigDecimal("8500.00"))
                .build();

        ReservationBookingResponseDto responseDto = ReservationBookingResponseDto.builder()
                .bookingId(100L)
                .reservationStatus("CONFIRMED")
                .totalRate(new BigDecimal("8500.00"))
                .build();

        when(reservationBookingMapper.toEntity(request)).thenReturn(toSave);
        when(reservationBookingRepository.save(any(ReservationBookingRecord.class))).thenReturn(toSave);
        when(reservationBookingMapper.toResponse(any(ReservationBookingRecord.class))).thenReturn(responseDto);

        reservationBookingService.createBooking(request);

        verify(propertyInventoryPort).validateInventory(eq("PROP001"), eq("Deluxe King"), eq(1));
        verify(propertyInventoryPort).deductInventory(any());
        verify(propertyInventoryPort).syncInventory(any());

        ArgumentCaptor<ReservationBookingRecord> recordCaptor = ArgumentCaptor.forClass(ReservationBookingRecord.class);
        verify(reservationBookingRepository).save(recordCaptor.capture());
        ReservationBookingRecord persistedRecord = recordCaptor.getValue();
        assertThat(persistedRecord.getInventoryDeductedAt()).isNotNull();
        assertThat(persistedRecord.getInventorySyncedAt()).isNotNull();
        assertThat(persistedRecord.getTotalRate()).isEqualByComparingTo("8500.00");
    }

    @Test
    void createBookingShouldRejectWhenGuestNamesCountDoesNotMatchRoomCount() {
        ReservationBookingRequestDto request = validRequest();
        request.setNumberOfRooms(2);
        request.setGuestNames(List.of("Alex Johnson"));

        assertThatThrownBy(() -> reservationBookingService.createBooking(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("guestNames count must match numberOfRooms");

        verify(reservationBookingRepository, never()).save(any());
    }

        @Test
        void createBookingShouldRejectWhenGuestNamesContainBlankValues() {
                ReservationBookingRequestDto request = validRequest();
                request.setNumberOfRooms(2);
                request.setGuestNames(List.of("Alex Johnson", " "));

                assertThatThrownBy(() -> reservationBookingService.createBooking(request))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessage("guestNames must not contain blank values");

                verify(reservationBookingRepository, never()).save(any());
        }

    @Test
    void createBookingShouldRejectWhenMoreThanNineRoomsSelected() {
        ReservationBookingRequestDto request = validRequest();
        request.setNumberOfRooms(10);
        request.setGuestNames(List.of(
                "Guest 1", "Guest 2", "Guest 3", "Guest 4", "Guest 5",
                "Guest 6", "Guest 7", "Guest 8", "Guest 9", "Guest 10"
        ));

        assertThatThrownBy(() -> reservationBookingService.createBooking(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("numberOfRooms must be between 1 and 9");

        verify(reservationBookingRepository, never()).save(any());
    }

        @Test
        void createBookingShouldRejectWhenPaymentModeIsUnsupported() {
                ReservationBookingRequestDto request = validRequest();
                request.setPayment("CHEQUE");

                assertThatThrownBy(() -> reservationBookingService.createBooking(request))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessage("payment must be one of CARD, CASH, UPI, NET_BANKING, WALLET");

                verify(reservationBookingRepository, never()).save(any());
        }

    @Test
    void createBookingShouldRejectWhenDepartureIsBeforeArrival() {
        ReservationBookingRequestDto request = validRequest();
        request.setArrivalDate(LocalDate.of(2026, 6, 22));
        request.setDepartureDate(LocalDate.of(2026, 6, 20));

        assertThatThrownBy(() -> reservationBookingService.createBooking(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("departureDate must be on or after arrivalDate");

        verify(propertyInventoryPort, never()).validateInventory(any(), any(), any());
    }

    @Test
    void createBookingShouldRejectWhenPropertyIsInvalidFromPropertyWizard() {
        ReservationBookingRequestDto request = validRequest();
        when(propertyWizardServiceProperties.isEnabled()).thenReturn(true);
        when(propertyInventoryPort.validateInventory(eq("PROP001"), eq("Deluxe King"), eq(1)))
                .thenReturn(validationResponse(false, true, 5));

        assertThatThrownBy(() -> reservationBookingService.createBooking(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("propertyId is invalid as per Property Wizard service");

        verify(reservationBookingRepository, never()).save(any());
    }

        @Test
        void createBookingShouldRejectWhenRoomTypeUnavailableFromPropertyWizard() {
                ReservationBookingRequestDto request = validRequest();
                when(propertyWizardServiceProperties.isEnabled()).thenReturn(true);
                when(propertyInventoryPort.validateInventory(eq("PROP001"), eq("Deluxe King"), eq(1)))
                                .thenReturn(validationResponse(true, false, 0));

                assertThatThrownBy(() -> reservationBookingService.createBooking(request))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessage("roomType is not available for selected property");

                verify(reservationBookingRepository, never()).save(any());
        }

    @Test
    void createBookingShouldRejectWhenRequestedRoomsExceedAvailability() {
        ReservationBookingRequestDto request = validRequest();
        request.setNumberOfRooms(3);
                request.setGuestNames(List.of("Guest 1", "Guest 2", "Guest 3"));
        when(propertyWizardServiceProperties.isEnabled()).thenReturn(true);
        when(propertyInventoryPort.validateInventory(eq("PROP001"), eq("Deluxe King"), eq(3)))
                .thenReturn(validationResponse(true, true, 2));

        assertThatThrownBy(() -> reservationBookingService.createBooking(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("numberOfRooms exceeds available rooms for selected property and roomType");

        verify(reservationBookingRepository, never()).save(any());
    }

    private PropertyInventoryValidationResponse validationResponse(
            boolean propertyExists,
            boolean roomTypeAvailable,
            Integer availableRooms
    ) {
        PropertyInventoryValidationResponse response = new PropertyInventoryValidationResponse();
        response.setPropertyExists(propertyExists);
        response.setRoomTypeAvailable(roomTypeAvailable);
        response.setAvailableRooms(availableRooms);
        return response;
    }

    private ReservationBookingRequestDto validRequest() {
        ReservationBookingRequestDto request = new ReservationBookingRequestDto();
        request.setPropertyId("PROP001");
        request.setSalutation("Mr.");
        request.setVipTag(Boolean.FALSE);
        request.setGuestName("Alex Johnson");
        request.setGuestNames(List.of("Alex Johnson"));
        request.setPersonalEmail("alex.personal@example.com");
        request.setOfficialEmail("alex.official@example.com");
        request.setCity("Mumbai");
        request.setCountry("India");
        request.setZipCode("400001");
        request.setPhoneNumber("+91-22-1234567");
        request.setMobileNumber("+91-9876543210");
        request.setLoyaltyNumber("LOY1234");
        request.setCompany("Contoso");
        request.setGuestGroup("Corporate");
        request.setSource("Website");
        request.setAgent("Agent A");
        request.setArrivalDate(LocalDate.of(2026, 6, 20));
        request.setDepartureDate(LocalDate.of(2026, 6, 22));
        request.setAdultCount(2);
        request.setChildCount(1);
        request.setReservationType("GTD");
        request.setRoomType("Deluxe King");
        request.setRateCode("BAR");
        request.setNumberOfRooms(1);
        request.setRate(new BigDecimal("8500.00"));
        request.setPayment("Card");
        request.setEta(LocalTime.of(15, 0));
        request.setCheckOutTime(LocalTime.of(11, 0));
        request.setDnm(Boolean.FALSE);
        request.setNoPost(Boolean.FALSE);
        request.setGuestBalance(new BigDecimal("0.00"));
        request.setSpecialRequests("High floor");
        request.setDiscount(new BigDecimal("500.00"));
        request.setAlertsMessages("Guest requested quiet room");
        return request;
    }
}
