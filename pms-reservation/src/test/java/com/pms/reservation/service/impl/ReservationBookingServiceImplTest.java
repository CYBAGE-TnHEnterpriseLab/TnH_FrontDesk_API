package com.pms.reservation.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

import com.pms.housekeeping.repository.HousekeepingRoomStatusRepository;
import com.pms.reservation.config.PropertyWizardServiceProperties;
import com.pms.reservation.dto.PaymentProcessingResult;
import com.pms.reservation.dto.ReservationBookingRequestDto;
import com.pms.reservation.dto.ReservationGuestRequestDto;
import com.pms.reservation.dto.ReservationViewResponseDto;
import com.pms.reservation.dto.RoomTypeOccupancyRateDto;
import com.pms.reservation.entity.ReservationBookingRecord;
import com.pms.reservation.entity.ReservationPaymentTransactionRecord;
import com.pms.reservation.integration.HousekeepingRoomCalendarClient;
import com.pms.reservation.integration.HousekeepingRoomStatusClient;
import com.pms.reservation.integration.FolioServiceClient;
import com.pms.reservation.integration.InventoryServiceClient;
import com.pms.reservation.integration.PropertyInventoryPort;
import com.pms.reservation.integration.RateManagementPort;
import com.pms.reservation.integration.dto.RatePlanPricingQuoteDto;
import com.pms.reservation.integration.dto.InventoryReservationRequest;
import com.pms.reservation.integration.dto.PropertyRoomOutletTypeDto;
import com.pms.reservation.mapper.ReservationBookingMapper;
import com.pms.reservation.repository.ReservationBookingRepository;
import com.pms.reservation.repository.ReservationPaymentTransactionRepository;
import com.pms.reservation.service.PaymentProcessingService;
import com.pms.reservation.service.ReservationGuestResolver;
import com.pms.reservation.service.ReservationGuestService;
import com.pms.reservation.service.ResolvedReservationGuest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReservationBookingServiceImplTest {

    private static final String PROPERTY_ID = "7cfd4559-b6f3-4b7d-b933-e93018ac1d47";

    @Mock private ReservationBookingRepository bookingRepository;
    @Mock private ReservationPaymentTransactionRepository paymentRepository;
    @Mock private HousekeepingRoomStatusRepository housekeepingRepository;
    @Mock private PropertyInventoryPort propertyInventoryPort;
    @Mock private InventoryServiceClient inventoryServiceClient;
    @Mock private ReservationGuestResolver guestResolver;
    @Mock private ReservationGuestService guestService;
    @Mock private PaymentProcessingService paymentProcessingService;
    @Mock private HousekeepingRoomStatusClient housekeepingStatusClient;
    @Mock private HousekeepingRoomCalendarClient housekeepingCalendarClient;
    @Mock private FolioServiceClient folioServiceClient;
    @Mock private RateManagementPort rateManagementPort;

    private ReservationBookingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReservationBookingServiceImpl(
                bookingRepository, paymentRepository, housekeepingRepository, propertyInventoryPort,
                inventoryServiceClient, new PropertyWizardServiceProperties(),
                new ReservationBookingMapper(), guestResolver, guestService, paymentProcessingService,
                housekeepingStatusClient, housekeepingCalendarClient, folioServiceClient, rateManagementPort);
    }

    @Test
    void singleRoomAssignsResolvedGuestsToSavedBookingId() {
        verifyCreateBooking(1, List.of("Ava Guest"), List.of(101L));
    }

    @Test
    void multipleRoomsResolveOnceAndAssignSameGuestsToEachSavedBookingId() {
        verifyCreateBooking(2, List.of("Ava Guest", "Bea Guest"), List.of(101L, 102L));
    }

    @Test
    void guestAssignmentFailureReleasesInventoryAndCompensatesPayment() {
        ReservationBookingRequestDto request = bookingRequest(1, List.of("Ava Guest"));
        List<ResolvedReservationGuest> resolvedGuests = List.of(
                new ResolvedReservationGuest(125L, true));
        when(guestResolver.resolveGuests(eq(PROPERTY_ID), same(request.getGuests())))
                .thenReturn(resolvedGuests);

        PropertyRoomOutletTypeDto roomType = new PropertyRoomOutletTypeDto();
        roomType.setId(1L);
        roomType.setRoomCode("DLX");
        when(propertyInventoryPort.fetchRoomOutletTypes(PROPERTY_ID)).thenReturn(List.of(roomType));

        PaymentProcessingResult paymentResult = PaymentProcessingResult.builder()
                .status("SUCCESS")
                .transactionReference("PAY-123")
                .build();
        when(paymentProcessingService.processPayment(same(request), any(String.class), any(BigDecimal.class)))
                .thenReturn(paymentResult);
        when(bookingRepository.save(any(ReservationBookingRecord.class))).thenAnswer(invocation -> {
            ReservationBookingRecord booking = invocation.getArgument(0);
            booking.setId(101L);
            return booking;
        });
        doThrow(new IllegalStateException("guest assignment failed"))
                .when(guestService).assignResolvedGuests(101L, resolvedGuests);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.createBooking(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("guest assignment failed");

        verify(inventoryServiceClient).release(any(String.class));
        verify(paymentProcessingService).compensatePayment(
                same(request),
                any(String.class),
                any(BigDecimal.class),
                same(paymentResult));
    }

    @Test
    void occupancyValidationRejectsAdultsAboveRoomTypeMaximum() {
        when(propertyInventoryPort.fetchRoomOutletTypes(PROPERTY_ID)).thenReturn(List.of(configuredRoomType()));

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        service.validateAdultOccupancyForRoomType(PROPERTY_ID, "DLX", 5))
                .isInstanceOf(com.pms.guestlisting.exception.BadRequestException.class)
                .hasMessage("The maximum occupancy for room type DLX is 4");
    }

    @Test
    void occupancyValidationRejectsAdultsBelowRoomTypeMinimum() {
        when(propertyInventoryPort.fetchRoomOutletTypes(PROPERTY_ID)).thenReturn(List.of(configuredRoomType()));

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        service.validateAdultOccupancyForRoomType(PROPERTY_ID, "DLX", 1))
                .isInstanceOf(com.pms.guestlisting.exception.BadRequestException.class)
                .hasMessage("The minimum occupancy for room type DLX is 2");
    }

    @Test
    void occupancyValidationAcceptsAdultsAtBoundaries() {
        when(propertyInventoryPort.fetchRoomOutletTypes(PROPERTY_ID)).thenReturn(List.of(configuredRoomType()));

        service.validateAdultOccupancyForRoomType(PROPERTY_ID, "dlx", 2);
        service.validateAdultOccupancyForRoomType(PROPERTY_ID, "DLX", 4);
    }

    @Test
    void roomRateQuoteReturnsCheapestRateManagementRateForEachOccupancy() {
        ReservationBookingRecord existing = new ReservationBookingRecord();
        existing.setId(101L);
        existing.setPropertyId(PROPERTY_ID);
        existing.setConfirmationNumber("1234567890");
        existing.setRoomType("DLX");
        existing.setChildCount(0);
        existing.setRateCode("LEGACY-RATE");
        when(bookingRepository.findByIdAndConfirmationNumber(101L, "1234567890"))
                .thenReturn(Optional.of(existing));
        when(propertyInventoryPort.fetchRoomOutletTypes(PROPERTY_ID)).thenReturn(List.of(configuredRoomType()));

        RatePlanPricingQuoteDto firstQuote = new RatePlanPricingQuoteDto();
        firstQuote.setRateCode("RATE-MGMT-SINGLE");
        firstQuote.setOccupancy("1 Guest");
        firstQuote.setFinalAmount(new BigDecimal("1000.00"));

        RatePlanPricingQuoteDto doubleGuestQuote = new RatePlanPricingQuoteDto();
        doubleGuestQuote.setRateCode("RATE-MGMT-PRIMARY");
        doubleGuestQuote.setOccupancy("2 Guest");
        doubleGuestQuote.setFinalAmount(new BigDecimal("1200.00"));

        RatePlanPricingQuoteDto savedRateCodeQuote = new RatePlanPricingQuoteDto();
        savedRateCodeQuote.setRateCode("LEGACY-RATE");
        savedRateCodeQuote.setOccupancy("2 Guest");
        savedRateCodeQuote.setFinalAmount(new BigDecimal("9999.00"));

        RatePlanPricingQuoteDto tripleGuestQuote = new RatePlanPricingQuoteDto();
        tripleGuestQuote.setRateCode("RATE-MGMT-TRIPLE");
        tripleGuestQuote.setOccupancy("3 Guest");
        tripleGuestQuote.setFinalAmount(new BigDecimal("1500.00"));

        RatePlanPricingQuoteDto quadGuestQuote = new RatePlanPricingQuoteDto();
        quadGuestQuote.setRateCode("RATE-MGMT-QUAD");
        quadGuestQuote.setOccupancy("4 Guest");
        quadGuestQuote.setFinalAmount(new BigDecimal("1800.00"));

        when(rateManagementPort.fetchRateQuotes(
                PROPERTY_ID,
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 2),
                "DLX",
                1L,
                1,
                0)).thenReturn(List.of(firstQuote));
        when(rateManagementPort.fetchRateQuotes(
                PROPERTY_ID,
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 2),
                "DLX",
                1L,
                2,
                0)).thenReturn(List.of(doubleGuestQuote, savedRateCodeQuote));
        when(rateManagementPort.fetchRateQuotes(
                PROPERTY_ID,
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 2),
                "DLX",
                1L,
                3,
                0)).thenReturn(List.of(tripleGuestQuote));
        when(rateManagementPort.fetchRateQuotes(
                PROPERTY_ID,
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 2),
                "DLX",
                1L,
                4,
                0)).thenReturn(List.of(quadGuestQuote));

        List<RoomTypeOccupancyRateDto> response = service.getRoomRateQuote(
                "1234567890",
                101L,
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 2),
                2);

        assertThat(response)
                .hasSize(4)
                .extracting(RoomTypeOccupancyRateDto::getGuestCount)
                .containsExactly(1, 2, 3, 4);
        assertThat(response)
                .extracting(RoomTypeOccupancyRateDto::getFinalAmount)
                .containsExactly(
                        new BigDecimal("1000.00"),
                        new BigDecimal("1200.00"),
                        new BigDecimal("1500.00"),
                        new BigDecimal("1800.00"));
        assertThat(response)
                .extracting(RoomTypeOccupancyRateDto::getRoomType)
                .containsOnly("DLX");
        verify(rateManagementPort).fetchRateQuotes(
                PROPERTY_ID,
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 2),
                "DLX",
                1L,
                1,
                0);
        verify(rateManagementPort).fetchRateQuotes(
                PROPERTY_ID,
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 2),
                "DLX",
                1L,
                2,
                0);
        verify(rateManagementPort).fetchRateQuotes(
                PROPERTY_ID,
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 2),
                "DLX",
                1L,
                3,
                0);
        verify(rateManagementPort).fetchRateQuotes(
                PROPERTY_ID,
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 2),
                "DLX",
                1L,
                4,
                0);
    }

    @Test
    void updateBookingRejectsAdultCountAboveRoomTypeMaximum() {
        ReservationBookingRecord existing = new ReservationBookingRecord();
        existing.setId(101L);
        existing.setPropertyId(PROPERTY_ID);
        existing.setConfirmationNumber("1234567890");
        existing.setRoomType("DLX");
        existing.setAdultCount(2);
        when(bookingRepository.findByConfirmationNumber("1234567890")).thenReturn(List.of(existing));
        when(propertyInventoryPort.fetchRoomOutletTypes(PROPERTY_ID)).thenReturn(List.of(configuredRoomType()));

        ReservationBookingRequestDto request = new ReservationBookingRequestDto();
        request.setAdultCount(6);

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        service.updateBooking("1234567890", request))
                .isInstanceOf(com.pms.guestlisting.exception.BadRequestException.class)
                .hasMessage("The maximum occupancy for room type DLX is 4");
    }

    @Test
    void updateBookingRefreshesRateFromRateManagementWhenAdultCountChanges() {
        ReservationBookingRecord existing = new ReservationBookingRecord();
        existing.setId(101L);
        existing.setPropertyId(PROPERTY_ID);
        existing.setConfirmationNumber("1234567890");
        existing.setReservationStatus("CONFIRMED");
        existing.setGuestName("Ava Guest");
        existing.setRoomType("DLX");
        existing.setRateCode("BAR");
        existing.setAdultCount(2);
        existing.setChildCount(0);
        existing.setArrivalDate(LocalDate.of(2026, 10, 1));
        existing.setDepartureDate(LocalDate.of(2026, 10, 3));
        existing.setNumberOfRooms(1);
        existing.setRate(new BigDecimal("1000.00"));
        existing.setTotalRate(new BigDecimal("2000.00"));
        existing.setGuestBalance(new BigDecimal("2000.00"));

        when(bookingRepository.findByConfirmationNumber("1234567890")).thenReturn(List.of(existing));
        when(bookingRepository.findByConfirmationNumberOrderByIdAsc("1234567890")).thenReturn(List.of(existing));
        when(propertyInventoryPort.fetchRoomOutletTypes(PROPERTY_ID)).thenReturn(List.of(configuredRoomType()));
        when(paymentRepository.findTopByBookingIdOrderByCreatedAtDesc(101L)).thenReturn(Optional.empty());
        when(folioServiceClient.getFolioBalance("1234567890", 101L)).thenReturn(Optional.empty());
        when(bookingRepository.save(any(ReservationBookingRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RatePlanPricingQuoteDto quote = new RatePlanPricingQuoteDto();
        quote.setRoomTypeId(1L);
        quote.setRoomType("DLX");
        quote.setRateCode("BAR");
        quote.setFinalAmount(new BigDecimal("1500.00"));
        when(rateManagementPort.fetchRateQuotes(
                PROPERTY_ID,
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 3),
                "DLX",
                1L,
                3,
                0)).thenReturn(List.of(quote));

        ReservationBookingRequestDto request = new ReservationBookingRequestDto();
        request.setAdultCount(3);

        ReservationViewResponseDto response = service.updateBooking("1234567890", request);

        assertThat(response.getStay().getAdults()).isEqualTo(3);
        assertThat(response.getPricing().getRoomRate()).isEqualByComparingTo("1500.00");
        assertThat(response.getPricing().getTotalRate()).isEqualByComparingTo("3000.00");

        verify(rateManagementPort).fetchRateQuotes(
                PROPERTY_ID,
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 3),
                "DLX",
                1L,
                3,
                0);
        verify(bookingRepository, never()).findByIdAndConfirmationNumber(101L, "1234567890");
    }

    private PropertyRoomOutletTypeDto configuredRoomType() {
        PropertyRoomOutletTypeDto roomType = new PropertyRoomOutletTypeDto();
        roomType.setId(1L);
        roomType.setRoomCode("DLX");
        roomType.setRoomName("DLX");
        roomType.setMinimumGuestOccupancy(2);
        roomType.setMaximumGuestOccupancy(4);
        return roomType;
    }

    private void verifyCreateBooking(int roomCount, List<String> roomNames, List<Long> savedIds) {
        ReservationBookingRequestDto request = bookingRequest(roomCount, roomNames);
        List<ResolvedReservationGuest> resolvedGuests = List.of(
                new ResolvedReservationGuest(125L, true),
                new ResolvedReservationGuest(126L, false));
        when(guestResolver.resolveGuests(eq(PROPERTY_ID), same(request.getGuests()))).thenReturn(resolvedGuests);

        PropertyRoomOutletTypeDto roomType = new PropertyRoomOutletTypeDto();
        roomType.setId(1L);
        roomType.setRoomCode("DLX");
        when(propertyInventoryPort.fetchRoomOutletTypes(PROPERTY_ID)).thenReturn(List.of(roomType));
        when(paymentProcessingService.processPayment(same(request), any(String.class), any(BigDecimal.class)))
                .thenReturn(PaymentProcessingResult.builder().status("SUCCESS").build());

        AtomicLong nextId = new AtomicLong(savedIds.get(0));
        when(bookingRepository.save(any(ReservationBookingRecord.class))).thenAnswer(invocation -> {
            ReservationBookingRecord booking = invocation.getArgument(0);
            booking.setId(nextId.getAndIncrement());
            return booking;
        });
        when(paymentRepository.save(any(ReservationPaymentTransactionRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.createBooking(request);

        verify(guestResolver, times(1)).resolveGuests(eq(PROPERTY_ID), same(request.getGuests()));
        ArgumentCaptor<ReservationBookingRecord> bookings = ArgumentCaptor.forClass(ReservationBookingRecord.class);
        verify(bookingRepository, times(roomCount)).save(bookings.capture());
        assertThat(bookings.getAllValues()).extracting(ReservationBookingRecord::getId)
                .containsExactlyElementsOf(savedIds);
        assertThat(bookings.getAllValues()).extracting(ReservationBookingRecord::getGuestName)
                .containsExactlyElementsOf(roomNames);
        assertThat(bookings.getAllValues()).extracting(ReservationBookingRecord::getConfirmationNumber)
                .containsOnly(response.getConfirmationNumber());

        InOrder order = inOrder(guestResolver, inventoryServiceClient, paymentProcessingService,
                bookingRepository, guestService, paymentRepository);
        order.verify(guestResolver).resolveGuests(eq(PROPERTY_ID), same(request.getGuests()));
        order.verify(inventoryServiceClient).reserve(any(InventoryReservationRequest.class));
        order.verify(paymentProcessingService).processPayment(same(request), any(String.class), any(BigDecimal.class));
        order.verify(bookingRepository, times(roomCount)).save(any(ReservationBookingRecord.class));
        ArgumentCaptor<Long> bookingIds = ArgumentCaptor.forClass(Long.class);
        verify(guestService, times(roomCount))
                .assignResolvedGuests(bookingIds.capture(), same(resolvedGuests));
        assertThat(bookingIds.getAllValues()).containsExactlyElementsOf(savedIds);
        for (Long id : savedIds) {
            assertThat(String.valueOf(id)).isNotEqualTo(response.getConfirmationNumber());
        }
        order.verify(guestService, times(roomCount))
                .assignResolvedGuests(any(Long.class), same(resolvedGuests));
        order.verify(paymentRepository).save(any(ReservationPaymentTransactionRecord.class));
        assertThat(response.getBookingId()).isEqualTo(savedIds.get(0));
        assertThat(response.getRoomBookings()).extracting(summary -> summary.getBookingId())
                .containsExactlyElementsOf(savedIds);
    }

    private ReservationBookingRequestDto bookingRequest(int roomCount, List<String> roomNames) {
        ReservationGuestRequestDto primary = new ReservationGuestRequestDto();
        primary.setGuestProfileId(125L);
        primary.setIsPrimary(true);
        ReservationGuestRequestDto additional = new ReservationGuestRequestDto();
        additional.setGuestProfileId(126L);
        additional.setIsPrimary(false);

        ReservationBookingRequestDto request = new ReservationBookingRequestDto();
        request.setPropertyId(PROPERTY_ID);
        request.setGuestName(roomNames.get(0));
        request.setGuestNames(roomNames);
        request.setGuests(List.of(primary, additional));
        request.setPersonalEmail("ava@example.com");
        request.setOfficialEmail("ava@work.example");
        request.setPhoneNumber("9876543210");
        request.setArrivalDate(LocalDate.of(2026, 10, 1));
        request.setDepartureDate(LocalDate.of(2026, 10, 2));
        request.setAdultCount(2);
        request.setChildCount(0);
        request.setRoomType("DLX");
        request.setRateCode("BAR");
        request.setNumberOfRooms(roomCount);
        request.setRate(new BigDecimal("1000"));
        request.setPayment("CARD");
        request.setPaymentType("FULL_PAYMENT");
        request.setEta(LocalTime.of(15, 0));
        request.setCheckOutTime(LocalTime.of(11, 0));
        return request;
    }
}
