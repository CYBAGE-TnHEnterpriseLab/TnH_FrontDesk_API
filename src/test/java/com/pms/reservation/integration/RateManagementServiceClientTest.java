package com.pms.reservation.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pms.guestlisting.exception.ExternalServiceException;
import com.pms.reservation.config.RateManagementServiceProperties;
import com.pms.reservation.integration.dto.RatePlanPricingQuoteDto;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class RateManagementServiceClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private RateManagementServiceProperties properties;

    @InjectMocks
    private RateManagementServiceClient client;

    @Test
    void fetchRateQuotesShouldUseAvailableAndCalculatedPriceApis() {
        configureProperties(1, 0);

        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)
        )).thenAnswer(invocation -> {
            String url = invocation.getArgument(0);

            if (url.contains("/available")) {
                return ResponseEntity.ok("""
                    {
                      "success": true,
                      "data": [
                        {
                          "id": 32,
                          "name": "DeluzeRP",
                          "code": "DLX",
                          "occupancyType": "2 Guest",
                          "mealOption": "BREAKFAST_AND_LUNCH",
                          "applicableRoomTypeIds": [28]
                        }
                      ]
                    }
                    """);
            }

            if (url.contains("/calculated-price") && url.contains("roomTypeId=28")) {
                return ResponseEntity.ok("""
                    {
                      "ratePlanId": 32,
                      "masterBarAmount": 10000,
                      "finalAmount": 9000
                    }
                    """);
            }

            throw new IllegalStateException("Unexpected URL: " + url);
        });

        var quotes = client.fetchRateQuotes(
            "PROP001",
            LocalDate.of(2026, 7, 14),
            LocalDate.of(2026, 7, 16),
            "Deluxe Room",
            28L,
            2,
            0
        );

        assertThat(quotes).hasSize(1);
        RatePlanPricingQuoteDto quote = quotes.get(0);
        assertThat(quote.getRatePlan()).isEqualTo("DeluzeRP");
        assertThat(quote.getRateCode()).isEqualTo("DLX");
        assertThat(quote.getRoomTypeId()).isEqualTo(28L);
        assertThat(quote.getRoomType()).isEqualTo("Deluxe Room");
        assertThat(quote.getBaseRate()).isEqualByComparingTo("9000");
        assertThat(quote.getFinalAmount()).isEqualByComparingTo("9000");

        verify(restTemplate, times(2)).exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)
        );
    }

    @Test
    void fetchRateQuotesShouldFallbackToListWhenAvailableGetReturns405() {
        configureProperties(1, 0);

        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)
        )).thenAnswer(invocation -> {
            String url = invocation.getArgument(0);

            if (url.contains("/available")) {
                throw HttpClientErrorException.create(
                    HttpStatus.METHOD_NOT_ALLOWED,
                    "Method Not Allowed",
                    HttpHeaders.EMPTY,
                    "get not supported".getBytes(StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8
                );
            }

            if (url.contains("/api/rate-plans/property/PROP001")
                && !url.contains("calculated-price")
                && !url.contains("available")) {
                return ResponseEntity.ok("""
                    [
                      {
                        "id": 32,
                        "name": "DeluzeRP",
                        "code": "DLX",
                        "status": "ACTIVE",
                        "startDate": "2026-07-01",
                        "endDate": "2026-07-31",
                        "applicableRoomTypeIds": [28]
                      }
                    ]
                    """);
            }

            if (url.contains("/calculated-price") && url.contains("roomTypeId=28")) {
                return ResponseEntity.ok("""
                    {
                      "ratePlanId": 32,
                      "masterBarAmount": 10000,
                      "finalAmount": 9000
                    }
                    """);
            }

            throw new IllegalStateException("Unexpected URL: " + url);
        });

        var quotes = client.fetchRateQuotes(
            "PROP001",
            LocalDate.of(2026, 7, 14),
            LocalDate.of(2026, 7, 16),
            "Deluxe Room",
            28L,
            2,
            0
        );

        assertThat(quotes).hasSize(1);
        RatePlanPricingQuoteDto quote = quotes.get(0);
        assertThat(quote.getRateCode()).isEqualTo("DLX");
        assertThat(quote.getFinalAmount()).isEqualByComparingTo("9000");
    }

    @Test
    void fetchRateQuotesShouldUseZeroFallbackWhenCalculatedPriceEndpointIsMissing() {
        configureProperties(1, 0);

        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)
        )).thenAnswer(invocation -> {
            String url = invocation.getArgument(0);

            if (url.contains("/available")) {
                throw HttpClientErrorException.create(
                    HttpStatus.METHOD_NOT_ALLOWED,
                    "Method Not Allowed",
                    HttpHeaders.EMPTY,
                    "get not supported".getBytes(StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8
                );
            }

            if (url.contains("/api/rate-plans/property/PROP001")
                && !url.contains("calculated-price")
                && !url.contains("available")) {
                return ResponseEntity.ok("""
                    [
                      {
                        "id": 32,
                        "name": "DeluzeRP",
                        "code": "DLX",
                        "status": "ACTIVE",
                        "startDate": "2026-07-01",
                        "endDate": "2026-07-31",
                        "manualAmount": 0,
                        "applicableRoomTypeIds": [28]
                      }
                    ]
                    """);
            }

            if (url.contains("/calculated-price") && url.contains("roomTypeId=28")) {
                throw HttpClientErrorException.create(
                    HttpStatus.NOT_FOUND,
                    "Not Found",
                    HttpHeaders.EMPTY,
                    "missing endpoint".getBytes(StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8
                );
            }

            if (url.contains("/api/master-rooms/32/pricing")) {
                return ResponseEntity.ok("[]");
            }

            throw new IllegalStateException("Unexpected URL: " + url);
        });

        var quotes = client.fetchRateQuotes(
            "PROP001",
            LocalDate.of(2026, 7, 14),
            LocalDate.of(2026, 7, 16),
            "Deluxe Room",
            28L,
            2,
            0
        );

        assertThat(quotes).hasSize(1);
        RatePlanPricingQuoteDto quote = quotes.get(0);
        assertThat(quote.getRateCode()).isEqualTo("DLX");
        assertThat(quote.getBaseRate()).isEqualByComparingTo("0");
        assertThat(quote.getFinalAmount()).isEqualByComparingTo("0");
    }

    @Test
    void fetchRateQuotesShouldUseMasterRoomPricingWhenCalculatedPriceEndpointIsMissing() {
        configureProperties(1, 0);

        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)
        )).thenAnswer(invocation -> {
            String url = invocation.getArgument(0);

            if (url.contains("/available")) {
                throw HttpClientErrorException.create(
                    HttpStatus.METHOD_NOT_ALLOWED,
                    "Method Not Allowed",
                    HttpHeaders.EMPTY,
                    "get not supported".getBytes(StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8
                );
            }

            if (url.contains("/api/rate-plans/property/PROP001")
                && !url.contains("calculated-price")
                && !url.contains("available")) {
                return ResponseEntity.ok("""
                    [
                      {
                        "id": 32,
                        "name": "DeluzeRP",
                        "code": "DLX",
                        "occupancyType": "2 Guest",
                        "status": "ACTIVE",
                        "startDate": "2026-07-01",
                        "endDate": "2026-07-31",
                        "manualAmount": 0,
                        "applicableRoomTypeIds": [28]
                      }
                    ]
                    """);
            }

            if (url.contains("/calculated-price") && url.contains("roomTypeId=28")) {
                throw HttpClientErrorException.create(
                    HttpStatus.NOT_FOUND,
                    "Not Found",
                    HttpHeaders.EMPTY,
                    "missing endpoint".getBytes(StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8
                );
            }

            if (url.contains("/api/master-rooms/32/pricing")) {
                return ResponseEntity.ok("""
                    [
                      {"occupancyType":"2_GUEST","price":800}
                    ]
                    """);
            }

            throw new IllegalStateException("Unexpected URL: " + url);
        });

        var quotes = client.fetchRateQuotes(
            "PROP001",
            LocalDate.of(2026, 7, 14),
            LocalDate.of(2026, 7, 16),
            "Deluxe Room",
            28L,
            2,
            0
        );

        assertThat(quotes).hasSize(1);
        RatePlanPricingQuoteDto quote = quotes.get(0);
        assertThat(quote.getRateCode()).isEqualTo("DLX");
        assertThat(quote.getBaseRate()).isEqualByComparingTo("800");
        assertThat(quote.getFinalAmount()).isEqualByComparingTo("800");
    }

    @Test
    void fetchRateQuotesShouldPreferPositiveMasterRoomOccupancyPriceWhenDuplicateOccupancyExists() {
        configureProperties(1, 0);

        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)
        )).thenAnswer(invocation -> {
            String url = invocation.getArgument(0);

            if (url.contains("/available")) {
                throw HttpClientErrorException.create(
                    HttpStatus.BAD_REQUEST,
                    "Bad Request",
                    HttpHeaders.EMPTY,
                    "available contract mismatch".getBytes(StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8
                );
            }

            if (url.contains("/api/rate-plans/property/PROP001")
                && !url.contains("calculated-price")
                && !url.contains("available")) {
                return ResponseEntity.ok("""
                    [
                      {
                        "id": 32,
                        "name": "DeluzeRP",
                        "code": "DLX",
                        "occupancyType": "2 Guest",
                        "status": "ACTIVE",
                        "startDate": "2026-07-01",
                        "endDate": "2026-07-31",
                        "manualAmount": 0,
                        "applicableRoomTypeIds": [28]
                      }
                    ]
                    """);
            }

            if (url.contains("/calculated-price") && url.contains("roomTypeId=28")) {
                throw HttpClientErrorException.create(
                    HttpStatus.NOT_FOUND,
                    "Not Found",
                    HttpHeaders.EMPTY,
                    "missing endpoint".getBytes(StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8
                );
            }

            if (url.contains("/api/master-rooms/32/pricing")) {
                return ResponseEntity.ok("""
                    [
                      {"occupancyType":"2 Guest","price":0},
                      {"occupancyType":"2_GUEST","price":800}
                    ]
                    """);
            }

            throw new IllegalStateException("Unexpected URL: " + url);
        });

        var quotes = client.fetchRateQuotes(
            "PROP001",
            LocalDate.of(2026, 7, 14),
            LocalDate.of(2026, 7, 16),
            "Deluxe Room",
            28L,
            2,
            0
        );

        assertThat(quotes).hasSize(1);
        RatePlanPricingQuoteDto quote = quotes.get(0);
        assertThat(quote.getRateCode()).isEqualTo("DLX");
        assertThat(quote.getBaseRate()).isEqualByComparingTo("800");
        assertThat(quote.getFinalAmount()).isEqualByComparingTo("800");
    }

    @Test
    void fetchRateQuotesShouldKeepHigherAmountWhenDuplicateRowsExistForSameSignature() {
        configureProperties(1, 0);

        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)
        )).thenAnswer(invocation -> {
            String url = invocation.getArgument(0);

            if (url.contains("/available")) {
                throw HttpClientErrorException.create(
                    HttpStatus.BAD_REQUEST,
                    "Bad Request",
                    HttpHeaders.EMPTY,
                    "available contract mismatch".getBytes(StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8
                );
            }

            if (url.contains("/api/rate-plans/property/PROP001")
                && !url.contains("calculated-price")
                && !url.contains("available")) {
                return ResponseEntity.ok("""
                    [
                      {
                        "id": 32,
                        "name": "DeluzeRP",
                        "code": "DLX",
                        "occupancyType": "2 Guest",
                        "status": "ACTIVE",
                        "startDate": "2026-07-01",
                        "endDate": "2026-07-31",
                        "manualAmount": 0,
                        "applicableRoomTypeIds": [28]
                      }
                    ]
                    """);
            }

            if (url.contains("/calculated-price") && url.contains("roomTypeId=28")) {
                throw HttpClientErrorException.create(
                    HttpStatus.NOT_FOUND,
                    "Not Found",
                    HttpHeaders.EMPTY,
                    "missing endpoint".getBytes(StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8
                );
            }

            if (url.contains("/api/master-rooms/32/pricing")) {
                return ResponseEntity.ok("""
                    [
                      {"occupancyType":"2 Guest","price":0},
                      {"occupancyType":"2_GUEST","price":1800}
                    ]
                    """);
            }

            throw new IllegalStateException("Unexpected URL: " + url);
        });

        var quotes = client.fetchRateQuotes(
            "PROP001",
            LocalDate.of(2026, 7, 15),
            LocalDate.of(2026, 7, 16),
            "Deluxe Room",
            null,
            2,
            0
        );

        assertThat(quotes).hasSize(1);
        RatePlanPricingQuoteDto quote = quotes.get(0);
        assertThat(quote.getFinalAmount()).isEqualByComparingTo("1800");
    }

    @Test
    void fetchRateQuotesShouldNotDisableAvailableEndpointAfterNullRoomType400() {
        configureProperties(1, 0);

        AtomicInteger nullRoomTypeAvailableCalls = new AtomicInteger(0);
        AtomicInteger typedRoomTypeAvailableCalls = new AtomicInteger(0);
        AtomicInteger listCalls = new AtomicInteger(0);

        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)
        )).thenAnswer(invocation -> {
            String url = invocation.getArgument(0);

            if (url.contains("/available") && !url.contains("roomTypeId=")) {
                nullRoomTypeAvailableCalls.incrementAndGet();
                throw HttpClientErrorException.create(
                    HttpStatus.BAD_REQUEST,
                    "Bad Request",
                    HttpHeaders.EMPTY,
                    "Required request parameter 'roomTypeId' for method parameter type Long is not present"
                        .getBytes(StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8
                );
            }

            if (url.contains("/available") && url.contains("roomTypeId=28")) {
                typedRoomTypeAvailableCalls.incrementAndGet();
                return ResponseEntity.ok("""
                    {
                      "success": true,
                      "data": [
                        {
                          "id": 32,
                          "name": "DeluzeRP",
                          "code": "DLX",
                          "occupancyType": "2 Guest",
                          "applicableRoomTypeIds": [28]
                        }
                      ]
                    }
                    """);
            }

            if (url.contains("/api/rate-plans/property/PROP001")
                && !url.contains("calculated-price")
                && !url.contains("available")) {
                listCalls.incrementAndGet();
                return ResponseEntity.ok("""
                    [
                      {
                        "id": 32,
                        "name": "DeluzeRP",
                        "code": "DLX",
                        "status": "ACTIVE",
                        "startDate": "2026-07-01",
                        "endDate": "2026-07-31",
                        "applicableRoomTypeIds": [28]
                      }
                    ]
                    """);
            }

            if (url.contains("/calculated-price") && url.contains("roomTypeId=28")) {
                return ResponseEntity.ok("""
                    {
                      "ratePlanId": 32,
                      "masterBarAmount": 10000,
                      "finalAmount": 1800
                    }
                    """);
            }

            throw new IllegalStateException("Unexpected URL: " + url);
        });

        var nullRoomTypeQuotes = client.fetchRateQuotes(
            "PROP001",
            LocalDate.of(2026, 7, 15),
            LocalDate.of(2026, 7, 16),
            "Deluxe Room",
            null,
            2,
            0
        );
        assertThat(nullRoomTypeQuotes).hasSize(1);
        assertThat(nullRoomTypeQuotes.get(0).getFinalAmount()).isEqualByComparingTo("1800");

        var typedRoomTypeQuotes = client.fetchRateQuotes(
            "PROP001",
            LocalDate.of(2026, 7, 15),
            LocalDate.of(2026, 7, 16),
            "Deluxe Room",
            28L,
            2,
            0
        );
        assertThat(typedRoomTypeQuotes).hasSize(1);
        assertThat(typedRoomTypeQuotes.get(0).getFinalAmount()).isEqualByComparingTo("1800");

        assertThat(nullRoomTypeAvailableCalls.get()).isEqualTo(1);
        assertThat(typedRoomTypeAvailableCalls.get()).isEqualTo(1);
        assertThat(listCalls.get()).isEqualTo(1);
    }

        @Test
        void fetchRateQuotesShouldPassNormalizedOccupancyTypeToCalculatedPrice() {
                configureProperties(1, 0);

                when(restTemplate.exchange(
                        anyString(),
                        eq(HttpMethod.GET),
                        any(HttpEntity.class),
                        eq(String.class)
                )).thenAnswer(invocation -> {
                        String url = invocation.getArgument(0);

                        if (url.contains("/available")) {
                                return ResponseEntity.ok("""
                                        {
                                            "success": true,
                                            "data": [
                                                {
                                                    "id": 32,
                                                    "name": "DeluzeRP",
                                                    "code": "DLX",
                                                    "occupancyType": "2 Guest",
                                                    "applicableRoomTypeIds": [28]
                                                }
                                            ]
                                        }
                                        """);
                        }

                        if (url.contains("/calculated-price") && url.contains("roomTypeId=28")) {
                            assertThat(url).containsPattern("occupancyType=1(\\+|%20| )Guest");
                                return ResponseEntity.ok("""
                                        {
                                            "ratePlanId": 32,
                                            "masterBarAmount": 10000,
                                            "finalAmount": 1500
                                        }
                                        """);
                        }

                        throw new IllegalStateException("Unexpected URL: " + url);
                });

                var quotes = client.fetchRateQuotes(
                        "PROP001",
                        LocalDate.of(2026, 7, 14),
                        LocalDate.of(2026, 7, 16),
                        "Deluxe Room",
                        28L,
                        1,
                        0
                );

                assertThat(quotes).hasSize(1);
                assertThat(quotes.get(0).getFinalAmount()).isEqualByComparingTo("1500");
        }

            @Test
            void fetchRateQuotesShouldFallbackToLegacyCalculatedPriceWhenOccupancyTypeVariantFails() {
                configureProperties(1, 0);

                when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
                )).thenAnswer(invocation -> {
                    String url = invocation.getArgument(0);

                    if (url.contains("/available")) {
                        return ResponseEntity.ok("""
                            {
                              "success": true,
                              "data": [
                                {
                                  "id": 32,
                                  "name": "DeluzeRP",
                                  "code": "DLX",
                                  "occupancyType": "2 Guest",
                                  "applicableRoomTypeIds": [28]
                                }
                              ]
                            }
                            """);
                    }

                    if (url.contains("/calculated-price")
                        && url.contains("roomTypeId=28")
                        && url.contains("occupancyType=1")) {
                        throw HttpClientErrorException.create(
                            HttpStatus.BAD_REQUEST,
                            "Bad Request",
                            HttpHeaders.EMPTY,
                            "invalid occupancyType".getBytes(StandardCharsets.UTF_8),
                            StandardCharsets.UTF_8
                        );
                    }

                    if (url.contains("/calculated-price") && url.contains("roomTypeId=28") && !url.contains("occupancyType=")) {
                        return ResponseEntity.ok("""
                            {
                              "ratePlanId": 32,
                              "masterBarAmount": 10000,
                              "finalAmount": 1800
                            }
                            """);
                    }

                    throw new IllegalStateException("Unexpected URL: " + url);
                });

                var quotes = client.fetchRateQuotes(
                    "PROP001",
                    LocalDate.of(2026, 7, 14),
                    LocalDate.of(2026, 7, 16),
                    "Deluxe Room",
                    28L,
                    1,
                    0
                );

                assertThat(quotes).hasSize(1);
                assertThat(quotes.get(0).getFinalAmount()).isEqualByComparingTo("1800");
            }

            @Test
            void fetchRateQuotesShouldFallbackToLegacyCalculatedPriceWhenOccupancyTypeVariantFailsWith5xx() {
                configureProperties(1, 0);

                when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
                )).thenAnswer(invocation -> {
                    String url = invocation.getArgument(0);

                    if (url.contains("/available")) {
                        return ResponseEntity.ok("""
                            {
                              "success": true,
                              "data": [
                                {
                                  "id": 43,
                                  "name": "Flexible Rate",
                                  "code": "FRP",
                                  "occupancyType": "2 Guest",
                                  "applicableRoomTypeIds": [27]
                                }
                              ]
                            }
                            """);
                    }

                    if (url.contains("/calculated-price")
                        && url.contains("roomTypeId=27")
                        && url.contains("occupancyType=4")) {
                        throw HttpServerErrorException.create(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "Internal Server Error",
                            HttpHeaders.EMPTY,
                            "InvalidRatePlanException: Requested occupancy type is not applicable for this rate plan"
                                .getBytes(StandardCharsets.UTF_8),
                            StandardCharsets.UTF_8
                        );
                    }

                    if (url.contains("/calculated-price") && url.contains("roomTypeId=27") && !url.contains("occupancyType=")) {
                        return ResponseEntity.ok("""
                            {
                              "ratePlanId": 43,
                              "masterBarAmount": 2000,
                              "finalAmount": 1900
                            }
                            """);
                    }

                    throw new IllegalStateException("Unexpected URL: " + url);
                });

                var quotes = client.fetchRateQuotes(
                    "PROP001",
                    LocalDate.of(2026, 7, 14),
                    LocalDate.of(2026, 7, 16),
                    "Deluxe Room",
                    27L,
                    4,
                    0
                );

                assertThat(quotes).hasSize(1);
                assertThat(quotes.get(0).getFinalAmount()).isEqualByComparingTo("1900");
            }

    @Test
    void getPricingByRoomTypeForRatePlanShouldCalculateForEachApplicableRoomType() {
        configureProperties(1, 0);

        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)
        )).thenAnswer(invocation -> {
            String url = invocation.getArgument(0);

            if (url.contains("/api/rate-plans/property/PROP001")
                && !url.contains("calculated-price")
                && !url.contains("available")) {
                return ResponseEntity.ok("""
                    [
                      {
                        "id": 50,
                        "name": "Corporate",
                        "code": "CORP",
                        "applicableRoomTypeIds": [27, 28]
                      }
                    ]
                    """);
            }

            if (url.contains("/50/calculated-price") && url.contains("roomTypeId=27")) {
                return ResponseEntity.ok("{" +
                    "\"ratePlanId\":50," +
                    "\"masterBarAmount\":10000," +
                    "\"finalAmount\":8700" +
                    "}");
            }

            if (url.contains("/50/calculated-price") && url.contains("roomTypeId=28")) {
                return ResponseEntity.ok("{" +
                    "\"ratePlanId\":50," +
                    "\"masterBarAmount\":12000," +
                    "\"finalAmount\":9900" +
                    "}");
            }

            throw new IllegalStateException("Unexpected URL: " + url);
        });

        Map<Long, BigDecimal> prices = client.getPricingByRoomTypeForRatePlan("PROP001", 50L);

        assertThat(prices).hasSize(2);
        assertThat(prices.get(27L)).isEqualByComparingTo("8700");
        assertThat(prices.get(28L)).isEqualByComparingTo("9900");
    }

    @Test
    void listRatePlansShouldMap401Unauthorized() {
        configureProperties(1, 0);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
            .thenThrow(HttpClientErrorException.create(
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                HttpHeaders.EMPTY,
                "invalid token".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
            ));

        assertThatThrownBy(() -> client.listRatePlans("PROP001"))
            .isInstanceOf(ExternalServiceException.class)
            .hasMessageContaining("401");
    }

    @Test
    void listRatePlansShouldMap403Forbidden() {
        configureProperties(1, 0);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
            .thenThrow(HttpClientErrorException.create(
                HttpStatus.FORBIDDEN,
                "Forbidden",
                HttpHeaders.EMPTY,
                "permission denied".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
            ));

        assertThatThrownBy(() -> client.listRatePlans("PROP001"))
            .isInstanceOf(ExternalServiceException.class)
            .hasMessageContaining("403");
    }

    @Test
    void listRatePlansShouldMap404NotFound() {
        configureProperties(1, 0);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
            .thenThrow(HttpClientErrorException.create(
                HttpStatus.NOT_FOUND,
                "Not Found",
                HttpHeaders.EMPTY,
                "missing endpoint".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
            ));

        assertThatThrownBy(() -> client.listRatePlans("PROP001"))
            .isInstanceOf(ExternalServiceException.class)
            .hasMessageContaining("404");
    }

    @Test
    void listRatePlansShouldMap5xxAndRetry() {
        configureProperties(3, 0);
        AtomicInteger attempts = new AtomicInteger(0);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
            .thenAnswer(invocation -> {
                attempts.incrementAndGet();
                throw HttpServerErrorException.create(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Service Unavailable",
                    HttpHeaders.EMPTY,
                    "downstream unavailable".getBytes(StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8
                );
            });

        assertThatThrownBy(() -> client.listRatePlans("PROP001"))
            .isInstanceOf(ExternalServiceException.class)
            .hasMessageContaining("5xx");

        assertThat(attempts.get()).isEqualTo(3);
    }

    private void configureProperties(int retryMaxAttempts, long retryBackoffMs) {
        lenient().when(properties.getBaseUrl()).thenReturn("http://localhost:8087");
        lenient().when(properties.getListRatePlansPath()).thenReturn("/api/rate-plans/property/{propertyId}");
        lenient().when(properties.getAvailablePlansPath()).thenReturn("/api/rate-plans/property/{propertyId}/available");
        lenient().when(properties.getCalculatedPricePath()).thenReturn("/api/rate-plans/property/{propertyId}/{ratePlanId}/calculated-price");
        lenient().when(properties.getRetryMaxAttempts()).thenReturn(retryMaxAttempts);
        lenient().when(properties.getRetryBackoffMs()).thenReturn(retryBackoffMs);
    }
}
