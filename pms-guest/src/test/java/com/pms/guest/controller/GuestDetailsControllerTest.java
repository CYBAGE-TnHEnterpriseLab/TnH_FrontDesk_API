package com.pms.guest.controller;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pms.guest.entity.GuestProfile;
import com.pms.guest.exception.GuestProfileExceptionHandler;
import com.pms.guest.exception.ReservationServiceException;
import com.pms.guest.integration.ReservationServiceClient;
import com.pms.guest.integration.dto.ReservationGuestAssignment;
import com.pms.guest.repository.GuestProfileRepository;
import com.pms.guest.service.GuestProfileService;
import com.pms.guest.service.impl.GuestDetailsServiceImpl;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class GuestDetailsControllerTest {

    @Mock
    private GuestProfileRepository guestProfileRepository;

    @Mock
    private ReservationServiceClient reservationServiceClient;

    @Mock
    private GuestProfileService guestProfileService;

    private MockMvc mockMvc;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new GuestProfileController(
                        guestProfileService,
                        new GuestDetailsServiceImpl(guestProfileRepository, reservationServiceClient)))
                .setValidator(validator)
                .setControllerAdvice(new GuestProfileExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        validator.close();
    }

    @Test
    void confirmationSearchReturnsAssignmentStructure() throws Exception {
        when(reservationServiceClient.findAssignmentsByConfirmationNumber("PROP-A", "CONF-1001"))
                .thenReturn(List.of(
                        assignment(101L, 25L, true),
                        assignment(101L, 31L, false),
                        assignment(102L, 25L, true)));
        GuestProfile enrolled = profile(25L);
        enrolled.setLoyaltyMembershipNumber("TEMP-GUEST");
        enrolled.setLoyaltyTier("STANDARD");
        when(guestProfileRepository.findByPropertyIdAndIdIn(eq("PROP-A"), anyCollection()))
                .thenReturn(List.of(enrolled, profile(31L)));

        mockMvc.perform(get("/api/v1/guests/details")
                        .param("propertyId", "PROP-A")
                        .param("confirmationNumber", "CONF-1001"))
                .andExpect(status().isOk())
                        .andExpect(jsonPath("$.propertyId").value("PROP-A"))
                        .andExpect(jsonPath("$.phoneNumber").doesNotExist())
                        .andExpect(jsonPath("$.email").doesNotExist())
                        .andExpect(jsonPath("$.bookingId").doesNotExist())
                        .andExpect(jsonPath("$.confirmationNumber").doesNotExist())
                        .andExpect(jsonPath("$.guests.length()").value(3))
                .andExpect(jsonPath("$.guests[0].bookingId").value(101))
                .andExpect(jsonPath("$.guests[0].confirmationNumber").value("CONF-1001"))
                .andExpect(jsonPath("$.guests[0].guestProfileId").value(25))
                .andExpect(jsonPath("$.guests[0].isPrimary").value(true))
                .andExpect(jsonPath("$.guests[0].guest.guestId").value("GST-25"))
                .andExpect(jsonPath("$.guests[0].guest.idDocumentPath").value("uploads/id.png"))
                .andExpect(jsonPath("$.guests[0].guest.dateOfBirth").exists())
                .andExpect(jsonPath("$.guests[0].guest.createdAt").doesNotExist())
                .andExpect(jsonPath("$.guests[0].membership.enrolled").value(true))
                .andExpect(jsonPath("$.guests[0].membership.membershipNumber").value("TEMP-GUEST"))
                .andExpect(jsonPath("$.guests[0].membership.tier").value("STANDARD"))
                .andExpect(jsonPath("$.guests[1].guestProfileId").value(31))
                .andExpect(jsonPath("$.guests[1].isPrimary").value(false))
                .andExpect(jsonPath("$.guests[1].membership.enrolled").value(false))
                .andExpect(jsonPath("$.guests[1].membership.membershipNumber").doesNotExist())
                .andExpect(jsonPath("$.guests[2].bookingId").value(102))
                .andExpect(jsonPath("$.guests[2].guestProfileId").value(25));
    }

    @Test
    void missingPropertyIdReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/guests/details").param("phoneNumber", "555"))
                .andExpect(status().isBadRequest())
                ;
        verifyNoInteractions(guestProfileRepository, reservationServiceClient);
    }

    @Test
    void blankPropertyIdReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/guests/details").param("propertyId", " ").param("phoneNumber", "555"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(guestProfileRepository, reservationServiceClient);
    }

    @Test
    void noSearchCriterionReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/guests/details").param("propertyId", "PROP-A"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Exactly one of phoneNumber, email, bookingId or confirmationNumber is required"));
        verifyNoInteractions(guestProfileRepository, reservationServiceClient);
    }

    @Test
    void multipleSearchCriteriaReturn400() throws Exception {
        mockMvc.perform(get("/api/v1/guests/details")
                        .param("propertyId", "PROP-A")
                        .param("email", "a@example.com")
                        .param("confirmationNumber", "CONF-1"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(guestProfileRepository, reservationServiceClient);
    }

    @Test
    void nonNumericBookingIdReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/guests/details").param("propertyId", "PROP-A").param("bookingId", "abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validSearchWithoutMatchesReturns200WithEmptyGuests() throws Exception {
        when(guestProfileRepository.findByPropertyIdAndPersonalEmail("PROP-A", "none@example.com"))
                .thenReturn(List.of());
        when(guestProfileRepository.findByPropertyIdAndOfficialEmail("PROP-A", "none@example.com"))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/guests/details")
                        .param("propertyId", "PROP-A")
                        .param("email", "none@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.guests").isArray())
                .andExpect(jsonPath("$.guests.length()").value(0));
    }

    @Test
    void reservationServiceFailureReturns502() throws Exception {
        when(reservationServiceClient.findAssignmentsByBookingId("PROP-A", 101L))
                .thenThrow(new ReservationServiceException("Reservation service unavailable"));

        mockMvc.perform(get("/api/v1/guests/details").param("propertyId", "PROP-A").param("bookingId", "101"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.success").value(false));
    }

    private static ReservationGuestAssignment assignment(Long bookingId, Long guestProfileId, boolean primary) {
        return ReservationGuestAssignment.builder()
                .bookingId(bookingId)
                .confirmationNumber("CONF-1001")
                .guestProfileId(guestProfileId)
                .isPrimary(primary)
                .build();
    }

    private static GuestProfile profile(Long id) {
        return GuestProfile.builder()
                .id(id)
                .guestId("GST-" + id)
                .propertyId("PROP-A")
                .firstName("Guest" + id)
                .lastName("Test")
                .dateOfBirth(java.time.LocalDate.of(1990, 1, 2))
                .vipStatus(false)
                .idDocumentPath("uploads/id.png")
                .build();
    }
}
