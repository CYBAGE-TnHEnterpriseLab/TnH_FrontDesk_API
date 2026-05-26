package com.frontdesk.pms.content.mapper;

import com.frontdesk.common.dto.PropertyDTO;
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

    public static SpecialRequestsResponseDTO toSpecialRequestsResponse(PropertySpecialRequestsConfiguration entity) {
        return SpecialRequestsResponseDTO.builder()
                .extraPillowEnabled(entity.isExtraPillowEnabled())
                .babyCribEnabled(entity.isBabyCribEnabled())
                .lateCheckOutEnabled(entity.isLateCheckOutEnabled())
                .hypoallergenicBeddingEnabled(entity.isHypoallergenicBeddingEnabled())
                .airportPickupEnabled(entity.isAirportPickupEnabled())
                .wheelchairAccessEnabled(entity.isWheelchairAccessEnabled())
                .build();
    }

    public static SpecialRequestsResponseDTO toSpecialRequestsResponse(
            PropertySpecialRequestsConfiguration entity,
            UUID propertyId
    ) {
        return toSpecialRequestsResponse(entity);
    }

    public static AmenitiesResponseDTO toAmenitiesResponse(PropertyAmenitiesConfiguration entity) {
        return AmenitiesResponseDTO.builder()
                .airportCode(entity.getAirportCode())
                .distanceJourneyTime(entity.getDistanceJourneyTime())
                .directions(entity.getDirections())
                .groundTransportEnabled(entity.isGroundTransportEnabled())
                .shuttleServiceEnabled(entity.isShuttleServiceEnabled())
                .swimmingPoolEnabled(entity.isSwimmingPoolEnabled())
                .build();
    }

    public static AmenitiesResponseDTO toAmenitiesResponse(
            PropertyAmenitiesConfiguration entity,
            UUID propertyId
    ) {
        return toAmenitiesResponse(entity);
    }

    public static ContentConfigurationResponseDTO toContentConfigurationResponse(
            PropertyDTO property,
            PropertySpecialRequestsConfiguration specialRequests,
            PropertyAmenitiesConfiguration amenities
    ) {
        return ContentConfigurationResponseDTO.builder()
                .propertyId(property.getId())
                .contactName(property.getContactName())
                .email(property.getEmail())
                .specialRequests(toSpecialRequestsResponse(specialRequests))
                .amenities(toAmenitiesResponse(amenities))
                .build();
    }

    public static ContentConfigurationResponseDTO toContentConfigurationResponse(
            UUID propertyId,
            String contactName,
            String email,
            PropertySpecialRequestsConfiguration specialRequests,
            PropertyAmenitiesConfiguration amenities
    ) {
        return ContentConfigurationResponseDTO.builder()
                .propertyId(propertyId)
                .contactName(contactName)
                .email(email)
                .specialRequests(toSpecialRequestsResponse(specialRequests, propertyId))
                .amenities(toAmenitiesResponse(amenities, propertyId))
                .build();
    }

    public static ContentConfigurationResponseDTO toContentConfigurationResponse(
            UUID propertyId,
            PropertySpecialRequestsConfiguration specialRequests,
            PropertyAmenitiesConfiguration amenities
    ) {
        return toContentConfigurationResponse(propertyId, null, null, specialRequests, amenities);
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
