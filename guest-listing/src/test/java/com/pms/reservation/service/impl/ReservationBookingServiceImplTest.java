package com.pms.reservation.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pms.guestlisting.exception.BadRequestException;
import com.pms.guestlisting.exception.ExternalServiceException;
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
                when(reservationBookingRepository.existsByConfirmationNumber(any())).thenReturn(false);
        when(paymentProcessingService.processPayment(any(), any(), any())).thenReturn(successResult("PAY-100"));

        ReservationBookingRecord toSave = ReservationBookingRecord.builder()
                .propertyId(request.getPropertyId())
                .roomType(request.getRoomType())
                .numberOfRooms(request.getNumberOfRooms())
                .build();

        ReservationBookingRecord savedRecord = ReservationBookingRecord.builder()
                .id(99L)
                .propertyId("PROP001")
                .confirmationNumber("1234567890")
                .reservationStatus("CONFIRMED")
                .guestName("Alex Johnson")
                .build();

        ReservationPaymentTransactionRecord savedTxn = ReservationPaymentTransactionRecord.builder()
                .id(501L)
                .bookingId(99L)
                .confirmationNumber("1234567890")
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
                .confirmationNumber("1234567890")
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
        assertThat(persistedRecord.getConfirmationNumber()).matches("\\d{10}");
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
                when(reservationBookingRepository.existsByConfirmationNumber(any())).thenReturn(false);
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
                .confirmationNumber("1234567891")
                .reservationStatus("CONFIRMED")
                .guestName("Alex Johnson")
                .build();

        ReservationPaymentTransactionRecord savedTxn = ReservationPaymentTransactionRecord.builder()
                .id(601L)
                .bookingId(100L)
                .confirmationNumber("1234567891")
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
        void createBookingShouldAutoPopulateMissingOptionalCreateFieldsForSingleRoom() {
                ReservationBookingRequestDto request = validRequest();
                request.setSalutation(null);
                request.setGuestNames(null);
                request.setPersonalEmail(null);
                request.setOfficialEmail("alex.official@example.com");
                request.setCity(null);
                request.setCountry(null);
                request.setZipCode(null);
                request.setMobileNumber(null);
                request.setReservationType(null);
                request.setNoPost(null);
                request.setPaymentType(null);

                when(propertyWizardServiceProperties.isEnabled()).thenReturn(false);
                when(reservationBookingRepository.existsByConfirmationNumber(any())).thenReturn(false);
                when(paymentProcessingService.processPayment(any(), any(), any())).thenReturn(successResult("PAY-330"));

                ReservationBookingRecord toSave = ReservationBookingRecord.builder()
                        .propertyId(request.getPropertyId())
                        .roomType(request.getRoomType())
                        .numberOfRooms(request.getNumberOfRooms())
                        .build();

                ReservationBookingRecord savedRecord = ReservationBookingRecord.builder()
                        .id(140L)
                        .propertyId("PROP001")
                        .confirmationNumber("1234567892")
                        .reservationStatus("CONFIRMED")
                        .guestName("Alex Johnson")
                        .build();

                ReservationPaymentTransactionRecord savedTxn = ReservationPaymentTransactionRecord.builder()
                        .id(701L)
                        .bookingId(140L)
                        .confirmationNumber("1234567892")
                        .propertyId("PROP001")
                        .paymentMode("CARD")
                        .amount(new BigDecimal("17000.00"))
                        .transactionStatus("SUCCESS")
                        .transactionReference("PAY-330")
                        .processorName("SIMULATED_GATEWAY")
                        .processedAt(LocalDateTime.of(2026, 7, 18, 12, 10))
                        .createdAt(LocalDateTime.of(2026, 7, 18, 12, 10))
                        .build();

                when(reservationBookingMapper.toEntity(any(ReservationBookingRequestDto.class))).thenReturn(toSave);
                when(reservationBookingRepository.save(any(ReservationBookingRecord.class))).thenReturn(savedRecord);
                when(reservationPaymentTransactionRepository.save(any(ReservationPaymentTransactionRecord.class))).thenReturn(savedTxn);
                when(reservationBookingMapper.toResponse(savedRecord, savedTxn)).thenReturn(ReservationBookingResponseDto.builder()
                        .bookingId(140L)
                        .reservationStatus("CONFIRMED")
                        .guestName("Alex Johnson")
                        .build());

                reservationBookingService.createBooking(request);

                ArgumentCaptor<ReservationBookingRequestDto> requestCaptor = ArgumentCaptor.forClass(ReservationBookingRequestDto.class);
                verify(reservationBookingMapper).toEntity(requestCaptor.capture());

                ReservationBookingRequestDto normalized = requestCaptor.getValue();
                assertThat(normalized.getSalutation()).isEqualTo("Mr");
                assertThat(normalized.getGuestNames()).containsExactly("Alex Johnson");
                assertThat(normalized.getPersonalEmail()).isEqualTo("alex.official@example.com");
                assertThat(normalized.getOfficialEmail()).isEqualTo("alex.official@example.com");
                assertThat(normalized.getCity()).isEqualTo("UNKNOWN");
                assertThat(normalized.getCountry()).isEqualTo("UNKNOWN");
                assertThat(normalized.getZipCode()).isEqualTo("000000");
                assertThat(normalized.getMobileNumber()).isEqualTo("+91-9876543210");
                assertThat(normalized.getReservationType()).isEqualTo("GTD");
                assertThat(normalized.getPaymentType()).isEqualTo("FULL_PAYMENT");
                assertThat(normalized.getNoPost()).isFalse();
        }

        @Test
        void createBookingShouldRejectWhenPaymentTypeIsUnsupported() {
                ReservationBookingRequestDto request = validRequest();
                request.setPaymentType("PARTIAL");

                assertThatThrownBy(() -> reservationBookingService.createBooking(request))
                        .isInstanceOf(BadRequestException.class)
                        .hasMessage("paymentType must be one of ADVANCE, FULL_PAYMENT");

                verify(reservationBookingRepository, never()).save(any());
                verify(paymentProcessingService, never()).processPayment(any(), any(), any());
        }

        @Test
        void createBookingShouldAutoExpandGuestNamesForMultiRoomWhenOnlyPrimaryGuestProvided() {
                ReservationBookingRequestDto request = validRequest();
                request.setNumberOfRooms(3);
                request.setGuestNames(List.of("Alex Johnson"));

                when(propertyWizardServiceProperties.isEnabled()).thenReturn(false);
                when(reservationBookingRepository.existsByConfirmationNumber(any())).thenReturn(false);
                when(paymentProcessingService.processPayment(any(), any(), any())).thenReturn(successResult("PAY-331"));

                ReservationBookingRecord toSave = ReservationBookingRecord.builder()
                        .propertyId(request.getPropertyId())
                        .roomType(request.getRoomType())
                        .numberOfRooms(request.getNumberOfRooms())
                        .build();

                ReservationBookingRecord savedRecord = ReservationBookingRecord.builder()
                        .id(141L)
                        .propertyId("PROP001")
                        .confirmationNumber("1234567893")
                        .reservationStatus("CONFIRMED")
                        .guestName("Alex Johnson")
                        .build();

                ReservationPaymentTransactionRecord savedTxn = ReservationPaymentTransactionRecord.builder()
                        .id(702L)
                        .bookingId(141L)
                        .confirmationNumber("1234567893")
                        .propertyId("PROP001")
                        .paymentMode("CARD")
                        .amount(new BigDecimal("51000.00"))
                        .transactionStatus("SUCCESS")
                        .transactionReference("PAY-331")
                        .processorName("SIMULATED_GATEWAY")
                        .processedAt(LocalDateTime.of(2026, 7, 18, 12, 12))
                        .createdAt(LocalDateTime.of(2026, 7, 18, 12, 12))
                        .build();

                when(reservationBookingMapper.toEntity(any(ReservationBookingRequestDto.class))).thenReturn(toSave);
                when(reservationBookingRepository.save(any(ReservationBookingRecord.class))).thenReturn(savedRecord);
                when(reservationPaymentTransactionRepository.save(any(ReservationPaymentTransactionRecord.class))).thenReturn(savedTxn);
                when(reservationBookingMapper.toResponse(savedRecord, savedTxn)).thenReturn(ReservationBookingResponseDto.builder()
                        .bookingId(141L)
                        .reservationStatus("CONFIRMED")
                        .guestName("Alex Johnson")
                        .build());

                reservationBookingService.createBooking(request);

                ArgumentCaptor<ReservationBookingRequestDto> requestCaptor = ArgumentCaptor.forClass(ReservationBookingRequestDto.class);
                verify(reservationBookingMapper).toEntity(requestCaptor.capture());

                ReservationBookingRequestDto normalized = requestCaptor.getValue();
                assertThat(normalized.getGuestNames()).hasSize(3);
                assertThat(normalized.getGuestNames()).containsExactly("Alex Johnson", "Alex Johnson", "Alex Johnson");
        }

    @Test
    void createBookingShouldNormalizeUiPlaceholderPaymentTypeAndNegativeGuestBalance() {
        ReservationBookingRequestDto request = validRequest();
        request.setPaymentType("Select Payment Type");
        request.setGuestBalance(new BigDecimal("-1800"));

        when(propertyWizardServiceProperties.isEnabled()).thenReturn(false);
                when(reservationBookingRepository.existsByConfirmationNumber(any())).thenReturn(false);
        when(paymentProcessingService.processPayment(any(), any(), any())).thenReturn(successResult("PAY-332"));

        ReservationBookingRecord toSave = ReservationBookingRecord.builder()
                .propertyId(request.getPropertyId())
                .roomType(request.getRoomType())
                .numberOfRooms(request.getNumberOfRooms())
                .build();

        ReservationBookingRecord savedRecord = ReservationBookingRecord.builder()
                .id(142L)
                .propertyId("PROP001")
                .confirmationNumber("1234567894")
                .reservationStatus("CONFIRMED")
                .guestName("Alex Johnson")
                .build();

        ReservationPaymentTransactionRecord savedTxn = ReservationPaymentTransactionRecord.builder()
                .id(703L)
                .bookingId(142L)
                .confirmationNumber("1234567894")
                .propertyId("PROP001")
                .paymentMode("CARD")
                .amount(new BigDecimal("17000.00"))
                .transactionStatus("SUCCESS")
                .transactionReference("PAY-332")
                .processorName("SIMULATED_GATEWAY")
                .processedAt(LocalDateTime.of(2026, 7, 18, 12, 12))
                .createdAt(LocalDateTime.of(2026, 7, 18, 12, 12))
                .build();

        when(reservationBookingMapper.toEntity(any(ReservationBookingRequestDto.class))).thenReturn(toSave);
        when(reservationBookingRepository.save(any(ReservationBookingRecord.class))).thenReturn(savedRecord);
        when(reservationPaymentTransactionRepository.save(any(ReservationPaymentTransactionRecord.class))).thenReturn(savedTxn);
        when(reservationBookingMapper.toResponse(savedRecord, savedTxn)).thenReturn(ReservationBookingResponseDto.builder()
                .bookingId(142L)
                .reservationStatus("CONFIRMED")
                .guestName("Alex Johnson")
                .build());

        reservationBookingService.createBooking(request);

        ArgumentCaptor<ReservationBookingRequestDto> requestCaptor = ArgumentCaptor.forClass(ReservationBookingRequestDto.class);
        verify(reservationBookingMapper).toEntity(requestCaptor.capture());

        ReservationBookingRequestDto normalized = requestCaptor.getValue();
        assertThat(normalized.getPaymentType()).isEqualTo("FULL_PAYMENT");
        assertThat(normalized.getGuestBalance()).isEqualByComparingTo("1800");
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
        void createBookingShouldRejectWhenOfficialEmailMissing() {
                ReservationBookingRequestDto request = validRequest();
                request.setOfficialEmail(" ");
                request.setPersonalEmail(" ");

                assertThatThrownBy(() -> reservationBookingService.createBooking(request))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessage("officialEmail is required");

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
        void createBookingShouldProceedWhenPropertyWizardValidationFailsAndFailOpenEnabled() {
                ReservationBookingRequestDto request = validRequest();
                when(propertyWizardServiceProperties.isEnabled()).thenReturn(true);
                when(propertyWizardServiceProperties.isFailOpenOnValidationError()).thenReturn(true);
                when(reservationBookingRepository.existsByConfirmationNumber(any())).thenReturn(false);
                when(propertyInventoryPort.validateInventory(eq("PROP001"), eq("Deluxe King"), eq(1)))
                        .thenThrow(new ExternalServiceException("pw unavailable"));
                when(paymentProcessingService.processPayment(any(), any(), any())).thenReturn(successResult("PAY-299"));

                ReservationBookingRecord toSave = ReservationBookingRecord.builder()
                        .propertyId(request.getPropertyId())
                        .roomType(request.getRoomType())
                        .numberOfRooms(request.getNumberOfRooms())
                        .build();

                ReservationBookingRecord savedRecord = ReservationBookingRecord.builder()
                        .id(120L)
                        .propertyId("PROP001")
                        .confirmationNumber("1234567895")
                        .reservationStatus("CONFIRMED")
                        .guestName("Alex Johnson")
                        .build();

                ReservationPaymentTransactionRecord savedTxn = ReservationPaymentTransactionRecord.builder()
                        .id(699L)
                        .bookingId(120L)
                        .confirmationNumber("1234567895")
                        .propertyId("PROP001")
                        .paymentMode("CARD")
                        .amount(new BigDecimal("17000.00"))
                        .transactionStatus("SUCCESS")
                        .transactionReference("PAY-299")
                        .processorName("SIMULATED_GATEWAY")
                        .processedAt(LocalDateTime.of(2026, 7, 18, 12, 20))
                        .createdAt(LocalDateTime.of(2026, 7, 18, 12, 20))
                        .build();

                when(reservationBookingMapper.toEntity(any(ReservationBookingRequestDto.class))).thenReturn(toSave);
                when(reservationBookingRepository.save(any(ReservationBookingRecord.class))).thenReturn(savedRecord);
                when(reservationPaymentTransactionRepository.save(any(ReservationPaymentTransactionRecord.class))).thenReturn(savedTxn);
                when(reservationBookingMapper.toResponse(savedRecord, savedTxn)).thenReturn(ReservationBookingResponseDto.builder()
                        .bookingId(120L)
                        .reservationStatus("CONFIRMED")
                        .guestName("Alex Johnson")
                        .build());

                ReservationBookingResponseDto response = reservationBookingService.createBooking(request);

                assertThat(response.getBookingId()).isEqualTo(120L);
                verify(propertyInventoryPort).validateInventory(eq("PROP001"), eq("Deluxe King"), eq(1));
                verify(propertyInventoryPort).deductInventory(any());
                verify(propertyInventoryPort).syncInventory(any());
                verify(reservationBookingRepository).save(any(ReservationBookingRecord.class));
        }

        @Test
        void createBookingShouldFailWhenPropertyWizardValidationFailsAndFailOpenDisabled() {
                ReservationBookingRequestDto request = validRequest();
                when(propertyWizardServiceProperties.isEnabled()).thenReturn(true);
                when(propertyWizardServiceProperties.isFailOpenOnValidationError()).thenReturn(false);
                when(propertyInventoryPort.validateInventory(eq("PROP001"), eq("Deluxe King"), eq(1)))
                        .thenThrow(new ExternalServiceException("pw unavailable"));

                assertThatThrownBy(() -> reservationBookingService.createBooking(request))
                        .isInstanceOf(ExternalServiceException.class)
                        .hasMessageContaining("pw unavailable");

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
        void createBookingShouldProceedWhenInventoryWriteFailsAndFailOpenEnabled() {
                ReservationBookingRequestDto request = validRequest();
                when(propertyWizardServiceProperties.isEnabled()).thenReturn(true);
                when(propertyWizardServiceProperties.isFailOpenOnWriteError()).thenReturn(true);
                when(reservationBookingRepository.existsByConfirmationNumber(any())).thenReturn(false);
                when(propertyInventoryPort.validateInventory(eq("PROP001"), eq("Deluxe King"), eq(1)))
                        .thenReturn(validationResponse(true, true, 5));
                doThrow(new ExternalServiceException("deduct unavailable"))
                        .when(propertyInventoryPort).deductInventory(any());
                when(paymentProcessingService.processPayment(any(), any(), any())).thenReturn(successResult("PAY-350"));

                ReservationBookingRecord toSave = ReservationBookingRecord.builder()
                        .propertyId(request.getPropertyId())
                        .roomType(request.getRoomType())
                        .numberOfRooms(request.getNumberOfRooms())
                        .build();

                ReservationBookingRecord savedRecord = ReservationBookingRecord.builder()
                        .id(150L)
                        .propertyId("PROP001")
                        .confirmationNumber("1234567896")
                        .reservationStatus("CONFIRMED")
                        .guestName("Alex Johnson")
                        .build();

                ReservationPaymentTransactionRecord savedTxn = ReservationPaymentTransactionRecord.builder()
                        .id(750L)
                        .bookingId(150L)
                        .confirmationNumber("1234567896")
                        .propertyId("PROP001")
                        .paymentMode("CARD")
                        .amount(new BigDecimal("17000.00"))
                        .transactionStatus("SUCCESS")
                        .transactionReference("PAY-350")
                        .processorName("SIMULATED_GATEWAY")
                        .processedAt(LocalDateTime.of(2026, 7, 18, 12, 30))
                        .createdAt(LocalDateTime.of(2026, 7, 18, 12, 30))
                        .build();

                when(reservationBookingMapper.toEntity(any(ReservationBookingRequestDto.class))).thenReturn(toSave);
                when(reservationBookingRepository.save(any(ReservationBookingRecord.class))).thenReturn(savedRecord);
                when(reservationPaymentTransactionRepository.save(any(ReservationPaymentTransactionRecord.class))).thenReturn(savedTxn);
                when(reservationBookingMapper.toResponse(savedRecord, savedTxn)).thenReturn(ReservationBookingResponseDto.builder()
                        .bookingId(150L)
                        .reservationStatus("CONFIRMED")
                        .guestName("Alex Johnson")
                        .build());

                ReservationBookingResponseDto response = reservationBookingService.createBooking(request);

                assertThat(response.getBookingId()).isEqualTo(150L);
                verify(propertyInventoryPort).validateInventory(eq("PROP001"), eq("Deluxe King"), eq(1));
                verify(propertyInventoryPort).deductInventory(any());
                verify(propertyInventoryPort, never()).syncInventory(any());
                verify(reservationBookingRepository).save(any(ReservationBookingRecord.class));
        }

        @Test
        void createBookingShouldFailWhenInventoryWriteFailsAndFailOpenDisabled() {
                ReservationBookingRequestDto request = validRequest();
                when(propertyWizardServiceProperties.isEnabled()).thenReturn(true);
                when(propertyWizardServiceProperties.isFailOpenOnWriteError()).thenReturn(false);
                when(propertyInventoryPort.validateInventory(eq("PROP001"), eq("Deluxe King"), eq(1)))
                        .thenReturn(validationResponse(true, true, 5));
                doThrow(new ExternalServiceException("deduct unavailable"))
                        .when(propertyInventoryPort).deductInventory(any());
                when(paymentProcessingService.processPayment(any(), any(), any())).thenReturn(successResult("PAY-351"));

                assertThatThrownBy(() -> reservationBookingService.createBooking(request))
                        .isInstanceOf(ExternalServiceException.class)
                        .hasMessageContaining("deduct unavailable");

                verify(reservationBookingRepository, never()).save(any());
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
        request.setPaymentType("FULL_PAYMENT");
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
