package com.frontdesk.pms.content.mapper;

import com.frontdesk.pms.content.dto.AmenitiesResponseDTO;
import com.frontdesk.pms.content.dto.ContentConfigurationResponseDTO;
import com.frontdesk.pms.content.dto.SpecialRequestOptionDTO;
import com.frontdesk.pms.content.dto.SpecialRequestsResponseDTO;
import com.frontdesk.pms.content.entity.PropertyAmenitiesConfiguration;
import com.frontdesk.pms.content.entity.PropertySpecialRequestsConfiguration;

import java.util.List;
import java.util.UUID;

public final class ContentMapper {

    private ContentMapper() {
    }

    public static SpecialRequestsResponseDTO toSpecialRequestsResponse(PropertySpecialRequestsConfiguration entity, UUID propertyId) {
        return SpecialRequestsResponseDTO.builder()
                .propertyId(propertyId)
                .extraPillowEnabled(entity.isExtraPillowEnabled())
                .babyCribEnabled(entity.isBabyCribEnabled())
                .lateCheckOutEnabled(entity.isLateCheckOutEnabled())
                .hypoallergenicBeddingEnabled(entity.isHypoallergenicBeddingEnabled())
                .airportPickupEnabled(entity.isAirportPickupEnabled())
                .wheelchairAccessEnabled(entity.isWheelchairAccessEnabled())
                .build();
    }

    public static AmenitiesResponseDTO toAmenitiesResponse(PropertyAmenitiesConfiguration entity, UUID propertyId) {
        return AmenitiesResponseDTO.builder()
                .propertyId(propertyId)
                .airportCode(entity.getAirportCode())
                .distanceJourneyTime(entity.getDistanceJourneyTime())
                .directions(entity.getDirections())
                .groundTransportEnabled(entity.isGroundTransportEnabled())
                .shuttleServiceEnabled(entity.isShuttleServiceEnabled())
                .swimmingPoolEnabled(entity.isSwimmingPoolEnabled())
                .build();
    }

    public static ContentConfigurationResponseDTO toContentConfigurationResponse(
            UUID propertyId,
            PropertySpecialRequestsConfiguration specialRequests,
            PropertyAmenitiesConfiguration amenities
    ) {
        return ContentConfigurationResponseDTO.builder()
                .propertyId(propertyId)
                .specialRequests(toSpecialRequestsResponse(specialRequests, propertyId))
                .amenities(toAmenitiesResponse(amenities, propertyId))
                .build();
    }

    public static List<SpecialRequestOptionDTO> predefinedOptions() {
        return List.of(
                option("EXTRA_PILLOW", "Extra Pillow"),
                option("BABY_CRIB", "Baby Crib"),
                option("LATE_CHECK_OUT", "Late Check-out"),
                option("HYPOALLERGENIC_BEDDING", "Hypoallergenic Bedding"),
                option("AIRPORT_PICKUP", "Airport Pickup"),
                option("WHEELCHAIR_ACCESS", "Wheelchair Access")
        );
    }

    private static SpecialRequestOptionDTO option(String code, String displayName) {
        return SpecialRequestOptionDTO.builder()
                .code(code)
                .displayName(displayName)
                .build();
    }
}
