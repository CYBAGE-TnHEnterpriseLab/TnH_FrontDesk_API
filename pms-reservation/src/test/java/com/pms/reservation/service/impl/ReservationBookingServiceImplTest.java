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
import com.pms.housekeeping.entity.HousekeepingRoomStatusRecord;
import com.pms.housekeeping.repository.HousekeepingRoomStatusRepository;
import com.pms.reservation.config.PropertyWizardServiceProperties;
import com.pms.reservation.dto.PaymentProcessingResult;
import com.pms.reservation.dto.ReservationBookingRequestDto;
import com.pms.reservation.dto.ReservationBookingResponseDto;
import com.pms.reservation.dto.ReservationViewResponseDto;
import com.pms.reservation.entity.ReservationBookingRecord;
import com.pms.reservation.entity.ReservationPaymentTransactionRecord;
import com.pms.reservation.integration.PropertyInventoryPort;
import com.pms.reservation.integration.HousekeepingRoomStatusClient;
import com.pms.reservation.integration.dto.PropertyInventoryValidationResponse;
import com.pms.reservation.integration.dto.PropertyTaxRuleResponseDto;
import com.pms.reservation.mapper.ReservationBookingMapper;
import com.pms.reservation.repository.ReservationBookingRepository;
import com.pms.reservation.repository.ReservationPaymentTransactionRepository;
import com.pms.reservation.service.PaymentProcessingService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
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
        private HousekeepingRoomStatusRepository housekeepingRoomStatusRepository;

    @Mock
    private PropertyInventoryPort propertyInventoryPort;

    @Mock
    private HousekeepingRoomStatusClient housekeepingRoomStatusClient;

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
                .propertyId("7cfd4559-b6f3-4b7d-b933-e93018ac1d47")
                .confirmationNumber("1234567890")
                .reservationStatus("CONFIRMED")
                .guestName("Alex Johnson")
                .build();

        ReservationPaymentTransactionRecord savedTxn = ReservationPaymentTransactionRecord.builder()
                .id(501L)
                .bookingId(99L)
                .confirmationNumber("1234567890")
                .propertyId("7cfd4559-b6f3-4b7d-b933-e93018ac1d47")
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
        when(propertyInventoryPort.validateInventory(eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"), eq("Deluxe King"), eq(1)))
                .thenReturn(validationResponse(true, true, 5));
        when(paymentProcessingService.processPayment(any(), any(), any())).thenReturn(successResult("PAY-200"));

        ReservationBookingRecord toSave = ReservationBookingRecord.builder()
                .propertyId(request.getPropertyId())
                .roomType(request.getRoomType())
                .numberOfRooms(request.getNumberOfRooms())
                .arrivalDate(request.getArrivalDate())
                .departureDate(request.getDepartureDate())
                .rate(request.getRate())
                .build();

        PropertyTaxRuleResponseDto taxRule = new PropertyTaxRuleResponseDto();
        taxRule.setRoomType("Deluxe King");
        taxRule.setTaxPercentage(new BigDecimal("10"));
        taxRule.setActive(true);
        when(propertyInventoryPort.fetchTaxRules(eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"))).thenReturn(List.of(taxRule));

        ReservationBookingRecord savedRecord = ReservationBookingRecord.builder()
                .id(100L)
                .propertyId("7cfd4559-b6f3-4b7d-b933-e93018ac1d47")
                .confirmationNumber("1234567891")
                .reservationStatus("CONFIRMED")
                .guestName("Alex Johnson")
                .build();

        ReservationPaymentTransactionRecord savedTxn = ReservationPaymentTransactionRecord.builder()
                .id(601L)
                .bookingId(100L)
                .confirmationNumber("1234567891")
                .propertyId("7cfd4559-b6f3-4b7d-b933-e93018ac1d47")
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

        verify(propertyInventoryPort).validateInventory(eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"), eq("Deluxe King"), eq(1));
        verify(propertyInventoryPort).deductInventory(any());
        verify(propertyInventoryPort).syncInventory(any());
        verify(propertyInventoryPort).fetchTaxRules(eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"));

        ArgumentCaptor<ReservationBookingRecord> recordCaptor = ArgumentCaptor.forClass(ReservationBookingRecord.class);
        verify(reservationBookingRepository).save(recordCaptor.capture());
        ReservationBookingRecord persistedRecord = recordCaptor.getValue();
        assertThat(persistedRecord.getInventoryDeductedAt()).isNotNull();
        assertThat(persistedRecord.getInventorySyncedAt()).isNotNull();
        assertThat(persistedRecord.getTotalRate()).isEqualByComparingTo("18700.00");
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
                        .propertyId("7cfd4559-b6f3-4b7d-b933-e93018ac1d47")
                        .confirmationNumber("1234567892")
                        .reservationStatus("CONFIRMED")
                        .guestName("Alex Johnson")
                        .build();

                ReservationPaymentTransactionRecord savedTxn = ReservationPaymentTransactionRecord.builder()
                        .id(701L)
                        .bookingId(140L)
                        .confirmationNumber("1234567892")
                        .propertyId("7cfd4559-b6f3-4b7d-b933-e93018ac1d47")
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
                        .propertyId("7cfd4559-b6f3-4b7d-b933-e93018ac1d47")
                        .confirmationNumber("1234567893")
                        .reservationStatus("CONFIRMED")
                        .guestName("Alex Johnson")
                        .build();

                ReservationPaymentTransactionRecord savedTxn = ReservationPaymentTransactionRecord.builder()
                        .id(702L)
                        .bookingId(141L)
                        .confirmationNumber("1234567893")
                        .propertyId("7cfd4559-b6f3-4b7d-b933-e93018ac1d47")
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
                .propertyId("7cfd4559-b6f3-4b7d-b933-e93018ac1d47")
                .confirmationNumber("1234567894")
                .reservationStatus("CONFIRMED")
                .guestName("Alex Johnson")
                .build();

        ReservationPaymentTransactionRecord savedTxn = ReservationPaymentTransactionRecord.builder()
                .id(703L)
                .bookingId(142L)
                .confirmationNumber("1234567894")
                .propertyId("7cfd4559-b6f3-4b7d-b933-e93018ac1d47")
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
        void createBookingShouldRejectWhenPhoneNumberIsNotTenDigits() {
                ReservationBookingRequestDto request = validRequest();
                request.setPhoneNumber("3338)1(+47");

                assertThatThrownBy(() -> reservationBookingService.createBooking(request))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessage("phoneNumber must be exactly 10 digits");

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
        when(propertyInventoryPort.validateInventory(eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"), eq("Deluxe King"), eq(1)))
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
                when(propertyInventoryPort.validateInventory(eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"), eq("Deluxe King"), eq(1)))
                        .thenThrow(new ExternalServiceException("pw unavailable"));
                when(paymentProcessingService.processPayment(any(), any(), any())).thenReturn(successResult("PAY-299"));

                ReservationBookingRecord toSave = ReservationBookingRecord.builder()
                        .propertyId(request.getPropertyId())
                        .roomType(request.getRoomType())
                        .numberOfRooms(request.getNumberOfRooms())
                        .build();

                ReservationBookingRecord savedRecord = ReservationBookingRecord.builder()
                        .id(120L)
                        .propertyId("7cfd4559-b6f3-4b7d-b933-e93018ac1d47")
                        .confirmationNumber("1234567895")
                        .reservationStatus("CONFIRMED")
                        .guestName("Alex Johnson")
                        .build();

                ReservationPaymentTransactionRecord savedTxn = ReservationPaymentTransactionRecord.builder()
                        .id(699L)
                        .bookingId(120L)
                        .confirmationNumber("1234567895")
                        .propertyId("7cfd4559-b6f3-4b7d-b933-e93018ac1d47")
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
                verify(propertyInventoryPort).validateInventory(eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"), eq("Deluxe King"), eq(1));
                verify(propertyInventoryPort).deductInventory(any());
                verify(propertyInventoryPort).syncInventory(any());
                verify(reservationBookingRepository).save(any(ReservationBookingRecord.class));
        }

        @Test
        void createBookingShouldFailWhenPropertyWizardValidationFailsAndFailOpenDisabled() {
                ReservationBookingRequestDto request = validRequest();
                when(propertyWizardServiceProperties.isEnabled()).thenReturn(true);
                when(propertyWizardServiceProperties.isFailOpenOnValidationError()).thenReturn(false);
                when(propertyInventoryPort.validateInventory(eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"), eq("Deluxe King"), eq(1)))
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
        when(propertyInventoryPort.validateInventory(eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"), eq("Deluxe King"), eq(1)))
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
                when(propertyInventoryPort.validateInventory(eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"), eq("Deluxe King"), eq(1)))
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
                        .propertyId("7cfd4559-b6f3-4b7d-b933-e93018ac1d47")
                        .confirmationNumber("1234567896")
                        .reservationStatus("CONFIRMED")
                        .guestName("Alex Johnson")
                        .build();

                ReservationPaymentTransactionRecord savedTxn = ReservationPaymentTransactionRecord.builder()
                        .id(750L)
                        .bookingId(150L)
                        .confirmationNumber("1234567896")
                        .propertyId("7cfd4559-b6f3-4b7d-b933-e93018ac1d47")
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
                verify(propertyInventoryPort).validateInventory(eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"), eq("Deluxe King"), eq(1));
                verify(propertyInventoryPort).deductInventory(any());
                verify(propertyInventoryPort, never()).syncInventory(any());
                verify(reservationBookingRepository).save(any(ReservationBookingRecord.class));
        }

        @Test
        void createBookingShouldFailWhenInventoryWriteFailsAndFailOpenDisabled() {
                ReservationBookingRequestDto request = validRequest();
                when(propertyWizardServiceProperties.isEnabled()).thenReturn(true);
                when(propertyWizardServiceProperties.isFailOpenOnWriteError()).thenReturn(false);
                when(propertyInventoryPort.validateInventory(eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"), eq("Deluxe King"), eq(1)))
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
        when(propertyInventoryPort.validateInventory(eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"), eq("Deluxe King"), eq(3)))
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

    @Test
    void getBookingsShouldReturnMappedReservationsWithPaymentTransactionDetails() {
        ReservationBookingRecord latestBooking = ReservationBookingRecord.builder()
                .id(200L)
                .confirmationNumber("1234567899")
                .build();
        ReservationBookingRecord olderBooking = ReservationBookingRecord.builder()
                .id(199L)
                .confirmationNumber("1234567898")
                .build();

        ReservationPaymentTransactionRecord latestBookingTxn = ReservationPaymentTransactionRecord.builder()
                .bookingId(200L)
                .transactionReference("PAY-900")
                .build();

        ReservationBookingResponseDto latestResponse = ReservationBookingResponseDto.builder()
                .bookingId(200L)
                .confirmationNumber("1234567899")
                .build();
        ReservationBookingResponseDto olderResponse = ReservationBookingResponseDto.builder()
                .bookingId(199L)
                .confirmationNumber("1234567898")
                .build();

        when(reservationBookingRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(latestBooking, olderBooking));
        when(reservationPaymentTransactionRepository.findByBookingIdIn(List.of(200L, 199L)))
                .thenReturn(List.of(latestBookingTxn));
        when(reservationBookingMapper.toResponse(latestBooking, latestBookingTxn)).thenReturn(latestResponse);
        when(reservationBookingMapper.toResponse(olderBooking, null)).thenReturn(olderResponse);

        List<ReservationBookingResponseDto> response = reservationBookingService.getBookings();

        assertThat(response).containsExactly(latestResponse, olderResponse);
        verify(reservationPaymentTransactionRepository).findByBookingIdIn(List.of(200L, 199L));
    }

    @Test
    void getBookingsShouldReturnEmptyWhenNoReservationsExist() {
        when(reservationBookingRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        List<ReservationBookingResponseDto> response = reservationBookingService.getBookings();

        assertThat(response).isEmpty();
        verify(reservationPaymentTransactionRepository, never()).findByBookingIdIn(any());
    }

    @Test
    void getBookingDetailsShouldReturnStructuredViewPayload() {
        ReservationBookingRecord booking = ReservationBookingRecord.builder()
                .id(410L)
                .confirmationNumber("10256CNF569")
                .reservationStatus("CONFIRMED")
                .createdAt(LocalDateTime.of(2026, 6, 12, 10, 45))
                .propertyId("7cfd4559-b6f3-4b7d-b933-e93018ac1d47")
                .salutation("Mr")
                .guestName("Sachin Shah")
                .phoneNumber("+91 89562314785")
                .personalEmail("sachin@gmail.com")
                .officialEmail("sachin.office@gmail.com")
                .loyaltyNumber("3600")
                .arrivalDate(LocalDate.of(2026, 6, 23))
                .departureDate(LocalDate.of(2026, 6, 25))
                .numberOfRooms(2)
                .adultCount(1)
                .childCount(2)
                .eta(LocalTime.of(11, 40))
                .checkOutTime(LocalTime.of(21, 0))
                .assignedRoomNo("101")
                .roomType("King")
                .floor(1)
                .guestGroup("CYB-CYBAGE")
                .company("CYBAGE")
                .source("WALK IN")
                .reservationType("WALK IN")
                .rateCode("CYBAGE")
                .rate(new BigDecimal("1400"))
                .totalRate(new BigDecimal("1568"))
                .guestBalance(new BigDecimal("-1800"))
                .discount(new BigDecimal("2"))
                .specialRequests("AC, TV, Wifi, Extra Bed")
                .alertsMessages("Breakfast, Internet")
                .build();

        ReservationPaymentTransactionRecord latestTxn = ReservationPaymentTransactionRecord.builder()
                .bookingId(410L)
                .transactionReference("PAY-900")
                .build();

        ReservationBookingResponseDto mapped = ReservationBookingResponseDto.builder()
                .guestNames(List.of("Sachin Shah", "Pradip Agarwal", "Mohan Agarwal"))
                .build();

        HousekeepingRoomStatusRecord housekeepingStatus = HousekeepingRoomStatusRecord.builder()
                .roomStatus("Inspected")
                .build();

        when(reservationBookingRepository.findByConfirmationNumber("10256CNF569")).thenReturn(Optional.of(booking));
        when(reservationPaymentTransactionRepository.findTopByBookingIdOrderByCreatedAtDesc(410L))
                .thenReturn(Optional.of(latestTxn));
        when(reservationBookingMapper.toResponse(booking, latestTxn)).thenReturn(mapped);
        when(housekeepingRoomStatusRepository.findByPropertyIdAndBusinessDateAndConfirmationNumber(
                "7cfd4559-b6f3-4b7d-b933-e93018ac1d47",
                LocalDate.of(2026, 6, 23),
                "10256CNF569"
        )).thenReturn(Optional.of(housekeepingStatus));
        when(propertyWizardServiceProperties.isEnabled()).thenReturn(false);

        ReservationViewResponseDto response = reservationBookingService.getBookingDetails("10256CNF569");

        assertThat(response.getReservationId()).isEqualTo("10256CNF569");
        assertThat(response.getConfirmationNumber()).isEqualTo("10256CNF569");
        assertThat(response.getStatus()).isEqualTo("CONFIRMED");
        assertThat(response.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 6, 12, 10, 45).atOffset(ZoneOffset.UTC));
        assertThat(response.getGuest().getFirstName()).isEqualTo("Sachin");
        assertThat(response.getGuest().getLastName()).isEqualTo("Shah");
        assertThat(response.getAdditionalGuests()).hasSize(2);
        assertThat(response.getAdditionalGuests().get(0).getName()).isEqualTo("Pradip Agarwal");
        assertThat(response.getStay().getNights()).isEqualTo(2);
        assertThat(response.getStay().getCheckInTime()).isEqualTo("11:40");
        assertThat(response.getRoom().getRoomStatus()).isEqualTo("Inspected");
        assertThat(response.getPricing().getCurrency()).isEqualTo("INR");
        assertThat(response.getPricing().getTaxAmount()).isEqualByComparingTo("0");
        assertThat(response.getComments().getGuestRequests()).containsExactly("AC", "TV", "Wifi", "Extra Bed");
        assertThat(response.getActions().getCanEdit()).isTrue();
        assertThat(response.getActions().getCanCheckIn()).isTrue();
        assertThat(response.getActions().getCanCheckOut()).isFalse();
        assertThat(response.getActions().getCanCancel()).isFalse();
    }

    @Test
    void getBookingDetailsShouldFailWhenBookingDoesNotExist() {
                when(reservationBookingRepository.findByConfirmationNumber("MISSING-CNF")).thenReturn(Optional.empty());

                assertThatThrownBy(() -> reservationBookingService.getBookingDetails("MISSING-CNF"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Reservation booking not found");

        verify(reservationBookingMapper, never()).toResponse(any(), any());
    }

    @Test
    void updateBookingShouldPersistUpdatedDetailsAndPreserveSystemManagedFields() {
        ReservationBookingRequestDto request = validRequest();
        request.setCity("Mumbai");
                request.setPayment("UPI");
                request.setPaymentType("ADVANCE");

        ReservationBookingRecord existing = ReservationBookingRecord.builder()
                .id(300L)
                .confirmationNumber("1234567000")
                .reservationStatus("CONFIRMED")
                .payment("CARD")
                .paymentType("FULL_PAYMENT")
                .assignedRoomNo("1203")
                .floor(12)
                .checkInCompletedBy("frontdesk.agent")
                .checkInBusinessDate(LocalDate.of(2026, 7, 21))
                .checkInCompletedAt(LocalDateTime.of(2026, 7, 21, 14, 10))
                .inventoryDeductedAt(LocalDateTime.of(2026, 7, 18, 10, 0))
                .inventorySyncedAt(LocalDateTime.of(2026, 7, 18, 10, 1))
                .createdAt(LocalDateTime.of(2026, 7, 18, 9, 0))
                .build();

        ReservationBookingRecord mapped = ReservationBookingRecord.builder()
                .propertyId("7cfd4559-b6f3-4b7d-b933-e93018ac1d47")
                .roomType("Deluxe King")
                .rate(new BigDecimal("8500"))
                .numberOfRooms(1)
                .arrivalDate(LocalDate.of(2026, 7, 20))
                .departureDate(LocalDate.of(2026, 7, 22))
                .city("Mumbai")
                .build();

        ReservationBookingRecord saved = ReservationBookingRecord.builder()
                .id(300L)
                .propertyId("7cfd4559-b6f3-4b7d-b933-e93018ac1d47")
                .confirmationNumber("1234567000")
                .reservationStatus("CONFIRMED")
                .city("Mumbai")
                .createdAt(LocalDateTime.of(2026, 7, 18, 9, 0))
                .build();

        ReservationPaymentTransactionRecord latestTxn = ReservationPaymentTransactionRecord.builder()
                .bookingId(300L)
                .transactionReference("PAY-UPDATE-1")
                .build();

        ReservationBookingResponseDto mappedResponse = ReservationBookingResponseDto.builder()
                .guestNames(List.of("Alex Johnson"))
                .build();

        PropertyTaxRuleResponseDto taxRule = new PropertyTaxRuleResponseDto();
        taxRule.setRoomType("Deluxe King");
        taxRule.setTaxPercentage(new BigDecimal("10"));
        taxRule.setActive(true);

        when(propertyWizardServiceProperties.isEnabled()).thenReturn(true);
        when(propertyInventoryPort.validateInventory(eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"), eq("Deluxe King"), eq(1)))
                .thenReturn(validationResponse(true, true, 5));
        when(reservationBookingRepository.findByConfirmationNumber("1234567000")).thenReturn(Optional.of(existing));
        when(reservationBookingMapper.toEntity(request)).thenReturn(mapped);
        when(reservationBookingRepository.save(any(ReservationBookingRecord.class))).thenReturn(saved);
        when(reservationPaymentTransactionRepository.findTopByBookingIdOrderByCreatedAtDesc(300L))
                .thenReturn(Optional.of(latestTxn));
        when(reservationBookingMapper.toResponse(saved, latestTxn)).thenReturn(mappedResponse);
        when(propertyInventoryPort.fetchTaxRules(eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"))).thenReturn(List.of(taxRule));

        ReservationViewResponseDto response = reservationBookingService.updateBooking("1234567000", request);

        assertThat(response.getConfirmationNumber()).isEqualTo("1234567000");
        assertThat(response.getPropertyId()).isEqualTo("7cfd4559-b6f3-4b7d-b933-e93018ac1d47");
        assertThat(response.getStatus()).isEqualTo("CONFIRMED");

        ArgumentCaptor<ReservationBookingRecord> savedCaptor = ArgumentCaptor.forClass(ReservationBookingRecord.class);
        verify(reservationBookingRepository).save(savedCaptor.capture());
        ReservationBookingRecord persisted = savedCaptor.getValue();
        assertThat(persisted.getId()).isEqualTo(300L);
        assertThat(persisted.getConfirmationNumber()).isEqualTo("1234567000");
        assertThat(persisted.getReservationStatus()).isEqualTo("CONFIRMED");
        assertThat(persisted.getAssignedRoomNo()).isEqualTo("1203");
        assertThat(persisted.getFloor()).isEqualTo(12);
        assertThat(persisted.getCheckInCompletedBy()).isEqualTo("frontdesk.agent");
        assertThat(persisted.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 7, 18, 9, 0));
        assertThat(persisted.getTotalRate()).isEqualByComparingTo("18700.00");
                assertThat(persisted.getPayment()).isEqualTo("CARD");
                assertThat(persisted.getPaymentType()).isEqualTo("FULL_PAYMENT");
    }

    @Test
    void updateBookingShouldFailWhenBookingDoesNotExist() {
        ReservationBookingRequestDto request = validRequest();
                when(reservationBookingRepository.findByConfirmationNumber("MISSING-CNF")).thenReturn(Optional.empty());

                assertThatThrownBy(() -> reservationBookingService.updateBooking("MISSING-CNF", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Reservation booking not found");

        verify(reservationBookingRepository, never()).save(any());
        verify(reservationPaymentTransactionRepository, never()).findTopByBookingIdOrderByCreatedAtDesc(any());
    }

        @Test
        void updateBookingShouldRejectWhenPhoneNumberIsNotTenDigits() {
                ReservationBookingRequestDto request = validRequest();
                request.setPhoneNumber("+91-9876543210");

                assertThatThrownBy(() -> reservationBookingService.updateBooking("1234567000", request))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessage("phoneNumber must be exactly 10 digits");

                verify(reservationBookingRepository, never()).findByConfirmationNumber(any());
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
        request.setPropertyId("7cfd4559-b6f3-4b7d-b933-e93018ac1d47");
        request.setSalutation("Mr");
        request.setVipTag(Boolean.FALSE);
        request.setGuestName("Alex Johnson");
        request.setGuestNames(List.of("Alex Johnson"));
        request.setPersonalEmail("alex.personal@example.com");
        request.setOfficialEmail("alex.official@example.com");
        request.setCity("Pune");
        request.setCountry("India");
        request.setZipCode("411001");
        request.setPhoneNumber("9876543210");
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
