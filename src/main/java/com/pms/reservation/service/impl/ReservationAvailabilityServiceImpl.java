package com.pms.reservation.service.impl;

import com.pms.guestlisting.exception.BadRequestException;
import com.pms.guestlisting.exception.ExternalServiceException;
import com.pms.reservation.dto.DailyAvailabilityPricingDto;
import com.pms.reservation.config.PropertyWizardServiceProperties;
import com.pms.reservation.dto.ReservationAvailabilityRequestDto;
import com.pms.reservation.dto.ReservationAvailabilityResponseDto;
import com.pms.reservation.dto.RoomAvailabilityPricingDto;
import com.pms.reservation.integration.PropertyInventoryPort;
import com.pms.reservation.integration.RateManagementPort;
import com.pms.reservation.integration.dto.PropertyTaxRuleResponseDto;
import com.pms.reservation.integration.dto.PropertyRoomInventoryDto;
import com.pms.reservation.integration.dto.RatePlanPricingQuoteDto;
import com.pms.reservation.mapper.ReservationAvailabilityMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import com.pms.reservation.service.ReservationAvailabilityService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientResponseException;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReservationAvailabilityServiceImpl implements ReservationAvailabilityService {

    private final PropertyInventoryPort propertyInventoryPort;
    private final RateManagementPort rateManagementPort;
    private final PropertyWizardServiceProperties propertyWizardServiceProperties;
    private final ReservationAvailabilityMapper reservationAvailabilityMapper;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    @Override
    public ReservationAvailabilityResponseDto getAvailability(ReservationAvailabilityRequestDto request) {
        validateDates(request.getArrivalDate(), request.getDepartureDate());

        if (!propertyWizardServiceProperties.isEnabled()) {
            throw new BadRequestException("Live inventory is unavailable because Property Wizard integration is disabled");
        }

        List<PropertyTaxRuleResponseDto> taxRules = safeFetchTaxRules(request.getPropertyId());

        AvailabilityRangeResult primaryRange = fetchAvailabilityForRange(
            request,
            request.getArrivalDate(),
            request.getDepartureDate(),
            taxRules
        );

        List<DailyAvailabilityPricingDto> next15DaysPricing = fetchNext15DaysPricing(request, taxRules);

        List<String> availableRateCodes = extractAvailableRateCodes(primaryRange.rateQuotes());

        return reservationAvailabilityMapper.toResponse(
            request,
            primaryRange.availability(),
            next15DaysPricing,
            availableRateCodes
        );
    }

        private List<DailyAvailabilityPricingDto> fetchNext15DaysPricing(
            ReservationAvailabilityRequestDto request,
            List<PropertyTaxRuleResponseDto> taxRules
        ) {
        List<DailyAvailabilityPricingDto> result = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            LocalDate date = request.getArrivalDate().plusDays(i);
            AvailabilityRangeResult dailyRange = fetchAvailabilityForRange(
                request,
                date,
                date.plusDays(1),
                taxRules
            );

            result.add(DailyAvailabilityPricingDto.builder()
                .date(date)
                .availability(dailyRange.availability())
                .build());
        }
        return result;
        }

        private AvailabilityRangeResult fetchAvailabilityForRange(
            ReservationAvailabilityRequestDto request,
            LocalDate arrivalDate,
            LocalDate departureDate,
            List<PropertyTaxRuleResponseDto> taxRules
        ) {
        List<PropertyRoomInventoryDto> inventory = propertyInventoryPort.fetchLiveInventory(
            request.getPropertyId(),
            arrivalDate,
            departureDate,
            null
        );
        if (inventory == null) {
            inventory = List.of();
        }

            List<RatePlanPricingQuoteDto> rateQuotes = fetchRateQuotesWithRoomTypeFallback(
                request,
                arrivalDate,
                departureDate,
                inventory
            );

        Map<Long, PropertyRoomInventoryDto> inventoryByRoomTypeId = new LinkedHashMap<>();
        Map<String, PropertyRoomInventoryDto> inventoryByRoomType = new LinkedHashMap<>();
        for (PropertyRoomInventoryDto item : inventory) {
            if (item.getRoomTypeId() != null) {
                inventoryByRoomTypeId.putIfAbsent(item.getRoomTypeId(), item);
            }
            if (StringUtils.hasText(item.getRoomType())) {
                inventoryByRoomType.putIfAbsent(normalize(item.getRoomType()), item);
            }
        }

        List<PropertyRoomInventoryDto> finalInventory = inventory;

        List<RoomAvailabilityPricingDto> joinedByRoomType = rateQuotes.stream()
            .map(item -> {
                PropertyRoomInventoryDto matchedInventory = findMatchedInventory(
                    item,
                    inventoryByRoomTypeId,
                    inventoryByRoomType,
                    finalInventory
                );
                if (matchedInventory == null) {
                    return null;
                }
                return reservationAvailabilityMapper.toRoomAvailability(item, matchedInventory);
            })
            .filter(java.util.Objects::nonNull)
            .toList();

        List<RoomAvailabilityPricingDto> afterRequestedRoomCount = joinedByRoomType.stream()
            .filter(item -> item.getAvailableRooms() != null
                && request.getNumberOfRooms() != null
                && item.getAvailableRooms() >= request.getNumberOfRooms())
            .toList();

        List<RoomAvailabilityPricingDto> afterRateCodeFilter = applyRateCodeFilter(
            request.getRateCode(),
            afterRequestedRoomCount,
            request.getPropertyId(),
            arrivalDate,
            departureDate
        );

        List<RoomAvailabilityPricingDto> finalAvailability = afterRateCodeFilter.stream()
            .map(item -> applyTaxRules(item, taxRules))
            .sorted(Comparator
                .comparing(RoomAvailabilityPricingDto::getRoomType, Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(RoomAvailabilityPricingDto::getRatePlan, Comparator.nullsLast(String::compareToIgnoreCase)))
            .toList();

        logPrimaryRangeDiagnostics(
            request,
            arrivalDate,
            departureDate,
            inventory,
            rateQuotes,
            joinedByRoomType,
            afterRequestedRoomCount,
            finalAvailability
        );

        return new AvailabilityRangeResult(finalAvailability, rateQuotes);
        }

    private List<String> extractAvailableRateCodes(List<RatePlanPricingQuoteDto> rateQuotes) {
        if (rateQuotes == null || rateQuotes.isEmpty()) {
            return List.of();
        }

        Set<String> distinctCodes = new LinkedHashSet<>();
        for (RatePlanPricingQuoteDto quote : rateQuotes) {
            if (quote == null || !StringUtils.hasText(quote.getRateCode())) {
                continue;
            }

            String trimmedRateCode = quote.getRateCode().trim();
            distinctCodes.add(trimmedRateCode);
        }

        return new ArrayList<>(distinctCodes);
    }

        private void logPrimaryRangeDiagnostics(
            ReservationAvailabilityRequestDto request,
            LocalDate arrivalDate,
            LocalDate departureDate,
            List<PropertyRoomInventoryDto> inventory,
            List<RatePlanPricingQuoteDto> rateQuotes,
            List<RoomAvailabilityPricingDto> joinedByRoomType,
            List<RoomAvailabilityPricingDto> afterRequestedRoomCount,
            List<RoomAvailabilityPricingDto> finalAvailability
        ) {
        if (!arrivalDate.equals(request.getArrivalDate()) || !departureDate.equals(request.getDepartureDate())) {
            return;
        }

        Set<String> inventoryTypes = inventory.stream()
            .map(PropertyRoomInventoryDto::getRoomType)
            .filter(StringUtils::hasText)
            .map(this::normalize)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        Set<String> rateTypes = rateQuotes.stream()
            .map(RatePlanPricingQuoteDto::getRoomType)
            .filter(StringUtils::hasText)
            .map(this::normalize)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        Set<String> matchedTypes = new LinkedHashSet<>(rateTypes);
        matchedTypes.retainAll(inventoryTypes);

        log.info(
            "Availability diagnostics propertyId={} arrival={} departure={} requestedRooms={} requestedRoomTypeIgnored={} rateCode={} inventoryRows={} rateQuotes={} joinedByRoomType={} afterRequestedRoomCount={} finalAvailability={}",
            request.getPropertyId(),
            arrivalDate,
            departureDate,
            request.getNumberOfRooms(),
            request.getRoomType(),
            request.getRateCode(),
            inventory.size(),
            rateQuotes.size(),
            joinedByRoomType.size(),
            afterRequestedRoomCount.size(),
            finalAvailability.size()
        );

        if (finalAvailability.isEmpty()) {
            log.info(
                "Availability diagnostics roomTypes inventory={} rate={} matched={}",
                inventoryTypes.stream().limit(10).toList(),
                rateTypes.stream().limit(10).toList(),
                matchedTypes.stream().limit(10).toList()
            );
        }
        }

    private List<RatePlanPricingQuoteDto> fetchRateQuotesWithRoomTypeFallback(
        ReservationAvailabilityRequestDto request,
        LocalDate arrivalDate,
        LocalDate departureDate,
        List<PropertyRoomInventoryDto> inventory
    ) {
        Map<String, PropertyRoomInventoryDto> roomTypeCandidates = buildRoomTypeCandidates(inventory);

        try {
            List<RatePlanPricingQuoteDto> directFetch = rateManagementPort.fetchRateQuotes(
                request.getPropertyId(),
                arrivalDate,
                departureDate,
                null,
                null,
                request.getAdultCount(),
                request.getChildCount()
            );

            List<RatePlanPricingQuoteDto> normalizedDirectFetch = directFetch == null ? List.of() : directFetch;
            if (!normalizedDirectFetch.isEmpty()) {
                if (!requiresRoomTypeEnrichment(normalizedDirectFetch, roomTypeCandidates)) {
                    return normalizedDirectFetch;
                }

                log.info(
                    "Rate quote direct fetch produced {} rows for propertyId={} arrival={} departure={} but room-type signals are not joinable; retrying per-room candidate count={} to enrich roomType labels",
                    normalizedDirectFetch.size(),
                    request.getPropertyId(),
                    arrivalDate,
                    departureDate,
                    roomTypeCandidates.size()
                );

                List<RatePlanPricingQuoteDto> enrichedByRoomType = fetchRateQuotesByRoomTypeCandidates(
                    request,
                    arrivalDate,
                    departureDate,
                    roomTypeCandidates,
                    "direct fetch returned quotes without joinable room-type context"
                );

                if (!enrichedByRoomType.isEmpty()) {
                    return enrichedByRoomType;
                }

                return normalizedDirectFetch;
            }

            if (roomTypeCandidates.isEmpty()) {
                log.info(
                    "Rate quote fetch returned empty for propertyId={} arrival={} departure={} and no room-type candidates are available for retry; returning empty rate quotes",
                    request.getPropertyId(),
                    arrivalDate,
                    departureDate
                );
                return List.of();
            }

            log.info(
                "Rate quote fetch returned empty for propertyId={} arrival={} departure={}; retrying per room-type candidate count={}",
                request.getPropertyId(),
                arrivalDate,
                departureDate,
                roomTypeCandidates.size()
            );

            return fetchRateQuotesByRoomTypeCandidates(
                request,
                arrivalDate,
                departureDate,
                roomTypeCandidates,
                "direct fetch returned empty"
            );
        } catch (ExternalServiceException ex) {
            if (isUnauthorizedRateManagementFailure(ex)) {
                log.warn(
                    "Rate quote fetch unauthorized for propertyId={} arrival={} departure={}; skipping per-room fallback to avoid repeated unauthorized calls. reason={}",
                    request.getPropertyId(),
                    arrivalDate,
                    departureDate,
                    ex.getMessage()
                );
                return List.of();
            }

            if (roomTypeCandidates.isEmpty()) {
                log.warn(
                    "Rate quote fetch failed for propertyId={} arrival={} departure={} and no room-type candidates are available for retry; returning empty rate quotes. reason={}",
                    request.getPropertyId(),
                    arrivalDate,
                    departureDate,
                    ex.getMessage()
                );
                return List.of();
            }

            log.warn(
                "Rate quote fetch without roomType/roomTypeId failed for propertyId={} arrival={} departure={}; retrying per room-type candidate count={}. reason={}",
                request.getPropertyId(),
                arrivalDate,
                departureDate,
                roomTypeCandidates.size(),
                ex.getMessage()
            );

            return fetchRateQuotesByRoomTypeCandidates(
                request,
                arrivalDate,
                departureDate,
                roomTypeCandidates,
                ex.getMessage()
            );
        }
    }

    private Map<String, PropertyRoomInventoryDto> buildRoomTypeCandidates(List<PropertyRoomInventoryDto> inventory) {
        Map<String, PropertyRoomInventoryDto> roomTypeCandidates = new LinkedHashMap<>();
        if (inventory == null || inventory.isEmpty()) {
            return roomTypeCandidates;
        }

        for (PropertyRoomInventoryDto item : inventory) {
            if (item == null) {
                continue;
            }

            String candidateKey;
            if (item.getRoomTypeId() != null) {
                candidateKey = "id:" + item.getRoomTypeId();
            } else if (StringUtils.hasText(item.getRoomType())) {
                candidateKey = "name:" + normalize(item.getRoomType());
            } else {
                continue;
            }

            roomTypeCandidates.putIfAbsent(candidateKey, item);
        }

        return roomTypeCandidates;
    }

    private boolean requiresRoomTypeEnrichment(
        List<RatePlanPricingQuoteDto> directFetch,
        Map<String, PropertyRoomInventoryDto> roomTypeCandidates
    ) {
        if (directFetch == null || directFetch.isEmpty() || roomTypeCandidates == null || roomTypeCandidates.isEmpty()) {
            return false;
        }

        Set<Long> candidateRoomTypeIds = new LinkedHashSet<>();
        for (PropertyRoomInventoryDto candidate : roomTypeCandidates.values()) {
            if (candidate != null && candidate.getRoomTypeId() != null) {
                candidateRoomTypeIds.add(candidate.getRoomTypeId());
            }
        }

        for (RatePlanPricingQuoteDto quote : directFetch) {
            if (quote == null) {
                continue;
            }

            if (StringUtils.hasText(quote.getRoomType())) {
                return false;
            }

            if (quote.getRoomTypeId() != null && candidateRoomTypeIds.contains(quote.getRoomTypeId())) {
                return false;
            }
        }

        return true;
    }

    private List<RatePlanPricingQuoteDto> fetchRateQuotesByRoomTypeCandidates(
        ReservationAvailabilityRequestDto request,
        LocalDate arrivalDate,
        LocalDate departureDate,
        Map<String, PropertyRoomInventoryDto> roomTypeCandidates,
        String reason
    ) {
        List<RatePlanPricingQuoteDto> aggregated = new ArrayList<>();
        for (PropertyRoomInventoryDto candidate : roomTypeCandidates.values()) {
            String roomType = candidate.getRoomType();
            Long roomTypeId = candidate.getRoomTypeId();
            try {
                List<RatePlanPricingQuoteDto> perRoomQuotes = rateManagementPort.fetchRateQuotes(
                    request.getPropertyId(),
                    arrivalDate,
                    departureDate,
                    roomType,
                    roomTypeId,
                    request.getAdultCount(),
                    request.getChildCount()
                );

                if (perRoomQuotes != null && !perRoomQuotes.isEmpty()) {
                    aggregated.addAll(perRoomQuotes);
                }
            } catch (ExternalServiceException perRoomEx) {
                if (isUnauthorizedRateManagementFailure(perRoomEx)) {
                    log.warn(
                        "Rate quote fetch unauthorized for propertyId={} roomType={} roomTypeId={} arrival={} departure={}; stopping remaining per-room retries. reason={}",
                        request.getPropertyId(),
                        roomType,
                        roomTypeId,
                        arrivalDate,
                        departureDate,
                        perRoomEx.getMessage()
                    );
                    break;
                }

                log.warn(
                    "Rate quote fetch failed for propertyId={} roomType={} roomTypeId={} arrival={} departure={}. reason={}",
                    request.getPropertyId(),
                    roomType,
                    roomTypeId,
                    arrivalDate,
                    departureDate,
                    perRoomEx.getMessage()
                );
            }
        }

        if (aggregated.isEmpty()) {
            log.warn(
                "Rate quote fetch returned empty for propertyId={} arrival={} departure={} after per-room fallback; returning empty rate quotes. reason={}",
                request.getPropertyId(),
                arrivalDate,
                departureDate,
                reason
            );
            return List.of();
        }

        return deduplicateRateQuotes(aggregated);
    }

    private List<RatePlanPricingQuoteDto> deduplicateRateQuotes(List<RatePlanPricingQuoteDto> quotes) {
        if (quotes == null || quotes.isEmpty()) {
            return List.of();
        }

        Map<String, RatePlanPricingQuoteDto> uniqueBySignature = new LinkedHashMap<>();
        for (RatePlanPricingQuoteDto quote : quotes) {
            String signature = String.join(
                "|",
                normalize(quote.getRoomType()),
                quote.getRoomTypeId() == null ? "" : String.valueOf(quote.getRoomTypeId()),
                normalize(quote.getRatePlan()),
                normalize(quote.getRateCode()),
                normalize(quote.getOccupancy()),
                normalize(quote.getMealPlan()),
                quote.getBaseRate() == null ? "" : quote.getBaseRate().toPlainString()
            );
            uniqueBySignature.putIfAbsent(signature, quote);
        }

        return new ArrayList<>(uniqueBySignature.values());
    }

        private List<PropertyTaxRuleResponseDto> safeFetchTaxRules(String propertyId) {
        try {
            List<PropertyTaxRuleResponseDto> fetchedTaxRules = propertyInventoryPort.fetchTaxRules(propertyId);
            return fetchedTaxRules == null ? List.of() : fetchedTaxRules;
        } catch (ExternalServiceException ex) {
            log.warn("Tax rules unavailable for propertyId={}; continuing availability without tax rules. reason={}",
                propertyId,
                ex.getMessage());
            return List.of();
        }
        }

        private RoomAvailabilityPricingDto applyTaxRules(
            RoomAvailabilityPricingDto item,
            List<PropertyTaxRuleResponseDto> taxRules
        ) {
        BigDecimal baseRate = item.getBaseRate() == null ? BigDecimal.ZERO : item.getBaseRate();
        PropertyTaxRuleResponseDto matchedRule = findMatchedTaxRule(item.getRoomType(), baseRate, taxRules);

        BigDecimal taxAmount = BigDecimal.ZERO;
        if (matchedRule != null) {
            if (matchedRule.getTaxPercentage() != null) {
            taxAmount = baseRate
                .multiply(matchedRule.getTaxPercentage())
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);
            }
            if (matchedRule.getFixedTaxAmount() != null) {
            taxAmount = taxAmount.add(matchedRule.getFixedTaxAmount());
            }
        }

        BigDecimal finalAmount = baseRate.add(taxAmount);

        return RoomAvailabilityPricingDto.builder()
            .roomType(item.getRoomType())
            .ratePlan(item.getRatePlan())
            .rateCode(item.getRateCode())
            .occupancy(item.getOccupancy())
            .mealPlan(item.getMealPlan())
            .availableRooms(item.getAvailableRooms())
            .baseRate(baseRate)
            .taxAmount(taxAmount)
            .finalAmount(finalAmount)
            .build();
        }

        private PropertyTaxRuleResponseDto findMatchedTaxRule(
            String roomType,
            BigDecimal baseRate,
            List<PropertyTaxRuleResponseDto> taxRules
        ) {
        if (taxRules == null || taxRules.isEmpty()) {
            return null;
        }

        return taxRules.stream()
            .filter(rule -> !Boolean.FALSE.equals(rule.getActive()))
            .filter(rule -> !StringUtils.hasText(rule.getRoomType())
                || isSameRoomType(rule.getRoomType(), roomType))
            .filter(rule -> rule.getMinAmount() == null || baseRate.compareTo(rule.getMinAmount()) >= 0)
            .filter(rule -> rule.getMaxAmount() == null || baseRate.compareTo(rule.getMaxAmount()) <= 0)
            .findFirst()
            .orElse(null);
        }

    private void validateDates(LocalDate arrivalDate, LocalDate departureDate) {
        if (arrivalDate == null || departureDate == null) {
            throw new BadRequestException("arrivalDate and departureDate are required");
        }
        if (arrivalDate != null && departureDate != null && departureDate.isBefore(arrivalDate)) {
            throw new BadRequestException("departureDate must be on or after arrivalDate");
        }
    }

    private boolean isSameRoomType(String left, String right) {
        String normalizedLeft = normalize(left);
        String normalizedRight = normalize(right);

        if (!StringUtils.hasText(normalizedLeft) || !StringUtils.hasText(normalizedRight)) {
            return false;
        }

        return normalizedLeft.equals(normalizedRight)
            || normalizedLeft.contains(normalizedRight)
            || normalizedRight.contains(normalizedLeft);
    }

    private List<RoomAvailabilityPricingDto> applyRateCodeFilter(
        String requestedRateCode,
        List<RoomAvailabilityPricingDto> availability,
        String propertyId,
        LocalDate arrivalDate,
        LocalDate departureDate
    ) {
        if (!StringUtils.hasText(requestedRateCode)) {
            return availability;
        }

        List<RoomAvailabilityPricingDto> matched = availability.stream()
            .filter(item -> isSameRateCode(item.getRateCode(), requestedRateCode))
            .toList();

        if (!matched.isEmpty()) {
            return matched;
        }

        log.info(
            "Availability diagnostics rateCode fallback propertyId={} arrival={} departure={} requestedRateCode={} candidates={} (no match found, returning unfiltered availability)",
            propertyId,
            arrivalDate,
            departureDate,
            requestedRateCode,
            availability.size()
        );

        return availability;
    }

    private boolean isSameRateCode(String left, String right) {
        String normalizedLeft = normalize(left);
        String normalizedRight = normalize(right);

        if (!StringUtils.hasText(normalizedLeft) || !StringUtils.hasText(normalizedRight)) {
            return false;
        }

        return normalizedLeft.equals(normalizedRight)
            || normalizedLeft.contains(normalizedRight)
            || normalizedRight.contains(normalizedLeft);
    }

    private PropertyRoomInventoryDto findMatchedInventory(
        RatePlanPricingQuoteDto quote,
        Map<Long, PropertyRoomInventoryDto> inventoryByRoomTypeId,
        Map<String, PropertyRoomInventoryDto> inventoryByRoomType,
        List<PropertyRoomInventoryDto> inventory
    ) {
        if (quote == null) {
            return null;
        }

        if (quote.getRoomTypeId() != null) {
            PropertyRoomInventoryDto idMatch = inventoryByRoomTypeId.get(quote.getRoomTypeId());
            if (idMatch != null) {
                return idMatch;
            }
        }

        String quoteRoomType = quote.getRoomType();
        if (!StringUtils.hasText(quoteRoomType)) {
            return null;
        }

        PropertyRoomInventoryDto directMatch = inventoryByRoomType.get(normalize(quoteRoomType));
        if (directMatch != null) {
            return directMatch;
        }

        return inventory.stream()
            .filter(item -> isSameRoomType(item.getRoomType(), quoteRoomType))
            .findFirst()
            .orElse(null);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isUnauthorizedRateManagementFailure(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor != null) {
            if (cursor instanceof RestClientResponseException responseException) {
                int status = responseException.getRawStatusCode();
                return status == 401 || status == 403;
            }
            cursor = cursor.getCause();
        }
        return false;
    }

    private record AvailabilityRangeResult(
        List<RoomAvailabilityPricingDto> availability,
        List<RatePlanPricingQuoteDto> rateQuotes
    ) {
    }
}
