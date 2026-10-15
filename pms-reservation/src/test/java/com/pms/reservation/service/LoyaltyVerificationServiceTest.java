package com.pms.reservation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

import com.pms.reservation.dto.LoyaltyVerificationResponse;
import com.pms.reservation.integration.GuestServiceClient;
import com.pms.reservation.integration.LoyaltyServiceClient;
import com.pms.reservation.integration.dto.GuestLookupRequest;
import com.pms.reservation.integration.dto.GuestProfileResponse;
import com.pms.reservation.integration.dto.LoyaltyMembershipResponse;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LoyaltyVerificationServiceTest {

    private static final String PROPERTY_ID = "7cfd4559-b6f3-4b7d-b933-e93018ac1d47";
    private static final String LOYALTY_NUMBER = "LOY00000042";

    @Mock
    private LoyaltyServiceClient loyaltyServiceClient;

    @Mock
    private GuestServiceClient guestServiceClient;

    @InjectMocks
    private LoyaltyVerificationService service;

    @Test
    void validLoyaltyNumberReturnsMembershipAndGuestProfile() {
        when(loyaltyServiceClient.findMembershipByLoyaltyNumber(LOYALTY_NUMBER))
                .thenReturn(membership(LOYALTY_NUMBER, "SILVER", "Silver", 1));
        when(guestServiceClient.findExistingGuest(ArgumentMatchers.any(GuestLookupRequest.class)))
                .thenReturn(Optional.of(guestProfile()));

        LoyaltyVerificationResponse response = service.verifyLoyaltyNumber(PROPERTY_ID, LOYALTY_NUMBER);

        assertThat(response.getValid()).isTrue();
        assertThat(response.getLoyaltyNumber()).isEqualTo(LOYALTY_NUMBER);
        assertThat(response.getTierCode()).isEqualTo("SILVER");
        assertThat(response.getTierName()).isEqualTo("Silver");
        assertThat(response.getTierLevel()).isEqualTo(1);
        assertThat(response.getMembershipStatus()).isEqualTo("ACTIVE");
        assertThat(response.getGuest().getFirstName()).isEqualTo("Ava");
        assertThat(response.getGuest().getLoyaltyMembershipNumber()).isEqualTo(LOYALTY_NUMBER);
    }

    @Test
    void loyaltyNumberNotKnownToLoyaltyServiceIsRejected() {
        when(loyaltyServiceClient.findMembershipByLoyaltyNumber(LOYALTY_NUMBER))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "unknown"));

        assertThatVerificationRejected();
        verifyNoInteractions(guestServiceClient);
    }

    @Test
    void loyaltyNumberWithoutGuestProfileIsRejected() {
        when(loyaltyServiceClient.findMembershipByLoyaltyNumber(LOYALTY_NUMBER))
                .thenReturn(membership(LOYALTY_NUMBER, "SILVER", "Silver", 1));
        when(guestServiceClient.findExistingGuest(ArgumentMatchers.any(GuestLookupRequest.class)))
                .thenReturn(Optional.empty());

        assertThatVerificationRejected();
    }

    @Test
    void missingLookupFieldsAreRejected() {
        assertThatThrownBy(() -> service.verifyLoyaltyNumber(" ", LOYALTY_NUMBER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("propertyId is required");
        assertThatThrownBy(() -> service.verifyLoyaltyNumber(PROPERTY_ID, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("loyaltyNumber is required");
    }

    private void assertThatVerificationRejected() {
        assertThatThrownBy(() -> service.verifyLoyaltyNumber(PROPERTY_ID, LOYALTY_NUMBER))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException statusException = (ResponseStatusException) ex;
                    assertThat(statusException.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(statusException.getReason())
                            .isEqualTo("loyalty number invalid and no guest profile found for the given loyalty number");
                });
    }

    private LoyaltyMembershipResponse membership(
            String loyaltyNumber,
            String tierCode,
            String tierName,
            Integer tierLevel
    ) {
        return LoyaltyMembershipResponse.builder()
                .membershipId(UUID.randomUUID())
                .guestId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .loyaltyNumber(loyaltyNumber)
                .tierCode(tierCode)
                .tierName(tierName)
                .tierLevel(tierLevel)
                .status("ACTIVE")
                .newlyEnrolled(false)
                .build();
    }

    private GuestProfileResponse guestProfile() {
        return GuestProfileResponse.builder()
                .id(42L)
                .guestId("GST-11111111-1111-1111-1111-111111111111")
                .propertyId(PROPERTY_ID)
                .firstName("Ava")
                .lastName("Guest")
                .loyaltyMembershipNumber(LOYALTY_NUMBER)
                .loyaltyTier("SILVER")
                .build();
    }
}
