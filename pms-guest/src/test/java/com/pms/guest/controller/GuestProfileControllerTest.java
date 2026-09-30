package com.pms.guest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pms.guest.dto.request.GuestProfileCreateRequest;
import com.pms.guest.dto.request.GuestLookupRequest;
import com.pms.guest.dto.request.GuestProfileUpdateRequest;
import com.pms.guest.dto.response.GuestProfileResponse;
import com.pms.guest.exception.GuestProfileExceptionHandler;
import com.pms.guest.service.GuestProfileService;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class GuestProfileControllerTest {

    @Mock
    private GuestProfileService guestProfileService;

    private MockMvc mockMvc;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new GuestProfileController(guestProfileService))
                .setValidator(validator)
                .setControllerAdvice(new GuestProfileExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        validator.close();
    }

    @Test
    void createReturns201AndIgnoresClientSuppliedGuestId() throws Exception {
        when(guestProfileService.createGuestProfile(any(GuestProfileCreateRequest.class)))
                .thenReturn(GuestProfileResponse.builder()
                        .id(42L)
                        .guestId("GST-SERVER-GENERATED")
                        .propertyId("PROP-A")
                        .firstName("Ava")
                        .lastName("Guest")
                        .idDocumentPath("uploads/id.png")
                        .build());

        mockMvc.perform(post("/api/v1/guests")
                        .contentType("application/json")
                        .content("""
                                {
                                  "propertyId": "PROP-A",
                                  "firstName": "Ava",
                                  "lastName": "Guest",
                                  "guestId": "GST-CLIENT-CONTROLLED",
                                  "idDocumentPath": "uploads/id.png"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.guestId").value("GST-SERVER-GENERATED"))
                .andExpect(jsonPath("$.idDocumentPath").value("uploads/id.png"));

        ArgumentCaptor<GuestProfileCreateRequest> request =
                ArgumentCaptor.forClass(GuestProfileCreateRequest.class);
        verify(guestProfileService).createGuestProfile(request.capture());
        org.assertj.core.api.Assertions.assertThat(request.getValue().getPropertyId()).isEqualTo("PROP-A");
    }

    @Test
    void createRejectsMissingRequiredFieldsAndInvalidLoyaltyPair() throws Exception {
        mockMvc.perform(post("/api/v1/guests")
                        .contentType("application/json")
                        .content("""
                                {"firstName":"Ava"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        mockMvc.perform(post("/api/v1/guests")
                        .contentType("application/json")
                        .content("""
                                {
                                  "propertyId": "PROP-A",
                                  "firstName": "Ava",
                                  "lastName": "Guest",
                                  "loyaltyMembershipNumber": "LOYALTY-1"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createAcceptsReservationRestTemplateLocalDateArrayRepresentation() throws Exception {
        when(guestProfileService.createGuestProfile(any(GuestProfileCreateRequest.class)))
                .thenReturn(GuestProfileResponse.builder()
                        .id(42L)
                        .guestId("GST-SERVER-GENERATED")
                        .propertyId("PROP-A")
                        .firstName("Ava")
                        .lastName("Guest")
                        .dateOfBirth(LocalDate.of(1990, 1, 2))
                        .build());

        mockMvc.perform(post("/api/v1/guests")
                        .contentType("application/json")
                        .content("""
                                {
                                  "propertyId":"PROP-A",
                                  "firstName":"Ava",
                                  "lastName":"Guest",
                                  "dateOfBirth":[1990,1,2]
                                }
                                """))
                .andExpect(status().isCreated());

        ArgumentCaptor<GuestProfileCreateRequest> request =
                ArgumentCaptor.forClass(GuestProfileCreateRequest.class);
        verify(guestProfileService).createGuestProfile(request.capture());
        org.assertj.core.api.Assertions.assertThat(request.getValue().getDateOfBirth())
                .isEqualTo(LocalDate.of(1990, 1, 2));
    }

    @Test
    void getRequiresPropertyContextAndReturnsNotFoundForMissingProfile() throws Exception {
        mockMvc.perform(get("/api/v1/guests/42"))
                .andExpect(status().isBadRequest());
        when(guestProfileService.getGuestProfileById(42L, "PROP-A"))
                .thenThrow(new EntityNotFoundException("Guest profile not found: 42"));

        mockMvc.perform(get("/api/v1/guests/42").param("propertyId", "PROP-A"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Guest profile not found: 42"));
        verify(guestProfileService).getGuestProfileById(42L, "PROP-A");
    }

    @Test
    void lookupReturns404ForNoMatchAnd400WhenNoDeterministicIdentifierIsProvided() throws Exception {
        when(guestProfileService.findExistingGuest(any(GuestLookupRequest.class)))
                .thenReturn(java.util.Optional.empty());
        mockMvc.perform(post("/api/v1/guests/lookup")
                        .contentType("application/json")
                        .content("""
                                {"propertyId":"PROP-A","phoneNumber":"555-1000"}
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/guests/lookup")
                        .contentType("application/json")
                        .content("""
                                {"propertyId":"PROP-A","firstName":"Ava","lastName":"Guest"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void ambiguousLookupReturns409() throws Exception {
        when(guestProfileService.findExistingGuest(any(GuestLookupRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Multiple guest profiles match"));

        mockMvc.perform(post("/api/v1/guests/lookup")
                        .contentType("application/json")
                        .content("""
                                {"propertyId":"PROP-A","phoneNumber":"555-1000"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void updateRequiresAndPassesImmutablePropertyContext() throws Exception {
        when(guestProfileService.updateGuestProfile(
                eq(42L), eq("PROP-A"), any(GuestProfileUpdateRequest.class)))
                .thenReturn(GuestProfileResponse.builder()
                        .id(42L)
                        .guestId("GST-42")
                        .propertyId("PROP-A")
                        .firstName("Updated")
                        .lastName("Guest")
                        .build());

        mockMvc.perform(put("/api/v1/guests/42")
                        .param("propertyId", "PROP-A")
                        .contentType("application/json")
                        .content("""
                                {"firstName":"Updated","lastName":"Guest","id":99,"guestId":"GST-OTHER","propertyId":"PROP-B"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guestId").value("GST-42"))
                .andExpect(jsonPath("$.propertyId").value("PROP-A"));
        verify(guestProfileService).updateGuestProfile(
                eq(42L), eq("PROP-A"), any(GuestProfileUpdateRequest.class));
    }
}
