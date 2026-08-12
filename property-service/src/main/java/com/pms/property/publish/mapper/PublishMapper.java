package com.pms.property.publish.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.property.common.exception.BadRequestException;
import com.pms.property.domain.content.entity.GuestServiceAmenityEntity;
import com.pms.property.domain.content.entity.NearbyLocationAccessibilityEntity;
import com.pms.property.domain.content.entity.PropertyOverviewEntity;
import com.pms.property.domain.finance.entity.ChartOfAccountEntity;
import com.pms.property.domain.finance.entity.RevenueMappingEntity;
import com.pms.property.domain.payment.entity.PaymentMethodEntity;
import com.pms.property.domain.property.entity.PropertyEntity;
import com.pms.property.domain.room.entity.FloorConfigurationEntity;
import com.pms.property.domain.room.entity.FloorPropertyAreaEntity;
import com.pms.property.domain.room.entity.InventoryRoomEntity;
import com.pms.property.domain.room.entity.PropertyAreaEntity;
import com.pms.property.domain.room.entity.RoomOutletTypeEntity;
import com.pms.property.domain.tax.entity.TaxRuleEntity;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PublishMapper {

    private final ObjectMapper objectMapper;

    public PublishMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public NormalizedPublishData toNormalized(String wizardJson) {
        try {
            JsonNode root = objectMapper.readTree(wizardJson);
            JsonNode propertyNode = root.path("propertyDetails");
            JsonNode contentNode = root.path("content");
            JsonNode roomsAndOutletsNode = firstObjectNode(root, "roomsAndOutlets", "roomAndOutlets", "roomsOutlets");

            PropertyEntity property = new PropertyEntity();
            property.setTitle(firstText(propertyNode, "name", "propertyName"));
            property.setPropertyCode(firstText(propertyNode, "propertyCode"));
            property.setPropertyType(firstText(propertyNode, "propertyType"));
            property.setTotalNoOfRooms(firstInt(propertyNode, "totalNoOfRooms", "totalRooms"));
            property.setTotalNoOfFloors(firstInt(propertyNode, "totalNoOfFloors", "totalFloors"));
            property.setAddress(resolveAddress(propertyNode));
            property.setCity(firstText(propertyNode, "city"));
            property.setState(firstText(propertyNode, "state"));
            property.setCountry(firstText(propertyNode, "country"));
            property.setZipCode(firstText(propertyNode, "zipCode", "postalCode", "zipPostalCode"));
            //new
            property.setLatitude(firstBigDecimal(propertyNode, "latitude"));
            property.setLongitude(firstBigDecimal(propertyNode, "longitude"));
            property.setWebsite(firstText(propertyNode, "website"));
            property.setContactName(firstText(propertyNode, "contactName", "primaryContactName"));
            property.setContactNumber(firstText(propertyNode, "contactNumber", "phoneNumber", "phone"));
            property.setTimeZone(firstText(propertyNode, "timeZone"));
            property.setNightAuditTime(firstText(propertyNode, "nightAuditTime"));
            property.setCheckInTime(firstText(propertyNode, "checkInTime"));
            property.setCheckOutTime(firstText(propertyNode, "checkOutTime"));
            property.setStatus("ACTIVE");
            property.setCreatedAt(Instant.now());

            PropertyOverviewEntity propertyOverview = mapPropertyOverview(contentNode);
            List<GuestServiceAmenityEntity> guestServiceAmenities = mapGuestServiceAmenities(contentNode);
            List<NearbyLocationAccessibilityEntity> nearbyLocationAccessibility = mapNearbyLocationAccessibility(contentNode);
            List<PropertyAreaEntity> propertyAreas = mapPropertyAreas(roomsAndOutletsNode);
            List<RoomOutletTypeEntity> roomOutletTypes = mapRoomOutletTypes(roomsAndOutletsNode);

            List<FloorConfigurationEntity> floors = new ArrayList<>();
            List<FloorPropertyAreaEntity> floorPropertyAreas = new ArrayList<>();
            List<InventoryRoomEntity> inventoryRooms = new ArrayList<>();
            JsonNode floorConfigurationNode = firstObjectNode(root, "floorConfiguration", "roomConfiguration");
            for (JsonNode floorNode : floorConfigurationNode.path("floors")) {
                String floorName = firstText(floorNode, "floorName", "name", "label");
                JsonNode floorRoomTypes = floorNode.path("roomTypes");

                if (floorRoomTypes.isArray() && !floorRoomTypes.isEmpty()) {
                    for (JsonNode floorRoomTypeNode : floorRoomTypes) {
                        JsonNode manualRoomNumbers = floorRoomTypeNode.path("roomNumbers");
                        int configuredRoomCount = firstInt(floorRoomTypeNode, "roomCount", "quantity", "qty", "count");
                        int manualRoomCount = countRoomNumbers(manualRoomNumbers);

                        FloorConfigurationEntity floor = new FloorConfigurationEntity();
                        floor.setFloorName(floorName);
                        floor.setRoomTypeName(firstText(floorRoomTypeNode, "roomTypeCode", "code", "roomTypeName", "name", "roomName"));
                        floor.setRoomCount(configuredRoomCount > 0 ? configuredRoomCount : manualRoomCount);
                        floor.setStartNumber(resolveStartNumber(floorRoomTypeNode, manualRoomNumbers));
                        floors.add(floor);

                        appendInventoryRooms(inventoryRooms, floorName, floor.getRoomTypeName(), manualRoomNumbers);
                    }
                } else {
                    JsonNode manualRoomNumbers = floorNode.path("roomNumbers");
                    int configuredRoomCount = firstInt(floorNode, "roomCount", "quantity", "qty", "count");
                    int manualRoomCount = countRoomNumbers(manualRoomNumbers);

                    FloorConfigurationEntity floor = new FloorConfigurationEntity();
                    floor.setFloorName(floorName);
                    floor.setRoomTypeName(firstText(floorNode, "roomTypeCode", "code", "roomTypeName", "name", "roomName"));
                    floor.setRoomCount(configuredRoomCount > 0 ? configuredRoomCount : manualRoomCount);
                    floor.setStartNumber(resolveStartNumber(floorNode, manualRoomNumbers));
                    floors.add(floor);

                    appendInventoryRooms(inventoryRooms, floorName, floor.getRoomTypeName(), manualRoomNumbers);
                }

                for (JsonNode propertyAreaNode : floorNode.path("propertyAreas")) {
                    FloorPropertyAreaEntity floorPropertyArea = new FloorPropertyAreaEntity();
                    floorPropertyArea.setFloorName(floorName);
                    floorPropertyArea.setAreaName(firstText(propertyAreaNode, "propertyAreaCode", "areaName", "name", "code"));
                    floorPropertyArea.setQuantity(firstInt(propertyAreaNode, "quantity", "qty", "count"));
                    floorPropertyAreas.add(floorPropertyArea);
                }
            }

            List<ChartOfAccountEntity> chartOfAccounts = new ArrayList<>();
            for (JsonNode accountNode : root.path("finance").path("chartOfAccounts")) {
                ChartOfAccountEntity chartOfAccount = new ChartOfAccountEntity();
                chartOfAccount.setAccountCode(accountNode.path("accountCode").asText());
                chartOfAccount.setAccountName(accountNode.path("accountName").asText());
                chartOfAccount.setAccountType(accountNode.path("accountType").asText());
                chartOfAccount.setLedgerType(accountNode.path("ledgerType").asText());
                chartOfAccount.setActive(accountNode.path("active").asBoolean());
                chartOfAccounts.add(chartOfAccount);
            }

            List<RevenueMappingEntity> revenueMappings = new ArrayList<>();
            for (JsonNode mappingNode : root.path("finance").path("revenueMappings")) {
                RevenueMappingEntity mapping = new RevenueMappingEntity();
                mapping.setChargeType(mappingNode.path("chargeType").asText());
                mapping.setMapGlAccount(mappingNode.path("mapGlAccount").asText());
                mapping.setStatus(mappingNode.path("status").asText());
                mapping.setDescription(mappingNode.path("description").asText());
                revenueMappings.add(mapping);
            }

            List<PaymentMethodEntity> paymentMethods = new ArrayList<>();
            for (JsonNode methodNode : root.path("payments").path("methods")) {
                PaymentMethodEntity paymentMethod = new PaymentMethodEntity();
                paymentMethod.setPaymentMethod(methodNode.path("paymentMethod").asText());
                paymentMethod.setAccountMapping(methodNode.path("accountMapping").asText());
                paymentMethod.setAllowRefund(methodNode.path("allowRefund").asBoolean());
                paymentMethod.setActive(methodNode.path("active").asBoolean());
                paymentMethods.add(paymentMethod);
            }

            List<TaxRuleEntity> taxRules = new ArrayList<>();
            for (JsonNode taxRuleNode : root.path("taxes").path("rules")) {
                TaxRuleEntity rule = new TaxRuleEntity();
                rule.setTaxName(taxRuleNode.path("taxName").asText());
                rule.setType(taxRuleNode.path("type").asText());
                rule.setRate(taxRuleNode.path("rate").asDouble());
                rule.setApplicableOn(taxRuleNode.path("applicableOn").asText());
                rule.setInclExcl(taxRuleNode.path("inclExcl").asText());
                rule.setEffectiveDate(taxRuleNode.path("effectiveDate").asText());
                rule.setActive(taxRuleNode.path("active").asBoolean());
                rule.setStatus(taxRuleNode.path("status").asText());
                rule.setPriority(taxRuleNode.path("priority").asInt());
                taxRules.add(rule);
            }

            return new NormalizedPublishData(
                root,
                property,
                propertyOverview,
                guestServiceAmenities,
                nearbyLocationAccessibility,
                propertyAreas,
                roomOutletTypes,
                floors,
                floorPropertyAreas,
                inventoryRooms,
                chartOfAccounts,
                revenueMappings,
                paymentMethods,
                taxRules
            );
        } catch (IOException ex) {
            throw new BadRequestException("Draft JSON is not valid");
        }
    }

    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.path(field);
            if (!value.isMissingNode() && !value.isNull()) {
                String text = value.asText().trim();
                if (!text.isBlank()) {
                    return text;
                }
            }
        }
        return "";
    }

    private BigDecimal firstBigDecimal(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.path(field);

            if (!value.isMissingNode() && !value.isNull()) {
                if (value.isNumber()) {
                    return value.decimalValue();
                }

                String text = value.asText().trim();

                if (!text.isBlank()) {
                    try {
                        return new BigDecimal(text);
                    } catch (NumberFormatException ignored) {
                        return null;
                    }
                }
            }
        }

        return null;
    }

    private String resolveAddress(JsonNode propertyNode) {
        String legacyAddress = firstText(propertyNode, "address");
        if (!legacyAddress.isBlank()) {
            return legacyAddress;
        }

        String addressLine1 = firstText(propertyNode, "addressLine1");
        String addressLine2 = firstText(propertyNode, "addressLine2");
        if (addressLine2.isBlank()) {
            return addressLine1;
        }
        return addressLine1 + ", " + addressLine2;
    }

    private PropertyOverviewEntity mapPropertyOverview(JsonNode contentNode) {
        JsonNode propertyOverviewNode = contentNode.path("propertyOverview");
        if (!propertyOverviewNode.isObject()) {
            return null;
        }

        String propertyDescription = firstText(propertyOverviewNode, "propertyDescription", "description");
        String propertyHeroImage = firstText(propertyOverviewNode, "propertyHeroImage", "propertyImage", "image", "imageUrl");
        if (propertyHeroImage.isBlank()) {
            propertyHeroImage = firstText(propertyOverviewNode.path("propertyHeroImage"), "url", "key", "id");
        }

        if (propertyDescription.isBlank() && propertyHeroImage.isBlank()) {
            return null;
        }

        PropertyOverviewEntity overview = new PropertyOverviewEntity();
        overview.setPropertyDescription(propertyDescription);
        overview.setPropertyHeroImage(propertyHeroImage);
        return overview;
    }

    private List<GuestServiceAmenityEntity> mapGuestServiceAmenities(JsonNode contentNode) {
        List<GuestServiceAmenityEntity> result = new ArrayList<>();
        JsonNode guestServicesAndAmenities = contentNode.path("guestServicesAndAmenities");

        appendGuestServiceAmenitySection(result, guestServicesAndAmenities.path("standardAmenities"), "STANDARD_AMENITIES");
        appendGuestServiceAmenitySection(result, guestServicesAndAmenities.path("paidServices"), "PAID_SERVICES");
        appendGuestServiceAmenitySection(result, guestServicesAndAmenities.path("onDemandServices"), "ON_DEMAND_SERVICES");

        return result;
    }

    private void appendGuestServiceAmenitySection(
        List<GuestServiceAmenityEntity> target,
        JsonNode items,
        String section
    ) {
        if (!items.isArray()) {
            return;
        }

        for (JsonNode itemNode : items) {
            String code = firstText(itemNode, "code", "name", "label");
            if (code.isBlank()) {
                continue;
            }

            GuestServiceAmenityEntity entity = new GuestServiceAmenityEntity();
            entity.setSection(section);
            entity.setCode(code);
            entity.setEnabled(itemNode.path("enabled").asBoolean(true));
            target.add(entity);
        }
    }

    private List<NearbyLocationAccessibilityEntity> mapNearbyLocationAccessibility(JsonNode contentNode) {
        List<NearbyLocationAccessibilityEntity> result = new ArrayList<>();
        JsonNode nearbyLocationAccessibility = contentNode.path("nearbyLocationAccessibility");

        appendNearbyLocationSection(
            result,
            nearbyLocationAccessibility.path("transportConnectivity"),
            "TRANSPORT_CONNECTIVITY"
        );
        appendNearbyLocationSection(
            result,
            nearbyLocationAccessibility.path("otherLandmarks"),
            "OTHER_LANDMARKS"
        );

        return result;
    }

    private void appendNearbyLocationSection(
        List<NearbyLocationAccessibilityEntity> target,
        JsonNode items,
        String section
    ) {
        if (!items.isArray()) {
            return;
        }

        for (JsonNode itemNode : items) {
            String locationType = firstText(itemNode, "type", "transportType", "landmarkType");
            String locationName = firstText(itemNode, "name", "transportName", "landmarkName");
            if (locationType.isBlank() || locationName.isBlank()) {
                continue;
            }

            NearbyLocationAccessibilityEntity entity = new NearbyLocationAccessibilityEntity();
            entity.setSection(section);
            entity.setLocationType(locationType);
            entity.setLocationName(locationName);
            entity.setDistanceValue(firstDouble(itemNode, "distance", "distanceValue"));
            entity.setDistanceUnit(firstText(itemNode, "distanceUnit"));
            entity.setTravelTimeValue(firstInt(itemNode, "travelTime", "travelTimeValue"));
            entity.setTravelTimeUnit(firstText(itemNode, "travelTimeUnit"));
            target.add(entity);
        }
    }

    private int firstInt(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.path(field);
            if (!value.isMissingNode() && !value.isNull()) {
                if (value.isNumber()) {
                    return value.asInt();
                }
                String text = value.asText().trim();
                if (!text.isBlank()) {
                    try {
                        return Integer.parseInt(text);
                    } catch (NumberFormatException ignored) {
                        return 0;
                    }
                }
            }
        }
        return 0;
    }

    private double firstDouble(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.path(field);
            if (!value.isMissingNode() && !value.isNull()) {
                if (value.isNumber()) {
                    return value.asDouble();
                }
                String text = value.asText().trim();
                if (!text.isBlank()) {
                    try {
                        return Double.parseDouble(text);
                    } catch (NumberFormatException ignored) {
                        return 0.0;
                    }
                }
            }
        }
        return 0.0;
    }

    private boolean firstBoolean(JsonNode node, boolean defaultValue, String... fields) {
        for (String field : fields) {
            JsonNode value = node.path(field);
            if (!value.isMissingNode() && !value.isNull()) {
                if (value.isBoolean()) {
                    return value.asBoolean();
                }
                String text = value.asText().trim();
                if (!text.isBlank()) {
                    return Boolean.parseBoolean(text);
                }
            }
        }
        return defaultValue;
    }

    private JsonNode firstObjectNode(JsonNode parent, String... fields) {
        for (String field : fields) {
            JsonNode node = parent.path(field);
            if (node.isObject()) {
                return node;
            }
        }
        return objectMapper.createObjectNode();
    }

    private List<PropertyAreaEntity> mapPropertyAreas(JsonNode roomsAndOutletsNode) {
        List<PropertyAreaEntity> result = new ArrayList<>();
        JsonNode propertyAreas = roomsAndOutletsNode.path("propertyAreas");
        if (!propertyAreas.isArray()) {
            return result;
        }

        for (JsonNode areaNode : propertyAreas) {
            String areaName = firstText(areaNode, "areaName", "name");
            if (areaName.isBlank()) {
                continue;
            }

            PropertyAreaEntity entity = new PropertyAreaEntity();
            entity.setAreaName(areaName);
            entity.setQuantity(firstInt(areaNode, "quantity", "qty", "count"));
            entity.setMaximumCapacity(firstInt(areaNode, "maximumCapacity", "maxCapacity", "occupancy"));
            entity.setDescription(firstText(areaNode, "description"));
            entity.setAmenitiesCsv(joinAsCsv(areaNode.path("amenities")));
            entity.setImagesCsv(joinAsCsv(areaNode.path("images")));
            result.add(entity);
        }
        return result;
    }

    private List<RoomOutletTypeEntity> mapRoomOutletTypes(JsonNode roomsAndOutletsNode) {
        List<RoomOutletTypeEntity> result = new ArrayList<>();
        JsonNode roomOutletTypes = roomsAndOutletsNode.path("roomTypes");
        if (!roomOutletTypes.isArray()) {
            return result;
        }

        for (JsonNode roomNode : roomOutletTypes) {
            String roomName = firstText(roomNode, "roomName", "name");
            if (roomName.isBlank()) {
                continue;
            }

            RoomOutletTypeEntity entity = new RoomOutletTypeEntity();
            entity.setRoomName(roomName);
            entity.setRoomCode(firstText(roomNode, "roomCode", "roomTypeCode", "code"));
            entity.setQuantity(firstInt(roomNode, "quantity", "qty", "count"));
            entity.setAvailableForSell(roomNode.path("availableForSell").asBoolean(true));
            entity.setMaximumGuestOccupancy(firstInt(roomNode, "maximumGuestOccupancy", "maxOccupancy", "occupancy"));
            entity.setDescription(firstText(roomNode, "description"));
            entity.setAmenitiesCsv(joinAsCsv(roomNode.path("amenities")));
            entity.setImagesCsv(joinAsCsv(roomNode.path("images")));
            result.add(entity);
        }
        return result;
    }

    private String joinAsCsv(JsonNode items) {
        if (!items.isArray()) {
            return "";
        }

        List<String> values = new ArrayList<>();
        for (JsonNode item : items) {
            if (item.isTextual()) {
                String text = item.asText().trim();
                if (!text.isBlank()) {
                    values.add(text);
                }
            } else if (item.isObject()) {
                String value = firstText(item, "value", "name", "code", "url", "id");
                if (!value.isBlank()) {
                    values.add(value);
                }
            }
        }
        return String.join(",", values);
    }

    private int countRoomNumbers(JsonNode roomNumbersNode) {
        if (!roomNumbersNode.isArray()) {
            return 0;
        }

        int count = 0;
        for (JsonNode roomNumberNode : roomNumbersNode) {
            String roomNumber = roomNumberNode.asText().trim();
            if (!roomNumber.isBlank()) {
                count++;
            }
        }
        return count;
    }

    private Integer resolveStartNumber(JsonNode floorNode, JsonNode roomNumbersNode) {
        int explicitStart = firstInt(floorNode, "startNumber");
        if (explicitStart > 0) {
            return explicitStart;
        }

        if (!roomNumbersNode.isArray() || roomNumbersNode.isEmpty()) {
            return 0;
        }

        Integer minParsed = null;
        for (JsonNode roomNumberNode : roomNumbersNode) {
            try {
                int parsed = Integer.parseInt(roomNumberNode.asText().trim());
                if (minParsed == null || parsed < minParsed) {
                    minParsed = parsed;
                }
            } catch (NumberFormatException ignored) {
                // Non-numeric room numbers are allowed; use 0 fallback.
            }
        }
        return minParsed != null ? minParsed : 0;
    }

    private void appendInventoryRooms(
        List<InventoryRoomEntity> target,
        String floorName,
        String roomTypeName,
        JsonNode manualRoomNumbers
    ) {
        if (!manualRoomNumbers.isArray() || manualRoomNumbers.isEmpty()) {
            return;
        }

        for (JsonNode roomNumberNode : manualRoomNumbers) {
            String roomNumber = roomNumberNode.asText().trim();
            if (roomNumber.isBlank()) {
                continue;
            }

            InventoryRoomEntity room = new InventoryRoomEntity();
            room.setFloorName(floorName);
            room.setRoomTypeName(roomTypeName);
            room.setRoomNumber(roomNumber);
            target.add(room);
        }
    }

    public record NormalizedPublishData(
        JsonNode root,
        PropertyEntity property,
        PropertyOverviewEntity propertyOverview,
        List<GuestServiceAmenityEntity> guestServiceAmenities,
        List<NearbyLocationAccessibilityEntity> nearbyLocationAccessibility,
        List<PropertyAreaEntity> propertyAreas,
        List<RoomOutletTypeEntity> roomOutletTypes,
        List<FloorConfigurationEntity> floors,
        List<FloorPropertyAreaEntity> floorPropertyAreas,
        List<InventoryRoomEntity> inventoryRooms,
        List<ChartOfAccountEntity> chartOfAccounts,
        List<RevenueMappingEntity> revenueMappings,
        List<PaymentMethodEntity> paymentMethods,
        List<TaxRuleEntity> taxRules
    ) {
    }
}


