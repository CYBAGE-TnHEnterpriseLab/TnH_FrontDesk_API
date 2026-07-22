package com.pms.reservation.integration;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.guestlisting.exception.ExternalServiceException;
import com.pms.reservation.config.RateManagementServiceProperties;
import com.pms.reservation.integration.dto.RateManagementPlanDto;
import com.pms.reservation.integration.dto.RatePlanCalculatedPriceResponseDto;
import com.pms.reservation.integration.dto.RatePlanPricingQuoteDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@Slf4j
public class RateManagementServiceClient implements RateManagementPort {

    private final RestTemplate restTemplate;

    private final RateManagementServiceProperties properties;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String MASTER_ROOM_PRICING_PATH = "/api/master-rooms/{id}/pricing";
    private final AtomicBoolean availablePlansGetUnsupported = new AtomicBoolean(false);
    private final AtomicBoolean availablePlansRequireRoomTypeId = new AtomicBoolean(false);
    private final AtomicBoolean calculatedPriceEndpointUnavailable = new AtomicBoolean(false);
    private final Map<Long, List<MasterRoomPricingEntry>> masterRoomPricingCache = new ConcurrentHashMap<>();

    public RateManagementServiceClient(
            @Qualifier("rateManagementRestTemplate") RestTemplate restTemplate,
            RateManagementServiceProperties properties
    ) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    @Override
    public List<RatePlanPricingQuoteDto> fetchRateQuotes(
            String propertyId,
            LocalDate arrivalDate,
            LocalDate departureDate,
            String roomType,
            Long roomTypeId,
            Integer adultCount,
            Integer childCount
    ) {
        String occupancyType = buildOccupancyType(adultCount, childCount);

        List<RateManagementPlanDto> availablePlans = resolvePlansForBookingContext(
            propertyId,
            roomTypeId,
            occupancyType,
            arrivalDate
        );
        if (availablePlans.isEmpty()) {
            return List.of();
        }

        List<RatePlanPricingQuoteDto> quotes = new ArrayList<>();
        for (RateManagementPlanDto plan : availablePlans) {
            if (plan.getId() == null) {
                log.warn(
                    "Rate Management plan missing id; skipping calculated-price call propertyId={} planCode={} roomType={} roomTypeId={}",
                    propertyId,
                    resolveRateCode(plan),
                    roomType,
                    roomTypeId
                );
                continue;
            }

            List<Long> candidateRoomTypeIds = resolveCandidateRoomTypeIds(plan, roomTypeId);
            if (candidateRoomTypeIds.isEmpty()) {
                log.info(
                    "Rate Management plan has no applicable roomTypeIds; skipping propertyId={} ratePlanId={} rateCode={}",
                    propertyId,
                    plan.getId(),
                    resolveRateCode(plan)
                );
                continue;
            }

            for (Long candidateRoomTypeId : candidateRoomTypeIds) {
                BigDecimal resolvedFinalAmount = resolveFinalAmount(propertyId, plan, candidateRoomTypeId, occupancyType);

                RatePlanPricingQuoteDto quote = new RatePlanPricingQuoteDto();
                quote.setRoomTypeId(candidateRoomTypeId);
                quote.setRoomType(resolveRoomType(plan, roomType));
                quote.setRatePlan(resolveRatePlanName(plan));
                quote.setRateCode(resolveRateCode(plan));
                quote.setOccupancy(resolveOccupancy(plan, occupancyType));
                quote.setMealPlan(resolveMealOption(plan));
                quote.setBaseRate(resolvedFinalAmount);
                quote.setTaxAmount(BigDecimal.ZERO);
                quote.setFinalAmount(resolvedFinalAmount);
                quotes.add(quote);
            }
        }

        return deduplicateQuotes(quotes);
    }

    @Override
    public List<RateManagementPlanDto> listRatePlans(String propertyId) {
        String url = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
            .path(properties.getListRatePlansPath())
            .buildAndExpand(propertyId)
            .toUriString();

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("propertyId", propertyId);

        String responseBody = executeGetWithRetry("list-rate-plans", url, context);
        return readListResponseBody(responseBody, RateManagementPlanDto.class, "list-rate-plans");
    }

    @Override
    public List<RateManagementPlanDto> getAvailableRatePlans(
            String propertyId,
            Long roomTypeId,
            String occupancyType,
            String mealOption,
            LocalDate stayDate
    ) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
            .path(properties.getAvailablePlansPath());

        if (roomTypeId != null) {
            builder.queryParam("roomTypeId", roomTypeId);
        }
        if (StringUtils.hasText(occupancyType)) {
            builder.queryParam("occupancyType", occupancyType);
        }
        if (StringUtils.hasText(mealOption)) {
            builder.queryParam("mealOption", mealOption);
        }
        if (stayDate != null) {
            builder.queryParam("stayDate", stayDate);
        }

        String url = builder.buildAndExpand(propertyId).toUriString();

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("propertyId", propertyId);
        context.put("roomTypeId", roomTypeId);
        context.put("occupancyType", occupancyType);
        context.put("mealOption", mealOption);
        context.put("stayDate", stayDate);

        String responseBody = executeGetWithRetry("available-rate-plans", url, context);
        return readListResponseBody(responseBody, RateManagementPlanDto.class, "available-rate-plans");
    }

    private List<RateManagementPlanDto> resolvePlansForBookingContext(
            String propertyId,
            Long roomTypeId,
            String occupancyType,
            LocalDate stayDate
    ) {
        boolean shouldTryAvailableEndpoint = !availablePlansGetUnsupported.get()
            && !(roomTypeId == null && availablePlansRequireRoomTypeId.get());

        if (shouldTryAvailableEndpoint) {
            try {
                return getAvailableRatePlans(propertyId, roomTypeId, occupancyType, null, stayDate);
            } catch (ExternalServiceException ex) {
                if (!isAvailableEndpointUnsupported(ex)) {
                    throw ex;
                }

                if (roomTypeId == null
                        && (hasHttpStatus(ex, 400) || isAvailableEndpointMissingRoomTypeId(ex))) {
                    if (availablePlansRequireRoomTypeId.compareAndSet(false, true)) {
                        log.info(
                            "Rate Management available endpoint requires roomTypeId. Falling back to list endpoint for null-roomType lookups while keeping per-room available endpoint enabled. propertyId={}",
                            propertyId
                        );
                    }
                } else if (availablePlansGetUnsupported.compareAndSet(false, true)) {
                    log.warn(
                        "Rate Management available endpoint rejected request (400/405). Falling back to list endpoint. propertyId={}",
                        propertyId
                    );
                }
            }
        }

        List<RateManagementPlanDto> configuredPlans = listRatePlans(propertyId);
        return filterFallbackPlans(configuredPlans, roomTypeId, stayDate);
    }

    @Override
    public RatePlanCalculatedPriceResponseDto getCalculatedPrice(
            String propertyId,
            Long ratePlanId,
            Long roomTypeId
    ) {
        if (roomTypeId == null) {
            throw new ExternalServiceException("roomTypeId is required for calculated-price API");
        }

        String url = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
            .path(properties.getCalculatedPricePath())
            .queryParam("roomTypeId", roomTypeId)
            .buildAndExpand(propertyId, ratePlanId)
            .toUriString();

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("propertyId", propertyId);
        context.put("ratePlanId", ratePlanId);
        context.put("roomTypeId", roomTypeId);

        String responseBody = executeGetWithRetry("calculated-price", url, context);
        return readObjectResponseBody(responseBody, RatePlanCalculatedPriceResponseDto.class, "calculated-price");
    }

    @Override
    public Map<Long, BigDecimal> getPricingByRoomTypeForRatePlan(String propertyId, Long ratePlanId) {
        List<RateManagementPlanDto> plans = listRatePlans(propertyId);
        RateManagementPlanDto selectedPlan = plans.stream()
            .filter(plan -> plan.getId() != null && plan.getId().equals(ratePlanId))
            .findFirst()
            .orElseThrow(() -> new ExternalServiceException(
                "Rate plan not found for propertyId=" + propertyId + " ratePlanId=" + ratePlanId
            ));

        List<Long> roomTypeIds = resolveCandidateRoomTypeIds(selectedPlan, null);
        if (roomTypeIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, BigDecimal> finalAmountByRoomTypeId = new LinkedHashMap<>();
        for (Long roomTypeId : roomTypeIds) {
            RatePlanCalculatedPriceResponseDto calculatedPrice = getCalculatedPrice(propertyId, ratePlanId, roomTypeId);
            if (calculatedPrice != null && calculatedPrice.getFinalAmount() != null) {
                finalAmountByRoomTypeId.put(roomTypeId, calculatedPrice.getFinalAmount());
            }
        }

        return finalAmountByRoomTypeId;
    }

    private String executeGetWithRetry(String operation, String url, Map<String, Object> context) {
        int maxAttempts = Math.max(1, properties.getRetryMaxAttempts());
        int attempt = 0;

        while (attempt < maxAttempts) {
            attempt++;
            try {
                ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    HttpEntity.EMPTY,
                    String.class
                );
                return response.getBody();
            } catch (RestClientException ex) {
                if (shouldRetry(ex, attempt, maxAttempts)) {
                    log.warn(
                        "Rate Management call retrying operation={} attempt={}/{} url={} context={} reason={}",
                        operation,
                        attempt,
                        maxAttempts,
                        url,
                        context,
                        describeRestClientException(ex)
                    );
                    applyRetryBackoff();
                    continue;
                }

                throw mapToExternalServiceException(operation, url, context, ex);
            }
        }

        throw new ExternalServiceException(
            "Rate Management call exhausted retries operation=" + operation + " url=" + url
        );
    }

    private boolean shouldRetry(RestClientException ex, int attempt, int maxAttempts) {
        if (attempt >= maxAttempts) {
            return false;
        }

        if (ex instanceof RestClientResponseException responseException) {
            return responseException.getRawStatusCode() >= 500;
        }

        return true;
    }

    private void applyRetryBackoff() {
        long backoffMs = Math.max(0L, properties.getRetryBackoffMs());
        if (backoffMs == 0L) {
            return;
        }

        try {
            Thread.sleep(backoffMs);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    private ExternalServiceException mapToExternalServiceException(
            String operation,
            String url,
            Map<String, Object> context,
            RestClientException ex
    ) {
        if (ex instanceof RestClientResponseException responseException) {
            int statusCode = responseException.getRawStatusCode();
            String responseBody = safeBodySnippet(responseException.getResponseBodyAsString());

            String message;
            if (statusCode == 401) {
                message = "Rate Management request unauthorized (401): token invalid or expired";
            } else if (statusCode == 403) {
                message = "Rate Management request forbidden (403): token has no permission";
            } else if (statusCode == 404) {
                message = "Rate Management resource not found (404)";
            } else if (statusCode == 405) {
                message = "Rate Management method not allowed (405)";
            } else if (statusCode >= 500) {
                message = "Rate Management downstream service failure (5xx)";
            } else {
                message = "Rate Management request failed (status=" + statusCode + ")";
            }

            if (statusCode >= 500) {
                log.error(
                    "Rate Management call failed operation={} status={} url={} context={} detail={}",
                    operation,
                    statusCode,
                    url,
                    context,
                    responseBody
                );
            } else {
                log.warn(
                    "Rate Management call failed operation={} status={} url={} context={} detail={}",
                    operation,
                    statusCode,
                    url,
                    context,
                    responseBody
                );
            }

            return new ExternalServiceException(message, ex);
        }

        log.warn(
            "Rate Management call failed operation={} url={} context={} detail={}",
            operation,
            url,
            context,
            ex.getMessage()
        );
        return new ExternalServiceException("Rate Management call failed", ex);
    }

    private String describeRestClientException(RestClientException ex) {
        if (ex instanceof RestClientResponseException responseException) {
            return "status=" + responseException.getRawStatusCode() + " body="
                + safeBodySnippet(responseException.getResponseBodyAsString());
        }
        return ex.getMessage();
    }

    private boolean isAvailableEndpointUnsupported(Throwable throwable) {
        return hasHttpStatus(throwable, 400) || hasHttpStatus(throwable, 405);
    }

    private boolean isAvailableEndpointMissingRoomTypeId(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof RestClientResponseException responseException
                    && responseException.getRawStatusCode() == 400) {
                String body = safeBodySnippet(responseException.getResponseBodyAsString()).toLowerCase(Locale.ROOT);
                if (body.contains("roomtypeid")) {
                    return true;
                }
            }
            current = current.getCause();
        }

        String message = throwable == null ? null : throwable.getMessage();
        return message != null && message.toLowerCase(Locale.ROOT).contains("roomtypeid");
    }

    private boolean isCalculatedPriceEndpointUnavailable(Throwable throwable) {
        return hasHttpStatus(throwable, 404) || hasHttpStatus(throwable, 405);
    }

    private boolean hasHttpStatus(Throwable throwable, int statusCode) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof RestClientResponseException responseException
                    && responseException.getRawStatusCode() == statusCode) {
                return true;
            }
            current = current.getCause();
        }

        return throwable != null
            && throwable.getMessage() != null
            && throwable.getMessage().contains(String.valueOf(statusCode));
    }

    private BigDecimal resolveFinalAmount(
            String propertyId,
            RateManagementPlanDto plan,
            Long roomTypeId,
            String requestedOccupancyType
    ) {
        if (calculatedPriceEndpointUnavailable.get()) {
            return deriveFallbackFinalAmount(propertyId, plan, requestedOccupancyType);
        }

        try {
            RatePlanCalculatedPriceResponseDto calculatedPrice = getCalculatedPrice(propertyId, plan.getId(), roomTypeId);
            if (calculatedPrice != null && calculatedPrice.getFinalAmount() != null) {
                return calculatedPrice.getFinalAmount();
            }
            return deriveFallbackFinalAmount(propertyId, plan, requestedOccupancyType);
        } catch (ExternalServiceException ex) {
            if (!isCalculatedPriceEndpointUnavailable(ex)) {
                throw ex;
            }

            if (calculatedPriceEndpointUnavailable.compareAndSet(false, true)) {
                log.warn(
                    "Rate Management calculated-price endpoint unavailable (404/405). Falling back to list plan pricing hints. propertyId={}",
                    propertyId
                );
            }

            return deriveFallbackFinalAmount(propertyId, plan, requestedOccupancyType);
        }
    }

    private BigDecimal deriveFallbackFinalAmount(
            String propertyId,
            RateManagementPlanDto plan,
            String requestedOccupancyType
    ) {
        BigDecimal occupancyPrice = resolveMasterRoomOccupancyPrice(plan, requestedOccupancyType);
        if (occupancyPrice != null && occupancyPrice.signum() > 0) {
            return occupancyPrice;
        }

        BigDecimal manualAmount = firstPositiveAmount(
            plan.getManualAmount(),
            extractManualPricingAmount(plan.getManualPricingByOccupancy(), plan.getOccupancyType()),
            extractManualPricingAmount(plan.getAdditionalFields(), "manualAmount")
        );

        if (manualAmount == null && plan.getId() != null) {
            log.info(
                "Rate plan fallback amount unresolved; defaulting to 0. propertyId={} ratePlanId={} rateCode={} occupancy={}",
                propertyId,
                plan.getId(),
                resolveRateCode(plan),
                firstNonBlank(plan.getOccupancyType(), requestedOccupancyType)
            );
        }

        return manualAmount == null ? BigDecimal.ZERO : manualAmount;
    }

    private BigDecimal resolveMasterRoomOccupancyPrice(
            RateManagementPlanDto plan,
            String requestedOccupancyType
    ) {
        if (plan.getId() == null) {
            return null;
        }

        List<MasterRoomPricingEntry> pricingEntries = masterRoomPricingCache.computeIfAbsent(
            plan.getId(),
            this::loadMasterRoomPricingEntries
        );
        if (pricingEntries.isEmpty()) {
            return null;
        }

        String primaryOccupancy = firstNonBlank(
            plan.getOccupancyType(),
            asText(plan.getAdditionalFields().get("occupancyType")),
            requestedOccupancyType
        );

        BigDecimal byPrimaryOccupancy = findPriceByOccupancy(pricingEntries, primaryOccupancy);
        if (byPrimaryOccupancy != null && byPrimaryOccupancy.signum() > 0) {
            return byPrimaryOccupancy;
        }

        BigDecimal byRequestedOccupancy = findPriceByOccupancy(pricingEntries, requestedOccupancyType);
        if (byRequestedOccupancy != null && byRequestedOccupancy.signum() > 0) {
            return byRequestedOccupancy;
        }

        for (MasterRoomPricingEntry entry : pricingEntries) {
            if (entry != null && entry.getPrice() != null && entry.getPrice().signum() > 0) {
                return entry.getPrice();
            }
        }

        return null;
    }

    private List<MasterRoomPricingEntry> loadMasterRoomPricingEntries(Long id) {
        String url = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
            .path(MASTER_ROOM_PRICING_PATH)
            .buildAndExpand(id)
            .toUriString();

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("id", id);

        try {
            String responseBody = executeGetWithRetry("master-room-pricing", url, context);
            return readListResponseBody(responseBody, MasterRoomPricingEntry.class, "master-room-pricing");
        } catch (ExternalServiceException ex) {
            log.info(
                "Master-room pricing lookup unavailable for id={}; skipping occupancy fallback. reason={}",
                id,
                ex.getMessage()
            );
            return List.of();
        }
    }

    private BigDecimal findPriceByOccupancy(List<MasterRoomPricingEntry> pricingEntries, String occupancyType) {
        if (!StringUtils.hasText(occupancyType) || pricingEntries == null || pricingEntries.isEmpty()) {
            return null;
        }

        String targetKey = normalizeOccupancyKey(occupancyType);
        BigDecimal bestNormalizedMatch = null;
        for (MasterRoomPricingEntry entry : pricingEntries) {
            if (entry == null || !StringUtils.hasText(entry.getOccupancyType())) {
                continue;
            }

            if (normalizeOccupancyKey(entry.getOccupancyType()).equals(targetKey)) {
                bestNormalizedMatch = maxPositive(bestNormalizedMatch, entry.getPrice());
            }
        }

        if (bestNormalizedMatch != null && bestNormalizedMatch.signum() > 0) {
            return bestNormalizedMatch;
        }

        Integer targetGuests = extractFirstInteger(occupancyType);
        if (targetGuests == null) {
            return bestNormalizedMatch;
        }

        BigDecimal bestNumericMatch = null;
        for (MasterRoomPricingEntry entry : pricingEntries) {
            Integer entryGuests = extractFirstInteger(entry.getOccupancyType());
            if (entryGuests != null && entryGuests.equals(targetGuests)) {
                bestNumericMatch = maxPositive(bestNumericMatch, entry.getPrice());
            }
        }

        if (bestNumericMatch != null && bestNumericMatch.signum() > 0) {
            return bestNumericMatch;
        }

        return bestNormalizedMatch;
    }

    private BigDecimal maxPositive(BigDecimal left, BigDecimal right) {
        if (right == null || right.signum() <= 0) {
            return left;
        }
        if (left == null || left.signum() <= 0) {
            return right;
        }

        return left.max(right);
    }

    private String normalizeOccupancyKey(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }

        String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.replaceAll("[^a-z0-9]", "");
    }

    private Integer extractFirstInteger(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        StringBuilder digits = new StringBuilder();
        for (char ch : value.toCharArray()) {
            if (Character.isDigit(ch)) {
                digits.append(ch);
            } else if (!digits.isEmpty()) {
                break;
            }
        }

        if (digits.isEmpty()) {
            return null;
        }

        try {
            return Integer.parseInt(digits.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private BigDecimal extractManualPricingAmount(Map<String, Object> source, String preferredKey) {
        if (source == null || source.isEmpty()) {
            return null;
        }

        if (StringUtils.hasText(preferredKey)) {
            BigDecimal preferredValue = toBigDecimal(source.get(preferredKey));
            if (preferredValue != null && preferredValue.signum() > 0) {
                return preferredValue;
            }
        }

        for (Object rawValue : source.values()) {
            BigDecimal amount = toBigDecimal(rawValue);
            if (amount != null && amount.signum() > 0) {
                return amount;
            }
        }

        return null;
    }

    private BigDecimal firstPositiveAmount(BigDecimal... candidates) {
        if (candidates == null) {
            return null;
        }

        for (BigDecimal candidate : candidates) {
            if (candidate != null && candidate.signum() > 0) {
                return candidate;
            }
        }

        return null;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof BigDecimal amount) {
            return amount;
        }

        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }

        if (value instanceof CharSequence text) {
            String normalized = text.toString().trim();
            if (!StringUtils.hasText(normalized)) {
                return null;
            }

            try {
                return new BigDecimal(normalized);
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        return null;
    }

    private String safeBodySnippet(String body) {
        if (!StringUtils.hasText(body)) {
            return "";
        }

        String flattened = body.replaceAll("[\\r\\n]+", " ");
        return flattened.length() <= 500 ? flattened : flattened.substring(0, 500) + "...";
    }

    private <T> List<T> readListResponseBody(String body, Class<T> itemType, String operation) {
        if (!StringUtils.hasText(body)) {
            return Collections.emptyList();
        }

        try {
            JsonNode root = OBJECT_MAPPER.readTree(body);
            JsonNode dataNode = unwrapDataNode(root);
            if (dataNode == null || dataNode.isNull()) {
                return Collections.emptyList();
            }

            JavaType listType = OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, itemType);
            if (dataNode.isArray()) {
                return OBJECT_MAPPER.readerFor(listType).readValue(dataNode);
            }

            if (dataNode.isObject()) {
                T single = OBJECT_MAPPER.treeToValue(dataNode, itemType);
                return single == null ? Collections.emptyList() : List.of(single);
            }

            return Collections.emptyList();
        } catch (Exception ex) {
            throw new ExternalServiceException("Failed to parse Rate Management " + operation + " response", ex);
        }
    }

    private <T> T readObjectResponseBody(String body, Class<T> itemType, String operation) {
        if (!StringUtils.hasText(body)) {
            return null;
        }

        try {
            JsonNode root = OBJECT_MAPPER.readTree(body);
            JsonNode dataNode = unwrapDataNode(root);
            if (dataNode == null || dataNode.isNull()) {
                return null;
            }

            return OBJECT_MAPPER.treeToValue(dataNode, itemType);
        } catch (Exception ex) {
            throw new ExternalServiceException("Failed to parse Rate Management " + operation + " response", ex);
        }
    }

    private JsonNode unwrapDataNode(JsonNode root) {
        if (root == null) {
            return null;
        }

        if (root.isObject()) {
            if (root.has("data")) {
                return root.get("data");
            }
            if (root.has("content")) {
                return root.get("content");
            }
        }

        return root;
    }

    private List<RateManagementPlanDto> filterFallbackPlans(
            List<RateManagementPlanDto> plans,
            Long requestedRoomTypeId,
            LocalDate stayDate
    ) {
        if (plans == null || plans.isEmpty()) {
            return List.of();
        }

        List<RateManagementPlanDto> filtered = new ArrayList<>();
        for (RateManagementPlanDto plan : plans) {
            if (plan == null) {
                continue;
            }
            if (!isPlanActive(plan)) {
                continue;
            }
            if (!isPlanApplicableForStayDate(plan, stayDate)) {
                continue;
            }
            if (!isApplicableToRequestedRoomTypeId(plan, requestedRoomTypeId)) {
                continue;
            }
            filtered.add(plan);
        }

        return filtered;
    }

    private boolean isPlanActive(RateManagementPlanDto plan) {
        if (!StringUtils.hasText(plan.getStatus())) {
            return true;
        }

        return "ACTIVE".equalsIgnoreCase(plan.getStatus().trim());
    }

    private boolean isPlanApplicableForStayDate(RateManagementPlanDto plan, LocalDate stayDate) {
        if (stayDate == null) {
            return true;
        }

        LocalDate startDate = parseLocalDate(plan.getStartDate());
        LocalDate endDate = parseLocalDate(plan.getEndDate());

        if (startDate != null && stayDate.isBefore(startDate)) {
            return false;
        }
        if (endDate != null && stayDate.isAfter(endDate)) {
            return false;
        }

        return true;
    }

    private LocalDate parseLocalDate(String rawDateValue) {
        if (!StringUtils.hasText(rawDateValue)) {
            return null;
        }

        String normalized = rawDateValue.trim();
        if (normalized.length() >= 10) {
            normalized = normalized.substring(0, 10);
        }

        try {
            return LocalDate.parse(normalized);
        } catch (Exception ex) {
            return null;
        }
    }

    private String resolveRatePlanName(RateManagementPlanDto plan) {
        return firstNonBlank(plan.getName(), asText(plan.getAdditionalFields().get("name")), "STANDARD");
    }

    private String resolveRateCode(RateManagementPlanDto plan) {
        return firstNonBlank(plan.getCode(), asText(plan.getAdditionalFields().get("code")));
    }

    private String resolveOccupancy(RateManagementPlanDto plan, String fallbackOccupancyType) {
        return firstNonBlank(
            plan.getOccupancyType(),
            asText(plan.getAdditionalFields().get("occupancyType")),
            fallbackOccupancyType
        );
    }

    private String resolveMealOption(RateManagementPlanDto plan) {
        return firstNonBlank(plan.getMealOption(), asText(plan.getAdditionalFields().get("mealOption")));
    }

    private String resolveRoomType(RateManagementPlanDto plan, String requestedRoomType) {
        return firstNonBlank(
            requestedRoomType,
            plan.getRoomType(),
            asText(plan.getAdditionalFields().get("roomTypeName"))
        );
    }

    private List<Long> resolveCandidateRoomTypeIds(RateManagementPlanDto plan, Long requestedRoomTypeId) {
        if (requestedRoomTypeId != null) {
            if (isApplicableToRequestedRoomTypeId(plan, requestedRoomTypeId)) {
                return List.of(requestedRoomTypeId);
            }
            return List.of();
        }

        List<Long> applicableRoomTypeIds = extractRoomTypeIds(plan);
        if (!applicableRoomTypeIds.isEmpty()) {
            return applicableRoomTypeIds;
        }

        if (plan.getRoomTypeId() != null && plan.getRoomTypeId() > 0) {
            return List.of(plan.getRoomTypeId());
        }

        return List.of();
    }

    private boolean isApplicableToRequestedRoomTypeId(RateManagementPlanDto plan, Long requestedRoomTypeId) {
        if (requestedRoomTypeId == null) {
            return true;
        }

        List<Long> applicableRoomTypeIds = extractRoomTypeIds(plan);
        if (applicableRoomTypeIds.isEmpty()) {
            return true;
        }

        for (Long roomTypeId : applicableRoomTypeIds) {
            if (roomTypeId != null && roomTypeId.equals(requestedRoomTypeId)) {
                return true;
            }
        }

        return false;
    }

    private List<Long> extractRoomTypeIds(RateManagementPlanDto plan) {
        if (plan.getApplicableRoomTypeIds() != null && !plan.getApplicableRoomTypeIds().isEmpty()) {
            return distinctPositiveLongs(plan.getApplicableRoomTypeIds());
        }

        Object nestedRoomTypeIds = plan.getAdditionalFields().get("applicableRoomTypeIds");
        return parseLongList(nestedRoomTypeIds);
    }

    private List<Long> parseLongList(Object rawValue) {
        if (rawValue == null) {
            return List.of();
        }

        List<Long> values = new ArrayList<>();
        if (rawValue instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                Long parsed = toLong(item);
                if (parsed != null && parsed > 0) {
                    values.add(parsed);
                }
            }
            return distinctPositiveLongs(values);
        }

        String asText = asText(rawValue);
        if (!StringUtils.hasText(asText)) {
            return List.of();
        }

        for (String token : asText.split("[,\\s]+")) {
            Long parsed = toLong(token);
            if (parsed != null && parsed > 0) {
                values.add(parsed);
            }
        }

        return distinctPositiveLongs(values);
    }

    private List<Long> distinctPositiveLongs(List<Long> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<Long> deduplicated = new LinkedHashSet<>();
        for (Long value : values) {
            if (value != null && value > 0) {
                deduplicated.add(value);
            }
        }

        return new ArrayList<>(deduplicated);
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.longValue();
        }

        if (value instanceof CharSequence text) {
            String normalized = text.toString().trim();
            if (!StringUtils.hasText(normalized)) {
                return null;
            }
            try {
                return Long.parseLong(normalized);
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        return null;
    }

    private List<RatePlanPricingQuoteDto> deduplicateQuotes(List<RatePlanPricingQuoteDto> quotes) {
        if (quotes == null || quotes.isEmpty()) {
            return List.of();
        }

        Map<String, RatePlanPricingQuoteDto> uniqueBySignature = new LinkedHashMap<>();
        for (RatePlanPricingQuoteDto quote : quotes) {
            String groupingKey = String.join(
                "|",
                quote.getRoomTypeId() == null ? "" : String.valueOf(quote.getRoomTypeId()),
                normalize(quote.getRoomType()),
                normalize(quote.getRatePlan()),
                normalize(quote.getRateCode()),
                normalize(quote.getOccupancy()),
                normalize(quote.getMealPlan())
            );

            RatePlanPricingQuoteDto existing = uniqueBySignature.get(groupingKey);
            if (existing == null || compareAmounts(quote.getFinalAmount(), existing.getFinalAmount()) > 0) {
                uniqueBySignature.put(groupingKey, quote);
            }
        }

        return new ArrayList<>(uniqueBySignature.values());
    }

    private int compareAmounts(BigDecimal left, BigDecimal right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }

        return left.compareTo(right);
    }

    private String asText(Object value) {
        if (value == null) {
            return null;
        }

        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }

        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }

        return null;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String buildOccupancyType(Integer adultCount, Integer childCount) {
        int adults = adultCount == null ? 0 : adultCount;
        int children = childCount == null ? 0 : childCount;

        if (adults <= 0 && children <= 0) {
            return null;
        }

        if (children <= 0) {
            return adults + " Adults";
        }

        return adults + " Adults " + children + " Children";
    }

    private static class MasterRoomPricingEntry {
        private String occupancyType;
        private BigDecimal price;

        public String getOccupancyType() {
            return occupancyType;
        }

        public void setOccupancyType(String occupancyType) {
            this.occupancyType = occupancyType;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }
    }
}
