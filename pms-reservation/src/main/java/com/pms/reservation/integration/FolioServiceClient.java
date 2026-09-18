package com.pms.reservation.integration;

import com.pms.guestlisting.exception.ExternalServiceException;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class FolioServiceClient {

    @Value("${folio-service.base-url:http://localhost:8080}")
    private String baseUrl;

    public BigDecimal getFolioBalance(String confirmationNumber) {
        if (confirmationNumber == null || confirmationNumber.isBlank()) {
            return BigDecimal.ZERO;
        }

        try {
            FolioServiceDto folioResponse = WebClient.create(baseUrl)
                    .get()
                    .uri("/api/v1/billingFolio/getFolioDetails?confirmationNumber={cn}", confirmationNumber)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, response -> response.createException().flatMap(Mono::error))
                    .onStatus(HttpStatusCode::is5xxServerError, response -> response.createException().flatMap(Mono::error))
                    .bodyToMono(FolioServiceDto.class)
                    .block();

            if (folioResponse == null || folioResponse.summary() == null) {
                return BigDecimal.ZERO;
            }

            BigDecimal totalBalance = folioResponse.summary().totalBalance();
            return totalBalance != null ? totalBalance : BigDecimal.ZERO;
        } catch (WebClientResponseException.NotFound ex) {
            return BigDecimal.ZERO;
        } catch (WebClientResponseException ex) {
            throw new ExternalServiceException(
                    "Failed to fetch folio balance for confirmationNumber=" + confirmationNumber
                            + " | status=" + ex.getStatusCode().value()
                            + " | body=" + ex.getResponseBodyAsString(), ex);
        } catch (Exception ex) {
            throw new ExternalServiceException(
                    "Failed to fetch folio balance for confirmationNumber=" + confirmationNumber
                            + " | error=" + ex.getClass().getName()
                            + " | message=" + ex.getMessage(), ex);
        }
    }
}
