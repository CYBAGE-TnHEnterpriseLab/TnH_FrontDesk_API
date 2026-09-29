package com.pms.reservation.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.guestlisting.exception.ExternalServiceException;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@RequiredArgsConstructor
public class FolioServiceClient {

    @Value("${folio-service.base-url:http://localhost:8080}")
    private String baseUrl;

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public BigDecimal getFolioBalance(String confirmationNumber) {
        return getFolioBalance(confirmationNumber, null);
    }

    /**
     * Resolves the folio balance for a single room of a multi-room booking when {@code bookingId}
     * is supplied; otherwise the balance across every folio of the confirmation is returned.
     */
    public BigDecimal getFolioBalance(String confirmationNumber, Long bookingId) {
        if (confirmationNumber == null || confirmationNumber.isBlank()) {
            return BigDecimal.ZERO;
        }

        try {
            ResponseEntity<FolioServiceDto> response = bookingId == null
                    ? restTemplate.exchange(
                            baseUrl + "/api/v1/billingFolio/getFolioDetails?confirmationNumber={cn}",
                            HttpMethod.GET,
                            new HttpEntity<>(headers()),
                            FolioServiceDto.class,
                            confirmationNumber)
                    : restTemplate.exchange(
                            baseUrl + "/api/v1/billingFolio/getFolioDetails?confirmationNumber={cn}&bookingId={bid}",
                            HttpMethod.GET,
                            new HttpEntity<>(headers()),
                            FolioServiceDto.class,
                            confirmationNumber,
                            bookingId);

            FolioServiceDto folioResponse = response.getBody();
            if (folioResponse == null || folioResponse.summary() == null) {
                return BigDecimal.ZERO;
            }

            BigDecimal totalBalance = folioResponse.summary().totalBalance();
            return totalBalance != null ? totalBalance : BigDecimal.ZERO;
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode().value() == 404) {
                return BigDecimal.ZERO;
            }
            throw new ExternalServiceException(
                    "Failed to fetch folio balance for confirmationNumber=" + confirmationNumber
                            + " | status=" + ex.getStatusCode().value()
                            + " | body=" + ex.getResponseBodyAsString(), ex);
        } catch (RestClientException ex) {
            throw new ExternalServiceException(
                    "Failed to fetch folio balance for confirmationNumber=" + confirmationNumber
                            + " | error=" + ex.getClass().getName()
                            + " | message=" + ex.getMessage(), ex);
        }
    }

    public void adjustReservationCharge(String confirmationNumber, Long bookingId, BigDecimal originalAmount, BigDecimal newAmount, String userId) {
        if (confirmationNumber == null || confirmationNumber.isBlank()
                || originalAmount == null || newAmount == null
                || originalAmount.compareTo(newAmount) == 0) {
            return;
        }

        BigDecimal delta = newAmount.subtract(originalAmount);
        FolioChargeAdjustmentRequest.ChargeAdjustmentType adjustmentType = delta.compareTo(BigDecimal.ZERO) > 0
                ? FolioChargeAdjustmentRequest.ChargeAdjustmentType.INCREASE
                : FolioChargeAdjustmentRequest.ChargeAdjustmentType.DECREASE;
        BigDecimal adjustmentAmount = delta.abs();

        String originalReferenceNumber = "RESERVATION-" + confirmationNumber
                + (bookingId == null ? "" : "-" + bookingId);
        String reason = "Reservation totalRate updated to " + newAmount;

        FolioChargeAdjustmentRequest request = new FolioChargeAdjustmentRequest(
                confirmationNumber,
                originalReferenceNumber,
                adjustmentType,
                adjustmentAmount,
                reason,
                userId != null ? userId : "reservation-service",
                bookingId
        );

        try {
            restTemplate.exchange(
                    baseUrl + "/api/v1/billingFolio/adjustCharge",
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers()),
                    FolioChargeAdjustmentRequest.class
            );
        } catch (HttpStatusCodeException ex) {
            throw new ExternalServiceException(
                    "Failed to adjust folio reservation charge for confirmationNumber=" + confirmationNumber
                            + " | status=" + ex.getStatusCode().value()
                            + " | body=" + ex.getResponseBodyAsString(), ex);
        } catch (RestClientException ex) {
            throw new ExternalServiceException(
                    "Failed to adjust folio reservation charge for confirmationNumber=" + confirmationNumber
                            + " | error=" + ex.getClass().getName()
                            + " | message=" + ex.getMessage(), ex);
        }
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
            String authorization = servletRequestAttributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
            if (StringUtils.hasText(authorization)) {
                headers.set(HttpHeaders.AUTHORIZATION, authorization);
            }
        }
        return headers;
    }
}
