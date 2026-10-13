package com.pms.reservation.integration;

import com.pms.guestlisting.exception.ExternalServiceException;
import java.math.BigDecimal;
import java.util.Optional;
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
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class FolioServiceClient {

    @Value("${folio-service.base-url:http://localhost:8080}")
    private String baseUrl;

    private final RestTemplate restTemplate;

    /**
     * Fetches the outstanding balance for a confirmation. An empty result means the folio service
     * has no folio for that booking, which is distinct from an authoritative zero balance reported
     * for a real folio that the guest has fully settled.
     */
    public Optional<BigDecimal> getFolioBalance(String confirmationNumber) {
        return getFolioBalance(confirmationNumber, null);
    }

    public Optional<BigDecimal> getFolioBalance(String confirmationNumber, Long bookingId) {
        if (!StringUtils.hasText(confirmationNumber)) {
            return Optional.empty();
        }

        try {
                UriComponentsBuilder uriBuilder = UriComponentsBuilder
                    .fromHttpUrl(baseUrl + "/api/v1/billingFolio/getFolioDetails")
                    .queryParam("confirmationNumber", confirmationNumber);
                if (bookingId != null) {
                uriBuilder.queryParam("bookingId", bookingId);
                }

                ResponseEntity<FolioServiceDto> response = restTemplate.exchange(
                    uriBuilder.toUriString(),
                    HttpMethod.GET,
                    new HttpEntity<>(headers()),
                    FolioServiceDto.class
                );
                FolioServiceDto folioResponse = response.getBody();

            if (folioResponse == null || folioResponse.summary() == null || !hasFolio(folioResponse)) {
                return Optional.empty();
            }

            BigDecimal totalBalance = folioResponse.summary().totalBalance();
            return totalBalance == null ? Optional.empty() : Optional.of(totalBalance);
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode().value() == 404) {
                return Optional.empty();
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

    /**
     * A folio exists only when the folio service reports at least one folio record. A response with
     * none is a missing folio, not a settled one, and must never be read as an authoritative zero.
     */
    private boolean hasFolio(FolioServiceDto folioResponse) {
        return folioResponse.summary().totalFolios() > 0
                || (folioResponse.folios() != null && !folioResponse.folios().isEmpty());
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
