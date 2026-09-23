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
import com.pms.reservation.integration.dto.PropertyRoomOutletTypeDto;
import com.pms.reservation.integration.dto.InventoryAvailabilityDto;
import com.pms.reservation.integration.dto.RatePlanPricingQuoteDto;
import com.pms.reservation.mapper.ReservationAvailabilityMapper;
import com.pms.reservation.config.AvailabilityPerformanceProperties;
import com.pms.reservation.support.AvailabilityParallelExecutor;
import com.pms.reservation.support.TtlCache;
import java.math.BigDecimal;
import java.math.RoundingMode;
import com.pms.reservation.service.ReservationAvailabilityService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.pms.reservation.integration.InventoryServiceClient;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.IntStream;
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
    private final InventoryServiceClient inventoryServiceClient;
    private final AvailabilityParallelExecutor parallelExecutor;
    private final AvailabilityPerformanceProperties performanceProperties;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    @Override
    public ReservationAvailabilityResponseDto getAvailability(ReservationAvailabilityRequestDto request) {
     //validateRequestedRoomCount(request.getNumberOfRooms());
        validateDates(request.getArrivalDate(), request.getDepartureDate());

        if (!propertyWizardServiceProperties.isEnabled()) {
            throw new BadRequestException("Live inventory is unavailable because Property Wizard integration is disabled");
        }

        CompletableFuture<List<PropertyTaxRuleResponseDto>> taxRulesFuture =
            parallelExecutor.submitIo(() -> safeFetchTaxRules(request.getPropertyId()));
        CompletableFuture<List<PropertyRoomOutletTypeDto>> roomOutletTypesFuture =
            parallelExecutor.submitIo(() -> propertyInventoryPort.fetchRoomOutletTypes(request.getPropertyId()));

        List<PropertyTaxRuleResponseDto> taxRules = parallelExecutor.join(taxRulesFuture);
        List<PropertyRoomOutletTypeDto> roomOutletTypes = parallelExecutor.join(roomOutletTypesFuture);

        int forecastDays = Math.max(1, performanceProperties.getForecastDays());
        LocalDate forecastEnd = request.getArrivalDate().plusDays(forecastDays);
        LocalDate windowEnd = request.getDepartureDate().isAfter(forecastEnd)
            ? request.getDepartureDate()
            : forecastEnd;

        AvailabilityLookupContext lookupContext = new AvailabilityLookupContext(
            request.getPropertyId(),
            request.getArrivalDate(),
            windowEnd
        );
        warmInventoryWindow(lookupContext, roomOutletTypes);

        AvailabilityRangeResult primaryRange = fetchAvailabilityForRange(
            request,
            request.getArrivalDate(),
            request.getDepartureDate(),
            taxRules,
            roomOutletTypes,
            lookupContext
        );

        List<DailyAvailabilityPricingDto> next15DaysPricing = fetchForecastPricing(
            request,
            taxRules,
            roomOutletTypes,
            lookupContext,
            forecastDays
        );

        List<String> availableRateCodes = extractAvailableRateCodes(primaryRange.rateQuotes());

        return reservationAvailabilityMapper.toResponse(
            request,
            primaryRange.availability(),
            next15DaysPricing,
            availableRateCodes
        );
    }


    /*private void validateRequestedRoomCount(Integer numberOfRooms) {
        if (numberOfRooms == null || numberOfRooms < 1 || numberOfRooms > 9) {
            throw new BadRequestException("numberOfRooms must be between 1 and 9");
        }
    }

        private List<DailyAvailabilityPricingDto> fetchNext15DaysPricing(
            ReservationAvailabilityRequestDto request,
            List<PropertyTaxRuleResponseDto> taxRules
        ) {
        List<DailyAvailabilityPricingDto> result = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            LocalDate date = request.getArrivalDate().plusDays(i);*/

    /**
     * Loads the full forecast window for every known room type up front so the per-day lookups
     * below become in-memory slices instead of one inventory round trip per day per room type.
     */
    private void warmInventoryWindow(
        AvailabilityLookupContext lookupContext,
        List<PropertyRoomOutletTypeDto> roomOutletTypes
    ) {
        if (roomOutletTypes == null || roomOutletTypes.isEmpty()) {
            return;
        }

        List<String> inventoryRoomTypeIds = roomOutletTypes.stream()
            .filter(roomType -> roomType != null && roomType.getId() != null)
            .map(roomType -> inventoryRoomTypeId(
                lookupContext.propertyId(), roomType.getRoomCode(), roomType.getRoomName()))
            .distinct()
            .toList();

        try {
            parallelExecutor.mapIo(inventoryRoomTypeIds, lookupContext::availabilityByDate);
        } catch (RuntimeException ex) {
            log.warn("Inventory window prefetch failed for propertyId={}; falling back to on-demand lookups. reason={}",
                lookupContext.propertyId(), ex.getMessage());
        }
    }

    private List<DailyAvailabilityPricingDto> fetchForecastPricing(
        ReservationAvailabilityRequestDto request,
        List<PropertyTaxRuleResponseDto> taxRules,
        List<PropertyRoomOutletTypeDto> roomOutletTypes,
        AvailabilityLookupContext lookupContext,
        int forecastDays
    ) {
        List<Integer> dayOffsets = IntStream.range(0, forecastDays).boxed().toList();

        return parallelExecutor.mapDays(dayOffsets, offset -> {
            LocalDate date = request.getArrivalDate().plusDays(offset);

            AvailabilityRangeResult dailyRange = fetchAvailabilityForRange(
                request,
                date,
                date.plusDays(1),
                taxRules,
                roomOutletTypes,
                lookupContext
            );

            return DailyAvailabilityPricingDto.builder()
                .date(date)
                .availability(dailyRange.availability())
                .build();
        });
    }

        private AvailabilityRangeResult fetchAvailabilityForRange(
            ReservationAvailabilityRequestDto request,
            LocalDate arrivalDate,
            LocalDate departureDate,
            List<PropertyTaxRuleResponseDto> taxRules,
            List<PropertyRoomOutletTypeDto> roomOutletTypes,
            AvailabilityLookupContext lookupContext
        ) {
        List<PropertyRoomInventoryDto> inventory = fetchInventoryFromInventoryService(
            arrivalDate, departureDate, roomOutletTypes, lookupContext);

        inventory = enrichInventoryWithRoomTypeIds(request.getPropertyId(), inventory, roomOutletTypes);

            List<PropertyRoomInventoryDto> baseInventory = inventory;
            List<RatePlanPricingQuoteDto> rateQuotes = lookupContext.rateQuotes(
                arrivalDate,
                departureDate,
                () -> fetchRateQuotesWithRoomTypeFallback(request, arrivalDate, departureDate, baseInventory)
            );

        inventory = enrichInventoryFromRateQuotes(
            arrivalDate,
            departureDate,
            inventory,
            rateQuotes,
            lookupContext
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
                return reservationAvailabilityMapper.toRoomAvailability(item, matchedInventory);
            })
            .toList();

        List<RoomAvailabilityPricingDto> afterRequestedRoomCount = joinedByRoomType.stream()
            .filter(item -> item.getAvailableRooms() != null
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

    private List<PropertyRoomInventoryDto> fetchInventoryFromInventoryService(
            LocalDate arrivalDate,
            LocalDate departureDate,
            List<PropertyRoomOutletTypeDto> roomOutletTypes,
            AvailabilityLookupContext lookupContext) {
        if (roomOutletTypes == null || roomOutletTypes.isEmpty()) {
            return new ArrayList<>();
        }

        List<PropertyRoomInventoryDto> result = new ArrayList<>();
        for (PropertyRoomOutletTypeDto roomType : roomOutletTypes) {
            if (roomType == null || roomType.getId() == null) {
                continue;
            }
            int availableRooms = lookupContext.minAvailableCount(
                    inventoryRoomTypeId(lookupContext.propertyId(), roomType.getRoomCode(), roomType.getRoomName()),
                    arrivalDate,
                    departureDate);
            PropertyRoomInventoryDto item = new PropertyRoomInventoryDto();
            item.setRoomTypeId(roomType.getId());
            item.setRoomCode(roomType.getRoomCode());
            item.setRoomType(roomType.getRoomName());
            item.setAvailableRooms(availableRooms);
            result.add(item);
        }
        return result;
    }

    private String inventoryRoomTypeId(String propertyId, String roomCode, String roomName) {
        String roomKey = StringUtils.hasText(roomCode)
                ? roomCode.trim()
                : roomName == null ? "" : roomName.trim();
        String payload = (propertyId + ":" + (roomKey.isBlank() ? "unknown" : roomKey))
                .toLowerCase(Locale.ROOT);
        return UUID.nameUUIDFromBytes(payload.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private List<PropertyRoomInventoryDto> enrichInventoryFromRateQuotes(
            LocalDate arrivalDate,
            LocalDate departureDate,
            List<PropertyRoomInventoryDto> inventory,
            List<RatePlanPricingQuoteDto> rateQuotes,
            AvailabilityLookupContext lookupContext) {
        Set<Long> knownRoomTypeIds = inventory.stream()
            .map(PropertyRoomInventoryDto::getRoomTypeId)
            .filter(java.util.Objects::nonNull)
            .collect(java.util.stream.Collectors.toSet());

        List<RatePlanPricingQuoteDto> missingQuotes = new ArrayList<>();
        for (RatePlanPricingQuoteDto quote : rateQuotes) {
            Long roomTypeId = quote.getRoomTypeId();
            if (roomTypeId == null || !knownRoomTypeIds.add(roomTypeId)) {
                continue;
            }
            missingQuotes.add(quote);
        }

        List<PropertyRoomInventoryDto> enriched = new ArrayList<>(inventory);
        if (missingQuotes.isEmpty()) {
            return enriched;
        }

        enriched.addAll(parallelExecutor.mapIo(missingQuotes, quote -> {
            int availableRooms = lookupContext.minAvailableCount(
                inventoryRoomTypeId(lookupContext.propertyId(), null, quote.getRoomType()),
                arrivalDate,
                departureDate
            );

            PropertyRoomInventoryDto item = new PropertyRoomInventoryDto();
            item.setRoomTypeId(quote.getRoomTypeId());
            item.setRoomType(quote.getRoomType());
            item.setRoomCode(quote.getRoomType());
            item.setAvailableRooms(availableRooms);
            return item;
        }));
        return enriched;
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
                    return deduplicateRateQuotes(normalizedDirectFetch);
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

                return deduplicateRateQuotes(normalizedDirectFetch);
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

    private List<PropertyRoomInventoryDto> enrichInventoryWithRoomTypeIds(
        String propertyId,
        List<PropertyRoomInventoryDto> inventory,
        List<PropertyRoomOutletTypeDto> outletTypes
    ) {
        if (inventory == null || inventory.isEmpty()) {
            return List.of();
        }

        boolean allRowsAlreadyHaveRoomTypeId = inventory.stream()
            .filter(java.util.Objects::nonNull)
            .allMatch(item -> item.getRoomTypeId() != null);
        if (allRowsAlreadyHaveRoomTypeId) {
            return inventory;
        }

        if (outletTypes == null || outletTypes.isEmpty()) {
            return inventory;
        }

        Map<String, Long> roomTypeIdByNormalizedCode = new LinkedHashMap<>();
        Map<String, Long> roomTypeIdByNormalizedName = new LinkedHashMap<>();
        for (PropertyRoomOutletTypeDto outletType : outletTypes) {
            if (outletType == null || outletType.getId() == null) {
                continue;
            }

            if (StringUtils.hasText(outletType.getRoomCode())) {
                roomTypeIdByNormalizedCode.putIfAbsent(normalize(outletType.getRoomCode()), outletType.getId());
            }
            if (StringUtils.hasText(outletType.getRoomName())) {
                roomTypeIdByNormalizedName.putIfAbsent(normalize(outletType.getRoomName()), outletType.getId());
            }
        }

        for (PropertyRoomInventoryDto item : inventory) {
            if (item == null || item.getRoomTypeId() != null || !StringUtils.hasText(item.getRoomType())) {
                continue;
            }

            String normalizedRoomType = normalize(item.getRoomType());
            Long mappedRoomTypeId = roomTypeIdByNormalizedCode.get(normalizedRoomType);
            if (mappedRoomTypeId == null) {
                mappedRoomTypeId = roomTypeIdByNormalizedName.get(normalizedRoomType);
            }

            if (mappedRoomTypeId != null) {
                item.setRoomTypeId(mappedRoomTypeId);
            }
        }

        return inventory;
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
        AtomicBoolean unauthorizedEncountered = new AtomicBoolean(false);

        List<List<RatePlanPricingQuoteDto>> perCandidateQuotes = parallelExecutor.mapIo(
            new ArrayList<>(roomTypeCandidates.values()),
            candidate -> {
                String roomType = candidate.getRoomType();
                Long roomTypeId = candidate.getRoomTypeId();
                if (unauthorizedEncountered.get()) {
                    return List.<RatePlanPricingQuoteDto>of();
                }

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

                    return perRoomQuotes == null ? List.<RatePlanPricingQuoteDto>of() : perRoomQuotes;
                } catch (ExternalServiceException perRoomEx) {
                    if (isUnauthorizedRateManagementFailure(perRoomEx)) {
                        unauthorizedEncountered.set(true);
                        log.warn(
                            "Rate quote fetch unauthorized for propertyId={} roomType={} roomTypeId={} arrival={} departure={}; skipping remaining per-room retries. reason={}",
                            request.getPropertyId(),
                            roomType,
                            roomTypeId,
                            arrivalDate,
                            departureDate,
                            perRoomEx.getMessage()
                        );
                        return List.<RatePlanPricingQuoteDto>of();
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
                    return List.<RatePlanPricingQuoteDto>of();
                }
            }
        );

        perCandidateQuotes.forEach(aggregated::addAll);

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
            String roomTypeGroupingKey = StringUtils.hasText(quote.getRoomType())
                ? normalize(quote.getRoomType())
                : "id:" + (quote.getRoomTypeId() == null ? "" : quote.getRoomTypeId());

            String signature = String.join(
                "|",
                roomTypeGroupingKey,
                normalize(quote.getRatePlan()),
                normalize(quote.getRateCode()),
                normalize(quote.getOccupancy()),
                normalize(quote.getMealPlan())
            );

            RatePlanPricingQuoteDto existing = uniqueBySignature.get(signature);
            if (existing == null || compareAmounts(quote.getFinalAmount(), existing.getFinalAmount()) > 0) {
                uniqueBySignature.put(signature, quote);
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

    /**
     * Request-scoped caches. Inventory is fetched once per room type for the whole forecast window
     * and sliced per stay range; rate quotes are memoized per date range so overlapping ranges
     * (for example a one-night stay and day 0 of the forecast) only hit Rate Management once.
     */
    private final class AvailabilityLookupContext {

        private static final long REQUEST_SCOPED_TTL_MS = 10 * 60 * 1000L;

        private final String propertyId;
        private final LocalDate windowStart;
        private final LocalDate windowEnd;
        private final TtlCache<String, Map<LocalDate, Integer>> inventoryWindowCache =
            new TtlCache<>(REQUEST_SCOPED_TTL_MS, 512);
        private final TtlCache<String, List<RatePlanPricingQuoteDto>> rateQuoteCache =
            new TtlCache<>(REQUEST_SCOPED_TTL_MS, 128);

        private AvailabilityLookupContext(String propertyId, LocalDate windowStart, LocalDate windowEnd) {
            this.propertyId = propertyId;
            this.windowStart = windowStart;
            this.windowEnd = windowEnd;
        }

        private String propertyId() {
            return propertyId;
        }

        private Map<LocalDate, Integer> availabilityByDate(String inventoryRoomTypeId) {
            return inventoryWindowCache.get(
                inventoryRoomTypeId,
                () -> loadAvailability(inventoryRoomTypeId, windowStart, windowEnd)
            );
        }

        private Map<LocalDate, Integer> loadAvailability(String inventoryRoomTypeId, LocalDate from, LocalDate to) {
            List<InventoryAvailabilityDto> rows = inventoryServiceClient.availability(
                propertyId, inventoryRoomTypeId, from, to);
            if (rows == null || rows.isEmpty()) {
                return Map.of();
            }

            Map<LocalDate, Integer> availableCountByDate = new HashMap<>();
            for (InventoryAvailabilityDto row : rows) {
                if (row == null || row.getBusinessDate() == null || row.getAvailableCount() == null) {
                    continue;
                }
                availableCountByDate.merge(row.getBusinessDate(), row.getAvailableCount(), Math::min);
            }
            return availableCountByDate;
        }

        /** Mirrors the previous "min available count over the stay range, 0 when no rows" rule. */
        private int minAvailableCount(String inventoryRoomTypeId, LocalDate arrivalDate, LocalDate departureDate) {
            Map<LocalDate, Integer> availableCountByDate =
                arrivalDate.isBefore(windowStart) || departureDate.isAfter(windowEnd)
                    ? loadAvailability(inventoryRoomTypeId, arrivalDate, departureDate)
                    : availabilityByDate(inventoryRoomTypeId);

            int minimum = Integer.MAX_VALUE;
            for (LocalDate date = arrivalDate; date.isBefore(departureDate); date = date.plusDays(1)) {
                Integer availableCount = availableCountByDate.get(date);
                if (availableCount != null) {
                    minimum = Math.min(minimum, availableCount);
                }
            }
            return minimum == Integer.MAX_VALUE ? 0 : minimum;
        }

        private List<RatePlanPricingQuoteDto> rateQuotes(
            LocalDate arrivalDate,
            LocalDate departureDate,
            Supplier<List<RatePlanPricingQuoteDto>> loader
        ) {
            return rateQuoteCache.get(arrivalDate + "|" + departureDate, loader);
        }
    }
}
