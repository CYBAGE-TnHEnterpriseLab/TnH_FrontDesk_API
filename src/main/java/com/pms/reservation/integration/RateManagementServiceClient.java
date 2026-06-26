package com.pms.reservation.integration;

import com.pms.guestlisting.exception.ExternalServiceException;
import com.pms.reservation.config.RateManagementServiceProperties;
import com.pms.reservation.integration.dto.RatePlanPricingQuoteDto;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class RateManagementServiceClient implements RateManagementPort {

    private final RestTemplate restTemplate;
    private final RateManagementServiceProperties properties;

    @Override
    public List<RatePlanPricingQuoteDto> fetchRateQuotes(
            String propertyId,
            LocalDate arrivalDate,
            LocalDate departureDate,
            String roomType,
            Integer adultCount,
            Integer childCount
    ) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
                .path(properties.getAvailabilityPricingPath())
                .queryParam("propertyId", propertyId)
                .queryParam("arrivalDate", arrivalDate)
                .queryParam("departureDate", departureDate)
                .queryParam("adultCount", adultCount)
                .queryParam("childCount", childCount);

        if (StringUtils.hasText(roomType)) {
            builder.queryParam("roomType", roomType);
        }

        String url = builder.toUriString();

        try {
            ResponseEntity<List<RatePlanPricingQuoteDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );
            return response.getBody() == null ? Collections.emptyList() : response.getBody();
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Failed to fetch pricing from Rate Management service", ex);
        }
    }
}
