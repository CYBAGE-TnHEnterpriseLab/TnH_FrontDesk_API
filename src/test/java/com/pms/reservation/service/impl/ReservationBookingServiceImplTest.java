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
import com.pms.reservation.dto.PaymentProcessingResult;
import com.pms.reservation.dto.ReservationBookingRequestDto;
import com.pms.reservation.dto.ReservationBookingResponseDto;
import com.pms.reservation.entity.ReservationBookingRecord;
import com.pms.reservation.entity.ReservationPaymentTransactionRecord;
import com.pms.reservation.integration.PropertyInventoryPort;
import com.pms.reservation.integration.dto.PropertyInventoryValidationResponse;
import com.pms.reservation.mapper.ReservationBookingMapper;
import com.pms.reservation.repository.ReservationBookingRepository;
import com.pms.reservation.repository.ReservationPaymentTransactionRepository;
import com.pms.reservation.service.PaymentProcessingService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private ReservationPaymentTransactionRepository reservationPaymentTransactionRepository;

    @Mock
    private PropertyInventoryPort propertyInventoryPort;

    @Mock
    private PropertyWizardServiceProperties propertyWizardServiceProperties;

    @Mock
    private ReservationBookingMapper reservationBookingMapper;

    @Mock
    private PaymentProcessingService paymentProcessingService;

    @InjectMocks
    private ReservationBookingServiceImpl reservationBookingService;

    @Test
    void createBookingShouldPersistAndReturnResponseWithGeneratedConfirmation() {
        ReservationBookingRequestDto request = validRequest();
        when(propertyWizardServiceProperties.isEnabled()).thenReturn(false);
        when(paymentProcessingService.processPayment(any(), any(), any())).thenReturn(successResult("PAY-100"));

        ReservationBookingRecord toSave = ReservationBookingRecord.builder()
                .propertyId(request.getPropertyId())
                .roomType(request.getRoomType())
                .numberOfRooms(request.getNumberOfRooms())
                .build();

        ReservationBookingRecord savedRecord = ReservationBookingRecord.builder()
                .id(99L)
                .propertyId("PROP001")
                .confirmationNumber("PROP001-20260718120000000-123")
                .reservationStatus("CONFIRMED")
                .guestName("Alex Johnson")
                .build();

        ReservationPaymentTransactionRecord savedTxn = ReservationPaymentTransactionRecord.builder()
                .id(501L)
                .bookingId(99L)
                .confirmationNumber("PROP001-20260718120000000-123")
                .propertyId("PROP001")
                .paymentMode("CARD")
                .amount(new BigDecimal("17000.00"))
                .transactionStatus("SUCCESS")
                .transactionReference("PAY-100")
                .processorName("SIMULATED_GATEWAY")
                .processedAt(LocalDateTime.of(2026, 7, 18, 12, 0))
                .createdAt(LocalDateTime.of(2026, 7, 18, 12, 0))
                .build();

        ReservationBookingResponseDto responseDto = ReservationBookingResponseDto.builder()
                .bookingId(99L)
                .confirmationNumber("PROP001-20260718120000000-123")
                .reservationStatus("CONFIRMED")
                .guestName("Alex Johnson")
                .totalRate(new BigDecimal("17000.00"))
                .build();

        when(reservationBookingMapper.toEntity(request)).thenReturn(toSave);
        when(reservationBookingRepository.save(any(ReservationBookingRecord.class))).thenReturn(savedRecord);
        when(reservationPaymentTransactionRepository.save(any(ReservationPaymentTransactionRecord.class))).thenReturn(savedTxn);
        when(reservationBookingMapper.toResponse(savedRecord, savedTxn)).thenReturn(responseDto);

        ReservationBookingResponseDto response = reservationBookingService.createBooking(request);

        assertThat(response.getBookingId()).isEqualTo(99L);
        assertThat(response.getReservationStatus()).isEqualTo("CONFIRMED");
        assertThat(response.getGuestName()).isEqualTo("Alex Johnson");

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
        when(paymentProcessingService.processPayment(any(), any(), any())).thenReturn(successResult("PAY-200"));

        ReservationBookingRecord toSave = ReservationBookingRecord.builder()
                .propertyId(request.getPropertyId())
                .roomType(request.getRoomType())
                .numberOfRooms(request.getNumberOfRooms())
                .build();

        ReservationBookingRecord savedRecord = ReservationBookingRecord.builder()
                .id(100L)
                .propertyId("PROP001")
                .confirmationNumber("PROP001-20260718120000000-456")
                .reservationStatus("CONFIRMED")
                .guestName("Alex Johnson")
                .build();

        ReservationPaymentTransactionRecord savedTxn = ReservationPaymentTransactionRecord.builder()
                .id(601L)
                .bookingId(100L)
                .confirmationNumber("PROP001-20260718120000000-456")
                .propertyId("PROP001")
                .paymentMode("CARD")
                .amount(new BigDecimal("17000.00"))
                .transactionStatus("SUCCESS")
                .transactionReference("PAY-200")
                .processorName("SIMULATED_GATEWAY")
                .processedAt(LocalDateTime.of(2026, 7, 18, 12, 5))
                .createdAt(LocalDateTime.of(2026, 7, 18, 12, 5))
                .build();

        ReservationBookingResponseDto responseDto = ReservationBookingResponseDto.builder()
                .bookingId(100L)
                .reservationStatus("CONFIRMED")
                .guestName("Alex Johnson")
                .build();

        when(reservationBookingMapper.toEntity(request)).thenReturn(toSave);
        when(reservationBookingRepository.save(any(ReservationBookingRecord.class))).thenReturn(savedRecord);
        when(reservationPaymentTransactionRepository.save(any(ReservationPaymentTransactionRecord.class))).thenReturn(savedTxn);
        when(reservationBookingMapper.toResponse(savedRecord, savedTxn)).thenReturn(responseDto);

        reservationBookingService.createBooking(request);

        verify(propertyInventoryPort).validateInventory(eq("PROP001"), eq("Deluxe King"), eq(1));
        verify(propertyInventoryPort).deductInventory(any());
        verify(propertyInventoryPort).syncInventory(any());

        ArgumentCaptor<ReservationBookingRecord> recordCaptor = ArgumentCaptor.forClass(ReservationBookingRecord.class);
        verify(reservationBookingRepository).save(recordCaptor.capture());
        ReservationBookingRecord persistedRecord = recordCaptor.getValue();
        assertThat(persistedRecord.getInventoryDeductedAt()).isNotNull();
        assertThat(persistedRecord.getInventorySyncedAt()).isNotNull();
    }

    @Test
    void createBookingShouldRejectWhenGuestNamesCountDoesNotMatchRoomCount() {
        ReservationBookingRequestDto request = validRequest();
        request.setGuestNames(List.of("Alex", "Sam"));

        assertThatThrownBy(() -> reservationBookingService.createBooking(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("guestNames count must match numberOfRooms");

        verify(reservationBookingRepository, never()).save(any());
    }

    @Test
    void createBookingShouldRejectWhenMoreThanNineRoomsSelected() {
        ReservationBookingRequestDto request = validRequest();
        request.setNumberOfRooms(10);

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
        verify(paymentProcessingService, never()).processPayment(any(), any(), any());
    }

    @Test
    void createBookingShouldRejectWhenDepartureIsBeforeArrival() {
        ReservationBookingRequestDto request = validRequest();
        request.setArrivalDate(LocalDate.of(2026, 7, 22));
        request.setDepartureDate(LocalDate.of(2026, 7, 20));

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
        verify(paymentProcessingService, never()).processPayment(any(), any(), any());
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
        verify(paymentProcessingService, never()).processPayment(any(), any(), any());
    }

    @Test
    void createBookingShouldRejectWhenRequestedRoomsExceedAvailability() {
        ReservationBookingRequestDto request = validRequest();
        request.setNumberOfRooms(3);
        request.setGuestNames(List.of("Alex Johnson", "Sam Lee", "Jordan Fox"));
        when(propertyWizardServiceProperties.isEnabled()).thenReturn(true);
        when(propertyInventoryPort.validateInventory(eq("PROP001"), eq("Deluxe King"), eq(3)))
                .thenReturn(validationResponse(true, true, 2));

        assertThatThrownBy(() -> reservationBookingService.createBooking(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("numberOfRooms exceeds available rooms for selected property and roomType");

        verify(reservationBookingRepository, never()).save(any());
        verify(paymentProcessingService, never()).processPayment(any(), any(), any());
    }

    @Test
    void createBookingShouldRejectWhenPaymentProcessingFails() {
        ReservationBookingRequestDto request = validRequest();
        when(propertyWizardServiceProperties.isEnabled()).thenReturn(false);
        when(paymentProcessingService.processPayment(any(), any(), any()))
                .thenReturn(PaymentProcessingResult.builder()
                        .status("FAILED")
                        .failureReason("declined")
                        .processedAt(LocalDateTime.of(2026, 7, 18, 12, 10))
                        .build());

        assertThatThrownBy(() -> reservationBookingService.createBooking(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("payment processing failed: declined");

        verify(reservationBookingRepository, never()).save(any());
        verify(reservationPaymentTransactionRepository, never()).save(any());
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

    private PaymentProcessingResult successResult(String txnRef) {
        return PaymentProcessingResult.builder()
                .status("SUCCESS")
                .transactionReference(txnRef)
                .processorName("SIMULATED_GATEWAY")
                .processedAt(LocalDateTime.of(2026, 7, 18, 12, 0))
                .build();
    }

    private ReservationBookingRequestDto validRequest() {
        ReservationBookingRequestDto request = new ReservationBookingRequestDto();
        request.setPropertyId("PROP001");
        request.setSalutation("Mr");
        request.setVipTag(Boolean.FALSE);
        request.setGuestName("Alex Johnson");
        request.setGuestNames(List.of("Alex Johnson"));
        request.setPersonalEmail("alex.personal@example.com");
        request.setOfficialEmail("alex.official@example.com");
        request.setCity("Pune");
        request.setCountry("India");
        request.setZipCode("411001");
        request.setPhoneNumber("+91-9876543210");
        request.setMobileNumber("+91-9876543210");
        request.setLoyaltyNumber("LOY1234");
        request.setCompany("Contoso");
        request.setGuestGroup("CORP");
        request.setSource("Website");
        request.setAgent("Online");
        request.setArrivalDate(LocalDate.of(2026, 7, 20));
        request.setDepartureDate(LocalDate.of(2026, 7, 22));
        request.setAdultCount(2);
        request.setChildCount(1);
        request.setReservationType("GTD");
        request.setRoomType("Deluxe King");
        request.setRateCode("BAR001");
        request.setNumberOfRooms(1);
        request.setRate(new BigDecimal("8500.00"));
        request.setPayment("CARD");
        request.setEta(LocalTime.of(15, 0));
        request.setCheckOutTime(LocalTime.of(11, 0));
        request.setDnm(Boolean.FALSE);
        request.setNoPost(Boolean.FALSE);
        request.setGuestBalance(new BigDecimal("0.00"));
        request.setSpecialRequests("High floor");
        request.setDiscount(new BigDecimal("0.00"));
        request.setAlertsMessages("N/A");
        return request;
    }
}
