package com.pms.reservation.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.reservation.integration.dto.LoyaltyApiResponse;
import com.pms.reservation.integration.dto.LoyaltyMembershipResponse;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * pms-loyalty wraps every payload in an ApiResponse envelope. Reading the body straight into
 * LoyaltyMembershipResponse silently discards the membership and returns a null number and tier, so this
 * pins the shape the client has to unwrap.
 */
class LoyaltyApiResponseTest {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules();

    @Test
    void unwrapsLoyaltyMembershipFromTheEnvelope() throws Exception {
        UUID membershipId = UUID.fromString("230a5846-7a68-4edf-8092-c9445137640b");
        UUID guestId = UUID.fromString("1837e723-8bec-4c78-b42f-1840e2c2ccc7");
        String body = """
                {
                  "success": true,
                  "message": "Membership ensured successfully",
                  "data": {
                    "membershipId": "%s",
                    "guestId": "%s",
                    "loyaltyNumber": "LOY00000001",
                    "tierCode": "GOLD",
                    "tierName": "Gold",
                    "tierLevel": 2,
                    "status": "ACTIVE",
                    "newlyEnrolled": true
                  },
                  "errors": null,
                  "timestamp": "2026-10-11T14:14:17.2319518Z"
                }
                """.formatted(membershipId, guestId);

        LoyaltyApiResponse<LoyaltyMembershipResponse> response =
                MAPPER.readValue(body, MAPPER.getTypeFactory()
                        .constructParametricType(LoyaltyApiResponse.class, LoyaltyMembershipResponse.class));

        assertThat(response.isSuccess()).isTrue();
        LoyaltyMembershipResponse membership = response.getData();
        assertThat(membership).isNotNull();
        assertThat(membership.getLoyaltyNumber()).isEqualTo("LOY00000001");
        assertThat(membership.getTierCode()).isEqualTo("GOLD");
        assertThat(membership.getTierLevel()).isEqualTo(2);
        assertThat(membership.isNewlyEnrolled()).isTrue();
        assertThat(response.getTimestamp()).isEqualTo(OffsetDateTime.parse("2026-10-11T14:14:17.2319518Z"));
    }

    @Test
    void toleratesAnErrorEnvelopeWithoutData() throws Exception {
        String body = """
                {
                  "success": false,
                  "message": "Loyalty number not found",
                  "data": null,
                  "errors": null,
                  "timestamp": "2026-10-11T14:14:17.2319518Z"
                }
                """;

        LoyaltyApiResponse<LoyaltyMembershipResponse> response =
                MAPPER.readValue(body, MAPPER.getTypeFactory()
                        .constructParametricType(LoyaltyApiResponse.class, LoyaltyMembershipResponse.class));

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getData()).isNull();
        assertThat(response.getMessage()).isEqualTo("Loyalty number not found");
    }
}
