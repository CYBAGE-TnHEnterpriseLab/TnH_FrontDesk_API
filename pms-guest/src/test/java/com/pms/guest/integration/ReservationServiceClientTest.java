package com.pms.guest.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.pms.guest.config.ReservationServiceProperties;
import com.pms.guest.exception.ReservationServiceException;
import com.pms.guest.integration.dto.ReservationGuestAssignment;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class ReservationServiceClientTest {

    private MockRestServiceServer server;
    private ReservationServiceClient client;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        ReservationServiceProperties properties = new ReservationServiceProperties();
        properties.setBaseUrl("http://reservation.test");
        client = new ReservationServiceClient(restTemplate, properties);
    }

    @Test
    void confirmationLookupParsesReservationApiEnvelope() {
        server.expect(once(), requestTo(Matchers.startsWith("http://reservation.test/api/v1/reservation-guests/assignments")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("propertyId", "PROP-A"))
                .andExpect(queryParam("confirmationNumber", "CONF-1001"))
                .andRespond(withSuccess("""
                        {"success":true,"message":"ok","data":[
                          {"bookingId":101,"confirmationNumber":"CONF-1001","guestProfileId":25,"isPrimary":true},
                          {"bookingId":102,"confirmationNumber":"CONF-1001","guestProfileId":25,"isPrimary":true}
                        ],"errors":null,"timestamp":"2026-01-01T00:00:00Z"}
                        """, MediaType.APPLICATION_JSON));

        List<ReservationGuestAssignment> result =
                client.findAssignmentsByConfirmationNumber("PROP-A", "CONF-1001");

        assertThat(result).extracting(ReservationGuestAssignment::getBookingId).containsExactly(101L, 102L);
        assertThat(result).allSatisfy(a -> {
            assertThat(a.getGuestProfileId()).isEqualTo(25L);
            assertThat(a.getIsPrimary()).isTrue();
        });
        server.verify();
    }

    @Test
    void guestProfileIdLookupSendsAllIdsAndBookingLookupSendsBookingId() {
        server.expect(once(), requestTo(Matchers.containsString("guestProfileIds=25")))
                .andExpect(queryParam("guestProfileIds", "25", "42"))
                .andRespond(withSuccess("{\"success\":true,\"data\":[]}", MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(Matchers.containsString("bookingId=101")))
                .andExpect(queryParam("propertyId", "PROP-A"))
                .andRespond(withSuccess("{\"success\":true,\"data\":[]}", MediaType.APPLICATION_JSON));

        assertThat(client.findAssignmentsByGuestProfileIds("PROP-A", List.of(25L, 42L))).isEmpty();
        assertThat(client.findAssignmentsByBookingId("PROP-A", 101L)).isEmpty();
        server.verify();
    }

    @Test
    void emptyGuestProfileIdsSkipsHttpCall() {
        assertThat(client.findAssignmentsByGuestProfileIds("PROP-A", List.of())).isEmpty();
        server.verify();
    }

    @Test
    void serverErrorIsMappedToReservationServiceException() {
        server.expect(once(), requestTo(Matchers.containsString("bookingId=101")))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.findAssignmentsByBookingId("PROP-A", 101L))
                .isInstanceOf(ReservationServiceException.class)
                .hasMessageContaining("HTTP 500");
    }

    @Test
    void malformedEnvelopeIsRejected() {
        server.expect(once(), requestTo(Matchers.containsString("bookingId=101")))
                .andRespond(withSuccess("{\"success\":false,\"data\":null}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.findAssignmentsByBookingId("PROP-A", 101L))
                .isInstanceOf(ReservationServiceException.class)
                .hasMessageContaining("malformed");
    }
}
