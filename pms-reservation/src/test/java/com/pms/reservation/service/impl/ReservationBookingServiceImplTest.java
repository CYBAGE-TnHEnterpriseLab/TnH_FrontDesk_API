package com.pms.reservation.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pms.housekeeping.repository.HousekeepingRoomStatusRepository;
import com.pms.reservation.config.PropertyWizardServiceProperties;
import com.pms.reservation.dto.PaymentProcessingResult;
import com.pms.reservation.dto.ReservationBookingRequestDto;
import com.pms.reservation.dto.ReservationGuestRequestDto;
import com.pms.reservation.entity.ReservationBookingRecord;
import com.pms.reservation.entity.ReservationPaymentTransactionRecord;
import com.pms.reservation.integration.HousekeepingRoomCalendarClient;
import com.pms.reservation.integration.HousekeepingRoomStatusClient;
import com.pms.reservation.integration.FolioServiceClient;
import com.pms.reservation.integration.InventoryServiceClient;
import com.pms.reservation.integration.PropertyInventoryPort;
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

    private ReservationBookingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReservationBookingServiceImpl(
                bookingRepository, paymentRepository, housekeepingRepository, propertyInventoryPort,
                inventoryServiceClient, new PropertyWizardServiceProperties(),
                new ReservationBookingMapper(), guestResolver, guestService, paymentProcessingService,
                housekeepingStatusClient, housekeepingCalendarClient, folioServiceClient);
    }

    @Test
    void singleRoomAssignsResolvedGuestsToSavedBookingId() {
        verifyCreateBooking(1, List.of("Ava Guest"), List.of(101L));
    }

    @Test
    void multipleRoomsResolveOnceAndAssignSameGuestsToEachSavedBookingId() {
        verifyCreateBooking(2, List.of("Ava Guest", "Bea Guest"), List.of(101L, 102L));
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
