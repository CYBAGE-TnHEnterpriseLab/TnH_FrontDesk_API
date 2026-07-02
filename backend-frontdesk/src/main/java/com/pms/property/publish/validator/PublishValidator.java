package com.pms.property.publish.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.pms.property.common.exception.BadRequestException;
import com.pms.property.common.exception.ValidationException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class PublishValidator {

    private static final Pattern ROOM_CODE_PATTERN = Pattern.compile("^[A-Z_]{2,20}$");

    public void validate(JsonNode root) {
        JsonNode propertyDetails = root.path("propertyDetails");
        JsonNode roomsAndOutlets = firstObjectNode(root, "roomsAndOutlets", "roomAndOutlets", "roomsOutlets");
        validatePropertyDetails(propertyDetails);
        AllocationLimits allocationLimits = validateRoomsAndOutlets(
            roomsAndOutlets,
            firstInt(propertyDetails, "totalNoOfRooms", "totalRooms")
        );
        JsonNode floorConfiguration = firstObjectNode(root, "floorConfiguration", "roomConfiguration");
        validateRoomConfiguration(floorConfiguration, allocationLimits);
        validateFinance(root.path("finance"));
        validatePayments(root.path("payments"));
        validateTaxes(root.path("taxes"));
    }

    private AllocationLimits validateRoomsAndOutlets(JsonNode roomsAndOutlets, int totalRoomsFromPropertyDetails) {
        Map<String, Integer> roomTypeLimits = new HashMap<>();
        Map<String, Integer> propertyAreaLimits = new HashMap<>();
        JsonNode propertyAreas = roomsAndOutlets.path("propertyAreas");
        if (propertyAreas.isArray()) {
            Set<String> propertyAreaNames = new HashSet<>();
            for (JsonNode propertyArea : propertyAreas) {
                String propertyAreaKey = resolvePropertyAreaKey(propertyArea, "roomsAndOutlets.propertyAreas[]");
                if (!propertyAreaNames.add(propertyAreaKey)) {
                    throw new BadRequestException("Property area name must be unique and non-empty");
                }
                int quantity = firstInt(propertyArea, "quantity", "qty", "count");
                if (quantity <= 0) {
                    throw new BadRequestException("Property area quantity must be greater than zero");
                }
                propertyAreaLimits.put(propertyAreaKey, quantity);

                int maximumCapacity = firstInt(propertyArea, "maximumCapacity", "maxCapacity", "occupancy");
                if (maximumCapacity <= 0) {
                    throw new BadRequestException("Property area maximumCapacity must be greater than zero");
                }
            }
        }

        JsonNode roomTypes = roomsAndOutlets.path("roomTypes");
        if (roomTypes.isArray()) {
            int totalRoomTypeQuantity = 0;
            Set<String> roomTypeCodes = new HashSet<>();
            for (JsonNode roomType : roomTypes) {
                requireAny(roomType, "roomTypeCode", "code", "roomName", "name");

                String roomCode = firstText(roomType, "roomCode", "roomTypeCode", "code");
                if (roomCode.isBlank()) {
                    throw new BadRequestException("roomCode is required in roomsAndOutlets.roomTypes");
                }
                if (!ROOM_CODE_PATTERN.matcher(roomCode).matches()) {
                    throw new BadRequestException("roomCode must match ^[A-Z_]{2,20}$");
                }
                if (!roomTypeCodes.add(roomCode)) {
                    throw new BadRequestException("roomCode must be unique in roomsAndOutlets.roomTypes");
                }

                int quantity = firstInt(roomType, "quantity", "qty", "count");
                if (quantity <= 0) {
                    throw new BadRequestException("Room type quantity must be greater than zero");
                }
                totalRoomTypeQuantity += quantity;
                String roomTypeKey = resolveRoomTypeKey(roomType, "roomsAndOutlets.roomTypes[]");
                roomTypeLimits.put(roomTypeKey, quantity);

                int maximumGuestOccupancy = firstInt(roomType, "maximumGuestOccupancy", "maxOccupancy", "occupancy");
                if (maximumGuestOccupancy <= 0) {
                    throw new BadRequestException("Room type maximumGuestOccupancy must be greater than zero");
                }
            }

            if (totalRoomTypeQuantity > 0 && totalRoomTypeQuantity != totalRoomsFromPropertyDetails) {
                throw new BadRequestException("Sum of room type quantities must equal totalNoOfRooms");
            }
        }

        return new AllocationLimits(roomTypeLimits, propertyAreaLimits);
    }

    private void validatePropertyDetails(JsonNode propertyDetails) {
        requireAny(propertyDetails, "name", "propertyName");
        requireAny(propertyDetails, "propertyType", "type");
        require(propertyDetails, "propertyCode");
        requireAny(propertyDetails, "totalNoOfRooms", "totalRooms");
        requireAny(propertyDetails, "totalNoOfFloors", "totalFloors");
        requireAny(propertyDetails, "address", "addressLine1");
        require(propertyDetails, "city");
        require(propertyDetails, "state");
        require(propertyDetails, "country");
        requireAny(propertyDetails, "zipCode", "postalCode", "zipPostalCode");
        requireAny(propertyDetails, "contactName", "primaryContactName");
        requireAny(propertyDetails, "contactNumber", "phoneNumber", "phone");
        require(propertyDetails, "timeZone");
        require(propertyDetails, "nightAuditTime");
        require(propertyDetails, "checkInTime");
        require(propertyDetails, "checkOutTime");

        int totalRooms = firstInt(propertyDetails, "totalNoOfRooms", "totalRooms");
        if (totalRooms <= 0) {
            throw new BadRequestException("totalNoOfRooms must be greater than zero");
        }

        int totalFloors = firstInt(propertyDetails, "totalNoOfFloors", "totalFloors");
        if (totalFloors <= 0) {
            throw new BadRequestException("totalNoOfFloors must be greater than zero");
        }

        String address = firstText(propertyDetails, "address", "addressLine1");
        if (address.length() < 10) {
            throw new BadRequestException("Address must have at least 10 characters");
        }

        String website = firstText(propertyDetails, "website");
        if (!website.isBlank() && !website.contains(".")) {
            throw new BadRequestException("Invalid website value");
        }

        String checkIn = firstText(propertyDetails, "checkInTime");
        String checkOut = firstText(propertyDetails, "checkOutTime");
        parseTime(checkIn, "checkInTime");
        parseTime(checkOut, "checkOutTime");
        parseTime(firstText(propertyDetails, "nightAuditTime"), "nightAuditTime");
        if (checkIn.equals(checkOut)) {
            throw new BadRequestException("checkOutTime must differ from checkInTime");
        }
    }

    private void validateRoomConfiguration(JsonNode roomConfiguration, AllocationLimits limits) {
        JsonNode floors = roomConfiguration.path("floors");
        if (limits.roomTypeLimits().isEmpty()) {
            throw new BadRequestException("At least one room type is required");
        }
        if (!floors.isArray() || floors.isEmpty()) {
            throw new BadRequestException("At least one floor configuration is required");
        }

        // roomConfiguration.roomTypes is optional; authoritative configured room types come from roomsAndOutlets.roomTypes.
        Set<String> roomTypeNames = new HashSet<>(limits.roomTypeLimits().keySet());

        Set<String> roomNumbers = new HashSet<>();
        Map<String, Integer> assignedPerRoomType = new HashMap<>();
        Map<String, Integer> assignedPerPropertyArea = new HashMap<>();
        int floorIndex = 0;
        for (JsonNode floor : floors) {
            requireAny(floor, "floorName", "name", "label");

            JsonNode nestedRoomTypes = floor.path("roomTypes");
            if (nestedRoomTypes.isArray() && !nestedRoomTypes.isEmpty()) {

                int roomTypeIndex = 0;

                for (JsonNode floorRoomType : nestedRoomTypes) {
                    String pathPrefix =
                            "roomConfiguration.floors[" + floorIndex + "].roomTypes[" + roomTypeIndex + "]";

                    validateFloorRoomTypeAssignment(
                            floorRoomType,
                            pathPrefix,
                            roomTypeNames,
                            roomNumbers,
                            assignedPerRoomType,
                            limits.roomTypeLimits()
                    );

                    roomTypeIndex++;
                }

            }

            JsonNode propertyAreas = floor.path("propertyAreas");
            if (propertyAreas.isArray()) {
                int propertyAreaIndex = 0;
                for (JsonNode floorPropertyArea : propertyAreas) {
                    String propertyAreaName = resolvePropertyAreaKey(
                        floorPropertyArea,
                        "roomConfiguration.floors[" + floorIndex + "].propertyAreas[" + propertyAreaIndex + "]"
                    );
                    if (!limits.propertyAreaLimits().containsKey(propertyAreaName)) {
                        throw validationError(
                            "UNKNOWN_PROPERTY_AREA",
                            "roomConfiguration.floors[" + floorIndex + "].propertyAreas[" + propertyAreaIndex + "]",
                            "Floor mapped to unknown property area: " + propertyAreaName
                        );
                    }

                    int assignedAreaCount = firstInt(floorPropertyArea, "quantity", "qty", "count");
                    if (assignedAreaCount <= 0) {
                        throw validationError(
                            "MISSING_PROPERTY_AREA_ASSIGNMENT",
                            "roomConfiguration.floors[" + floorIndex + "].propertyAreas[" + propertyAreaIndex + "]",
                            "Each property area assignment must include positive quantity"
                        );
                    }

                    int totalAreaAssigned = assignedPerPropertyArea.getOrDefault(propertyAreaName, 0) + assignedAreaCount;
                    assignedPerPropertyArea.put(propertyAreaName, totalAreaAssigned);

                    int configuredAreaLimit = limits.propertyAreaLimits().get(propertyAreaName);
                    if (totalAreaAssigned > configuredAreaLimit) {
                        throw validationError(
                            "PROPERTY_AREA_QUANTITY_EXCEEDED",
                            "roomConfiguration.floors[" + floorIndex + "].propertyAreas[" + propertyAreaIndex + "]",
                            "Assigned property areas exceed configured quantity for area: " + propertyAreaName
                        );
                    }
                    propertyAreaIndex++;
                }
            }

            floorIndex++;
        }

        for (String configuredRoomType : roomTypeNames) {
            if (assignedPerRoomType.getOrDefault(configuredRoomType, 0) <= 0) {
                throw validationError(
                    "ROOM_TYPE_UNASSIGNED",
                    "roomConfiguration.roomTypes",
                    "At least one room must be assigned for room type: " + configuredRoomType
                );
            }
        }
    }

    private ValidationException validationError(String code, String fieldPath, String message) {
        return new ValidationException(code, fieldPath, message);
    }

    private void validateFloorRoomTypeAssignment(
        JsonNode floorRoomType,
        String fieldPath,
        Set<String> roomTypeNames,
        Set<String> roomNumbers,
        Map<String, Integer> assignedPerRoomType,
        Map<String, Integer> roomTypeLimits
    ) {
        String roomTypeName = resolveRoomTypeKey(floorRoomType, fieldPath);
        if (!roomTypeNames.contains(roomTypeName)) {
            throw validationError(
                "UNKNOWN_ROOM_TYPE",
                fieldPath,
                "Floor mapped to unknown room type: " + roomTypeName
            );
        }

        int assignedCount = resolveAssignedRoomCount(floorRoomType, fieldPath, roomNumbers);
        int totalAssigned = assignedPerRoomType.getOrDefault(roomTypeName, 0) + assignedCount;
        assignedPerRoomType.put(roomTypeName, totalAssigned);

        Integer configuredLimit = roomTypeLimits.get(roomTypeName);
        if (configuredLimit != null && totalAssigned > configuredLimit) {
            throw validationError(
                "ROOM_TYPE_QUANTITY_EXCEEDED",
                fieldPath,
                "Assigned rooms exceed configured quantity for room type: " + roomTypeName
            );
        }
    }

    private int resolveAssignedRoomCount(JsonNode floorRoomType, String fieldPath, Set<String> roomNumbers) {
        JsonNode explicitRooms = floorRoomType.path("roomNumbers");
        if (explicitRooms.isArray() && !explicitRooms.isEmpty()) {
            int assignedCount = 0;
            for (JsonNode roomNumberNode : explicitRooms) {
                String roomNumber = roomNumberNode.asText().trim();
                if (roomNumber.isBlank() || !roomNumbers.add(roomNumber)) {
                    throw validationError(
                        "DUPLICATE_OR_BLANK_ROOM_NUMBER",
                        fieldPath + ".roomNumbers",
                        "Duplicate or blank room number found"
                    );
                }
                assignedCount++;
            }
            return assignedCount;
        }

        int assignedCount = firstInt(floorRoomType, "roomCount", "quantity", "qty", "count");
        if (assignedCount <= 0) {
            throw validationError(
                "MISSING_ROOM_ASSIGNMENT",
                fieldPath,
                "Each floor allocation must include roomNumbers or positive roomCount"
            );
        }
        return assignedCount;
    }

    private String resolveRoomTypeKey(JsonNode node, String fieldPath) {
        String key = firstText(node, "roomTypeCode", "code", "roomTypeName", "name", "roomName");
        if (key.isBlank()) {
            throw validationError("MISSING_ROOM_TYPE_KEY", fieldPath, "Room type identifier is required");
        }
        return key;
    }

    private String resolvePropertyAreaKey(JsonNode node, String fieldPath) {
        String key = firstText(node, "propertyAreaCode", "areaName", "name", "code");
        if (key.isBlank()) {
            throw validationError("MISSING_PROPERTY_AREA_KEY", fieldPath, "Property area identifier is required");
        }
        return key;
    }

    private void validateFinance(JsonNode finance) {
        JsonNode chartOfAccounts = finance.path("chartOfAccounts");
        JsonNode revenueMappings = finance.path("revenueMappings");
        if (!chartOfAccounts.isArray() || chartOfAccounts.isEmpty()) {
            throw new BadRequestException("At least one chart of account entry is required");
        }
        if (!revenueMappings.isArray() || revenueMappings.isEmpty()) {
            throw new BadRequestException("At least one revenue mapping is required");
        }

        Set<String> accountCodes = new HashSet<>();
        for (JsonNode account : chartOfAccounts) {
            String accountCode = account.path("accountCode").asText().trim();
            if (accountCode.isBlank()) {
                throw new BadRequestException("accountCode is required in chartOfAccounts");
            }

            String accountName = account.path("accountName").asText().trim();
            if (accountName.isBlank()) {
                throw new BadRequestException("accountName is required in chartOfAccounts");
            }

            String accountType = account.path("accountType").asText().trim();
            if (accountType.isBlank()) {
                throw new BadRequestException("accountType is required in chartOfAccounts");
            }

            String ledgerType = account.path("ledgerType").asText().trim();
            if (ledgerType.isBlank()) {
                throw new BadRequestException("ledgerType is required in chartOfAccounts");
            }

            if (account.path("active").isMissingNode() || account.path("active").isNull()) {
                throw new BadRequestException("active is required in chartOfAccounts");
            }

            accountCodes.add(accountCode);
        }

        for (JsonNode mapping : revenueMappings) {
            String chargeType = mapping.path("chargeType").asText().trim();
            if (chargeType.isBlank()) {
                throw new BadRequestException("chargeType is required in revenueMappings");
            }

            String mapGlAccount = mapping.path("mapGlAccount").asText().trim();
            if (!mapGlAccount.isBlank() && !"UNASSIGNED".equalsIgnoreCase(mapGlAccount) && !accountCodes.contains(mapGlAccount)) {
                throw new BadRequestException("Revenue mapping refers to unknown accountCode: " + mapGlAccount);
            }

            String status = mapping.path("status").asText().trim();
            if (status.isBlank()) {
                throw new BadRequestException("status is required in revenueMappings");
            }
        }
    }

    private void validatePayments(JsonNode payments) {
        JsonNode methods = payments.path("methods");
        if (!methods.isArray() || methods.isEmpty()) {
            throw new BadRequestException("At least one payment method is required");
        }

        for (JsonNode method : methods) {
            require(method, "paymentMethod");
            require(method, "accountMapping");
            if (method.path("allowRefund").isMissingNode() || method.path("allowRefund").isNull()) {
                throw new BadRequestException("allowRefund is required in payments.methods");
            }
            if (method.path("active").isMissingNode() || method.path("active").isNull()) {
                throw new BadRequestException("active is required in payments.methods");
            }
        }
    }

    private void validateTaxes(JsonNode taxes) {
        JsonNode rules = taxes.path("rules");
        if (!rules.isArray() || rules.isEmpty()) {
            throw new BadRequestException("At least one tax rule is required");
        }
        for (JsonNode rule : rules) {
            require(rule, "taxName");
            require(rule, "type");
            require(rule, "applicableOn");
            require(rule, "inclExcl");
            require(rule, "effectiveDate");
            require(rule, "status");
            if (rule.path("active").isMissingNode() || rule.path("active").isNull()) {
                throw new BadRequestException("active is required in taxes.rules");
            }
            if (rule.path("rate").asDouble() <= 0) {
                throw new BadRequestException("Tax rate must be greater than zero");
            }
        }
    }

    private void require(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull() || (value.isTextual() && value.asText().isBlank())) {
            throw new BadRequestException("Missing required field: " + field);
        }
    }

    private void requireAny(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.path(field);
            if (!value.isMissingNode() && !value.isNull() && (!value.isTextual() || !value.asText().isBlank())) {
                return;
            }
        }
        throw new BadRequestException("Missing required field: one of " + String.join(", ", fields));
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
                        return -1;
                    }
                }
            }
        }
        return -1;
    }

    private JsonNode firstObjectNode(JsonNode parent, String... fields) {
        for (String field : fields) {
            JsonNode node = parent.path(field);
            if (node.isObject()) {
                return node;
            }
        }
        return parent.path("__missing__");
    }

    private void parseTime(String value, String fieldName) {
        try {
            LocalTime.parse(value);
            return;
        } catch (DateTimeParseException ex) {
            // Fallback for 12-hour values from UI widgets, e.g. 11:55 AM
            try {
                DateTimeFormatter twelveHour = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
                LocalTime.parse(value.toUpperCase(Locale.ENGLISH), twelveHour);
            } catch (DateTimeParseException innerEx) {
                throw new BadRequestException("Invalid time format for: " + fieldName);
            }
        }
    }

    private record AllocationLimits(Map<String, Integer> roomTypeLimits, Map<String, Integer> propertyAreaLimits) {
    }
}
