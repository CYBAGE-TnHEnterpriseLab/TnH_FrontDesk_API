package com.pms.reservation.mapper;

import com.pms.reservation.dto.DailyAvailabilityPricingDto;
import com.pms.reservation.dto.RatePlanAvailabilityDto;
import com.pms.reservation.dto.ReservationAvailabilityRequestDto;
import com.pms.reservation.dto.ReservationAvailabilityResponseDto;
import com.pms.reservation.dto.RoomAvailabilityPricingDto;
import com.pms.reservation.integration.dto.PropertyRoomInventoryDto;
import com.pms.reservation.integration.dto.RatePlanPricingQuoteDto;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ReservationAvailabilityMapper {

        private static final Pattern SIMPLE_COUNT_PATTERN = Pattern.compile("^(\\d+)\\s*(adult|adults|guest|guests)?$", Pattern.CASE_INSENSITIVE);
        private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+)");

    public RoomAvailabilityPricingDto toRoomAvailability(
            RatePlanPricingQuoteDto quote,
            PropertyRoomInventoryDto inventory
    ) {
        String roomType = StringUtils.hasText(quote.getRoomType())
                ? quote.getRoomType()
                : inventory.getRoomType();

        String occupancy = StringUtils.hasText(quote.getOccupancy())
                ? quote.getOccupancy()
                : inventory.getOccupancy();

        return RoomAvailabilityPricingDto.builder()
                .roomType(roomType)
                .ratePlan(quote.getRatePlan())
                .rateCode(quote.getRateCode())
                                .occupancy(normalizeOccupancyLabel(occupancy))
                .mealPlan(quote.getMealPlan())
                .availableRooms(inventory.getAvailableRooms())
                .baseRate(quote.getBaseRate())
                .taxAmount(quote.getTaxAmount())
                .finalAmount(quote.getFinalAmount())
                .build();
    }

        private String normalizeOccupancyLabel(String occupancy) {
                if (!StringUtils.hasText(occupancy)) {
                        return occupancy;
                }

                String trimmed = occupancy.trim();
                String normalized = trimmed.toLowerCase(Locale.ROOT);

                Integer mappedCount = switch (normalized) {
                        case "single" -> 1;
                        case "double", "twin" -> 2;
                        case "triple" -> 3;
                        case "quad", "quadruple" -> 4;
                        default -> null;
                };

                if (mappedCount != null) {
                        return String.valueOf(mappedCount);
                }

                Optional<Integer> simpleCount = extractSimpleCount(trimmed);
                if (simpleCount.isPresent()) {
                        return String.valueOf(simpleCount.get());
                }

                Optional<Integer> aggregatedCount = extractAggregatedCount(trimmed);
                if (aggregatedCount.isPresent()) {
                        return String.valueOf(aggregatedCount.get());
                }

                return trimmed;
        }

        private Optional<Integer> extractSimpleCount(String occupancy) {
                Matcher matcher = SIMPLE_COUNT_PATTERN.matcher(occupancy);
                if (!matcher.matches()) {
                        return Optional.empty();
                }
                return Optional.of(Integer.parseInt(matcher.group(1)));
        }

        private Optional<Integer> extractAggregatedCount(String occupancy) {
                Matcher matcher = NUMBER_PATTERN.matcher(occupancy);
                int total = 0;
                boolean found = false;
                while (matcher.find()) {
                        total += Integer.parseInt(matcher.group(1));
                        found = true;
                }
                return found ? Optional.of(total) : Optional.empty();
        }

    public ReservationAvailabilityResponseDto toResponse(
            ReservationAvailabilityRequestDto request,
                        List<RoomAvailabilityPricingDto> availability,
                        List<DailyAvailabilityPricingDto> next15DaysPricing,
                        List<String> availableRateCodes
    ) {
        return ReservationAvailabilityResponseDto.builder()
                .propertyId(request.getPropertyId())
                .arrivalDate(request.getArrivalDate())
                .departureDate(request.getDepartureDate())
                                .night(request.getNight())
                                .numberOfRooms(request.getNumberOfRooms())
                                .adults(request.getAdultCount())
                                .children(request.getChildCount())
                                .ageOfChild1(request.getAgeOfChild1())
                                .ageOfChild2(request.getAgeOfChild2())
                                .groupCode(request.getGroupCode())
                                .company(request.getCompany())
                                .rateCode(request.getRateCode())
                                .availableRateCodes(availableRateCodes)
                                                                .ratePlans(buildRatePlans(availability))
                                .blockCode(request.getBlockCode())
                .availability(availability)
                                .next15DaysPricing(next15DaysPricing)
                .build();
    }

        private List<RatePlanAvailabilityDto> buildRatePlans(List<RoomAvailabilityPricingDto> availability) {
                if (availability == null || availability.isEmpty()) {
                        return List.of();
                }

                Map<String, List<RoomAvailabilityPricingDto>> grouped = new LinkedHashMap<>();
                Map<String, GroupSignature> signatures = new LinkedHashMap<>();

                for (RoomAvailabilityPricingDto item : availability) {
                        if (item == null) {
                                continue;
                        }

                        String ratePlan = StringUtils.hasText(item.getRatePlan()) ? item.getRatePlan().trim() : "STANDARD";
                        String rateCode = StringUtils.hasText(item.getRateCode()) ? item.getRateCode().trim() : null;
                        String signature = normalize(ratePlan) + "|" + normalize(rateCode);

                        grouped.computeIfAbsent(signature, ignored -> new ArrayList<>()).add(item);
                        signatures.putIfAbsent(signature, new GroupSignature(ratePlan, rateCode));
                }

                return signatures.entrySet().stream()
                        .sorted(Comparator
                                .comparing((Map.Entry<String, GroupSignature> entry) -> normalize(entry.getValue().ratePlan()))
                                .thenComparing(entry -> normalize(entry.getValue().rateCode())))
                        .map(entry -> {
                                List<RoomAvailabilityPricingDto> rows = grouped.getOrDefault(entry.getKey(), List.of()).stream()
                                        .sorted(Comparator
                                                .comparing(RoomAvailabilityPricingDto::getRoomType, Comparator.nullsLast(String::compareToIgnoreCase))
                                                .thenComparing(RoomAvailabilityPricingDto::getOccupancy, Comparator.nullsLast(String::compareToIgnoreCase))
                                                .thenComparing(RoomAvailabilityPricingDto::getMealPlan, Comparator.nullsLast(String::compareToIgnoreCase)))
                                        .toList();

                                return RatePlanAvailabilityDto.builder()
                                        .ratePlan(entry.getValue().ratePlan())
                                        .rateCode(entry.getValue().rateCode())
                                        .roomTypes(rows)
                                        .build();
                        })
                        .toList();
        }

        private String normalize(String value) {
                return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        }

        private record GroupSignature(String ratePlan, String rateCode) {
        }
}
