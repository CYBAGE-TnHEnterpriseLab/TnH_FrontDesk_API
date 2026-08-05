package com.folio.billing.client.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.folio.billing.client.ReservationServiceClient;
import com.folio.billing.config.IntegrationProperties;
import com.folio.billing.dto.FolioBillingFilter;
import com.folio.billing.dto.FolioBillingRow;
import com.folio.billing.dto.GuestDetail;
import com.folio.billing.dto.ReservationSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnProperty(prefix = "integration.reservation-service", name = "enabled", havingValue = "true")
public class ReservationServiceHttpClient implements ReservationServiceClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationServiceHttpClient.class);
    private static final String PROPERTY_ID_HEADER = "X-Property-Id";

    private final IntegrationProperties.ServiceConfig reservationServiceConfig;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public ReservationServiceHttpClient(
            RestClient.Builder restClientBuilder,
            IntegrationProperties integrationProperties,
            ObjectMapper objectMapper
    ) {
        this.reservationServiceConfig = integrationProperties.getReservationService();
        this.restClient = restClientBuilder
                .baseUrl(normalizeBaseUrl(this.reservationServiceConfig.getBaseUrl()))
                .build();
        this.objectMapper = objectMapper;
    }

    @Override
    public List<FolioBillingRow> searchFolioBilling(FolioBillingFilter filter) {
        LocalDate businessDate = filter.checkInDate() != null ? filter.checkInDate() : LocalDate.now();
        String search = firstNonBlank(filter.confirmationNumber(), filter.guestName(), filter.roomNumber());

        List<JsonNode> rows = fetchGuestListingRows(
                businessDate,
                search,
                filter.actnerCrop()
        );

        return rows.stream()
                .map(this::toFolioBillingRow)
                .filter(row -> matches(filter.roomNumber(), row.room()))
                .filter(row -> matches(filter.guestName(), row.guest()))
                .filter(row -> matches(filter.confirmationNumber(), row.confirmationNo()))
                .filter(row -> matchesDate(filter.checkInDate(), row.checkIn()))
                .filter(row -> matchesDate(filter.checkOutDate(), row.checkOut()))
                .toList();
    }

    @Override
    public Optional<ReservationSummary> getReservationSummary(String confirmationNo, String roomNo, String guestName) {
        String search = firstNonBlank(confirmationNo, guestName, roomNo);
        List<JsonNode> rows = fetchGuestListingRows(LocalDate.now(), search, null);

        Optional<JsonNode> listingRow = rows.stream()
                .filter(row -> matches(confirmationNo, text(row, "confirmationNumber")))
                .filter(row -> matches(roomNo, text(row, "roomNo")))
                .filter(row -> matches(guestName, fullName(text(row, "firstName"), text(row, "lastName"))))
                .findFirst();

        if (listingRow.isEmpty()) {
            return Optional.empty();
        }

        JsonNode guestListingRow = listingRow.get();
        Optional<JsonNode> reservationDetails = fetchReservationDetailsByBookingId(guestListingRow.path("id").asLong(-1));

        return Optional.of(toReservationSummary(guestListingRow, reservationDetails.orElse(null)));
    }

    @Override
    public List<GuestDetail> getGuestDetails(String confirmationNo) {
        if (!StringUtils.hasText(confirmationNo)) {
            return List.of();
        }

        List<JsonNode> rows = fetchGuestListingRows(LocalDate.now(), confirmationNo, null);
        Optional<JsonNode> listingRow = rows.stream()
                .filter(row -> matches(confirmationNo, text(row, "confirmationNumber")))
                .findFirst();

        if (listingRow.isEmpty()) {
            return List.of();
        }

        JsonNode guestListingRow = listingRow.get();
        Optional<JsonNode> reservationDetails = fetchReservationDetailsByBookingId(guestListingRow.path("id").asLong(-1));

        if (reservationDetails.isEmpty()) {
            return List.of(toGuestDetailFromListing(guestListingRow));
        }

        return toGuestDetails(reservationDetails.get(), guestListingRow);
    }

    @Override
    public Optional<String> findDefaultConfirmationNo() {
        return fetchGuestListingRows(LocalDate.now(), null, null).stream()
                .map(row -> text(row, "confirmationNumber"))
                .filter(StringUtils::hasText)
                .findFirst();
    }

    private List<JsonNode> fetchGuestListingRows(LocalDate businessDate, String search, String company) {
        String propertyId = resolvePropertyId();
        if (!isGuestListingConfigurationValid(propertyId)) {
            return List.of();
        }

        try {
            String payload = restClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/api/v1/guest-listing/list")
                                .queryParam("propertyId", propertyId)
                                .queryParam("businessDate", businessDate)
                                .queryParam("view", "all")
                                .queryParam("page", 0);
                        if (StringUtils.hasText(search)) {
                            uriBuilder.queryParam("search", search.trim());
                        }
                        if (StringUtils.hasText(company)) {
                            uriBuilder.queryParam("company", company.trim());
                        }
                        return uriBuilder.build();
                    })
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(this::addInboundAuthorizationHeader)
                    .retrieve()
                    .body(String.class);

            return extractGuestListingRows(payload);
        } catch (RestClientResponseException ex) {
            LOGGER.warn("Reservation guest-listing API failed with status {}", ex.getStatusCode().value());
            return List.of();
        } catch (Exception ex) {
            LOGGER.warn("Reservation guest-listing API call failed: {}", ex.getMessage());
            return List.of();
        }
    }

    private Optional<JsonNode> fetchReservationDetailsByBookingId(long bookingId) {
        if (bookingId <= 0 || !isBaseUrlConfigured()) {
            return Optional.empty();
        }

        try {
            String payload = restClient.get()
                    .uri("/api/v1/reservations/bookings/{bookingId}", bookingId)
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(this::addInboundAuthorizationHeader)
                    .retrieve()
                    .body(String.class);

            if (!StringUtils.hasText(payload)) {
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(payload);
            JsonNode data = root.path("data");
            if (!data.isMissingNode() && !data.isNull()) {
                return Optional.of(data);
            }

            return Optional.of(root);
        } catch (RestClientResponseException ex) {
            LOGGER.warn("Reservation booking details API failed with status {} for bookingId {}", ex.getStatusCode().value(), bookingId);
            return Optional.empty();
        } catch (Exception ex) {
            LOGGER.warn("Reservation booking details API call failed for bookingId {}: {}", bookingId, ex.getMessage());
            return Optional.empty();
        }
    }

    private List<JsonNode> extractGuestListingRows(String payload) {
        if (!StringUtils.hasText(payload)) {
            return List.of();
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode data = root.path("data");

            JsonNode content = data.path("content");
            if (content.isArray()) {
                return toList(content);
            }

            if (data.isArray()) {
                return toList(data);
            }

            JsonNode fallbackContent = root.path("content");
            if (fallbackContent.isArray()) {
                return toList(fallbackContent);
            }

            return List.of();
        } catch (Exception ex) {
            LOGGER.warn("Unable to parse guest-listing payload: {}", ex.getMessage());
            return List.of();
        }
    }

    private List<JsonNode> toList(JsonNode arrayNode) {
        List<JsonNode> values = new ArrayList<>();
        arrayNode.forEach(values::add);
        return values;
    }

    private FolioBillingRow toFolioBillingRow(JsonNode node) {
        String firstName = text(node, "firstName");
        String lastName = text(node, "lastName");

        return new FolioBillingRow(
                text(node, "tier"),
                lastName,
                firstName,
                text(node, "roomNo"),
                fullName(firstName, lastName),
                firstNonBlank(text(node, "stayStatus"), text(node, "status")),
                parseDate(text(node, "checkInDate")),
                parseDate(text(node, "checkOutDate")),
                intValue(node, "nights", 0),
                text(node, "roomStatus"),
                text(node, "roomType"),
                text(node, "confirmationNumber")
        );
    }

    private ReservationSummary toReservationSummary(JsonNode guestListingRow, JsonNode reservationDetails) {
        JsonNode guest = reservationDetails != null ? reservationDetails.path("guest") : null;
        JsonNode stay = reservationDetails != null ? reservationDetails.path("stay") : null;
        JsonNode room = reservationDetails != null ? reservationDetails.path("room") : null;
        JsonNode booking = reservationDetails != null ? reservationDetails.path("booking") : null;
        JsonNode comments = reservationDetails != null ? reservationDetails.path("comments") : null;
        JsonNode additionalGuests = reservationDetails != null ? reservationDetails.path("additionalGuests") : null;

        String firstName = firstNonBlank(text(guest, "firstName"), text(guestListingRow, "firstName"));
        String lastName = firstNonBlank(text(guest, "lastName"), text(guestListingRow, "lastName"));
        String guestName = fullName(firstName, lastName);

        String guest2 = "";
        if (additionalGuests != null && additionalGuests.isArray() && !additionalGuests.isEmpty()) {
            guest2 = text(additionalGuests.get(0), "name");
        }

        LocalDate checkInDate = firstNonNullDate(
                parseDate(text(stay, "checkInDate")),
                parseDate(text(guestListingRow, "checkInDate"))
        );
        LocalDate checkOutDate = firstNonNullDate(
                parseDate(text(stay, "checkOutDate")),
                parseDate(text(guestListingRow, "checkOutDate"))
        );

        String commentsText = firstNonBlank(
                text(comments, "billingComments"),
                joinTextArray(comments != null ? comments.path("guestRequests") : null)
        );

        return new ReservationSummary(
                guestName,
                guestName,
                guest2,
                firstNonBlank(text(reservationDetails, "confirmationNumber"), text(guestListingRow, "confirmationNumber")),
                intValue(stay, "adults", 0),
                intValue(stay, "children", 0),
                firstNonBlank(text(booking, "company"), text(guestListingRow, "company")),
                firstNonBlank(text(booking, "source"), text(guestListingRow, "reservationType")),
                firstNonBlank(text(guestListingRow, "ratePlan"), text(booking, "rateCode")),
                firstNonBlank(text(reservationDetails, "status"), text(guestListingRow, "status")),
                firstNonBlank(text(guestListingRow, "stayStatus"), "Open"),
                firstNonBlank(text(room, "roomNo"), text(guestListingRow, "roomNo")),
                firstNonBlank(text(room, "roomType"), text(guestListingRow, "roomType")),
                checkInDate,
                checkOutDate,
                intValue(stay, "nights", intValue(guestListingRow, "nights", 0)),
                commentsText
        );
    }

    private List<GuestDetail> toGuestDetails(JsonNode reservationDetails, JsonNode guestListingRow) {
        List<GuestDetail> guestDetails = new ArrayList<>();

        JsonNode guest = reservationDetails.path("guest");
        String primaryName = fullName(text(guest, "firstName"), text(guest, "lastName"));
        if (!StringUtils.hasText(primaryName)) {
            primaryName = fullName(text(guestListingRow, "firstName"), text(guestListingRow, "lastName"));
        }

        if (StringUtils.hasText(primaryName)) {
            guestDetails.add(new GuestDetail(
                    primaryName,
                    0,
                    text(guest, "phoneNumber"),
                    text(guest, "email"),
                    "",
                    BigDecimal.ZERO
            ));
        }

        JsonNode additionalGuests = reservationDetails.path("additionalGuests");
        if (additionalGuests.isArray()) {
            additionalGuests.forEach(item -> guestDetails.add(new GuestDetail(
                    text(item, "name"),
                    0,
                    text(item, "phoneNumber"),
                    text(item, "email"),
                    "",
                    BigDecimal.ZERO
            )));
        }

        if (guestDetails.isEmpty()) {
            guestDetails.add(toGuestDetailFromListing(guestListingRow));
        }

        return guestDetails;
    }

    private GuestDetail toGuestDetailFromListing(JsonNode guestListingRow) {
        return new GuestDetail(
                fullName(text(guestListingRow, "firstName"), text(guestListingRow, "lastName")),
                0,
                "",
                "",
            "",
            BigDecimal.ZERO
        );
    }

    private void addInboundAuthorizationHeader(HttpHeaders headers) {
        String authorization = resolveAuthorizationHeader();
        if (StringUtils.hasText(authorization)) {
            headers.set(HttpHeaders.AUTHORIZATION, authorization);
        }
    }

    private String resolvePropertyId() {
        String headerPropertyId = resolveInboundHeader(PROPERTY_ID_HEADER);
        return firstNonBlank(headerPropertyId, reservationServiceConfig.getPropertyId());
    }

    private String resolveAuthorizationHeader() {
        return resolveInboundHeader(HttpHeaders.AUTHORIZATION);
    }

    private String resolveInboundHeader(String headerName) {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes requestAttributes)) {
            return null;
        }
        return requestAttributes.getRequest().getHeader(headerName);
    }

    private boolean matches(String expected, String actual) {
        return !StringUtils.hasText(expected) || containsIgnoreCase(actual, expected);
    }

    private boolean containsIgnoreCase(String actualValue, String expectedValue) {
        if (!StringUtils.hasText(actualValue) || !StringUtils.hasText(expectedValue)) {
            return false;
        }
        return actualValue.toLowerCase().contains(expectedValue.trim().toLowerCase());
    }

    private boolean matchesDate(LocalDate expected, LocalDate actual) {
        return expected == null || expected.equals(actual);
    }

    private String text(JsonNode node, String field) {
        if (node == null) {
            return "";
        }

        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return "";
        }

        String output = value.asText("");
        return output == null ? "" : output.trim();
    }

    private int intValue(JsonNode node, String field, int defaultValue) {
        if (node == null) {
            return defaultValue;
        }
        JsonNode value = node.path(field);
        return value.isNumber() ? value.intValue() : defaultValue;
    }

    private LocalDate parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private LocalDate firstNonNullDate(LocalDate first, LocalDate second) {
        return first != null ? first : second;
    }

    private String fullName(String firstName, String lastName) {
        String first = firstName == null ? "" : firstName.trim();
        String last = lastName == null ? "" : lastName.trim();
        String full = (first + " " + last).trim();
        return full.isEmpty() ? firstNonBlank(first, last) : full;
    }

    private String joinTextArray(JsonNode values) {
        if (values == null || !values.isArray() || values.isEmpty()) {
            return "";
        }

        List<String> parts = new ArrayList<>();
        values.forEach(item -> {
            String value = item.asText("").trim();
            if (!value.isEmpty()) {
                parts.add(value);
            }
        });

        return String.join(", ", parts);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private boolean isGuestListingConfigurationValid(String propertyId) {
        if (!isBaseUrlConfigured()) {
            return false;
        }
        if (!StringUtils.hasText(propertyId)) {
            LOGGER.warn("Reservation propertyId is empty. Provide X-Property-Id header or set integration.reservation-service.property-id.");
            return false;
        }
        return true;
    }

    private boolean isBaseUrlConfigured() {
        if (!StringUtils.hasText(reservationServiceConfig.getBaseUrl())) {
            LOGGER.warn("Reservation service base URL is empty. Skipping reservation API call.");
            return false;
        }
        return true;
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return "http://localhost:8090";
        }

        String normalized = baseUrl.trim();
        if (normalized.endsWith("/")) {
            return normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
