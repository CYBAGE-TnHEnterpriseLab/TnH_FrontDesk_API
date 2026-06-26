package com.pms.reservation.service.impl;

import com.pms.guestlisting.exception.BadRequestException;
import com.pms.reservation.config.PropertyWizardServiceProperties;
import com.pms.reservation.dto.ReservationAvailabilityRequestDto;
import com.pms.reservation.dto.ReservationAvailabilityResponseDto;
import com.pms.reservation.dto.RoomAvailabilityPricingDto;
import com.pms.reservation.integration.PropertyInventoryPort;
import com.pms.reservation.integration.RateManagementPort;
import com.pms.reservation.integration.dto.PropertyRoomInventoryDto;
import com.pms.reservation.integration.dto.RatePlanPricingQuoteDto;
import com.pms.reservation.mapper.ReservationAvailabilityMapper;
import com.pms.reservation.service.ReservationAvailabilityService;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ReservationAvailabilityServiceImpl implements ReservationAvailabilityService {

    private final PropertyInventoryPort propertyInventoryPort;
    private final RateManagementPort rateManagementPort;
    private final PropertyWizardServiceProperties propertyWizardServiceProperties;
    private final ReservationAvailabilityMapper reservationAvailabilityMapper;

    @Override
    public ReservationAvailabilityResponseDto getAvailability(ReservationAvailabilityRequestDto request) {
        validateDates(request.getArrivalDate(), request.getDepartureDate());

        if (!propertyWizardServiceProperties.isEnabled()) {
            throw new BadRequestException("Live inventory is unavailable because Property Wizard integration is disabled");
        }

        List<PropertyRoomInventoryDto> inventory = propertyInventoryPort.fetchLiveInventory(
                request.getPropertyId(),
                request.getArrivalDate(),
                request.getDepartureDate(),
                request.getRoomType()
        );

        List<RatePlanPricingQuoteDto> rateQuotes = rateManagementPort.fetchRateQuotes(
                request.getPropertyId(),
                request.getArrivalDate(),
                request.getDepartureDate(),
                request.getRoomType(),
                request.getAdultCount(),
                request.getChildCount()
        );

        if (StringUtils.hasText(request.getRoomType())) {
            inventory = inventory.stream()
                    .filter(item -> isSameRoomType(item.getRoomType(), request.getRoomType()))
                    .toList();
            rateQuotes = rateQuotes.stream()
                    .filter(item -> isSameRoomType(item.getRoomType(), request.getRoomType()))
                    .toList();
        }

        Map<String, PropertyRoomInventoryDto> inventoryByRoomType = new LinkedHashMap<>();
        for (PropertyRoomInventoryDto item : inventory) {
            if (StringUtils.hasText(item.getRoomType())) {
                inventoryByRoomType.putIfAbsent(normalize(item.getRoomType()), item);
            }
        }

        List<RoomAvailabilityPricingDto> availability = rateQuotes.stream()
                .filter(item -> StringUtils.hasText(item.getRoomType()))
                .filter(item -> inventoryByRoomType.containsKey(normalize(item.getRoomType())))
            .map(item -> reservationAvailabilityMapper.toRoomAvailability(
                item,
                inventoryByRoomType.get(normalize(item.getRoomType()))
            ))
                .sorted(Comparator
                        .comparing(RoomAvailabilityPricingDto::getRoomType, Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(RoomAvailabilityPricingDto::getRatePlan, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        return reservationAvailabilityMapper.toResponse(request, availability);
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
        return normalize(left).equals(normalize(right));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
