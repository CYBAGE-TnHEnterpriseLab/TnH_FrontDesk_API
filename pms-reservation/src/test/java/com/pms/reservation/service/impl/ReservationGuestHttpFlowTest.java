package com.pms.reservation.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.containsString;

import com.pms.housekeeping.repository.HousekeepingRoomStatusRepository;
import com.pms.reservation.config.GuestServiceProperties;
import com.pms.reservation.config.PropertyWizardServiceProperties;
import com.pms.reservation.dto.PaymentProcessingResult;
import com.pms.reservation.dto.ReservationBookingRequestDto;
import com.pms.reservation.dto.ReservationGuestRequestDto;
import com.pms.reservation.entity.ReservationBookingRecord;
import com.pms.reservation.entity.ReservationGuest;
import com.pms.reservation.entity.ReservationPaymentTransactionRecord;
import com.pms.reservation.integration.GuestServiceClient;
import com.pms.reservation.integration.FolioServiceClient;
import com.pms.reservation.integration.HousekeepingRoomCalendarClient;
import com.pms.reservation.integration.HousekeepingRoomStatusClient;
import com.pms.reservation.integration.InventoryServiceClient;
import com.pms.reservation.integration.PropertyInventoryPort;
import com.pms.reservation.integration.dto.PropertyRoomOutletTypeDto;
import com.pms.reservation.mapper.ReservationBookingMapper;
import com.pms.reservation.repository.ReservationBookingRepository;
import com.pms.reservation.repository.ReservationGuestRepository;
import com.pms.reservation.repository.ReservationPaymentTransactionRepository;
import com.pms.reservation.service.PaymentProcessingService;
import com.pms.reservation.service.ReservationGuestResolver;
import com.pms.reservation.service.ResolvedReservationGuest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class ReservationGuestHttpFlowTest {

    private static final String PROPERTY_ID = "7cfd4559-b6f3-4b7d-b933-e93018ac1d47";
    private static final String OTHER_PROPERTY_ID = "5bf6458d-758c-4e9a-8ce6-99fbfe793f35";
    private static final String GUEST_SERVICE_URL = "http://guest-service";

    @Mock private ReservationBookingRepository bookingRepository;
    @Mock private ReservationPaymentTransactionRepository paymentRepository;
    @Mock private HousekeepingRoomStatusRepository housekeepingRepository;
    @Mock private PropertyInventoryPort propertyInventoryPort;
    @Mock private InventoryServiceClient inventoryServiceClient;
    @Mock private ReservationGuestRepository reservationGuestRepository;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private PaymentProcessingService paymentProcessingService;
    @Mock private HousekeepingRoomStatusClient housekeepingStatusClient;
    @Mock private HousekeepingRoomCalendarClient housekeepingCalendarClient;
    @Mock private FolioServiceClient folioServiceClient;

    private MockRestServiceServer guestHttp;
    private ReservationBookingServiceImpl bookingService;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        guestHttp = MockRestServiceServer.bindTo(restTemplate).build();
        GuestServiceProperties guestProperties = new GuestServiceProperties();
        guestProperties.setBaseUrl(GUEST_SERVICE_URL);
        GuestServiceClient guestClient = new GuestServiceClient(restTemplate, guestProperties);
        ReservationGuestResolver resolver = new ReservationGuestResolver(guestClient);
        when(transactionManager.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(new SimpleTransactionStatus());
        doNothing().when(transactionManager).commit(any(TransactionStatus.class));

        ReservationGuestServiceImpl guestAssignmentService = new ReservationGuestServiceImpl(
                reservationGuestRepository,
                bookingRepository,
                guestClient,
                transactionManager);
        bookingService = new ReservationBookingServiceImpl(
                bookingRepository,
                paymentRepository,
                housekeepingRepository,
                propertyInventoryPort,
                inventoryServiceClient,
                new PropertyWizardServiceProperties(),
                new ReservationBookingMapper(),
                resolver,
                guestAssignmentService,
                paymentProcessingService,
                housekeepingStatusClient,
                housekeepingCalendarClient,
                folioServiceClient);

        PropertyRoomOutletTypeDto roomType = new PropertyRoomOutletTypeDto();
        roomType.setId(1L);
        roomType.setRoomCode("DLX");
        lenient().when(propertyInventoryPort.fetchRoomOutletTypes(PROPERTY_ID)).thenReturn(List.of(roomType));
        when(paymentProcessingService.processPayment(any(), any(String.class), any(BigDecimal.class)))
                .thenReturn(PaymentProcessingResult.builder().status("SUCCESS").build());

        AtomicLong nextBookingId = new AtomicLong(900L);
        when(bookingRepository.save(any(ReservationBookingRecord.class))).thenAnswer(invocation -> {
            ReservationBookingRecord booking = invocation.getArgument(0);
            booking.setId(nextBookingId.getAndIncrement());
            return booking;
        });
        when(bookingRepository.existsById(any(Long.class))).thenReturn(true);
        when(paymentRepository.save(any(ReservationPaymentTransactionRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void newGuestsAreResolvedOverHttpOnceAndAssignedToEachGeneratedRoomBookingId() {
        ReservationGuestRequestDto primary = newGuest("Ava", "5551000", true, false);
        primary.setPersonalEmail("ava@example.com");
        primary.setOfficialEmail("ava@work.example");
        primary.setMobileNumber("5551999");
        primary.setSalutation("Ms");
        primary.setAddress("1 Main Street");
        primary.setCity("New York");
        primary.setState("NY");
        primary.setCountry("USA");
        primary.setPostalCode("10001");
        primary.setNationality("American");
        primary.setDateOfBirth(LocalDate.of(1990, 1, 2));
        primary.setGender("Female");
        primary.setCompanyName("Example Inc.");
        primary.setVipStatus(true);
        primary.setIdType("PASSPORT");
        primary.setIdNumber("P1234567");
        primary.setIdDocumentPath("uploads/ava-id.png");
        ReservationGuestRequestDto secondary = newGuest("Bea", "5552000", false, false);
        ReservationBookingRequestDto request = bookingRequest(2, List.of("Ava Guest", "Bea Guest"), primary, secondary);

        expectNoGuestLookup("5551000", "Ava");
        expectNoGuestLookup("5552000", "Bea");
        expectCreate("Ava", "5551000", 701L, "GST-SERVER-701", false);
        expectCreate("Bea", "5552000", 702L, "GST-SERVER-702", false);

        var response = bookingService.createBooking(request);
        guestHttp.verify();

        assertThat(response.getRoomBookings()).extracting(room -> room.getBookingId())
                .containsExactly(900L, 901L);
        ArgumentCaptor<ReservationBookingRecord> bookings =
                ArgumentCaptor.forClass(ReservationBookingRecord.class);
        org.mockito.Mockito.verify(bookingRepository, org.mockito.Mockito.times(2)).save(bookings.capture());
        assertThat(bookings.getAllValues()).extracting(ReservationBookingRecord::getId)
                .containsExactly(900L, 901L);

        ArgumentCaptor<List<ReservationGuest>> assignments =
                ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(reservationGuestRepository, org.mockito.Mockito.times(2))
                .saveAllAndFlush(assignments.capture());
        List<ReservationGuest> savedAssignments = assignments.getAllValues().stream()
                .flatMap(List::stream)
                .toList();
        assertThat(savedAssignments).extracting(ReservationGuest::getBookingId)
                .containsExactly(900L, 900L, 901L, 901L);
        assertThat(savedAssignments).extracting(ReservationGuest::getGuestProfileId)
                .containsExactly(701L, 702L, 701L, 702L);
        assertThat(savedAssignments).extracting(ReservationGuest::getIsPrimary)
                .containsExactly(true, false, true, false);
        assertThat(savedAssignments).allSatisfy(assignment ->
                assertThat(assignment.getBookingId()).isNotEqualTo(Long.valueOf(response.getConfirmationNumber())));
    }

    @Test
    void newEnrolledGuestIsCreatedWithTemporaryMembershipWithoutFollowupUpdate() {
        ReservationGuestRequestDto guest = newGuest("Ava", "5551000", true, true);
        ReservationBookingRequestDto request = bookingRequest(1, List.of("Ava Guest"), guest);
        expectNoGuestLookup("5551000", "Ava");
        guestHttp.expect(requestTo(GUEST_SERVICE_URL + "/api/v1/guests"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath(
                        "$.loyaltyMembershipNumber").value("TEMP-GUEST"))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath(
                        "$.loyaltyTier").value("STANDARD"))
                .andRespond(withSuccess(profileJson(711L, "GST-SERVER-711", PROPERTY_ID, null, null),
                        org.springframework.http.MediaType.APPLICATION_JSON));

        bookingService.createBooking(request);
        guestHttp.verify();
        assertAssignments(List.of(711L), List.of(900L));
    }

    @Test
    void existingGuestLookupIsReusedWithoutCreatingOrUpdatingProfile() {
        ReservationGuestRequestDto guest = newGuest("Ava", "5551000", true, false);
        ReservationBookingRequestDto request = bookingRequest(1, List.of("Ava Guest"), guest);
        guestHttp.expect(requestTo(GUEST_SERVICE_URL + "/api/v1/guests/lookup"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath(
                        "$.propertyId").value(PROPERTY_ID))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath(
                        "$.phoneNumber").value("5551000"))
                .andRespond(withSuccess(profileJson(721L, "GST-EXISTING-721", PROPERTY_ID, null, null),
                        org.springframework.http.MediaType.APPLICATION_JSON));

        bookingService.createBooking(request);
        guestHttp.verify();
        assertAssignments(List.of(721L), List.of(900L));
    }

    @Test
    void suppliedInternalGuestIdIsRetrievedWithPropertyScopeAndReused() {
        ReservationGuestRequestDto guest = newGuest("Ava", "5551000", true, false);
        guest.setGuestProfileId(751L);
        ReservationBookingRequestDto request = bookingRequest(1, List.of("Ava Guest"), guest);
        guestHttp.expect(requestTo(
                        GUEST_SERVICE_URL + "/api/v1/guests/751?propertyId=" + PROPERTY_ID))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(profileJson(751L, "GST-EXISTING-751", PROPERTY_ID, null, null),
                        org.springframework.http.MediaType.APPLICATION_JSON));

        bookingService.createBooking(request);
        guestHttp.verify();
        assertAssignments(List.of(751L), List.of(900L));
    }

    @Test
    void existingGuestEnrollmentUpdatesAndReusesTheSameProfile() {
        ReservationGuestRequestDto guest = newGuest("Ava", "5551000", true, true);
        ReservationBookingRequestDto request = bookingRequest(1, List.of("Ava Guest"), guest);
        guestHttp.expect(requestTo(GUEST_SERVICE_URL + "/api/v1/guests/lookup"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(profileJson(731L, "GST-EXISTING-731", PROPERTY_ID, null, null),
                        org.springframework.http.MediaType.APPLICATION_JSON));
        guestHttp.expect(requestTo(
                        GUEST_SERVICE_URL + "/api/v1/guests/731?propertyId=" + PROPERTY_ID))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath(
                        "$.loyaltyMembershipNumber").value("TEMP-GUEST"))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath(
                        "$.loyaltyTier").value("STANDARD"))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath(
                        "$.firstName").value("Ava"))
                .andRespond(withSuccess(profileJson(731L, "GST-EXISTING-731", PROPERTY_ID,
                                "TEMP-GUEST", "STANDARD"),
                        org.springframework.http.MediaType.APPLICATION_JSON));

        bookingService.createBooking(request);
        guestHttp.verify();
        assertAssignments(List.of(731L), List.of(900L));
    }

    @Test
    void lookupForOtherPropertyDoesNotReuseExistingProfiles() {
        ReservationGuestRequestDto guest = newGuest("Ava", "5551000", true, false);
        ReservationBookingRequestDto request = bookingRequest(
                1, List.of("Ava Guest"), guest);
        request.setPropertyId(OTHER_PROPERTY_ID);
        PropertyRoomOutletTypeDto roomType = new PropertyRoomOutletTypeDto();
        roomType.setId(1L);
        roomType.setRoomCode("DLX");
        when(propertyInventoryPort.fetchRoomOutletTypes(OTHER_PROPERTY_ID)).thenReturn(List.of(roomType));
        guestHttp.expect(requestTo(GUEST_SERVICE_URL + "/api/v1/guests/lookup"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath(
                        "$.propertyId").value(OTHER_PROPERTY_ID))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        guestHttp.expect(requestTo(GUEST_SERVICE_URL + "/api/v1/guests"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath(
                        "$.propertyId").value(OTHER_PROPERTY_ID))
                .andRespond(withSuccess(profileJson(
                                741L, "GST-PROPERTY-B-741", OTHER_PROPERTY_ID, null, null),
                        org.springframework.http.MediaType.APPLICATION_JSON));

        bookingService.createBooking(request);
        guestHttp.verify();
        assertAssignments(List.of(741L), List.of(900L));
    }

    private void expectNoGuestLookup(String phone, String firstName) {
        guestHttp.expect(requestTo(GUEST_SERVICE_URL + "/api/v1/guests/lookup"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath(
                        "$.propertyId").value(PROPERTY_ID))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath(
                        "$.phoneNumber").value(phone))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath(
                        "$.firstName").value(firstName))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
    }

    private void expectCreate(
            String firstName,
            String phone,
            Long internalId,
            String guestId,
            boolean enrolled
    ) {
        var expectation = guestHttp.expect(requestTo(GUEST_SERVICE_URL + "/api/v1/guests"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath(
                        "$.propertyId").value(PROPERTY_ID))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath(
                        "$.firstName").value(firstName))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath(
                        "$.phoneNumber").value(phone));
        if ("Ava".equals(firstName)) {
            expectation.andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath(
                            "$.salutation").value("Ms"))
                    .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath(
                            "$.lastName").value("Guest"))
                    .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath(
                            "$.personalEmail").value("ava@example.com"))
                    .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath(
                            "$.officialEmail").value("ava@work.example"))
                    .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath(
                            "$.mobileNumber").value("5551999"))
                    .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath(
                            "$.address").value("1 Main Street"))
                    .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath(
                            "$.city").value("New York"))
                    .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath(
                            "$.state").value("NY"))
                    .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath(
                            "$.country").value("USA"))
                    .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath(
                            "$.postalCode").value("10001"))
                    .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath(
                            "$.nationality").value("American"))
                    .andExpect(content().string(containsString("\"dateOfBirth\":[1990,1,2]")))
                    .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath(
                            "$.gender").value("Female"))
                    .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath(
                            "$.companyName").value("Example Inc."))
                    .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath(
                            "$.vipStatus").value(true))
                    .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath(
                            "$.idType").value("PASSPORT"))
                    .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath(
                            "$.idNumber").value("P1234567"))
                    .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath(
                            "$.idDocumentPath").value("uploads/ava-id.png"));
        }
        if (enrolled) {
            expectation.andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath(
                            "$.loyaltyMembershipNumber").value("TEMP-GUEST"))
                    .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath(
                            "$.loyaltyTier").value("STANDARD"));
        } else {
            expectation.andExpect(content().string(containsString("\"loyaltyMembershipNumber\":null")))
                    .andExpect(content().string(containsString("\"loyaltyTier\":null")));
        }
        expectation.andRespond(withSuccess(profileJson(internalId, guestId, PROPERTY_ID,
                        enrolled ? "TEMP-GUEST" : null,
                        enrolled ? "STANDARD" : null),
                org.springframework.http.MediaType.APPLICATION_JSON));
    }

    private String profileJson(Long id, String guestId, String propertyId, String membership, String tier) {
        return """
                {
                  "id": %d,
                  "guestId": "%s",
                  "propertyId": "%s",
                  "salutation": "Ms",
                  "firstName": "Ava",
                  "lastName": "Guest",
                  "personalEmail": "ava@example.com",
                  "officialEmail": "ava@work.example",
                  "phoneNumber": "5551000",
                  "mobileNumber": "5552000",
                  "address": "1 Main Street",
                  "city": "New York",
                  "state": "NY",
                  "country": "USA",
                  "postalCode": "10001",
                  "nationality": "American",
                  "dateOfBirth": "1990-01-02",
                  "gender": "Female",
                  "companyName": "Example Inc.",
                  "vipStatus": false,
                  "idType": "PASSPORT",
                  "idNumber": "P1234567",
                  "idDocumentPath": "uploads/ava-id.png",
                  "loyaltyMembershipNumber": %s,
                  "loyaltyTier": %s
                }
                """.formatted(
                id,
                guestId,
                propertyId,
                membership == null ? "null" : "\"" + membership + "\"",
                tier == null ? "null" : "\"" + tier + "\"");
    }

    private void assertAssignments(List<Long> guestIds, List<Long> bookingIds) {
        ArgumentCaptor<List<ReservationGuest>> assignments = ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(reservationGuestRepository, org.mockito.Mockito.times(bookingIds.size()))
                .saveAllAndFlush(assignments.capture());
        List<ReservationGuest> saved = new ArrayList<>();
        assignments.getAllValues().forEach(saved::addAll);
        assertThat(saved).extracting(ReservationGuest::getGuestProfileId)
                .containsExactlyElementsOf(guestIds);
        assertThat(saved).extracting(ReservationGuest::getBookingId)
                .containsExactlyElementsOf(bookingIds);
        assertThat(saved).extracting(ReservationGuest::getIsPrimary).containsOnly(true);
    }

    private ReservationGuestRequestDto newGuest(
            String firstName,
            String phone,
            boolean primary,
            boolean enroll
    ) {
        ReservationGuestRequestDto guest = new ReservationGuestRequestDto();
        guest.setIsPrimary(primary);
        guest.setEnrollGuest(enroll);
        guest.setFirstName(firstName);
        guest.setLastName("Guest");
        guest.setPhoneNumber(phone);
        return guest;
    }

    private ReservationBookingRequestDto bookingRequest(
            int roomCount,
            List<String> roomNames,
            ReservationGuestRequestDto... guests
    ) {
        ReservationBookingRequestDto request = new ReservationBookingRequestDto();
        request.setPropertyId(PROPERTY_ID);
        request.setGuestName(roomNames.get(0));
        request.setGuestNames(roomNames);
        request.setGuests(List.of(guests));
        request.setPersonalEmail("reservation@example.com");
        request.setOfficialEmail("reservation@work.example");
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
